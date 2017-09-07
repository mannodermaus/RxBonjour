package de.mannodermaus.rxbonjour.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Backlog manager class, polling objects and processing them until an external object calls
 * {@link #proceed()}.
 */
public abstract class Backlog<T> {

    /**
     * Item sent to the Backlog upon requesting the next item to be consumed.
     * Actually unused, but RxJava 2.x doesn't allow null values anymore
     */
    private static final Object NEXT_MARKER = new Object();

    /**
     * Item sent to the Backlog upon requesting to quit processing with it
     */
    private static final Object STOP_MARKER = new Object();

    /**
     * Queue to which pending objects are added
     */
    private BlockingQueue<Object> queue = new LinkedBlockingQueue<>(32);

    /**
     * Subject responsible for notifying the backlog to continue processing
     */
    private BehaviorSubject<Object> subject;

    /**
     * Disposable to the subject, held right after instantiation
     */
    private Disposable disposable;

    /**
     * Busy flag, set upon processing an item, until {@link #proceed()} is called
     */
    private AtomicBoolean idle = new AtomicBoolean(true);

    /**
     * Constructor
     */
    public Backlog() {
        subject = BehaviorSubject.create();
        disposable = subject
                .compose(BonjourSchedulers.backlogSchedulers())
                .subscribe(new Consumer<Object>() {
                    @Override public void accept(Object unused) {
                        try {
                            // Take an item pushed to the queue and check for the STOP marker
                            Object info = queue.take();
                            if (!STOP_MARKER.equals(info)) {
                                // If an item other than the STOP marker is added,
                                // invoke the onNext callback with it
                                idle.set(false);

                                //noinspection unchecked
                                onNext(Backlog.this, (T) info);
                            }

                        } catch (InterruptedException ignored) {
                        }
                    }
                });
    }

    /**
     * Terminates the work of this backlog instance
     */
    public void quit() {
        // Send the STOP signal to the queue
        queue.add(STOP_MARKER);
        subject.onComplete();
        disposable.dispose();
    }

    /**
     * Adds the provided item to the backlog's queue for processing
     *
     * @param item Item enqueued to the backlog
     */
    public void add(T item) {
        // Add to the queue, and if ready for another item, proceed right away
        queue.add(item);
        if (idle.get()) proceed();
    }

    /**
     * Signalizes that the backlog can proceed with the next item
     */
    public void proceed() {
        idle.set(true);
        subject.onNext(NEXT_MARKER);
    }

	/* Begin abstract */

    /**
     * Callback invoked upon processing an item. This method is executed on a background thread;
     * after the caller is done processing the item, it is his responsibility to call {@link #proceed()}
     * or {@link #quit()} on the backlog.
     *
     * @param item Item to be processed next
     */
    public abstract void onNext(Backlog<T> backlog, T item);
}
