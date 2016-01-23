package rxbonjour.exc;

import rxbonjour.discovery.BonjourDiscovery;

/**
 * Thrown when service discovery fails upon starting
 */
public class DiscoveryFailed extends Exception {

	public DiscoveryFailed(Class<? extends BonjourDiscovery> implClass, String type, int errorCode) {
		super(implClass.getSimpleName() + " discovery failed for type " + type + " with error code " + errorCode);
	}

	public DiscoveryFailed(Class<? extends BonjourDiscovery> implClass, String type) {
		super(implClass.getSimpleName() + " discovery failed for type " + type);
	}
}
