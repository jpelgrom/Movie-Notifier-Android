package nl.jpelgrm.movienotifier.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.models.User;
import retrofit2.Call;
import retrofit2.Response;

public class FcmRefreshWorker extends Worker {
    private final static String DATA_NEW_TOKEN = "newToken";
    private final static String DATA_USER_TO_UPDATE = "userToUpdate";

    public FcmRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        SharedPreferences settings = getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);

        // 1. Update data for existing users before starting.
        String userToUpdate = getInputData().getString(DATA_USER_TO_UPDATE);
        List<User> users;
        if(userToUpdate == null) {
            users = db.users().getUsersSynchronous();
            for(User user: users) {
                Call<User> call = APIHelper.getInstance().getUser(user.getApikey(), user.getId());
                try {
                    Response<User> response = call.execute();
                    if(response.code() == 200) {
                        if(response.body() != null) {
                            db.users().update(response.body());
                        }
                    } else if(response.code() == 401) {
                        // Authentication failed, which cannot happen unless the user has been deleted, so make sure to delete it here as well
                        db.users().delete(user);

                        if(settings.getString("userID", "").equals(user.getId())) {
                            settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
                        }
                    } // else other error, possibly server error that we cannot handle, try to continue
                } catch(IOException | RuntimeException e) {
                    return Result.retry();
                }
            }
        } // else specific user, data is already up to date

        // 2. Add the new token to the user (if push not disabled) and send it to the server to receive notifications.
        // This worker can be queued if a token has been changed for an existing device, so we should check old as well.
        SharedPreferences notificationSettings = getApplicationContext().getSharedPreferences("notifications", Context.MODE_PRIVATE);
        String oldToken = notificationSettings.getString("token", "");
        String newToken = getInputData().getString(DATA_NEW_TOKEN);
        if(userToUpdate == null) {
            users = db.users().getUsersSynchronous();
        } else {
            users = Collections.singletonList(db.users().getUserByIdSynchronous(userToUpdate));
        }

        for(User user: users) {
            List<String> userFcmTokens = user.getFcmTokens();
            boolean hasOldToken = userFcmTokens.contains(oldToken);
            boolean hasNewToken = userFcmTokens.contains(newToken);
            boolean disabledNotifications = notificationSettings.getBoolean("disabled-" + user.getId(), false);
            boolean changed = false;
            if(!oldToken.equals("") && hasOldToken && !oldToken.equals(newToken)) {
                changed = userFcmTokens.remove(oldToken);
            }
            if(!disabledNotifications && !hasNewToken) {
                changed = userFcmTokens.add(newToken); // always true
            }

            if(changed) {
                user.setFcmTokens(userFcmTokens);
                Call<User> call = APIHelper.getInstance().updateUser(user.getApikey(), user.getId(), user);
                try {
                    Response<User> response = call.execute();
                    if(response.code() == 200) {
                        if(response.body() != null) {
                            db.users().update(response.body());
                        }
                    } else { // Problem we can't handle, let's hope it doesn't happen in the future
                        return Result.retry();
                    }
                } catch(IOException | RuntimeException e) {
                    return Result.retry();
                }
            }
        }

        // 3. Store token for future reference
        notificationSettings.edit().putString("token", newToken).apply();
        return Result.success();
    }

    public static OneTimeWorkRequest getRequestToUpdateImmediately(String newToken, @Nullable String userId) {
        // https://developer.android.com/topic/libraries/architecture/workmanager/basics#workflow
        // "In most cases, (...) WorkManager runs your task right away"
        Data workData = new Data.Builder()
                .putString(DATA_NEW_TOKEN, newToken)
                .putString(DATA_USER_TO_UPDATE, userId)
                .build();
        Constraints workConstraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        return new OneTimeWorkRequest.Builder(FcmRefreshWorker.class)
                .setInputData(workData)
                .setConstraints(workConstraints)
                .addTag("fcmRefresh")
                .build();
    }
}
