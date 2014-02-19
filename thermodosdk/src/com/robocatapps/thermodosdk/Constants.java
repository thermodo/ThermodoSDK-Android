package com.robocatapps.thermodosdk;

import android.media.MediaRecorder;
import android.os.Build;

public class Constants {

    public static final int CLIPPING_THRESHOLD = 32000;
    public static final int SAMPLE_RATE = 44100;
    public static final int FREQUENCY = 1000;
    public static final int NUMBER_OF_CELLS = 10;
    public static final int SYNC_CELL_INDEX = 9;
    public static final int PERIODS_PER_CELL = 10;
    public static final int TEMPERATURE_INTERVAL = 1;
    public static final float REFERENCE_AMPLITUDE = 0.5f;
    // Narrowed amplitudes band to avoid clipping and increase signal to noise ratio to improve
    // measurements stability
    public static final float UPPER_AMPLITUDE = 0.9f;
    public static final float LOWER_AMPLITUDE = 0.1f;
    public static final int SAMPLES_PER_CELL = (SAMPLE_RATE / FREQUENCY) * PERIODS_PER_CELL;
    public static final int SAMPLES_PER_FRAME = (NUMBER_OF_CELLS - 1) * SAMPLES_PER_CELL;
    public static final float SECONDS = 0.5f;

    /**
     * The default audio source for the AudioRecord should be different depending on model. For
     * example,on Galaxy S3, better readings are done when using the CAMCORDER AudioSource.
     */
    public static final int DEFAULT_AUDIO_RECORD_SOURCE;

    static {
        //Prepare the DEFAULT_AUDIO_RECORD_SOURCE depending on model.
        String[] camcorderAudioRecordSourceModels = new String[]{"GT-I9305"};
        String model = Build.MODEL;
        int defaultAudioRecordSource = Integer.MAX_VALUE;

        for (String m : camcorderAudioRecordSourceModels)
            if (m.equalsIgnoreCase(model)) {
                defaultAudioRecordSource = MediaRecorder.AudioSource.CAMCORDER;
                break;
            }
        if (defaultAudioRecordSource == Integer.MAX_VALUE) {
            defaultAudioRecordSource = MediaRecorder.AudioSource.MIC;
        }

        DEFAULT_AUDIO_RECORD_SOURCE = defaultAudioRecordSource;
    }

}
