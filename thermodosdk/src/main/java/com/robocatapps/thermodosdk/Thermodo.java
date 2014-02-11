package com.robocatapps.thermodosdk;

/**
 * The main interface that is used for interaction with the Thermodo Sensor's measurements and states.
 */
public interface Thermodo {

	/**
	 * The error returned when the audio focus could not be gained.
	 */
	public static final int ERROR_AUDIO_FOCUS_GAIN_FAILED = 100;

	/**
	 * This method notifies the instance of this object to start detection of the temperature. Must be called on the main thread.
	 * <p>First of all it will setup additional settings to make measurements precise as possible.
	 * After that, if Thermodo device (or any headset) is inserted, it will start device detection algorithm,
	 * after which, actual measurements begin
	 * <p/>
	 */
	public void start();

	/**
	 * Checks whether the Thermodo detection algorithm is currently running.
	 */
	public boolean isRunning();

	/**
	 * Stops any measurement or detection actions. Must be called on the main thread.
	 */
	public void stop();

	/**
	 * Sets the ThermodoListener whose methods are called whenever some Thermodo related events occur.
	 */
	public void setThermodoListener(ThermodoListener listener);

	/**
	 * Gets the ThermodoListener whose methods are called whenever some Thermodo related events occur.
	 */
	public ThermodoListener getThermodoListener();

	/**
	 * Sets whether a check is made to make sure the device connected to the audio jack is a Thermodo
	 * or something else (e.g. headphones, microphone). By default, a check is done.
	 */
	public void setEnabledDeviceCheck(boolean newValue);

	/**
	 * Checks whether a verification is made to make sure the device connected to the audio jack is a Thermodo
	 * or something else (e.g. headphones, microphone).
	 */
	public boolean isEnabledDeviceCheck();

	/**
	 * Interface for receiving notifications for events related to the Thermodo Sensor. Methods will be called on
	 * the main thread.
	 */
	public interface ThermodoListener {

		/**
		 * Called when the Thermodo SDK starts the measuring.
		 */
		public void onStartedMeasuring();

		/**
		 * Called when the Thermodo SDK stops the measuring.
		 */
		public void onStoppedMeasuring();

		/**
		 * Called when a new temperature reading has been made available. The measurement system used for the
		 * provided temperature values is Celsius.
		 *
		 * @param temperature the measured temperature value, in degrees Celsius
		 */
		public void onGotTemperature(float temperature);

		/**
		 * Called when the Thermodo SDK detects that the sensor has been plugged in.
		 */
		public void onThermodoPluggedIn();

		/**
		 * Called when the Thermodo SDK detects that the sensor has been unplugged.
		 */
		public void onThermodoUnplugged();

		/**
		 * Called when an error has occurred during the initialization or the measurements done by the Thermodo.
		 * <p>Common error codes are found in the Thermodo interface.</p>
		 */
		public void onErrorOccurred(int what);
	}
}
