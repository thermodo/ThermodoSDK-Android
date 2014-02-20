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
    public static final int DEFAULT_AUDIO_RECORD_SOURCE = getDefaultAudioRecordSource();

    private static int getDefaultAudioRecordSource() {

        // Prefixes of Build.MODEL for which to use AudioSource.CAMCORDER
        // Source: http://en.wikipedia.org/wiki/Samsung_Galaxy_S_III
        final String[] camcorderAudioSourceModels = {
                "GT-I9300", "GT-I9305", // Samsung Galaxy S3 International
                "SGH-T999", // Samsung Galaxy S3 T-Mobile
                "SGH-I747", // Samsung Galaxy S3 AT&T
                "SCH-R530", // Samsung Galaxy S3 Cricket Wireless, U.S. Cellular, MetroPCS
                "SCH-I535", // Samsung Galaxy S3 Verizon
                "SPH-L710", "SCH-960L", // Samsung Galaxy S3 Sprint, Boost Mobile, Virgin Mobile
                "SCH-S968C", // Samsung Galaxy S3 Straight Talk
        };

        String model = Build.MODEL.toUpperCase();
        for (String m : camcorderAudioSourceModels)
            if (model.startsWith(m))
                return MediaRecorder.AudioSource.CAMCORDER;

        return MediaRecorder.AudioSource.MIC;
    }
}
