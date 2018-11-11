package nl.jpelgrm.movienotifier.models;

import com.google.gson.annotations.Expose;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "Users")
public class User {
    @Expose(serialize = false)
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "ID")
    private String id = "";

    @Expose
    @ColumnInfo(name = "Name")
    private String name;

    @Expose
    @ColumnInfo(name = "Email")
    private String email;

    @Expose
    @ColumnInfo(name = "Phone")
    private String phonenumber;

    @Expose
    @Ignore
    private String password;

    @Expose
    @ColumnInfo(name = "Notifications")
    private List<String> notifications = null;

    @Expose(serialize = false)
    @ColumnInfo(name = "APIKey")
    private String apikey;

    @Ignore
    public User() {
        // Default constructor
    }

    @Ignore
    public User(String name, String email, String phonenumber, String password) {
        this.name = name;
        this.email = email;
        this.phonenumber = phonenumber;
        this.password = password;
    }

    public User(@NonNull String id, String name, String email, String phonenumber, List<String> notifications, String apikey) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phonenumber = phonenumber;
        this.notifications = notifications;
        this.apikey = apikey;
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

    public void setPassword(String password) {
        this.password = password;
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
}