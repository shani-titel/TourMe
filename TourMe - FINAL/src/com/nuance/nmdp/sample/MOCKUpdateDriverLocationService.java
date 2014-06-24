package com.nuance.nmdp.sample;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

public class MOCKUpdateDriverLocationService extends Service {
	private ArrayList<LatLng> locations;
	private int index;
	private Timer timer;
	private TimerTask timerTask;
	

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.i("info", "MOCK Update location service was created");
		timer = new Timer();
		index = 0;
		locations = new ArrayList<LatLng>();
		
		// fill with path locations
		Log.i("info",
				"MOCK Update location service filled locations list with total "
						+ locations.size() + " positions");
	}

	@Override
	public void onDestroy() {
		Log.i("info", "MOCK Update location service was stopped");
//		Toast.makeText(this, "MOCK UpdateDriverLocation Service stopped",
//				Toast.LENGTH_LONG).show();
		timerTask.cancel();
		timer.cancel();
		stopSelf();
	}

	@Override
	public void onStart(Intent intent, int startid) {
		Log.i("info", "MOCK Update location service was started");
//		Toast.makeText(this, "MOCK UpdateDriverLocation Service Started",
//				Toast.LENGTH_LONG).show();
		locations = intent.getParcelableArrayListExtra("pointsOfRoute");
		index = 0;
		startTimer();
	}

	private void startTimer() {
		timerTask = new TimerTask() {
			@Override
			public void run() {
				Log.i("info", "*** time tick");
				// get next location and raise index
				if (locations.size() > 0 && index < locations.size()) {
					LatLng location = locations.get(index);
					Location l = new Location("");
					l.setLatitude(location.latitude);
					l.setLongitude(location.longitude);

					
					// notify RideOnMapDriver
					Intent i = new Intent("DriverLocationUpdated");
					i.putExtra("driverLocation", l);
					getApplicationContext().sendBroadcast(i);
					Log.i("info",
							"MOCK Update location service send location= "
									+ location + " index= " + index);
					index++;
				} else
					timer.cancel();
			}
		};

		timer.scheduleAtFixedRate(timerTask, 0, 1000);
	}
}
