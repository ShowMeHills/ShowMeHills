package com.showmehills;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class LocationObserver implements LocationListener {

	private RapidGPSLock myController;
	
	public LocationObserver(RapidGPSLock myController) {
		super();
		this.myController=myController;
	}
	
	public void onLocationChanged(Location location) {
		try {			
			myController.setPosition(location);
		} catch (Exception ex) 
		{
			ex.printStackTrace();
		}
	}

	public void onProviderDisabled(String provider) {		
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

}
