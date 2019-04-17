package nl.jpelgrm.movienotifier.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.ui.settings.SettingsActivity;
import nl.jpelgrm.movienotifier.ui.view.FilterBottomSheet;
import nl.jpelgrm.movienotifier.ui.view.SortBottomSheet;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.bottomnav) BottomNavigationView bottomnav;

    int dayNightPreference;
    SharedPreferences settings;

    public enum NavigationTab {
        WATCHERS, NOTIFICATIONS
    }
    NavigationTab selectedTab = NavigationTab.WATCHERS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        dayNightPreference = getSharedPreferences("settings", MODE_PRIVATE).getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO);

        bottomnav.setOnNavigationItemSelectedListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case R.id.navWatchers:
                    selectTab(NavigationTab.WATCHERS);
                    return true;
                case R.id.navNotifications:
                    selectTab(NavigationTab.NOTIFICATIONS);
                    return true;
                default:
                    return false;
            }
        });
        bottomnav.setOnNavigationItemReselectedListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case R.id.navWatchers:
                    if(getSupportFragmentManager().findFragmentByTag("watchersFragment") != null) {
                        ((WatchersFragment) getSupportFragmentManager().findFragmentByTag("watchersFragment")).scrollListToTop();
                    }
                    break;
                case R.id.navNotifications:
                    if(getSupportFragmentManager().findFragmentByTag("notificationsFragment") != null) {
                        ((NotificationsFragment) getSupportFragmentManager().findFragmentByTag("notificationsFragment")).scrollListToTop();
                    }
                    break;
            }
        });

        if(savedInstanceState == null) {
            if(getIntent() == null || getIntent().getExtras() == null
                    || getIntent().getExtras().getSerializable("tab") != NavigationTab.NOTIFICATIONS) {
                selectTab(NavigationTab.WATCHERS); // Default tab is watchers, execute action
            } else {
                bottomnav.setSelectedItemId(R.id.navNotifications);
            }
        }

        settings = getSharedPreferences("settings", MODE_PRIVATE);
    }

    private void selectTab(NavigationTab tab) {
        Fragment fragment = null;
        String tag = null;
        switch(tab) {
            case WATCHERS:
                fragment = new WatchersFragment();
                tag = "watchersFragment";
                break;
            case NOTIFICATIONS:
                fragment = new NotificationsFragment();
                tag = "notificationsFragment";
                break;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.frame, fragment, tag)
                .commit();
        selectedTab = tab;
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(settings.getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO) != dayNightPreference) {
            recreate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if(selectedTab != NavigationTab.WATCHERS) {
            menu.findItem(R.id.action_sort).setVisible(false);
            menu.findItem(R.id.action_filter).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_sort:
                SortBottomSheet sortSheet = new SortBottomSheet();
                sortSheet.show(getSupportFragmentManager(), sortSheet.getTag());
                return true;
            case R.id.action_filter:
                FilterBottomSheet filterSheet = new FilterBottomSheet();
                filterSheet.show(getSupportFragmentManager(), filterSheet.getTag());
                return true;
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case WatchersFragment.PERMISSION_LOCATION_AUTOMAGIC:
                if(getSupportFragmentManager().findFragmentByTag("watchersFragment") != null) {
                    getSupportFragmentManager().findFragmentByTag("watchersFragment").onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}
