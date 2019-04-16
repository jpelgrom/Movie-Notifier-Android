package nl.jpelgrm.movienotifier.util;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.models.User;

public class NotificationUtil {
    public static final String NOTIFICATION_CHANNEL_PREFIX = "notification.";
    public static final String NOTIFICATION_CHANNEL_WATCHERS_PUSH = ".watchers.push";

    public static void createChannelWatchersPush(Context context, @NonNull User user) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager == null) {
                return;
            }
            createUserGroup(context, user);

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_PREFIX + user.getId() + NOTIFICATION_CHANNEL_WATCHERS_PUSH,
                    context.getString(R.string.notification_channel_watchers_push_title),
                    NotificationManager.IMPORTANCE_HIGH); // HIGH is 'Urgent' for users and DEFAULT is 'High' for users
            channel.setDescription(context.getString(R.string.notification_channel_watchers_push_description));
            channel.enableLights(true);
            channel.setLightColor(ContextCompat.getColor(context, R.color.colorPrimary));
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setGroup(NOTIFICATION_CHANNEL_PREFIX + user.getId());

            manager.createNotificationChannel(channel);
        }
    }

    public static void createUserGroup(Context context, User user) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager == null) {
                return;
            }
            manager.createNotificationChannelGroup(new NotificationChannelGroup(NOTIFICATION_CHANNEL_PREFIX + user.getId(), user.getName()));
        }
    }

    public static void cleanupPreferencesForUser(Context context, String userId) {
        SharedPreferences notificationSettings = context.getSharedPreferences("notifications", Context.MODE_PRIVATE);
        notificationSettings.edit()
                .remove("disabled-" + userId)
                .remove("headsup-" + userId)
                .remove("sound-" + userId)
                .remove("vibrate-" + userId)
                .remove("light-" + userId)
                .apply();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager == null) {
                return;
            }
            manager.deleteNotificationChannelGroup(NOTIFICATION_CHANNEL_PREFIX + userId);
            manager.deleteNotificationChannel(NOTIFICATION_CHANNEL_PREFIX + userId + NOTIFICATION_CHANNEL_WATCHERS_PUSH);
        }
    }
}
