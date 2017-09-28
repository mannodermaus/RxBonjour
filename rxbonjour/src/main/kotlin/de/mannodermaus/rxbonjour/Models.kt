package de.mannodermaus.rxbonjour

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

typealias TxtRecords = Map<String, String>

private val DEFAULT_NAME = "RxBonjour Service"
private val DEFAULT_PORT = 80

data class BonjourBroadcastConfig @JvmOverloads constructor(
        val type: String,
        val name: String = DEFAULT_NAME,
        val address: InetAddress? = null,
        val port: Int = DEFAULT_PORT,
        val txtRecords: TxtRecords? = emptyMap())

data class BonjourService(
        val type: String,
        val name: String,
        val v4Host: Inet4Address?,
        val v6Host: Inet6Address?,
        val port: Int,
        val txtRecords: TxtRecords = emptyMap()) {

    val host: InetAddress = v4Host ?: v6Host!!
}

sealed class BonjourEvent(val service: BonjourService) {
    class Added(service: BonjourService) : BonjourEvent(service)
    class Removed(service: BonjourService) : BonjourEvent(service)
}
