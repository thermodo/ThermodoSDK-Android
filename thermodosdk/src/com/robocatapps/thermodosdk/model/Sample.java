package com.robocatapps.thermodosdk.model;

public class Sample {


    public enum SampleType {
        ZERO,
        MAX,
        MIN
    }

    public final short amplitude;
    public final int bufferIndex;
    public final int deltaBufferIndex;
    public final SampleType sampleType;

    public Sample(short amplitude, int bufferIndex, int deltaBufferIndex, SampleType sampleType) {
        this.amplitude = amplitude;
        this.bufferIndex = bufferIndex;
        this.deltaBufferIndex = deltaBufferIndex;
        this.sampleType = sampleType;
    }

}
