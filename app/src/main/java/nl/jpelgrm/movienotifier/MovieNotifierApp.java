package nl.jpelgrm.movienotifier;

import android.app.Application;

import nl.jpelgrm.movienotifier.util.StethoUtil;

public class MovieNotifierApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StethoUtil.install(this);
    }
}
