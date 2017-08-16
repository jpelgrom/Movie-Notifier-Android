package nl.jpelgrm.movienotifier.util;

import android.app.Application;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import okhttp3.OkHttpClient;

public class StethoUtil {
    public static void install(Application application) {
        Stetho.initializeWithDefaults(application);
    }

    public static OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();
    }
}
