package nl.jpelgrm.movienotifier.models;

import com.google.gson.annotations.Expose;

public class Watcher {
    @Expose(serialize = false)
    private String uuid;

    @Expose
    private String user;

    @Expose
    private String name;

    @Expose
    private Integer movieid;

    @Expose
    private String cinemaid;

    @Expose
    private String startAfter;

    @Expose
    private String startBefore;

    @Expose
    private Props props;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMovieid() {
        return movieid;
    }

    public void setMovieid(Integer movieid) {
        this.movieid = movieid;
    }

    public String getCinemaid() {
        return cinemaid;
    }

    public void setCinemaid(String cinemaid) {
        this.cinemaid = cinemaid;
    }

    public String getStartAfter() {
        return startAfter;
    }

    public void setStartAfter(String startAfter) {
        this.startAfter = startAfter;
    }

    public String getStartBefore() {
        return startBefore;
    }

    public void setStartBefore(String startBefore) {
        this.startBefore = startBefore;
    }

    public Props getProps() {
        return props;
    }

    public void setProps(Props props) {
        this.props = props;
    }

}
