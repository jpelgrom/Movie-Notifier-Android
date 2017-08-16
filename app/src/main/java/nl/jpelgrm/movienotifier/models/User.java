package nl.jpelgrm.movienotifier.models;

import android.content.ContentValues;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class User {
    @Expose
    private String uuid;

    @Expose
    private String name;

    @Expose
    private String email;

    @Expose
    private String phonenumber;

    @Expose
    private List<String> notifications = null;

    @Expose(serialize = false)
    private String apikey;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public List<String> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<String> notifications) {
        this.notifications = notifications;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put("UUID", uuid);
        cv.put("Name", name);
        cv.put("Email", email);
        cv.put("Phone", phonenumber);
        cv.put("Notifications", new Gson().toJson(notifications));
        cv.put("APIKey", apikey);

        return cv;
    }

    public void setContent(ContentValues cv) {
        uuid = cv.getAsString("UUID");
        name = cv.getAsString("Name");
        email = cv.getAsString("Email");
        phonenumber = cv.getAsString("Phone");

        Type listType = new TypeToken<List<String>>() {}.getType();
        notifications = new Gson().fromJson(cv.getAsString("Notifications"), listType);

        apikey = cv.getAsString("APIKey");
    }
}