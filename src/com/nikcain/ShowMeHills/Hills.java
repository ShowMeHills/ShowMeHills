package com.nikcain.ShowMeHills;

public class Hills {
	public Hills(int _id, String n, double lon, double lat, double ht) {
		id = _id;
		hillname = n;
		longitude = lon;
		latitude = lat;
		height = ht;
	}
	int id;
	String hillname;
	double longitude;
	double latitude;
	double direction;
	double distance;
	double visualElevation; // vertical angle looking at peak
	double height;
}

