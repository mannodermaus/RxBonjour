package de.mannodermaus.rxbonjour.platforms.android

import android.content.Context
import android.net.wifi.WifiManager
import de.mannodermaus.rxbonjour.Platform
import de.mannodermaus.rxbonjour.PlatformConnection
import io.reactivex.android.MainThreadDisposable
import io.reactivex.disposables.Disposable
import java.net.InetAddress

private val WIFI_MULTICAST_LOCK_TAG = "RxBonjour Android Multicast Lock"

class AndroidPlatform
private constructor(private val context: Context) : Platform {

    override fun createConnection(): PlatformConnection = AndroidConnection(context)

    override fun getWifiAddress(): InetAddress {
        val intAddress = context.getWifiManager().connectionInfo.ipAddress
        val byteAddress = byteArrayOf(
                (intAddress and 0xff).toByte(),
                (intAddress shr 8 and 0xff).toByte(),
                (intAddress shr 16 and 0xff).toByte(),
                (intAddress shr 24 and 0xff).toByte())
        return InetAddress.getByAddress(byteAddress)
    }

    override fun runOnTeardown(action: () -> Unit): Disposable? = object : MainThreadDisposable() {
        override fun onDispose() {
            action.invoke()
        }
    }

    companion object {
        @JvmStatic
        fun create(context: Context): Platform = AndroidPlatform(context)
    }
}

private class AndroidConnection
constructor(val context: Context)
    : PlatformConnection {

    private lateinit var multicastLock: WifiManager.MulticastLock

    override fun initialize() {
        multicastLock = context.getWifiManager().createMulticastLock(WIFI_MULTICAST_LOCK_TAG)
        multicastLock.setReferenceCounted(true)
        multicastLock.acquire()
    }

    override fun teardown() {
        multicastLock.release()
    }
}

/* Extension Functions */

private fun Context.getWifiManager(): WifiManager =
        this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
