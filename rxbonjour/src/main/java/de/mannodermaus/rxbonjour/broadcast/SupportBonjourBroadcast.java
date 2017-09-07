package de.mannodermaus.rxbonjour.broadcast;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import de.mannodermaus.rxbonjour.exc.BroadcastFailed;
import de.mannodermaus.rxbonjour.exc.StaleContextException;
import de.mannodermaus.rxbonjour.internal.BonjourSchedulers;
import de.mannodermaus.rxbonjour.model.BonjourEvent;
import de.mannodermaus.rxbonjour.model.BonjourService;
import de.mannodermaus.rxbonjour.utils.SupportUtils;
import rx.Emitter;
import rx.Observable;
import rx.Subscriber;
import rx.android.MainThreadSubscription;
import rx.functions.Action1;

final class SupportBonjourBroadcast extends BonjourBroadcast<SupportUtils> {

    /**
     * Tag to associate with the multicast lock
     */
    private static final String LOCK_TAG = "RxBonjourBroadcast";

    protected SupportBonjourBroadcast(BonjourBroadcastBuilder builder) {
        super(builder);
    }

    @Override protected SupportUtils createUtils() {
        return SupportUtils.get();
    }

    @Override public Observable<BonjourEvent> start(Context context) {
        // Create a weak reference to the incoming Context
        final WeakReference<Context> weakContext = new WeakReference<>(context);

        return Observable.create(new Action1<Emitter<BonjourEvent>>() {
            @Override public void call(final Emitter<BonjourEvent> emitter) {
                Context context = weakContext.get();
                if (context == null) {
                    emitter.onError(new StaleContextException());
                    return;
                }

                // Obtain a multicast lock from the Wifi Manager and acquire it
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                final WifiManager.MulticastLock lock = wifiManager.createMulticastLock(LOCK_TAG);
                lock.setReferenceCounted(true);
                lock.acquire();

                try {
                    // Create a JmDNS service using the BonjourService information and register that
                    final BonjourService bonjourService = createBonjourService(context);
                    final ServiceInfo jmdnsService = createJmdnsService(bonjourService);
                    final JmDNS jmdns = utils.getManager(context);

                    emitter.setSubscription(new MainThreadSubscription() {
                        @Override
                        protected void onUnsubscribe() {
                            jmdns.unregisterService(jmdnsService);
                            utils.decrementSubscriberCount();
                            lock.release();

                            Observable<Void> cleanUpObservable = Observable.create(new Observable.OnSubscribe<Void>() {
                                @Override
                                public void call(final Subscriber<? super Void> subscriber) {
                                    utils.closeIfNecessary();
                                    subscriber.unsubscribe();
                                }
                            });

                            cleanUpObservable
                                    .compose(BonjourSchedulers.cleanupSchedulers())
                                    .subscribe();
                        }
                    });

                    jmdns.registerService(jmdnsService);
                    utils.incrementSubscriberCount();
                    emitter.onNext(new BonjourEvent(BonjourEvent.Type.ADDED, bonjourService));
                } catch (IOException e) {
                    emitter.onError(new BroadcastFailed(SupportBonjourBroadcast.class, type));
                }
            }
        }, Emitter.BackpressureMode.LATEST);
    }

    private ServiceInfo createJmdnsService(BonjourService serviceInfo) {
        int txtRecordCount = serviceInfo.getTxtRecordCount();
        Bundle txtRecordBundle = serviceInfo.getTxtRecords();
        Map<String, String> txtRecordMap = new HashMap<>(txtRecordCount);

        if (txtRecordBundle.size() > 0) {
            for (String key : txtRecordBundle.keySet()) {
                txtRecordMap.put(key, txtRecordBundle.getString(key));
            }
        }

        return ServiceInfo.create(
                serviceInfo.getType(),
                serviceInfo.getName(),
                serviceInfo.getPort(),
                0,
                0,
                true,
                txtRecordMap
        );
    }

	/* Begin static */

    static BonjourBroadcastBuilder newBuilder(String type) {
        return new SupportBonjourBroadcastBuilder(type);
    }

	/* Begin inner classes */

    private static final class SupportBonjourBroadcastBuilder extends BonjourBroadcastBuilder {

        /**
         * Suffix appended to input types
         */
        private static final String SUFFIX = ".local.";

        protected SupportBonjourBroadcastBuilder(String type) {
            super((type.endsWith(SUFFIX)) ? type : type + SUFFIX);
        }

        @Override public BonjourBroadcast build() {
            return new SupportBonjourBroadcast(this);
        }
    }
}
