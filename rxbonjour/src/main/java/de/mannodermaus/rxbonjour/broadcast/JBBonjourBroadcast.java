package de.mannodermaus.rxbonjour.broadcast;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import de.mannodermaus.rxbonjour.exc.BroadcastFailed;
import de.mannodermaus.rxbonjour.exc.StaleContextException;
import de.mannodermaus.rxbonjour.model.BonjourEvent;
import de.mannodermaus.rxbonjour.model.BonjourService;
import de.mannodermaus.rxbonjour.utils.JBUtils;
import rx.Emitter;
import rx.Observable;
import rx.android.MainThreadSubscription;
import rx.functions.Action1;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
final class JBBonjourBroadcast extends BonjourBroadcast<JBUtils> {

    protected JBBonjourBroadcast(BonjourBroadcastBuilder builder) {
        super(builder);
    }

    @Override protected JBUtils createUtils() {
        return JBUtils.get();
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

                try {
                    final BonjourService bonjourService = createBonjourService(context);
                    final NsdServiceInfo nsdService = createServiceInfo(bonjourService);
                    final NsdManager nsdManager = utils.getManager(context);

                    final NsdManager.RegistrationListener listener = new NsdManager.RegistrationListener() {
                        @Override
                        public void onRegistrationFailed(NsdServiceInfo info, int errorCode) {
                            emitter.onError(new BroadcastFailed(JBBonjourBroadcast.class,
                                    bonjourService.getName(), errorCode));
                        }

                        @Override
                        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        }

                        @Override
                        public void onServiceRegistered(NsdServiceInfo info) {
                            emitter.onNext(new BonjourEvent(BonjourEvent.Type.ADDED, bonjourService));
                        }

                        @Override
                        public void onServiceUnregistered(NsdServiceInfo info) {
                            emitter.onNext(new BonjourEvent(BonjourEvent.Type.REMOVED, mapNsdServiceInfo(info)));
                        }
                    };

                    nsdManager.registerService(nsdService, NsdManager.PROTOCOL_DNS_SD, listener);

                    emitter.setSubscription(new MainThreadSubscription() {
                        @Override
                        protected void onUnsubscribe() {
                            try {
                                nsdManager.unregisterService(listener);
                            } catch (IllegalArgumentException ignored) {
                                ignored.printStackTrace();
                            }
                        }
                    });
                } catch (IOException e) {
                    emitter.onError(new BroadcastFailed(JBBonjourBroadcast.class, type));
                }
            }
        }, Emitter.BackpressureMode.LATEST);
    }

    private BonjourService mapNsdServiceInfo(NsdServiceInfo info) {
        BonjourService.Builder builder =
                new BonjourService.Builder(info.getServiceName(), info.getServiceType())
                        .addAddress(info.getHost())
                        .setPort(info.getPort());

        if (Build.VERSION.SDK_INT >= LOLLIPOP) {
            Map<String, byte[]> attrs = info.getAttributes();
            for (Map.Entry<String, byte[]> entry : attrs.entrySet()) {
                builder.addTxtRecord(entry.getKey(),
                        new String(entry.getValue(), StandardCharsets.UTF_8));
            }
        }

        return builder.build();
    }

    private NsdServiceInfo createServiceInfo(BonjourService serviceInfo) throws IOException {
        NsdServiceInfo nsdService = new NsdServiceInfo();
        nsdService.setServiceType(serviceInfo.getType());
        nsdService.setServiceName(serviceInfo.getName());
        nsdService.setHost(serviceInfo.getHost());
        nsdService.setPort(serviceInfo.getPort());

        // Add TXT records on Lollipop and up
        if (Build.VERSION.SDK_INT >= LOLLIPOP) {
            Bundle txtRecordBundle = serviceInfo.getTxtRecords();
            Map<String, String> txtRecordMap = new HashMap<>(serviceInfo.getTxtRecordCount());
            for (String key : txtRecordBundle.keySet()) {
                txtRecordMap.put(key, txtRecordBundle.getString(key));
            }

            for (String key : txtRecordMap.keySet()) {
                nsdService.setAttribute(key, txtRecordMap.get(key));
            }
        }

        return nsdService;
    }

	/* Begin static */

    static BonjourBroadcastBuilder newBuilder(String type) {
        return new JBBonjourBroadcastBuilder(type);
    }

	/* Begin inner classes */

    private static final class JBBonjourBroadcastBuilder extends BonjourBroadcastBuilder {

        protected JBBonjourBroadcastBuilder(String type) {
            super(type);
        }

        @Override public BonjourBroadcast build() {
            return new JBBonjourBroadcast(this);
        }
    }
}
