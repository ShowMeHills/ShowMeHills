package com.showmehills;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener; 

import com.showmehills.R;

public class Help extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    setContentView(R.layout.help);	    	    
	    
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
	    CheckBox cb = (CheckBox) findViewById(R.id.showhelp);
	    if (prefs.getBoolean("showhelp", true))
	    {
	    	cb.setChecked(true);
	    }
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		
			//@Override
			public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) {
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				SharedPreferences.Editor editor = prefs.edit();
				if (buttonView.isChecked()) {
					// 	Checked
					editor.putBoolean("showhelp", true);
				}
				else
				{
					editor.putBoolean("showhelp", false);
				
				} 
				editor.commit();
			}
		});
		
		Button btn = (Button) findViewById(R.id.okbtn);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		} );
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
	}
}
