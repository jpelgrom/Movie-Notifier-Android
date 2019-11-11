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
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.databinding.FragmentSettingsMainBinding;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.service.CinemaUpdateWorker;
import nl.jpelgrm.movienotifier.ui.adapter.AccountsAdapter;
import nl.jpelgrm.movienotifier.util.NotificationUtil;
import retrofit2.Call;
import retrofit2.Response;

public class SettingsMainFragment extends Fragment {
    public final static int PERMISSION_LOCATION_AUTOCOMPLETE = 150;
    public static final int PERMISSION_LOCATION_AUTOMAGIC = 152;

    private FragmentSettingsMainBinding binding;

    private List<Cinema> cinemas = null;
    private CharSequence[] cinemaItems;

    private List<User> users = new ArrayList<>();
    private AccountsAdapter adapter;

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

        AppDatabase db = AppDatabase.getInstance(getContext());
        db.cinemas().getCinemas().observe(this, cinemas -> {
            // Data
            Collections.sort(cinemas, (c1, c2) -> c1.getName().compareTo(c2.getName()));
            this.cinemas = cinemas;

            // Dialog
            List<String> choices = new ArrayList<>();

            choices.add(getString(R.string.settings_general_location_default));
            for(Cinema cinema : cinemas) {
                choices.add(cinema.getName());
            }
            cinemaItems = choices.toArray(new CharSequence[0]);

            updateCinemaValues();
        });
        db.users().getUsers().observe(this, users -> {
            this.users = users;
            updateUsersValues();
        });

        settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.darkTheme.setOnClickListener(view1 -> {
            CharSequence[] items = { getString(R.string.settings_general_darktheme_light), getString(R.string.settings_general_darktheme_dark),
                    getString(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? R.string.settings_general_darktheme_system : R.string.settings_general_darktheme_batterysaver) };
            int currentPreference = settings.getInt("prefDarkTheme",
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            int currentValueIndex = (currentPreference == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM || currentPreference == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY) ? 2 : (currentPreference - 1);

            new AlertDialog.Builder(getContext()).setTitle(R.string.settings_general_darktheme_title)
                    .setSingleChoiceItems(items, currentValueIndex, (dialogInterface, which) -> {
                dialogInterface.dismiss();
                setDarkTheme(which);
            }).setNegativeButton(R.string.cancel, null).show();
        });
        binding.location.setOnClickListener(view4 -> {
            int currentPreference = settings.getInt("prefSelectedCinema", 0);
            int currentValueIndex = 0;
            if(cinemas != null) {
                for(int i = 0; i < cinemas.size(); i++) {
                    if(cinemas.get(i).getId().equals(currentPreference)) {
                        currentValueIndex = i + 1;
                    }
                }
            }
            if(currentPreference != 0 && currentValueIndex == 0) {
                currentValueIndex = -1; // Don't select anything
            }

            new AlertDialog.Builder(getContext()).setTitle(R.string.settings_general_location_title).setSingleChoiceItems(cinemaItems, currentValueIndex, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    dialogInterface.dismiss();
                    setCinemaPreference(which);
                }
            }).setNegativeButton(R.string.cancel, null).show();
        });
        binding.service.setOnClickListener(view5 -> {
            binding.service.setValue(R.string.settings_general_location_service_updating);
            WorkManager.getInstance().enqueue(CinemaUpdateWorker.getRequestToUpdateImmediately());
        });
        binding.autocomplete.setOnCheckedChangeListener((buttonView, isChecked) -> setAutocompleteLocationPreference(isChecked));
        binding.automagic.setOnCheckedChangeListener((buttonView, isChecked) -> setAutomagicLocationPreference(isChecked));

        adapter = new AccountsAdapter(getContext(), users);
        binding.accountsRecycler.setAdapter(adapter);
        binding.accountsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(binding.accountsRecycler.getContext(), LinearLayoutManager.VERTICAL);
        binding.accountsRecycler.addItemDecoration(dividerItemDecoration);
        binding.accountsRecycler.setNestedScrollingEnabled(false);

        adapter.swapItems(users);

        binding.accountFlow.setOnClickListener(view6 -> startActivity(new Intent(getActivity(), AccountActivity.class)));

        updateValues();
    }

    @Override
    public void onResume() {
        super.onResume();

        settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        updateValues();
        updateAccountsList();

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastComplete, new IntentFilter(CinemaUpdateWorker.BROADCAST_COMPLETE));
    }

    @Override
    public void onPause() {
        super.onPause();
        if(getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(broadcastComplete);
        }
    }

    private void updateValues() {
        int darkThemePreference = settings.getInt("prefDarkTheme",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
        switch(darkThemePreference) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                binding.darkTheme.setValue(R.string.settings_general_darktheme_light);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                binding.darkTheme.setValue(R.string.settings_general_darktheme_dark);
                break;
            case AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY:
                binding.darkTheme.setValue(R.string.settings_general_darktheme_batterysaver);
                break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
            default:
                binding.darkTheme.setValue(R.string.settings_general_darktheme_system);
                break;
        }

        updateCinemaValues();

        if(settings.getLong("cinemasUpdated", -1) != -1) {
            DateFormat format = SimpleDateFormat.getDateInstance(DateFormat.LONG);
            binding.service.setValue(getString(R.string.settings_general_location_service_lastupdate, format.format(new Date(settings.getLong("cinemasUpdated", -1)))));
        } else {
            binding.service.setValue(getString(R.string.settings_general_location_service_lastupdate, getString(R.string.settings_general_location_service_never)));
        }

        boolean granted = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if(settings.getInt("prefAutocompleteLocation", -1) == 1 && !granted) {
            settings.edit().putInt("prefAutocompleteLocation", 0).apply(); // Turn off, we won't get the location anyway
        }
        if(settings.getInt("prefAutomagicLocation", -1) == 1 && !granted) {
            settings.edit().putInt("prefAutomagicLocation", 0).apply(); // Turn off, we won't get the location anyway
        }
        binding.autocomplete.setChecked(settings.getInt("prefAutocompleteLocation", -1) == 1 && granted);
        binding.automagic.setChecked(settings.getInt("prefAutomagicLocation", -1) == 1 && granted);

        updateUsersValues();
    }

    private void updateCinemaValues() {
        int locationPreference = settings.getInt("prefSelectedCinema", 0);
        String locationPrefText = "";
        if(locationPreference == 0) {
            locationPrefText = getString(R.string.settings_general_location_default);
        } else {
            locationPrefText = String.valueOf(locationPreference);
            if(cinemas != null) {
                for(Cinema cinema : cinemas) {
                    if(cinema.getId().equals(locationPreference)) {
                        locationPrefText = cinema.getName();
                    }
                }
            }
        }
        binding.location.setValue(locationPrefText);
    }

    private void updateUsersValues() {
        int accounts = users.size();
        if(accounts == 0) {
            binding.accountsRecycler.setVisibility(View.GONE);
        } else {
            binding.accountsRecycler.setVisibility(View.VISIBLE);
            adapter.swapItems(users);
        }
    }

    private void setDarkTheme(int ordinal) {
        int currentValue = settings.getInt("prefDarkTheme",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
        int newValue = 0;
        switch(ordinal) {
            case 0:
                newValue = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case 1:
                newValue = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case 2:
                newValue = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
                break;
        }

        settings.edit().putInt("prefDarkTheme", newValue).apply();
        AppCompatDelegate.setDefaultNightMode(newValue);

        updateValues();
    }

    private void setCinemaPreference(int value) {
        String chose = cinemaItems[value].toString();
        int setTo = 0; // Default; no preference

        if(!chose.equals(getString(R.string.settings_general_location_default))) {
            if(cinemas != null) {
                for(Cinema cinema : cinemas) {
                    if(cinema.getName().equals(chose)) {
                        setTo = cinema.getId();
                    }
                }
            }
        }

        settings.edit().putInt("prefSelectedCinema", setTo).apply();
        updateValues();
    }

    private void setAutocompleteLocationPreference(boolean on) {
        setLocationPreference(on, "prefAutocompleteLocation", binding.autocomplete, PERMISSION_LOCATION_AUTOCOMPLETE);
    }

    private void setAutomagicLocationPreference(boolean on) {
        setLocationPreference(on, "prefAutomagicLocation", binding.automagic, PERMISSION_LOCATION_AUTOMAGIC);
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

                        Snackbar.make(binding.settingsCoordinator, R.string.settings_general_location_permission_rationale, Snackbar.LENGTH_LONG)
                                .setAction(R.string.ok, view -> ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode)).show();
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

            for(final User user : users) {
                Call<User> call = APIHelper.getInstance().getUser(user.getApikey(), user.getId());
                try {
                    Response<User> response = call.execute();
                    if(response.code() == 200) {
                        if(response.body() != null) {
                            db.users().update(response.body());
                            if(!user.getName().equals(response.body().getName())) {
                                NotificationUtil.createUserGroup(getContext(), response.body());
                            }
                        }
                    } else if(response.code() == 401) {
                        // Authentication failed, which cannot happen unless the user has been deleted, so make sure to delete it here as well
                        db.users().delete(user);
                        NotificationUtil.cleanupPreferencesForUser(getContext(), user.getId());

                        if(settings.getString("userID", "").equals(user.getId())) {
                            settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
                        }
                    } // else: failed with user facing error, but do nothing because it is a background task
                } catch(IOException | RuntimeException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch(requestCode) {
            case PERMISSION_LOCATION_AUTOCOMPLETE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    settings.edit().putInt("prefAutocompleteLocation", 1).apply();
                } else {
                    Snackbar.make(binding.settingsCoordinator, R.string.settings_general_location_permission_denied, Snackbar.LENGTH_LONG).show();
                    settings.edit().putInt("prefAutocompleteLocation", 0).apply();
                    settings.edit().putInt("prefAutomagicLocation", 0).apply(); // The other one also won't be possible now
                }
                break;
            case PERMISSION_LOCATION_AUTOMAGIC:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    settings.edit().putInt("prefAutomagicLocation", 1).apply();
                } else {
                    Snackbar.make(binding.settingsCoordinator, R.string.settings_general_location_permission_denied, Snackbar.LENGTH_LONG).show();
                    settings.edit().putInt("prefAutomagicLocation", 0).apply();
                    settings.edit().putInt("prefAutocompleteLocation", 0).apply(); // The other one also won't be possible now
                }
                break;
        }
        updateValues();
    }
}
