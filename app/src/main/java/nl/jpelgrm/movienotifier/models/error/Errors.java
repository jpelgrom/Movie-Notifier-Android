package nl.jpelgrm.movienotifier.models.error;

import com.google.gson.annotations.Expose;

import java.util.List;

public class Errors {
    @Expose
    private List<String> errors = null;

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
