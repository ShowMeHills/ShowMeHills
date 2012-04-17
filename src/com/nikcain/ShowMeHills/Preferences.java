package com.nikcain.ShowMeHills;


import android.preference.PreferenceActivity;
import android.os.Bundle;

public class Preferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
/*
		Preference customPref = (Preference) findPreference("distance_reference");
		customPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange( Preference arg0, Object arg1) {
				SharedPreferences customSharedPreference = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		        SharedPreferences.Editor editor = customSharedPreference.edit();
		        editor.putFloat("distance", Float.valueOf(arg1.toString().trim()).floatValue());
		        editor.commit();
				return false;
			}
		});
		customPref = (Preference) findPreference("textsize_reference");
		customPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange( Preference arg0, Object arg1) {
				SharedPreferences customSharedPreference = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		        SharedPreferences.Editor editor = customSharedPreference.edit();
		        editor.putFloat("textsize", Float.valueOf(arg1.toString().trim()).floatValue());
		        editor.commit();
				return false;
			}
		});*/

	}
}

