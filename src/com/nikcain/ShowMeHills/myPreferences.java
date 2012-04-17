package com.nikcain.ShowMeHills;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.os.Bundle;

public class myPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		addPreferencesFromResource(R.xml.preferences);
	}
}

