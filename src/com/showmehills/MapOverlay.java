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
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.showmehills.R;

public class MapOverlay  extends MapActivity {
	
	private HillDatabase myDbHelper;
	private Location curLocation;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
        Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
        
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
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
        case R.id.preferences_menutitem:
        	Intent settingsActivity = new Intent(getBaseContext(),AppPreferences.class);
        	startActivity(settingsActivity);
            break;
        case R.id.cameraview:
        	finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
