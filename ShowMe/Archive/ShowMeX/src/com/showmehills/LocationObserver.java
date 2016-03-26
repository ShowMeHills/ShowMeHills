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
    
    This source originated from mixare, another GPL project.
 */
package com.showmehills;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class LocationObserver implements LocationListener {

	private RapidGPSLock myController;
	
	public LocationObserver(RapidGPSLock myController) {
		super();
		this.myController=myController;
	}
	
	public void onLocationChanged(Location location) {
		try {
			Log.d("showmehills", "new location(ob) " + location.getAccuracy() + " " + location.getLatitude() + "," + location.getLongitude());
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
