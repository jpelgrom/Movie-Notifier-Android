package nl.jpelgrm.movienotifier.ui.settings;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.service.CinemaUpdateJob;
import nl.jpelgrm.movienotifier.ui.adapter.AccountsAdapter;
import nl.jpelgrm.movienotifier.ui.view.DoubleRowIconPreferenceView;
import retrofit2.Call;
import retrofit2.Response;

public class SettingsMainFragment extends Fragment {
    public final static int PERMISSION_LOCATION_AUTOCOMPLETE = 150;
    public static final int PERMISSION_LOCATION_AUTOMAGIC = 152;
    public static final int PERMISSION_LOCATION_DAYNIGHT = 154;

    @BindView(R.id.settingsCoordinator) CoordinatorLayout coordinator;

    @BindView(R.id.dayNight) DoubleRowIconPreferenceView dayNight;
    @BindView(R.id.dayNightLocation) TextView dayNightLocation;
    @BindView(R.id.location) DoubleRowIconPreferenceView location;
    @BindView(R.id.service) DoubleRowIconPreferenceView service;
    @BindView(R.id.autocomplete) SwitchCompat autocomplete;
    @BindView(R.id.automagic) SwitchCompat automagic;

    @BindView(R.id.accountsRecycler) RecyclerView accountsRecycler;
    @BindView(R.id.accountFlow) DoubleRowIconPreferenceView addAccount;

    private List<Cinema> cinemas = null;
    private CharSequence[] cinemaItems;

    private List<User> users = new ArrayList<>();
    private AccountsAdapter adapter;
    private int userProgress = 0;

    private SharedPreferences settings;

    private BroadcastReceiver broadcastComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateValues();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppDatabase.getInstance(getContext()).cinemas().getCinemas().observe(this, cinemas -> {
            // Data
            Collections.sort(cinemas, new Comparator<Cinema>() {
                @Override
                public int compare(Cinema c1, Cinema c2) {
                    return c1.getName().compareTo(c2.getName());
                }
            });

            // Dialog
            List<String> choices = new ArrayList<>();

            choices.add(getString(R.string.settings_general_location_default));
            for(Cinema cinema : cinemas) {
                choices.add(cinema.getName());
            }
            cinemaItems = choices.toArray(new CharSequence[0]);

            updateValues();
        });

        settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        dayNight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharSequence[] items = { getString(R.string.settings_general_daynight_auto), getString(R.string.settings_general_daynight_day),
                        getString(R.string.settings_general_daynight_night) };
                int currentValueIndex = settings.getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO);

                new AlertDialog.Builder(getContext()).setTitle(R.string.settings_general_daynight_title).setSingleChoiceItems(items, currentValueIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                        setDayNight(which);
                    }
                }).setNegativeButton(R.string.cancel, null).show();
            }
        });
        dayNightLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getActivity() != null && !getActivity().isFinishing()) {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Snackbar.make(coordinator, R.string.settings_general_location_permission_rationale, Snackbar.LENGTH_LONG)
                                .setAction(R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_DAYNIGHT);
                                    }
                                }).show();
                    } else {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_DAYNIGHT);
                    }
                }
            }
        });
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentPreference = settings.getString("prefDefaultCinema", "");
                int currentValueIndex = 0;
                if(cinemas != null) {
                    for(int i = 0; i < cinemas.size(); i++) {
                        if(cinemas.get(i).getId().equals(currentPreference)) {
                            currentValueIndex = i + 1;
                        }
                    }
                }
                if(!currentPreference.equals("") && currentValueIndex == 0) {
                    currentValueIndex = -1; // Don't select anything
                }

                new AlertDialog.Builder(getContext()).setTitle(R.string.settings_general_location_title).setSingleChoiceItems(cinemaItems, currentValueIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                        setCinemaPreference(which);
                    }
                }).setNegativeButton(R.string.cancel, null).show();
            }
        });
        service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.setValue(R.string.settings_general_location_service_updating);
                FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(getContext()));
                dispatcher.mustSchedule(CinemaUpdateJob.getJobToUpdateImmediately(dispatcher));
            }
        });
        autocomplete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAutocompleteLocationPreference(isChecked);
            }
        });
        automagic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAutomagicLocationPreference(isChecked);
            }
        });

        adapter = new AccountsAdapter(getContext(), users);
        accountsRecycler.setAdapter(adapter);
        accountsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(accountsRecycler.getContext(), LinearLayoutManager.VERTICAL);
        accountsRecycler.addItemDecoration(dividerItemDecoration);
        accountsRecycler.setNestedScrollingEnabled(false);

        adapter.swapItems(users);
        updateAccountsList();

        addAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AccountActivity.class));
            }
        });

        updateValues();
    }

    @Override
    public void onResume() {
        super.onResume();

        settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        updateValues();
        updateAccountsList();

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastComplete, new IntentFilter(CinemaUpdateJob.BROADCAST_COMPLETE));
    }

    @Override
    public void onPause() {
        super.onPause();
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(broadcastComplete);
        }
    }

    private void updateValues() {
        int dayNightPreference = settings.getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO);
        switch(dayNightPreference) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                dayNight.setIcon(R.drawable.ic_brightness_day);
                dayNight.setValue(R.string.settings_general_daynight_day);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                dayNight.setIcon(R.drawable.ic_brightness_night);
                dayNight.setValue(R.string.settings_general_daynight_night);
                break;
            case AppCompatDelegate.MODE_NIGHT_AUTO:
            default:
                dayNight.setIcon(R.drawable.ic_brightness_auto);
                dayNight.setValue(R.string.settings_general_daynight_auto);
                break;
        }
        dayNightLocation.setVisibility((dayNightPreference == AppCompatDelegate.MODE_NIGHT_AUTO && getContext() != null
                && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                ? View.VISIBLE : View.GONE);

        String locationPreference = settings.getString("prefDefaultCinema", "");
        String locationPrefText = "";
        if(locationPreference.equals("")) {
            locationPrefText = getString(R.string.settings_general_location_default);
        } else {
            locationPrefText = locationPreference;
            if(cinemas != null) {
                for(Cinema cinema : cinemas) {
                    if(cinema.getId().equals(locationPreference)) {
                        locationPrefText = cinema.getName();
                    }
                }
            }
        }
        location.setValue(locationPrefText);

        if(settings.getLong("cinemasUpdated", -1) != -1) {
            DateFormat format = SimpleDateFormat.getDateInstance(DateFormat.LONG);
            service.setValue(getString(R.string.settings_general_location_service_lastupdate, format.format(new Date(settings.getLong("cinemasUpdated", -1)))));
        } else {
            service.setValue(getString(R.string.settings_general_location_service_lastupdate, getString(R.string.settings_general_location_service_never)));
        }

        boolean granted = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if(settings.getInt("prefAutocompleteLocation", -1) == 1 && !granted) {
            settings.edit().putInt("prefAutocompleteLocation", 0).apply(); // Turn off, we won't get the location anyway
        }
        if(settings.getInt("prefAutomagicLocation", -1) == 1 && !granted) {
            settings.edit().putInt("prefAutomagicLocation", 0).apply(); // Turn off, we won't get the location anyway
        }
        autocomplete.setChecked(settings.getInt("prefAutocompleteLocation", -1) == 1 && granted);
        automagic.setChecked(settings.getInt("prefAutomagicLocation", -1) == 1 && granted);

        int accounts = users.size();
        if(accounts == 0) {
            accountsRecycler.setVisibility(View.GONE);
        } else {
            accountsRecycler.setVisibility(View.VISIBLE);
            adapter.swapItems(users);
        }
    }

    private void setDayNight(int value) {
        int currentValue = settings.getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO);

        settings.edit().putInt("prefDayNight", value).apply();
        AppCompatDelegate.setDefaultNightMode(value);

        if(value != currentValue) {
            getActivity().recreate();
        } else {
            updateValues();
        }
    }

    private void setCinemaPreference(int value) {
        String chose = cinemaItems[value].toString();
        String setTo = ""; // Default; no preference

        if(!chose.equals(getString(R.string.settings_general_location_default))) {
            if(cinemas != null) {
                for(Cinema cinema : cinemas) {
                    if(cinema.getName().equals(chose)) {
                        setTo = cinema.getId();
                    }
                }
            }
        }

        settings.edit().putString("prefDefaultCinema", setTo).apply();
        updateValues();
    }

    private void setAutocompleteLocationPreference(boolean on) {
        setLocationPreference(on, "prefAutocompleteLocation", autocomplete, PERMISSION_LOCATION_AUTOCOMPLETE);
    }

    private void setAutomagicLocationPreference(boolean on) {
        setLocationPreference(on, "prefAutomagicLocation", automagic, PERMISSION_LOCATION_AUTOMAGIC);
    }

    private void setLocationPreference(boolean on, String prefKey, SwitchCompat check, final int requestCode) {
        if(on) {
            if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                settings.edit().putInt(prefKey, 1).apply();
                updateValues();
            } else {
                if(getActivity() != null && !getActivity().isFinishing()) {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                        check.setChecked(false); // Wait for result until switch is set

                        Snackbar.make(coordinator, R.string.settings_general_location_permission_rationale, Snackbar.LENGTH_LONG)
                                .setAction(R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
                                    }
                                }).show();
                    } else {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
                    }
                }
            }
        } else {
            settings.edit().putInt(prefKey, 0).apply();
            updateValues();
        }
    }

    private void updateAccountsList() {
        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            users = db.users().getUsersSynchronous();
            userProgress = 0;

            for(final User user : users) {
                Call<User> call = APIHelper.getInstance().getUser(user.getApikey(), user.getId());
                try {
                    Response<User> response = call.execute();
                    if(response.code() == 200) {
                        if(response.body() != null) {
                            db.users().update(response.body());
                        }
                    } else if(response.code() == 401) {
                        // Authentication failed, which cannot happen unless the user has been deleted, so make sure to delete it here as well
                        db.users().delete(user);

                        if(settings.getString("userID", "").equals(user.getId())) {
                            settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
                        }
                    } // else: failed with user facing error, but do nothing because it is a background task

                    finishedUserUpdate();
                } catch(IOException | RuntimeException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void finishedUserUpdate() {
        userProgress++;
        if(userProgress >= users.size() && getActivity() != null) {
            users = AppDatabase.getInstance(getContext()).users().getUsersSynchronous();
            if(getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(this::updateValues);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch(requestCode) {
            case PERMISSION_LOCATION_AUTOCOMPLETE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    settings.edit().putInt("prefAutocompleteLocation", 1).apply();
                } else {
                    Snackbar.make(coordinator, R.string.settings_general_location_permission_denied, Snackbar.LENGTH_LONG).show();
                    settings.edit().putInt("prefAutocompleteLocation", 0).apply();
                    settings.edit().putInt("prefAutomagicLocation", 0).apply(); // The other one also won't be possible now
                }
                break;
            case PERMISSION_LOCATION_AUTOMAGIC:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    settings.edit().putInt("prefAutomagicLocation", 1).apply();
                } else {
                    Snackbar.make(coordinator, R.string.settings_general_location_permission_denied, Snackbar.LENGTH_LONG).show();
                    settings.edit().putInt("prefAutomagicLocation", 0).apply();
                    settings.edit().putInt("prefAutocompleteLocation", 0).apply(); // The other one also won't be possible now
                }
                break;
            case PERMISSION_LOCATION_DAYNIGHT:
                if(grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(coordinator, R.string.settings_general_location_permission_denied, Snackbar.LENGTH_LONG).show();
                    settings.edit().putInt("prefAutocompleteLocation", 0).apply(); // These also won't be possible now
                    settings.edit().putInt("prefAutomagicLocation", 0).apply(); // These also won't be possible now
                } // else if granted is handled by updateValues();
                break;
        }
        updateValues();
    }
}
