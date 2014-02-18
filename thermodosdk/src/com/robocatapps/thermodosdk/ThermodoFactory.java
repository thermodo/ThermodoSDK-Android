package com.robocatapps.thermodosdk;

import android.content.Context;

/**
 * A factory for obtaining Thermodo instances.
 */
public class ThermodoFactory {

	private static Thermodo sInstance;

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
}
