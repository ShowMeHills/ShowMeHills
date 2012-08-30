/*
    Copyright 2012 Nik Cain nik@showmehills.com
    
    This file is part of ShowMeHills.

    ShowMeHills is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ShowMeHills is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ShowMeHills.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.showmehills;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.showmehills.R;

public class MapOverlay extends MapActivity implements IShowMeHillsActivity, SensorEventListener {
	
	private HillDatabase myDbHelper;
	private Location curLocation;
	MapOverlayCompassItem compassOverlay;

	private RapidGPSLock mGPS;
	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;  
	float[] mGravity;
	float[] mGeomagnetic;
	float mDeclination = 0;
	int minLat = 0;
    int maxLat = 0;
    int minLon = 0;
    int maxLon = 0;
    private boolean mHasAccurateGravity = false;
    private boolean mHasAccurateAccelerometer = false;
    
	Timer timer = new Timer();
	private int GPSretryTime = 15;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);	 
		
		mGPS = new RapidGPSLock(this);
        mGPS.switchOn();
        mGPS.findLocation();
        
        myDbHelper = new HillDatabase(this); 
        myDbHelper.createDataBase(); 
	 		 	
        setContentView(R.layout.mapoverlay);
        MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        UpdateMarkers();
        MapController mc = mapView.getController();
		double fitFactor = 1.5;
        mc.zoomToSpan((int) (Math.abs(maxLat - minLat) * fitFactor), (int)(Math.abs(maxLon - minLon) * fitFactor));
        mc.animateTo(new GeoPoint( (maxLat + minLat)/2, (maxLon + minLon)/2 ));
        
		timer.scheduleAtFixedRate(new LocationTimerTask(),GPSretryTime* 1000,GPSretryTime* 1000);
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

	public void UpdateMarkers()
	{
        curLocation = mGPS.getCurrentLocation();
        if (curLocation == null) return;
        if (!myDbHelper.checkDataBase()) return;
        myDbHelper.SetDirections(curLocation);
        
        MapView mapView = (MapView) findViewById(R.id.mapview);
        List<Overlay> mapOverlays = mapView.getOverlays();
        mapOverlays.clear();
        Drawable barrw = this.getResources().getDrawable(R.drawable.bluearrow);
        compassOverlay = new MapOverlayCompassItem(barrw, this);

        OverlayItem compassitem = new OverlayItem(new GeoPoint((int)(curLocation.getLatitude()*1E6),(int)(curLocation.getLongitude()*1E6)), "me","me");

        compassOverlay.addOverlay(compassitem);
        mapOverlays.add(compassOverlay);
        Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
        
        minLat = (int) ((curLocation.getLatitude() - 0.01)*1E6);
        maxLat = (int) ((curLocation.getLatitude() + 0.01)*1E6);
        minLon = (int) ((curLocation.getLongitude() - 0.01)*1E6);
        maxLon = (int) ((curLocation.getLongitude() + 0.01)*1E6);
        Log.d("showmehills", "map lon-lat = " + minLat + "," + minLon);
	    ArrayList<Hills> localhills = myDbHelper.localhills;
		for (int h = 0; h < localhills.size(); h++)
		{
			Hills h1 = localhills.get(h);
			Log.d("showmehills", "adding " + h1.hillname);
			GeoPoint point = new GeoPoint((int)(h1.latitude*1E6),(int)(h1.longitude*1E6));
	        OverlayItem overlayitem = new OverlayItem(point, h1.hillname, h1.hillname);

	        MapOverlayItem itemizedoverlay = new MapOverlayItem(drawable, this);
	        itemizedoverlay.addOverlay(overlayitem);
	        mapOverlays.add(itemizedoverlay);

            maxLat = Math.max(point.getLatitudeE6(), maxLat);
            minLat = Math.min(point.getLatitudeE6(), minLat);
            maxLon = Math.max(point.getLongitudeE6(), maxLon);
            minLon = Math.min(point.getLongitudeE6(), minLon);
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

	@Override
	protected boolean isRouteDisplayed() {
		return false;
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
				if (compassOverlay != null && compassOverlay.size() > 0)
				{
					compassOverlay.mBearing = (float) Math.toDegrees((float)dv[0]);

			        MapView mapView = (MapView) findViewById(R.id.mapview);
			        mapView.invalidate();
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
}
