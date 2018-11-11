package nl.jpelgrm.movienotifier.data;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import nl.jpelgrm.movienotifier.models.User;

@Dao
public interface DaoUsers {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(User user);

    @Query("SELECT * FROM Users")
    LiveData<List<User>> getUsers();

    @Query("SELECT * FROM Users")
    List<User> getUsersSynchronous();

    @Query("SELECT * FROM Users WHERE ID = :id")
    LiveData<User> getUserById(String id);

    @Update
    void update(User user);

    @Delete
    void delete(User user);
}
