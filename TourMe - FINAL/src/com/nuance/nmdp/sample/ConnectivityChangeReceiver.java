package com.nuance.nmdp.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

	public boolean isConnected=true;
	@Override
	public void onReceive(Context context, Intent intent) {
		debugIntent(intent, "grokkingandroid");
	}

	private void debugIntent(Intent intent, String tag) {
		Log.v(tag, "action: " + intent.getAction());
		Log.v(tag, "component: " + intent.getComponent());
		Bundle extras = intent.getExtras();
		if (extras != null) {
			for (String key : extras.keySet()) {
				Log.v(tag, "key [" + key + "]: " + extras.get(key));
				if(key == ConnectivityManager.EXTRA_NO_CONNECTIVITY){
					isConnected = false;
				}
				else
					isConnected = true;
			}
		} else {
			Log.v(tag, "no extras");
		}
	}

}
