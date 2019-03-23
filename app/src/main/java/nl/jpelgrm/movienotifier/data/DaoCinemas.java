package nl.jpelgrm.movienotifier.data;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import nl.jpelgrm.movienotifier.models.Cinema;

@Dao
public interface DaoCinemas {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(Cinema cinema);

    @Query("SELECT * FROM Cinemas")
    LiveData<List<Cinema>> getCinemas();

    @Query("SELECT * FROM Cinemas")
    List<Cinema> getCinemasSynchronous();

    @Query("SELECT * FROM Cinemas WHERE ID = :id")
    Cinema getCinemaById(int id);

    @Update
    void update(Cinema cinema);

    @Delete
    void delete(Cinema cinema);
}
