package nl.jpelgrm.movienotifier.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import nl.jpelgrm.movienotifier.R
import nl.jpelgrm.movienotifier.data.AppDatabase
import nl.jpelgrm.movienotifier.models.Notification
import nl.jpelgrm.movienotifier.models.User
import nl.jpelgrm.movienotifier.ui.MainActivity
import nl.jpelgrm.movienotifier.ui.WatcherActivity
import nl.jpelgrm.movienotifier.util.NotificationUtil

class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        WorkManager.getInstance(this).enqueue(FcmRefreshWorker.getRequestToUpdateImmediately(token, null))
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Add to database
        val userID = remoteMessage.data["user.id"]
        val watcherID = remoteMessage.data["watcher.id"]
        val watcherName = remoteMessage.data["watcher.name"]
        val body = remoteMessage.data["body"]
        val sMovieID = remoteMessage.data["watcher.movieid"]
        val sMatches = remoteMessage.data["matches.count"]
        if (userID != null && sMovieID != null && sMatches != null) {
            val db = AppDatabase.getInstance(applicationContext)
            val movieID = sMovieID.toInt()
            val matches = sMatches.toInt()
            val dbUsers = db.users().usersSynchronous
            var foundUser: User? = null
            for (dbUser in dbUsers) {
                if (dbUser.id == userID) {
                    foundUser = dbUser
                    break
                }
            }
            if (foundUser != null) {
                val dbNotification = Notification(remoteMessage.sentTime, userID, watcherID, watcherName, movieID, matches, body)
                db.notifications().add(dbNotification)

                // Notify the user
                sendNotification(dbNotification, foundUser, dbUsers.size > 1)
            }
        }
    }

    private fun sendNotification(notification: Notification, user: User, showUser: Boolean) {
        val manager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        NotificationUtil.createChannelWatchersPush(applicationContext, user)
        val builder = NotificationCompat.Builder(applicationContext,
                NotificationUtil.NOTIFICATION_CHANNEL_PREFIX + user.id + NotificationUtil.NOTIFICATION_CHANNEL_WATCHERS_PUSH)
                .setSmallIcon(R.drawable.ic_movie)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentTitle(notification.watchername)
                .setAutoCancel(true)
                .setWhen(notification.time)
                .setShowWhen(true)
        builder.setContentText(getString(R.string.notifications_notification_matches, notification.matches))
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(
                getString(R.string.notifications_notification_matchesandbody, notification.matches, notification.body)
        ))
        val notificationSettings = applicationContext.getSharedPreferences("notifications", MODE_PRIVATE)
        if (notificationSettings.getBoolean("headsup-" + user.id, true)) {
            builder.priority = NotificationCompat.PRIORITY_HIGH
        }
        if (notificationSettings.getBoolean("sound-" + user.id, true)) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }
        if (notificationSettings.getBoolean("vibrate-" + user.id, true)) {
            builder.setVibrate(longArrayOf(0, 250, 250, 250)) // Android framework default
        }
        if (notificationSettings.getBoolean("lights-" + user.id, true)) {
            builder.setLights(ContextCompat.getColor(applicationContext, R.color.colorPrimary), 1000, 9000)
        }
        val startIntent = Intent(this, MainActivity::class.java)
        startIntent.putExtra("tab", "notifications")
        startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val startPendingIntent = PendingIntent.getActivity(this, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(startPendingIntent)
        if (showUser) { // Display for which account the notification is, if multiple
            builder.setSubText(user.name)
        }
        val patheIntent = Intent(Intent.ACTION_VIEW, Uri.parse("patheapp://showMovie/" + notification.watchermovieid))
        if (patheIntent.resolveActivity(packageManager) != null) {
            val pathePendingIntent = PendingIntent.getActivity(this, 0, patheIntent, PendingIntent.FLAG_CANCEL_CURRENT)
            builder.addAction(R.drawable.ic_exit_to_app, getString(R.string.notifications_notification_action_pathe), pathePendingIntent)
        }
        val watcherIntent = Intent(this, WatcherActivity::class.java)
        watcherIntent.putExtra("id", notification.watcherid)
        val watcherPendingIntent = PendingIntent.getActivity(this, 0, watcherIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        builder.addAction(R.drawable.ic_visibility, getString(R.string.notifications_notification_action_view), watcherPendingIntent)
        manager.notify(notification.id.hashCode(), builder.build())
    }
}