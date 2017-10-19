package nl.jpelgrm.movienotifier.util;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

import nl.jpelgrm.movienotifier.models.Cinema;

public class DataUtil {
    public static List<Cinema> readCinemasJson(Context context) {
        String json = null;
        try {
            InputStream inputStream = context.getAssets().open("cinemas.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Type listType = new TypeToken<List<Cinema>>() {}.getType();
        return new Gson().fromJson(json, listType);
    }
}
