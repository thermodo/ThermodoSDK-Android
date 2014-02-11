package com.robocatapps.thermodosdk;

import android.content.Context;

/**
 * A factory for obtaining Thermodo instances.
 */
public class ThermodoFactory {

	private static Thermodo sInstance;

	/**
	 * Returns an instance of the {@link Thermodo} object associated with the Application's context.
	 */
	public static Thermodo getInstance(Context context) {
		if (sInstance == null)
			sInstance = new ThermodoImpl(context.getApplicationContext());
		return sInstance;
	}
}
