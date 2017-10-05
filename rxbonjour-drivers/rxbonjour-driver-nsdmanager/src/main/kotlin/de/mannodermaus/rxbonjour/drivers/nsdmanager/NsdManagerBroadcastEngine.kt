package de.mannodermaus.rxbonjour.drivers.nsdmanager

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import de.mannodermaus.rxbonjour.BonjourBroadcastConfig
import de.mannodermaus.rxbonjour.BroadcastCallback
import de.mannodermaus.rxbonjour.BroadcastEngine
import java.net.InetAddress

internal class NsdManagerBroadcastEngine(private val context: Context) : BroadcastEngine {

  private var nsdManager: NsdManager? = null
  private var listener: NsdRegistrationListener? = null

  override fun initialize() {
  }

  override fun start(address: InetAddress, config: BonjourBroadcastConfig,
      callback: BroadcastCallback) {
    val nsdManager = context.getNsdManager()
    val listener = NsdRegistrationListener(callback)

    this.nsdManager = nsdManager
    this.listener = listener

    val nsdService = config.copy(address = config.address ?: address).toNsdModel()
    nsdManager.registerService(nsdService, NsdManager.PROTOCOL_DNS_SD, listener)
  }

  override fun teardown() {
    try {
      nsdManager?.unregisterService(listener)
    } catch (ignored: IllegalArgumentException) {
    }
  }

  private class NsdRegistrationListener(
      private val callback: BroadcastCallback) : NsdManager.RegistrationListener {

    override fun onRegistrationFailed(p0: NsdServiceInfo?, code: Int) {
      callback.broadcastFailed(NsdBroadcastException(code))
    }

    override fun onUnregistrationFailed(p0: NsdServiceInfo?, code: Int) {
      callback.broadcastFailed(NsdBroadcastException(code))
    }

    override fun onServiceUnregistered(service: NsdServiceInfo) {
    }

    override fun onServiceRegistered(p0: NsdServiceInfo) {
    }

  }
}
