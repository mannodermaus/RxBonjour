package rxbonjour.broadcast;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

import rx.Observable;
import rxbonjour.model.BonjourEvent;
import rxbonjour.model.BonjourService;
import rxbonjour.utils.BonjourUtils;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;

public abstract class BonjourBroadcast<T extends BonjourUtils<?>> {

	protected final InetAddress address;
	protected final String name;
	protected final String type;
	protected final int port;
	protected final Map<String, String> txtRecords;
	protected final T utils;

	protected BonjourBroadcast(BonjourBroadcastBuilder builder) {
		this.address = builder.address();
		this.name = builder.name();
		this.type = builder.type();
		this.port = builder.port();
		this.txtRecords = builder.txtRecords();

		// Create the utilities reference
		this.utils = createUtils();
	}

	protected BonjourService createBonjourService(Context context) throws IOException {
		InetAddress ia = this.address;

		if (ia == null) {
			WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			ia = utils.getInetAddress(wifiManager);
		}

		// Create a BonjourService object with the builder's properties
		BonjourService.Builder serviceBuilder = new BonjourService.Builder(name, type)
				.setPort(port)
				.addAddress(ia);

		if (txtRecords != null) {
			for (String key : txtRecords.keySet()) {
				serviceBuilder.addTxtRecord(key, txtRecords.get(key));
			}
		}

		return serviceBuilder.build();
	}

	protected abstract T createUtils();

	public abstract Observable<BonjourEvent> start(Context context);

	public static BonjourBroadcastBuilder newBuilder(String type, boolean forceNsdManager) {
		if (forceNsdManager && Build.VERSION.SDK_INT >= JELLY_BEAN) {
			return JBBonjourBroadcast.newBuilder(type);
		} else {
			return SupportBonjourBroadcast.newBuilder(type);
		}
	}
}
