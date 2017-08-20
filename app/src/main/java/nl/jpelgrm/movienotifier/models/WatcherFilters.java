package nl.jpelgrm.movienotifier.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WatcherFilters {
    public enum WatcherFilterValue {
        @SerializedName("yes")
        YES,

        @SerializedName("no-preference")
        NOPREFERENCE,

        @SerializedName("no")
        NO
    }

    @Expose
    private String cinemaid;

    @Expose
    private Long startafter;

    @Expose
    private Long startbefore;

    @Expose
    private WatcherFilterValue ov = WatcherFilterValue.NOPREFERENCE;

    @Expose
    private WatcherFilterValue nl = WatcherFilterValue.NOPREFERENCE;

    @Expose
    private WatcherFilterValue imax = WatcherFilterValue.NOPREFERENCE;

    @SerializedName("3d")
    @Expose
    private WatcherFilterValue _3d = WatcherFilterValue.NOPREFERENCE;

    @Expose
    private WatcherFilterValue hfr = WatcherFilterValue.NOPREFERENCE;

    @SerializedName("4k")
    @Expose
    private WatcherFilterValue _4k = WatcherFilterValue.NOPREFERENCE;

    @Expose
    private WatcherFilterValue laser = WatcherFilterValue.NOPREFERENCE;

    @Expose
    private WatcherFilterValue dbox = WatcherFilterValue.NOPREFERENCE;

    @Expose
    private WatcherFilterValue dolbycinema = WatcherFilterValue.NOPREFERENCE;

    @Expose
    private WatcherFilterValue dolbyatmos = WatcherFilterValue.NOPREFERENCE;

    public String getCinemaID() {
        return cinemaid;
    }

    public void setCinemaID(String cinemaid) {
        this.cinemaid = cinemaid;
    }

    public Long getStartAfter() {
        return startafter;
    }

    public void setStartAfter(Long startafter) {
        this.startafter = startafter;
    }

    public Long getStartBefore() {
        return startbefore;
    }

    public void setStartBefore(Long startbefore) {
        this.startbefore = startbefore;
    }

    public WatcherFilterValue isOriginalVersion() {
        return ov;
    }

    public void setOriginalVersion(WatcherFilterValue ov) {
        this.ov = ov;
    }

    public WatcherFilterValue isDutchVersion() {
        return nl;
    }

    public void setDutchVersion(WatcherFilterValue nl) {
        this.nl = nl;
    }

    public WatcherFilterValue isIMAX() {
        return imax;
    }

    public void setIMAX(WatcherFilterValue imax) {
        this.imax = imax;
    }

    public WatcherFilterValue is3D() {
        return _3d;
    }

    public void set3D(WatcherFilterValue _3d) {
        this._3d = _3d;
    }

    public WatcherFilterValue isHFR() {
        return hfr;
    }

    public void setHFR(WatcherFilterValue hfr) {
        this.hfr = hfr;
    }

    public WatcherFilterValue is4K() {
        return _4k;
    }

    public void set4K(WatcherFilterValue _4k) {
        this._4k = _4k;
    }

    public WatcherFilterValue isLaser() {
        return laser;
    }

    public void setLaser(WatcherFilterValue laser) {
        this.laser = laser;
    }

    public WatcherFilterValue isDBOX() {
        return dbox;
    }

    public void setDBOX(WatcherFilterValue dbox) {
        this.dbox = dbox;
    }

    public WatcherFilterValue isDolbyCinema() {
        return dolbycinema;
    }

    public void setDolbyCinema(WatcherFilterValue dolbycinema) {
        this.dolbycinema = dolbycinema;
    }

    public WatcherFilterValue isDolbyAtmos() {
        return dolbyatmos;
    }

    public void setDolbyAtmos(WatcherFilterValue dolbyatmos) {
        this.dolbyatmos = dolbyatmos;
    }
}
