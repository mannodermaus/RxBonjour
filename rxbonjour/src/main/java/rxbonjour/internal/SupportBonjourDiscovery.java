package rxbonjour.internal;

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

import rx.Observable;
import rx.Subscriber;
import rxbonjour.exc.DiscoveryFailed;
import rxbonjour.exc.StaleContextException;
import rxbonjour.model.BonjourEvent;
import rxbonjour.model.BonjourService;

/**
 * Support implementation for Bonjour service discovery on pre-Jelly Bean devices,
 * utilizing Android's WifiManager and the JmDNS library for lookups.
 */
public final class SupportBonjourDiscovery implements BonjourDiscovery {

	static {
		// Disable logging for some JmDNS classes, since those severely clutter log output
		Logger.getLogger(DNSIncoming.class.getName()).setLevel(Level.OFF);
		Logger.getLogger(DNSRecordType.class.getName()).setLevel(Level.OFF);
		Logger.getLogger(DNSRecordClass.class.getName()).setLevel(Level.OFF);
		Logger.getLogger(DNSIncoming.MessageInputStream.class.getName()).setLevel(Level.OFF);
	}

	/** Suffix appended to input types */
	private static final String SUFFIX = ".local.";

	/** Tag to associate with the multicast lock */
	private static final String LOCK_TAG = "RxBonjourDiscovery";

	/** The JmDNS instance used for discovery, shared among subscribers */
	private JmDNS jmdnsInstance;
	/** Synchronization lock on the JmDNS instance */
	private final Object jmdnsLock = new Object();
	/** Number of subscribers listening to Bonjour events */
	private int subscriberCount = 0;

	/**
	 * Constructor
	 */
	public SupportBonjourDiscovery() {
		super();
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

	/**
	 * Returns the current connection's IP address.
	 * This implementation is taken from http://stackoverflow.com/a/13677686/1143172
	 * and takes note of a JmDNS issue with resolved IP addresses.
	 *
	 * @param wifiManager WifiManager to look up the IP address from
	 * @return The InetAddress of the current connection
	 * @throws IOException In case the InetAddress can't be resolved
	 */
	private InetAddress getInetAddress(WifiManager wifiManager) throws IOException {
		int intaddr = wifiManager.getConnectionInfo().getIpAddress();

		byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
				(byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
		return InetAddress.getByAddress(byteaddr);
	}

	/**
	 * Returns the JmDNS shared among all subscribers for Bonjour events, creating it if necessary.
	 *
	 * @param wifiManager WifiManager used to access the device's IP address with which JmDNS is initialized
	 * @return The JmDNS instance
	 * @throws IOException In case the device's address can't be resolved
	 */
	private JmDNS getJmdns(WifiManager wifiManager) throws IOException {
		synchronized (jmdnsLock) {
			if (jmdnsInstance == null) {
				InetAddress inetAddress = getInetAddress(wifiManager);
				jmdnsInstance = JmDNS.create(inetAddress, inetAddress.toString());
			}
			return jmdnsInstance;
		}
	}

	/* Begin overrides */

	@Override public Observable<BonjourEvent> start(Context context, final String type) {
		// Append ".local." suffix in order to have JmDNS pick up on the services
		final String dnsType = (type.endsWith(SUFFIX)) ? type : type + SUFFIX;

		// Create a weak reference to the incoming Context
		final WeakReference<Context> weakContext = new WeakReference<>(context);

		return Observable.create(new Observable.OnSubscribe<BonjourEvent>() {
			@Override public void call(final Subscriber<? super BonjourEvent> subscriber) {
				Context context = weakContext.get();
				if (context == null) {
					subscriber.onError(new StaleContextException());
					return;
				}

				// Create the service listener
				final ServiceListener listener = new ServiceListener() {
					@Override public void serviceAdded(ServiceEvent event) {
						event.getDNS().requestServiceInfo(event.getType(), event.getName());
					}

					@Override public void serviceRemoved(ServiceEvent event) {
						if (!subscriber.isUnsubscribed()) {
							subscriber.onNext(newBonjourEvent(BonjourEvent.Type.REMOVED, event));
						}
					}

					@Override public void serviceResolved(ServiceEvent event) {
						if (!subscriber.isUnsubscribed()) {
							subscriber.onNext(newBonjourEvent(BonjourEvent.Type.ADDED, event));
						}
					}
				};

				// Obtain a multicast lock from the Wifi Manager and acquire it
				WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				final WifiManager.MulticastLock lock = wifiManager.createMulticastLock(LOCK_TAG);
				lock.setReferenceCounted(true);
				lock.acquire();

				// Obtain the current IP address and initialize JmDNS' discovery service with that
				try {
					final JmDNS jmdns = getJmdns(wifiManager);

					// Add onUnsubscribe() hook
					subscriber.add(new MainThreadSubscription() {
						@Override protected void onUnsubscribe() {
							// Release the lock and clean up the JmDNS client
							jmdns.removeServiceListener(dnsType, listener);
							subscriberCount--;

							Observable<Void> cleanUpObservable = Observable.create(new Observable.OnSubscribe<Void>() {
								@Override public void call(final Subscriber<? super Void> subscriber) {
									// Release the held multicast lock
									lock.release();

									// Close the JmDNS instance if no more subscribers remain
									if (subscriberCount <= 0) {
										// This call blocks, which is why it is running on a computation thread
										try {
											jmdns.close();
										} catch (IOException ignored) {
										} finally {
											synchronized (jmdnsLock) {
												jmdnsInstance = null;
												subscriberCount = 0;
											}
										}
									}

									// Unsubscribe from the observable automatically
									subscriber.unsubscribe();
								}
							});
							cleanUpObservable
									.compose(BonjourSchedulers.cleanupSchedulers())
									.subscribe();
						}
					});

					// Start discovery
					jmdns.addServiceListener(dnsType, listener);
					subscriberCount++;

				} catch (IOException e) {
					subscriber.onError(new DiscoveryFailed(SupportBonjourDiscovery.class, dnsType));
				}
			}
		});
	}
}
