package com.robocatapps.thermodosdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.robocatapps.thermodosdk.model.AnalyzerResult;

import static com.robocatapps.thermodosdk.Constants.FREQUENCY;
import static com.robocatapps.thermodosdk.Constants.LOWER_AMPLITUDE;
import static com.robocatapps.thermodosdk.Constants.NUMBER_OF_CELLS;
import static com.robocatapps.thermodosdk.Constants.PERIODS_PER_CELL;
import static com.robocatapps.thermodosdk.Constants.REFERENCE_AMPLITUDE;
import static com.robocatapps.thermodosdk.Constants.SECONDS;
import static com.robocatapps.thermodosdk.Constants.SYNC_CELL_INDEX;
import static com.robocatapps.thermodosdk.Constants.UPPER_AMPLITUDE;

/**
 * A main class that utilizes all measurments and states.
 */
public final class Thermodo implements AudioRecorder.OnBufferFilledListener,
    AudioManager.OnAudioFocusChangeListener, DeviceDetector.OnDetectionResultListener {

    public static final int ERROR_AUDIO_FOCUS_GAIN_FAILED = 100;

    private static final IntentFilter HEADSET_PLUG_INTENT_FILTER = new IntentFilter(Intent
        .ACTION_HEADSET_PLUG);

    private static final int MSG_STARTED_MEASURING = 0;
    private static final int MSG_STOPPED_MEASURING = 1;
    private static final int MSG_GOT_TEMPERATURE = 2;
    private static final int MSG_THERMODO_PLUGGED_IN = 3;
    private static final int MSG_THERMODO_UNPLUGGED = 4;

    private static final String MSG_TEMPERATURE = "_temperature";

    private static Thermodo sInstance;

    private Context mAppContext;
    private AudioManager mAudioManager;

    private AudioRecorder mRecorder;
    private Sound mAudioTrack;
    private ThermodoListener mListener;
    private DeviceDetector mDeviceDetector;
    private AbstractAnalyzer mAnalyzer;

    private volatile boolean mIsMeasuring = false;
    private volatile boolean mIsRunning = false;

    private int mPreviousVolume = -1;

    private boolean mThermodoIsPlugged;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            //If listener is null, we have nothing to do here
            if (mListener == null)
                return;

            //Switch over message and call needed method
            switch (msg.what) {
                case MSG_STARTED_MEASURING:
                    mListener.onStartedMeasuring();
                    break;
                case MSG_STOPPED_MEASURING:
                    mListener.onStoppedMeasuring();
                    break;
                case MSG_GOT_TEMPERATURE:
                    float temperature = msg.getData().getFloat(MSG_TEMPERATURE);
                    mListener.onGotTemperature(temperature);
                    break;
                case MSG_THERMODO_PLUGGED_IN:
                    mListener.onThermodoPluggedIn();
                    break;
                case MSG_THERMODO_UNPLUGGED:
                    mListener.onThermodoUnplugged();
                    break;
            }
        }
    };

    private final BroadcastReceiver mHeadsetDetector = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("state", -1);
            headSetPluggedIn(state == 1);
        }
    };


    /**
     * Returns an instance of the {@link com.robocatapps.thermodosdk.Thermodo} object associated
     * with the Application's context.
     */
    public synchronized static Thermodo getInstance(Context context) {
        if (sInstance == null)
            sInstance = new Thermodo(context);
        return sInstance;
    }

    private Thermodo(Context context) {
        //Get application context
        mAppContext = context.getApplicationContext();

        //Instantiate required helpers classes
        mRecorder = new AudioRecorder(this);
        mAudioTrack = SoundGenerator.generateSweepSignal(NUMBER_OF_CELLS, PERIODS_PER_CELL,
            SYNC_CELL_INDEX, FREQUENCY, REFERENCE_AMPLITUDE, UPPER_AMPLITUDE,
            LOWER_AMPLITUDE);

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mDeviceDetector = new DeviceDetector(this);
        mAnalyzer = new DefaultSignalAnalyzer();
    }

    /**
     * This method notifies the instance of this object to start detection of the temperature.
     * <p>First of all it will setup additional settings to make measurements precise as possible
     * . After that, if Thermodo device(or any headset) is inserted,
     * it will start device detection algorithm.<p/>
     */
    public void start() {
        if (mIsRunning)
            return;

        //Request focus
        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN);

        //Couldn't get audio focus
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (mListener != null)
                mListener.onErrorOccurred(ERROR_AUDIO_FOCUS_GAIN_FAILED);
            return;
        }

        //Set listener for the headset detector
        mAppContext.registerReceiver(mHeadsetDetector, HEADSET_PLUG_INTENT_FILTER);

        //Set Thermodo is running
        mIsRunning = true;
    }

    /**
     * Stops any measurement or detection actions.
     */
    public void stop() {
        //If Thermodo isn't running we shouldn't execute this method
        if (!mIsRunning)
            return;

        //Unregister for headset plug detection
        mAppContext.unregisterReceiver(mHeadsetDetector);

        //Stop measurements
        stopMeasuring();

        //Restore volume settings
        restoreVolumeSettings();
        mAudioManager.abandonAudioFocus(this);

        //Stop Thermodo
        mIsRunning = false;
    }

    /**
     * This method is called from the {@link android.content.BroadcastReceiver} when headset is
     * plugged in or plugged out.
     *
     * @param pluggedIn Represents is headset was plugged in.
     */
    private void headSetPluggedIn(boolean pluggedIn) {
        //Set volume settings
        if (pluggedIn)
            setVolumeSettings();

        //If plugged and is running check device
        if (pluggedIn && mIsRunning && !mIsMeasuring)
            // For now, disable detection.
            // checkDevice();
            onDetectionResult(true);
        else if (mIsMeasuring)
            stopMeasuring();

        if (mThermodoIsPlugged && !pluggedIn) {
            mThermodoIsPlugged = false;

            mHandler.sendEmptyMessage(MSG_THERMODO_UNPLUGGED);
        }
    }

    /**
     * Checks if plugged in headset is thermodo or just generic headset(or phones).
     */
    private void checkDevice() {
        //Start detection
        mDeviceDetector.startDetection();
    }

    /**
     * Starts measurements.
     */
    private void startMeasuring() {
        if (mIsMeasuring)
            return;

        mAudioTrack.play(-1);
        mRecorder.startRecording();

        if (!mIsMeasuring) {
            mIsMeasuring = true;
            //Notify that measurement started
            mHandler.sendEmptyMessage(MSG_STARTED_MEASURING);
        }
    }

    /**
     * Stops measurements.
     */
    private void stopMeasuring() {
        if (!mIsMeasuring)
            return;

        //Reload track
        mAudioTrack.stop();

        //Stop recorder
        mRecorder.stopRecording();

        //Notify listener that we stop measuring temperature
        if (mIsMeasuring) {
            mIsMeasuring = false;

            mHandler.sendEmptyMessage(MSG_STOPPED_MEASURING);
        }
    }

    /**
     * Analyzes specified data.
     */
    private void analyzeData(short[] data) {

        AnalyzerResult result = mAnalyzer.resultFromAnalyzingData(data);

        if (mIsRunning && result.numberOfFrames > 0) {


            Message msg = mHandler.obtainMessage();
            msg.what = MSG_GOT_TEMPERATURE;

            Bundle msgData = new Bundle();
            msgData.putFloat(MSG_TEMPERATURE, result.temperature);

            msg.setData(msgData);

            mHandler.sendMessage(msg);
        }
    }

    /**
     * Sets volume settings which improve overall measurements.
     */
    private void setVolumeSettings() {
        //Unmute mic
        mAudioManager.setMicrophoneMute(false);

        //Set additional settings
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        mAudioManager.setStreamSolo(AudioManager.STREAM_MUSIC, true);

        //Set volume, save previous value
        mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume
            (AudioManager.STREAM_MUSIC), 0);
    }

    /**
     * Restores settings to those which were before calling {@link Thermodo#setVolumeSettings()}.
     */
    private void restoreVolumeSettings() {
        //Restore audio settings
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mPreviousVolume, 0);
        mAudioManager.setStreamSolo(AudioManager.STREAM_MUSIC, false);
    }

    public void switchToSimplifiedAnalyzer(boolean switchToSimple) {
        boolean wasRunning = mIsRunning;
        if (mIsRunning)
            stop();

        if (switchToSimple) {
            if (!(mAnalyzer instanceof SimplifiedSignalAnalyzer)) {
                mAnalyzer = new SimplifiedSignalAnalyzer();
                mAudioTrack = SoundGenerator.generateL2RSignal((int) (SECONDS / 2 * 1000),
                    FREQUENCY);
            }

        } else {
            if (!(mAnalyzer instanceof DefaultSignalAnalyzer)) {
                mAnalyzer = new DefaultSignalAnalyzer();
                mAudioTrack = SoundGenerator.generateSweepSignal(NUMBER_OF_CELLS, PERIODS_PER_CELL,
                    SYNC_CELL_INDEX, FREQUENCY, REFERENCE_AMPLITUDE, UPPER_AMPLITUDE,
                    LOWER_AMPLITUDE);
            }
        }
        if (wasRunning)
            start();
    }

    @Override
    public void onBufferFilled(short[] data) {
        analyzeData(data);
    }

    @Override
    public void onError(int what) {

    }

    public void setThermodoListener(ThermodoListener listener) {
        mListener = listener;
    }

    public ThermodoListener getThermodoListener() {
        return mListener;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        //If it's not gain, we should stop
        if (focusChange != AudioManager.AUDIOFOCUS_GAIN && mIsRunning)
            stop();
    }

    @Override
    public void onDetectionResult(boolean thermodoDetected) {
        //If thermodo is detected and state is running, start measurements
        if (thermodoDetected && mIsRunning)
            startMeasuring();

        mThermodoIsPlugged = thermodoDetected;

        //Notify listener if needed
        if (mThermodoIsPlugged)
            mHandler.sendEmptyMessage(MSG_THERMODO_PLUGGED_IN);
        else
            restoreVolumeSettings();
    }

    public interface ThermodoListener {

        public void onStartedMeasuring();

        public void onStoppedMeasuring();

        public void onGotTemperature(float temperature);

        public void onThermodoPluggedIn();

        public void onThermodoUnplugged();

        public void onErrorOccurred(int what);
    }
}
