package com.showmehills;

import com.showmehills.R;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.os.Bundle;

public class AppPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		addPreferencesFromResource(R.xml.preferences);
	}
}

