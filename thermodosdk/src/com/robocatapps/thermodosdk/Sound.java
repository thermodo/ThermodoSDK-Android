package com.robocatapps.thermodosdk;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import static com.robocatapps.thermodosdk.Constants.SAMPLE_RATE;

/**
 * This class stores all generated sound data.
 */
public class Sound {

    public static final int CHANNELS_COUNT = 2;
    public static final int BYTES_PER_SAMPLE = 2;
    public static final int MAX_AMPLITUDE = 32767;

    public final short[] mSamples;
    public final int mSamplesCount;

    private AudioTrack mTrack;

    public Sound(short[] samples) {
        this.mSamples = samples;
        this.mSamplesCount = samples.length / CHANNELS_COUNT;
        this.mTrack = getAudioTrack();
    }

    /**
     * Returns value of the sample specified by its index and channel number.
     */
    public short getSampleValue(int index, int channel) {
        if (index >= mSamplesCount || channel >= CHANNELS_COUNT || index < 0 || channel < 0)
            throw new IndexOutOfBoundsException();

        return mSamples[index * CHANNELS_COUNT + channel];
    }

    /**
     * Starts the sound playback.
     *
     * @param loopCount Sets the number of times this sound will be played. -1 results in an
     *                  infinite loop. 0 disables looping.
     */
    public void play(int loopCount) {
        if(mTrack != null)
            stop();

        mTrack = getAudioTrack();

        if (loopCount != 0)
            mTrack.setLoopPoints(0, mSamplesCount, loopCount);

        mTrack.play();
    }

    /**
     * Stops the playback.
     */
    public void stop() {
        if (mTrack == null)
            return;

        mTrack.stop();
        mTrack.release();
        mTrack = null;
    }

    /**
     * Creates an {@link android.media.AudioTrack} from the stored sound data.
     *
     * @return Resulting AudioTrack object.
     */
    private AudioTrack getAudioTrack() {
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
            mSamples.length * BYTES_PER_SAMPLE, AudioTrack.MODE_STATIC);
        track.write(mSamples, 0, mSamples.length);

        return track;
    }
}
