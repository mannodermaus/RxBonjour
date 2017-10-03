package de.mannodermaus.rxbonjour

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.net.Inet4Address
import java.net.Inet6Address

private val SERVICE = BonjourService(
        type = "_http._tcp",
        name = "Test Bonjour Service",
        v4Host = null,
        v6Host = null,
        port = 80,
        txtRecords = emptyMap())

@DisplayName("BonjourService")
class BonjourServiceTests {

    @Nested
    @DisplayName("BonjourService#host()")
    class HostTests {

        @Test
        @DisplayName("safe access if no address is present whatsoever")
        fun safeAccessIfNoAddressIsPresent() {
            val service = SERVICE.copy(
                    v4Host = null,
                    v6Host = null)

            assertEquals(null, service.host)
        }

        @Test
        @DisplayName("prefer IPv4 Address if both are present")
        fun preferV4AddressIfBothPresent() {
            val addressV4 = Mockito.mock(Inet4Address::class.java)
            val addressV6 = Mockito.mock(Inet6Address::class.java)
            val service = SERVICE.copy(
                    v4Host = addressV4,
                    v6Host = addressV6)

            assertEquals(addressV4, service.host)
        }

        @Test
        @DisplayName("use IPv6 Address if IPv4 isn't present")
        fun useV6AddressIfV4NotPresent() {
            val addressV6 = Mockito.mock(Inet6Address::class.java)
            val service = SERVICE.copy(
                    v4Host = null,
                    v6Host = addressV6)

            assertEquals(addressV6, service.host)
        }

        @Test
        @DisplayName("use IPv4 Address if IPv6 isn't present")
        fun useV4AddressIfV6NotPresent() {
            val addressV4 = Mockito.mock(Inet4Address::class.java)
            val service = SERVICE.copy(
                    v4Host = addressV4,
                    v6Host = null)

            assertEquals(addressV4, service.host)
        }
    }
}