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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

	public class HillDatabase extends SQLiteOpenHelper{
		private static String DB_PATH = "/data/data/com.showmehills/databases/";		 
	    private static String DB_NAME = "hills.db";	 
	    private SQLiteDatabase myDataBase; 	 
	    private final Context myContext;
	    
	    public ArrayList<Hills> localhills = new ArrayList<Hills>();

	    public HillDatabase(Context context) {	 
	    	super(context, DB_NAME, null, 1);
	        this.myContext = context;
	    }	
	 
	    public void createDataBase() throws IOException{	 
	    	boolean dbExist = checkDataBase();	 
	    	if(dbExist){
	    		//do nothing - database already exist
	    	}else{	 
	        	this.getReadableDatabase();	 
	        	try {	 
	    			copyDataBase();	 
	    		} catch (IOException e) {	 
	        		throw new Error("Error copying database");	 
	        	}
	    	}	 
	    }
	 
	    private boolean checkDataBase(){	 
	    	SQLiteDatabase checkDB = null;	 
	    	try{
	    		String myPath = DB_PATH + DB_NAME;
	    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);	 
	    	}catch(SQLiteException e){	 
	    		//database does't exist yet.	 
	    	}
	 
	    	if(checkDB != null){	 
	    		checkDB.close();	 
	    	}
	 
	    	return checkDB != null ? true : false;
	    }
	 
	    private void copyDataBase() throws IOException{	 
	    	InputStream myInput = myContext.getAssets().open(DB_NAME);
	    	String outFileName = DB_PATH + DB_NAME;
	    	OutputStream myOutput = new FileOutputStream(outFileName);
	    	byte[] buffer = new byte[1024];
	    	int length;
	    	while ((length = myInput.read(buffer))>0){
	    		myOutput.write(buffer, 0, length);
	    	}
	    	myOutput.flush();
	    	myOutput.close();
	    	myInput.close();
	 
	    }
	 
	    public void openDataBase() throws SQLException{
	        String myPath = DB_PATH + DB_NAME;
	    	myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);	 
	    }
	    	    
	    @Override
		public synchronized void close() {	 
	    	    if(myDataBase != null)
	    		    myDataBase.close();	 
	    	    super.close();	 
		}
	 
		@Override
		public void onCreate(SQLiteDatabase db) {}
	 
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
	 
		public void SetDirections(Location curLocation)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(myContext);
	        Float maxdistance = Float.parseFloat(prefs.getString("distance", "25"));
	        
			Log.d("showmehills", "new location: " + curLocation.getLongitude() +"," + curLocation.getLatitude() + " +/-" + curLocation.getAccuracy() + "m");
			localhills.clear();
			
			// use a rule of thumb for distance between lines of lat & long
			// 1 line of latitude = 111km
			// 1 line of longitude = sin(latitude)* 111km. 
			String qu = "select * from mountains where latitude between " +
			(curLocation.getLatitude() - (maxdistance/111.0 )) + " and "+(curLocation.getLatitude() + (maxdistance/111.0 ))
			+" and longitude between "+
			(curLocation.getLongitude() - (maxdistance/(111.0 * Math.sin(curLocation.getLatitude() * Math.PI / 180))))+" and " +
			(curLocation.getLongitude() + (maxdistance/(111.0 * Math.sin(curLocation.getLatitude() * Math.PI / 180))));
			Log.d("showmehills", qu);
			Cursor cursor = getReadableDatabase().rawQuery( qu, null);
	        
			if(cursor.moveToFirst()) {
	        	do {
	        		Hills h = new Hills( 
	        				cursor.getInt(cursor.getColumnIndex("_id")),
	        				cursor.getString(cursor.getColumnIndex("Name")),
	        				cursor.getDouble(cursor.getColumnIndex("Longitude")),
	        				cursor.getDouble(cursor.getColumnIndex("Latitude")),
	        				cursor.getDouble(cursor.getColumnIndex("Metres")));
	        		//Log.d("showmehills", "Adding " + h.hillname + "@"+h.longitude+","+h.latitude);
	        		localhills.add(h);
	        	} while (cursor.moveToNext());
	        }
	        
/*
			localhills.add(new Hills(0,"London Eye",   -0.119700, 51.5033,   135));
			localhills.add(new Hills(0,"Shard",        -0.086667, 51.504444, 308));
			localhills.add(new Hills(0,"1 Canada Sq",  -0.019611, 51.505, 240));
			localhills.add(new Hills(0,"BT Tower",     -0.138900, 51.5215, 191));
			localhills.add(new Hills(0,"Gherkin",      -0.080278, 51.514444, 180));
			*/
			for (int h = 0; h < localhills.size(); h++)
			{
				Hills h1 = localhills.get(h);

				double dLat = Math.toRadians(h1.latitude - curLocation.getLatitude()); 
				double dLon =  Math.toRadians(h1.longitude - curLocation.getLongitude()); 
				double lat1 = Math.toRadians(curLocation.getLatitude());
				double lat2 = Math.toRadians(h1.latitude);
				
				// direction calculation
				double y = Math.sin(dLon) * Math.cos(lat2);
				double x = Math.cos(lat1)*Math.sin(lat2) -
				        Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
				double brng = Math.atan2(y, x)  * 180 / Math.PI;
				
				h1.direction = (brng<0)?brng+360:brng;
				
				// distance calculation				
				double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
				        Math.cos(lat2) * Math.cos(lat1) * 
				        Math.sin(dLon/2) * Math.sin(dLon/2); 
				double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
				h1.distance = Math.floor(10 * 6371 * c) / 10.0; // Distance in km
				
				// vertical angle
				double dheight = h1.height - curLocation.getAltitude();
				
				h1.visualElevation = Math.atan2(dheight, h1.distance*1000);
				
				localhills.set(h, h1);
			}
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
