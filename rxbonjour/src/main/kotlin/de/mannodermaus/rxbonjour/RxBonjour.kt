package de.mannodermaus.rxbonjour

import io.reactivex.Completable
import io.reactivex.Observable

/* Extensions & Constants */

private val TYPE_PATTERN = Regex("_[a-zA-Z0-9\\-_]+\\.(_tcp|_udp)(\\.[a-zA-Z0-9\\-]+\\.)?")

/* Classes */

class RxBonjour private constructor(
        private val platform: Platform,
        private val driver: Driver) {

    /**
     * Starts a Bonjour service discovery for the provided service type with the given {@link Driver}.
     * <p>
     * The stream will immediately end with an {@link IllegalBonjourTypeException}
     * if the input type does not obey Bonjour type specifications.
     * If you intend to use this method with arbitrary types that can be provided by user input,
     * it is highly encouraged to verify this input
     * using {@link #isBonjourType(String)} <b>before</b> calling this method!
     *
     * @param type    Type of service to discover
     * @return An {@link Observable} of {@link BonjourEvent}s for the specific type
     */
    fun newDiscovery(type: String): Observable<BonjourEvent> =
            if (type.isBonjourType()) {
                // New Discovery request for the Driver
                val discovery = driver.createDiscovery(type)
                val connection = platform.createConnection()

                Observable.defer<BonjourEvent> {
                    Observable.create { emitter ->
                        // Initialization
                        discovery.initialize()
                        connection.initialize()

                        // Lifetime
                        val callback = object : DiscoveryCallback {
                            override fun discoveryFailed(code: Int) {
                                // Abort stream
                                emitter.onError(DiscoveryFailedException(driver.name, code))
                            }

                            override fun serviceResolved(service: BonjourService) {
                                // Convert to event
                                emitter.onNext(BonjourEvent.Added(service))
                            }

                            override fun serviceLost(service: BonjourService) {
                                // Convert to event
                                emitter.onNext(BonjourEvent.Removed(service))
                            }
                        }
                        val address = platform.getWifiAddress()
                        discovery.discover(address, callback)

                        // Destruction
                        val disposable = platform.runOnTeardown {
                            discovery.teardown()
                            connection.teardown()
                        }
                        emitter.setDisposable(disposable)
                    }
                }

            } else {
                // Not a Bonjour type
                Observable.error(IllegalBonjourTypeException(type))
            }

    /**
     * Starts a Bonjour service broadcast with the given configuration.
     * <p>
     * The stream will immediately end with an {@link IllegalBonjourTypeException}
     * if the input type does not obey Bonjour type specifications.
     * If you intend to use this method with arbitrary types that can be provided by user input,
     * it is highly encouraged to verify this input
     * using {@link #isBonjourType(String)} <b>before</b> calling this method!
     * <p>
     * When the returned {@link Completable} is unsubscribed from, the broadcast ends.
     *
     * @param config    Configuration of the service to advertise
     * @return A {@link Completable} holding the state of the broadcast, valid until unsubscription
     */
    fun newBroadcast(config: BonjourBroadcast): Completable =
            if (config.type.isBonjourType()) {
                // New Broadcast request for the Driver
                val broadcast = driver.createBroadcast()
                val connection = platform.createConnection()

                Completable.defer {
                    Completable.create { emitter ->
                        // Initialization
                        broadcast.initialize()
                        connection.initialize()

                        // Lifetime
                        val address = config.address ?: platform.getWifiAddress()
                        broadcast.start(address, config)

                        // Destruction
                        val disposable = platform.runOnTeardown {
                            broadcast.teardown()
                            connection.teardown()
                        }
                        emitter.setDisposable(disposable)
                    }
                }

            } else {
                // Not a Bonjour type
                Completable.error(IllegalBonjourTypeException(config.type))
            }

    class Builder {
        private var platform: Platform? = null
        private var driver: Driver? = null

        fun platform(platform: Platform) = also { this.platform = platform }
        fun driver(driver: Driver) = also { this.driver = driver }

        fun create(): RxBonjour {
            require(platform != null, { "You need to provide a platform() to RxBonjour's builder" })
            require(driver != null, { "You need to provide a driver() to RxBonjour's builder" })
            return RxBonjour(platform!!, driver!!)
        }
    }

    companion object {
        @JvmStatic
        fun String.isBonjourType() = this.matches(TYPE_PATTERN)
    }
}
