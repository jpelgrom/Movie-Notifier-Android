package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.DBHelper;
import nl.jpelgrm.movienotifier.models.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {
    @BindView(R.id.coordinator) CoordinatorLayout coordinator;
    @BindView(R.id.toolbar) Toolbar toolbar;

    private SharedPreferences settings;
    private String lastUserID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.settings);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.frame, new SettingsMainFragment(), "settingsMainFragment").commit();
        }

        settings = getSharedPreferences("settings", Context.MODE_PRIVATE);

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                updateToolbar();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    public void showUser(String id) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.frame, SettingsAccountOverviewFragment.newInstance(id), "settingsAccountOverviewFragment")
                .addToBackStack(null)
                .commit();

        lastUserID = id;
    }

    public void editUserDetail(String id, SettingsAccountUpdateFragment.UpdateMode mode) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.frame, SettingsAccountUpdateFragment.newInstance(id, mode), "settingsAccountUpdateFragment")
                .addToBackStack(null)
                .commit();

        lastUserID = id; // Just to be sure
    }

    public void hideUserWithMessage(boolean loginNext, String exclude, String message) {
        if(loginNext) {
            tryLoggingInNextUser(exclude);
        }

        if(message != null && !message.equals("")) {
            Snackbar.make(coordinator, message, Snackbar.LENGTH_SHORT).show();
        }

        if(getSupportFragmentManager().findFragmentByTag("settingsAccountOverviewFragment") != null) {
            getSupportFragmentManager().popBackStack();
        }
    }

    public void updatedUser(SettingsAccountUpdateFragment.UpdateMode mode) {
        switch(mode) {
            case NAME:
                Snackbar.make(coordinator, R.string.user_settings_update_name_success, Snackbar.LENGTH_SHORT).show();
                break;
            case EMAIL:
                Snackbar.make(coordinator, R.string.user_settings_update_email_success, Snackbar.LENGTH_SHORT).show();
                break;
            case PHONE:
                Snackbar.make(coordinator, R.string.user_settings_update_phone_success, Snackbar.LENGTH_SHORT).show();
                break;
            case PASSWORD:
                Snackbar.make(coordinator, R.string.user_settings_security_password_success, Snackbar.LENGTH_SHORT).show();
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

                User displayed = null;
                if(lastUserID != null && !lastUserID.equals("")) {
                    displayed = DBHelper.getInstance(this).getUserByID(lastUserID);
                }
                if(displayed != null) {
                    getSupportActionBar().setTitle(displayed.getName());
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
        if(toolbar.getMenu() != null) {
            for(int i = 0; i < toolbar.getMenu().size(); i++) {
                toolbar.getMenu().getItem(i).setVisible(visible);
            }
        }
    }

    private void tryLoggingInNextUser(String exclude) {
        User switchTo = getNextInactiveUser(exclude);

        if(switchTo != null) {
            Call<User> call = APIHelper.getInstance().getUser(switchTo.getApikey(), switchTo.getID());
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if(response.code() == 200 && response.body() != null) {
                        User received = response.body();
                        if(received != null) {
                            DBHelper.getInstance(SettingsActivity.this).updateUser(received);
                            settings.edit().putString("userID", received.getID()).putString("userAPIKey", received.getApikey()).apply();
                        }
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    // failed
                }
            });
        } // else failed
    }

    private User getNextInactiveUser(String exclude) {
        for(User user : DBHelper.getInstance(this).getUsers()) {
            if(!user.getID().equals(settings.getString("userID", "")) && !user.getID().equals(exclude)) {
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
            case SettingsMainFragment.PERMISSION_LOCATION_DAYNIGHT:
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
