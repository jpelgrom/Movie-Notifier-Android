package nl.jpelgrm.movienotifier.models;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "Notifications",
    foreignKeys = @ForeignKey(entity = User.class,
        parentColumns = "ID",
        childColumns = "UserID",
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE))
public class Notification {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "ID")
    private String id = "";

    @ColumnInfo(name = "Time")
    private long time;

    @NonNull
    @ColumnInfo(name = "UserID", index = true)
    private String userid = "";

    @ColumnInfo(name = "WatcherID", index = true)
    private String watcherid;

    @ColumnInfo(name = "WatcherName")
    private String watchername;

    @ColumnInfo(name = "WatcherMovieID")
    private int watchermovieid;

    @ColumnInfo(name = "Matches")
    private int matches;

    @ColumnInfo(name = "Body")
    private String body;

    @Ignore
    public Notification(long time, @NonNull String userid, String watcherid, String watchername, int watchermovieid, int matches, String body) {
        this.id = UUID.randomUUID().toString();
        this.time = time;
        this.userid = userid;
        this.watcherid = watcherid;
        this.watchername = watchername;
        this.watchermovieid = watchermovieid;
        this.matches = matches;
        this.body = body;
    }

    public Notification(@NonNull String id, long time, @NonNull String userid, String watcherid, String watchername, int watchermovieid, int matches, String body) {
        this.id = id;
        this.time = time;
        this.userid = userid;
        this.watcherid = watcherid;
        this.watchername = watchername;
        this.watchermovieid = watchermovieid;
        this.matches = matches;
        this.body = body;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @NonNull
    public String getUserid() {
        return userid;
    }

    public void setUserid(@NonNull String userid) {
        this.userid = userid;
    }

    public String getWatcherid() {
        return watcherid;
    }

    public void setWatcherid(String watcherid) {
        this.watcherid = watcherid;
    }

    public String getWatchername() {
        return watchername;
    }

    public void setWatchername(String watchername) {
        this.watchername = watchername;
    }

    public int getWatchermovieid() {
        return watchermovieid;
    }

    public void setWatchermovieid(int watchermovieid) {
        this.watchermovieid = watchermovieid;
    }

    public int getMatches() {
        return matches;
    }

    public void setMatches(int matches) {
        this.matches = matches;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Notification that = (Notification) o;

        if (time != that.time) return false;
        if (watchermovieid != that.watchermovieid) return false;
        if (matches != that.matches) return false;
        if (!id.equals(that.id)) return false;
        if (!userid.equals(that.userid)) return false;
        if (watcherid != null ? !watcherid.equals(that.watcherid) : that.watcherid != null)
            return false;
        if (watchername != null ? !watchername.equals(that.watchername) : that.watchername != null)
            return false;
        return body != null ? body.equals(that.body) : that.body == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + userid.hashCode();
        result = 31 * result + (watcherid != null ? watcherid.hashCode() : 0);
        result = 31 * result + (watchername != null ? watchername.hashCode() : 0);
        result = 31 * result + watchermovieid;
        result = 31 * result + matches;
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
    }
}
