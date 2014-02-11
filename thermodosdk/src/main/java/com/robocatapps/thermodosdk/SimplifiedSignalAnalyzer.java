package com.robocatapps.thermodosdk;

import com.robocatapps.thermodosdk.model.AnalyzerResult;
import com.robocatapps.thermodosdk.model.Sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimplifiedSignalAnalyzer extends AbstractAnalyzer {
    @Override
    public AnalyzerResult resultFromAnalyzingData(short[] data) {

        AnalyzerResult result = new AnalyzerResult();
        // Working simplified algorithm of temperature measurement
        int threshold = 1000;

        // Check all the values of the samples and find the start of the real signal and finish
        // of real signal
        int startIndex = 0;
        for (; startIndex < data.length; startIndex++) {
            if (Math.abs(data[startIndex]) > threshold)
                break;
        }
        int stopIndex = data.length - 1;
        for (; stopIndex > 0; stopIndex--) {
            if (Math.abs(data[stopIndex]) > threshold)
                break;
        }

        if (stopIndex == 0 && startIndex == data.length)
            return result;

        int newSamplesCount = stopIndex - startIndex + 1;
        short[] newSamples = new short[newSamplesCount];
        System.arraycopy(data, startIndex, newSamples, 0, newSamplesCount);

        int numberOfSamplesForAnalysis = (int) Math.round(newSamples.length * 0.5 * 0.75);
        short[] leftSamples = new short[numberOfSamplesForAnalysis];
        System.arraycopy(newSamples, (int) (numberOfSamplesForAnalysis * 0.05f), leftSamples, 0,
            numberOfSamplesForAnalysis);
        short[] rightSamples = new short[numberOfSamplesForAnalysis];
        System.arraycopy(newSamples, newSamples.length - (int) (numberOfSamplesForAnalysis * 1.05f),
            rightSamples, 0, numberOfSamplesForAnalysis);

        List<Sample> xSamples = samplesFromBuffer(leftSamples);
        List<Short> xValues = new ArrayList<Short>(0);
        for (Sample sample : xSamples) {
            if (sample.sampleType == Sample.SampleType.MAX) {
                xValues.add(sample.amplitude);
            } else if (sample.sampleType == Sample.SampleType.MIN) {
                xValues.add((short) -sample.amplitude);
            }
        }

        short leftAmplitude = medianValueOfShortList(xValues);

        xSamples = samplesFromBuffer(rightSamples);
        xValues = new ArrayList<Short>(0);
        for (Sample sample : xSamples) {
            if (sample.sampleType == Sample.SampleType.MAX) {
                xValues.add(sample.amplitude);
            } else if (sample.sampleType == Sample.SampleType.MIN) {
                xValues.add((short) -sample.amplitude);
            }
        }

        short rightAmplitude = medianValueOfShortList(xValues);

        Logger.log("Left ampl: %d , Right ampl: %d", leftAmplitude, rightAmplitude);
        float resistance = ((float) rightAmplitude) / leftAmplitude * 100.0f;
        float temperature = temperatureFromResistance(resistance);
        Logger.log("Temperature: " + temperature);

        result.temperature = temperature;
        result.numberOfFrames = 4;

        return result;
    }


    /**
     * Returns median value from the specified list.
     */
    private static short medianValueOfShortList(List<Short> values) {
        if (values.size() == 0) return 0;
        if (values.size() <= 2) return values.get(0);

        //Create a list which will contain a copy of specified array
        List<Short> copyList = new ArrayList<Short>(values);

        Collections.sort(copyList);

        //Get median value
        return copyList.get(copyList.size() / 2);
    }
}
