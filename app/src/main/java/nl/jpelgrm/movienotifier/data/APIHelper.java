package nl.jpelgrm.movienotifier.data;

import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.util.StethoUtil;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class APIHelper {
    private static APIClient instance = null;

    public static APIClient getInstance() {
        if(instance == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.SERVER_BASE_URL)
                    .client(StethoUtil.getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instance = retrofit.create(APIClient.class);
        }
        return instance;
    }
}
