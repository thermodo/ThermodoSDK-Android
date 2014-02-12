package com.robocatapps.thermodosdk;

import android.os.Handler;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Manages Thermodo detection process.
 */
public class DeviceDetector implements AudioRecorder.OnBufferFilledListener {

	private static Logger sLog = Logger.getLogger(DeviceDetector.class.getName());
	private static final int TEST_TONE_DURATION = 700;
    private static final int TEST_TONE_FREQUENCY = 200;
    private static final short SILENCE_THRESHOLD = 100;
    private static final short TONE_THRESHOLD = 1000;
    private static final int CUT_SAMPLES_COUNT = 1000;

    private OnDetectionResultListener mListener;

    private Sound mSound;
    private AudioRecorder mRecorder;
    private Handler mHandler;

    private volatile boolean mDetectByTone = false;

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

                if(isInterrupted)
                    Thread.currentThread().interrupt();

                mRecorder.startRecording();
            }
        }
    }

    @Override
    public void onError(int what) {
	    sLog.warning("Recording error: " + what);
        invokeListener(false);
    }

    /**
     * Preforms thermodo detection by assuming that the sound was recorded while not playing
     * anything. Thermodo is detected if the difference between median and max amplitudes is
     * lower than {@link #SILENCE_THRESHOLD}.
     *
     * @param data Raw recorded sound data array.
     * @return true if Thermodo was detected, false otherwise.
     */
    private boolean isDetectedBySilence(short[] data) {
	    sLog.fine("Detecting by silence...");

        Arrays.sort(data);

        // Cutting samples from both ends of the sorted samples array to avoid the influence of
        // occasional amplitude jumps
//        short median = data[(data.length - CUT_SAMPLES_COUNT) / 2];
//        short max = data[data.length - CUT_SAMPLES_COUNT - 1];
//        Logger.log("Median: %d", median);
//        Logger.log("Min: %d\nMax: %d", data[CUT_SAMPLES_COUNT], max);
//
//        boolean detected = max - median <= SILENCE_THRESHOLD;
//        Logger.log("Thermodo detected by silence: " + detected);
//
//        return detected;
	    short average=arrayAverageValue(data);
	    boolean detected=average<SILENCE_THRESHOLD;
	    sLog.finer("Average: " + average);
	    sLog.finer("Thermodo detected by silence: " + detected);

	    return detected;
    }

    /**
     * Preforms thermodo detection by assuming that the sound was recorded while playing a tone.
     * Thermodo is detected if the difference between median and max amplitudes is
     * higher than {@link #TONE_THRESHOLD}.
     *
     * @param data Raw recorded sound data array.
     * @return true if Thermodo was detected, false otherwise.
     */
    private boolean isDetectedByTone(short[] data) {
	    sLog.fine("Detecting by test tone");

//        Arrays.sort(data);
//        short median = data[data.length / 2];
//        short max = data[data.length - 1];
//        Logger.log("Median: %d", median);
//        Logger.log("Min: %d\nMax: %d", data[0], max);
//
//        return max - median > TONE_THRESHOLD;
		short average=arrayAverageValue(data);
	    boolean detected=average>TONE_THRESHOLD;
	    sLog.finer("Average: " + average);
	    sLog.finer("Thermodo detected by tone: " + detected);

	    return detected;
    }

	private short arrayAverageValue(short[] data){
		long sum=0;
		for(short v:data)
			sum+=Math.abs(v);
		return (short) (sum/data.length);
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

    public static interface OnDetectionResultListener {
        public void onDetectionResult(boolean thermodoDetected);
    }
}
