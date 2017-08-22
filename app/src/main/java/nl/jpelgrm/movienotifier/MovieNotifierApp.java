package nl.jpelgrm.movienotifier;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

import nl.jpelgrm.movienotifier.util.StethoUtil;

public class MovieNotifierApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StethoUtil.install(this);

        AppCompatDelegate.setDefaultNightMode(getSharedPreferences("settings", MODE_PRIVATE).getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO));
    }
}
