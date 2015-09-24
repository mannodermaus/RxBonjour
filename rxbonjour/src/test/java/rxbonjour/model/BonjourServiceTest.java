package rxbonjour.model;

import org.junit.Test;

import java.net.InetAddress;

import rxbonjour.base.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BonjourServiceTest extends BaseTest {

	@Test public void testV4Address() throws Exception {
		BonjourService service = new BonjourService.Builder("service", "_http._tcp")
				.setPort(80)
				.addAddress(InetAddress.getByName("127.0.0.1"))
				.build();

		assertEquals(InetAddress.getByName("127.0.0.1"), service.getHost());
		assertEquals(InetAddress.getByName("127.0.0.1"), service.getV4Host());
		assertNull(service.getV6Host());
	}

	@Test public void testV6Address() throws Exception {
		BonjourService service = new BonjourService.Builder("service", "_http._tcp")
				.setPort(80)
				.addAddress(InetAddress.getByName("::1"))
				.build();

		assertEquals(InetAddress.getByName("::1"), service.getHost());
		assertEquals(InetAddress.getByName("::1"), service.getV6Host());
		assertNull(service.getV4Host());
	}

	@Test public void testMultipleAddresses() throws Exception {
		BonjourService service = new BonjourService.Builder("service", "_http._tcp")
				.setPort(80)
				.addAddress(InetAddress.getByName("127.0.0.1"))
				.addAddress(InetAddress.getByName("::1"))
				.build();

		assertEquals(InetAddress.getByName("127.0.0.1"), service.getHost());
		assertEquals(InetAddress.getByName("127.0.0.1"), service.getV4Host());
		assertEquals(InetAddress.getByName("::1"), service.getV6Host());
	}
}
