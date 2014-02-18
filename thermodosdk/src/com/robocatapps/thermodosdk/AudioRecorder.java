package com.robocatapps.thermodosdk;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import static com.robocatapps.thermodosdk.Constants.SAMPLE_RATE;
import static com.robocatapps.thermodosdk.Constants.SECONDS;

/**
 * A class that wraps {@link android.media.AudioRecord} class. It reads data from the recorder in
 * the separate {@link Thread} and passes read data to the listener.
 */
public class AudioRecorder {

    /**
     * Interface for receiving callbacks when the audio buffer is full. Methods will be called on
     * a background thread.
     */
    public interface OnBufferFilledListener {

        /**
         * Called when the buffer has been filled.
         *
         * @param data The audio data recorded. This is only guaranteed to be valid during the
         *             execution of the call-back as the buffer is re-used.
         */
        public void onBufferFilled(short[] data);

	    /**
	     * Called if an error occurs while recording. Recording thread will reach an unstable state,
	     * so the recording should be stopped.
	     * <p/>
	     * This method will be called from a background thread.
	     *
	     * @param what The value returned from {@link android.media.AudioRecord#read(short[], int,
	     * int)}
	     */
	    public void onRecorderError(int what);
    }

    private final OnBufferFilledListener mBufferListener;
    private RecorderThread mRecordingThread;

    public AudioRecorder(OnBufferFilledListener onBufferListener) {
        mBufferListener = onBufferListener;
    }

    /**
     * Starts recording.
     * <p><b>Note:<b/> It most likely recording will not start right after calling this method due
     * to the native layer latencies.</p>
     */
    public void startRecording() {
        if (isRecording())
            return;

        mRecordingThread = new RecorderThread(mBufferListener);
        mRecordingThread.setName("AudioRecorder");
        mRecordingThread.start();
    }

    /**
     * Stops recording and all underlying threads.
     */
    public void stopRecording() {
        if (!isRecording())
            return;

        mRecordingThread.stopRecording();
        mRecordingThread = null;
    }

    private boolean isRecording() {
        return mRecordingThread != null;
    }

    /**
     * Thread in which all recording operations are actually performed.
     */
    private static class RecorderThread extends Thread {

        private final short[] mBuffer;
        private final OnBufferFilledListener mListener;
        private final AudioRecord mAudioRecord;

        public RecorderThread(OnBufferFilledListener listener) {
            int bufferSize = getBufferSize();
            mBuffer = new short[bufferSize / 2];
            mListener = listener;
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                int read = mAudioRecord.read(mBuffer, 0, mBuffer.length);

                // Check for interruption
                if (isInterrupted())
                    break;

                // Check for error
                if (read < 0 ) {
                    //Fire error to the listener
                    if (mListener != null)
                        mListener.onRecorderError(read);
                    break;
                }

                // Fire obtained data to the listener
                if (mListener != null && !isInterrupted())
                    mListener.onBufferFilled(mBuffer);
            }

            mAudioRecord.release();
        }

        @Override
        public synchronized void start() {
            mAudioRecord.startRecording();
            super.start();
        }

        public void stopRecording() {
            mAudioRecord.stop();
            interrupt(); // Background thread will release the AudioRecord object
        }

        /**
         * @return The buffer size, in bytes, to use for recording Audio.
         */
        private static int getBufferSize() {
            // Size should be enough to hold half a second of audio, or more if the system requires
            // a larger buffer.
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            return Math.max(minBufferSize, (int) (SECONDS * SAMPLE_RATE) * 2);
        }
    }
}
