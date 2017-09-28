package de.mannodermaus.rxbonjour.samples.java;

import de.mannodermaus.rxbonjour.RxBonjour;
import de.mannodermaus.rxbonjour.drivers.jmdns.JmDNSDriver;
import de.mannodermaus.rxbonjour.platforms.desktop.DesktopPlatform;

public class Main {

    public static void main(String[] args) {
        RxBonjour rxBonjour = new RxBonjour.Builder()
                .platform(DesktopPlatform.create())
                .driver(JmDNSDriver.create())
                .create();

        rxBonjour.newDiscovery("_ssh._tcp")
                .blockingSubscribe(
                        event -> System.out.println("Event: " + event),
                        error -> System.err.println("Error: " + error.getMessage()));
    }
}
