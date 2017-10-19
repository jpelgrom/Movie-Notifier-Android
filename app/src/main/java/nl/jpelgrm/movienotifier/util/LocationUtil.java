package nl.jpelgrm.movienotifier.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

import nl.jpelgrm.movienotifier.models.Cinema;

public class LocationUtil implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient googleClient;
    private boolean ready = false;
    private ArrayDeque<LocationUtilRequest> queue = new ArrayDeque<>();

    public void setupGoogleClient(Context context, boolean onCreate) {
        if(googleClient == null) {
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleClient = new GoogleApiClient.Builder(context)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();
            }
        }
        if(!onCreate && !googleClient.isConnected()) {
            googleClient.connect();
        }
    }

    public void onStart() {
        if(googleClient != null) {
            googleClient.connect();
        }
    }

    public void onStop() {
        if(googleClient != null) {
            googleClient.disconnect();
        }
    }

    public void getLocation(Context context, final LocationUtilRequest request) {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(ready) { // Immediately give something back as well, improves UI speed while we wait for a fresh location
                request.onLocationReceived(LocationServices.FusedLocationApi.getLastLocation(googleClient), true);
            }

            queue.add(request);
            checkQueue();
        } else {
            request.onLocationReceived(null, false);
        }
    }

    private void checkQueue() {
        if(ready && queue.size() > 0) {
            processQueue();
        }
    }

    private void processQueue() {
        if(ContextCompat.checkSelfPermission(queue.getFirst().getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && googleClient != null) {
            if(googleClient.isConnected()) {
                LocationRequest googleRequest = new LocationRequest();
                googleRequest.setInterval(2500);
                googleRequest.setFastestInterval(2500);
                googleRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                LocationServices.FusedLocationApi.requestLocationUpdates(googleClient, googleRequest, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        for(LocationUtilRequest request : queue) {
                            request.onLocationReceived(location, false);
                            queue.remove(request);
                        }
                        LocationServices.FusedLocationApi.removeLocationUpdates(googleClient, this);
                    }
                });
            } // else onConnected should be called soon and execute this code
        } else {
            for(LocationUtilRequest request : queue) {
                request.onLocationReceived(null, false);
                queue.remove(request);
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        ready = true;
        checkQueue();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        ready = false;
    }

    @Override
    public void onConnectionSuspended(int i) {
        ready = false;
    }

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
