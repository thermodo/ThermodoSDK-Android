package com.robocatapps.thermodosdk;

import com.robocatapps.thermodosdk.model.AnalyzerResult;
import com.robocatapps.thermodosdk.model.Cell;
import com.robocatapps.thermodosdk.model.Frame;
import com.robocatapps.thermodosdk.model.Sample;
import com.robocatapps.thermodosdk.model.Trendline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.robocatapps.thermodosdk.Constants.CLIPPING_THRESHOLD;
import static com.robocatapps.thermodosdk.Constants.LOWER_AMPLITUDE;
import static com.robocatapps.thermodosdk.Constants.PERIODS_PER_CELL;
import static com.robocatapps.thermodosdk.Constants.REFERENCE_AMPLITUDE;
import static com.robocatapps.thermodosdk.Constants.SAMPLES_PER_CELL;
import static com.robocatapps.thermodosdk.Constants.SAMPLES_PER_FRAME;
import static com.robocatapps.thermodosdk.Constants.TEMPERATURE_INTERVAL;
import static com.robocatapps.thermodosdk.Constants.UPPER_AMPLITUDE;

public class SignalAnalyzer {

    private static final float[] NTC100K_VALUES = {
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
    private static final double MIN_TEMP = -40.0;
    private static final double MAX_TEMP = 125.0;
    private static final double REF_RESISTANCE = 100.0;

    public static void preformSimpleAnalysis(short[] samples) {
        int leftValue = 0;
        int rightValue = 0;
        int nSamples = 500;
        int silenceThreshold = 1000;

        int leftSampleIndex = 0;
        int rightSampleIndex = 0;

        for (int i = 1; i < samples.length - 1; i++) {
            int prev = Math.abs(samples[i - 1]);
            int current = Math.abs(samples[i]);
            int next = Math.abs(samples[i + 1]);

            if (prev <= current && next < current && current > silenceThreshold) {
                leftValue += current;
                leftSampleIndex++;
            }

            if (leftSampleIndex == nSamples)
                break;
        }

        for (int i = samples.length - 2; i > 0; i--) {
            int next = Math.abs(samples[i + 1]);
            int current = Math.abs(samples[i]);
            int previous = Math.abs(samples[i - 1]);

            if (next < current && previous <= current && current > silenceThreshold) {
                rightValue += current;
                rightSampleIndex++;
            }

            if (rightSampleIndex == nSamples)
                break;
        }

        if (leftSampleIndex < nSamples || rightSampleIndex < nSamples) {
            Logger.log("Not enough samples detected");
            return;
        }

        leftValue /= nSamples;
        rightValue /= nSamples;

        Logger.log(String.format("Average values: %6d  |  %6d", leftValue, rightValue));
        Logger.log(String.format("left/right: %f", (double) leftValue / rightValue));
        Logger.log(String.format("right/left: %f", (double) rightValue / leftValue));

        double resistance = (rightValue * REF_RESISTANCE) / leftValue;
        Logger.log("Left resistor: %f ohm", resistance);

        for (int i = 0; i < NTC100K_VALUES.length - 1; i++) {
            if (resistance < NTC100K_VALUES[i] && resistance > NTC100K_VALUES[i + 1]) {
                double temperature = (i + MIN_TEMP) + (1 / (NTC100K_VALUES[i + 1] -
                    NTC100K_VALUES[i])) *
                    (resistance -
                        NTC100K_VALUES[i]);
                Logger.log("Temperature: %f ˚C", temperature);
                break;
            }
        }
    }


    public static AnalyzerResult resultFromAnalyzingData(short[] data) {

        AnalyzerResult result = new AnalyzerResult();

        int maxSample = clippingDetectedInBuffer(data);

        if (maxSample > Short.MAX_VALUE) {
            result.error = new Exception("Clipping occurred");
            return result;
        }

        List<Sample> samples = samplesFromBuffer(data);
        List<Frame> frames = framesFromSamples(samples);

        result.numberOfFrames = frames.size();

        int nFrames = frames.size();

        if (nFrames == 0) {
            result.error = new Exception("No Frames were found");
            return result;
        }

        List<Float> intersectionValues = new ArrayList<Float>();

        for (int i = 0; i < nFrames; i++) {
            List<Cell> cells = frames.get(i).cells;

            Trendline trendline = getTrendlineFromCells(cells);

            float intersection = xAxisIntersectionOfTrendline(trendline);
            intersectionValues.add(intersection);
        }

        //Get all needed values
        float medianIntersection = medianValueOfList(intersectionValues);
        float cancellationAmplitude = cancellationAmplitudeFromAbscissaIntersection
            (medianIntersection);
        float resistance = resistanceFromCancellationAmplitude(cancellationAmplitude);
        float temperature = temperatureFromResistance(resistance);

        result.temperature = temperature;
        result.resistance = resistance;

        return result;
    }

    /**
     * Returns the largest sample value or <b>Integer.MAX_VALUE</b> if clipping occurred.
     */
    public static int clippingDetectedInBuffer(short[] data) {
        int clippedSamples = 0;

        int maxSample = 0;

        for (short sampleAmplitude : data) {
            if (sampleAmplitude > maxSample)
                maxSample = sampleAmplitude;

            if (sampleAmplitude > CLIPPING_THRESHOLD) {
                clippedSamples++;
                if (clippedSamples > 10)
                    return Integer.MAX_VALUE;
            }
        }

        return maxSample;
    }


    /**
     * Extracts zero, high and low sample from the buffer.
     *
     * @param data Buffer to analyze.
     * @return {@link java.util.List} of samples, which contains only zero and extreme points.
     */
    public static List<Sample> samplesFromBuffer(short[] data) {
        List<Sample> samples = new ArrayList<Sample>();

        Sample previousZeroSample = null;

        int previousZeroIndex = 0;

        for (int sampleIndex = 1; sampleIndex < data.length; sampleIndex++) {
            short currentSampleAmplitude = data[sampleIndex];
            short previousSampleAmplitude = data[sampleIndex - 1];

            boolean previousSampleIsPositive = previousSampleAmplitude >= 0;
            boolean currentSampleIsPositive = currentSampleAmplitude >= 0;

            if (previousSampleIsPositive != currentSampleIsPositive) {
                int deltaOffset = sampleIndex - previousZeroIndex;

                Sample zeroSample = new Sample(currentSampleAmplitude, sampleIndex, deltaOffset,
                    Sample.SampleType.ZERO);

                if (previousZeroSample != null) {
                    Sample extremeSample = findExtremeSampleInBuffer(data,
                        previousZeroSample.bufferIndex, zeroSample.bufferIndex);

                    if (extremeSample != null) {
                        samples.add(previousZeroSample);
                        samples.add(extremeSample);
                    }
                }

                previousZeroSample = zeroSample;
                previousZeroIndex = sampleIndex;
            }
        }

        return samples;
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
    private static Sample findExtremeSampleInBuffer(short[] data, int fromIndex, int toIndex) {

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

        return new Sample(selectedAmplitude, highestAmplitudeIndex, 0, type);
    }

    /**
     * Detects frames boundaries and returns a list of {@link com.robocatapps.thermodosdk.model
     * .Frame}s with recognized cells inside.
     *
     * @param samples A list of samples containing only zero and extremal values.
     */
    private static List<Frame> framesFromSamples(List<Sample> samples) {
        List<Frame> frames = new ArrayList<Frame>();
        int syncSamplesPerHalfPeriod = SAMPLES_PER_CELL / PERIODS_PER_CELL / 4;

        int syncSamplesCount = 0;
        int frameStartIndex = 0;
        int frameEndIndex = 0;

        for (int i = 0; i < samples.size(); i++) {
            Sample sample = samples.get(i);

            if (sample.sampleType != Sample.SampleType.ZERO) {
                continue;
            }

            // If the signal has double frequency
            if (roundToNearestMultiple(sample.deltaBufferIndex, syncSamplesPerHalfPeriod) ==
                syncSamplesPerHalfPeriod) {
                if (frameStartIndex > 0 && frameEndIndex == 0) {
                    // Frame start has been already detected, so this sync may point to the end
                    // of the frame. We will continue counting sync samples and will use this
                    // index as the end of the frame if we detect a sync cell.
                    frameEndIndex = i;
                }
                syncSamplesCount++;
            } else {
                // If the difference between expected and detected number
                // of sync periods is less than 3 samples, we can mark the start of the new frame
                // and start cells recognition for the previous frame if its end was marked.
                if (Math.abs(syncSamplesCount - PERIODS_PER_CELL * 4) < 3) {
                    if (frameEndIndex > 0) {
                        Frame frame = frameWithCellsFromSamples(samples, frameStartIndex,
                            frameEndIndex);
                        frames.add(frame);
                    }
                    frameStartIndex = i;
                }
                syncSamplesCount = 0;
                frameEndIndex = 0;
            }
        }

        return frames;
    }

    /**
     * Detects cells within the frame specified by its start and end sample indexes.
     *
     * @param samples    A list of samples containing only zero and extremal values.
     * @param startIndex Index of the frame start sample.
     * @param endIndex   Index of the frame end sample.
     * @return {@link com.robocatapps.thermodosdk.model.Frame} object with the list of detected
     * cells.
     */
    private static Frame frameWithCellsFromSamples(List<Sample> samples, int startIndex,
                                                   int endIndex) {
        // Create a sublist with samples of the current frame, remove all zero samples and set
        // buffer indexes relative to the startIndex.
        int fromIndex = samples.get(startIndex).bufferIndex;
        int indexCount = samples.get(endIndex).bufferIndex - fromIndex;
        List<Sample> minMaxSamples = new ArrayList<Sample>();
        for (int i = startIndex; i <= endIndex; i++) {
            Sample sample = samples.get(i);
            if (sample.sampleType == Sample.SampleType.ZERO)
                continue;

            minMaxSamples.add(new Sample(sample.amplitude, sample.bufferIndex - fromIndex,
                sample.deltaBufferIndex, sample.sampleType));
        }

        List<Cell> cells = new ArrayList<Cell>();

        int pointIndex = 0;
        List<Float> amplitudesInCell = new ArrayList<Float>();

        // We don't analyze sync cells, so using NUMBER_OF_CELLS - 1
        for (int cellIndex = 0; cellIndex < Constants.NUMBER_OF_CELLS - 1; cellIndex++) {
            amplitudesInCell.clear();

            for (; pointIndex < minMaxSamples.size(); pointIndex++) {
                Sample sample = minMaxSamples.get(pointIndex);

                if (sample.bufferIndex > (cellIndex + 1) * SAMPLES_PER_CELL) {
                    pointIndex--;
                    break;
                }

                amplitudesInCell.add(Math.abs((float) sample.amplitude));
            }

            short amplitude = amplitudesInCell.size() == 0 ? 0 : (short) medianValueOfList
                (amplitudesInCell);
            cells.add(new Cell(amplitude, cellIndex));
        }

        short lowestValue = cells.size() == 0 ? 0 : cells.get(0).amplitude;
        int lowestIndex = 0;
        for (int i = 1; i < cells.size(); i++) {
            Cell cell = cells.get(i);

            if (cell.amplitude < lowestValue) {
                lowestValue = cell.amplitude;
                lowestIndex = i;
            }
        }

        // Invert amplitudes of cells after the lowest one
        for (int i = lowestIndex; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            cell.amplitude = (short) -cell.amplitude;
        }

        // Remove the cell with the lowest amplitude
        cells.remove(lowestIndex);

        return new Frame(cells);
    }


    /**
     * Creates {@link com.robocatapps.thermodosdk.model.Trendline} object from the specified {@link
     * java.util.List} of {@link com.robocatapps.thermodosdk.model.Cell}'s.
     */
    private static Trendline getTrendlineFromCells(List<Cell> cells) {
        int numberOfCells = cells.size();

        float sumX = 0;
        float sumY = 0;
        float sumXY = 0;
        float sumXX = 0;

        for (int cellIndex = 0; cellIndex < numberOfCells; cellIndex++) {
            Cell cell = cells.get(cellIndex);

            int x = cell.cellIndex * SAMPLES_PER_CELL + (SAMPLES_PER_CELL / 2);
            int y = cell.amplitude;

            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
        }

        float a = (float) ((numberOfCells * sumXY - sumX * sumY) / (numberOfCells * sumXX - Math
            .pow(sumX, 2)));
        float b = (sumY - a * sumX) / numberOfCells;

        return new Trendline(a, b);
    }


    /**
     * Finds an intersection of the specified {@link com.robocatapps.thermodosdk.model.Trendline}
     * with the abscissa axis.
     */
    private static float xAxisIntersectionOfTrendline(Trendline trendLine) {

        float intersection = -trendLine.intersection / trendLine.slope;

        float intersectionLocal = intersection / SAMPLES_PER_FRAME;
        //TODO figure out what these magic numbers mean
        intersectionLocal = (float) ((intersectionLocal - 1.0 / 18.0) / (1 - 1.0 / 9.0));

        return intersectionLocal;
    }

    private static float cancellationAmplitudeFromAbscissaIntersection(float intersection) {
        return UPPER_AMPLITUDE - intersection * (UPPER_AMPLITUDE - LOWER_AMPLITUDE);
    }

    private static float resistanceFromCancellationAmplitude(float cancellationAmplitude) {
        return (float) ((cancellationAmplitude / REFERENCE_AMPLITUDE) * REF_RESISTANCE);
    }

    /**
     * Finds closest value of the temperature taking in count specified resistance value.
     *
     * @return A value of the temperature for the specified resistance. {@code Float.NaN} if
     * temperature couldn't be determined.
     */
    private static float temperatureFromResistance(float resistance) {
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
     * Returns median value from the specified list.
     */
    private static float medianValueOfList(List<Float> values) {
        if (values.size() == 0) return 0;
        if (values.size() <= 2) return values.get(0);

        //Create a list which will contain a copy of specified array
        List<Float> copyList = new ArrayList<Float>(values);

        Collections.sort(copyList);

        //Get median value
        return copyList.get(copyList.size() / 2);
    }

    public static int roundToNearestMultiple(int value, int base) {
        return Math.round((float) value / base) * base;
    }
}
