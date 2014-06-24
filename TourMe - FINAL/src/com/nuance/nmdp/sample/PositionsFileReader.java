package com.nuance.nmdp.sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import com.google.android.gms.maps.model.LatLng;



//for tests only
public class PositionsFileReader {

	/**
	 * Constructor.
	 * 
	 * @param aFileName
	 *            full name of an existing, readable file.
	 */
	public PositionsFileReader() {
		File sdcard = Environment.getExternalStorageDirectory();
		//Get the text file
		fFilePath = new File(sdcard,"pos.txt");
	}

	/** Template method that calls {@link #processLine(String)}. */
	public final ArrayList<LatLng> getPathPositions(Context context) throws IOException {
		ArrayList<LatLng> results = new ArrayList<LatLng>();
		
		AssetManager am = context.getAssets();
		InputStream is = am.open("pos.txt");
		InputStreamReader inputStreamReader = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String receiveString = "";
        while ( (receiveString = bufferedReader.readLine()) != null ) {
        	results.add(processLine(receiveString));
        }
        is.close();
		return results;
	}

	/**
	 * Overridable method for processing lines in different ways.
	 * 
	 * <P>
	 * This simple default implementation expects simple name-value pairs,
	 * separated by an '=' sign. Examples of valid input:
	 * <tt>height = 167cm</tt> <tt>mass =  65kg</tt>
	 * <tt>disposition =  "grumpy"</tt>
	 * <tt>this is the name = this is the value</tt>
	 */
	protected LatLng processLine(String aLine) {
		// use a second Scanner to parse the content of each line
		LatLng pos = null;
		Scanner scanner = new Scanner(aLine);
		scanner.useDelimiter(":");
		if (scanner.hasNext()) {
			// assumes the line has a certain structure
			String s4 = scanner.next();
			String s5 = scanner.next();
			String s1 = scanner.next();
			String s2 = scanner.next();
			String s3 = scanner.next();
			String t = scanner.next();
			t=t.trim();
			String fromLat = t.substring(t.indexOf("=")+1);
			String lat = fromLat.substring(0,fromLat.indexOf(","));
			String lng = fromLat.substring(fromLat.indexOf("=")+1);
			pos = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
		}
		return pos;
	}

	// PRIVATE
	private final File fFilePath;
}
