package rxbonjour;

import org.junit.Test;

import rxbonjour.base.BaseTest;

import static org.junit.Assert.assertEquals;

public class RxBonjourTest extends BaseTest {

	@Test public void testBonjourTypes() {
		assertEquals(true, RxBonjour.isBonjourType("_http._tcp"));
		assertEquals(true, RxBonjour.isBonjourType("_http._udp"));
		assertEquals(true, RxBonjour.isBonjourType("_ssh._tcp"));
		assertEquals(true, RxBonjour.isBonjourType("_ssh._udp"));
		assertEquals(true, RxBonjour.isBonjourType("_xmpp-server._tcp"));
		assertEquals(true, RxBonjour.isBonjourType("_printer._tcp"));
		assertEquals(true, RxBonjour.isBonjourType("_somelocalservice._tcp.local."));

		assertEquals(false, RxBonjour.isBonjourType("_invalid§/(chars._tcp"));
		assertEquals(false, RxBonjour.isBonjourType("_http._invalidprotocol"));
		assertEquals(false, RxBonjour.isBonjourType("wrong._format"));
	}
}
