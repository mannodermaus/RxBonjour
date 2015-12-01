package rxbonjour.broadcast;

import android.content.Context;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import rx.Observable;
import rx.Subscriber;
import rxbonjour.exc.StaleContextException;
import rxbonjour.utils.SupportUtils;

final class SupportBonjourBroadcast extends BonjourBroadcast<SupportUtils> {

	protected SupportBonjourBroadcast(BonjourBroadcastBuilder builder) {
		super(builder);
	}

	@Override protected SupportUtils createUtils() {
		return SupportUtils.get();
	}

	@Override public Observable<Void> start(Context context) {
		// Create a weak reference to the incoming Context
		final WeakReference<Context> weakContext = new WeakReference<>(context);

		Observable<Void> obs = Observable.create(new Observable.OnSubscribe<Void>() {
			@Override public void call(Subscriber<? super Void> subscriber) {
				Context context = weakContext.get();
				if (context == null) {
					subscriber.onError(new StaleContextException());
					return;
				}

				// Create a JmDNS service using the BonjourService information and register that
				ServiceInfo jmdnsService = createJmdnsService();

			}
		});
		return null;
	}

	private ServiceInfo createJmdnsService() {
		int txtRecordCount = serviceInfo.getTxtRecordCount();
		Bundle txtRecordBundle = serviceInfo.getTxtRecords();
		Map<String, String> txtRecordMap = new HashMap<>(txtRecordCount);
		for (String key : txtRecordBundle.keySet()) {
			txtRecordMap.put(key, txtRecordBundle.getString(key));
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

		protected SupportBonjourBroadcastBuilder(String type) {
			super(type);
		}

		@Override public BonjourBroadcast build() {
			return new SupportBonjourBroadcast(this);
		}
	}
}
