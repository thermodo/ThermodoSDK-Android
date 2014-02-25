package com.robocatapps.thermodosdk;

import com.robocatapps.thermodosdk.model.AnalyzerResult;
import com.robocatapps.thermodosdk.model.Sample;
import com.robocatapps.thermodosdk.model.SamplePool;

import java.util.List;

import static com.robocatapps.thermodosdk.Constants.TEMPERATURE_INTERVAL;

public abstract class AbstractAnalyzer {

    protected static final double MIN_TEMP = -40.0;
    protected static final double MAX_TEMP = 125.0;
    protected static final double REF_RESISTANCE = 100.0;

    protected static final float[] NTC100K_VALUES = {
        4397.119f,
        4092.873f,
        3811.717f,
        3551.748f,
        3311.235f,
        3088.598f,
        2882.395f,
        2691.309f,
        2514.137f,
        2349.777f,
        2197.225f,
        2055.557f,
        1923.931f,
        1801.573f,
        1687.773f,
        1581.88f,
        1483.099f,
        1391.113f,
        1305.412f,
        1225.53f,
        1151.036f,
        1081.535f,
        1016.661f,
        956.0796f,
        899.4806f,
        846.5788f,
        797.111f,
        750.8341f,
        707.5237f,
        666.9723f,
        628.9882f,
        593.3421f,
        559.9309f,
        528.6016f,
        499.2124f,
        471.6321f,
        445.7716f,
        421.4796f,
        398.6521f,
        377.1927f,
        357.0117f,
        338.0058f,
        320.1216f,
        303.2866f,
        287.4335f,
        272.4995f,
        258.4264f,
        245.1598f,
        232.6491f,
        220.8471f,
        209.7098f,
        199.1962f,
        189.2681f,
        179.8896f,
        171.0275f,
        162.6506f,
        154.7264f,
        147.2321f,
        140.142f,
        133.4322f,
        127.0802f,
        121.0658f,
        115.3684f,
        109.9695f,
        104.8521f,
        100.0f,
        95.3981f,
        91.0322f,
        86.889f,
        82.9561f,
        79.2216f,
        75.6752f,
        72.306f,
        69.1042f,
        66.0608f,
        63.1671f,
        60.415f,
        57.7969f,
        55.3056f,
        52.9343f,
        50.6766f,
        48.5283f,
        46.482f,
        44.5325f,
        42.6745f,
        40.9035f,
        39.2132f,
        37.601f,
        36.0629f,
        34.5953f,
        33.1946f,
        31.8591f,
        30.5839f,
        29.366f,
        28.2026f,
        27.0909f,
        26.0284f,
        25.0127f,
        24.0416f,
        23.1128f,
        22.2243f,
        21.3743f,
        20.5607f,
        19.782f,
        19.0364f,
        18.3225f,
        17.6401f,
        16.9864f,
        16.36f,
        15.7596f,
        15.1841f,
        14.631f,
        14.1006f,
        13.5918f,
        13.1037f,
        12.6354f,
        12.1871f,
        11.7567f,
        11.3436f,
        10.9468f,
        10.5657f,
        10.1996f,
        9.8479f,
        9.5098f,
        9.1849f,
        8.8726f,
        8.5722f,
        8.2834f,
        8.0055f,
        7.7383f,
        7.4811f,
        7.2344f,
        6.9971f,
        6.7685f,
        6.5484f,
        6.3365f,
        6.1316f,
        5.9341f,
        5.7439f,
        5.5606f,
        5.3839f,
        5.2143f,
        5.0507f,
        4.893f,
        4.7409f,
        4.5942f,
        4.4527f,
        4.3161f,
        4.1843f,
        4.057f,
        3.9342f,
        3.8156f,
        3.7011f,
        3.5905f,
        3.4836f,
        3.3804f,
        3.2812f,
        3.1853f,
        3.0926f,
        3.0031f,
        2.9164f,
        2.8322f,
        2.7508f,
        2.672f,
        2.5958f,
        2.522f
    };

    protected SamplePool mSamplePool = new SamplePool();

