package de.mannodermaus.rxbonjour.drivers.jmdns

import de.mannodermaus.rxbonjour.BonjourBroadcast
import de.mannodermaus.rxbonjour.BonjourSchedulers
import de.mannodermaus.rxbonjour.BroadcastEngine
import io.reactivex.Completable
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

internal class JmDNSBroadcastEngine : BroadcastEngine {

    private var jmdns: JmDNS? = null
    private var jmdnsService: ServiceInfo? = null

    override fun initialize() {
    }

    override fun start(address: InetAddress, config: BonjourBroadcast) {
        val jmdns = JmDNS.create(address, address.toString())
        this.jmdns = jmdns
        this.jmdnsService = config.toJmDNSModel()

        // This will start the broadcast immediately
        jmdns.registerService(jmdnsService)
    }

    override fun teardown() {
        jmdns?.let { jmdns ->
            jmdnsService?.let { jmdns.unregisterService(it) }

            // Closing JmDNS might take a while, so defer it to a background thread
            Completable.fromAction { jmdns.close() }
                    .compose(BonjourSchedulers.cleanupSchedulers())
                    .onErrorComplete()
                    .subscribe()
        }
    }
}

/* Extension Functions */

private fun BonjourBroadcast.toJmDNSModel() = ServiceInfo.create(
        /* type */ this.type,
        /* name */ this.name,
        /* port */ this.port,
        /* weight */ 0,
        /* priority */ 0,
        /* persistent */ true,
        /* props */ this.txtRecords)
