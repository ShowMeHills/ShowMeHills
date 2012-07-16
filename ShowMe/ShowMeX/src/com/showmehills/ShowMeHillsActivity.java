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

/*
 * Main Activity. Implements a camera preview with an overlay. Listens for orientation changes and 
 * asks HillDatabase for hills that are in view. It then draws HillOverlayItem in the appropriate place.
 * The 'appropriate place' is dependent on two calibration factors - the first gives the field of view 
 * and the second adjusts the compass direction.
 * Tapping on the HillOverlayItem opens the HillInfo activity.
 * 
 * The field of view calibration seemed necessary as the hardware parameters of the phone didn't match 
 * reality. This approach seemed more failsafe for any camera, even though it introduces the element of
 * human error.
 * 
 * The compass adjustment is necessary since the compass on my HTC Desire HD is temperamental to the point
 * of unusable. Even with the adjustment it rarely works well. 
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import com.showmehills.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.graphics.*;
import android.hardware.*;
import android.location.*;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public class ShowMeHillsActivity extends Activity implements IShowMeHillsActivity, SensorEventListener, OnTouchListener {

	public float hfov = (float) 50.2;
	public float vfov = (float) 20.0;
	private SensorManager mSensorManager;
	private RapidGPSLock mGPS;
	private PowerManager.WakeLock wl;
	Sensor accelerometer;
	Sensor magnetometer;  
	float[] mGravity;
	float[] mGeomagnetic;

	Timer timer = new Timer();
	private int GPSretryTime = 60;
	private int CompassSmoothingWindow = 50;
	
	//private Location curLocation;
	private String acc = "";
	private boolean badsensor = false;
	private boolean isCalibrated = false;
	private double calibrationStep = -1;
	private float compassAdjustment = 0;
	private ArrayList<HillMarker> mMarkers = new ArrayList<HillMarker>();

	float mRotationMatrixA[] = new float[9];
	float mRotationMatrixB[] = new float[9];
	float mOrientationVector[] = new float[9];
	float mAzimuthVector[] = new float[4];
	float mDeclination = 0;

	public int scrwidth = 10;
	public int scrheight = 10;
	private int mMainTextSize = 20;
	public static CameraPreviewSurface cv;
	public DrawOnTop mDraw;
	private HillDatabase myDbHelper;
	private filteredDirection fd = new filteredDirection();
	private filteredElevation fe = new filteredElevation();

	// preferences
	Float maxdistance = 30f;
	Float textsize = 25f;
	boolean showdir = false;
	boolean showdist = false;
	boolean typeunits = false; // true for metric, false for imperial
	boolean showheight = false;
	boolean showhelp = true;
	
	public class HillMarker
	{
		public HillMarker(int id, Rect loc) { location = loc; hillid=id; }
		public Rect location;
		public int hillid;
	}
	

	public int GetRotation()
	{
		Display display = getWindowManager().getDefaultDisplay(); 
		int rot = display.getRotation();
		return rot;
	}

	private void getPrefs() {
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		maxdistance = Float.parseFloat(prefs.getString("distance", ""+maxdistance));
		textsize = Float.parseFloat(prefs.getString("textsize", ""+textsize));

		showdir = prefs.getBoolean("showdir", false);
		showdist = prefs.getBoolean("showdist", false);
		showheight = prefs.getBoolean("showheight", false);
		typeunits = prefs.getString("distunits", "metric").equalsIgnoreCase("metric");
		isCalibrated = prefs.getBoolean("isCalibrated", false);
		hfov = prefs.getFloat("hfov", (float) 50.2);
		compassAdjustment = prefs.getFloat("compassAdjustment", 0);
		showhelp = prefs.getBoolean("showhelp", true);
	}

	@Override
	protected void onResume() {
		Log.d("showmehills", "onResume");
		super.onResume();

		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);	 
		mGPS.switchOn();
		getPrefs();
		wl.acquire();
		timer = new Timer();
		timer.scheduleAtFixedRate(new LocationTimerTask(),GPSretryTime* 1000,GPSretryTime* 1000);
		UpdateMarkers();
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
		timer.cancel();
		mGPS.switchOff(); 
		mSensorManager.unregisterListener(this);
		wl.release();
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
			mGPS.switchOff(); 
			mSensorManager.unregisterListener(this);
			//wl.release();
			myDbHelper.close();	 
		}catch(SQLException sqle){	 
			throw sqle;	 
		}
		super.onStop();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {  

		super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Tag");
        //wl.acquire();
        mGPS = new RapidGPSLock(this);
        mGPS.switchOn();
        mGPS.findLocation();
		timer.scheduleAtFixedRate(new LocationTimerTask(),GPSretryTime* 1000,GPSretryTime* 1000);

		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		myDbHelper = new HillDatabase(this); 
		try { 
			myDbHelper.createDataBase(); 
		} catch (IOException ioe) {	 
			throw new Error("Unable to create database");	 
		}	 
	
		Display display = getWindowManager().getDefaultDisplay(); 
		scrwidth = display.getWidth();
		scrheight = display.getHeight();

		cv = new CameraPreviewSurface( this.getApplicationContext(), this);
		FrameLayout rl = new FrameLayout( this.getApplicationContext());		
		setContentView(rl);
		
		mDraw = new DrawOnTop(this);        
		addContentView(mDraw, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		rl.addView(cv);            
        cv.setOnTouchListener((OnTouchListener) this); 
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		if (prefs.getBoolean("showhelp", true))
		{
			Intent myHelpIntent = new Intent(getBaseContext(), Help.class);
			startActivityForResult(myHelpIntent, 0);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate menu from XML resource
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.preferences_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences.Editor editor = prefs.edit();
		if (item.getItemId() == R.id.preferences_menutitem) {
			Intent settingsActivity = new Intent(getBaseContext(),AppPreferences.class);
			startActivity(settingsActivity);
		} else if (item.getItemId() == R.id.mapoverlay) {
			Location curLocation = mGPS.getCurrentLocation();
			myDbHelper.SetDirections(curLocation);
			editor.putFloat("longitude", (float)curLocation.getLongitude());
			editor.putFloat("latitude", (float)curLocation.getLatitude());
			editor.commit();
			Intent myIntent = new Intent(getBaseContext(), MapOverlay.class);
			startActivityForResult(myIntent, 0);
		} else if (item.getItemId() == R.id.help) {
			Intent myHelpIntent = new Intent(getBaseContext(), Help.class);
			startActivityForResult(myHelpIntent, 0);
		} else if (item.getItemId() == R.id.about) {
			Intent myAboutIntent = new Intent(getBaseContext(), About.class);
			startActivityForResult(myAboutIntent, 0);
		} else if (item.getItemId() == R.id.exit) {
			finish();
		} else if (item.getItemId() == R.id.fovcalibrate) {
			calibrationStep = -1;
			isCalibrated = false;
			editor.putBoolean("isCalibrated", false);
			editor.commit();
		}
		return super.onOptionsItemSelected(item);
	}

	public void UpdateMarkers()
	{
		Location curLocation = mGPS.getCurrentLocation();
		if (curLocation != null)
		{
			myDbHelper.SetDirections(curLocation);
		}
	}
	
	class filteredDirection
	{
		double dir;
		double sinevalues[] = new double[CompassSmoothingWindow];
		double cosvalues[] = new double[CompassSmoothingWindow];
		int index = 0;
		int outlierCount = 0;
		
		void AddLatest( double d )
		{
			sinevalues[index] = Math.sin(d);
			cosvalues[index] = Math.cos(d);
			index++;
			if (index > CompassSmoothingWindow - 1) index = 0;
			double sumc = 0; 
			double sums = 0;
			for (int a = 0; a < CompassSmoothingWindow; a++) 
			{
				sumc += cosvalues[a];
				sums += sinevalues[a];
			}
			dir = Math.atan2(sums/CompassSmoothingWindow,sumc/CompassSmoothingWindow);
		}
		
		double getDirection() 
		{ 
			double ret = (dir<0)?360+Math.toDegrees(dir):Math.toDegrees(dir); 
			ret += compassAdjustment;
			if (ret < 0) ret = 360-ret;
			if (ret > 360) ret = ret - 360;
			return ret;
		}

		int GetVariation()
		{
			double Q = 0;
			double sumc = 0; 
			double sums = 0;
			for (int a = 0; a < CompassSmoothingWindow; a++)
			{
				sumc += cosvalues[a];
				sums += sinevalues[a];
			}
			double avgc = sumc/CompassSmoothingWindow;
			double avgs = sums/CompassSmoothingWindow;

			sumc = 0; 
			sums = 0;
			for (int a = 0; a < CompassSmoothingWindow; a++)
			{
				sumc += Math.pow(cosvalues[a] - avgc, 2);
				sums += Math.pow(sinevalues[a] - avgs, 2);
			}
			Q = (sumc/(CompassSmoothingWindow-1)) + (sums/(CompassSmoothingWindow-1));
			
			return (int)(Q*1000);
		}
	}
	
	class filteredElevation
	{
		int AVERAGINGWINDOW = 10;
		double dir;
		double sinevalues[] = new double[AVERAGINGWINDOW];
		double cosvalues[] = new double[AVERAGINGWINDOW];
		int index = 0;
		void AddLatest( double d )
		{
			sinevalues[index] = Math.sin(d);
			cosvalues[index] = Math.cos(d);
			index++;
			if (index > AVERAGINGWINDOW - 1) index = 0;
			double sumc = 0; 
			double sums = 0;
			for (int a = 0; a < AVERAGINGWINDOW; a++) 
			{
				sumc += cosvalues[a];
				sums += sinevalues[a];
			}
			dir = Math.atan2(sums/AVERAGINGWINDOW,sumc/AVERAGINGWINDOW);
		}
		double getDirection() { return dir; }
	}

	class DrawOnTop extends View {

		private Paint strokePaint = new Paint();
		private Paint textPaint = new Paint();
		private Paint paint = new Paint();
		private Paint transpRedPaint = new Paint();
		private Paint variationPaint = new Paint();
		
		public DrawOnTop(Context context) {     
			super(context);      

			textPaint.setARGB(255, 255, 255, 255);
			textPaint.setTextAlign(Paint.Align.CENTER);
			textPaint.setTypeface(Typeface.DEFAULT_BOLD);

			strokePaint.setARGB(255, 0, 0, 0);
			strokePaint.setTextAlign(Paint.Align.CENTER);
			strokePaint.setTypeface(Typeface.DEFAULT_BOLD);
			strokePaint.setStyle(Paint.Style.STROKE);
			strokePaint.setStrokeWidth(2);

			paint.setARGB(255, 255, 255, 255);
			transpRedPaint.setARGB(100,255,0,0);
		}

		@Override     
		protected void onDraw(Canvas canvas) { 
			if (!isCalibrated)
			{
				// adjust text to fit any screen - lol, so hacky :-D
				boolean happyWithSize = false;
				do
				{
					textPaint.setTextSize(mMainTextSize);
					float sz = textPaint.measureText("screen, wait for stabilisation, and tap again.");
					if (sz > scrwidth*0.7 )
					{
						mMainTextSize--;
					}
					else if (sz < scrwidth*0.6)
					{
						mMainTextSize++;
					}
					else
					{
						happyWithSize = true;
					}
				} while (!happyWithSize);
				
				textPaint.setTextAlign(Paint.Align.LEFT);
				textPaint.setARGB(255, 255, 255, 255);				
				paint.setARGB(100, 0, 0, 0);

				int subwidth = (int)(scrwidth*0.7);
				int subheight = (int)(scrheight*0.7);
				int gap = (scrwidth - subwidth) / 2;
				int txtgap = gap+(subwidth/30);
				int vtxtgap = (int)(subheight / 10);
				// left, top, right, bottom
				canvas.drawRoundRect(new RectF(gap,vtxtgap,scrwidth-gap,vtxtgap*11), 50,50,paint);
				canvas.drawText( "To calibrate, view an object at the very", txtgap, vtxtgap*3, textPaint);
				canvas.drawText( "left edge of the screen, and wait for", txtgap, vtxtgap*4, textPaint);
				canvas.drawText( "the direction sensor to stabilise. Then", txtgap, vtxtgap*5, textPaint);
				canvas.drawText( "tap the screen (gently, so you don't move", txtgap, vtxtgap*6, textPaint);
				canvas.drawText( "the view!). Then turn around until the ", txtgap, vtxtgap*7, textPaint);
				canvas.drawText( "object is at the very right edge of the ", txtgap, vtxtgap*8, textPaint);
				canvas.drawText( "screen, wait for stabilisation, and tap again.", txtgap, vtxtgap*9, textPaint);
				
				canvas.drawText( "SD: "+fd.GetVariation(), 200, 350, textPaint);

				textPaint.setTextAlign(Paint.Align.CENTER);
				if (calibrationStep == -1)
				{
					canvas.drawRect(0,0, 10, scrheight, transpRedPaint);
				}
				else
				{
					canvas.drawRect(scrwidth-10,0, scrwidth, scrheight, transpRedPaint);
				}
				int va = fd.GetVariation();
				variationPaint.setARGB(255, 255, 0, 0);
				variationPaint.setStrokeWidth(4);
				for (int i = 0; i < 360; i+=15)
				{
					if (i > va) variationPaint.setARGB(255, 0, 255, 0);
					canvas.drawLine(70.0f+(15.0f*(float)Math.sin( Math.toRadians(i))),  
									380.0f-(15.0f*(float)Math.cos( Math.toRadians(i))),  
									70.0f+(50.0f*(float)Math.sin( Math.toRadians(i))), 
									380.0f-(50.0f*(float)Math.cos( Math.toRadians(i))), 
									variationPaint);
				}
				return;
			}
			int toppt = (int) (scrheight/1.6);

			ArrayList<Hills> localhills = myDbHelper.localhills;
			int alpha = 255;
			Float drawtextsize = textsize;

			class tmpHill {
				Hills h;
				double ratio;
				int toppt;
			};
			ArrayList<tmpHill> hillstoplot = new ArrayList<tmpHill>();
			mMarkers.clear();
			for (int h = 0; h < localhills.size() && drawtextsize > 5 && toppt > 0; h++)
			{
				Hills h1 = localhills.get(h);

				// this is the angle of the peak from our line of sight
				double offset = fd.getDirection() - h1.direction;
				double offset2 = fd.getDirection() - (360+h1.direction);
				double offset3 = 360+fd.getDirection() - (h1.direction);
				double ratio = 0;
				// is it in our line of sight
				boolean inlineofsight=false;
				if (Math.abs(offset) * 2 < hfov)
				{
					ratio = offset / hfov * -1;
					inlineofsight = true;
				}
				if (Math.abs(offset2) * 2 < hfov)
				{
					ratio = offset2 / hfov * -1;
					inlineofsight = true;
				}
				if (Math.abs(offset3) * 2 < hfov)
				{
					ratio = offset3 / hfov * -1;
					inlineofsight = true;
				}
				if (inlineofsight)
				{
					tmpHill th = new tmpHill();

					th.h = h1;
					th.ratio = ratio;
					th.toppt = toppt;
					hillstoplot.add(th);

					toppt -= (showdir || showdist)?drawtextsize*2:drawtextsize;
					drawtextsize -= 1;
				}
			}
			drawtextsize = textsize;
			// draw lines first
			for (int i = 0; i < hillstoplot.size(); i++)
			{
				textPaint.setARGB(alpha, 255, 255, 255);				
				strokePaint.setARGB(alpha, 0, 0, 0);
				tmpHill th = hillstoplot.get(i);
				double vratio = Math.toDegrees(th.h.visualElevation - fe.getDirection());
				int yloc = (int)((scrheight * vratio / vfov) + (scrheight/2));
				int xloc = ((int)(scrwidth * th.ratio) + (scrwidth/2));
				canvas.drawLine(xloc, yloc, xloc, th.toppt - toppt, strokePaint);
				canvas.drawLine(xloc, yloc, xloc, th.toppt - toppt, textPaint);
				canvas.drawLine(xloc-20, th.toppt - toppt, xloc+20, th.toppt - toppt, strokePaint);
				canvas.drawLine(xloc-20, th.toppt - toppt, xloc+20, th.toppt - toppt, textPaint);
				alpha -= 10;
			}
			alpha = 255;
			// draw text over top
			for (int i = 0; i < hillstoplot.size(); i++)
			{
				textPaint.setARGB(alpha, 255, 255, 255);				
				strokePaint.setARGB(alpha, 0, 0, 0);

				textPaint.setTextSize(drawtextsize);
				strokePaint.setTextSize(drawtextsize);
				
				tmpHill th = hillstoplot.get(i);
				int xloc = ((int)(scrwidth * th.ratio) + (scrwidth/2));
				
				Rect bnds = new Rect();
				strokePaint.getTextBounds(th.h.hillname,0,th.h.hillname.length(),bnds);
				bnds.left += xloc - (textPaint.measureText(th.h.hillname) / 2.0);
				bnds.right += xloc - (textPaint.measureText(th.h.hillname) / 2.0);
				bnds.top += ((showdir || showdist)?th.toppt-drawtextsize-5:th.toppt-5) - toppt;
				bnds.bottom += th.toppt-5 - toppt;				

				// draws bounding box of touch region to select hill
				//canvas.drawRect(bnds, strokePaint);
				
				mMarkers.add(new HillMarker(th.h.id, bnds));
				canvas.drawText(th.h.hillname, xloc, ((showdir || showdist)?th.toppt-drawtextsize-5:th.toppt-5) - toppt, strokePaint);
				canvas.drawText(th.h.hillname, xloc, ((showdir || showdist)?th.toppt-drawtextsize-5:th.toppt-5) - toppt, textPaint);
				
				if (showdir || showdist || showheight) 
				{
					String marker = " (";						
					if (showdir) marker += " " + Math.floor(10*th.h.direction)/10 + (char) 0x00B0;
					if (showdist) 
					{
						double multip = (typeunits)?1:0.621371;
						marker += " " + Math.floor(10*th.h.distance*multip)/10;
						if (typeunits) marker += "km"; else marker += "miles";
					}
					if (showheight) 
					{
						double multip = (typeunits)?1:3.2808399;
						marker += " " + th.h.height * multip;
						if (typeunits) marker += "m"; else marker += "ft";
					}
					marker += ")";
					canvas.drawText(marker, xloc, th.toppt-5 - toppt, strokePaint);
					canvas.drawText(marker, xloc, th.toppt-5 - toppt, textPaint);					
				}

				alpha -= 10;
				drawtextsize -= 1;
			}

			textPaint.setTextSize(mMainTextSize);
			strokePaint.setTextSize(mMainTextSize);
			textPaint.setARGB(255, 255, 255, 255);				
			strokePaint.setARGB(255, 0, 0, 0);
			
			String compadj = (compassAdjustment>=0)?"+":"";
			compadj += String.format("%.01f", compassAdjustment);
			
			String basetext = "" + (int)fd.getDirection() + (char)0x00B0;
			basetext +=" (adj:"+compadj+")";
			basetext +=" FOV: "+String.format("%.01f", hfov);
			basetext +=" Location " + acc;
			canvas.drawText( basetext, scrwidth/2, scrheight-70, strokePaint);
			canvas.drawText( basetext, scrwidth/2, scrheight-70, textPaint);	
			if (badsensor)
			{
				canvas.drawText( "Recalibrate sensor!", 10, 80, paint);	
			}
			Location curLocation = mGPS.getCurrentLocation();
			if (curLocation != null)
			{
				acc = "+/- ";
				if (typeunits) 
				{
					acc += (int)curLocation.getAccuracy();
					acc+= "m"; 
				}
				else
				{
					acc+=(int)(curLocation.getAccuracy()*3.2808399);
					acc+="ft";
				}				
			}
			if (curLocation == null || curLocation.getAccuracy() > 200)
			{
				if (curLocation == null || acc=="") basetext = "No GPS position yet";
				else if (curLocation.getAccuracy() > 200) basetext = "Warning - GPS position too inaccurate";
				canvas.drawText( basetext, scrwidth/2, scrheight/2, strokePaint);
				canvas.drawText( basetext, scrwidth/2, scrheight/2, textPaint);	
			}
			
			int va = fd.GetVariation();
			variationPaint.setARGB(255, 255, 0, 0);
			variationPaint.setStrokeWidth(4);
			for (int i = 0; i < 360; i+=15)
			{
				if (i > va) variationPaint.setARGB(255, 0, 255, 0);
				canvas.drawLine(70.0f+(15.0f*(float)Math.sin( Math.toRadians(i))),  
								380.0f-(15.0f*(float)Math.cos( Math.toRadians(i))),  
								70.0f+(35.0f*(float)Math.sin( Math.toRadians(i))), 
								380.0f-(35.0f*(float)Math.cos( Math.toRadians(i))), 
								variationPaint);
			}
			super.onDraw(canvas);     
		}    
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	
	public void onSensorChanged(SensorEvent event) {
		/*
		 * this is preventing the compass working under certain conditions
		 if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
			return;
		}
		*/

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)  mGravity = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) mGeomagnetic = event.values;

		if (mGravity != null && mGeomagnetic != null) {

			float[] rotationMatrixA = mRotationMatrixA;
			if (SensorManager.getRotationMatrix(rotationMatrixA, null, mGravity, mGeomagnetic)) {
				Matrix tmpA = new Matrix();
				tmpA.setValues(rotationMatrixA);
				tmpA.postRotate( -mDeclination );
				tmpA.getValues(rotationMatrixA);
				
				float[] rotationMatrixB = mRotationMatrixB;
				switch (GetRotation())
				{
				// portrait - normal
				case Surface.ROTATION_0: SensorManager.remapCoordinateSystem(rotationMatrixA,
						SensorManager.AXIS_X, SensorManager.AXIS_Z,
						rotationMatrixB);
				break;
				// rotated left (landscape)
				case Surface.ROTATION_90: SensorManager.remapCoordinateSystem(rotationMatrixA,
						SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X,
						rotationMatrixB); 
				break;
				// upside down
				case Surface.ROTATION_180: SensorManager.remapCoordinateSystem(rotationMatrixA,
						SensorManager.AXIS_X, SensorManager.AXIS_Z,
						rotationMatrixB); 
				break;
				// rotated right (landscape)
				case Surface.ROTATION_270: SensorManager.remapCoordinateSystem(rotationMatrixA,
						SensorManager.AXIS_MINUS_Z, SensorManager.AXIS_X,
						rotationMatrixB); 
				break;

				default:  break;
				}

				float[] dv = new float[3]; 
				SensorManager.getOrientation(rotationMatrixB, dv);
				
				fd.AddLatest(dv[0]); 
				fe.AddLatest((double)dv[1]);
			}
			mDraw.invalidate();		
		}
	}

	public boolean onTouch(View v, MotionEvent event) {
		if (!isCalibrated)
		{
			if (calibrationStep == -1)
			{
				calibrationStep = fd.getDirection();

				Log.d("showmehills", "1st cal pt="+calibrationStep);
			}
			else
			{
				double curdir = fd.getDirection();
				if (calibrationStep - curdir < 0) calibrationStep += 360;
				hfov = (float)(calibrationStep - curdir);
				Log.d("showmehills", "2nd cal pt="+curdir);
				Log.d("showmehills", "Setting hfov calibration="+hfov);
				isCalibrated = true;
				calibrationStep = 0;
				SharedPreferences customSharedPreference = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		        SharedPreferences.Editor editor = customSharedPreference.edit();
		        editor.putFloat("hfov", hfov);
		        editor.putBoolean("isCalibrated", true);
		        editor.commit();
			}
			return false;
		}
		Iterator<HillMarker> itr = mMarkers.iterator();
	    while (itr.hasNext()) {
	    	HillMarker m = itr.next();
	    	if (m.location.contains((int)event.getX(), (int)event.getY()))
			{
	    		Intent infoActivity = new Intent(getBaseContext(),HillInfo.class);
	    		Bundle b = new Bundle();

	    		b.putInt("key", m.hillid);

	    		infoActivity.putExtras(b);
				startActivity(infoActivity);
			}
		}

		return false;
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		super.onKeyDown(keyCode, event);
        switch(keyCode)
        {
        case KeyEvent.KEYCODE_VOLUME_UP:
        	compassAdjustment+=0.1;
            return true;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        	compassAdjustment-=0.1;
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN  )
		{
			SharedPreferences customSharedPreference = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	        SharedPreferences.Editor editor = customSharedPreference.edit();
	        editor.putFloat("compassAdjustment", compassAdjustment);
	        editor.commit();
	      return true;
		}
	   return super.onKeyUp(keyCode, event);
	   }

	class LocationTimerTask extends TimerTask 
	{
		@Override
		public void run() 
		{
			Log.d("showmehills", "renew GPS search");
			runOnUiThread(new Runnable() {
				  public void run() {
					  mGPS.RenewLocation();
				  }
			});
		}
	}


	public LocationManager GetLocationManager() {
		return (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	}
}


