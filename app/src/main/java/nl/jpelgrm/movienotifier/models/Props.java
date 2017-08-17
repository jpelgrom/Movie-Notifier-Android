package nl.jpelgrm.movienotifier.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Props {
    @Expose
    private Boolean ov;

    @Expose
    private Boolean nl;

    @Expose
    private Boolean imax;

    @SerializedName("3d")
    @Expose
    private Boolean _3d;

    @Expose
    private Boolean hfr;

    @SerializedName("4k")
    @Expose
    private Boolean _4k;

    @Expose
    private Boolean laser;

    @Expose
    private Boolean dbox;

    @Expose
    private Boolean dolbycinema;

    @Expose
    private Boolean dolbyatmos;

    public Boolean isOriginalVersion() {
        return ov;
    }

    public void setIsOriginalVersion(Boolean ov) {
        this.ov = ov;
    }

    public Boolean isDutchVersion() {
        return nl;
    }

    public void setIsDutchVersion(Boolean nl) {
        this.nl = nl;
    }

    public Boolean isIMAX() {
        return imax;
    }

    public void setIMAX(Boolean imax) {
        this.imax = imax;
    }

    public Boolean is3D() {
        return _3d;
    }

    public void set3D(Boolean _3d) {
        this._3d = _3d;
    }

    public Boolean isHFR() {
        return hfr;
    }

    public void setHFR(Boolean hfr) {
        this.hfr = hfr;
    }

    public Boolean is4K() {
        return _4k;
    }

    public void set4K(Boolean _4k) {
        this._4k = _4k;
    }

    public Boolean isLaser() {
        return laser;
    }

    public void setLaser(Boolean laser) {
        this.laser = laser;
    }

    public Boolean isDBOX() {
        return dbox;
    }

    public void setDBOX(Boolean dbox) {
        this.dbox = dbox;
    }

    public Boolean isDolbyCinema() {
        return dolbycinema;
    }

    public void setDolbyCinema(Boolean dolbycinema) {
        this.dolbycinema = dolbycinema;
    }

    public Boolean isDolbyAtmos() {
        return dolbyatmos;
    }

    public void setDolbyAtmos(Boolean dolbyatmos) {
        this.dolbyatmos = dolbyatmos;
    }

}
