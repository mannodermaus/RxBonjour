package de.mannodermaus.rxbonjour.platforms.desktop

import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicBoolean

internal class RunActionDisposable(private val action: () -> Unit) : Disposable {

    private val disposed: AtomicBoolean = AtomicBoolean(false)

    override fun dispose() {
        if (disposed.compareAndSet(false, true)) {
            action.invoke()
        }
    }

    override fun isDisposed(): Boolean = disposed.get()
}
