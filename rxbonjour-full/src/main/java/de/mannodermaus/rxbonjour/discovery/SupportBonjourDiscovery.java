package de.mannodermaus.rxbonjour.discovery;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

import de.mannodermaus.rxbonjour.exc.DiscoveryFailed;
import de.mannodermaus.rxbonjour.exc.StaleContextException;
import de.mannodermaus.rxbonjour.internal.BonjourSchedulers;
import de.mannodermaus.rxbonjour.model.BonjourEvent;
import de.mannodermaus.rxbonjour.model.BonjourService;
import de.mannodermaus.rxbonjour.utils.SupportUtils;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.MainThreadDisposable;

/**
 * Support implementation for Bonjour service discovery on pre-Jelly Bean devices,
 * utilizing Android's WifiManager and the JmDNS library for lookups.
 */
final class SupportBonjourDiscovery extends BonjourDiscovery<SupportUtils> {

    static {
        // Disable logging for some JmDNS classes, since those severely clutter log output
        Logger.getLogger(DNSIncoming.class.getName()).setLevel(Level.OFF);
        Logger.getLogger(DNSRecordType.class.getName()).setLevel(Level.OFF);
        Logger.getLogger(DNSRecordClass.class.getName()).setLevel(Level.OFF);
        Logger.getLogger(DNSIncoming.MessageInputStream.class.getName()).setLevel(Level.OFF);
    }

    /**
     * Suffix appended to input types
     */
    private static final String SUFFIX = ".local.";

    /**
     * Tag to associate with the multicast lock
     */
    private static final String LOCK_TAG = "RxBonjourDiscovery";

    /**
     * Constructor
     */
    public SupportBonjourDiscovery() {
        super();
    }

    @Override protected SupportUtils createUtils() {
        return SupportUtils.get();
    }

	/* Begin private */

    /**
     * Creates a new BonjourEvent instance from a JmDNS ServiceEvent.
     *
     * @param type  Type of event, either ADDED or REMOVED
     * @param event Event containing information about the changed service
     * @return A BonjourEvent containing the necessary information
     */
    private BonjourEvent newBonjourEvent(BonjourEvent.Type type, ServiceEvent event) {
        // Construct a new BonjourService
        ServiceInfo info = event.getInfo();
        BonjourService.Builder serviceBuilder = new BonjourService.Builder(event.getName(), event.getType());

        // Prepare TXT record Bundle
        Enumeration<String> keys = info.getPropertyNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            serviceBuilder.addTxtRecord(key, info.getPropertyString(key));
        }

        // Add non-null host addresses and port
        InetAddress[] addresses = info.getInetAddresses();
        for (InetAddress address : addresses) {
            if (address == null) continue;
            serviceBuilder.addAddress(address);
        }
        serviceBuilder.setPort(info.getPort());

        // Create and return an event wrapping the BonjourService
        return new BonjourEvent(type, serviceBuilder.build());
    }

	/* Begin overrides */

    @Override public Flowable<BonjourEvent> start(Context context, final String type) {
        // Append ".local." suffix in order to have JmDNS pick up on the services
        final String dnsType = (type.endsWith(SUFFIX)) ? type : type + SUFFIX;

        // Create a weak reference to the incoming Context
        final WeakReference<Context> weakContext = new WeakReference<>(context);

        return Flowable.create(new FlowableOnSubscribe<BonjourEvent>() {
            @Override public void subscribe(final FlowableEmitter<BonjourEvent> emitter) throws Exception {
                Context context = weakContext.get();
                if (context == null) {
                    emitter.onError(new StaleContextException());
                    return;
                }

                // Create the service listener
                final ServiceListener listener = new ServiceListener() {
                    @Override public void serviceAdded(ServiceEvent event) {
                        event.getDNS().requestServiceInfo(event.getType(), event.getName());
                    }

                    @Override public void serviceRemoved(ServiceEvent event) {
                        if (!emitter.isCancelled()) {
                            emitter.onNext(newBonjourEvent(BonjourEvent.Type.REMOVED, event));
                        }
                    }

                    @Override public void serviceResolved(ServiceEvent event) {
                        if (!emitter.isCancelled()) {
                            emitter.onNext(newBonjourEvent(BonjourEvent.Type.ADDED, event));
                        }
                    }
                };

                // Obtain a multicast lock from the Wifi Manager and acquire it
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                final WifiManager.MulticastLock lock = wifiManager.createMulticastLock(LOCK_TAG);
                lock.setReferenceCounted(true);
                lock.acquire();

                // Obtain the current IP address and initialize JmDNS' discovery service with that
                try {
                    final JmDNS jmdns = utils.getManager(context);

                    // Add onUnsubscribe() hook
                    emitter.setDisposable(new MainThreadDisposable() {
                        @Override protected void onDispose() {
                            // Release the lock and clean up the JmDNS client
                            jmdns.removeServiceListener(dnsType, listener);
                            utils.decrementSubscriberCount();

                            Completable cleanUp = Completable.fromRunnable(new Runnable() {
                                @Override public void run() {
                                    // Release the held multicast lock
                                    lock.release();

                                    // Close the JmDNS instance if no more subscribers remain
                                    utils.closeIfNecessary();
                                }
                            });

                            cleanUp
                                    .compose(BonjourSchedulers.cleanupSchedulers())
                                    .subscribe();
                        }
                    });

                    // Start discovery
                    jmdns.addServiceListener(dnsType, listener);
                    utils.incrementSubscriberCount();

                } catch (IOException e) {
                    emitter.onError(new DiscoveryFailed(SupportBonjourDiscovery.class, dnsType));
                }
            }
        }, BackpressureStrategy.LATEST);
    }
}
