package de.mannodermaus.rxbonjour.drivers.nsdmanager

import android.content.Context
import de.mannodermaus.rxbonjour.BroadcastEngine
import de.mannodermaus.rxbonjour.DiscoveryEngine
import de.mannodermaus.rxbonjour.Driver

/**
 * RxBonjour Driver implementation using Android's NsdManager API.
 */
class NsdManagerDriver private constructor(val context: Context) : Driver {
  override val name: String = "nsdmanager"
  override fun createDiscovery(type: String): DiscoveryEngine = NsdManagerDiscoveryEngine(context,
      type)

  override fun createBroadcast(): BroadcastEngine = NsdManagerBroadcastEngine(context)

  companion object {
    @JvmStatic
    fun create(context: Context): Driver = NsdManagerDriver(context)
  }
}
