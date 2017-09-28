package de.mannodermaus.rxbonjour.drivers.jmdns

import de.mannodermaus.rxbonjour.BonjourBroadcastConfig
import de.mannodermaus.rxbonjour.BonjourSchedulers
import de.mannodermaus.rxbonjour.BroadcastCallback
import de.mannodermaus.rxbonjour.BroadcastEngine
import io.reactivex.Completable
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

private val LOCAL_DOMAIN_SUFFIX = ".local."

internal class JmDNSBroadcastEngine : BroadcastEngine {

    private var jmdns: JmDNS? = null
    private var jmdnsService: ServiceInfo? = null

    override fun initialize() {
    }

    override fun start(address: InetAddress, config: BonjourBroadcastConfig, callback: BroadcastCallback) {
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
                    .compose(BonjourSchedulers.completableAsync())
                    .onErrorComplete()
                    .subscribe()
        }
    }
}

/* Extension Functions */

private fun String.ensureLocalDomain(): String =
        if (this.endsWith(LOCAL_DOMAIN_SUFFIX))
            this
        else
            "$this$LOCAL_DOMAIN_SUFFIX"

private fun BonjourBroadcastConfig.toJmDNSModel() = ServiceInfo.create(
        /* type */ this.type.ensureLocalDomain(),
        /* name */ this.name,
        /* port */ this.port,
        /* weight */ 0,
        /* priority */ 0,
        /* persistent */ true,
        /* props */ this.txtRecords)
