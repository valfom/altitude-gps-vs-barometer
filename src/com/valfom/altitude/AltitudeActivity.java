package com.valfom.altitude;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class AltitudeActivity extends Activity implements SensorEventListener, LocationListener {

	private static final String TAG = "AltitudeActivity";
	
	private static final float TORR_IN_HPA = 0.7501f;
	private static final int MIN_UPDATE_TIME = 500;
	private static final int MIN_UPDATE_DISTANCE = 0;
	private static final String TAG_METAR = "metar";

	private SensorManager sensorManager;
	private Sensor sensorPressure;
	private LocationManager locationManager;

	private Float localPressureAtSeaLevel;
	private boolean localPressureObtained = false;

	private TextView tvAtmosphericPressure;
	private TextView tvPressureAtSeaLevel;
	private TextView tvLocalPressureAtSeaLevel;
	private TextView tvAltitude;
	private TextView tvLocalAltitude;
	private TextView tvAltitudeGPS;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
			
		tvAtmosphericPressure = (TextView) findViewById(R.id.tvAtmosphericPressure);
		tvPressureAtSeaLevel = (TextView) findViewById(R.id.tvPressureAtSeaLevel);
		tvLocalPressureAtSeaLevel = (TextView) findViewById(R.id.tvLocalPressureAtSeaLevel);
		tvAltitude = (TextView) findViewById(R.id.tvAltitude);
		tvLocalAltitude = (TextView) findViewById(R.id.tvLocalAltitude);
		
		tvAltitudeGPS = (TextView) findViewById(R.id.tvAltitudeGPS);
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	}

	@Override
	protected void onPause() {

		super.onPause();

		sensorManager.unregisterListener(this);
		unregisterAllListeners();
	}

	@Override
	protected void onResume() {

		super.onResume();

		sensorManager.registerListener(this, sensorPressure,
				SensorManager.SENSOR_DELAY_NORMAL);
		registerListener();
		
		getLocalPressure();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.altitude, menu);

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		Intent settings = new Intent(this, AltitudePreferenceActivity.class);
		startActivity(settings);
		
		return true;
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		float atmosphericPressure, pressureAtSeaLevel;
		float altitude, localAltitude;

		atmosphericPressure = event.values[0];
		atmosphericPressure *= TORR_IN_HPA; // Converts from hectopascal (hPa) 
											// to millimeters of mercury (torr)

		pressureAtSeaLevel = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;
		pressureAtSeaLevel *= TORR_IN_HPA;

		altitude = SensorManager.getAltitude(pressureAtSeaLevel, atmosphericPressure);

		if (localPressureObtained) {

			localAltitude = SensorManager.getAltitude(localPressureAtSeaLevel, atmosphericPressure);
			tvLocalAltitude.setText(Float.toString(localAltitude));
			tvLocalPressureAtSeaLevel.setText(Float.toString(localPressureAtSeaLevel));
		}

		tvAtmosphericPressure.setText(Float.toString(atmosphericPressure));
		tvPressureAtSeaLevel.setText(Float.toString(pressureAtSeaLevel));
		tvAltitude.setText(Float.toString(altitude));
	}

	@Override
	public void onLocationChanged(Location location) {

		if (location != null) updateGPSAltitude(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
		
		tvAltitudeGPS.setText(R.string.default_value);
	}

	@Override
	public void onProviderEnabled(String provider) {

		registerListener();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}

	private void updateGPSAltitude(Location location) {

		float altitude = (float) location.getAltitude();

		tvAltitudeGPS.setText(Float.toString(altitude));
	}

	private void unregisterAllListeners() {

		locationManager.removeUpdates(this);
	}

	private void registerListener() {

		unregisterAllListeners();

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				MIN_UPDATE_TIME, MIN_UPDATE_DISTANCE, this);
	}

	private class getLocalPressureTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			
			String metar = getWeatherReport();

			if (metar != null) localPressureAtSeaLevel = getPressureValue(metar);
			else localPressureAtSeaLevel = null;
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			
			if (localPressureAtSeaLevel != null) {
				
				localPressureAtSeaLevel *= TORR_IN_HPA;
				localPressureObtained = true;
				
			} else {
				
				Toast.makeText(getApplicationContext(), getString(R.string.err_msg), Toast.LENGTH_LONG).show();
				
				localPressureObtained = false;
				tvLocalPressureAtSeaLevel.setText(getString(R.string.default_value));
				tvLocalAltitude.setText(getString(R.string.default_value));
			}
		}
	}
	
	private void getLocalPressure() {
		
		new getLocalPressureTask().execute();
	}

	public String getWeatherReport() {

		InputStream is = null;
		String result = null;

		try {
			
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			
			String airportId = sharedPreferences.getString(AltitudePreferenceActivity.KEY_AIRPORT_ID, 
					getString(R.string.settings_airport_id_default_value));

			String url = "http://avdata.geekpilot.net/weather/" + airportId + ".xml";

			URL text = new URL(url);

			URLConnection connection = text.openConnection();
			connection.setReadTimeout(30000);
			connection.setConnectTimeout(30000);

			is = connection.getInputStream();
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder domParser = dbf.newDocumentBuilder();
			Document xmldoc = domParser.parse(is);
			Element root = xmldoc.getDocumentElement();

			result = getMetarTagValue(TAG_METAR, root);

		} catch (Exception e) {

			Log.e(TAG, "Error in network call", e);

		} finally {

			try {

				if (is != null) is.close();

			} catch (IOException e) {

				e.printStackTrace();
			}
		}

		return result;
	}

	public String getMetarTagValue(String tag, Element element) {

		NodeList list = element.getElementsByTagName(tag).item(0).getChildNodes();
		Node value = (Node) list.item(0);

		return value.getNodeValue();
	}

	public Float getPressureValue(String metar) {

		Float pressure = null;
		String[] codes = metar.split(" ");

		for (String code : codes) {

			if (code.charAt(0) == 'Q') pressure = Float.parseFloat(code.substring(1));
		}

		return pressure;
	}
}
