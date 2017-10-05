package de.mannodermaus.rxbonjour

import io.reactivex.disposables.Disposable
import java.net.InetAddress

interface Platform {
  fun getWifiAddress(): InetAddress
  fun runOnTeardown(action: () -> Unit): Disposable?
  fun createConnection(): PlatformConnection
}

interface PlatformConnection {
  fun initialize()
  fun teardown()
}
