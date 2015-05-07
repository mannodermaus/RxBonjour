package rxbonjour.exc;

import rxbonjour.internal.BonjourDiscovery;

/**
 * @author marcel
 */
public class DiscoveryFailed extends Exception {

	public DiscoveryFailed(Class<? extends BonjourDiscovery> implClass, String type, int errorCode) {
		super(implClass.getSimpleName() + " discovery failed for type " + type + " with error code " + errorCode);
	}
}
