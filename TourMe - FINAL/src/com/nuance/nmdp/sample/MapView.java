package com.nuance.nmdp.sample;



import java.util.ArrayList;

import org.w3c.dom.Document;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.SpeechKit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MapView extends Activity {

	private static SpeechKit _speechKit;
	private GoogleMap googleMap;
	private TextView hdr;
	private RouteFinder routeFinder = new RouteFinder();
	private Document document;
	private Document documentProgress;
	private LatLng fromPosition;
	private LatLng originPos = new LatLng(48.862932, 2.287119);
	private LatLng eiffelPos;
	private boolean isDrivingMode = false;
	protected MarkerOptions driverMarkerOptions;
	protected Marker driverMarker;
	private Intent updateLocationService =new Intent(this,MOCKUpdateDriverLocationService.class);
	private ArrayList<LatLng> pointsOfRoute;
	

	private void prepareMapMarkers() {
		driverMarkerOptions = new MarkerOptions();
		driverMarkerOptions.icon(BitmapDescriptorFactory
				.fromResource(R.drawable.little_person));
	
	}
	
	// Allow other activities to access the SpeechKit instance.
	static SpeechKit getSpeechKit() {
		return _speechKit;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		removeHeader();
		setContentView(R.layout.map);

		hdr = (TextView) findViewById(R.id.mapHeader);

		// If this Activity is being recreated due to a config change (e.g.
		// screen rotation), check for the saved SpeechKit instance.
		_speechKit = (SpeechKit) getLastNonConfigurationInstance();
		if (_speechKit == null) {
			_speechKit = SpeechKit.initialize(getApplication()
					.getApplicationContext(), AppInfo.SpeechKitAppId,
					AppInfo.SpeechKitServer, AppInfo.SpeechKitPort,
					AppInfo.SpeechKitSsl, AppInfo.SpeechKitApplicationKey);
			_speechKit.connect();
			// TODO: Keep an eye out for audio prompts not working on the Droid
			// 2 or other 2.2 devices.
			Prompt beep = _speechKit.defineAudioPrompt(R.raw.beep);
			_speechKit.setDefaultRecognizerPrompts(beep, Prompt.vibration(100),
					null, null);
		}

		prepareMap();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void prepareMap() {
		googleMap = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.map)).getMap();
		googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		// Showing / hiding your current location
		googleMap.setMyLocationEnabled(false);
		// Enable / Disable zooming controls
		googleMap.getUiSettings().setZoomControlsEnabled(false);
		// Enable / Disable my location button
		googleMap.getUiSettings().setMyLocationButtonEnabled(false);
		// Enable / Disable Compass icon
		googleMap.getUiSettings().setCompassEnabled(true);
		// Enable / Disable Rotate gesture
		googleMap.getUiSettings().setRotateGesturesEnabled(true);
		// Enable / Disable zooming functionality
		googleMap.getUiSettings().setZoomGesturesEnabled(true);

		prepareMapMarkers();
		markEiffelTower();
		markPerson(originPos);
		calcRoute();
	}

	private void markPerson(LatLng pos){
		fromPosition = pos; //48.862932, 2.287119 Statue Equestre du Marechal
		if(driverMarker == null){
			driverMarkerOptions.position(fromPosition);
			driverMarkerOptions.title("You");
			driverMarker = googleMap.addMarker(driverMarkerOptions);	
		}
		else{
			driverMarker.setPosition(pos);
		}
			
		
	}
	
	private void calcRoute(){
		if (fromPosition != null && eiffelPos != null) {
			Log.i("info", "******* About to find route from: "
					+ fromPosition + " to: " + eiffelPos);
			new GetRouteTask().execute();
		}
	}
	
	private void markEiffelTower() {
		// eiffel 48.858434, 2.294460
		eiffelPos = new LatLng(48.858434, 2.294460);
		CameraPosition camPos = new CameraPosition.Builder().target(originPos)
				.zoom(16).build();
		CameraUpdate camUpdate = CameraUpdateFactory.newCameraPosition(camPos);
		googleMap.moveCamera(camUpdate);

		googleMap.addMarker(new MarkerOptions().position(eiffelPos));
	}
	
	private void createRouteHeader() {
		Log.i("info", "********** About to add header");
		String hdrText;
		if(isDrivingMode)
			hdrText = "Driving: ";
		else
			hdrText = "Walking: ";
		hdrText+= routeFinder.getTotalDurationText(document)
				+ ", " + routeFinder.getTotalDistanceText(document);
		hdr.setText(hdrText);
		hdr.setTypeface(hdr.getTypeface(), Typeface.BOLD);
		hdr.setPadding(20, 0, 0, 0);
	}
	
	private void updateHeader(){
		Log.i("info", "********** About to update header");
		String hdrText;
		if(isDrivingMode)
			hdrText = "Driving: ";
		else
			hdrText = "Walking: ";
		hdrText+= routeFinder.getTotalDurationText(documentProgress)
				+ ", " + routeFinder.getTotalDistanceText(documentProgress);
		hdr.setText(hdrText);
		hdr.setTypeface(hdr.getTypeface(), Typeface.BOLD);
		hdr.setPadding(20, 0, 0, 0);
	}
	
	public void onDrivingClicked(View v){
		isDrivingMode = true;
		googleMap.clear();
		markEiffelTower();
		driverMarker=null;
		markPerson(originPos);
		calcRoute();
	}
	
	public void onWalkingClicked(View v){
		isDrivingMode = false;
		googleMap.clear();
		markEiffelTower();
		driverMarker=null;
		markPerson(originPos);
		calcRoute();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		stopService(updateLocationService);
		
		// if (_speechKit != null) {
		// _speechKit.release();
		// _speechKit = null;
		// }
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		// Save the SpeechKit instance, because we know the Activity will be
		// immediately recreated.
		SpeechKit sk = _speechKit;
		_speechKit = null; // Prevent onDestroy() from releasing SpeechKit
		return sk;
	}

	public void removeHeader() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	public void onEiffelClicked(View v) {

		Intent i = new Intent(getApplicationContext(), ChosenActivity.class);
		startActivity(i);

	}
	
	// when getting notification from service that the driver position has
		// changed
		private BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context ctxt, Intent i) {
				Log.i("info", "BroadcastReceiver received event from service.");
				Location driverLoc = i.getParcelableExtra("driverLocation");
				LatLng driverPosition = new LatLng(driverLoc.getLatitude(),
						driverLoc.getLongitude());
				
				// update driver marker position
				markPerson(driverPosition);
				new JustRouteTask().execute();
			}
		};
		
		@Override
		protected void onPause() {
			super.onPause();
			unregisterReceiver(myBroadcastReceiver); // no problem because the service updates driver location in DB, OnReceive just taking care of UI
		}

		@Override
		protected void onResume() {
			super.onResume();
			registerReceiver(myBroadcastReceiver, new IntentFilter(
					"DriverLocationUpdated"));
		}

	// ***************** GetRouteTask ******************************
	private class GetRouteTask extends AsyncTask<String, Void, String> {
		private ProgressDialog Dialog;
		String response = "";

		@Override
		protected void onPreExecute() {// draw on progress
			Dialog = new ProgressDialog(MapView.this);
			Dialog.setMessage("Loading route...");
			Dialog.show();
		}

		@Override
		protected String doInBackground(String... urls) {// calc route
			document = routeFinder.requestRoute(fromPosition, eiffelPos, isDrivingMode);
			if (document != null)
				response = "Success";
			else
				response = "";
			return response;
		}

		@Override
		protected void onPostExecute(String result) {// draw route on map
			if (response.equalsIgnoreCase("Success")) {
				pointsOfRoute = routeFinder
						.getDirection(document);
				
				//printPathPositions(pointsOfRoute);
				
				PolylineOptions rectLine = new PolylineOptions().width(10)
						.color(Color.MAGENTA);
				for (int i = 0; i < pointsOfRoute.size(); i++) {
					rectLine.add(pointsOfRoute.get(i));
				}
				// Adding route on the map
				googleMap.addPolyline(rectLine);
				createRouteHeader();
				Dialog.dismiss();
				
				if(updateLocationService != null){
					stopService(updateLocationService);
				}
				
				updateLocationService =new Intent(getApplicationContext(),MOCKUpdateDriverLocationService.class);
				updateLocationService.putExtra("pointsOfRoute",
						pointsOfRoute);
				startService(updateLocationService);
			}
		}
		
		private void printPathPositions(ArrayList<LatLng> directionPoints) {
			for (int i = 0; i < directionPoints.size(); i++) {
				Log.i("info", "path position num " + i + ": lat= "
						+ directionPoints.get(i).latitude + " ,lng= "
						+ directionPoints.get(i).longitude);
			}
		}
	}
	
	// ***************** GetRouteTask ******************************
		private class JustRouteTask extends AsyncTask<String, Void, String> {
			String response = "";

			@Override
			protected void onPreExecute() {// draw on progress

			}

			@Override
			protected String doInBackground(String... urls) {// calc route
				documentProgress = routeFinder.requestRoute(fromPosition, eiffelPos, isDrivingMode);
				if (documentProgress != null)
					response = "Success";
				else
					response = "";
				return response;
			}

			@Override
			protected void onPostExecute(String result) {// draw route on map
				if (response.equalsIgnoreCase("Success")) {
					updateHeader();
				}
			}
		}
}