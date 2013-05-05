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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

	public class HillDatabase extends SQLiteOpenHelper{
		private static String DB_PATH;// = "/data/data/com.showmehills.showmehillsuk/databases/";		 
	    private static String DB_NAME;	
	    private static int mDatabaseVersion = 9;
	    private SQLiteDatabase myDataBase; 	 
	    private final Context myContext;
	    private boolean mDbCopied = false;
	    public ArrayList<Hills> localhills = new ArrayList<Hills>();

	    public HillDatabase(Context context, String dbname, String dbpath) {
	    	super(context, dbname, null, 1);
	        DB_NAME = dbname;
	        DB_PATH = dbpath;
	        this.myContext = context;
	    }	
	 
	    public void createDataBase(){
	    	// made some changes in the database, but need to update it in existing installs!
	    	// so need to add a version number
	    	// for now just update db every time
	    	boolean dbExist = checkDataBase();	 
	    	if(dbExist){
	    		//do nothing - database already exist
	    	}else{	 
	        	this.getReadableDatabase();	 
	        	copyDataBase();
	    	}	 
	    }
	 
	    public boolean checkDataBase(){
	    	if (!mDbCopied) return false;
	    	if (myDataBase != null)
	    	{
	    		// already ok
	    		return true;
	    	}
	    	try{
	    		String myPath = DB_PATH + DB_NAME;
	    		
	    		try {
	    	    	myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
	    		}
    	    	catch(Exception e){	 
    	    		return false;	 
    	    	}
	    		if (myDataBase == null)
	    		{
	    			return false;
	    		}
	    		
				String qu = "select ver from dbversions limit 1";				
				Cursor cursor = getReadableDatabase().rawQuery( qu, null);
				if(cursor.moveToFirst()) {
					if (cursor.getInt(0) != mDatabaseVersion)
					{
						Log.d("showmehills", "Old database ("+cursor.getInt(0)+"). Updating!");
						myDataBase.close();
						myDataBase = null;
						if (myContext.deleteDatabase(DB_NAME))
						{
							Log.d("showmehills", "Deleted old database " + myContext.getDatabasePath(DB_NAME));
						}
						else
						{
							Log.d("showmehills", "Failed to delete old database!");
						}
						return false;
					}
				}				
	    	}catch(SQLiteException e){	 
	    		//database does't exist yet.
	    		e.printStackTrace();
	    	}
	 
	    	return myDataBase != null ? true : false;
	    }
	 
	    private void copyDataBase() {	 
	    	Log.d("showmehills", "Attempting to copy database " + DB_NAME + " from assets to " + DB_PATH + DB_NAME);
	    	InputStream myInput;
			try {
				myInput = myContext.getAssets().open(DB_NAME);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
	    	String outFileName = DB_PATH + DB_NAME;
	    	OutputStream myOutput;
	    	long bytesCopied = 0;
			try {
				myOutput = new FileOutputStream(outFileName);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
		    	try {
					myInput.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		    	return;
			}
	    	byte[] buffer = new byte[1024];
	    	int length;
	    	try {
				while ((length = myInput.read(buffer))>0){
					myOutput.write(buffer, 0, length);
					bytesCopied += length;
				}
			} catch (IOException e) {
				e.printStackTrace();

		    	try {
					myInput.close();
			    	myOutput.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		    	return;
			}
	    	try {
				myOutput.flush();
		    	myOutput.close();
		    	myInput.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    	mDbCopied = true;
	    	Log.d("showmehills", "Database copied successfully (" + bytesCopied + " bytes), attempting to check database again...");
	    	// should be created, so now open
	    	checkDataBase();
	    }
	 
	    @Override
		public synchronized void close() {	 
	    	    if(myDataBase != null)
	    		    myDataBase.close();
	    	    myDataBase = null;
	    	    super.close();	 
		}
	 
		@Override
		public void onCreate(SQLiteDatabase db) {}
	 
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
	 
		public void SetDirections(Location curLocation)
		{
			if (curLocation == null) return;
			
			if (myDataBase == null)
			{
				createDataBase();
				if (myDataBase == null) return;
			}
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(myContext);
			String md = prefs.getString("distance", "25");
			if (md == "") md = "25";
	        Float maxdistance = Float.parseFloat(md);
	        
			md = prefs.getString("mindistance", "0");
			if (md == "") md = "0";
	        Float mindistance = Float.parseFloat(md);
	        
			localhills.clear();
			
			double curLatitude = curLocation.getLatitude();
			double curLongitude = curLocation.getLongitude();
			
			// use a rule of thumb for distance between lines of lat & long
			// 1 line of latitude = 111km
			// 1 line of longitude = sin(latitude)* 111km. 
			String qu = "select * from mountains where latitude between " +
			(curLatitude - (maxdistance/111.0 )) + " and " + (curLatitude + (maxdistance/111.0 ))
			+ " and longitude between " +
			(curLongitude - (maxdistance/(111.0 * Math.sin(curLatitude * Math.PI / 180)))) + " and " +
			(curLongitude + (maxdistance/(111.0 * Math.sin(curLatitude * Math.PI / 180))));
			
			Cursor cursor;
			try {
				cursor = getReadableDatabase().rawQuery( qu, null);
			}	
	    	catch(SQLiteException e){	 
	    		return;	 
	    	}
	        if (cursor == null) return;
	        
			int tooNear = 0, tooFar = 0;
			if(cursor.moveToFirst()) {
				double dLat, dLon, lat1, lat2;
				double x, y, brng, a, c, dheight;
				Hills h;
				
	        	do {
	        		try {
		        		h = new Hills( 
		        				cursor.getInt(cursor.getColumnIndex("_id")),
		        				cursor.getString(cursor.getColumnIndex("name")),
		        				cursor.getDouble(cursor.getColumnIndex("longitude")),
		        				cursor.getDouble(cursor.getColumnIndex("latitude")),
		        				cursor.getDouble(cursor.getColumnIndex("height")));
		        		
						dLat = Math.toRadians(h.latitude - curLatitude); 
						dLon =  Math.toRadians(h.longitude - curLongitude); 
						lat1 = Math.toRadians(curLatitude);
						lat2 = Math.toRadians(h.latitude);
						
						// direction calculation
						y = Math.sin(dLon) * Math.cos(lat2);
						x = Math.cos(lat1)*Math.sin(lat2) -
						        Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
						brng = Math.atan2(y, x)  * 180 / Math.PI;
						
						h.direction = (brng<0)?brng+360:brng;
						
						// distance calculation				
						a = Math.sin(dLat/2) * Math.sin(dLat/2) +
						        Math.cos(lat2) * Math.cos(lat1) * 
						        Math.sin(dLon/2) * Math.sin(dLon/2); 
						c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
						h.distance = Math.floor(10 * 6371 * c) / 10.0; // Distance in km
	
						// vertical angle
						dheight = h.height - curLocation.getAltitude();
						
						h.visualElevation = Math.atan2(dheight, h.distance*1000);
		        		
						if (h.distance > maxdistance)
						{
							tooFar++;
						}
						else if (h.distance < mindistance)
						{
							tooNear++;
						}
						else
						{
			        		//Log.d("showmehills", "Adding " + h.hillname + "@"+h.longitude+","+h.latitude);
			        		localhills.add(h);
						}
					} catch(Exception e)
					{
						Log.e("showmehills", "bad database read: " + e.getMessage());
					}			
	        	} while (cursor.moveToNext());
	        }
	        Log.d("showmehills", "Added " + localhills.size() + " markers; skipped " + tooNear + " too near, " + tooFar + " too far.");
/*
 * for testing:
			localhills.add(new Hills(0,"London Eye",   -0.119700, 51.5033,   135));
			localhills.add(new Hills(0,"Shard",        -0.086667, 51.504444, 308));
			localhills.add(new Hills(0,"1 Canada Sq",  -0.019611, 51.505, 240));
			localhills.add(new Hills(0,"BT Tower",     -0.138900, 51.5215, 191));
			localhills.add(new Hills(0,"Gherkin",      -0.080278, 51.514444, 180));
			*/

			Collections.sort(localhills, new Comparator<Object>(){
				 
	            public int compare(Object o1, Object o2) {
	                Hills p1 = (Hills) o1;
	                Hills p2 = (Hills) o2;
	               if (p1.distance==p2.distance) return 0;
	               if (p1.distance < p2.distance) return -1;
	               return 1;
	            }
	 
	        });
		}
	}
