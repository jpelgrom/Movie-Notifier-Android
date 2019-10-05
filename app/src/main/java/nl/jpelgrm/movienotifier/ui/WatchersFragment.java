package nl.jpelgrm.movienotifier.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.databinding.FragmentWatchersBinding;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.Watcher;
import nl.jpelgrm.movienotifier.ui.adapter.WatchersAdapter;
import nl.jpelgrm.movienotifier.ui.settings.AccountActivity;
import nl.jpelgrm.movienotifier.util.LocationUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

public class WatchersFragment extends Fragment {
    public static final int PERMISSION_LOCATION_AUTOMAGIC = 153;

    private FragmentWatchersBinding binding;

    private List<Watcher> watchers = new ArrayList<>();
    private List<Watcher> watchersSorted = new ArrayList<>();
    private WatchersAdapter adapter;

    private List<Cinema> cinemas = new ArrayList<>();

    private SharedPreferences settings;

    private LocationUtil locationUtil = new LocationUtil();
    private Location locationUser = null;

    private Snackbar snackbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppDatabase.getInstance(getContext()).cinemas().getCinemas()
                .observe(this, cinemas -> this.cinemas = cinemas);

        settings = getContext().getSharedPreferences("settings", MODE_PRIVATE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWatchersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // List
        binding.listSwiper.setOnRefreshListener(() -> {
            if(snackbar != null && snackbar.isShown()) {
                snackbar.dismiss();
            }
            refreshList(true, true);
            if(settings.getInt("prefAutomagicLocation", -1) == 1 && settings.getInt("listSort", 0) == 0) {
                startLocation();
            }
        });
        adapter = new WatchersAdapter(getContext(), watchers);
        binding.listRecycler.setAdapter(adapter);
        binding.listRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(binding.listRecycler.getContext(), LinearLayoutManager.VERTICAL);
        binding.listRecycler.addItemDecoration(dividerItemDecoration);

        binding.fab.setOnClickListener(view1 -> startActivity(new Intent(getContext(), WatcherActivity.class)));

        if(settings.getInt("prefAutomagicLocation", -1) == -1) {
            binding.automagicSuggestion.setOnClickListener(view2 -> askForLocation());
            binding.automagicSuggestionCancel.setOnClickListener(view3 -> {
                settings.edit().putInt("prefAutomagicLocation", 0).apply();
                binding.automagicSuggestion.setVisibility(View.GONE);
            });
        } else {
            if(settings.getInt("prefAutomagicLocation", -1) == 1 && settings.getInt("listSort", 0) == 0) {
                startLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        settings = getContext().getSharedPreferences("settings", MODE_PRIVATE);

        if(snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }

        refreshList(false, true);
        if(settings.getInt("prefAutomagicLocation", -1) == 1 && settings.getInt("listSort", 0) == 0) {
            startLocation();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        locationUtil.onStop();
    }

    private void refreshList(final boolean userTriggered, final boolean showError) {
        binding.listSwiper.post(() -> {
            binding.listSwiper.setRefreshing(true);

            if(!settings.getString("userID", "").equals("")) {
                Call<List<Watcher>> call = APIHelper.getInstance().getWatchers(settings.getString("userAPIKey", ""));
                call.enqueue(new Callback<List<Watcher>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Watcher>> call, @NonNull Response<List<Watcher>> response) {
                        binding.listSwiper.setRefreshing(false);

                        if(response.code() == 200) {
                            watchers = response.body();
                            filterAndSort(false);
                        } else {
                            if(showError) {
                                if(response.code() == 401) {
                                    snackbar = Snackbar.make(binding.coordinator, R.string.error_general_401, Snackbar.LENGTH_INDEFINITE);
                                    snackbar.setAction(R.string.ok, view -> startActivity(new Intent(getActivity(), AccountActivity.class)));
                                } else if(response.code() >= 500 && response.code() < 600){
                                    snackbar = Snackbar.make(binding.coordinator, R.string.error_watchers_500, Snackbar.LENGTH_INDEFINITE);
                                } else {
                                    snackbar = Snackbar.make(binding.coordinator, R.string.error_watchers_400, Snackbar.LENGTH_INDEFINITE);
                                }
                                snackbar.show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Watcher>> call, @NonNull Throwable t) {
                        t.printStackTrace();

                        binding.listSwiper.setRefreshing(false);

                        if(showError) {
                            snackbar = Snackbar.make(binding.coordinator, R.string.error_general_exception, Snackbar.LENGTH_INDEFINITE);
                            snackbar.show();
                        }
                    }
                });
            } else {
                watchers.clear();
                showEmptyView();

                if(userTriggered) {
                    snackbar = Snackbar.make(binding.coordinator, R.string.watchers_empty_account, Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.add, view -> startActivity(new Intent(getActivity(), AccountActivity.class)));
                    snackbar.show();
                }
            }
        });
    }

    public void filterAndSort(boolean scrollToTop) {
        watchersSorted.clear();

        // Filter
        int filter = settings.getInt("listFilter", 0);
        for(int i = 0; i < watchers.size(); i++) {
            Watcher watcher = watchers.get(i);
            boolean match = false;

            if(filter == 0) {
                match = true;
            } else {
                if((filter == 2 && watcher.getBegin() <= System.currentTimeMillis() && watcher.getEnd() > System.currentTimeMillis())
                    || (filter == 1 && watcher.getEnd() < System.currentTimeMillis())
                    || (filter == 3 && watcher.getBegin() > System.currentTimeMillis())) {
                    match = true;
                }
            }

            if(match) {
                watchersSorted.add(watcher);
            }
        }

        // Sort
        int sort = settings.getInt("listSort", 0);

        Comparator<Watcher> sortAZ = (w1, w2) -> w1.getName().compareToIgnoreCase(w2.getName());

        if(sort == 0) { // Automagic
            boolean highlightNearby = false;
            Cinema nearby = null;
            if(settings.getInt("prefAutomagicLocation", -1) == 1
                    && locationUser != null
                    && getContext() != null
                    && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                nearby = locationUtil.getClosestCinema(locationUser, cinemas);
                if(nearby != null && LocationUtil.getDistance(locationUser, nearby.getLat(), nearby.getLon()) < 2000) {
                    highlightNearby = true;
                }
            }

            // First separate by status
            List<Watcher> watchersPastNearby = new ArrayList<>();
            List<Watcher> watchersPast = new ArrayList<>();
            List<Watcher> watchersNowNearby = new ArrayList<>();
            List<Watcher> watchersNow = new ArrayList<>();
            List<Watcher> watchersFutureNearby = new ArrayList<>();
            List<Watcher> watchersFuture = new ArrayList<>();
            for(int i = 0; i < watchersSorted.size(); i++) {
                Watcher watcher = watchersSorted.get(i);
                if(watcher.getBegin() <= System.currentTimeMillis() && watcher.getEnd() > System.currentTimeMillis()) {
                    if(highlightNearby && watcher.getFilters().getCinemaID() == nearby.getId()) {
                        watchersNowNearby.add(watcher);
                    } else {
                        watchersNow.add(watcher);
                    }
                } else if(watcher.getEnd() < System.currentTimeMillis()) {
                    if(highlightNearby && watcher.getFilters().getCinemaID() == nearby.getId()) {
                        watchersPastNearby.add(watcher);
                    } else {
                        watchersPast.add(watcher);
                    }
                } else if(watcher.getBegin() > System.currentTimeMillis()) {
                    if(highlightNearby && watcher.getFilters().getCinemaID() == nearby.getId()) {
                        watchersFutureNearby.add(watcher);
                    } else {
                        watchersFuture.add(watcher);
                    }
                }
            }

            // Sort by alphabet
            Collections.sort(watchersPastNearby, sortAZ);
            Collections.sort(watchersPast, sortAZ);
            Collections.sort(watchersNowNearby, sortAZ);
            Collections.sort(watchersNow, sortAZ);
            Collections.sort(watchersFutureNearby, sortAZ);
            Collections.sort(watchersFuture, sortAZ);

            watchersSorted.clear();
            watchersSorted.addAll(watchersNowNearby);
            watchersSorted.addAll(watchersNow);
            watchersSorted.addAll(watchersFutureNearby);
            watchersSorted.addAll(watchersFuture);
            watchersSorted.addAll(watchersPastNearby);
            watchersSorted.addAll(watchersPast);
        } else {
            Comparator<Watcher> comparator;
            switch(sort) {
                case 1: // Begin (start checking)
                    comparator = (w1, w2) -> Long.compare(w2.getBegin(), w1.getBegin());
                    break;
                case 2: // End (stop checking)
                    comparator = (w1, w2) -> Long.compare(w2.getEnd(), w1.getEnd());
                    break;
                case 3: // Start after (first showing)
                    comparator = (w1, w2) -> Long.compare(w2.getFilters().getStartAfter(), w1.getFilters().getStartAfter());
                    break;
                case 4: // A-Z
                default:
                    comparator = sortAZ;
                    break;
            }
            Collections.sort(watchersSorted, comparator);
        }

        // Show
        adapter.swapItems(watchersSorted);
        if(scrollToTop) {
            ((LinearLayoutManager) binding.listRecycler.getLayoutManager()).scrollToPositionWithOffset(0, 0);
        }

        // Ensure the correct state is shown
        if(watchersSorted.size() == 0) {
            showEmptyView();
        } else {
            binding.emptyView.setVisibility(View.GONE);
            binding.listRecycler.setVisibility(View.VISIBLE);

            if(sort == 0 && settings.getInt("prefAutomagicLocation", -1) == -1) {
                binding.automagicSuggestion.setVisibility(View.VISIBLE);
            } else {
                binding.automagicSuggestion.setVisibility(View.GONE);
            }
        }
    }

    public void scrollListToTop() {
        if(binding != null && binding.listRecycler != null && binding.listRecycler.getLayoutManager() instanceof LinearLayoutManager) {
            ((LinearLayoutManager) binding.listRecycler.getLayoutManager()).scrollToPositionWithOffset(0, 0);
        }
    }

    private void showEmptyView() {
        binding.listSwiper.setRefreshing(false);
        binding.automagicSuggestion.setVisibility(View.GONE);
        binding.listRecycler.setVisibility(View.GONE);

        binding.emptyView.setVisibility(View.VISIBLE);
        if(watchers.size() != 0 && watchersSorted.size() == 0) {
            binding.emptyText.setText(R.string.watchers_empty_filters);
        } else {
            binding.emptyText.setText(R.string.watchers_empty_add);
        }
    }

    public void deleteWatcher(final String id) {
        new AlertDialog.Builder(getContext()).setMessage(R.string.watcher_delete_confirm).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                binding.progress.setVisibility(View.VISIBLE);
                binding.listRecycler.setClickable(false);

                Call<ResponseBody> call = APIHelper.getInstance().deleteWatcher(settings.getString("userAPIKey", ""), id);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        binding.progress.setVisibility(View.GONE);
                        binding.listRecycler.setClickable(true);

                        String message;
                        if(response.code() == 200) {
                            message = getString(R.string.watcher_delete_success);
                        } else {
                            if(response.code() == 400) {
                                message = getString(R.string.error_watcher_400);
                            } else if(response.code() == 401) {
                                message = getString(R.string.error_watcher_401);
                            } else {
                                message = getString(R.string.error_general_server, "H" + response.code());
                            }
                        }

                        snackbar = Snackbar.make(binding.coordinator, message, Snackbar.LENGTH_SHORT);
                        snackbar.show();
                        refreshList(false, false);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        t.printStackTrace();

                        binding.progress.setVisibility(View.GONE);
                        binding.listRecycler.setClickable(true);

                        snackbar = Snackbar.make(binding.coordinator, R.string.error_general_exception, Snackbar.LENGTH_SHORT);
                        snackbar.show();
                        refreshList(false, false);
                    }
                });
            }
        }).setNegativeButton(R.string.no, null).show();
    }

    private void askForLocation() {
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            settings.edit().putInt("prefAutomagicLocation", 1).apply();
            binding.automagicSuggestion.setVisibility(View.GONE);
            startLocation();
        } else {
            if(!getActivity().isFinishing()) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                    snackbar = Snackbar.make(binding.coordinator, R.string.settings_general_location_permission_rationale, Snackbar.LENGTH_LONG)
                            .setAction(R.string.ok, view -> ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_AUTOMAGIC));
                    snackbar.show();
                } else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_AUTOMAGIC);
                }
            }
        }
    }

    private void startLocation() {
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationUtil.setupLocationClient(getContext());
            locationUtil.getLocation(getContext(), new LocationUtil.LocationUtilRequest() {
                @Override
                public void onLocationReceived(Location location, boolean isCachedResult) {
                    locationUser = location;
                    if(!isCachedResult && !binding.listSwiper.isRefreshing()) {
                        filterAndSort(true);
                    }
                }

                @Override
                public Context getContext() {
                    return WatchersFragment.this.getContext();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case PERMISSION_LOCATION_AUTOMAGIC:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    settings.edit().putInt("prefAutomagicLocation", 1).apply();
                    startLocation();
                } else {
                    snackbar = Snackbar.make(binding.coordinator, R.string.settings_general_location_permission_denied, Snackbar.LENGTH_LONG);
                    snackbar.show();
                    settings.edit().putInt("prefAutomagicLocation", 0).apply();
                }
                binding.automagicSuggestion.setVisibility(View.GONE);
                break;
        }
    }
}
