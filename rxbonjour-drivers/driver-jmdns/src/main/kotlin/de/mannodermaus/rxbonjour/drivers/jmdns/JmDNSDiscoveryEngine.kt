package de.mannodermaus.rxbonjour.drivers.jmdns

import de.mannodermaus.rxbonjour.BonjourSchedulers
import de.mannodermaus.rxbonjour.BonjourService
import de.mannodermaus.rxbonjour.DiscoveryCallback
import de.mannodermaus.rxbonjour.DiscoveryEngine
import io.reactivex.Completable
import java.net.InetAddress
import java.util.logging.Level
import java.util.logging.Logger
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import javax.jmdns.impl.DNSIncoming
import javax.jmdns.impl.constants.DNSRecordClass
import javax.jmdns.impl.constants.DNSRecordType

private val BONJOUR_TYPE_LOCAL_SUFFIX = ".local."

class JmDNSDiscoveryEngine
internal constructor(type: String) : DiscoveryEngine {

    // Append type suffix in order to have JmDNS pick up on the resolved services
    private val serviceType = if (type.endsWith(BONJOUR_TYPE_LOCAL_SUFFIX)) type else type + BONJOUR_TYPE_LOCAL_SUFFIX

    private var jmdns: JmDNS? = null
    private var listener: JmDNSListener? = null

    override fun initialize() {
        // Disable logging for some JmDNS classes, since those severely clutter log output
        Logger.getLogger(DNSIncoming::class.java.name).level = Level.OFF
        Logger.getLogger(DNSRecordType::class.java.name).level = Level.OFF
        Logger.getLogger(DNSRecordClass::class.java.name).level = Level.OFF
        Logger.getLogger(DNSIncoming.MessageInputStream::class.java.name).level = Level.OFF
    }

    override fun discover(address: InetAddress, callback: DiscoveryCallback) {

        // Setup JmDNS instance & configure it
        jmdns = JmDNS.create(address, address.toString())
        listener = JmDNSListener(callback)

        // This will start the discovery
        jmdns?.addServiceListener(serviceType, listener)
    }

    override fun teardown() {
        // Remove service listener & shut down JmDNS
        jmdns?.let { jmdns ->
            listener?.let { jmdns.removeServiceListener(serviceType, it) }

            // Closing JmDNS might take a while, so defer it to a background thread
            Completable.fromAction { jmdns.close() }
                    .compose(BonjourSchedulers.cleanupSchedulers())
                    .onErrorComplete()
                    .subscribe()
        }
    }

    private class JmDNSListener(val callback: DiscoveryCallback) : ServiceListener {

        override fun serviceAdded(event: ServiceEvent) {
            // Resolve the service's info, don't call through with success yet
            event.dns.requestServiceInfo(event.type, event.name)
        }

        override fun serviceRemoved(event: ServiceEvent) {
            callback.serviceLost(event.info.toLibraryModel())
        }

        override fun serviceResolved(event: ServiceEvent) {
            callback.serviceResolved(event.info.toLibraryModel())
        }
    }
}

/* Extension Functions */

// Mapping between JmDNS namespace & RxBonjour model type
private fun ServiceInfo.toLibraryModel() = BonjourService(
        name = this.name,
        type = this.type,
        v4Host = this.inet4Addresses.firstOrNull(),
        v6Host = this.inet6Addresses.firstOrNull(),
        port = this.port,
        txtRecords = this.propertyNames
                .toList()
                .associate { Pair(it, this.getPropertyString(it)) })
