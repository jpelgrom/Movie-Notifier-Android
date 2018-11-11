package nl.jpelgrm.movienotifier.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.Trigger;

import java.io.IOException;
import java.util.List;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.models.Cinema;
import retrofit2.Call;
import retrofit2.Response;

public class CinemaUpdateJob extends JobService {
    private boolean finished = false;
    private Call<List<Cinema>> update;

    public final static String BROADCAST_COMPLETE = BuildConfig.APPLICATION_ID + ".CinemasListUpdated";

    @Override
    public boolean onStartJob(final JobParameters job) {
        AppDatabase db = AppDatabase.getInstance(this);

        // Get up to date internet data
        update = APIHelper.getInstance().getCinemas();
        AsyncTask.execute(() -> {
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

                    SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
                    settings.edit().putLong("cinemasUpdated", System.currentTimeMillis()).apply();

                    // Finally: check existing user preference for default cinema, and reset if necessary
                    if(db.cinemas().getCinemaById(settings.getString("prefDefaultCinema", "")) == null) {
                        settings.edit().putString("prefDefaultCinema", "").apply();
                    }

                    // Notify, UI might need to update
                    LocalBroadcastManager.getInstance(CinemaUpdateJob.this).sendBroadcast(new Intent(BROADCAST_COMPLETE));

                    finished = true;
                    jobFinished(job, false);
                } else {
                    finished = true;
                    jobFinished(job, true);
                }
            } catch(IOException | RuntimeException e) {
                finished = true;
                jobFinished(job, true);
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        if(!finished && update != null) {
            update.cancel();
        }

        return !finished;
    }

    public static Job getJobToUpdateImmediately(FirebaseJobDispatcher dispatcher) {
        return dispatcher.newJobBuilder()
                .setService(CinemaUpdateJob.class)
                .setTag("cinemasListSetup")
                .setRecurring(false)
                .setTrigger(Trigger.executionWindow(0, 0))
                .setReplaceCurrent(false)
                .build();
    }
}
