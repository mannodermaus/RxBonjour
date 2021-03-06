package de.mannodermaus.rxbonjour.drivers.nsdmanager

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import de.mannodermaus.rxbonjour.BonjourSchedulers
import de.mannodermaus.rxbonjour.DiscoveryCallback
import de.mannodermaus.rxbonjour.DiscoveryEngine
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.net.InetAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

private val BACKLOG_QUEUE_SIZE = 32

internal class NsdManagerDiscoveryEngine(
    private val context: Context,
    private val type: String) : DiscoveryEngine {

  private var nsdManager: NsdManager? = null
  private var listener: NsdDiscoveryListener? = null
  private var resolveBacklog: NsdResolveBacklog? = null

  override fun initialize() {
  }

  override fun discover(address: InetAddress, callback: DiscoveryCallback) {
    val nsdManager = context.getNsdManager()
    val resolveBacklog = NsdResolveBacklog(nsdManager, callback)

    this.nsdManager = nsdManager
    this.resolveBacklog = resolveBacklog
    this.listener = NsdDiscoveryListener(callback, resolveBacklog)

    nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
  }

  override fun teardown() {
    try {
      nsdManager?.stopServiceDiscovery(listener)
    } catch (ignored: Exception) {
      // "Service discovery not active on discoveryListener",
      // thrown if starting the service discovery was unsuccessful earlier
    } finally {
      resolveBacklog?.quit()
    }
  }

  private class NsdDiscoveryListener(val callback: DiscoveryCallback,
      val backlog: NsdResolveBacklog) : NsdManager.DiscoveryListener {
    override fun onServiceFound(service: NsdServiceInfo) {
      // Add the found service to the resolve backlog
      // (it'll be processed once the backlog gets to it).
      // The NsdServiceInfo passed to this method doesn't really have a lot of info,
      // so the callback isn't triggered from here directly. Instead,
      // the "serviceResolved" event happens inside the NsdResolveBacklog
      backlog.add(service)
    }

    override fun onServiceLost(service: NsdServiceInfo) {
      callback.serviceLost(service.toLibraryModel())
    }

    override fun onStartDiscoveryFailed(type: String?, code: Int) {
      callback.discoveryFailed(NsdDiscoveryException(code))
    }

    override fun onStopDiscoveryFailed(type: String?, code: Int) {
      callback.discoveryFailed(NsdDiscoveryException(code))
    }

    override fun onDiscoveryStarted(p0: String?) {
    }

    override fun onDiscoveryStopped(p0: String?) {
    }
  }
}

// Linear Processor of found NsdServiceInfo objects.
// Necessary because of NsdManager's "one resolve at a time" limitation
private class NsdResolveBacklog(
    private val nsdManager: NsdManager,
    private val callback: DiscoveryCallback) {

  /* Marker objects for processing the queue */

  object NEXT
  object STOP

  private val queue = EvictingQueue<Any>()
  private val subject = BehaviorSubject.create<Any>()
  private val disposable: Disposable
  private val idle = AtomicBoolean(true)

  init {
    disposable = subject
        .compose(BonjourSchedulers.observableAsync())
        .subscribe {
          try {
            // Take the next item pushed to the queue & abort if STOP marker.
            // This call blocks
            val next = queue.take()
            when (next) {
              is NsdServiceInfo -> {
                // Resolve the service
                idle.set(false)
                nsdManager.resolveService(next, object : NsdManager.ResolveListener {
                  override fun onResolveFailed(p0: NsdServiceInfo?, p1: Int) {
                  }

                  override fun onServiceResolved(service: NsdServiceInfo) {
                    callback.serviceResolved(service.toLibraryModel())
                    proceed()
                  }

                })
              }
              STOP -> throw InterruptedException()
            }

          } catch (ignored: InterruptedException) {
          }
        }
  }

  /** Terminates the work of this backlog instance */
  fun quit() {
    // Send the STOP signal
    queue.clear()
    queue.add(STOP)
    subject.onComplete()
    disposable.dispose()
  }

  /** Adds the provided item to the backlog's queue for processing */
  fun add(service: NsdServiceInfo) {
    queue.add(service)
    if (idle.get()) {
      proceed()
    }
  }

  /** Signalizes that the backlog can proceed with the next item */
  fun proceed() {
    idle.set(true)
    subject.onNext(NEXT)
  }
}

// Queue Implementation that automatically evicts
// the oldest element when trying to push beyond its capacity.
private class EvictingQueue<T>(size: Int = BACKLOG_QUEUE_SIZE) : LinkedBlockingQueue<T>(size) {
  override fun add(element: T): Boolean {
    if (remainingCapacity() == 0) remove()
    return super.add(element)
  }
}
