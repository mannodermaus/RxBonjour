package rxbonjour;

import android.content.Context;
import android.os.Build;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rxbonjour.exc.TypeMalformedException;
import rxbonjour.internal.BonjourDiscovery;
import rxbonjour.internal.JBBonjourDiscovery;
import rxbonjour.internal.SupportBonjourDiscovery;
import rxbonjour.model.BonjourEvent;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;

/**
 * RxBonjour:
 * Enables clients to perform network service discovery for Bonjour devices using Reactive Extensions.
 *
 * @author marcel
 */
public final class RxBonjour {

	private static final String TYPE_PATTERN = "_[a-zA-Z0-9]+.(_tcp|_udp)";

	private RxBonjour() {
		throw new AssertionError("no instances");
	}

	/**
	 * Starts a Bonjour service discovery for the provided service type. This method chooses the correct NSD implementation based on
	 * the device's API level. This method's return Observable is scheduled to run on an I/O thread and notify subscribers
	 * on the main thread.
	 * <p/>
	 * This method will throw a Runtime Exception if the input type does not obey Bonjour type specifications. If you intend
	 * to use this method with arbitrary types that can be provided by user input, it is highly encouraged to verify this input
	 * using {@link #isBonjourType(String)} <b>before</b> calling this method!
	 *
	 * @param context Context of the request
	 * @param type    Type of service to discover
	 * @return An Observable for Bonjour events
	 */
	public static rx.Observable<BonjourEvent> startDiscovery(Context context, String type) {
		// Verify input
		if (!isBonjourType(type)) throw new TypeMalformedException(type);

		BonjourDiscovery discovery;
		if (Build.VERSION.SDK_INT >= JELLY_BEAN) {
			discovery = new JBBonjourDiscovery(context);
		} else {
			discovery = new SupportBonjourDiscovery(context);
		}
		return discovery.start(type)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
	}

	/**
	 * Checks the provided type String against Bonjour specifications, and returns whether or not the type is valid.
	 *
	 * @param type Type of service to check
	 * @return True if the type refers to a valid Bonjour type, false otherwise
	 */
	public static boolean isBonjourType(String type) {
		return type.matches(TYPE_PATTERN);
	}
}
