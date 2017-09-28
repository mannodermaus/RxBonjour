package de.mannodermaus.rxbonjour.samples.desktop

import de.mannodermaus.rxbonjour.RxBonjour
import de.mannodermaus.rxbonjour.drivers.jmdns.JmDNSDriver
import de.mannodermaus.rxbonjour.platforms.desktop.DesktopPlatform

fun main(args: Array<String>) {
    val rxBonjour = RxBonjour.Builder()
            .platform(DesktopPlatform.create())
            .driver(JmDNSDriver.create())
            .create()

    rxBonjour.newDiscovery("_ssh._tcp")
            .subscribe(
                    { event -> println("Event: " + event) },
                    { error -> System.err.println("Error: " + error.message) })
}
