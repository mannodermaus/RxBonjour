package rxbonjour.internal;

import android.content.Context;

import rxbonjour.model.BonjourEvent;

/**
 * Base interface for DNS-SD implementations
 */
public interface BonjourDiscovery {

	/**
	 * Starts a Bonjour service discovery for the provided service type.
	 *
	 * @param context Context of the request
	 * @param type    Type of service to discover
	 * @return An Observable for Bonjour events
	 */
	rx.Observable<BonjourEvent> start(Context context, String type);
}
