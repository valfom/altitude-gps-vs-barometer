package com.valfom.altitude;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class AltitudePreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	public static final String KEY_AIRPORT_ID = "etAirportId";
	
	private EditTextPreference etAirportId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		etAirportId = (EditTextPreference) getPreferenceScreen().findPreference(KEY_AIRPORT_ID);
	}
	
	@Override
    protected void onPause() {
        
    	super.onPause();
          
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
    }
	
	@Override
	protected void onResume() {

		super.onResume();
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		etAirportId.setSummary(sharedPreferences.getString(KEY_AIRPORT_ID, 
				getString(R.string.settings_airport_id_default_value)));
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
    	if (key.equals(KEY_AIRPORT_ID)) {
        	
    		etAirportId.setSummary(sharedPreferences.getString(KEY_AIRPORT_ID, 
    				getString(R.string.settings_airport_id_default_value)));
    	}
	}
}
