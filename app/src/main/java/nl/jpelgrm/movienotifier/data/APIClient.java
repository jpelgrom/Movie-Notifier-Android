package nl.jpelgrm.movienotifier.data;

import java.util.List;

import nl.jpelgrm.movienotifier.models.NotificationType;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.models.UserLogin;
import nl.jpelgrm.movienotifier.models.Watcher;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface APIClient {
    // User
    @PUT("/user")
    Call<User> addUser(@Body User user);

    @POST("/login")
    Call<User> login(@Body UserLogin user);

    @GET("/user/{userid}")
    Call<User> getUser(@Header("APIKEY") String apikey, @Path("userid") String userid);

    @POST("/user/{userid}")
    Call<User> updateUser(@Header("APIKEY") String apikey, @Path("userid") String userid, @Body User user);

    @DELETE("/user/{userid}")
    Call<ResponseBody> deleteUser(@Header("APIKEY") String apikey, @Path("userid") String userid);


    // Watcher
    @GET("/watchers")
    Call<List<Watcher>> getWatchers(@Header("APIKEY") String apikey);

    @PUT("/watchers")
    Call<Watcher> addWatcher(@Header("APIKEY") String apikey, @Body Watcher watcher);

    @GET("/watchers/{watcherid}")
    Call<Watcher> getWatcher(@Header("APIKEY") String apikey, @Path("watcherid") String watcherid);

    @POST("/watchers/{watcherid}")
    Call<Watcher> updateWatcher(@Header("APIKEY") String apikey, @Path("watcherid") String watcherid, @Body Watcher watcher);

    @DELETE("/watchers/{watcherid}")
    Call<ResponseBody> deleteWatcher(@Header("APIKEY") String apikey, @Path("watcherid") String watcherid);


    // Notification type
    @GET("/notificationtypes")
    Call<List<NotificationType>> getNotificationTypes(@Header("APIKEY") String apikey);

    @GET("/notificationtype/{notificationtypekey}")
    Call<NotificationType> getNotificationType(@Header("APIKEY") String apikey, @Path("notificationtypekey") String notificationtypekey);
}
