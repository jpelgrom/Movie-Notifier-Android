package nl.jpelgrm.movienotifier.service

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import kotlinx.coroutines.coroutineScope
import nl.jpelgrm.movienotifier.BuildConfig
import nl.jpelgrm.movienotifier.data.APIHelper
import nl.jpelgrm.movienotifier.data.AppDatabase
import java.io.IOException

class CinemaUpdateWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val db = AppDatabase.getInstance(applicationContext)
        try {
            val results = APIHelper.instance.getCinemas()
            if (results.isNotEmpty()) {
                db.cinemas().add(results) // Database will handle add vs. update

                val existing = db.cinemas().cinemasSynchronous
                val onlyLocal = existing.filterNot { cinema -> results.contains(cinema) }
                db.cinemas().delete(onlyLocal) // If something is in the database, but not the online (up-to-date) list, it should be removed
            }
            val settings = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            settings.edit().putLong("cinemasUpdated", System.currentTimeMillis()).apply()

            // Finally: check existing user preference for default cinema, and reset if necessary
            if (db.cinemas().getCinemaById(settings.getInt("prefSelectedCinema", 0)) == null) {
                settings.edit().putInt("prefSelectedCinema", 0).apply()
            }

            // Notify, UI might need to update
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(BROADCAST_COMPLETE))
            Result.success()
        } catch (e: IOException) { // Both network errors and non-200 HTTP statuses
            Result.retry()
        } catch (e: RuntimeException) {
            Result.retry()
        }
    }

    companion object {
        const val BROADCAST_COMPLETE = BuildConfig.APPLICATION_ID + ".CinemasListUpdated"

        @JvmStatic
        val requestToUpdateImmediately: OneTimeWorkRequest
            get() {
                // https://developer.android.com/topic/libraries/architecture/workmanager/basics#workflow
                // "In most cases, (...) WorkManager runs your task right away"
                val workConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                return OneTimeWorkRequest.Builder(CinemaUpdateWorker::class.java)
                        .setConstraints(workConstraints)
                        .addTag("cinemasListSetup")
                        .build()
            }
    }
}