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

import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.showmehills.R;

public class MapOverlay  extends MapActivity implements SensorEventListener {
	
	private HillDatabase myDbHelper;
	private Location curLocation;
	MapOverlayCompassItem compassOverlay;

	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;  
	float[] mGravity;
	float[] mGeomagnetic;
	float mDeclination = 0;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);	 

        myDbHelper = new HillDatabase(this); 
        try { 
        	myDbHelper.createDataBase(); 
	 	} catch (IOException ioe) {	 
	 		throw new Error("Unable to create database");	 
	 	}	 
	 	try {	 
	 		myDbHelper.openDataBase();	 
	 	}catch(SQLException sqle){	 
	 		throw sqle;	 
	 	}
	 	
        setContentView(R.layout.mapoverlay);
        MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        AddItems();
        
    }	

	@Override
	protected void onResume() {
		Log.d("showmehills", "onResume");
		super.onResume();

		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);	 

		try {	 
			myDbHelper.openDataBase();	 
		}catch(SQLException sqle){	 
			throw sqle;	 
		}
	}

	@Override
	protected void onPause() {
		Log.d("showmehills", "onPause");
		super.onPause();   
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

	
	void AddItems()
	{

        Criteria fine = new Criteria();
		fine.setAccuracy(Criteria.ACCURACY_FINE);

		LocationManager mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        curLocation = mLocationManager.getLastKnownLocation(mLocationManager.getBestProvider(fine, true));
        if (curLocation == null) 
        {

    		fine.setAccuracy(Criteria.ACCURACY_COARSE);

    		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            curLocation = mLocationManager.getLastKnownLocation(mLocationManager.getBestProvider(fine, true));
            if (curLocation == null) 
            {
            	//give up
            	return;
            }
        }
        myDbHelper.SetDirections(curLocation);
        
        MapView mapView = (MapView) findViewById(R.id.mapview);
        List<Overlay> mapOverlays = mapView.getOverlays();

        Drawable barrw = this.getResources().getDrawable(R.drawable.bluearrow);
        compassOverlay = new MapOverlayCompassItem(barrw, this);

        OverlayItem compassitem = new OverlayItem(new GeoPoint((int)(curLocation.getLatitude()*1E6),(int)(curLocation.getLongitude()*1E6)), "me","me");

        compassOverlay.addOverlay(compassitem);
        mapOverlays.add(compassOverlay);
        Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
        
        int minLat = Integer.MAX_VALUE;
        int maxLat = Integer.MIN_VALUE;
        int minLon = Integer.MAX_VALUE;
        int maxLon = Integer.MIN_VALUE;

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
		MapController mc = mapView.getController();
		double fitFactor = 1.5;
        mc.zoomToSpan((int) (Math.abs(maxLat - minLat) * fitFactor), (int)(Math.abs(maxLon - minLon) * fitFactor));
        mc.animateTo(new GeoPoint( (maxLat + minLat)/2, (maxLon + minLon)/2 ));
        
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
		// TODO Auto-generated method stub
		return false;
	}
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	public void onSensorChanged(SensorEvent event) {
		/*if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
			return;
		}*/

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
				if (compassOverlay.size() > 0)
				{
					compassOverlay.mBearing = (float) Math.toDegrees((float)dv[0]);

			        MapView mapView = (MapView) findViewById(R.id.mapview);
			        mapView.invalidate();
				}
			}
		}
	}
}
