package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import butterknife.BindView;
import butterknife.ButterKnife;
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
    }

    public void showUser(String id) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.frame, SettingsAccountOverviewFragment.newInstance(id), "settingsAccountOverviewFragment")
                .addToBackStack(null)
                .commit();
    }

    public void editUserDetail(String id, SettingsAccountUpdateFragment.UpdateMode mode) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.frame, SettingsAccountUpdateFragment.newInstance(id, mode), "settingsAccountUpdateFragment")
                .addToBackStack(null)
                .commit();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
            case R.id.homeAsUp:
                super.onBackPressed();
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
