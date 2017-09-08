# RxBonjour
A wrapper around Android's network service discovery functionalities with a support implementation for devices below Jelly Bean, going down all the way to API level 14.

## Download

`RxBonjour` is available on `jcenter()`:

```groovy
api "de.mannodermaus.rxjava2:rxbonjour:2.0.0-beta1"
```

For the RxJava 1 version, have a look at the [1.x][onex] branch.

## Discovery

Create a network service discovery request using `RxBonjour.newDiscovery(Context, String)` and subscribe to the returned `Flowable`:

```java
RxBonjour.newDiscovery(this, "_http._tcp")
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

Make sure to off-load this work onto a background thread, since RxBonjour won't enforce any threading on the Flowable.

## Registration

Create a service to broadcast using `RxBonjour.newBroadcast(Context, String)` and subscribe to the returned `Flowable` of the broadcast object:

```java
BonjourBroadcast<?> broadcast = RxBonjour.newBroadcast("_http._tcp")
	.name("My Broadcast")
	.port(65335)
	.build();
	
broadcast.start(this)
	.subscribe(bonjourEvent -> {
		// Same as above
	});
```

Again, make sure to off-load this work onto a background thread, since RxBonjour won't enforce any threading on the Flowable.

## Implementations

RxBonjour comes with two implementations for network service discovery. By default, the support implementation is used because of the unreliable state of the `NsdManager` APIs and known bugs with that. If you **really** want to use `NsdManager` on devices running Jelly Bean and up though, you can specify this when creating service discovery Flowables:

```java
// If you're feeling real and ready to reboot your device once NsdManager breaks, pass in "true" to use it for supported devices
RxBonjour.newDiscovery(this, "_http._tcp", true)
		.subscribe(bonjourEvent -> {
			// ...
		}, error -> {
			// ...
		});
```

### NsdManager implementation (v16)

On devices running Jelly Bean and up, Android's native Network Service Discovery API, centered around `NsdManager`, can be used.

### Support implementation (v14)

The support implementation utilizes [jmDNS][jmdns] and a `WifiManager` multicast lock as its service discovery backbone;
because of this, including this library in your application's dependencies automatically adds the following permissions to your `AndroidManifest.xml`, in order to allow jmDNS to do its thing:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE"/>
```

## License

	Copyright 2017 Marcel Schnelle

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
 [onex]: https://github.com/aurae/RxBonjour/tree/1.x
	