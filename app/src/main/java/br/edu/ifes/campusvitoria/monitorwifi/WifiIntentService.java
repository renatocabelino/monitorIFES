package br.edu.ifes.campusvitoria.monitorwifi;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import static android.support.v4.content.WakefulBroadcastReceiver.completeWakefulIntent;

public class WifiIntentService extends IntentService {

    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";


    public WifiIntentService() {
        super( "WifiIntentService" );
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        LocationManager locationManager;
        LocationListener locationListener;
        String locationProvider;
        WifiManager wifiManager;

        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService( Context.WIFI_SERVICE );
        Log.i( "WifiScanServiceIntent: ", "Iniciando tarefa ..." );
        mWifiManager.startScan();
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService( Context.LOCATION_SERVICE );
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                Log.i( "wifiMonitor", location.toString() );

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
        // Register the listener with the Location Manager to receive location updates
        locationProvider = LocationManager.NETWORK_PROVIDER;
        if (ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            locationManager.requestLocationUpdates( locationProvider, 0, 0, locationListener );
            Location lastKnownLocation = locationManager.getLastKnownLocation( locationProvider );
            // Remove the listener you previously added
            locationManager.removeUpdates( locationListener );
            MainActivity.LATITUDE = lastKnownLocation.getLatitude();
            MainActivity.LONGITUDE = lastKnownLocation.getLongitude();
        }

        Log.i( "WifiScanIntentService: ", "Finalizando tarefa ..." );
        completeWakefulIntent( intent );
    }
}
