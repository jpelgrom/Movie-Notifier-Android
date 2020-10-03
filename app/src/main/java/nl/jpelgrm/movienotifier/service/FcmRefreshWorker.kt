package nl.jpelgrm.movienotifier.service

import android.content.Context
import androidx.work.*
import nl.jpelgrm.movienotifier.data.APIHelper
import nl.jpelgrm.movienotifier.data.AppDatabase
import nl.jpelgrm.movienotifier.models.User
import nl.jpelgrm.movienotifier.util.NotificationUtil
import java.io.IOException

class FcmRefreshWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val settings = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

        // 1. Update data for existing users before starting.
        val userToUpdate = inputData.getString(DATA_USER_TO_UPDATE)
        var users: List<User>
        if (userToUpdate == null) {
            users = db.users().usersSynchronous
            for (user in users) {
                val call = APIHelper.instance.getUser(user.apikey, user.id)
                try {
                    val response = call.execute()
                    if (response.code() == 200) {
                        if (response.body() != null) {
                            db.users().update(response.body()!!)
                            if (user.name != response.body()!!.name) {
                                NotificationUtil.createUserGroup(applicationContext, response.body())
                            }
                        }
                    } else if (response.code() == 401) {
                        // Authentication failed, which cannot happen unless the user has been deleted, so make sure to delete it here as well
                        db.users().delete(user)
                        NotificationUtil.cleanupPreferencesForUser(applicationContext, user.id)
                        if (settings.getString("userID", "") == user.id) {
                            settings.edit().putString("userID", "").putString("userAPIKey", "").apply()
                        }
                    } // else other error, possibly server error that we cannot handle, try to continue
                } catch (e: IOException) {
                    return Result.retry()
                } catch (e: RuntimeException) {
                    return Result.retry()
                }
            }
        } // else specific user, data is already up to date

        // 2. Add the new token to the user (if push not disabled) and send it to the server to receive notifications.
        // This worker can be queued if a token has been changed for an existing device, so we should check old as well.
        val notificationSettings = applicationContext.getSharedPreferences("notifications", Context.MODE_PRIVATE)
        val oldToken = notificationSettings.getString("token", "")
        val newToken = inputData.getString(DATA_NEW_TOKEN)
        users = if (userToUpdate == null) {
            db.users().usersSynchronous
        } else {
            val dbUser = db.users().getUserByIdSynchronous(userToUpdate)
            if(dbUser != null) { listOf(dbUser) } else { emptyList() }
        }
        for (user in users) {
            val userFcmTokens = user.fcmTokens
            val hasOldToken = userFcmTokens.contains(oldToken)
            val hasNewToken = userFcmTokens.contains(newToken)
            val disabledNotifications = notificationSettings.getBoolean("disabled-" + user.id, false)
            var changed = false
            if (oldToken != "" && hasOldToken && oldToken != newToken) {
                changed = userFcmTokens.remove(oldToken)
            }
            if (!disabledNotifications && !hasNewToken) {
                changed = userFcmTokens.add(newToken) // always true
            }
            if (changed) {
                user.fcmTokens = userFcmTokens
                val call = APIHelper.instance.updateUser(user.apikey, user.id, user)
                try {
                    val response = call.execute()
                    if (response.code() == 200) {
                        if (response.body() != null) {
                            db.users().update(response.body()!!)
                        }
                    } else { // Problem we can't handle, let's hope it doesn't happen in the future
                        return Result.retry()
                    }
                } catch (e: IOException) {
                    return Result.retry()
                } catch (e: RuntimeException) {
                    return Result.retry()
                }
            }
        }

        // 3. Store token for future reference
        notificationSettings.edit().putString("token", newToken).apply()
        return Result.success()
    }

    companion object {
        private const val DATA_NEW_TOKEN = "newToken"
        private const val DATA_USER_TO_UPDATE = "userToUpdate"
        @JvmStatic
        fun getRequestToUpdateImmediately(newToken: String?, userId: String?): OneTimeWorkRequest {
            // https://developer.android.com/topic/libraries/architecture/workmanager/basics#workflow
            // "In most cases, (...) WorkManager runs your task right away"
            val workData = Data.Builder()
                    .putString(DATA_NEW_TOKEN, newToken)
                    .putString(DATA_USER_TO_UPDATE, userId)
                    .build()
            val workConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            return OneTimeWorkRequest.Builder(FcmRefreshWorker::class.java)
                    .setInputData(workData)
                    .setConstraints(workConstraints)
                    .addTag("fcmRefresh")
                    .build()
        }
    }
}