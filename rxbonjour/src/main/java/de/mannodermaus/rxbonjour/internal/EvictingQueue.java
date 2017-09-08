package de.mannodermaus.rxbonjour.internal;

import java.util.concurrent.LinkedBlockingQueue;

public class EvictingQueue<E> extends LinkedBlockingQueue<E> {

    public EvictingQueue(int capacity) {
        super(capacity);
    }

    @Override public boolean add(E e) {
        if (remainingCapacity() == 0) remove();
        return super.add(e);
    }
}
