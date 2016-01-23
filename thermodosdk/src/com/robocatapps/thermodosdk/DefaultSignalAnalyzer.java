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
import static com.robocatapps.thermodosdk.Constants.UPPER_AMPLITUDE;

/**
 * Analyzes an audio signal, calculating an {@link com.robocatapps.thermodosdk.model.AnalyzerResult}
 * NOTE: This class is not re-entrant, due to re-use of arrays during calculation.
 */
public class DefaultSignalAnalyzer extends AbstractAnalyzer {

    // Arrays used in resultFromAnalyzingData and framesWithCellsInSamples.
    // Allocated here to avoid constant re-allocation
    List<Sample> mSamples = new ArrayList<Sample>();
    List<Frame> mFrames = new ArrayList<Frame>();
    List<Sample> mMinMaxSamples = new ArrayList<Sample>();
    List<Float> mAmplitudesInCell = new ArrayList<Float>();
    List<Float> mIntersectionValues = new ArrayList<Float>();

    @Override
    public AnalyzerResult resultFromAnalyzingData(short[] data) {

        AnalyzerResult result = new AnalyzerResult();

        int maxSample = clippingDetectedInBuffer(data);

        if (maxSample > Short.MAX_VALUE) {
            result.error = new Exception("Clipping occurred");
            return result;
        }

        samplesFromBuffer(data, mSamples);
        framesFromSamples(mSamples, mFrames);

        result.numberOfFrames = mFrames.size();

        int nFrames = mFrames.size();

        if (nFrames == 0) {
            result.error = new Exception("No Frames were found");
            return result;
        }

        mIntersectionValues.clear();
        for (int i = 0; i < nFrames; i++) {
            List<Cell> cells = mFrames.get(i).cells;

            Trendline trendline = getTrendlineFromCells(cells);

            float intersection = xAxisIntersectionOfTrendline(trendline);
            mIntersectionValues.add(intersection);
        }

        //Get all needed values
        float medianIntersection = medianValueOfList(mIntersectionValues);
        float cancellationAmplitude = cancellationAmplitudeFromAbscissaIntersection
            (medianIntersection);
        float resistance = resistanceFromCancellationAmplitude(cancellationAmplitude);

        result.temperature = temperatureFromResistance(resistance);
        result.resistance = resistance;

        // Don't hold on to memory
        mSamplePool.recycleSamples(mSamples);
        mSamplePool.recycleSamples(mMinMaxSamples);
        mFrames.clear();
        mAmplitudesInCell.clear();
        mIntersectionValues.clear();

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
     * Detects frames boundaries and returns a list of {@link com.robocatapps.thermodosdk.model
     * .Frame}s with recognized cells inside.
     *
     * @param samples A list of samples containing only zero and extremal values.
     */
    private void framesFromSamples(List<Sample> samples, List<Frame> outFrames) {
        outFrames.clear();
        int syncSamplesPerHalfPeriod = SAMPLES_PER_CELL / PERIODS_PER_CELL / 4;

        int syncSamplesCount = 0;
        int frameStartIndex = 0;
        int frameEndIndex = 0;

        for (int i = 0; i < samples.size(); i++) {
            Sample sample = samples.get(i);

            if (sample.getSampleType() != Sample.SampleType.ZERO) {
                continue;
            }

            // If the signal has double frequency
            if (roundToNearestMultiple(sample.getDeltaBufferIndex(), syncSamplesPerHalfPeriod) ==
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
                        outFrames.add(frame);
                    }
                    frameStartIndex = i;
                }
                syncSamplesCount = 0;
                frameEndIndex = 0;
            }
        }
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
    private Frame frameWithCellsFromSamples(List<Sample> samples, int startIndex,
                                                   int endIndex) {
        // Create a sublist with samples of the current frame, remove all zero samples and set
        // buffer indexes relative to the startIndex.
        int fromIndex = samples.get(startIndex).getBufferIndex();
        mMinMaxSamples.clear();
        for (int i = startIndex; i <= endIndex; i++) {
            Sample sample = samples.get(i);
            if (sample.getSampleType() == Sample.SampleType.ZERO)
                continue;

            mMinMaxSamples.add(mSamplePool.createSample(sample.getAmplitude(),
                    sample.getBufferIndex() - fromIndex,
                    sample.getDeltaBufferIndex(),
                    sample.getSampleType()));
        }

        List<Cell> cells = new ArrayList<Cell>();

        int pointIndex = 0;
        mAmplitudesInCell.clear();

        // We don't analyze sync cells, so using NUMBER_OF_CELLS - 1
        for (int cellIndex = 0; cellIndex < Constants.NUMBER_OF_CELLS - 1; cellIndex++) {
            mAmplitudesInCell.clear();

            for (; pointIndex < mMinMaxSamples.size(); pointIndex++) {
                Sample sample = mMinMaxSamples.get(pointIndex);

                if (sample.getBufferIndex() > (cellIndex + 1) * SAMPLES_PER_CELL) {
                    pointIndex--;
                    break;
                }

                mAmplitudesInCell.add(Math.abs((float) sample.getAmplitude()));
            }

            if(mAmplitudesInCell.size() > 3) {
                mAmplitudesInCell.remove(0);
                mAmplitudesInCell.remove(mAmplitudesInCell.size()-1);
            }

            short amplitude = mAmplitudesInCell.size() == 0 ? 0 : (short) medianValueOfList
                (mAmplitudesInCell);
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
     * Returns median value from the specified list.
     * NOTE: A side-effect of calling this method is that the list provided will be sorted.
     */
    private static float medianValueOfList(List<Float> values) {
        if (values.size() == 0)
            return 0;
        if (values.size() <= 2)
            return values.get(0);

        Collections.sort(values);
        return values.get(values.size() / 2);
    }

    private static int roundToNearestMultiple(int value, int base) {
        return Math.round((float) value / base) * base;
    }
}
