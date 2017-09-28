package de.mannodermaus.rxbonjour.drivers.jmdns

import de.mannodermaus.rxbonjour.BroadcastEngine
import de.mannodermaus.rxbonjour.DiscoveryEngine
import de.mannodermaus.rxbonjour.Driver

/**
 * RxBonjour Driver implementation using JmDNS for Network Service Discovery.
 */
class JmDNSDriver private constructor() : Driver {
    override val name: String = "jmdns"
    override fun createDiscovery(type: String): DiscoveryEngine = JmDNSDiscoveryEngine(type)
    override fun createBroadcast(): BroadcastEngine = JmDNSBroadcastEngine()

    companion object {
        @JvmStatic
        fun create(): Driver = JmDNSDriver()
    }
}
