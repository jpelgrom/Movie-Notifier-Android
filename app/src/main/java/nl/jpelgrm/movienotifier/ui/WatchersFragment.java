package nl.jpelgrm.movienotifier.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.DBHelper;
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

    @BindView(R.id.coordinator) CoordinatorLayout coordinator;

    @BindView(R.id.progress) ProgressBar progress;

    @BindView(R.id.emptyView) LinearLayout emptyView;
    @BindView(R.id.emptyText) TextView emptyText;

    @BindView(R.id.listSwiper) SwipeRefreshLayout listSwiper;
    @BindView(R.id.automagicSuggestion) LinearLayout listAutomagicSuggestion;
    @BindView(R.id.automagicSuggestionCancel) AppCompatImageButton listAutomagicSuggestionCancel;
    @BindView(R.id.listRecycler) RecyclerView listRecycler;

    @BindView(R.id.fab) FloatingActionButton fab;

    private List<Watcher> watchers = new ArrayList<>();
    private List<Watcher> watchersSorted = new ArrayList<>();
    private WatchersAdapter adapter;

    private List<Cinema> cinemas = null;

    private SharedPreferences settings;

    private LocationUtil locationUtil = new LocationUtil();
    private Location locationUser = null;

    private Snackbar snackbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cinemas = DBHelper.getInstance(getContext()).getCinemas();

        settings = getContext().getSharedPreferences("settings", MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchers, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        // List
        listSwiper.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(snackbar != null && snackbar.isShown()) {
                    snackbar.dismiss();
                }
                refreshList(true, true);
                if(settings.getInt("prefAutomagicLocation", -1) == 1 && settings.getInt("listSort", 0) == 0) {
                    startLocation();
                }
            }
        });
        adapter = new WatchersAdapter(getContext(), watchers);
        listRecycler.setAdapter(adapter);
        listRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(listRecycler.getContext(), LinearLayoutManager.VERTICAL);
        listRecycler.addItemDecoration(dividerItemDecoration);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getContext(), WatcherActivity.class));
            }
        });

        if(settings.getInt("prefAutomagicLocation", -1) == -1) {
            listAutomagicSuggestion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    askForLocation();
                }
            });
            listAutomagicSuggestionCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    settings.edit().putInt("prefAutomagicLocation", 0).apply();
                    listAutomagicSuggestion.setVisibility(View.GONE);
                }
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
        locationUtil.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        locationUtil.onStop();
    }

    private void refreshList(final boolean userTriggered, final boolean showError) {
        listSwiper.post(new Runnable() {
            @Override
            public void run() {
                listSwiper.setRefreshing(true);

                if(!settings.getString("userID", "").equals("")) {
                    Call<List<Watcher>> call = APIHelper.getInstance().getWatchers(settings.getString("userAPIKey", ""));
                    call.enqueue(new Callback<List<Watcher>>() {
                        @Override
                        public void onResponse(Call<List<Watcher>> call, Response<List<Watcher>> response) {
                            listSwiper.setRefreshing(false);
                            watchers = response.body();

                            if(response.code() == 200) {
                                filterAndSort(false);
                            } else {
                                if(showError) {
                                    if(response.code() == 401) {
                                        snackbar = Snackbar.make(coordinator, R.string.error_general_401, Snackbar.LENGTH_INDEFINITE);
                                        snackbar.setAction(R.string.ok, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                startActivity(new Intent(getActivity(), AccountActivity.class));
                                            }
                                        });
                                    } else if(response.code() >= 500 && response.code() < 600){
                                        snackbar = Snackbar.make(coordinator, R.string.error_watchers_500, Snackbar.LENGTH_INDEFINITE);
                                    } else {
                                        snackbar = Snackbar.make(coordinator, R.string.error_watchers_400, Snackbar.LENGTH_INDEFINITE);
                                    }
                                    snackbar.show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Watcher>> call, Throwable t) {
                            t.printStackTrace();

                            listSwiper.setRefreshing(false);

                            if(showError) {
                                snackbar = Snackbar.make(coordinator, R.string.error_general_exception, Snackbar.LENGTH_INDEFINITE);
                                snackbar.show();
                            }
                        }
                    });
                } else {
                    showEmptyView();

                    if(userTriggered) {
                        snackbar = Snackbar.make(coordinator, R.string.watchers_empty_account, Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(R.string.add, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                startActivity(new Intent(getActivity(), AccountActivity.class));
                            }
                        });
                        snackbar.show();
                    }
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

        Comparator<Watcher> sortAZ = new Comparator<Watcher>() {
            @Override
            public int compare(Watcher w1, Watcher w2) {
                return w1.getName().compareToIgnoreCase(w2.getName());
            }
        };

        if(sort == 0) { // Automagic
            boolean highlightNearby = false;
            Cinema nearby = null;
            if(settings.getInt("prefAutomagicLocation", -1) == 1
                    && locationUser != null
                    && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                nearby = locationUtil.getClosestCinema(locationUser, cinemas);
                if(nearby != null && LocationUtil.getDistance(locationUser, nearby.getLatitude(), nearby.getLongitude()) < 2000) {
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
                    if(highlightNearby && watcher.getFilters().getCinemaID().equals(nearby.getID())) {
                        watchersNowNearby.add(watcher);
                    } else {
                        watchersNow.add(watcher);
                    }
                } else if(watcher.getEnd() < System.currentTimeMillis()) {
                    if(highlightNearby && watcher.getFilters().getCinemaID().equals(nearby.getID())) {
                        watchersPastNearby.add(watcher);
                    } else {
                        watchersPast.add(watcher);
                    }
                } else if(watcher.getBegin() > System.currentTimeMillis()) {
                    if(highlightNearby && watcher.getFilters().getCinemaID().equals(nearby.getID())) {
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
                case 2: // End (stop checking)
                    comparator = new Comparator<Watcher>() {
                        @Override
                        public int compare(Watcher w1, Watcher w2) {
                            return Long.compare(w1.getEnd(), w2.getEnd());
                        }
                    };
                    break;
                case 3: // Start after (first showing)
                    comparator = new Comparator<Watcher>() {
                        @Override
                        public int compare(Watcher w1, Watcher w2) {
                            return Long.compare(w1.getFilters().getStartAfter(), w2.getFilters().getStartAfter());
                        }
                    };
                    break;
                case 4: // A-Z
                    comparator = sortAZ;
                    break;
                case 1: // Begin (start checking)
                default:
                    comparator = new Comparator<Watcher>() {
                        @Override
                        public int compare(Watcher w1, Watcher w2) {
                            return Long.compare(w1.getBegin(), w2.getBegin());
                        }
                    };
                    break;
            }
            Collections.sort(watchersSorted, comparator);
        }

        // Show
        adapter.swapItems(watchersSorted);
        if(scrollToTop) {
            ((LinearLayoutManager) listRecycler.getLayoutManager()).scrollToPositionWithOffset(0, 0);
        }

        // Ensure the correct state is shown
        if(watchersSorted.size() == 0) {
            showEmptyView();
        } else {
            emptyView.setVisibility(View.GONE);
            listRecycler.setVisibility(View.VISIBLE);

            if(sort == 0 && settings.getInt("prefAutomagicLocation", -1) == -1) {
                listAutomagicSuggestion.setVisibility(View.VISIBLE);
            } else {
                listAutomagicSuggestion.setVisibility(View.GONE);
            }
        }
    }

    private void showEmptyView() {
        listSwiper.setRefreshing(false);
        listAutomagicSuggestion.setVisibility(View.GONE);
        listRecycler.setVisibility(View.GONE);

        emptyView.setVisibility(View.VISIBLE);
        if(watchers.size() != 0 && watchersSorted.size() == 0) {
            emptyText.setText(R.string.watchers_empty_filters);
        } else {
            emptyText.setText(R.string.watchers_empty_add);
        }
    }

    public void deleteWatcher(final String id) {
        new AlertDialog.Builder(getContext()).setMessage(R.string.watcher_delete_confirm).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                progress.setVisibility(View.VISIBLE);
                listRecycler.setClickable(false);

                Call<ResponseBody> call = APIHelper.getInstance().deleteWatcher(settings.getString("userAPIKey", ""), id);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        progress.setVisibility(View.GONE);
                        listRecycler.setClickable(true);

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

                        snackbar = Snackbar.make(coordinator, message, Snackbar.LENGTH_SHORT);
                        snackbar.show();
                        refreshList(false, false);
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        t.printStackTrace();

                        progress.setVisibility(View.GONE);
                        listRecycler.setClickable(true);

                        snackbar = Snackbar.make(coordinator, R.string.error_general_exception, Snackbar.LENGTH_SHORT);
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
            listAutomagicSuggestion.setVisibility(View.GONE);
            startLocation();
        } else {
            if(!getActivity().isFinishing()) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                    snackbar = Snackbar.make(coordinator, R.string.settings_general_location_permission_rationale, Snackbar.LENGTH_LONG)
                            .setAction(R.string.ok, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_AUTOMAGIC);
                                }
                            });
                    snackbar.show();
                } else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_AUTOMAGIC);
                }
            }
        }
    }

    private void startLocation() {
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationUtil.setupGoogleClient(getContext(), false);
            locationUtil.getLocation(getContext(), new LocationUtil.LocationUtilRequest() {
                @Override
                public void onLocationReceived(Location location, boolean isCachedResult) {
                    locationUser = location;
                    if(!isCachedResult && !listSwiper.isRefreshing()) {
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
                    snackbar = Snackbar.make(coordinator, R.string.settings_general_location_permission_denied, Snackbar.LENGTH_LONG);
                    snackbar.show();
                    settings.edit().putInt("prefAutomagicLocation", 0).apply();
                }
                listAutomagicSuggestion.setVisibility(View.GONE);
                break;
        }
    }
}
