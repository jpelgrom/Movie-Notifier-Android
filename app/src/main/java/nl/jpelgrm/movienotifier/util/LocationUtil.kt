package nl.jpelgrm.movienotifier.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.util.*

class LocationUtil {
    private var locationClient: FusedLocationProviderClient? = null
    private val queue = ArrayDeque<LocationUtilRequest>()
    private fun setupLocationClientIfNecessary(context: Context?) {
        if (context != null && locationClient == null) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationClient = LocationServices.getFusedLocationProviderClient(context)
            }
        }
    }

    fun onStop() = locationClient?.removeLocationUpdates(updateCallback)

    fun getLocation(context: Context?, request: LocationUtilRequest) {
        setupLocationClientIfNecessary(context)

        if (context != null && locationClient != null
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient?.lastLocation // Immediately give something back as well, improves UI speed while we wait for a fresh location
                    ?.addOnSuccessListener { location -> request.onLocationReceived(location, true) }
            queue.add(request)
            checkQueue()
        } else {
            request.onLocationReceived(null, false)
        }
    }

    private fun checkQueue() {
        if (queue.size > 0) {
            processQueue()
        }
    }

    private fun processQueue() {
        if (queue.first.context != null && locationClient != null
                && ContextCompat.checkSelfPermission(queue.first.context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val googleRequest = LocationRequest()
            googleRequest.interval = 2500
            googleRequest.fastestInterval = 2500
            googleRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationClient?.requestLocationUpdates(googleRequest, updateCallback, null)
        } else {
            clearQueueWithEmptyResult()
        }
    }

    private fun clearQueueWithEmptyResult() {
        queue.forEach { it.onLocationReceived(null, false) }
        queue.clear()
    }

    private val updateCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (locationResult.locations.size > 0) {
                queue.forEach { it.onLocationReceived(locationResult.locations[0], false) }
                queue.clear()
            } else {
                clearQueueWithEmptyResult()
            }
            locationClient?.removeLocationUpdates(this)
        }
    }

    interface LocationUtilRequest {
        fun onLocationReceived(location: Location?, isCachedResult: Boolean)
        val context: Context?
    }
}