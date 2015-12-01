package rxbonjour.broadcast;

import android.content.Context;
import android.os.Build;

import java.util.Map;

import rxbonjour.model.BonjourService;
import rxbonjour.utils.BonjourUtils;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;

public abstract class BonjourBroadcast<T extends BonjourUtils<?>> {

	protected final BonjourService serviceInfo;
	protected final T utils;

	protected BonjourBroadcast(BonjourBroadcastBuilder builder) {
		// Create a BonjourService object with the builder's properties
		BonjourService.Builder serviceBuilder = new BonjourService.Builder(builder.name(), builder.type())
				.setPort(builder.port())
				.addAddress(builder.address());

		Map<String, String> txtRecords = builder.txtRecords();
		if (txtRecords != null) {
			for (String key : txtRecords.keySet()) {
				serviceBuilder.addTxtRecord(key, txtRecords.get(key));
			}
		}

		this.serviceInfo = serviceBuilder.build();

		// Create the utilities reference
		this.utils = createUtils();
	}

	protected abstract T createUtils();

	public abstract rx.Observable<Void> start(Context context);

	public static BonjourBroadcastBuilder newBuilder(String type, boolean forceNsdManager) {
		if (forceNsdManager && Build.VERSION.SDK_INT >= JELLY_BEAN) {
			return JBBonjourBroadcast.newBuilder(type);
		} else {
			return SupportBonjourBroadcast.newBuilder(type);
		}
	}
}
