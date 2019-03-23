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
    @Ignore
    private String password;

    @Expose
    @ColumnInfo(name = "FCMTokens")
    private List<String> fcmTokens = null;

    @Expose(serialize = false)
    @ColumnInfo(name = "APIKey")
    private String apikey;

    @Ignore
    public User() {
        // Default constructor
    }

    @Ignore
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public User(@NonNull String id, String name, String email, List<String> fcmTokens, String apikey) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.fcmTokens = fcmTokens;
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

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getFcmTokens() {
        return fcmTokens;
    }

    public void setFcmTokens(List<String> fcmTokens) {
        this.fcmTokens = fcmTokens;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!id.equals(user.id)) return false;
        if (name != null ? !name.equals(user.name) : user.name != null) return false;
        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        if (password != null ? !password.equals(user.password) : user.password != null)
            return false;
        if (fcmTokens != null ? !fcmTokens.equals(user.fcmTokens) : user.fcmTokens != null)
            return false;
        return apikey != null ? apikey.equals(user.apikey) : user.apikey == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (fcmTokens != null ? fcmTokens.hashCode() : 0);
        result = 31 * result + (apikey != null ? apikey.hashCode() : 0);
        return result;
    }
}