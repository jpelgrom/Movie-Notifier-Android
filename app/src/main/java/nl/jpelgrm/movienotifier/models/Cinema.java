package nl.jpelgrm.movienotifier.models;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
}
