package nl.jpelgrm.movienotifier.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.models.Cinema;
import retrofit2.Call;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

public class CinemaUpdateWorker extends Worker {
    private boolean finished = false;
    private Call<List<Cinema>> update;

    public final static String BROADCAST_COMPLETE = BuildConfig.APPLICATION_ID + ".CinemasListUpdated";

    public CinemaUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        update = APIHelper.getInstance().getCinemas();

        try {
            Response<List<Cinema>> response = update.execute();
            if(response.code() == 200) {
                List<Cinema> results = response.body();
                if(results != null) {
                    for(Cinema online : results) {
                        db.cinemas().add(online); // Database will handle add vs. update
                    }

                    List<Cinema> existing = db.cinemas().getCinemasSynchronous();
                    for(Cinema exists : existing) {
                        if(!results.contains(exists)) {
                            // If something is in the database, but not the online (up-to-date) list, it should be removed
                            db.cinemas().delete(exists);
                        }
                    }
                }

                SharedPreferences settings = getApplicationContext().getSharedPreferences("settings", MODE_PRIVATE);
                settings.edit().putLong("cinemasUpdated", System.currentTimeMillis()).apply();

                // Finally: check existing user preference for default cinema, and reset if necessary
                if(db.cinemas().getCinemaById(settings.getInt("prefSelectedCinema", 0)) == null) {
                    settings.edit().putInt("prefSelectedCinema", 0).apply();
                }

                // Notify, UI might need to update
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(BROADCAST_COMPLETE));

                finished = true;
                return Result.success();
            } else {
                finished = true;
                return Result.retry();
            }
        } catch(IOException | RuntimeException e) {
            return Result.retry();
        }
    }

    @Override
    public void onStopped() {
        if(!finished && update != null) {
            update.cancel();
        }

        super.onStopped();
    }

    public static OneTimeWorkRequest getRequestToUpdateImmediately() {
        // https://developer.android.com/topic/libraries/architecture/workmanager/basics#workflow
        // "In most cases, (...) WorkManager runs your task right away"
        return new OneTimeWorkRequest.Builder(CinemaUpdateWorker.class)
                .addTag("cinemasListSetup")
                .build();
    }
}
