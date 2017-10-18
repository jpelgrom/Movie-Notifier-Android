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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationUtil implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient googleClient;
    private boolean ready = false;
    private List<LocationUtilRequest> queue = new ArrayList<>();

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
                request.onLocationReceived(LocationServices.FusedLocationApi.getLastLocation(googleClient));
            }

            queue.add(request);
            checkQueue();
        } else {
            request.onLocationReceived(null);
        }
    }

    private void checkQueue() {
        if(ready && queue.size() > 0) {
            processQueue();
        }
    }

    private void processQueue() {
        if(ContextCompat.checkSelfPermission(queue.get(0).getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest googleRequest = new LocationRequest();
            googleRequest.setInterval(2500);
            googleRequest.setFastestInterval(2500);
            googleRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates(googleClient, googleRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    for(LocationUtilRequest request : queue) {
                        request.onLocationReceived(location);
                        queue.remove(request);
                    }
                    LocationServices.FusedLocationApi.removeLocationUpdates(googleClient, this);
                }
            });
        } else {
            for(LocationUtilRequest request : queue) {
                request.onLocationReceived(null);
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

    public interface LocationUtilRequest {
        void onLocationReceived(Location location);
        Context getContext();
    }
}
