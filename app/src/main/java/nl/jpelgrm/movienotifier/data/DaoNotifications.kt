package nl.jpelgrm.movienotifier.data

import androidx.paging.DataSource
import androidx.room.*
import nl.jpelgrm.movienotifier.models.Notification

@Dao
interface DaoNotifications {
    @Insert
    fun add(notification: Notification?)

    @Query("SELECT * FROM Notifications WHERE UserID = :userid ORDER BY time DESC")
    fun getNotificationsForUser(userid: String): DataSource.Factory<Int, Notification>

    @Query("SELECT * FROM Notifications WHERE WatcherID = :watcherid")
    fun getNotificationsForWatcher(watcherid: String): List<Notification>

    @Update
    fun update(notification: Notification)

    @Delete
    fun delete(notification: Notification)
}