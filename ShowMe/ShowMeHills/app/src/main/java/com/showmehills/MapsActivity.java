package com.showmehills;

import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, IShowMeHillsActivity, SensorEventListener {

    private GoogleMap mMap;

    private HillDatabase myDbHelper;
    private Location curLocation;

    Marker compassMarker = null;

    private RapidGPSLock mGPS;
    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    float mDeclination = 0;

    private boolean mHasAccurateGravity = false;
    private boolean mHasAccurateAccelerometer = false;

    Timer timer = new Timer();
    private int GPSretryTime = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);

        mGPS = new RapidGPSLock(this);
        mGPS.switchOn();
        mGPS.findLocation();

        myDbHelper = new HillDatabase(this, getString(R.string.dbname), getString(R.string.dbpath));
        myDbHelper.createDataBase();

        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        UpdateMarkers();
        LatLng here = new LatLng(curLocation.getLatitude(), curLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(here));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(10));

        timer.scheduleAtFixedRate(new LocationTimerTask(), GPSretryTime * 1000, GPSretryTime * 1000);
    }

    public void UpdateMarkers()
    {
        if (mMap == null) return;
        curLocation = mGPS.getCurrentLocation();
        if (curLocation == null) return;
        if (!myDbHelper.checkDataBase()) return;
        myDbHelper.SetDirections(curLocation);


        LatLng pt = new LatLng(curLocation.getLatitude(), curLocation.getLongitude());
        if (compassMarker == null) {
            compassMarker = mMap.addMarker(new MarkerOptions().position(pt)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bluearrow)));
        }

        ArrayList<Hills> localhills = myDbHelper.localhills;
        for (int h = 0; h < localhills.size(); h++)
        {
            Hills h1 = localhills.get(h);
            Log.d("showmehills", "adding " + h1.hillname);
            pt = new LatLng(h1.latitude, h1.longitude);
            mMap.addMarker(new MarkerOptions().position(pt)
                                                .title(h1.hillname)
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.androidmarker)));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mappreferences_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.preferences_menutitem) {
            Intent settingsActivity = new Intent(getBaseContext(),AppPreferences.class);
            startActivity(settingsActivity);
        } else if (item.getItemId() == R.id.cameraview) {
            finish();
        } else if (item.getItemId() == R.id.help) {
            Intent myHelpIntent = new Intent(getBaseContext(), Help.class);
            startActivityForResult(myHelpIntent, 0);
        } else if (item.getItemId() == R.id.about) {
            Intent myAboutIntent = new Intent(getBaseContext(), About.class);
            startActivityForResult(myAboutIntent, 0);
        }
        return super.onOptionsItemSelected(item);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {

        // some phones never set the sensormanager as reliable, even when readings are ok
        // That means if we try to block it, those phones will never get a compass reading.
        // So we let any readings through until we know we can get accurate readings. Once We know that
        // we'll block the inaccurate ones
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && mHasAccurateAccelerometer) return;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && mHasAccurateGravity) return;
        }
        else
        {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) mHasAccurateAccelerometer = true;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) mHasAccurateGravity = true;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)  mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {

            float[] rotationMatrixA = new float[9];
            if (SensorManager.getRotationMatrix(rotationMatrixA, null, mGravity, mGeomagnetic)) {
                Matrix tmpA = new Matrix();
                tmpA.setValues(rotationMatrixA);
                tmpA.postRotate( -mDeclination );
                tmpA.getValues(rotationMatrixA);

                float[] dv = new float[3];
                SensorManager.getOrientation(rotationMatrixA, dv);
                if (compassMarker != null)
                {
                    compassMarker.setRotation( (float) Math.toDegrees(dv[0]));
                }
            }
        }
    }

    public LocationManager GetLocationManager() {
        return (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    }

    class LocationTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            Log.d("showmehills", "renew GPS search");
            runOnUiThread(new Runnable() {
                public void run() {
                    mGPS.RenewLocation();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        Log.d("showmehills", "onResume");
        super.onResume();
        mGPS.switchOn();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);

        timer = new Timer();
        timer.scheduleAtFixedRate(new LocationTimerTask(),GPSretryTime* 1000,GPSretryTime* 1000);

        UpdateMarkers();

        myDbHelper.checkDataBase();
    }

    @Override
    protected void onPause() {
        Log.d("showmehills", "onPause");
        super.onPause();
        timer.cancel();
        mGPS.switchOff();
        mSensorManager.unregisterListener(this);

        try {
            myDbHelper.close();
        }catch(SQLException sqle){
            throw sqle;
        }
    }
    @Override
    protected void onStop()
    {
        try {
            myDbHelper.close();
        }catch(SQLException sqle){
            throw sqle;
        }
        super.onStop();
    }

}
