package com.robocatapps.thermodosdk;

import static com.robocatapps.thermodosdk.Constants.SAMPLE_RATE;
import static com.robocatapps.thermodosdk.Sound.CHANNELS_COUNT;
import static com.robocatapps.thermodosdk.Sound.MAX_AMPLITUDE;

public class SoundGenerator {

    /**
     * Returns total number of samples for a one channel signal of a certain duration
     *
     * @param duration Sound duration in milliseconds.
     */
    public static int getSamplesCount(int frequency, int duration) {
        double period = 1.f / frequency;
        int numberOfPeriods = (int) (duration / 1000.f / period);
        return (int) (SAMPLE_RATE * (numberOfPeriods * period));
    }

    /**
     * Generates a constant tone in selected channels with the specified frequency and duration.
     *
     * @param duration     Tone duration in milliseconds.
     * @param frequency    Tone frequency in Hertz.
     * @param leftChannel  Enables the left channel.
     * @param rightChannel Enables the right channel.
     * @return An array of generated samples.
     */
    public static Sound generateWave(int duration, int frequency, boolean leftChannel,
                                     boolean rightChannel) {
        int nSamples = getSamplesCount(frequency, duration);
        short[] samples = new short[nSamples * CHANNELS_COUNT];

        int index = 0;
        for (int i = 0; i < nSamples; i++) {
            short sample = (short) generateSample(i, MAX_AMPLITUDE, frequency);
            samples[index++] = leftChannel ? sample : 0; // left channel
            samples[index++] = rightChannel ? sample : 0; // right channel
        }

        return new Sound(samples);
    }

    /**
     * Generates a signal which switches from left to right channel in the middle
     *
     * @param channelDuration Duration of every channel
     * @param frequency
     * @return
     */
    public static Sound generateL2RSignal(int channelDuration, int frequency) {
        int samplesPerChannel = getSamplesCount(frequency, channelDuration);
        short[] samples = new short[samplesPerChannel * CHANNELS_COUNT * 2];

        int index = 0;
        for (int i = 0; i < samplesPerChannel * 2; i++) {
            short sample = (short) generateSample(i, MAX_AMPLITUDE, frequency);
            samples[index++] = i < samplesPerChannel ? sample : 0;
            samples[index++] = i >= samplesPerChannel ? sample : 0;
        }

        return new Sound(samples);
    }

    /**
     * Generates a sweep signal on the left channel with the constant wave on the right one.
     */
    public static Sound generateSweepSignal(int nCells, int periodsPerCell, int syncCellIndex,
                                            int frequency, double refVolume, double maxVolume,
                                            double minVolume) {
        int samplesPerCell = (SAMPLE_RATE / frequency) * periodsPerCell;
        double volumeStep = (maxVolume - minVolume) / (nCells - 2);

        short[] samples = new short[samplesPerCell * nCells * CHANNELS_COUNT];

        int index = 0;
        int sampleIndex = 0;
        double volume = maxVolume;
        for (int cell = 0; cell < nCells; cell++) {
            for (int i = 0; i < samplesPerCell; i++) {
                // Sync cell phase is inverted to match the signal phase at the start of
                // the next frame. It reduces amplitude jump between frames.
                samples[index++] = (short) generateSample(sampleIndex,
                        cell == syncCellIndex ? -maxVolume * MAX_AMPLITUDE : volume *
                                MAX_AMPLITUDE,
                        cell == syncCellIndex ? frequency * 2 : frequency);
                samples[index++] = (short) -generateSample(sampleIndex,
                        (cell == syncCellIndex ? -1 : 1) * refVolume * MAX_AMPLITUDE,
                        cell == syncCellIndex ? frequency * 2 : frequency);
                sampleIndex++;
            }
            volume -= volumeStep;
        }

        return new Sound(samples);
    }

    /**
     * Calculates a single sample value of a sine wave with the given amplitude and frequency.
     *
     * @param sampleNumber Number of the generated sample. Must be incremented for continuous
     *                     signal generation.
     * @return Sample value in range [0, amplitude].
     */
    private static double generateSample(int sampleNumber, double amplitude, int frequency) {
        return Math.sin((2.0 * Math.PI * frequency * sampleNumber) / SAMPLE_RATE) * amplitude;
    }
}
