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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class AltitudeActivity extends Activity implements SensorEventListener, LocationListener {
	
	private static final float COEFFICIENT_MILLIMETER_OF_MERCURY = 0.7501f;
	private static final int MIN_UPDATE_TIME = 1000;
	private static final int MIN_UPDATE_DISTANCE = 0;
	private static final String TAG_METAR = "metar";
	private static final String TAG = "AltitudeActivity";
	
	private SensorManager sensorManager;
	private Sensor sensorPressure;
	private LocationManager locationManager;
	
	private float customPressureAtSeaLevel;
	private boolean customPressureObtained = false;
	
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
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		tvAtmosphericPressure = (TextView) findViewById(R.id.tvAtmosphericPressure);
		tvPressureAtSeaLevel = (TextView) findViewById(R.id.tvPressureAtSeaLevel);
		tvCustomPressureAtSeaLevel = (TextView) findViewById(R.id.tvCustomPressureAtSeaLevel);
		tvAltitude = (TextView) findViewById(R.id.tvAltitude);
		tvCustomAltitude = (TextView) findViewById(R.id.tvCustomAltitude);
		tvAltitudeGPS = (TextView) findViewById(R.id.tvAltitudeGPS);
		
		updatePressure();
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
		
		sensorManager.registerListener(this, sensorPressure, SensorManager.SENSOR_DELAY_NORMAL);
		registerListener();
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

		float atmosphericPressure, pressureAtSeaLevel;
		float altitude, customAltitude;
		
		atmosphericPressure = event.values[0];
		atmosphericPressure *= COEFFICIENT_MILLIMETER_OF_MERCURY; // Converts from hectopascal (hPa) to millimeters of mercury (torr)
		
		pressureAtSeaLevel = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;
		pressureAtSeaLevel *= COEFFICIENT_MILLIMETER_OF_MERCURY;
		
		altitude = SensorManager.getAltitude(pressureAtSeaLevel, atmosphericPressure);
		
		if (customPressureObtained) { 
			
			customAltitude = SensorManager.getAltitude(customPressureAtSeaLevel, atmosphericPressure);
			tvCustomAltitude.setText(Float.toString(customAltitude));
		}
		
		tvAtmosphericPressure.setText(Float.toString(atmosphericPressure));
		tvPressureAtSeaLevel.setText(Float.toString(pressureAtSeaLevel));
		tvCustomPressureAtSeaLevel.setText(Float.toString(customPressureAtSeaLevel));
		tvAltitude.setText(Float.toString(altitude));
	}

	@Override
	public void onLocationChanged(Location location) {

		if (location != null) updateGPSAltitude(location);
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

		locationManager.removeUpdates(this);
	}

	private void registerListener() {

		unregisterAllListeners();

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				MIN_UPDATE_TIME, MIN_UPDATE_DISTANCE, this);
	}
	
	private void updatePressure() {
		
		Thread task = new Thread(new Runnable() {
	    	
			public void run() {
				
				String metar = getMetarWeatherReport();
				
				if (metar != null) {
					
					customPressureAtSeaLevel = getPressureValueFromMetar(metar);
					customPressureAtSeaLevel *= COEFFICIENT_MILLIMETER_OF_MERCURY;
					
					customPressureObtained = true;
				}
			}
		});
	    
		task.start();
	}
	
	public String getMetarWeatherReport() {
		
	    InputStream is = null;
	    String result = null;
	    
	    try {
	    	
	    	String url = "http://avdata.geekpilot.net/weather/DME.xml";
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
	        
	    		if(is != null) is.close();
	    		
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
	
	public float getPressureValueFromMetar(String metar) {
		
		float pressure = 0.0f;
		String[] codes = metar.split(" ");
		
		for (String code : codes) {
			
			if (code.charAt(0) == 'Q') {
				
				pressure = Float.parseFloat(code.substring(1));
				
				return pressure;
			}
		}
		
		return pressure;
	}
}