    /**
     * Extracts zero, high and low sample from the buffer.
     *
     * @param data       Buffer to analyze.
     * @param outSamples {@link java.util.List} of samples to which samples containing only zero
     *                   and extreme points will be added.
     */
    protected void samplesFromBuffer(short[] data, List<Sample> outSamples) {
        Sample previousZeroSample = null;

        int previousZeroIndex = 0;

        for (int sampleIndex = 1; sampleIndex < data.length; sampleIndex++) {
            short currentSampleAmplitude = data[sampleIndex];
            short previousSampleAmplitude = data[sampleIndex - 1];

            boolean previousSampleIsPositive = previousSampleAmplitude >= 0;
            boolean currentSampleIsPositive = currentSampleAmplitude >= 0;

            if (previousSampleIsPositive != currentSampleIsPositive) {
                int deltaOffset = sampleIndex - previousZeroIndex;

                Sample zeroSample = mSamplePool.createSample(currentSampleAmplitude, sampleIndex,
                        deltaOffset, Sample.SampleType.ZERO);

                if (previousZeroSample != null) {
                    Sample extremeSample = findExtremeSampleInBuffer(data,
                        previousZeroSample.getBufferIndex(), zeroSample.getBufferIndex());

                    if (extremeSample != null) {
                        outSamples.add(previousZeroSample);
                        outSamples.add(extremeSample);
                    }
                }

                previousZeroSample = zeroSample;
                previousZeroIndex = sampleIndex;
            }
        }
    }

    /**
     * Finds extreme amplitude values in specified range.
     *
     * @param data      A short array with data from mic buffer.
     * @param fromIndex Start position of the range.
     * @param toIndex   End position of the range.
     * @return The {@link com.robocatapps.thermodosdk.model.Sample} which represent MAXIMUM or
     * MINIMUM extreme point on specified range. {@code NULL} if extreme point wasn't found.
     */
    protected Sample findExtremeSampleInBuffer(short[] data, int fromIndex, int toIndex) {

        if (toIndex - fromIndex < 3)
            return null;

        short highestAmplitude = 0;
        int highestAmplitudeIndex = 0;

        //Go through data and try to find extreme point
        for (int sampleIndex = fromIndex; sampleIndex < toIndex; sampleIndex++) {
            short sampleAmplitude = data[sampleIndex];

            short absSampleAmplitude = (short) Math.abs(sampleAmplitude);
            if (absSampleAmplitude > highestAmplitude) {
                highestAmplitude = absSampleAmplitude;
                highestAmplitudeIndex = sampleIndex;
            }
        }

        //We couldn't find extreme point
        if (highestAmplitudeIndex == 0)
            return null;

        short selectedAmplitude = data[highestAmplitudeIndex];
        //Select type of extreme point
        Sample.SampleType type = selectedAmplitude > 0 ? Sample.SampleType.MAX : Sample
            .SampleType.MIN;

        return mSamplePool.createSample(selectedAmplitude, highestAmplitudeIndex, 0, type);
    }


    /**
     * Finds closest value of the temperature taking in count specified resistance value.
     *
     * @return A value of the temperature for the specified resistance. {@code Float.NaN} if
     * temperature couldn't be determined.
     */
    protected static float temperatureFromResistance(float resistance) {
        for (int resistanceIndex = 1; resistanceIndex < NTC100K_VALUES.length; resistanceIndex++) {
            float currentResistance = NTC100K_VALUES[resistanceIndex];

            //We got that calculated resistance if lower that one from the list
            if (currentResistance < resistance) {
                float resistanceFrom = NTC100K_VALUES[resistanceIndex - 1];
                float resistanceTo = currentResistance;

                //Calculate temperatures in range of resistance we've got
                float temperatureFrom = (float) (MIN_TEMP + (resistanceIndex - 1) *
                    TEMPERATURE_INTERVAL);
                float temperatureTo = (float) (MIN_TEMP + resistanceIndex * TEMPERATURE_INTERVAL);

                //Calculate ratio
                float ratio = (resistance - resistanceFrom) / (resistanceTo - resistanceFrom);
                //Calculate temperature
                float temperature = (temperatureTo - temperatureFrom) * ratio + temperatureFrom;

                return temperature;
            }
        }

        //Return NaN if we couldn't get temperature
        return Float.NaN;
    }

	/**
	 * Obtain an analysis result from the provided data.
	 *
	 * @return the analysis results
	 */
    public abstract AnalyzerResult resultFromAnalyzingData(short[] data);
}
