package com.robocatapps.thermodosdk.model;

public class Sample {

    public enum SampleType {
        ZERO,
        MAX,
        MIN
    }

    private short mAmplitude;
    private int mBufferIndex;
    private int mDeltaBufferIndex;
    private SampleType mSampleType;

    public short getAmplitude() {
        return mAmplitude;
    }

    public int getBufferIndex() {
        return mBufferIndex;
    }

    public int getDeltaBufferIndex() {
        return mDeltaBufferIndex;
    }

    public SampleType getSampleType() {
        return mSampleType;
    }

    public Sample(short amplitude, int bufferIndex, int deltaBufferIndex, SampleType sampleType) {
        set(amplitude, bufferIndex, deltaBufferIndex, sampleType);
    }

    public Sample set(short amplitude, int bufferIndex, int deltaBufferIndex, SampleType sampleType) {
        mAmplitude = amplitude;
        mBufferIndex = bufferIndex;
        mDeltaBufferIndex = deltaBufferIndex;
        mSampleType = sampleType;
        return this;
    }
}
