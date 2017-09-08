package de.mannodermaus.rxbonjour.exc;

import de.mannodermaus.rxbonjour.broadcast.BonjourBroadcast;

public class BroadcastFailed extends Exception {

    public BroadcastFailed(Class<? extends BonjourBroadcast> implClass, String type, int errorCode) {
        super(implClass.getSimpleName() + " broadcast failed for type " + type + " with error code " + errorCode);
    }

    public BroadcastFailed(Class<? extends BonjourBroadcast> implClass, String type) {
        super(implClass.getSimpleName() + " broadcast failed for type " + type);
    }
}
