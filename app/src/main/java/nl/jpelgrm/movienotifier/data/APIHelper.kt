package nl.jpelgrm.movienotifier.data

import nl.jpelgrm.movienotifier.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object APIHelper {
    val instance: APIClient by lazy {
        val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.SERVER_BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

        retrofit.create(APIClient::class.java)
    }
}