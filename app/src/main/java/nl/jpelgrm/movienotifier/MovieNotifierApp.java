package nl.jpelgrm.movienotifier;

import android.app.Application;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.provider.FontRequest;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import nl.jpelgrm.movienotifier.service.CinemaUpdateWorker;

public class MovieNotifierApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setDefaultNightMode(getSharedPreferences("settings", MODE_PRIVATE)
                .getInt("prefDarkTheme", Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY));

        FontRequest fontRequest = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs);
        EmojiCompat.Config config = new FontRequestEmojiCompatConfig(this, fontRequest);
        EmojiCompat.init(config);

        setupCinemaListUpdates();
    }

    private void setupCinemaListUpdates() {
        long lastUpdated = getSharedPreferences("settings", MODE_PRIVATE).getLong("cinemasUpdated", -1);

        if(lastUpdated < (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))) {
            // It's been more than a week since the last update, so setup/reset job because it should have run by now
            Constraints updateConstraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
            PeriodicWorkRequest updateWork = new PeriodicWorkRequest.Builder(CinemaUpdateWorker.class, 7, TimeUnit.DAYS)
                    .setConstraints(updateConstraints)
                    .addTag("cinemasListUpdate")
                    .build();
            WorkManager.getInstance(this).cancelAllWorkByTag("cinemasListUpdate"); // 'Replace' any existing jobs
            WorkManager.getInstance(this).enqueue(updateWork);
        }

        // Also run immediately if the list has never been updated
        if(lastUpdated == -1) {
            WorkManager.getInstance(this).enqueue(CinemaUpdateWorker.getRequestToUpdateImmediately());
        }
    }
}
