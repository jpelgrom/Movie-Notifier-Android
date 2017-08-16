package nl.jpelgrm.movienotifier.models;

import com.google.gson.annotations.Expose;

public class NotificationType {
    @Expose
    private String key;

    @Expose
    private String name;

    @Expose
    private String description;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
