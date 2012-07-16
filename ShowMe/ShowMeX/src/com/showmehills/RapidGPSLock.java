package com.showmehills;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.Criteria;

public class RapidGPSLock {

	private ShowMeHillsActivity mixContext;
	private LocationManager mLocationManager;
	private Location curLoc;
	private String bestLocationProvider;
	private Location locationAtLastDownload;	
	private LocationFinderState state;
	private final LocationObserver lob;
	private List<LocationResolver> locationResolvers;
	private final long freq = 5000; // 5 seconds
	private final float dist = 20; // 20 meters

	public enum LocationFinderState {
		Active, // Providing Location Information
		Inactive, // No-Active
		Confused // Same problem in internal state
	}
	
	public RapidGPSLock(ShowMeHillsActivity mixContext) {
		this.mixContext = mixContext;
		this.lob = new LocationObserver(this);
		this.state = LocationFinderState.Inactive;
		this.locationResolvers = new ArrayList<LocationResolver>();
	}
	
	public void findLocation() 
	{
		// fallback for the case where GPS and network providers are disabled
		Location hardFix = new Location("reverseGeocoded");

		// Frangart, Eppan, Bozen, Italy
		hardFix.setLatitude(46.480302);
		hardFix.setLongitude(11.296005);
		hardFix.setAltitude(300);

		try {
			requestBestLocationUpdates();
			//temporary set the current location, until a good provider is found
			curLoc = mLocationManager.getLastKnownLocation(mLocationManager.getBestProvider(new Criteria(), true));
		} catch (Exception ex2) {
			// ex2.printStackTrace();
			curLoc = hardFix;
		}
	}
	
	public void locationCallback(String provider) 
	{
		Location foundLocation = mLocationManager.getLastKnownLocation(provider);
		if (bestLocationProvider != null) 
		{
			Location bestLocation = mLocationManager.getLastKnownLocation(bestLocationProvider);
			if (foundLocation.getAccuracy() < bestLocation.getAccuracy()) 
			{
				curLoc = foundLocation;
				bestLocationProvider = provider;
			}
		} else 
		{
			curLoc = foundLocation;
			bestLocationProvider = provider;
		}
		setLocationAtLastDownload(curLoc);	
		mixContext.UpdateMarkers();
	}
	
	private void requestBestLocationUpdates() 
	{
		for (String p : mLocationManager.getAllProviders()) 
		{
			if(mLocationManager.isProviderEnabled(p))
			{
				LocationResolver lr = new LocationResolver(mLocationManager, p, this);
				locationResolvers.add(lr);
				mLocationManager.requestLocationUpdates(p, 0, 0, lr);
			}
		}
	}
	
	public void setLocationAtLastDownload(Location locationAtLastDownload) 
	{
		this.locationAtLastDownload = locationAtLastDownload;
	}
	
	public void setPosition(Location location) {
		synchronized (curLoc) 
		{
			curLoc = location;
		}
		Location lastLoc = getLocationAtLastDownload();
		if (lastLoc == null) 
		{
			setLocationAtLastDownload(location);
		}
	}	
	
	public Location getCurrentLocation() {
		if (curLoc == null) {
			return null;
			//throw new RuntimeException("No GPS Found");
		}
		synchronized (curLoc) {
			return curLoc;
		}
	}
	
	public Location getLocationAtLastDownload() 
	{
		return locationAtLastDownload;
	}

	public void switchOn() {
		if (!LocationFinderState.Active.equals(state)) {
			mLocationManager = (LocationManager) mixContext.getSystemService(Context.LOCATION_SERVICE);
			state = LocationFinderState.Confused;
		}
	}
	
	public void switchOff() {
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(getObserver());
			state = LocationFinderState.Inactive;
		}
	}
	
	public LocationFinderState getStatus() {
		return state;
	}

	private synchronized LocationObserver getObserver() {
		return lob;
	}
	public void RenewLocation() {
		
		if(bestLocationProvider != null)
		{
			//remove all location updates
		/*	for(LocationResolver locationResolver: locationResolvers)
			{
				mLocationManager.removeUpdates(locationResolver);
			}
			*/
			mLocationManager.removeUpdates(getObserver());
			state=LocationFinderState.Confused;
			requestBestLocationUpdates();
			
			state=LocationFinderState.Active;
		}
		else
		{ //no location found
			
		}
	}
}
