package com.nuance.nmdp.sample;

import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.SpeechKit;
import com.nuance.nmdp.speechkit.Vocalizer;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ChosenActivity extends Activity {

	private static SpeechKit _speechKit;
	private Handler _handler = null;
	private ArrayAdapter<String> _arrayAdapter;
	private boolean _destroyed;
	public static final String TTS_KEY = "com.nuance.nmdp.sample.tts";
	private boolean isSpeechDone = false;
	private final String instructions1 = "You can check out information about";
	private final String instructions2 = "and navigate to it";
	private String activityName = "Eiffel Tower";
	// tts
	private Vocalizer _vocalizer;
	private Vocalizer.Listener _vocaListener = null;
	private final String ttsVoice = "Samantha"; // also available: Tom
	private boolean isTTSAvailable = true;

	// Allow other activities to access the SpeechKit instance.
	static SpeechKit getSpeechKit() {
		return _speechKit;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		removeHeader();
		setContentView(R.layout.chosen_activity);

		setVolumeControlStream(AudioManager.STREAM_MUSIC); // So 'Media Volume'
															// applies

		// ///////////////// tts
		_vocaListener = createVocaListener();
		// Create a single Vocalizer
		createAndConnectSpeechKit();
		_vocalizer = _speechKit.createVocalizerWithLanguage("en_US",
				_vocaListener, new Handler());
		_vocalizer.setVoice(ttsVoice);
		_vocalizer.speakString(instructions1 + activityName + instructions2,
				getApplicationContext());
	}

	private Vocalizer.Listener createVocaListener() {
		// Create Vocalizer listener
		return new Vocalizer.Listener() {
			@Override
			public void onSpeakingBegin(Vocalizer vocalizer, String text,
					Object context) {
				isTTSAvailable = false;
			}

			@Override
			public void onSpeakingDone(Vocalizer vocalizer, String text,
					SpeechError error, Object context) {
				isTTSAvailable = true;
			}
		};
	}

	private void createAndConnectSpeechKit() {
		if (_speechKit != null) {
			_speechKit.release();
			_speechKit = null;
		}
		_speechKit = SpeechKit.initialize(getApplication()
				.getApplicationContext(), AppInfo.SpeechKitAppId,
				AppInfo.SpeechKitServer, AppInfo.SpeechKitPort,
				AppInfo.SpeechKitSsl, AppInfo.SpeechKitApplicationKey);
		_speechKit.connect();

		Prompt beep = _speechKit.defineAudioPrompt(R.raw.beep);
		_speechKit.setDefaultRecognizerPrompts(beep, Prompt.vibration(100),
				null, null);
	}

	public void stopTTS() {
		_vocalizer.cancel();
	}

	public void startTTS(String text) {
		_vocalizer.speakString(text, getApplicationContext());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		if (_speechKit != null) {
//			_speechKit.release();
//			_speechKit = null;
//		}
		if (_vocalizer != null) {
			_vocalizer.cancel();
			_vocalizer = null;
		}
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

	public void onInfoClicked(View v) {
		Intent i = new Intent(getApplicationContext(), Info.class);
		startActivity(i);
	}

	public void onImagesClicked(View v) {
		Intent i = new Intent(getApplicationContext(), Images.class);
		startActivity(i);
	}

	public void onMapClicked(View v) {
		Intent i = new Intent(getApplicationContext(), MapView.class);
		startActivity(i);
	}
}