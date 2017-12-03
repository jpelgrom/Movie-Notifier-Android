package nl.jpelgrm.movienotifier.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.service.CinemaUpdateJob;
import nl.jpelgrm.movienotifier.ui.settings.SettingsActivity;
import nl.jpelgrm.movienotifier.ui.view.FilterBottomSheet;
import nl.jpelgrm.movienotifier.ui.view.SortBottomSheet;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.toolbar) Toolbar toolbar;

    int dayNightPreference;
    boolean setupCinemaListUpdates = false;
    SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        dayNightPreference = getSharedPreferences("settings", MODE_PRIVATE).getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO);

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.frame, new WatchersFragment(), "watchersFragment").commit();
        }

        settings = getSharedPreferences("settings", MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(settings.getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO) != dayNightPreference) {
            recreate();
        }
        setupCinemaListUpdates();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    private void setupCinemaListUpdates() {
        if(setupCinemaListUpdates) { return; }

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job updateJob = dispatcher.newJobBuilder()
                .setService(CinemaUpdateJob.class)
                .setTag("cinemasListUpdate")
                .setRecurring(true)
                .setLifetime(Lifetime.FOREVER)
                .setTrigger(Trigger.executionWindow(0, 60 * 60 * 24 * 7 /* once a week */))
                .setReplaceCurrent(true)
                .build();
        dispatcher.mustSchedule(updateJob);

        // Also run immediately if the list has never been updated
        if(settings.getLong("cinemasUpdated", -1) == -1) {
            dispatcher.mustSchedule(CinemaUpdateJob.getJobToUpdateImmediately(dispatcher));
        }

        setupCinemaListUpdates = true;
    }
}
