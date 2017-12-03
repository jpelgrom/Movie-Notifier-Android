package nl.jpelgrm.movienotifier.models;

import android.content.ContentValues;

import com.google.gson.annotations.Expose;

public class Cinema {
    @Expose
    private String id;

    @Expose
    private String name;

    @Expose
    private Double lat;

    @Expose
    private Double lon;

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLatitude() {
        return lat;
    }

    public void setLatitude(Double lat) {
        this.lat = lat;
    }

    public Double getLongitude() {
        return lon;
    }

    public void setLongitude(Double lon) {
        this.lon = lon;
    }

    @Override
    public String toString() {
        return name;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put("ID", id);
        cv.put("Name", name);
        cv.put("Lat", lat);
        cv.put("Lon", lon);

        return cv;
    }

    public void setContent(ContentValues cv) {
        id = cv.getAsString("ID");
        name = cv.getAsString("Name");
        lat = cv.getAsDouble("Lat");
        lon = cv.getAsDouble("Lon");
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
