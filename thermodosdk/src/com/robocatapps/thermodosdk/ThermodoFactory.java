package com.robocatapps.thermodosdk;

import android.content.Context;

/**
 * A factory for obtaining Thermodo instances.
 */
public class ThermodoFactory {

	private static Thermodo sInstance;
	private static Thermodo sMockInstance;

	/**
	 * Returns an instance of the {@link Thermodo} object associated with the Application's context.
	 * <p>The returned value is a singleton, so multiple calls to this method will return the same
	 * Thermodo instance, no matter what context is provided.</p>
	 */
	public static Thermodo getThermodoInstance(Context context) {
		if (sInstance == null)
			sInstance = new ThermodoImpl(context.getApplicationContext());
		return sInstance;
	}

	/**
	 * Returns a Mocked instance of a {@link Thermodo} object associated with the Application's
	 * context. This instance does not interact with any plugged in device and should be used during
	 * the development and for testing purposes only. <p>It simply provides temperatures oscillating
	 * between 14 and 24 degrees Celsius every minute while periodically signalling that the device
	 * has been unplugged and that the measuring has stopped.</p><p>The returned value is a
	 * singleton, so multiple calls to this method will return the same Thermodo instance, no matter
	 * what context is provided.</p>
	 */
	public static Thermodo getMockThermodoInstance(Context context) {
		if (sMockInstance == null)
			sMockInstance = new ThermodoMockImpl(context.getApplicationContext());
		return sMockInstance;
	}
}
