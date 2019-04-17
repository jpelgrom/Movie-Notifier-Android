package nl.jpelgrm.movienotifier.data;

import java.util.List;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import nl.jpelgrm.movienotifier.models.Notification;
@Dao
public interface DaoNotifications {
    @Insert
    void add(Notification notification);

    @Query("SELECT * FROM Notifications WHERE UserID = :userid ORDER BY time DESC")
    DataSource.Factory<Integer, Notification> getNotificationsForUser(String userid);

    @Query("SELECT * FROM Notifications WHERE WatcherID = :watcherid")
    List<Notification> getNotificationsForWatcher(String watcherid);

    @Update
    void update(Notification notification);

    @Delete
    void delete(Notification notification);
}
