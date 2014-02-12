package com.robocatapps.thermodosdk;

import android.os.Handler;
import android.util.Log;

import java.util.Arrays;

/**
 * Manages Thermodo detection process.
 */
public class DeviceDetector implements AudioRecorder.OnBufferFilledListener {

	private static final String LOG_TAG = "DeviceDetector";
	private static final int TEST_TONE_DURATION = 700;
	private static final int TEST_TONE_FREQUENCY = 200;
	private static final short SILENCE_THRESHOLD = 150;
	private static final short TONE_THRESHOLD = 1000;
	private static final short TONE_TO_SILENCE_RATIO = 10;
	private static final int CUT_SAMPLES_COUNT = 800;

	private OnDetectionResultListener mListener;

	private Sound mSound;
	private AudioRecorder mRecorder;
	private Handler mHandler;

	private volatile boolean mDetectByTone = false;
	private short mSilenceMaxLevel;

	public DeviceDetector(OnDetectionResultListener listener) {
		mListener = listener;
		mRecorder = new AudioRecorder(this);
		mHandler = new Handler();
		mSound = SoundGenerator.generateWave(TEST_TONE_DURATION, TEST_TONE_FREQUENCY, true, true);
	}

	/**
	 * Starts the device detection process. A listener method will be invoked on the main thread
	 * passing results to the caller.
	 */
	public void startDetection() {
		mRecorder.startRecording();
	}

	@Override
	public void onBufferFilled(short[] data) {
		mRecorder.stopRecording();

		if (mDetectByTone) {
			invokeListener(isDetectedByTone(data));
			mDetectByTone = false;
		} else {
			if (!isDetectedBySilence(data))
				invokeListener(false);
			else {
				mDetectByTone = true;
				mSound.play(0);

				// As this code runs in the recording thread, tone recording won't be delayed and
				// InterruptedException will be thrown if current thread is interrupted. So
				// saving current state to restore it later.
				// TODO: Consider better solution
				boolean isInterrupted = Thread.interrupted();

				// In most cases playback starts later than recording and half of the
				// recorded buffer contains noise or silence. Short delay between playback and
				// recording start helps to avoid such situations.
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (isInterrupted)
					Thread.currentThread().interrupt();

				mRecorder.startRecording();
			}
		}
	}

	@Override
	public void onRecorderError(int what) {
		Log.w(LOG_TAG, "Recording error: " + what);
		invokeListener(false);
	}

	/**
	 * Preforms Thermodo detection by assuming that the sound was recorded while not playing
	 * anything.
	 *
	 * @param data Raw recorded sound data array.
	 * @return true if Thermodo was detected, false otherwise.
	 */
	private boolean isDetectedBySilence(short[] data) {
		Log.d(LOG_TAG, "Detecting by silence...");

		//OPTION 1: The median of the absolutes need to be under a certain level
		//convertArrayToAbsoluteValues(data);
		//Arrays.sort(data);
		//short median = data[data.length / 2];
		//boolean detected = median < SILENCE_THRESHOLD;

		//OPTION 2: The maximum (after a noise cut) needs to be below a certain level
		convertArrayToAbsoluteValues(data);
		Arrays.sort(data);
		mSilenceMaxLevel = data[data.length - CUT_SAMPLES_COUNT];
		boolean detected = mSilenceMaxLevel < SILENCE_THRESHOLD;

		if (Log.isLoggable(LOG_TAG, Log.INFO)) {
			Log.d(LOG_TAG, "Last elems: " + stringifyLastElements(data, 0, 150));
			Log.d(LOG_TAG, "Almost Last elems: " + stringifyLastElements(data, CUT_SAMPLES_COUNT - 150, 150));
			Log.d(LOG_TAG, "Silence max level: " + mSilenceMaxLevel);
		}
		Log.i(LOG_TAG, "Thermodo detected by silence: " + detected);

		return detected;
	}

	/**
	 * Performs Thermodo detection by assuming that the sound was recorded while playing a tone.
	 *
	 * @param data Raw recorded sound data array.
	 * @return true if Thermodo was detected, false otherwise.
	 */
	private boolean isDetectedByTone(short[] data) {
		Log.d(LOG_TAG, "Detecting by test tone...");

		// OPTION 2: The maximum (after a noise cut) needs to be above a certain level, compared to
		// the silence level
		convertArrayToAbsoluteValues(data);
		Arrays.sort(data);
		short max = data[data.length - CUT_SAMPLES_COUNT];
		boolean detected = max / mSilenceMaxLevel > TONE_TO_SILENCE_RATIO;

		if (Log.isLoggable(LOG_TAG, Log.INFO)) {
			Log.d(LOG_TAG, "Last elems: " + stringifyLastElements(data, 0, 150));
			Log.d(LOG_TAG, "Almost Last elems: " + stringifyLastElements(data, CUT_SAMPLES_COUNT - 150, 150));
			Log.d(LOG_TAG, "Tone max level: " + max);
		}
		Log.i(LOG_TAG, "Thermodo detected by tone: " + detected);

		return detected;
	}

	/**
	 * Utility method to create a string with the last {@code count} elements, with an {@code
	 * offset} from the end.
	 */
	private String stringifyLastElements(short[] data, int offset, int count) {
		StringBuffer b = new StringBuffer();
		for (int i = data.length - count - offset; i < data.length - offset; i++)
			b.append(data[i]).append(", ");
		return b.toString();
	}

	private void convertArrayToAbsoluteValues(short[] data) {
		for (int i = 0; i < data.length; i++)
			data[i] = (short) Math.abs(data[i]);
	}

	private void invokeListener(final boolean detected) {
		if (mListener != null)
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mListener.onDetectionResult(detected);
				}
			});
	}

	/**
	 * Interface for receiving notifications for events related to the Thermodo detection.
	 */
	public static interface OnDetectionResultListener {

		/**
		 * Method called when a detection result is obtained. This method will be called from the
		 * main thread by the {@link com.robocatapps.thermodosdk.DeviceDetector}.
		 */
		public void onDetectionResult(boolean thermodoDetected);
	}
}
