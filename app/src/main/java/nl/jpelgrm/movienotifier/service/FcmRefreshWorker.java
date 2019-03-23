package nl.jpelgrm.movienotifier.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class FcmRefreshWorker extends Worker {
    private final static String DATA_NEW_TOKEN = "newToken";

    public FcmRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // TODO:
        // - Loop through existing users, update data, remove existing token if present and if so add new token
        // - Store token for future use
        return null;
    }

    public static OneTimeWorkRequest getRequestToUpdateImmediately(String newToken) {
        // https://developer.android.com/topic/libraries/architecture/workmanager/basics#workflow
        // "In most cases, (...) WorkManager runs your task right away"
        Data workData = new Data.Builder().putString(DATA_NEW_TOKEN, newToken).build();
        return new OneTimeWorkRequest.Builder(FcmRefreshWorker.class)
                .setInputData(workData)
                .addTag("fcmRefresh")
                .build();
    }
}
