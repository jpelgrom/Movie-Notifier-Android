package nl.jpelgrm.movienotifier.data

import com.google.gson.GsonBuilder
import nl.jpelgrm.movienotifier.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object APIHelper {
    val instance: APIClient by lazy {
        val gson = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create()
        val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.SERVER_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        retrofit.create(APIClient::class.java)
    }
}