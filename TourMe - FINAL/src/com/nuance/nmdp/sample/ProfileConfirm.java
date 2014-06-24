package com.nuance.nmdp.sample;

import java.util.ArrayList;

import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.SpeechKit;
import com.nuance.nmdp.speechkit.Vocalizer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class ProfileConfirm extends Activity {

	private static SpeechKit _speechKit;
	private String[] languages = { "English", "French", "Spanish", "Hebrew",
			"Russian" };
	private String lang1;
	private String lang2;
	private String name;
	private int age;
	private Vocalizer.Listener _vocaListener = null;
	private boolean isTTSAvailable = true;
	private Vocalizer _vocalizer;
	private final String ttsVoice = "Samantha"; // also available: Tom
	

	// Allow other activities to access the SpeechKit instance.
	static SpeechKit getSpeechKit() {
		return _speechKit;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		removeHeader();
		setContentView(R.layout.profile_confirm);
		Bundle bundle = getIntent().getExtras();
		name = bundle.getString("name");
		age = bundle.getInt("Age");
		lang1 = bundle.getString("lang1");
		lang2 = bundle.getString("lang2");

		TextView nameView = (TextView) findViewById(R.id.nameL);
		TextView ageView = (TextView) findViewById(R.id.ageL);
		TextView langView = (TextView) findViewById(R.id.langList);

		nameView.setText(name);
		ageView.setText("" + age);
		String languagesLbl = lang1;
		if (lang2 != null)
			languagesLbl += ", "+ lang2;
		langView.setText(languagesLbl);

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
		
		// ///////////////// tts

				_vocaListener = createVocaListener();
				// Create a single Vocalizer
				createAndConnectSpeechKit();
				_vocalizer = _speechKit.createVocalizerWithLanguage("en_US",
						_vocaListener, new Handler());
				_vocalizer.setVoice(ttsVoice);
				String welcomeText = name + ", is this correct?";
				// operate
				if (isTTSAvailable)
					_vocalizer.speakString(welcomeText, getApplicationContext());

		// final Button dictationButton = (Button)
		// findViewById(R.id.btn_dictation);
		// final Button ttsButton = (Button) findViewById(R.id.btn_tts);
		//
		// Button.OnClickListener l = new Button.OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// if (v == dictationButton) {
		// Intent intent = new Intent(v.getContext(),
		// DictationView.class);
		// MainView.this.startActivity(intent);
		// } else if (v == ttsButton) {
		// Intent intent = new Intent(v.getContext(), TtsView.class);
		// MainView.this.startActivity(intent);
		// }
		// }
		// };
		//
		// dictationButton.setOnClickListener(l);
		// ttsButton.setOnClickListener(l);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		if (_speechKit != null) {
//			_speechKit.release();
//			_speechKit = null;
//		}
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

	public void onNextClicked(View v) {
		Intent i = new Intent(getApplicationContext(), Activities.class);
		startActivity(i);
	}

	public void onPrevClicked(View v) {
		onBackPressed();
	}
}