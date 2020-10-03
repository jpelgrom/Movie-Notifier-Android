package nl.jpelgrm.movienotifier.util

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import nl.jpelgrm.movienotifier.R
import nl.jpelgrm.movienotifier.models.User

object NotificationUtil {
    const val NOTIFICATION_CHANNEL_PREFIX = "notification."
    const val NOTIFICATION_CHANNEL_WATCHERS_PUSH = ".watchers.push"
    @JvmStatic
    fun createChannelWatchersPush(context: Context, user: User) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createUserGroup(context, user)
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_PREFIX + user.id + NOTIFICATION_CHANNEL_WATCHERS_PUSH,
                    context.getString(R.string.notifications_channel_watchers_push_title),
                    NotificationManager.IMPORTANCE_HIGH) // HIGH is 'Urgent' for users and DEFAULT is 'High' for users
            channel.description = context.getString(R.string.notifications_channel_watchers_push_description)
            channel.enableLights(true)
            channel.lightColor = ContextCompat.getColor(context, R.color.colorPrimary)
            channel.enableVibration(true)
            channel.setShowBadge(true)
            channel.group = NOTIFICATION_CHANNEL_PREFIX + user.id
            manager.createNotificationChannel(channel)
        }
    }

    @JvmStatic
    fun createUserGroup(context: Context, user: User) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannelGroup(NotificationChannelGroup(NOTIFICATION_CHANNEL_PREFIX + user.id, user.name))
        }
    }

    @JvmStatic
    fun cleanupPreferencesForUser(context: Context, userId: String) {
        val notificationSettings = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
        notificationSettings.edit()
                .remove("disabled-$userId")
                .remove("headsup-$userId")
                .remove("sound-$userId")
                .remove("vibrate-$userId")
                .remove("lights-$userId")
                .apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannelGroup(NOTIFICATION_CHANNEL_PREFIX + userId)
            manager.deleteNotificationChannel(NOTIFICATION_CHANNEL_PREFIX + userId + NOTIFICATION_CHANNEL_WATCHERS_PUSH)
        }
    }
}