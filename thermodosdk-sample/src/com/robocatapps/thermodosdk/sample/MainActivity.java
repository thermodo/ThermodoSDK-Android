package com.robocatapps.thermodosdk.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.robocatapps.thermodosdk.Thermodo;
import com.robocatapps.thermodosdk.ThermodoFactory;
import com.robocatapps.thermodosdk.ThermodoListener;

import java.util.logging.Logger;

public class MainActivity extends Activity implements ThermodoListener {

	private static Logger sLog = Logger.getLogger(MainActivity.class.getName());
	private Thermodo mThermodo;
	private TextView mTemperatureTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTemperatureTextView = (TextView) findViewById(R.id.temperatureTextView);
		mThermodo = ThermodoFactory.getThermodoInstance(this);
		mThermodo.setThermodoListener(this);
	}


	@Override
	public void onStartedMeasuring() {
		Toast.makeText(this, "Started measuring", Toast.LENGTH_SHORT).show();
		sLog.info("Started measuring");
	}

	@Override
	public void onStoppedMeasuring() {
		Toast.makeText(this, "Stopped measuring", Toast.LENGTH_SHORT).show();
		mTemperatureTextView.setText(getString(R.string.thermodo_unplugged));
		sLog.info("Stopped measuring");
	}

	@Override
	public void onTemperatureMeasured(float temperature) {
		mTemperatureTextView.setText(Float.toString(temperature));
		sLog.fine("Got temparature: " + temperature);
	}

	@Override
	public void onErrorOccurred(int what) {
		Toast.makeText(this, "An error has occurred: " + what, Toast.LENGTH_SHORT).show();
		switch (what) {
			case Thermodo.ERROR_AUDIO_FOCUS_GAIN_FAILED:
				sLog.severe("An error has occurred: Audio Focus Gain Failed");
				mTemperatureTextView.setText(getString(R.string.thermodo_unplugged));
				break;
			case Thermodo.ERROR_AUDIO_RECORD_FAILURE:
				sLog.severe("An error has occurred: Audio Record Failure");
				break;
			case Thermodo.ERROR_SET_MAX_VOLUME_FAILED:
				sLog.warning("An error has occurred: The volume could not be set to maximum");
				break;
			default:
				sLog.severe("An unidentified error has occurred: " + what);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		mThermodo.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mThermodo.stop();
	}
}
