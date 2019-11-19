package nl.jpelgrm.movienotifier.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.databinding.ActivityMainBinding;
import nl.jpelgrm.movienotifier.ui.settings.SettingsActivity;
import nl.jpelgrm.movienotifier.ui.view.FilterBottomSheet;
import nl.jpelgrm.movienotifier.ui.view.SortBottomSheet;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    SharedPreferences settings;

    public enum NavigationTab {
        WATCHERS, NOTIFICATIONS
    }
    NavigationTab selectedTab = NavigationTab.WATCHERS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
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

        binding.bottomnav.setOnNavigationItemSelectedListener(menuItem -> {
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
        binding.bottomnav.setOnNavigationItemReselectedListener(menuItem -> {
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
            if(getIntent() == null || getIntent().getExtras() == null || getIntent().getExtras().getString("tab") == null
                    || !getIntent().getExtras().getString("tab").equals("notifications")) {
                selectTab(NavigationTab.WATCHERS); // Default tab is watchers, execute action
            } else {
                binding.bottomnav.setSelectedItemId(R.id.navNotifications);
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
