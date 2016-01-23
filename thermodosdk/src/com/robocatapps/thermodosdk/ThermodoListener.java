package com.robocatapps.thermodosdk;

/**
 * Interface for receiving notifications for events related to the Thermodo Sensor.
 * <p/>
 * All the methods will be called on the main thread.
 */
public interface ThermodoListener {

    /**
     * Called when the Thermodo SDK starts measuring.
     */
    public void onStartedMeasuring();

    /**
     * Called when the Thermodo SDK stops measuring.
     */
    public void onStoppedMeasuring();

    /**
     * Called when a new temperature reading has been made available. The measurement unit of
     * the provided temperature values is Celsius.
     *
     * @param temperature the measured temperature value, in degrees Celsius
     */
    public void onTemperatureMeasured(float temperature);

    /**
     * Called when an error has occurred during the initialization or the measurements done by
     * the Thermodo. <p>Common error codes are found in the Thermodo interface.</p>
     */
    public void onErrorOccurred(int what);

    /**
     * Called when system permissions are not set, we request an Activity to present the permission dialog upon.
     */
    public void onPermissionsMissing();
}
