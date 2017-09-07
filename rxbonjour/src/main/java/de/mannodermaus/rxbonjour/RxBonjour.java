package de.mannodermaus.rxbonjour;

import android.content.Context;

import de.mannodermaus.rxbonjour.broadcast.BonjourBroadcast;
import de.mannodermaus.rxbonjour.broadcast.BonjourBroadcastBuilder;
import de.mannodermaus.rxbonjour.discovery.BonjourDiscovery;
import de.mannodermaus.rxbonjour.exc.TypeMalformedException;
import de.mannodermaus.rxbonjour.model.BonjourEvent;
import io.reactivex.Flowable;

/**
 * RxBonjour:
 * Enables clients to perform network service discovery for Bonjour devices using Reactive Extensions.
 */
public final class RxBonjour {

    private static final String TYPE_PATTERN = "_[a-zA-Z0-9\\-_]+\\.(_tcp|_udp)(\\.[a-zA-Z0-9\\-]+\\.)?";

    private RxBonjour() {
        throw new AssertionError("no instances");
    }

    /**
     * @deprecated Use {@link #newDiscovery(Context, String)} instead.
     */
    public static Flowable<BonjourEvent> startDiscovery(Context context, String type) {
        return newDiscovery(context, type);
    }

    /**
     * @deprecated Use {@link #newDiscovery(Context, String, boolean)} instead.
     */
    public static Flowable<BonjourEvent> startDiscovery(Context context, String type, boolean useNsdManager) {
        return newDiscovery(context, type, useNsdManager);
    }

    /**
     * Starts a Bonjour service discovery for the provided service type.
     * This method utilizes the support implementation with JmDNS as its backbone,
     * seeing as the official NsdManager APIs are subject to multiple deal-breaking bugs. If you really want to use NsdManager on devices that
     * support it (API level 16 or greater), use {@link #newDiscovery(Context, String, boolean)} and pass in <b>true</b> as the final argument.
     * This method's return Observable is scheduled to run on an I/O thread and notify subscribers on the main thread.
     * This method will throw a Runtime Exception if the input type does not obey Bonjour type specifications. If you intend
     * to use this method with arbitrary types that can be provided by user input, it is highly encouraged to verify this input
     * using {@link #isBonjourType(String)} <b>before</b> calling this method!
     *
     * @param context Context of the request
     * @param type    Type of service to discover
     * @return An Observable for Bonjour events
     * @see <a href="https://code.google.com/p/android/issues/detail?id=70778">"NsdManager blocks calling thread forever" - Issue on Google Code</a>
     * @see <a href="https://code.google.com/p/android/issues/detail?id=35585">"Problems with Network Services Discovery APIs" - Issue on Google Code</a>
     * @see <a href="https://code.google.com/p/android/issues/detail?id=39750">"NSD causes Nexus 7 device to spontaneously restart." - Issue on Google Code</a>
     */
    public static Flowable<BonjourEvent> newDiscovery(Context context, String type) {
        return newDiscovery(context, type, false);
    }

    /**
     * Starts a Bonjour service discovery for the provided service type.
     * <p>
     * This method chooses the correct NSD implementation based on the device's API level. Please note that the implementation used on Jelly Bean
     * and up is subject to multiple deal-breaking bugs, so whenever possible, the support implementation using JmDNS should be used until Google
     * resolves these known issues with NsdManager.
     * <p>
     * This method's return Observable is scheduled to run on an I/O thread and notify subscribers on the main thread.
     * <p>
     * This method will throw a Runtime Exception if the input type does not obey Bonjour type specifications. If you intend
     * to use this method with arbitrary types that can be provided by user input, it is highly encouraged to verify this input
     * using {@link #isBonjourType(String)} <b>before</b> calling this method!
     *
     * @param context Context of the request
     * @param type    Type of service to discover
     * @return An Observable for Bonjour events
     * @see <a href="https://code.google.com/p/android/issues/detail?id=70778">"NsdManager blocks calling thread forever" - Issue on Google Code</a>
     * @see <a href="https://code.google.com/p/android/issues/detail?id=35585">"Problems with Network Services Discovery APIs" - Issue on Google Code</a>
     * @see <a href="https://code.google.com/p/android/issues/detail?id=39750">"NSD causes Nexus 7 device to spontaneously restart." - Issue on Google Code</a>
     */
    public static Flowable<BonjourEvent> newDiscovery(Context context, String type, boolean forceNsdManager) {
        // Verify input
        if (!isBonjourType(type)) throw new TypeMalformedException(type);

        // Choose discovery strategy
        BonjourDiscovery<?> discovery = BonjourDiscovery.get(forceNsdManager);

        // Create the discovery Observable and pre-configure it
        return discovery.start(context, type);
    }

    public static BonjourBroadcastBuilder newBroadcast(String type) {
        return newBroadcast(type, false);
    }

    public static BonjourBroadcastBuilder newBroadcast(String type, boolean forceNsdManager) {
        // Verify input
        if (!isBonjourType(type)) throw new TypeMalformedException(type);

        // Choose broadcast strategy and return its Builder object
        return BonjourBroadcast.newBuilder(type, forceNsdManager);
    }

    /**
     * Checks the provided type String against Bonjour specifications, and returns whether or not the type is valid.
     *
     * @param type Type of service to check
     * @return True if the type refers to a valid Bonjour type, false otherwise
     */
    public static boolean isBonjourType(String type) {
        return type.matches(TYPE_PATTERN);
    }
}
