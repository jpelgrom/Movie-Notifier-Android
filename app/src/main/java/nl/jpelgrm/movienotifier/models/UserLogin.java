package nl.jpelgrm.movienotifier.models;

import com.google.gson.annotations.Expose;

public class UserLogin {
    @Expose
    private String name;

    @Expose
    private String password;

    public UserLogin(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}