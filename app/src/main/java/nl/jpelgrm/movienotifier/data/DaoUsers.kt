package nl.jpelgrm.movienotifier.data

import androidx.lifecycle.LiveData
import androidx.room.*
import nl.jpelgrm.movienotifier.models.User

@Dao
interface DaoUsers {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(user: User)

    @get:Query("SELECT * FROM Users")
    val users: LiveData<List<User>>

    @get:Query("SELECT * FROM Users")
    val usersSynchronous: List<User>

    @Query("SELECT * FROM Users WHERE ID = :id")
    fun getUserById(id: String): LiveData<User>

    @Query("SELECT * FROM Users WHERE ID = :id")
    fun getUserByIdSynchronous(id: String): User

    @Update
    fun update(user: User)

    @Delete
    fun delete(user: User)
}