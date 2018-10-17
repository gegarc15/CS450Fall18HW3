package edu.stlawu.locationgps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Observable;

public class LocationHandler extends Observable implements LocationListener {

    private LocationManager lm;
    private MainActivity act;

    public LocationHandler(MainActivity act){
        this.act = act;
        this.lm = (LocationManager) this.act.getSystemService(Context.LOCATION_SERVICE);
        if(this.act.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
            lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000, 0, this);

            ArrayList compareAccuracy = new ArrayList<Float>();
            float gps;
            float network;
            float passive;

            Hashtable accuracyMap = new Hashtable<Float, Location>();

            // check for initial GPS location
            // TODO: check for accuracy
            Location lGPS = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(lGPS != null) {
                gps = lGPS.getAccuracy();
                compareAccuracy.add(gps);
                accuracyMap.put(gps, lGPS);
            }

            Location lNetwork = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(lNetwork != null){
                network = lNetwork.getAccuracy();
                compareAccuracy.add(network);
                accuracyMap.put(network, lNetwork);
            }

            Location lPassive = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if(lPassive != null) {
                passive = lPassive.getAccuracy();
                compareAccuracy.add(passive);
                accuracyMap.put(passive, lPassive);
            }

            if(compareAccuracy.size() != 0) {
                Location l;
                float bestAccuracy = (float) compareAccuracy.get(0);
                for (int i = 1; i < compareAccuracy.size(); i++) {
                    float curr = (float) compareAccuracy.get(i);
                    if (curr < bestAccuracy) {
                        bestAccuracy = curr;
                    }
                }

                l = (Location) accuracyMap.get(bestAccuracy);
                setChanged();
                notifyObservers(l);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // tell the observers that the location has changed
        setChanged();
        notifyObservers(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}