package nl.jpelgrm.movienotifier.models.error;

import com.google.gson.annotations.Expose;

public class Message {
    @Expose
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
