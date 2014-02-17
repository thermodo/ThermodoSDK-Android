package com.robocatapps.thermodosdk;

/**
 * The main interface that is used for interaction with the Thermodo sensor's measurements and
 * states. An instance is retrieved through
 * {@link ThermodoFactory#getThermodoInstance(android.content.Context)}.
 */
public interface Thermodo {

	/**
	 * The error returned when the audio focus could not be gained. If this error is encountered,
	 * the Thermodo measurement has not started.
	 */
	public static final int ERROR_AUDIO_FOCUS_GAIN_FAILED = 100;

	/**
	 * The error returned when Thermodo has tried to set the volume to the maximum volume, but has
	 * failed. This does not stop the measuring process, but if this error has ocurred the results
	 * might not be accurate.
	 */
	public static final int ERROR_SET_MAX_VOLUME_FAILED = 101;

	/**
	 * The error returned when a critical recorder failure has been detected while measuring. Before
	 * sending this error, the measurement is stopped, so this error is preceded by a call to {@link
	 * ThermodoListener#onStoppedMeasuring()}.
	 */
	public static final int ERROR_AUDIO_RECORD_FAILURE = 102;

	/**
	 * This method notifies the instance of this object to start detection of the temperature. Must
	 * be called on the main thread. <p>First of all it will set up volume-related settings to make
	 * measurements as precise as possible. After that, if the Thermodo device (or any headset) is
	 * inserted, actual measurements begin.
	 * <p/>
	 * Must be called on the main thread.
	 * <p/>
	 * <b>NOTE:</b> In the current version of the SDK, no sensor detection is done so, after this
	 * method has been called, any device inserted into the audio jack of the mobile device will be
	 * treated as a Thermodo device.
	 * <p/>
	 */
	public void start();

	/**
	 * Checks whether this Thermodo instance has been started and is prepared to measure.
	 */
	public boolean isRunning();

	/**
	 * Stops any measurement or detection actions.
	 * <p/>
	 * Must be called on the main thread.
	 */
	public void stop();

	/**
	 * Sets the ThermodoListener whose methods are called whenever some Thermodo related events
	 * occur.
	 *
	 * @see {@link Thermodo.ThermodoListener}
	 */
	public void setThermodoListener(ThermodoListener listener);

	/**
	 * Gets the ThermodoListener whose methods are called whenever some Thermodo related events
	 * occur.
	 *
	 * @see {@link com.robocatapps.thermodosdk.Thermodo.ThermodoListener}
	 */
	public ThermodoListener getThermodoListener();

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
	}
}
