package nl.jpelgrm.movienotifier.service;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.work.WorkManager;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.models.Notification;

public class MessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        WorkManager.getInstance().enqueue(FcmRefreshWorker.getRequestToUpdateImmediately(token));
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if(remoteMessage.getData() != null) {
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

                Notification dbNotification = new Notification(remoteMessage.getSentTime(), userID, watcherID, watcherName, movieID, matches, body);
                AppDatabase.getInstance(getApplicationContext()).notifications().add(dbNotification);

                // Notify the user
                sendNotification(dbNotification);
            }
        }
    }

    private void sendNotification(Notification notification) {
        // TODO
    }
}
