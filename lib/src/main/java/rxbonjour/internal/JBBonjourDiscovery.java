package rxbonjour.internal;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import rx.Subscriber;
import rxbonjour.exc.DiscoveryFailed;

import rx.Observable;
import rx.functions.Action0;
import rxbonjour.model.BonjourEvent;
import rxbonjour.model.BonjourService;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;

/**
 * Bonjour implementation for Jelly-Bean and up, utilizing the NsdManager APIs.
 *
 * @author marcel
 */
@TargetApi(JELLY_BEAN)
public final class JBBonjourDiscovery extends BonjourDiscovery {

	/** Discovery listener fed into the NsdManager */
	private NsdManager.DiscoveryListener discoveryListener;

	/** Resolve listener fed into the NsdManager */
	private NsdManager.ResolveListener resolveListener;

	/** NsdManager itself */
	private NsdManager nsdManager;

	/** Resolver backlog, necessary because of NsdManager's "one resolve at a time" limitation */
	private Backlog<NsdServiceInfo> resolveBacklog;

	/**
	 * Constructor
	 *
	 * @param context Context of the discovery
	 */
	public JBBonjourDiscovery(Context context) {
		super(context);
	}

	/* Begin private */

	/**
	 * Creates a new BonjourEvent instance from an Nsd Service info object.
	 *
	 * @param type        Type of event, either ADDED or REMOVED
	 * @param serviceInfo ServiceInfo containing information about the changed service
	 * @return A BonjourEvent containing the necessary information
	 */
	private BonjourEvent newBonjourEvent(BonjourEvent.Type type, NsdServiceInfo serviceInfo) {
		BonjourService service = new BonjourService(serviceInfo.getServiceName(), serviceInfo.getServiceType(), serviceInfo.getHost(), serviceInfo.getPort());
		return new BonjourEvent(type, service);
	}

	/* Begin overrides */

	@Override public Observable<BonjourEvent> start(final String type) {
		Observable<BonjourEvent> obs = Observable.create(new Observable.OnSubscribe<BonjourEvent>() {
			@Override public void call(final Subscriber<? super BonjourEvent> subscriber) {
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
						nsdManager.resolveService(info, resolveListener);
					}
				};

				// Create the resolve listener
				resolveListener = new NsdManager.ResolveListener() {
					@Override public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
					}

					@Override public void onServiceResolved(NsdServiceInfo serviceInfo) {
						if (!subscriber.isUnsubscribed()) {
							subscriber.onNext(newBonjourEvent(BonjourEvent.Type.ADDED, serviceInfo));
						}

						// Inform the backlog to continue processing
						resolveBacklog.proceed();
					}
				};

				// Obtain the NSD manager and start discovering once received
				nsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
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
							// "Service discovery not active on discoveryListener", thrown if starting the service discovery was unsuccessful
						}
					}
				});
	}
}
