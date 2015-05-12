package rxbonjour.internal;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rxbonjour.model.BonjourEvent;
import rxbonjour.model.BonjourService;

/**
 * Support implementation for Bonjour service discovery on pre-Jelly Bean devices,
 * utilizing Android's WifiManager and the JmDNS library for lookups.
 *
 * @author marcel
 */
public final class SupportBonjourDiscovery extends BonjourDiscovery {

	/** Suffix appended to input types */
	private static final String SUFFIX = ".local.";

	/** Tag to associate with the multicast lock */
	private static final String LOCK_TAG = "RxBonjourDiscovery";

	/** Multicast lock acquired by the device */
	private WifiManager.MulticastLock lock;
	/** JmDNS used to search for services */
	private JmDNS jmdns;
	/** Listener to notify about services */
	private ServiceListener listener;

	/**
	 * Constructor
	 *
	 * @param context Context of the discovery
	 */
	public SupportBonjourDiscovery(Context context) {
		super(context);
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
		// Access the event's ServiceInfo and obtain a suitable IP address
		ServiceInfo info = event.getInfo();
		InetAddress[] addresses = info.getInetAddresses();
		InetAddress address = null;
		for (InetAddress a : addresses) {
			if (a != null) {
				address = a;
				break;
			}
		}

		// Create the service object and wrap it in an event
		BonjourService service = new BonjourService(event.getName(), event.getType(), address, info.getPort());
		return new BonjourEvent(type, service);
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

	/* Begin overrides */

	@Override public Observable<BonjourEvent> start(final String type) {
		// Append ".local." suffix in order to have JmDNS pick up on the services
		final String dnsType = type + SUFFIX;

		Observable<BonjourEvent> obs = Observable.create(new Observable.OnSubscribe<BonjourEvent>() {
			@Override public void call(final Subscriber<? super BonjourEvent> subscriber) {
				// Create the service listener
				listener = new ServiceListener() {
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

				// Obtain the multicast lock from the Wifi Manager and acquire it
				WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
				lock = wifiManager.createMulticastLock(LOCK_TAG);
				lock.setReferenceCounted(true);
				lock.acquire();

				// Obtain the current IP address and initialize JmDNS' discovery service with that
				try {
					InetAddress inetAddress = getInetAddress(wifiManager);
					jmdns = JmDNS.create(inetAddress, inetAddress.toString());
					jmdns.addServiceListener(dnsType, listener);

				} catch (IOException e) {
					subscriber.onError(e);
				}
			}
		});

		// Add an unsubscribe action releasing the multicast lock, then return the Observable
		return obs
				.doOnUnsubscribe(new Action0() {
					@Override public void call() {
						// Release the lock and clean up the JmDNS client
						jmdns.removeServiceListener(dnsType, listener);
						Observable<Void> cleanUpObservable = Observable.create(new Observable.OnSubscribe<Void>() {
							@Override public void call(final Subscriber<? super Void> subscriber) {
								// Close the JmDNS instance and release the multicast lock
								// (do this asynchronously because JmDNS.close() blocks)
								try {
									jmdns.close();
								} catch (IOException ignored) {
								}
								lock.release();

								// Unsubscribe from the observable automatically
								subscriber.unsubscribe();
							}
						});
						cleanUpObservable
							.subscribeOn(Schedulers.io())
							.observeOn(Schedulers.io())
							.subscribe();
					}
				});
	}
}
