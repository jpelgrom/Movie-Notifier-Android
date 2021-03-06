package nl.jpelgrm.movienotifier.data

import androidx.lifecycle.LiveData
import androidx.room.*
import nl.jpelgrm.movienotifier.models.Cinema

@Dao
interface DaoCinemas {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(cinemas: List<Cinema>)

    @get:Query("SELECT * FROM Cinemas")
    val cinemas: LiveData<List<Cinema>>

    @get:Query("SELECT * FROM Cinemas")
    val cinemasSynchronous: List<Cinema>

    @Query("SELECT * FROM Cinemas WHERE ID = :id")
    fun getCinemaById(id: Int): Cinema?

    @Update
    fun update(cinema: Cinema)

    @Delete
    suspend fun delete(cinema: List<Cinema>)
}