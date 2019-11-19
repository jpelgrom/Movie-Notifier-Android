package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.databinding.ActivitySettingsBinding;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.util.EmptyLiveData;
import retrofit2.Call;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;

    private SharedPreferences settings;
    private MutableLiveData<String> lastUserID = new MutableLiveData<>();
    private LiveData<User> lastUser = EmptyLiveData.create();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.getRoot().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

            ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
                v.setPadding(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            });
        }

        setSupportActionBar(binding.toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.settings);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.frame, new SettingsMainFragment(), "settingsMainFragment").commit();
        }

        settings = getSharedPreferences("settings", Context.MODE_PRIVATE);

        lastUser = Transformations.switchMap(lastUserID, newID -> {
            if(newID != null && !newID.equals("")) {
                return AppDatabase.getInstance(this).users().getUserById(newID);
            } else {
                return EmptyLiveData.create();
            }
        });
        lastUserID.setValue(null);
        lastUser.observe(this, user -> updateToolbar());

        getSupportFragmentManager().addOnBackStackChangedListener(() -> updateToolbar());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    public void showUser(String id, boolean isCurrentUser) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.frame, SettingsAccountOverviewFragment.newInstance(id, isCurrentUser), "settingsAccountOverviewFragment")
                .addToBackStack(null)
                .commit();

        lastUserID.setValue(id);
    }

    public void editUserDetail(String id, SettingsAccountUpdateFragment.UpdateMode mode) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.frame, SettingsAccountUpdateFragment.newInstance(id, mode), "settingsAccountUpdateFragment")
                .addToBackStack(null)
                .commit();

        lastUserID.setValue(id); // Just to be sure
    }

    public void hideUserWithMessage(boolean loginNext, String exclude, String message) {
        if(loginNext) {
            tryLoggingInNextUser(exclude);
        }

        if(message != null && !message.equals("")) {
            Snackbar.make(binding.coordinator, message, Snackbar.LENGTH_SHORT).show();
        }

        if(getSupportFragmentManager().findFragmentByTag("settingsAccountOverviewFragment") != null) {
            getSupportFragmentManager().popBackStack();
        }
    }

    public void updatedUser(SettingsAccountUpdateFragment.UpdateMode mode) {
        switch(mode) {
            case NAME:
                Snackbar.make(binding.coordinator, R.string.user_settings_update_name_success, Snackbar.LENGTH_SHORT).show();
                break;
            case EMAIL:
                Snackbar.make(binding.coordinator, R.string.user_settings_update_email_success, Snackbar.LENGTH_SHORT).show();
                break;
            case PASSWORD:
                Snackbar.make(binding.coordinator, R.string.user_settings_security_password_success, Snackbar.LENGTH_SHORT).show();
                break;
        }

        if(getSupportFragmentManager().findFragmentByTag("settingsAccountUpdateFragment") != null) {
            getSupportFragmentManager().popBackStack();
        }

        if(getSupportFragmentManager().findFragmentByTag("settingsAccountOverviewFragment") != null) {
            ((SettingsAccountOverviewFragment) getSupportFragmentManager().findFragmentByTag("settingsAccountOverviewFragment")).updatedUser();
        }
    }

    private void updateToolbar() {
        if(getSupportActionBar() != null) {
            Fragment account = getSupportFragmentManager().findFragmentByTag("settingsAccountOverviewFragment");
            Fragment update = getSupportFragmentManager().findFragmentByTag("settingsAccountUpdateFragment");
            Fragment licenses = getSupportFragmentManager().findFragmentByTag("settingsLicensesFragment");

            if((account != null && account.isVisible()) || (update != null && update.isVisible())) {
                showMenuItems(false);


                if(lastUser.getValue() != null) {
                    getSupportActionBar().setTitle(lastUser.getValue().getName());
                } else {
                    getSupportActionBar().setTitle(R.string.settings);
                }
            } else if(licenses != null && licenses.isVisible()) {
                showMenuItems(false);
                getSupportActionBar().setTitle(R.string.settings_licenses);
            } else {
                showMenuItems(true);
                getSupportActionBar().setTitle(R.string.settings);
            }
        }
    }

    private void showMenuItems(boolean visible) {
        if(binding.toolbar.getMenu() != null) {
            for(int i = 0; i < binding.toolbar.getMenu().size(); i++) {
                binding.toolbar.getMenu().getItem(i).setVisible(visible);
            }
        }
    }

    private void tryLoggingInNextUser(String exclude) {
        AsyncTask.execute(() -> {
            User switchTo = getNextInactiveUser(exclude);

            if(switchTo != null) {
                Call<User> call = APIHelper.getInstance().getUser(switchTo.getApikey(), switchTo.getId());
                try {
                    Response<User> response = call.execute();
                    if(response.code() == 200 && response.body() != null) {
                        User received = response.body();
                        if(received != null) {
                            AppDatabase.getInstance(SettingsActivity.this).users().update(received);
                            settings.edit().putString("userID", received.getId()).putString("userAPIKey", received.getApikey()).apply();
                        }
                    }
                } catch(IOException | RuntimeException e) {
                    // failed
                }
            } // else failed
        });
    }

    private User getNextInactiveUser(String exclude) {
        for(User user : AppDatabase.getInstance(this).users().getUsersSynchronous()) {
            if(!user.getId().equals(settings.getString("userID", "")) && !user.getId().equals(exclude)) {
                return user;
            }
        }
        return null;
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = View.inflate(this, R.layout.view_about, null);
        builder.setView(dialogView);
        builder.setCancelable(true);

        ((TextView) dialogView.findViewById(R.id.version)).setText(getString(R.string.settings_about_version, BuildConfig.VERSION_NAME, String.valueOf(BuildConfig.VERSION_CODE)));
        ((TextView) dialogView.findViewById(R.id.source)).setMovementMethod(new LinkMovementMethod());

        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
            case R.id.homeAsUp:
                onBackPressed();
                return true;
            case R.id.about:
                showAboutDialog();
                return true;
            case R.id.licenses:
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.frame, new SettingsLicensesFragment(), "settingsLicensesFragment")
                        .addToBackStack(null)
                        .commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case SettingsMainFragment.PERMISSION_LOCATION_AUTOCOMPLETE:
            case SettingsMainFragment.PERMISSION_LOCATION_AUTOMAGIC:
                if(getSupportFragmentManager().findFragmentByTag("settingsMainFragment") != null) {
                    getSupportFragmentManager().findFragmentByTag("settingsMainFragment").onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}
