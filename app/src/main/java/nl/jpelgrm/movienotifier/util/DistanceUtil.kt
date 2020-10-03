package nl.jpelgrm.movienotifier.util

import android.location.Location
import nl.jpelgrm.movienotifier.models.Cinema
import java.util.*

object DistanceUtil {
    @JvmStatic
    fun getClosestCinema(location: Location, cinemas: List<Cinema>): Cinema? {
        var closest: Cinema? = null
        for (cinema in cinemas) {
            if (cinema.lat != null && cinema.lon != null) {
                val distance = getDistance(location, cinema.lat, cinema.lon)
                if (closest == null || distance < getDistance(location, closest.lat, closest.lon)) {
                    closest = cinema
                }
            }
        }
        return closest
    }

    fun getDistance(loc1: Location?, loc2: Location?): Float {
        return loc1?.distanceTo(loc2) ?: error("Location 1 should never be null here")
    }

    @JvmStatic
    fun getDistance(loc1: Location?, loc2lat: Double, loc2lon: Double): Float {
        val loc2 = Location("").apply {
            latitude = loc2lat
            longitude = loc2lon
        }
        return getDistance(loc1, loc2)
    }

    private fun getFormattedDistance(loc1: Location?, loc2: Location?): String {
        return String.format(Locale.getDefault(), "%.0f km", getDistance(loc1, loc2) / 1000)
    }

    @JvmStatic
    fun getFormattedDistance(loc1: Location?, loc2lat: Double, loc2lon: Double): String {
        val loc2 = Location("").apply {
            latitude = loc2lat
            longitude = loc2lon
        }
        return getFormattedDistance(loc1, loc2)
    }
}