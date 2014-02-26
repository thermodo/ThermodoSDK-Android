package com.robocatapps.thermodosdk;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;

/**
 * A mock implementation of the {@link Thermodo} interface. This instance does not interact with any
 * plugged in device and should be used during the development and for testing purposes only. It
 * simply provides temperatures oscillating between 14 and 24 degrees Celsius every minute while
 * periodically signalling that the device has been unplugged and that the measuring has stopped.
 */
public class ThermodoMockImpl implements Thermodo {

	private ThermodoListener mThermodoListener;
	private boolean mRunning;
	private boolean mMeasuring;

	private final Handler mHandler;

	private final Runnable mMockThermometerRunnable = new Runnable() {

		private boolean shouldRun(long millis) {
			// Run for 20 seconds and stop for 5 seconds
			return millis % 25000 > 5000;
		}

		@Override
		public void run() {
			long millis = SystemClock.elapsedRealtime();

			if (shouldRun(millis)) {
				if (!mMeasuring) {
					// Simulate Thermodo starting measurements, if it's not running
					mMeasuring = true;
					if (mThermodoListener != null)
						mThermodoListener.onStartedMeasuring();
				} else {
					// Simulate a sinus curve that cycles from 14 to 24 degrees every minute
					double temperature = 19 + 5.0 * Math.sin(Math.PI * millis / 60000);
					if (mThermodoListener != null)
						mThermodoListener.onTemperatureMeasured((float) temperature);
				}
			} else if (mMeasuring) {
				// Once in a while simulate Thermodo stopping measurements
				mMeasuring = false;
				if (mThermodoListener != null)
					mThermodoListener.onStoppedMeasuring();
			}

			mHandler.postDelayed(this, 1000);
		}
	};

	public ThermodoMockImpl(Context context) {
		mHandler = new Handler();
		mRunning = false;
		mMeasuring = false;
	}

	@Override
	public void start() {
		mRunning = true;
		mHandler.postDelayed(mMockThermometerRunnable, 2000);
	}

	@Override
	public void stop() {
		mHandler.removeCallbacks(mMockThermometerRunnable);
		mRunning = false;
		mMeasuring = false;
		if (mThermodoListener != null)
			mThermodoListener.onStoppedMeasuring();
	}

	@Override
	public boolean isRunning() {
		return mRunning;
	}

    @Override
    public boolean isMeasuring() { return mMeasuring; }

    @Override
	public void setThermodoListener(ThermodoListener listener) {
		this.mThermodoListener = listener;
	}

	@Override
	public ThermodoListener getThermodoListener() {
		return this.mThermodoListener;
	}
}
