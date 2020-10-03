package nl.jpelgrm.movienotifier.models

import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

@Entity(tableName = "Cinemas")
data class Cinema(
        @Expose @PrimaryKey @ColumnInfo(name = "ID") val id: Int = 0,
        @Expose @ColumnInfo(name = "Name") val name: String? = null,
        @Expose @ColumnInfo(name = "Lat") val lat: Double? = null,
        @Expose @ColumnInfo(name = "Lon") val lon: Double? = null
) {
    override fun toString(): String = name!!
}

@Entity(tableName = "Notifications",
        foreignKeys = [
            ForeignKey(entity = User::class,
                    parentColumns = ["ID"],
                    childColumns = ["UserID"],
                    onUpdate = ForeignKey.CASCADE,
                    onDelete = ForeignKey.CASCADE)
        ])
data class Notification(
        @PrimaryKey @ColumnInfo(name = "ID") val id: String = UUID.randomUUID().toString(),
        @ColumnInfo(name = "Time") val time: Long = 0L,
        @ColumnInfo(name = "UserID", index = true) val userid: String = "",
        @ColumnInfo(name = "WatcherID", index = true) val watcherid: String? = null,
        @ColumnInfo(name = "WatcherName") val watchername: String? = null,
        @ColumnInfo(name = "WatcherMovieID") val watchermovieid: Int = 0,
        @ColumnInfo(name = "Matches") val matches: Int = 0,
        @ColumnInfo(name = "Body") val body: String? = null
)

@Entity(tableName = "Users")
data class User(
        @Expose(serialize = false) @PrimaryKey @ColumnInfo(name = "ID") var id: String = "",
        @Expose @ColumnInfo(name = "Name") var name: String? = null,
        @Expose @ColumnInfo(name = "Email") var email: String? = null,
        @Expose @Ignore var password: String? = null,
        @Expose @SerializedName("fcm-registration-tokens") @ColumnInfo(name = "FCMTokens") var fcmTokens: List<String>? = null,
        @Expose(serialize = false) @ColumnInfo(name = "APIKey") var apikey: String? = null
)