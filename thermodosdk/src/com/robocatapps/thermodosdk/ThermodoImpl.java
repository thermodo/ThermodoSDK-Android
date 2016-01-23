package com.robocatapps.thermodosdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.robocatapps.thermodosdk.model.AnalyzerResult;

import java.util.logging.Logger;

import static com.robocatapps.thermodosdk.Constants.FREQUENCY;
import static com.robocatapps.thermodosdk.Constants.LOWER_AMPLITUDE;
import static com.robocatapps.thermodosdk.Constants.NUMBER_OF_CELLS;
import static com.robocatapps.thermodosdk.Constants.PERIODS_PER_CELL;
import static com.robocatapps.thermodosdk.Constants.REFERENCE_AMPLITUDE;
import static com.robocatapps.thermodosdk.Constants.SECONDS;
import static com.robocatapps.thermodosdk.Constants.SYNC_CELL_INDEX;
import static com.robocatapps.thermodosdk.Constants.UPPER_AMPLITUDE;

/**
 * Main implementation of the {@link Thermodo} interface.
 */
public final class ThermodoImpl implements AudioRecorder.OnBufferFilledListener,
        AudioManager.OnAudioFocusChangeListener, DeviceDetector.OnDetectionResultListener, Thermodo {

    private static Logger sLog = Logger.getLogger(Thermodo.class.getName());

    private static final IntentFilter HEADSET_PLUG_INTENT_FILTER = new IntentFilter(Intent
            .ACTION_HEADSET_PLUG);

    private static final int MSG_STARTED_MEASURING = 0;
    private static final int MSG_STOPPED_MEASURING = 1;
    private static final int MSG_GOT_TEMPERATURE = 2;

    private static final String MSG_TEMPERATURE = "_temperature";

    private Context mAppContext;
    private AudioManager mAudioManager;

    private AudioRecorder mRecorder;
    private Sound mAudioTrack;
    private ThermodoListener mListener;
    private DeviceDetector mDeviceDetector;
    private AbstractAnalyzer mAnalyzer;

    private volatile boolean mIsMeasuring = false;
    private volatile boolean mIsRunning = false;

    private boolean mDeviceCheckEnabled;
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
                    mListener.onTemperatureMeasured(temperature);
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

    protected ThermodoImpl(Context context) {
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
        mDeviceCheckEnabled = false; //disable device check by default
    }


    private Boolean checkAudioPermission() {
        String permission = "android.permission.RECORD_AUDIO";
        int res = mAppContext.checkCallingOrSelfPermission(permission);
        String secondPermission = "android.permission.MODIFY_AUDIO_SETTINGS";
        int resModify = mAppContext.checkCallingOrSelfPermission(secondPermission);
        return (res == PackageManager.PERMISSION_GRANTED && resModify == PackageManager.PERMISSION_GRANTED);
    }

    @Override
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


    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public boolean isMeasuring() {
        return mIsMeasuring;
    }

    @Override
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
        mListener.onThermodoPlugged(pluggedIn);
        //Set volume settings
        if (pluggedIn)
            setVolumeSettings();

        //If plugged and is running, check the device or directly start measuring
        if (pluggedIn && mIsRunning && !mIsMeasuring && checkAudioPermission()) {
            if (mDeviceCheckEnabled)
                checkDevice();
            else
                onDetectionResult(true);
        } else if (mIsMeasuring) {
            stopMeasuring();
        } else if (!checkAudioPermission()) {
            mListener.onPermissionsMissing();
        }

        if (mThermodoIsPlugged && !pluggedIn) {
            mThermodoIsPlugged = false;
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

        mIsMeasuring = true;
        //Notify that measurement started
        mHandler.sendEmptyMessage(MSG_STARTED_MEASURING);
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
        mIsMeasuring = false;
        mHandler.sendEmptyMessage(MSG_STOPPED_MEASURING);
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
     * Sets volume settings in order to improve overall measurements. If possible, the volume
     * is adjusted value between 80% and 90% of the maximum. This is chosen in an attempt to avoid
     * automatic gain control being applied to the microphone on some devices.
     */
    private void setVolumeSettings() {
        // Unmute mic
        mAudioManager.setMicrophoneMute(false);

        // Set additional settings
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        mAudioManager.setStreamSolo(AudioManager.STREAM_MUSIC, true);

        // Set volume to 85% of maximum, saving previous value
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int desiredVolume = maxVolume * 85 / 100;
        mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        int minAllowedVolume = maxVolume * 80 / 100;
        int maxAllowedVolume = maxVolume * 90 / 100;
        if (minAllowedVolume <= mPreviousVolume && mPreviousVolume <= maxAllowedVolume)
            return; // Already within working range

        // Try to change the volume without notifying the user
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, desiredVolume, 0);
        int adjustedVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (minAllowedVolume <= adjustedVolume && adjustedVolume <= maxAllowedVolume)
            return; // Volume adjusted correctly

        // Finally try to change the volume notifying the user, so, if needed, permissions are
        // given to raise the volume above a certain level (helpful on some devices)
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI);
        adjustedVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (minAllowedVolume <= adjustedVolume && adjustedVolume <= maxAllowedVolume)
            return; // Volume adjusted correctly

        // We were unsuccessfull in adjusting the volume. Error:
        sLog.warning("Volume could not be set to the maximum value.");
        mListener.onErrorOccurred(Thermodo.ERROR_SET_MAX_VOLUME_FAILED);
    }

    /**
     * Restores settings to those which were before calling {@link ThermodoImpl#setVolumeSettings()}.
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
    public void onRecorderError(int what) {
        // If we get a recording error, the Audio Recorder should be stopped and we need to make
        // sure that we stop the measurement to keep the state consistent and notify the listener
        // NOTE: This method is called from a background thread so we need to make sure we run in
        // on the main thread
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // NOTE: Calling stopMeasuring will stop the AudioRecorder and will do the cleanup
                stopMeasuring();
                mListener.onErrorOccurred(ERROR_AUDIO_RECORD_FAILURE);
            }
        });
    }

    @Override
    public void setThermodoListener(ThermodoListener listener) {
        mListener = listener;
    }

    @Override
    public ThermodoListener getThermodoListener() {
        return mListener;
    }

    /**
     * Sets whether a check is made to make sure the device connected to the audio jack is a
     * Thermodo or something else (e.g. headphones, microphone). By default, a check is done.
     * <p/>
     * NOTE: Keep this out of the main Thermodo interface until further testing
     */
    public void setDeviceCheckEnabled(boolean newValue) {
        mDeviceCheckEnabled = newValue;
    }

    /**
     * Checks whether a verification is made to make sure the device connected to the audio jack is
     * a Thermodo or something else (e.g. headphones, microphone).
     * <p/>
     * NOTE: Keep this out of the main Thermodo interface until further testing
     */
    public boolean isDeviceCheckEnabled() {
        return mDeviceCheckEnabled;
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

        if (!mThermodoIsPlugged)
            restoreVolumeSettings();
    }
}
