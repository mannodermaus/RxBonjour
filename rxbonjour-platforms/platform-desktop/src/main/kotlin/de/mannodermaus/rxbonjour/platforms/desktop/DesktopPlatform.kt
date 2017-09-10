package de.mannodermaus.rxbonjour.platforms.desktop

import de.mannodermaus.rxbonjour.Platform
import de.mannodermaus.rxbonjour.PlatformConnection
import io.reactivex.disposables.Disposable
import java.net.InetAddress

class DesktopPlatform private constructor() : Platform {
    override fun createConnection(): PlatformConnection {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getWifiAddress(): InetAddress {
        // TODO How to go about this?
        return InetAddress.getLocalHost()
    }

    override fun runOnTeardown(action: () -> Unit): Disposable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        @JvmStatic
        fun create(): Platform = DesktopPlatform()
    }
}
