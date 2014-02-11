package com.robocatapps.thermodosdk;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kiva on 1/20/14.
 */
public class Logger {

    public static interface LoggerObserver {
        public void onLog(String message);
    }

    private static List<LoggerObserver> observers = new ArrayList<LoggerObserver>();

    public static void addObserver(LoggerObserver observer) {
        if(observer != null)
            observers.add(observer);
    }

    public static void removeObserver(LoggerObserver observer) {
        if(observer != null)
            observers.remove(observer);
    }

    public static void log(String message) {
        notifyObservers(message);
    }

    public static void log(String format, Object ... args) {
        notifyObservers(String.format(format, args));
    }

    private static void notifyObservers(String message) {
        Log.d("THERMODO", message);
        for(LoggerObserver observer : observers)
            observer.onLog(message);
    }
}
