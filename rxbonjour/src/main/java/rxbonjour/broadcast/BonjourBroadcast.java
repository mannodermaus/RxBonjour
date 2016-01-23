package rxbonjour.broadcast;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

import rxbonjour.model.BonjourEvent;
import rxbonjour.model.BonjourService;
import rxbonjour.utils.BonjourUtils;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;

public abstract class BonjourBroadcast<T extends BonjourUtils<?>> {

	protected final BonjourBroadcastBuilder broadcastBuilder;
	protected final T utils;

	protected BonjourBroadcast(BonjourBroadcastBuilder builder) {
		broadcastBuilder = builder;

		// Create the utilities reference
		this.utils = createUtils();
	}

	protected BonjourService createBonjourService(Context context) throws IOException {
		InetAddress ia = broadcastBuilder.address();

		if (ia == null) {
			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			ia = utils.getInetAddress(wifiManager);
		}

		// Create a BonjourService object with the builder's properties
		BonjourService.Builder serviceBuilder = new BonjourService.Builder(broadcastBuilder.name(),
				broadcastBuilder.type())
				.setPort(broadcastBuilder.port())
				.addAddress(ia);

		Map<String, String> txtRecords = broadcastBuilder.txtRecords();
		if (txtRecords != null) {
			for (String key : txtRecords.keySet()) {
				serviceBuilder.addTxtRecord(key, txtRecords.get(key));
			}
		}

		return serviceBuilder.build();
	}

	protected abstract T createUtils();

	public abstract rx.Observable<BonjourEvent> start(Context context);

	public static BonjourBroadcastBuilder newBuilder(String type, boolean forceNsdManager) {
		if (forceNsdManager && Build.VERSION.SDK_INT >= JELLY_BEAN) {
			return JBBonjourBroadcast.newBuilder(type);
		} else {
			return SupportBonjourBroadcast.newBuilder(type);
		}
	}
}
