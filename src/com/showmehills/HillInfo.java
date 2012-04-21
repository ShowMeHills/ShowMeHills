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

import com.showmehills.R;

import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class HillInfo extends Activity{

	private HillDatabase myDbHelper;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    setContentView(R.layout.mountaininfo);
	    
		Bundle b = getIntent().getExtras();
		int hillid = b.getInt("key", 0);		

		myDbHelper = new HillDatabase(this); 
		try { 
        	myDbHelper.createDataBase(); 
	 	} catch (IOException ioe) {	 
	 		throw new Error("Unable to create database");	 
	 	}	 
	 
		String qu = "select * from mountains where _id = '"+hillid+"'";
		Log.d("showmehills", "query: "+qu);
		Cursor cursor = myDbHelper.getReadableDatabase().rawQuery( qu, null);
		if (cursor.moveToFirst())
		{
			TextView t = (TextView) findViewById(R.id.hillname);
			String n = cursor.getString(cursor.getColumnIndex("Name"));
			t.setText(n);
			
			t = (TextView) findViewById(R.id.hillheight);
			double h = cursor.getDouble(cursor.getColumnIndex("Metres"));
			t.setText(""+h);
			
			t = (TextView) findViewById(R.id.infolink);
			n = cursor.getString(cursor.getColumnIndex("Hillbagging"));
			t.setText("web: "+n);
			
		}
		else
		{
			Log.d("showmehills", "zero item count.");
		}
		cursor.close();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		try {	 
			myDbHelper.openDataBase();	 
		}catch(SQLException sqle){	 
			throw sqle;	 
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		
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
