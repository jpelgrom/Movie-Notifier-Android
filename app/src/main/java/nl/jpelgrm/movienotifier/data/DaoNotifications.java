package nl.jpelgrm.movienotifier.data;

import java.util.List;

import androidx.lifecycle.LiveData;
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

    @Query("SELECT * FROM Notifications WHERE UserID = :userid")
    LiveData<List<Notification>> getNotificationsForUser(String userid);

    @Update
    void update(Notification notification);

    @Delete
    void delete(Notification notification);
}
