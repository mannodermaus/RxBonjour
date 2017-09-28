# RxBonjour
A reactive wrapper around network service discovery functionalities for Kotlin and Java.

## Download

**RxBonjour 2** is available on `jcenter()` and consists of three distinct components, all of which are detailed below.

```groovy
// Always include this
implementation "de.mannodermaus.rxjava2:rxbonjour:2.0.0-SNAPSHOT"

// Example: Usage on Android with JmDNS
implementation "de.mannodermaus.rxjava2:rxbonjour-platform-android:2.0.0-SNAPSHOT"
implementation "de.mannodermaus.rxjava2:rxbonjour-driver-jmdns:2.0.0-SNAPSHOT"
```

For the (less flexible & Android-only) RxJava 1 version, have a look at the [1.x][onex] branch.

## Components

**RxBonjour 2** is composed of a core library, a `Platform` to run on, and a `Driver` to access the NSD stack.

### Core Library (rxbonjour)

The main entry point to the API, `RxBonjour` is contained in this library. All other libraries depend on this common core.

### Platform Library (rxbonjour-platform-xxx)

Provides access to the host device's IP and Network controls.
During the creation of your `RxBonjour` instance, you attach exactly 1 implementation of the `Platform` interface.
 
Below is a list of available `Platform` libraries supported by **RxBonjour 2**:

|Group|Artifact|Description|
|---|---|---|
|`de.mannodermaus.rxjava2`|`rxbonjour-platform-android`|Android-aware Platform, utilizing `WifiManager` APIs|
|`de.mannodermaus.rxjava2`|`rxbonjour-platform-desktop`|Default JVM Platform|

#### About the AndroidPlatform

When running on Android, the `rxbonjour-platform-android` has to be applied to the module.
Doing so will add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE"/>
```

### Driver Library (rxbonjour-driver-xxx)

Provides the connection to a Network Service Discovery stack.
During the creation of your `RxBonjour` instance, you attach exactly 1 implementation of the `Driver` interface.

Below is a list of available `Driver` libraries supported by **RxBonjour 2**:

|Group|Artifact|Description|
|---|---|---|
|`de.mannodermaus.rxjava2`|`rxbonjour-driver-jmdns`|Service Discovery with [JmDNS][jmdns]|
|`de.mannodermaus.rxjava2`|`rxbonjour-driver-nsdmanager`|Service Discovery with Android's [NsdManager][nsdmanager] APIs|

## Usage

### Creation

Configure a `RxBonjour` service object using its `Builder`,
attaching your desired `Platform` and `Driver` implementations.
If you forget to provide either dependency, an Exception will be thrown:

```kotlin
val rxBonjour = RxBonjour.Builder()
    .platform(AndroidPlatform.create(this))
    .driver(JmDNSDriver.create())
    .create()
```

Your `RxBonjour` is ready for use now!

### Discovery

Create a network service discovery request using `RxBonjour#newDiscovery(String)`:

```kotlin
val disposable = rxBonjour.newDiscovery("_http._tcp")
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
        { event ->
            when(event) {
                is BonjourEvent.Added -> println("Resolved Service: ${event.service}")
                is BonjourEvent.Removed -> println("Lost Service: ${event.service}")
            }
        },
        { error -> println("Error during Discovery: ${error.message}") }
    )
```

Make sure to off-load this work onto a background thread, since the library won't enforce any threading. 
In this example, *RxAndroid* is utilized to return the events back to Android's main thread.

## Registration

Configure your advertised service & start the broadcast using `RxBonjour#newBroadcast(BonjourBroadcastConfig)`.
The only required property to set on a `BonjourBroadcastConfig` is its Bonjour type, the remaining parameters
are filled with defaults as stated in the comments below:

```kotlin
val broadcastConfig = BonjourBroadcastConfig(
        type = "_http._tcp",
        name = "My Bonjour Service",        // default: "RxBonjour Service"
        address = null,                     // default: Fallback to WiFi address provided by Platform
        port = 13337,                       // default: 80
        txtRecords = mapOf(                 // default: Empty Map
                "my.record" to "my value",
                "other.record" to "0815"))
                
val disposable = rxBonjour.newBroadcast(broadcastConfig)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe()
```

The broadcast is valid until the returned `Completable` is unsubscribed from.
Again, make sure to off-load this work onto a background thread like above, since the library won't do it for you.

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
 [nsdmanager]: https://developer.android.com/reference/android/net/nsd/NsdManager
 [jit]: https://jitpack.io
 [onex]: https://github.com/aurae/RxBonjour/tree/1.x
	