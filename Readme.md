<p align="center">
  <a href="http://developer.thermodo.com/"><img src="http://thermodo.com/assets/images/thermodo_logo.jpg" alt="Thermodo" title="Thermodo" /></a>
</p>

The ThermodoSDK allows you to integrate [Thermodo](http://thermodo.com) into your Android application. ThermodoSDK provides a simple interface for interacting with Thermodo and getting temperature readings into your application.

Thermodo is a tiny electrical thermometer for your mobile device that lets you measure the temperature right where you are. For more information about Thermodo please checkout our [website](http://thermodo.com) or watch the [Kickstarter Video](http://www.kickstarter.com/projects/robocat/thermodo-the-tiny-thermometer-for-mobile-devices).

## Installation
### Using Gradle
<!---
If you are using Gradle for building your Android project, you can simply add a dependency for this library, specifying the packaging type of 'aar':
```
dependencies {
    // ... other dependencies
    compile 'com.robocatapps:thermodosdk:1.0.+@aar'
}
```
--->

If you are using Gradle for building your Android project, you must follow the these steps:

1. Download the latest [version](https://github.com/thermodo/ThermodoSDK-Android) of the SDK
2. Add the `thermodosdk` folder as a module in your Gradle project
3. Add the following dependency in your gradle build file:
```
dependencies {
    // ... other dependencies
    compile project (':thermodosdk')
}
```

### Using Eclipse Development Environment
If you are using the Eclipse Development Environment with the ADT plugin version 0.9.7 or greater you can include the ThermodoSdk project as a library project, following these steps:

1. Download the latest [version](https://github.com/thermodo/ThermodoSDK-Android) of the SDK
2. Create a new Android project in Eclipse using the `thermodosdk` folder as the existing source
3. In the project properties, add the created project under the ‘Libraries’ section of the ‘Android’ category.

<!---## Using Maven
If you are using Maven for building your Android project, you can simply add a dependency for this library:
```
<dependency>
  <groupId>com.robocatapps</groupId>
  <artifactId>thermodosdk</artifactId>
  <version>1.0.0</version>
  <type>apklib</type>
</dependency>
```
--->

##Usage

To use the ThermodoSDK in your Android project, 3 separate steps need to be followed:
1. Declare the proper permissions required by the SDK
2. Implement a `ThermodoListener` that will be notified of Thermodo related events
3. Create a `Thermodo` instance and start measuring

More details about each of these steps cand be found below. 

A full sample application that uses the ThermodoSDK can be found in the **_thermodosdk-sample_** folder.

###Permissions
The first thing that needs to be configured in a project that uses the ThermodoSDK is the set up of the required permissions. Make sure you declare the following permissions in your Android manifest file:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
```

###ThermodoListener
After being started, the ThermodoSDK communicates with your application through a registered `ThermodoListener`. Thus, you need to define a class implementing the `ThermodoListener` interface and defining the following methods:
```java
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
     * the Thermodo. Common error codes are found in the Thermodo interface.
     */
    public void onErrorOccurred(int what);
```
All of these methods will be called on the main thread of the application, so make sure you take this into account.

###Creating a Thermodo instance 
You can interact with the Thermodo device through the `Thermodo` interface provided by the SDK. It allows you to control and check the current state of the measuring process (start, stop) and to configure the associated `ThermodoListener`.

In order to get hold of a `Thermodo` instance, a method is available in the `ThermodoFactory` class. This will return a singleton instance of a `Thermodo`, associated to the application's context:
```java
    Thermodo thermodo = ThermodoFactory.getThermodoInstance(getContext());
```

As stated before, the ThermodoSDK will notify your application of any related events and available readings through a registered `ThermodoListener`. Thus, make sure you have implemented this interface and registered the listener with the `Thermodo` instance:
```java
    ThermodoListener myThermodoListener = new MyThermodoListenerImplementation();
    thermodo.setThermodoListener(myThermodoListenerImplementation);
```

The final step needed is to actually start `Thermodo` so it can detect inserted Thermodo devices and start providing temperature readings from the device. This is done using the `start()` method of the `Thermodo` instance:
```java
    thermodo.start();
```
After being started, `Thermodo` continues to run until it is stopped, keeping control of some required system resources (for example microphone, audio focus etc.), so the recommended approach is to make sure it is stopped as soon as it is not needed anymore. As a best practice, we recommend starting the `Thermodo` instance when the corresponding activity is shown, and stopping it as soon as it is not visible anymore:
```java
    @Override
    protected void onStart() {
        super.onStart();
		mThermodo.start();
	}
    
    @Override
	protected void onStop() {
		super.onStop();
		mThermodo.stop();
	}
```
**NOTE**: An important requirement of the SDK is to call the `start()` and `stop()` methods on the main thread of the application.

That's it! If you have followed the steps above, at this point you should have the ThermodoSDK integrated into your application and it should get the measurement results as soon as a Thermodo device is plugged in.


## FAQ

### Can I try/use ThermodoSDK without a Thermodo Device?

In general, you will need a real Thermodo device build anything meaningful with this SDK. However, in case you need to run your app in an emulator (e.g. for automated UI tests on a build server), we've provided a `MockThermodo` class you can use in that case.

Real Thermodo devices can be ordered from [our website](http://thermodo.com).

### The readings from the device are too hot

We have used a lot of time on making sure that Thermodo delivers accurate and consistent temperature readings. Unfortunately this precision also means that the Thermodo sensor is easily affected by heat from your Android device or just the palm of your hand. To get the most accurate readings we recommend using an extension cord. For more information see [this video update from the Kickstarter project](http://vimeo.com/76458958).

## Contributing

We love our community and our fellow developers. If you find some way to improve ThermodoSDK or find a bug somewhere please let us know by [opening an issue](https://github.com/thermodo/ThermodoSDK-Android/issues/new).

