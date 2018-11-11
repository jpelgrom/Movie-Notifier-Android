package nl.jpelgrm.movienotifier.models;

import com.google.gson.annotations.Expose;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "Cinemas")
public class Cinema {
    @Expose
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "ID")
    private String id= "";

    @Expose
    @ColumnInfo(name = "Name")
    private String name;

    @Expose
    @ColumnInfo(name = "Lat")
    private Double lat;

    @Expose
    @ColumnInfo(name = "Lon")
    private Double lon;

    @Ignore
    public Cinema() {
        // Default constructor
    }

    public Cinema(@NonNull String id, String name, Double lat, Double lon) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cinema cinema = (Cinema) o;

        if (!id.equals(cinema.id)) return false;
        if (!name.equals(cinema.name)) return false;
        if (lat != null ? !lat.equals(cinema.lat) : cinema.lat != null) return false;
        return lon != null ? lon.equals(cinema.lon) : cinema.lon == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (lat != null ? lat.hashCode() : 0);
        result = 31 * result + (lon != null ? lon.hashCode() : 0);
        return result;
    }
}
