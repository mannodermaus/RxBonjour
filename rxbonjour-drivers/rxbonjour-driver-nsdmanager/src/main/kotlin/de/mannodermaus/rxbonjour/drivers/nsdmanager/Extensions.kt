package de.mannodermaus.rxbonjour.drivers.nsdmanager

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import de.mannodermaus.rxbonjour.BonjourBroadcastConfig
import de.mannodermaus.rxbonjour.BonjourService
import de.mannodermaus.rxbonjour.TxtRecords
import java.net.Inet4Address
import java.net.Inet6Address
import java.nio.charset.Charset

internal fun Context.getNsdManager() = this.getSystemService(Context.NSD_SERVICE) as NsdManager

internal fun NsdServiceInfo.getTxtRecords(): TxtRecords {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    // NSD Attributes only available on API 21+
    this.attributes
        .map { (key, bytes) -> Pair(key, String(bytes, Charset.forName("UTF-8"))) }
        .associate { it }

  } else {
    emptyMap()
  }
}

internal fun NsdServiceInfo.setTxtRecords(records: TxtRecords?) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    records?.forEach { (key, value) ->
      setAttribute(key, value)
    }
  }
}

internal fun NsdServiceInfo.toLibraryModel() = BonjourService(
    name = this.serviceName,
    type = this.serviceType,
    v4Host = if (this.host is Inet4Address) this.host as Inet4Address else null,
    v6Host = if (this.host is Inet6Address) this.host as Inet6Address else null,
    port = this.port,
    txtRecords = this.getTxtRecords())

internal fun BonjourBroadcastConfig.toNsdModel() = NsdServiceInfo().apply {
  val model = this@toNsdModel

  serviceType = model.type
  serviceName = model.name
  host = model.address
  port = model.port
  setTxtRecords(model.txtRecords)
}
