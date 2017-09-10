package nl.jpelgrm.movienotifier;

import android.app.Application;
import android.support.text.emoji.EmojiCompat;
import android.support.text.emoji.FontRequestEmojiCompatConfig;
import android.support.v4.provider.FontRequest;
import android.support.v7.app.AppCompatDelegate;

import nl.jpelgrm.movienotifier.util.StethoUtil;

public class MovieNotifierApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StethoUtil.install(this);

        AppCompatDelegate.setDefaultNightMode(getSharedPreferences("settings", MODE_PRIVATE).getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO));

        FontRequest fontRequest = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs);
        EmojiCompat.Config config = new FontRequestEmojiCompatConfig(this, fontRequest);
        EmojiCompat.init(config);
    }
}
