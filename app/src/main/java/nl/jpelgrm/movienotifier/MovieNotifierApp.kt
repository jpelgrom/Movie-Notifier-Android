package nl.jpelgrm.movienotifier

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.work.*
import nl.jpelgrm.movienotifier.service.CinemaUpdateWorker
import java.util.concurrent.TimeUnit

class MovieNotifierApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(getSharedPreferences("settings", MODE_PRIVATE)
                .getInt("prefDarkTheme", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY))
        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs)
        val config: EmojiCompat.Config = FontRequestEmojiCompatConfig(this, fontRequest)
        EmojiCompat.init(config)
        setupCinemaListUpdates()
    }

    private fun setupCinemaListUpdates() {
        val lastUpdated = getSharedPreferences("settings", MODE_PRIVATE).getLong("cinemasUpdated", -1)
        if (lastUpdated < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)) {
            // It's been more than a week since the last update, so setup/reset job because it should have run by now
            val updateConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val updateWork = PeriodicWorkRequestBuilder<CinemaUpdateWorker>(7, TimeUnit.DAYS)
                    .setConstraints(updateConstraints)
                    .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "cinemasListUpdate",
                    ExistingPeriodicWorkPolicy.KEEP,
                    updateWork
            )
        }

        // Also run immediately if the list has never been updated
        if (lastUpdated == -1L) {
            WorkManager.getInstance(this).enqueue(CinemaUpdateWorker.requestToUpdateImmediately)
        }
    }
}