# RxBonjour
Say "Hi!" to RxBonjour, a wrapper around Android's network service discovery functionalities with a support implementation for devices below Jelly Bean, going down all the way to API level 8.

Disclaimer: This library is to be considered **very early** in terms of maturity. There will probably be issues, especially since NSD is a fragile topic on Android!

## Download

```groovy
As of now, this project isn't available as a Maven artifact. I'm working on it!
```

## Usage

Start a network service discovery using `RxBonjour.startDiscovery(Context, String)` and subscribe to the returned `Observable`:

```java
RxBonjour.startDiscovery(this, "_http._tcp")
		.subscribe(new Action1<BonjourEvent>() {
			@Override public void call(BonjourEvent bonjourEvent) {
				BonjourService item = bonjourEvent.getService();
				switch (bonjourEvent.getType()) {
					case ADDED:
						// Called when a service was discovered
						break;

					case REMOVED:
						// Called when a service is no longer visible
						break;
				}
			}
		}, new Action1<Throwable>() {
			@Override public void call(Throwable throwable) {
				// ...
			}
		});
```

RxBonjour pre-configures the returned Observables to run on an I/O thread, but return their callbacks on the main thread. The discovery will be stopped automatically upon unsubscribing from the Observable.

## Implementations

RxBonjour comes with two implementations for network service discovery.

### NsdManager implementation (v16)

On devices running Jelly Bean and up, Android's native Network Service Discovery API, centered around `NsdManager`, is used. If your app's minimum API level is 16 or higher, you don't need to do any additional setup in order to have RxBonjour ready to go.

### Support implementation (v8)

The support implementation utilizes [jmDNS 3.4.1][jmdns] and a `WifiManager` multicasts lock as its service discovery backbone; because of this, you need to add the following permissions to your `AndroidManifest.xml` in order to allow jmDNS to do its thing:

```xml
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
	