package nl.jpelgrm.movienotifier.util;

import android.app.Application;

import okhttp3.OkHttpClient;

public class StethoUtil {
    public static void install(Application application) {
        // Do nothing
    }

    public static OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder().build();
    }
}
