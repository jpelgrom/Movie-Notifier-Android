package nl.jpelgrm.movienotifier.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.content.ContextCompat;
import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.R;

public class NotificationUtil {
    public static final String NOTIFICATION_CHANNEL_WATCHERS_GENERAL = BuildConfig.APPLICATION_ID + ".notification.channel.watchers.general";

    public static void createChannelWatchersGeneral(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager == null) {
                return;
            }

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_WATCHERS_GENERAL,
                    context.getString(R.string.notification_channel_watchers_general_title),
                    NotificationManager.IMPORTANCE_HIGH); // HIGH is 'Urgent' for users and DEFAULT is 'High' for users
            channel.setDescription(context.getString(R.string.notification_channel_watchers_general_description));
            channel.enableLights(true);
            channel.setLightColor(ContextCompat.getColor(context, R.color.colorPrimary));
            channel.enableVibration(true);
            channel.setShowBadge(true);

            manager.createNotificationChannel(channel);
        }
    }
}
