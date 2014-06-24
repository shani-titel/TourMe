package com.nuance.nmdp.sample;

import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.SpeechKit;

import android.R.integer;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;

import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.SpeechKit;
import com.nuance.nmdp.speechkit.Vocalizer;


public class ProfileAge extends Activity {

	private NumberPicker numberPicker;
	private String name;
	private String age;

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

	// tts
	private String welcomeText;
	private String ttsText = ". What is Your Age?";
	private final String nextActivityText = "Let's move on";
	private Vocalizer _vocalizer;
	private final String ttsVoice = "Samantha"; // also available: Tom
	private boolean isTTSAvailable = true;
	private Vocalizer.Listener _vocaListener = null;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		removeHeader();
		setContentView(R.layout.profile_age);
		numberPicker = (NumberPicker) findViewById(R.id.num_picker);
		numberPicker.setMaxValue(100);
		numberPicker.setMinValue(16);
		Bundle bundle = getIntent().getExtras();
		name = bundle.getString("Name");
		TextView hdr = (TextView)findViewById(R.id.header);
		welcomeText = name + ", " + hdr.getText();
		hdr.setText(welcomeText);
		setVolumeControlStream(AudioManager.STREAM_MUSIC); // So 'Media Volume'

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
		// operate
		if (isTTSAvailable)
			_vocalizer.speakString(welcomeText+ttsText, getApplicationContext());
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
				setResult(detail + "\n" + suggestion);
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
		numberPicker.setValue(Integer.parseInt(result));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		if (_speechKit != null) {
//			_speechKit.release();
//			_speechKit = null;
//		}
//		if (_vocalizer != null) {
//			_vocalizer.cancel();
//			_vocalizer = null;
//		}
//		if (_currentRecognizer != null) {
//			_currentRecognizer.cancel();
//			_currentRecognizer = null;
//		}
	}

	private void setResults(Recognition.Result[] results) {
		_arrayAdapter.clear();
		if (results.length > 0) {
			setResult(results[0].getText());

		}
	}

	public void removeHeader() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	public void onNextClicked(View v) {
		createAndConnectSpeechKit();
		_vocalizer = _speechKit.createVocalizerWithLanguage("en_US",
				_vocaListener, new Handler());
		_vocalizer.speakString(nextActivityText, getApplicationContext());
		Intent i = new Intent(getApplicationContext(), ProfileLanguage.class);
		i.putExtra("name", name);
		EditText t = (EditText) findViewById(R.id.age);
		i.putExtra("Age", numberPicker.getValue());
		startActivity(i);
	}

	public void onPrevClicked(View v) {
		onBackPressed();
	}
}