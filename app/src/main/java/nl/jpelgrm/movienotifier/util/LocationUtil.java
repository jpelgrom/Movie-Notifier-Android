package nl.jpelgrm.movienotifier.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

import nl.jpelgrm.movienotifier.models.Cinema;

public class LocationUtil {
    private FusedLocationProviderClient locationClient;
    private ArrayDeque<LocationUtilRequest> queue = new ArrayDeque<>();

    public void setupLocationClient(Context context) {
        if(locationClient == null) {
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationClient = LocationServices.getFusedLocationProviderClient(context);
            }
        }
    }

    public void onStop() {
        if(locationClient != null) {
            locationClient.removeLocationUpdates(updateCallback);
        }
    }

    public void getLocation(Context context, final LocationUtilRequest request) {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.getLastLocation() // Immediately give something back as well, improves UI speed while we wait for a fresh location
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            request.onLocationReceived(location, true);
                        }
                    });

            queue.add(request);
            checkQueue();
        } else {
            request.onLocationReceived(null, false);
        }
    }

    private void checkQueue() {
        if(queue.size() > 0) {
            processQueue();
        }
    }

    private void processQueue() {
        if(ContextCompat.checkSelfPermission(queue.getFirst().getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && locationClient != null) {
            LocationRequest googleRequest = new LocationRequest();
            googleRequest.setInterval(2500);
            googleRequest.setFastestInterval(2500);
            googleRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            locationClient.requestLocationUpdates(googleRequest, updateCallback, null);
        } else {
            for(LocationUtilRequest request : queue) {
                request.onLocationReceived(null, false);
                queue.remove(request);
            }
        }
    }

    private LocationCallback updateCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if(locationResult.getLocations().size() > 0) {
                for(LocationUtilRequest request : queue) {
                    request.onLocationReceived(locationResult.getLocations().get(0), false);
                    queue.remove(request);
                }
            } else {
                for(LocationUtilRequest request : queue) {
                    request.onLocationReceived(null, false);
                    queue.remove(request);
                }
            }
            locationClient.removeLocationUpdates(this);
        };
    };

    public static float getDistance(Location loc1, Location loc2) {
        return loc1.distanceTo(loc2);
    }

    public static float getDistance(Location loc1, double loc2lat, double loc2lon) {
        Location loc2 = new Location("");
        loc2.setLatitude(loc2lat);
        loc2.setLongitude(loc2lon);
        return getDistance(loc1, loc2);
    }

    public static String getFormattedDistance(Location loc1, Location loc2) {
        float distance = loc1.distanceTo(loc2);
        return String.format(Locale.getDefault(), "%.0f km", distance / 1000);
    }

    public static String getFormattedDistance(Location loc1, double loc2lat, double loc2lon) {
        Location loc2 = new Location("");
        loc2.setLatitude(loc2lat);
        loc2.setLongitude(loc2lon);
        return getFormattedDistance(loc1, loc2);
    }

    public Cinema getClosestCinema(Location location, List<Cinema> cinemas) {
        Cinema closest = null;
        for(Cinema cinema: cinemas) {
            if(cinema.getLatitude() != null && cinema.getLongitude() != null) {
                float distance = getDistance(location, cinema.getLatitude(), cinema.getLongitude());
                if(closest == null || distance < getDistance(location, closest.getLatitude(), closest.getLongitude())) {
                    closest = cinema;
                }
            }
        }

        return closest;
    }

    public interface LocationUtilRequest {
        void onLocationReceived(Location location, boolean isCachedResult);
        Context getContext();
    }
}
