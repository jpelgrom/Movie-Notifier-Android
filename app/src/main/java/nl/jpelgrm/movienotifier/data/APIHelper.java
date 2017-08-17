package nl.jpelgrm.movienotifier.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.util.StethoUtil;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class APIHelper {
    private static APIClient instance = null;

    public static APIClient getInstance() {
        if(instance == null) {
            Gson gson = new GsonBuilder().serializeNulls().excludeFieldsWithoutExposeAnnotation().create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.SERVER_BASE_URL)
                    .client(StethoUtil.getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            instance = retrofit.create(APIClient.class);
        }
        return instance;
    }
}
