package nl.jpelgrm.movienotifier;

import android.app.Application;
import android.os.Build;

import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;
import androidx.core.provider.FontRequest;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import nl.jpelgrm.movienotifier.service.CinemaUpdateWorker;
import nl.jpelgrm.movienotifier.util.StethoUtil;

public class MovieNotifierApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StethoUtil.install(this);

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
            WorkManager.getInstance().cancelAllWorkByTag("cinemasListUpdate"); // 'Replace' any existing jobs
            WorkManager.getInstance().enqueue(updateWork);
        }

        // Also run immediately if the list has never been updated
        if(lastUpdated == -1) {
            WorkManager.getInstance().enqueue(CinemaUpdateWorker.getRequestToUpdateImmediately());
        }
    }
}
