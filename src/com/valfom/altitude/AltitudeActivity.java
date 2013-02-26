package com.valfom.altitude;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class AltitudeActivity extends Activity implements SensorEventListener, LocationListener {
	
	private static final float COEFFICIENT_MILLIMETER_OF_MERCURY = 0.7501f;
	private static final int MIN_UPDATE_TIME = 1000;
	private static final int MIN_UPDATE_DISTANCE = 0;
	
	private SensorManager mSensorManager;
	private Sensor mPressure;
	private LocationManager mLocationManager;
	
	private TextView tvAtmosphericPressure;
	private TextView tvPressureAtSeaLevel;
	private TextView tvCustomPressureAtSeaLevel;
	private TextView tvAltitude;
	private TextView tvCustomAltitude;
	private TextView tvAltitudeGPS;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		tvAtmosphericPressure = (TextView) findViewById(R.id.tvAtmosphericPressure);
		tvPressureAtSeaLevel = (TextView) findViewById(R.id.tvPressureAtSeaLevel);
		tvCustomPressureAtSeaLevel = (TextView) findViewById(R.id.tvCustomPressureAtSeaLevel);
		tvAltitude = (TextView) findViewById(R.id.tvAltitude);
		tvCustomAltitude = (TextView) findViewById(R.id.tvCustomAltitude);
		tvAltitudeGPS = (TextView) findViewById(R.id.tvAltitudeGPS);
		
		registerListener();
	}

	@Override
	protected void onPause() {
	
		super.onPause();
		
		mSensorManager.unregisterListener(this);
		unregisterAllListeners();
	}

	@Override
	protected void onResume() {

		super.onResume();
		
		mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.altitude, menu);
		
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	@Override
	public void onSensorChanged(SensorEvent event) {

		float atmosphericPressure, pressureAtSeaLevel, customPressureAtSeaLevel;
		float altitude, customAltitude;
		
		atmosphericPressure = event.values[0];
		atmosphericPressure *= COEFFICIENT_MILLIMETER_OF_MERCURY; // Converts from hectopascal (hPa) to millimeters of mercury (torr)
		
		pressureAtSeaLevel = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;
		pressureAtSeaLevel *= COEFFICIENT_MILLIMETER_OF_MERCURY;
		
		customPressureAtSeaLevel = 1022; // Value from airport database (http://avdata.geekpilot.net/)
		customPressureAtSeaLevel *= COEFFICIENT_MILLIMETER_OF_MERCURY;
		
		altitude = SensorManager.getAltitude(pressureAtSeaLevel, atmosphericPressure);
		customAltitude = SensorManager.getAltitude(customPressureAtSeaLevel, atmosphericPressure);
		
		tvAtmosphericPressure.setText(Float.toString(atmosphericPressure));
		tvPressureAtSeaLevel.setText(Float.toString(pressureAtSeaLevel));
		tvCustomPressureAtSeaLevel.setText(Float.toString(customPressureAtSeaLevel));
		tvAltitude.setText(Float.toString(altitude));
		tvCustomAltitude.setText(Float.toString(customAltitude));
	}

	@Override
	public void onLocationChanged(Location location) {

		if (location != null) {
			
			updateGPSAltitude(location);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {}

	@Override
	public void onProviderEnabled(String provider) {
		
		registerListener();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	private void updateGPSAltitude(Location location) {
		
		float altitude;
		
		altitude = (float) location.getAltitude();
		
		tvAltitudeGPS.setText(Float.toString(altitude));
	}
	
	private void unregisterAllListeners() {

		mLocationManager.removeUpdates(this);
	}

	private void registerListener() {

		unregisterAllListeners();

		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				MIN_UPDATE_TIME, MIN_UPDATE_DISTANCE, this);
	}

}
