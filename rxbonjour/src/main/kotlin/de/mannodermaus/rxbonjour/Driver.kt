package de.mannodermaus.rxbonjour

import java.net.InetAddress

interface Driver {
    val name: String
    fun createDiscovery(type: String): DiscoveryEngine
    fun createBroadcast(): BroadcastEngine
}

interface Engine {
    fun initialize()
    fun teardown()
}

interface DiscoveryEngine : Engine {
    fun discover(address: InetAddress, callback: DiscoveryCallback)
}

interface DiscoveryCallback {
    fun discoveryFailed(cause: Exception?)
    fun serviceResolved(service: BonjourService)
    fun serviceLost(service: BonjourService)
}

interface BroadcastEngine : Engine {
    fun start(address: InetAddress, config: BonjourBroadcastConfig, callback: BroadcastCallback)
}

interface BroadcastCallback {
    fun broadcastFailed(cause: Exception?)
}
