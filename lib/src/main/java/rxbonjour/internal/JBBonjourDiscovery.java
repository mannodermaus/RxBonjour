package rxbonjour.internal;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import rx.Subscriber;
import rxbonjour.exc.DiscoveryFailed;

import rx.Observable;
import rx.functions.Action0;
import rxbonjour.exc.StaleContextException;
import rxbonjour.model.BonjourEvent;
import rxbonjour.model.BonjourService;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Bonjour implementation for Jelly-Bean and up, utilizing the NsdManager APIs.
 *
 * @author marcel
 */
@TargetApi(JELLY_BEAN)
public final class JBBonjourDiscovery extends BonjourDiscovery {

	/** Discovery listener fed into the NsdManager */
	private NsdManager.DiscoveryListener discoveryListener;

	/** NsdManager itself */
	private NsdManager nsdManager;

	/** Resolver backlog, necessary because of NsdManager's "one resolve at a time" limitation */
	private Backlog<NsdServiceInfo> resolveBacklog;

	/**
	 * Constructor
	 *
	 */
	public JBBonjourDiscovery() {
		super();
	}

	/* Begin private */

	/**
	 * Creates a new BonjourEvent instance from an Nsd Service info object.
	 *
	 * @param type        Type of event, either ADDED or REMOVED
	 * @param serviceInfo ServiceInfo containing information about the changed service
	 * @return A BonjourEvent containing the necessary information
	 */
	@TargetApi(LOLLIPOP) private BonjourEvent newBonjourEvent(BonjourEvent.Type type, NsdServiceInfo serviceInfo) {
		// Prepare TXT record Bundle (on Lollipop and up)
		Bundle txtRecords;
		if (Build.VERSION.SDK_INT >= LOLLIPOP) {
			Map<String, byte[]> attributes = serviceInfo.getAttributes();
			txtRecords = new Bundle(attributes.size());
			for (String key : attributes.keySet()) {
				txtRecords.putString(key, new String(attributes.get(key), Charset.forName("UTF-8")));
			}

		} else {
			txtRecords = new Bundle(0);
		}

		Map<BonjourService.IPv, InetAddress> addressMap = new HashMap<>();
		if (serviceInfo.getHost() != null) {
			if (serviceInfo.getHost() instanceof Inet4Address) {
				addressMap.put(BonjourService.IPv.V4, serviceInfo.getHost());
			} else if (serviceInfo.getHost() instanceof Inet6Address) {
				addressMap.put(BonjourService.IPv.V6, serviceInfo.getHost());
			}
		}

		// Create and return an event wrapping the BonjourService
		BonjourService service = new BonjourService(serviceInfo.getServiceName(), serviceInfo.getServiceType(), addressMap, serviceInfo.getPort(), txtRecords);
		return new BonjourEvent(type, service);
	}

	/* Begin overrides */

	@Override public Observable<BonjourEvent> start(Context context, final String type) {
		// Create a weak reference to the incoming Context
		final WeakReference<Context> weakContext = new WeakReference<>(context);

		Observable<BonjourEvent> obs = Observable.create(new Observable.OnSubscribe<BonjourEvent>() {
			@Override public void call(final Subscriber<? super BonjourEvent> subscriber) {
				Context context = weakContext.get();
				if (context == null) {
					subscriber.onError(new StaleContextException());
					return;
				}

				// Create the discovery listener
				discoveryListener = new NsdManager.DiscoveryListener() {
					@Override public void onStartDiscoveryFailed(String serviceType, int errorCode) {
						subscriber.onError(new DiscoveryFailed(JBBonjourDiscovery.class, serviceType, errorCode));
					}

					@Override public void onStopDiscoveryFailed(String serviceType, int errorCode) {
						subscriber.onError(new DiscoveryFailed(JBBonjourDiscovery.class, serviceType, errorCode));
					}

					@Override public void onDiscoveryStarted(String serviceType) {
					}

					@Override public void onDiscoveryStopped(String serviceType) {
					}

					@Override public void onServiceFound(NsdServiceInfo serviceInfo) {
						// Add the found service to the resolve backlog (it will be processed once the backlog gets to it)
						resolveBacklog.add(serviceInfo);
					}

					@Override public void onServiceLost(NsdServiceInfo serviceInfo) {
						if (!subscriber.isUnsubscribed()) {
							subscriber.onNext(newBonjourEvent(BonjourEvent.Type.REMOVED, serviceInfo));
						}
					}
				};

				// Create the resolver backlog
				resolveBacklog = new Backlog<NsdServiceInfo>() {
					@Override public void onNext(NsdServiceInfo info) {
						// Resolve this service info using the corresponding listener
						nsdManager.resolveService(info, new NsdManager.ResolveListener() {
							@Override public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
							}

							@Override public void onServiceResolved(NsdServiceInfo serviceInfo) {
								if (!subscriber.isUnsubscribed()) {
									subscriber.onNext(newBonjourEvent(BonjourEvent.Type.ADDED, serviceInfo));
								}

								// Inform the backlog to continue processing
								resolveBacklog.proceed();
							}
						});
					}
				};

				// Obtain the NSD manager and start discovering once received
				nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
				nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
			}
		});

		// Add schedulers and an unsubscribe action to stop service discovery and return the observable
		return obs
				.doOnUnsubscribe(new Action0() {
					@Override public void call() {
						try {
							resolveBacklog.quit();
							nsdManager.stopServiceDiscovery(discoveryListener);
						} catch (Exception ignored) {
							// "Service discovery not active on discoveryListener", thrown if starting the service discovery was unsuccessful earlier
						}
					}
				});
	}
}
