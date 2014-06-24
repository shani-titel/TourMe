package com.nuance.nmdp.sample;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
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

import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.SpeechKit;
import com.nuance.nmdp.speechkit.Vocalizer;

public class ProfileName extends Activity {

	private static SpeechKit _speechKit;
	private static final int LISTENING_DIALOG = 0;
	private Handler _handler = null;
	private Recognizer.Listener _listener;
	private Recognizer _currentRecognizer;
	private ListeningDialog _listeningDialog;
	private ArrayAdapter<String> _arrayAdapter;
	private boolean _destroyed;
	public static final String TTS_KEY = "com.nuance.nmdp.sample.tts";
	private boolean isSpeechDone = false;
	private Button dictationButton;
	private ConnectivityChangeReceiver reciver;
	
	// tts
	private final String welcomeText = "Welcome to Tour Me! Let's get to know each other! What is your name? To speak click the speak button";
	private final String nextActivityText = "Let's move on";
	private final String errorText = "You must enter your name!";
	private final String noWiFi = "Cannot find internet connection. Please turn on 3G or connect to Wi-Fi";
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
		setContentView(R.layout.profile_name);
		
		reciver = new ConnectivityChangeReceiver();
		registerReceiver(reciver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC); // So 'Media Volume'
															// applies

		// ///////////////////////// asr
		_listener = createListener();

