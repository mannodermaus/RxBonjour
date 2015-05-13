package rxbonjour.internal;

import android.content.Context;

import rxbonjour.model.BonjourEvent;

/**
 * @author marcel
 */
public abstract class BonjourDiscovery {

	protected BonjourDiscovery() {
	}

	public abstract rx.Observable<BonjourEvent> start(Context context, String type);
}
