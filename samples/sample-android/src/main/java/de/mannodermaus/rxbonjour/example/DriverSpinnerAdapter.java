package de.mannodermaus.rxbonjour.example;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import de.mannodermaus.rxbonjour.Driver;
import de.mannodermaus.rxbonjour.drivers.jmdns.JmDNSDriver;
import de.mannodermaus.rxbonjour.drivers.nsdmanager.NsdManagerDriver;
import kotlin.jvm.functions.Function1;

public class DriverSpinnerAdapter extends ArrayAdapter<DriverSpinnerAdapter.DriverLib> {

    public enum DriverLib {
        JMDNS(
                "JmDNS",
                "rxbonjour-driver-jmdns",
                context -> JmDNSDriver.create()),
        NSD_MANAGER(
                "NsdManager",
                "rxbonjour-driver-nsdmanager",
                NsdManagerDriver::create);

        public final String name;
        public final String artifact;
        public final Function1<Context, Driver> factory;

        DriverLib(String name, String artifact, Function1<Context, Driver> factory) {
            this.name = name;
            this.artifact = artifact;
            this.factory = factory;
        }

        @Override public String toString() {
            return name + " (" + artifact + ")";
        }
    }

    public DriverSpinnerAdapter(@NonNull Context context) {
        super(context, R.layout.support_simple_spinner_dropdown_item, DriverLib.values());
    }
}
