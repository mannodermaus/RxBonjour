package de.mannodermaus.rxbonjour

import io.reactivex.disposables.Disposable
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Fake Implementations of the Driver interfaces,
 * useful for assertions during unit testing.
 */

enum class DiscoveryState {
    New,
    Initialized,
    Discovering,
    TornDown
}

enum class BroadcastState {
    New,
    Initialized,
    Broadcasting,
    TornDown
}

class FakeDriver : Driver {
    val discoveryEngine: FakeDiscoveryEngine = FakeDiscoveryEngine()
    val broadcastEngine: FakeBroadcastEngine = FakeBroadcastEngine()

    override val name: String = "fake"
    override fun createDiscovery(type: String) = discoveryEngine
    override fun createBroadcast(): BroadcastEngine = broadcastEngine
}

class FakeDiscoveryEngine : DiscoveryEngine {
    private var state: DiscoveryState = DiscoveryState.New
    private var callback: DiscoveryCallback? = null

    fun state() = state

    override fun initialize() {
        state = DiscoveryState.Initialized
    }

    override fun discover(address: InetAddress, callback: DiscoveryCallback) {
        this.state = DiscoveryState.Discovering
        this.callback = callback
    }

    override fun teardown() {
        state = DiscoveryState.TornDown
    }

    fun emitFailure(error: Exception) {
        require(state == DiscoveryState.Discovering)
        callback?.discoveryFailed(error)
    }

    fun emitResolved(service: BonjourService) {
        require(state == DiscoveryState.Discovering)
        callback?.serviceResolved(service)
    }

    fun emitLost(service: BonjourService) {
        require(state == DiscoveryState.Discovering)
        callback?.serviceLost(service)
    }
}

class FakeBroadcastEngine : BroadcastEngine {
    private var state: BroadcastState = BroadcastState.New
    private var callback: BroadcastCallback? = null

    fun state() = state

    override fun initialize() {
        state = BroadcastState.Initialized
    }

    override fun start(address: InetAddress, config: BonjourBroadcastConfig, callback: BroadcastCallback) {
        this.state = BroadcastState.Broadcasting
        this.callback = callback
    }

    override fun teardown() {
        state = BroadcastState.TornDown
    }

    fun emitFailure(error: Exception) {
        require(state == BroadcastState.Broadcasting)
        callback?.broadcastFailed(error)
    }
}

/*
 * Fake Implementations of the Platform interfaces,
 * useful for assertions during unit testing.
 */

enum class ConnectionState {
    New,
    Initialized,
    TornDown
}

class FakePlatform(
        private val address: InetAddress = mock(InetAddress::class.java)) : Platform {
    val connection: FakePlatformConnection = FakePlatformConnection()

    override fun createConnection() = connection
    override fun getWifiAddress() = address

    override fun runOnTeardown(action: () -> Unit): Disposable? {
        return object : Disposable {
            private val disposed: AtomicBoolean = AtomicBoolean(false)

            override fun dispose() {
                if (disposed.compareAndSet(false, true)) {
                    action.invoke()
                }
            }

            override fun isDisposed(): Boolean = disposed.get()
        }
    }
}

class FakePlatformConnection : PlatformConnection {

    private var state: ConnectionState = ConnectionState.New

    fun state() = state

    override fun initialize() {
        state = ConnectionState.Initialized
    }

    override fun teardown() {
        state = ConnectionState.TornDown
    }
}