		dictationButton = (Button) findViewById(R.id.speak_btn);
		Button.OnClickListener startListener = new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
					_listeningDialog.setText("Initializing...");
					showDialog(LISTENING_DIALOG);
					_listeningDialog.setStoppable(false);
					setResults(new Recognition.Result[0]);
					createAndConnectSpeechKit();
					_currentRecognizer = _speechKit.createRecognizer(
							Recognizer.RecognizerType.Dictation,
							Recognizer.EndOfSpeechDetection.Long, "eng-USA",
							_listener, _handler);
					if (_currentRecognizer != null) {
						_currentRecognizer.start();
					}
			}
		};
		dictationButton.setOnClickListener(startListener);
		setListAdapter();

		// Initialize the listening dialog
		createListeningDialog();
		_handler = new Handler();

		// ///////////////// tts

		_vocaListener = createVocaListener();
		// Create a single Vocalizer
		createAndConnectSpeechKit();
		_vocalizer = _speechKit.createVocalizerWithLanguage("en_US",
				_vocaListener, new Handler());
		_vocalizer.setVoice(ttsVoice);
		
		
		if (!reciver.isConnected) {
			Toast.makeText(getApplicationContext(), noWiFi, Toast.LENGTH_LONG)
					.show();
			createAndConnectSpeechKit();
			_vocalizer.speakString(noWiFi, getApplicationContext());
		}
		// operate
		if (isTTSAvailable && reciver.isConnected)
			_vocalizer.speakString(welcomeText, getApplicationContext());
		
		
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

	public void setListAdapter() {
		ListView list = (ListView) findViewById(R.id.list_results);
		// Set up the list to display multiple results
		_arrayAdapter = new ArrayAdapter<String>(list.getContext(),
				R.layout.listitem) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				Button b = (Button) super
						.getView(position, convertView, parent);
				b.setBackgroundColor(Color.LTGRAY);
				b.setOnClickListener(new Button.OnClickListener() {
					@Override
					public void onClick(View v) {
						Button b = (Button) v;
						EditText t = (EditText) findViewById(R.id.name);

						// Copy the text (without the [score]) into the edit box
						String text = b.getText().toString();
						int startIndex = text.indexOf("]: ");
						t.setText(text
								.substring(startIndex > 0 ? (startIndex + 3)
										: 0));
					}
				});
				return b;
			}
		};
		list.setAdapter(_arrayAdapter);
	}

	private void createAndConnectSpeechKit() {
//		if (_speechKit != null) {
//			_speechKit.release();
//			_speechKit = null;
//		}
		_speechKit = SpeechKit.initialize(getApplication()
				.getApplicationContext(), AppInfo.SpeechKitAppId,
				AppInfo.SpeechKitServer, AppInfo.SpeechKitPort,
				AppInfo.SpeechKitSsl, AppInfo.SpeechKitApplicationKey);
		_speechKit.connect();

		Prompt beep = _speechKit.defineAudioPrompt(R.raw.beep);
		_speechKit.setDefaultRecognizerPrompts(beep, Prompt.vibration(100),
				null, null);
	}

	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		switch (id) {
		case LISTENING_DIALOG:
			_listeningDialog.prepare(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (_currentRecognizer != null) {
						_currentRecognizer.stopRecording();
					}
				}
			});
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case LISTENING_DIALOG:
			return _listeningDialog;
		}
		return null;
	}

	public void stopTTS() {
		_vocalizer.cancel();
	}

	public void startTTS(String text) {
		_vocalizer.speakString(text, getApplicationContext());
	}

	private Recognizer.Listener createListener() {
		return new Recognizer.Listener() {
			@Override
			public void onRecordingBegin(Recognizer recognizer) {
				_listeningDialog.setText("Recording...");
				_listeningDialog.setStoppable(true);
				_listeningDialog.setRecording(true);

				// Create a repeating task to update the audio level
				Runnable r = new Runnable() {
					public void run() {
						if (_listeningDialog != null
								&& _listeningDialog.isRecording()
								&& _currentRecognizer != null) {
							_listeningDialog.setLevel(Float
									.toString(_currentRecognizer
											.getAudioLevel()));
							_handler.postDelayed(this, 500);
						}
					}
				};
				r.run();
			}

			@Override
			public void onRecordingDone(Recognizer recognizer) {
				_listeningDialog.setText("Processing...");
				_listeningDialog.setLevel("");
				_listeningDialog.setRecording(false);
				_listeningDialog.setStoppable(false);
			}

			@Override
			public void onError(Recognizer recognizer, SpeechError error) {
				if (recognizer != _currentRecognizer)
					return;
				if (_listeningDialog.isShowing())
					dismissDialog(LISTENING_DIALOG);
				_currentRecognizer = null;
				_listeningDialog.setRecording(false);

				// Display the error + suggestion in the edit box
				String detail = error.getErrorDetail();
				String suggestion = error.getSuggestion();

				if (suggestion == null)
					suggestion = "";
				// setResult(detail + "\n" + suggestion);
				Toast.makeText(getApplicationContext(),
						detail + '\n' + suggestion, Toast.LENGTH_LONG).show();
				createAndConnectSpeechKit();
				_vocalizer.speakString(detail + '\n' + suggestion,
						getApplicationContext());
			}

			@Override
			public void onResults(Recognizer recognizer, Recognition results) {
				if (_listeningDialog.isShowing())
					dismissDialog(LISTENING_DIALOG);
				_currentRecognizer = null;
				_listeningDialog.setRecording(false);
				int count = results.getResultCount();
				Recognition.Result[] rs = new Recognition.Result[count];
				for (int i = 0; i < count; i++) {
					rs[i] = results.getResult(i);
				}
				setResults(rs);
			}
		};
	}

	private void setResult(String result) {
		EditText t = (EditText) findViewById(R.id.name);
		if (t != null)
			t.setText(result);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// if (_speechKit != null) {
		// _speechKit.release();
		// _speechKit = null;
		// }
//		if (_vocalizer != null) {
//			_vocalizer.cancel();
//			_vocalizer = null;
//		}
//		if (_currentRecognizer != null) {
//			_currentRecognizer.cancel();
//			_currentRecognizer = null;
//		}
		
		unregisterReceiver(reciver);
	}

	private void setResults(Recognition.Result[] results) {
		_arrayAdapter.clear();
		if (results.length > 0) {
			setResult(results[0].getText());

			// for (int i = 0; i < results.length; i++)
			// _arrayAdapter.add("[" + results[i].getScore() + "]: "
			// + results[i].getText());
		} else {
			setResult("");
		}
	}

	public void removeHeader() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	public void onNextClicked(View v) {
		if ( reciver.isConnected) {
			EditText nameEditText = (EditText) findViewById(R.id.name);
			String name = nameEditText.getText().toString();
			if (!name.isEmpty()) {
				// Create a single Vocalizer
				createAndConnectSpeechKit();
				_vocalizer = _speechKit.createVocalizerWithLanguage("en_US",
						_vocaListener, new Handler());
				_vocalizer.setVoice(ttsVoice);
				// operate
				if (isTTSAvailable)
					_vocalizer
							.speakString(welcomeText, getApplicationContext());
				_vocalizer.speakString(nextActivityText,
						getApplicationContext());
				Intent i = new Intent(getApplicationContext(), ProfileAge.class);
				i.putExtra("Name", name);
				startActivity(i);
			} else {
				Toast.makeText(getApplicationContext(),
						"You must enter your name!", Toast.LENGTH_SHORT).show();
				_vocalizer.speakString(errorText, getApplicationContext());
			}
		} else {
			Toast.makeText(this, noWiFi, Toast.LENGTH_SHORT).show();
		}
	}

	// public void onSpeakClicked(View v) {
	// if (isSpeechDone == false) { // to start speaking
	// _listeningDialog.setText("Initializing...");
	// showDialog(LISTENING_DIALOG);
	// _listeningDialog.setStoppable(false);
	// setResults(new Recognition.Result[0]);
	// SpeechKit sk = MainView.getSpeechKit();
	// _currentRecognizer = MainView.getSpeechKit().createRecognizer(
	// Recognizer.RecognizerType.Dictation,
	// Recognizer.EndOfSpeechDetection.Long, "eng-USA", _listener,
	// _handler);
	// if (_currentRecognizer != null) {
	// _currentRecognizer.start();
	// dictationButton.setText("Stop"); // maybe finish? done?
	// isSpeechDone = true;
	// }
	//
	// } else { // to finish speaking
	// dictationButton.setText("Speak");
	// isSpeechDone = false;
	// }
	// }

	public Recognizer.Listener get_listener() {
		return _listener;
	}

	private void createListeningDialog() {
		_listeningDialog = new ListeningDialog(this);
		_listeningDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				if (_currentRecognizer != null) // Cancel the current recognizer
				{
					_currentRecognizer.cancel();
					_currentRecognizer = null;
				}

				if (!_destroyed) {
					// Remove the dialog so that it will be recreated next time.
					// This is necessary to avoid a bug in Android >= 1.6 where
					// the
					// animation stops working.
					removeDialog(LISTENING_DIALOG);
					createListeningDialog();
				}
			}
		});
	}
}