package de.mannodermaus.rxbonjour

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

typealias TxtRecords = Map<String, String>

private val DEFAULT_NAME = "RxBonjour Service"
private val DEFAULT_PORT = 80

data class BonjourBroadcast(
        val type: String,
        val address: InetAddress? = null,
        val port: Int = DEFAULT_PORT,
        val name: String = DEFAULT_NAME,
        val txtRecords: TxtRecords = emptyMap())

data class BonjourService(
        val name: String,
        val type: String,
        val v4Host: Inet4Address?,
        val v6Host: Inet6Address?,
        val port: Int,
        private val txtRecords: TxtRecords = emptyMap()) {

    val host: InetAddress = v4Host ?: v6Host!!
    val txtRecordCount: Int = txtRecords.size
    val txtRecordKeys: Set<String> = txtRecords.keys

    fun getTxtRecord(key: String): String? = txtRecords[key]
}

sealed class BonjourEvent(val service: BonjourService) {
    class Added(service: BonjourService) : BonjourEvent(service)
    class Removed(service: BonjourService) : BonjourEvent(service)
}
