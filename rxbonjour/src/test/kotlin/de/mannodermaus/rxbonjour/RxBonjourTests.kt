package de.mannodermaus.rxbonjour

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class RxBonjourTests {

  @Nested
  @DisplayName("Building RxBonjour Instances")
  class BuilderTests {

    @Test
    @DisplayName("Throws if Platform isn't provided to Builder")
    fun throwsIfPlatformMissing() {
      val driver = mock(Driver::class.java)
      val builder = RxBonjour.Builder().driver(driver)

      assertThrows(IllegalArgumentException::class.java, { builder.create() })
    }

    @Test
    @DisplayName("Throws if Driver isn't provided to Builder")
    fun throwsIfDriverMissing() {
      val platform = mock(Platform::class.java)
      val builder = RxBonjour.Builder().platform(platform)

      assertThrows(IllegalArgumentException::class.java, { builder.create() })
    }

    @Test
    @DisplayName("Successfully creates an RxBonjour instances if everything is provided")
    fun successfulIfPlatformAndDriverProvided() {
      val driver = mock(Driver::class.java)
      val platform = mock(Platform::class.java)
      val builder = RxBonjour.Builder()
          .driver(driver)
          .platform(platform)

      builder.create()
    }
  }

  @Nested
  @DisplayName("RxBonjour#newDiscovery()")
  class DiscoveryTests {

    private val VALID_BONJOUR_TYPE = "_http._tcp"

    @Test
    @DisplayName("Emit Error if not a Bonjour Type")
    fun emitErrorIfNotBonjourType() {
      val driver = mock(Driver::class.java)
      val platform = mock(Platform::class.java)
      val rxb = RxBonjour.Builder().driver(driver).platform(platform).create()

      rxb.newDiscovery("Totally Not Valid").test()
          .assertError({
            it is IllegalBonjourTypeException
                && it.message!!.contains("Totally Not Valid")
          })
    }

    @Test
    @DisplayName("Emit Error if Discovery fails after starting")
    fun emitErrorIfDiscoveryFailsAfterStarting() {
      val driver = FakeDriver()
      val platform = FakePlatform()
      val rxb = RxBonjour.Builder().driver(driver).platform(platform).create()

      val observer = rxb.newDiscovery(VALID_BONJOUR_TYPE).test()
      val expected = RuntimeException("driver crashed")
      driver.discoveryEngine.emitFailure(expected)

      observer.assertError({ it is DiscoveryFailedException && it.cause == expected })
    }

    @Test
    @DisplayName("Successful Round Trip")
    fun successfulRoundTrip() {
      val driver = FakeDriver()
      val platform = FakePlatform()

      // 1. Before subscribing
      val rxb = RxBonjour.Builder().driver(driver).platform(platform).create()
      assertEquals(DiscoveryState.New, driver.discoveryEngine.state())
      assertEquals(ConnectionState.New, platform.connection.state())

      // 2. Start discovering
      val observer = rxb.newDiscovery(VALID_BONJOUR_TYPE).test()
          .assertSubscribed()
          .assertEmpty()
          .assertNotComplete()

      assertEquals(DiscoveryState.Discovering, driver.discoveryEngine.state())
      assertEquals(ConnectionState.Initialized, platform.connection.state())

      // 3. Discover service
      val service = mock(BonjourService::class.java)
      driver.discoveryEngine.emitResolved(service)

      observer.assertValueCount(1)
      observer.assertValueAt(0, { it is BonjourEvent.Added && it.service == service })

      // 4. Lose service
      driver.discoveryEngine.emitLost(service)

      observer.assertValueCount(2)
      observer.assertValueAt(1, { it is BonjourEvent.Removed && it.service == service })

      // 5. Tear down discovery
      observer.dispose()

      observer.assertNotComplete().assertNoErrors()
      assertEquals(DiscoveryState.TornDown, driver.discoveryEngine.state())
      assertEquals(ConnectionState.TornDown, platform.connection.state())
    }
  }

  @Nested
  @DisplayName("RxBonjour#newBroadcast()")
  class BroadcastTests {

    private val VALID_BROADCAST = BonjourBroadcastConfig("_http._tcp")

    @Test
    @DisplayName("Emit Error if not a Bonjour Type")
    fun emitErrorIfNotBonjourType() {
      val driver = mock(Driver::class.java)
      val platform = mock(Platform::class.java)
      val rxb = RxBonjour.Builder().driver(driver).platform(platform).create()

      val config = VALID_BROADCAST.copy(type = "Totally Not Valid")
      rxb.newBroadcast(config).test()
          .assertError({
            it is IllegalBonjourTypeException
                && it.message!!.contains("Totally Not Valid")
          })
    }

    @Test
    @DisplayName("Emit Error if Broadcast fails after starting")
    fun emitErrorIfBroadcastFailsAfterStarting() {
      val driver = FakeDriver()
      val platform = FakePlatform()
      val rxb = RxBonjour.Builder().driver(driver).platform(platform).create()

      val observer = rxb.newBroadcast(VALID_BROADCAST).test()
      val expected = RuntimeException("driver crashed")
      driver.broadcastEngine.emitFailure(expected)

      observer.assertError({ it is BroadcastFailedException && it.cause == expected })
    }

    @Test
    @DisplayName("Successful Round Trip")
    fun successfulRoundTrip() {
      val driver = FakeDriver()
      val platform = FakePlatform()

      // 1. Before subscribing
      val rxb = RxBonjour.Builder().driver(driver).platform(platform).create()
      assertEquals(BroadcastState.New, driver.broadcastEngine.state())
      assertEquals(ConnectionState.New, platform.connection.state())

      // 2. Start broadcasting
      val observer = rxb.newBroadcast(VALID_BROADCAST).test()
          .assertSubscribed()
          .assertNotComplete()

      assertEquals(BroadcastState.Broadcasting, driver.broadcastEngine.state())
      assertEquals(ConnectionState.Initialized, platform.connection.state())

      // 5. Tear down broadcast
      observer.dispose()

      observer.assertNotComplete().assertNoErrors()
      assertEquals(BroadcastState.TornDown, driver.broadcastEngine.state())
      assertEquals(ConnectionState.TornDown, platform.connection.state())
    }
  }
}

@DisplayName("String.isBonjourType()")
class IsBonjourTypeTests {

  @Test
  fun valid() {
    assertTrue("_http._tcp".isBonjourType())
    assertTrue("_http._udp".isBonjourType())
    assertTrue("_ssh._tcp".isBonjourType())
    assertTrue("_ssh._udp".isBonjourType())
    assertTrue("_xmpp-server._tcp".isBonjourType())
    assertTrue("_printer._tcp".isBonjourType())
    assertTrue("_somelocalservice._tcp.local.".isBonjourType())
  }

  @Test
  fun invalid() {
    assertFalse("_invalid§/(chars._tcp".isBonjourType())
    assertFalse("_http._invalidprotocol".isBonjourType())
    assertFalse("wrong._format".isBonjourType())
  }
}
