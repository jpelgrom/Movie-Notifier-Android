package nl.jpelgrm.movienotifier.util

import androidx.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType

class RoomUtil {
    private fun getMoshiListAdapter(): JsonAdapter<List<String>> {
        val type: ParameterizedType = Types.newParameterizedType(List::class.java, String::class.java)
        return Moshi.Builder().build().adapter(type)
    }

    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return if (value == null) null else getMoshiListAdapter().fromJson(value)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return getMoshiListAdapter().toJson(list)
    }
}