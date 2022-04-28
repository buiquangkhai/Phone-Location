package com.example.mapdemo;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.mapdemo.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, SensorEventListener {

    private static final String TAG1 = "Main Activity";
    private static final float DELTA_T = (float) 0.2;
    private static final float M_TO_LAT = (float) 1/110600;
    private static final float M_TO_LONG = (float) 1/111300;
    private GoogleMap mMap = null;
    private ActivityMapsBinding binding;

    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor sensorAcc, sensorGra, sensorMag;
    private double curLat, curLong;
    private LatLng curLocation;
    private Marker marker = null;
    private float[] gravityValues = null;
    private float[] magneticValues = null;
    private float velocityX, velocityY, velocityZ, dispX, dispY, dispZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        curLat = 39.12;
        curLong = -76.54;

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            return;
        }
        //mMap.setMyLocationEnabled(true);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 0, this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorGra = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    //Use firebase?
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG1, "Map is ready");
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setTrafficEnabled(true);
        // Add a marker in Sydney and move the camera
        curLocation = new LatLng(curLat, curLong);
        if (marker != null) {
            marker.remove();
        }
        marker = mMap.addMarker(new MarkerOptions().position(curLocation).title("Initial Position"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(curLocation));

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG1, "Program on resume");

        sensorManager.registerListener(this, sensorAcc, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorGra, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorMag, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG1, "Program on paused");
        sensorManager.unregisterListener(this, sensorAcc);
        sensorManager.unregisterListener(this, sensorGra);
        sensorManager.unregisterListener(this, sensorMag);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG1, "GPS: Location changed");
        curLat = location.getLatitude();
        curLong = location.getLongitude();
        curLocation = new LatLng(curLat,curLong);
        if (marker != null) {
            marker.remove();
        }

        marker = mMap.addMarker(new MarkerOptions().position(curLocation).title("My current location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curLocation,20));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Work with Accelerometer

        //Code snippet from StackOverFlow to translate accelerometer data into the same coordinate
        //system used by the navigation system on Google Map
        if ((gravityValues != null) && (magneticValues != null)
                && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {

            float[] deviceRelativeAcceleration = new float[4];
            deviceRelativeAcceleration[0] = event.values[0];
            deviceRelativeAcceleration[1] = event.values[1];
            deviceRelativeAcceleration[2] = event.values[2];
            deviceRelativeAcceleration[3] = 0;

            float[] R = new float[16], I = new float[16], earthAcc = new float[16];

            SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

            float[] inv = new float[16];

            android.opengl.Matrix.invertM(inv, 0, R, 0);
            android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);

            //End StackOverFlow code snippet

            //Calculate instantaneous velocity (basic formula) with delta_t = 0.2s
            velocityX = earthAcc[0] * DELTA_T;
            velocityY = earthAcc[1] * DELTA_T;

            //Calculate instantaneous displacement (basic formula) with delta_t = 0.2s
            dispX = velocityX * DELTA_T;
            dispY = velocityY * DELTA_T;

            //Log.d("Displacement", "Values: (" + dispX + ", " + dispY + ")");
            //Convert displacement into latitude and longitude
            curLat = curLat + dispX * M_TO_LAT;
            curLong = curLong + dispY * M_TO_LONG;
            if (mMap != null) {
                curLocation = new LatLng(curLat, curLong);
                if (marker != null) {
                    marker.remove();
                }

                marker = mMap.addMarker(new MarkerOptions().position(curLocation).title("My current location"));

            }

        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravityValues = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}