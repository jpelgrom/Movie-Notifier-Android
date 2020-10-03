package nl.jpelgrm.movienotifier.data

import nl.jpelgrm.movienotifier.models.Cinema
import nl.jpelgrm.movienotifier.models.User
import nl.jpelgrm.movienotifier.models.UserLogin
import nl.jpelgrm.movienotifier.models.Watcher
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface APIClient {
    // User
    @PUT("/user")
    fun addUser(@Body user: User): Call<User>

    @POST("/login")
    fun login(@Body user: UserLogin): Call<User>

    @GET("/user/{userid}")
    fun getUser(@Header("APIKEY") apikey: String, @Path("userid") userid: String): Call<User>

    @POST("/user/{userid}")
    fun updateUser(@Header("APIKEY") apikey: String, @Path("userid") userid: String, @Body user: User): Call<User>

    @DELETE("/user/{userid}")
    fun deleteUser(@Header("APIKEY") apikey: String, @Path("userid") userid: String): Call<ResponseBody>

    // Watcher
    @GET("/watchers")
    fun getWatchers(@Header("APIKEY") apikey: String): Call<List<Watcher>>

    @PUT("/watchers")
    fun addWatcher(@Header("APIKEY") apikey: String, @Body watcher: Watcher): Call<Watcher>

    @GET("/watchers/{watcherid}")
    fun getWatcher(@Header("APIKEY") apikey: String, @Path("watcherid") watcherid: String): Call<Watcher>

    @POST("/watchers/{watcherid}")
    fun updateWatcher(@Header("APIKEY") apikey: String, @Path("watcherid") watcherid: String, @Body watcher: Watcher): Call<Watcher>

    @DELETE("/watchers/{watcherid}")
    fun deleteWatcher(@Header("APIKEY") apikey: String, @Path("watcherid") watcherid: String): Call<ResponseBody>

    // Cinemas
    @GET("/cinemas")
    suspend fun getCinemas(): List<Cinema>
}