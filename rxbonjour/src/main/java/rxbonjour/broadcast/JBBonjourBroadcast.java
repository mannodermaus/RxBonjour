package rxbonjour.broadcast;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;

import java.lang.ref.WeakReference;
import java.util.Map;

import rx.Observable;
import rxbonjour.utils.JBUtils;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
final class JBBonjourBroadcast extends BonjourBroadcast<JBUtils> {

	protected JBBonjourBroadcast(BonjourBroadcastBuilder builder) {
		super(builder);
	}

	@Override protected JBUtils createUtils() {
		return JBUtils.get();
	}

	@Override public Observable<Void> start(Context context) {
		// Create a weak reference to the incoming Context
		final WeakReference<Context> weakContext = new WeakReference<>(context);

		return null;
	}

	private NsdServiceInfo createServiceInfo(BonjourBroadcastBuilder builder) {
		NsdServiceInfo serviceInfo = new NsdServiceInfo();
		serviceInfo.setServiceType(builder.type());
		serviceInfo.setServiceName(builder.name());
		serviceInfo.setHost(builder.address());
		serviceInfo.setPort(builder.port());

		// Add TXT records on Lollipop and up
		Map<String, String> txtRecords = builder.txtRecords();
		if (Build.VERSION.SDK_INT >= LOLLIPOP && txtRecords != null) {
			for (String key : txtRecords.keySet()) {
				serviceInfo.setAttribute(key, txtRecords.get(key));
			}
		}

		return serviceInfo;
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
