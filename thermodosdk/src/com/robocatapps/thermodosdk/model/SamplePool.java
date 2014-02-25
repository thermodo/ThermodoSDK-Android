package com.robocatapps.thermodosdk.model;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

/**
 * Class managing a pool of {@link com.robocatapps.thermodosdk.model.Sample} objects.
 */
public class SamplePool {
    private Queue<Sample> mPool = new ArrayDeque<Sample>();

    public Sample createSample(short amplitude, int bufferIndex, int deltaBufferIndex,
                               Sample.SampleType sampleType) {
        if (mPool.isEmpty())
            return new Sample(amplitude, bufferIndex, deltaBufferIndex, sampleType);
        else
            return mPool.remove().set(amplitude, bufferIndex, deltaBufferIndex, sampleType);
    }

    public void recycleSample(Sample sample) {
        mPool.add(sample);
    }

    public void recycleSamples(Collection<Sample> samples) {
        for (Sample s : samples)
            mPool.add(s);
        samples.clear();
    }
}
