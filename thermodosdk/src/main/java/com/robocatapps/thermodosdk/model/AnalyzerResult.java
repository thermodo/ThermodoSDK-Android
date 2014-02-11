package com.robocatapps.thermodosdk.model;

import java.util.List;

public class AnalyzerResult {

    public float temperature;
    public float resistance;

    public float ratio;
    public float intersection;
    public float trendlineIntersection;
    public float trendlineSlope;
    public float maxSample;
    public int numberOfFrames;
    public String baseCellType;

    public List<Sample> samples;
    public List<Cell> cells;
    public Throwable error;
}
