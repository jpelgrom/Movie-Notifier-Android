package nl.jpelgrm.movienotifier.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.WorkManager;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.models.Notification;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.ui.MainActivity;
import nl.jpelgrm.movienotifier.ui.WatcherActivity;
import nl.jpelgrm.movienotifier.util.NotificationUtil;

public class MessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        WorkManager.getInstance(this).enqueue(FcmRefreshWorker.getRequestToUpdateImmediately(token, null));
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // Add to database
        String userID = remoteMessage.getData().get("user.id");
        String watcherID = remoteMessage.getData().get("watcher.id");
        String watcherName = remoteMessage.getData().get("watcher.name");
        String body = remoteMessage.getData().get("body");

        String sMovieID = remoteMessage.getData().get("watcher.movieid");
        String sMatches = remoteMessage.getData().get("matches.count");
        int movieID, matches;
        if(userID != null && sMovieID != null && sMatches != null) {
            movieID = Integer.parseInt(sMovieID);
            matches = Integer.parseInt(sMatches);

            List<User> dbUsers = AppDatabase.getInstance(getApplicationContext()).users().getUsersSynchronous();
            User foundUser = null;
            for(User dbUser: dbUsers) {
                if(dbUser.getId().equals(userID)) {
                    foundUser = dbUser;
                    break;
                }
            }
            if(foundUser != null) {
                Notification dbNotification = new Notification(remoteMessage.getSentTime(), userID, watcherID, watcherName, movieID, matches, body);
                AppDatabase.getInstance(getApplicationContext()).notifications().add(dbNotification);

                // Notify the user
                sendNotification(dbNotification, foundUser, dbUsers.size() > 1);
            }
        }
    }

    private void sendNotification(Notification notification, @NonNull User user, boolean showUser) {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager == null) {
            return;
        }

        NotificationUtil.createChannelWatchersPush(getApplicationContext(), user);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                NotificationUtil.NOTIFICATION_CHANNEL_PREFIX + user.getId() + NotificationUtil.NOTIFICATION_CHANNEL_WATCHERS_PUSH)
                .setSmallIcon(R.drawable.ic_movie)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentTitle(notification.getWatchername())
                .setAutoCancel(true)
                .setWhen(notification.getTime())
                .setShowWhen(true);

        builder.setContentText(getString(R.string.notifications_notification_matches, notification.getMatches()));
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(
                getString(R.string.notifications_notification_matchesandbody, notification.getMatches(), notification.getBody())
        ));

        SharedPreferences notificationSettings = getApplicationContext().getSharedPreferences("notifications", Context.MODE_PRIVATE);
        if(notificationSettings.getBoolean("headsup-" + user.getId(), true)) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }
        if(notificationSettings.getBoolean("sound-" + user.getId(), true)) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }
        if(notificationSettings.getBoolean("vibrate-" + user.getId(), true)) {
            builder.setVibrate(new long[]{0, 250, 250, 250}); // Android framework default
        }
        if(notificationSettings.getBoolean("lights-" + user.getId(), true)) {
            builder.setLights(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary), 1000, 9000);
        }

        Intent startIntent = new Intent(this, MainActivity.class);
        startIntent.putExtra("tab", "notifications");
        startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent startPendingIntent = PendingIntent.getActivity(this, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(startPendingIntent);

        if(showUser) { // Display for which account the notification is, if multiple
            builder.setSubText(user.getName());
        }

        Intent patheIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("patheapp://showMovie/" + notification.getWatchermovieid()));
        if(patheIntent.resolveActivity(getPackageManager()) != null) {
            PendingIntent pathePendingIntent = PendingIntent.getActivity(this, 0, patheIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.addAction(R.drawable.ic_exit_to_app, getString(R.string.notifications_notification_action_pathe), pathePendingIntent);
        }

        Intent watcherIntent = new Intent(this, WatcherActivity.class);
        watcherIntent.putExtra("id", notification.getWatcherid());
        PendingIntent watcherPendingIntent = PendingIntent.getActivity(this, 0, watcherIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.addAction(R.drawable.ic_visibility, getString(R.string.notifications_notification_action_view), watcherPendingIntent);

        manager.notify(notification.getId().hashCode(), builder.build());
    }
}
