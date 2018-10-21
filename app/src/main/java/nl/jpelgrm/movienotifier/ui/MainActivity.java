package nl.jpelgrm.movienotifier.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.ui.settings.SettingsActivity;
import nl.jpelgrm.movienotifier.ui.view.FilterBottomSheet;
import nl.jpelgrm.movienotifier.ui.view.SortBottomSheet;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.toolbar) Toolbar toolbar;

    int dayNightPreference;
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
}
