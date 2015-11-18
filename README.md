# RxBonjour
Say "Hi!" to RxBonjour, a wrapper around Android's network service discovery functionalities with a support implementation for devices below Jelly Bean, going down all the way to API level 9.

Disclaimer: This library is to be considered **very early** in terms of maturity. There will probably be issues, especially since NSD is a fragile topic on Android!

## Download

`RxBonjour` is available on `jcenter()`:

```groovy
compile 'com.github.aurae:rxbonjour:0.3.2'
```

## Usage

Create a network service discovery request using `RxBonjour.startDiscovery(Context, String)` and subscribe to the returned `Observable` (this code is using Java 8 syntax for brevity):

```java
RxBonjour.startDiscovery(this, "_http._tcp")
		.subscribe(bonjourEvent -> {
			BonjourService item = bonjourEvent.getService();
			switch (bonjourEvent.getType()) {
				case ADDED:
					// Called when a service was discovered
					break;

				case REMOVED:
					// Called when a service is no longer visible
					break;
			}
		}, error -> {
			// Service discovery failed, for instance
		});
```

RxBonjour pre-configures the returned Observables to run on an I/O thread, but return their callbacks on the main thread. The discovery will be stopped automatically upon unsubscribing from the Observable.

## Implementations

RxBonjour comes with two implementations for network service discovery. By default, the support implementation is used because of the unreliable state of the `NsdManager` APIs and known bugs with that. If you **really** want to use `NsdManager` on devices running Jelly Bean and up though, you can specify this when creating service discovery Observables:

```java
// If you're feeling real and ready to reboot your device once NsdManager breaks, pass in "true" to use it for supported devices
RxBonjour.startDiscovery(this, "_http._tcp", true)
		.subscribe(bonjourEvent -> {
			// ...
		}, error -> {
			// ...
		});
```

### NsdManager implementation (v16)

On devices running Jelly Bean and up, Android's native Network Service Discovery API, centered around `NsdManager`, can be used.

### Support implementation (v9)

The support implementation utilizes the latest available version of [jmDNS][jmdns] (a snapshot of version **3.4.2**) and a `WifiManager` multicast lock as its service discovery backbone; because of this, including this library in your application's dependencies automatically adds the following permissions to your `AndroidManifest.xml`, in order to allow jmDNS to do its thing:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE"/>
```

## License

	Copyright 2015 Marcel Schnelle

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

	
 [jmdns]: https://github.com/openhab/jmdns
 [jit]: https://jitpack.io
	