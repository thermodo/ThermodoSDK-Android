package com.robocatapps.thermodosdk.model;

public class Cell {

    public short amplitude;
    public final int cellIndex;

    public Cell(short amplitude, int cellIndex) {
        this.amplitude = amplitude;
        this.cellIndex = cellIndex;
    }

}
