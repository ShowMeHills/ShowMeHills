package com.nikcain.ShowMeHills;

import java.io.IOException;

import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MountainInfo extends Activity{

	private DataBaseHelper myDbHelper;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    setContentView(R.layout.mountaininfo);
	    
		Bundle b = getIntent().getExtras();
		int hillid = b.getInt("key", 0);		

		myDbHelper = new DataBaseHelper(this); 
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
			/*for (int i = 0; i < cursor.getColumnCount(); i++)
			{
				Log.d("showmehills", "col "+ i + ": " + cursor.getColumnName(i));
			}*/
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
