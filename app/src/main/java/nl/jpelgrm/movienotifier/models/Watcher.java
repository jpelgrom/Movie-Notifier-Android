package nl.jpelgrm.movienotifier.models;

import com.google.gson.annotations.Expose;

public class Watcher {
    @Expose(serialize = false)
    private String id;

    @Expose
    private String userid;

    @Expose
    private String name;

    @Expose
    private Integer movieid;

    @Expose
    private Long begin;

    @Expose
    private Long end;

    @Expose
    private WatcherFilters filters;

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getUserID() {
        return userid;
    }

    public void setUserID(String userid) {
        this.userid = userid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMovieID() {
        return movieid;
    }

    public void setMovieID(Integer movieid) {
        this.movieid = movieid;
    }

    public Long getBegin() {
        return begin;
    }

    public void setBegin(Long begin) {
        this.begin = begin;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public WatcherFilters getFilters() {
        return filters;
    }

    public void setFilters(WatcherFilters filters) {
        this.filters = filters;
    }
}
