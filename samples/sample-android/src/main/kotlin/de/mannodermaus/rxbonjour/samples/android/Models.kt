package de.mannodermaus.rxbonjour.samples.android

import android.content.Context
import de.mannodermaus.rxbonjour.Driver
import de.mannodermaus.rxbonjour.drivers.jmdns.JmDNSDriver
import de.mannodermaus.rxbonjour.drivers.nsdmanager.NsdManagerDriver

enum class DriverImpl(
        private val library: String,
        private val artifact: String,
        val factory: (Context) -> Driver) {

    JMDNS(
            "JmDNS",
            "rxbonjour-driver-jmdns",
            { JmDNSDriver.create() }),

    NSDMANAGER(
            "NsdManager",
            "rxbonjour-driver-nsdmanager",
            { NsdManagerDriver.create(it) });

    override fun toString(): String = "$library ($artifact)"
}
