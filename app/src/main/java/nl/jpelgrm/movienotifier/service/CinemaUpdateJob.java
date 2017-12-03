package nl.jpelgrm.movienotifier.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.Trigger;

import java.util.List;

import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.DBHelper;
import nl.jpelgrm.movienotifier.models.Cinema;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CinemaUpdateJob extends JobService {
    private boolean finished = false;
    private Call<List<Cinema>> update;

    public final static String BROADCAST_COMPLETE = BuildConfig.APPLICATION_ID + ".CinemasListUpdated";

    @Override
    public boolean onStartJob(final JobParameters job) {
        final DBHelper db = DBHelper.getInstance(this);

        // Get up to date internet data
        update = APIHelper.getInstance().getCinemas();
        update.enqueue(new Callback<List<Cinema>>() {
            @Override
            public void onResponse(Call<List<Cinema>> call, Response<List<Cinema>> response) {
                if(response.code() == 200) {
                    List<Cinema> results = response.body();
                    if(results != null) {
                        for(Cinema online : results) {
                            db.addCinema(online); // Database will handle add vs. update
                        }

                        List<Cinema> existing = db.getCinemas();
                        for(Cinema exists : existing) {
                            if(!results.contains(exists)) {
                                // If something is in the database, but not the online (up-to-date) list, it should be removed
                                db.deleteCinema(exists.getID());
                            }
                        }
                    }

                    SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
                    settings.edit().putLong("cinemasUpdated", System.currentTimeMillis()).apply();

                    // Finally: check existing user preference for default cinema, and reset if necessary
                    if(db.getCinemaByID(settings.getString("prefDefaultCinema", "")) == null) {
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
            }

            @Override
            public void onFailure(Call<List<Cinema>> call, Throwable t) {
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
