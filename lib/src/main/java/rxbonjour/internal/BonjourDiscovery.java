package rxbonjour.internal;

import android.content.Context;

import rxbonjour.model.BonjourEvent;

/**
 * @author marcel
 */
public abstract class BonjourDiscovery {

	protected final Context mContext;

	protected BonjourDiscovery(Context context) {
		mContext = context;
	}

	public abstract rx.Observable<BonjourEvent> start(String type);
}
