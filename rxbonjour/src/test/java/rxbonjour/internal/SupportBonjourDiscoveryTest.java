package rxbonjour.internal;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceListener;

import rx.observers.TestSubscriber;
import rxbonjour.base.BaseTest;
import rxbonjour.exc.StaleContextException;
import rxbonjour.model.BonjourEvent;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({ JmDNS.class })
public class SupportBonjourDiscoveryTest extends BaseTest {

	private JmDNS jmdns;

	@Override protected void setupMocks() throws Exception {
		WifiManager wifiManager = mock(WifiManager.class);
		WifiInfo wifiInfo = mock(WifiInfo.class);
		WifiManager.MulticastLock lock = mock(WifiManager.MulticastLock.class);
		jmdns = mock(JmDNS.class);

		// Wire default return values
		when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);
		when(wifiManager.createMulticastLock(anyString())).thenReturn(lock);
		when(wifiInfo.getIpAddress()).thenReturn(0);
		when(wifiManager.getConnectionInfo()).thenReturn(wifiInfo);

		// Mock statics
		mockStatic(JmDNS.class);

		// Wire statics
		given(JmDNS.create(any(InetAddress.class), anyString())).willReturn(jmdns);
	}

	@Test public void testAddAndRemoveOneCycle() throws Exception {
		BonjourDiscovery discovery = new SupportBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

		discovery.start(context, "_http._tcp").subscribe(subscriber);

		subscriber.assertNoErrors();
		verify(jmdns, times(1)).addServiceListener(eq("_http._tcp.local."), any(ServiceListener.class));
		subscriber.unsubscribe();
		verify(jmdns, times(1)).removeServiceListener(eq("_http._tcp.local."), any(ServiceListener.class));
		verify(jmdns, times(1)).close();
	}

	@Test public void testAddAndRemoveOneCycleWithLocalDomain() throws Exception {
		BonjourDiscovery discovery = new SupportBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

		discovery.start(context, "_http._tcp.local.").subscribe(subscriber);

		subscriber.assertNoErrors();
		verify(jmdns, times(1)).addServiceListener(eq("_http._tcp.local."), any(ServiceListener.class));
		subscriber.unsubscribe();
		verify(jmdns, times(1)).removeServiceListener(eq("_http._tcp.local."), any(ServiceListener.class));
		verify(jmdns, times(1)).close();
	}

	@Test public void testAddAndRemoveTwoCycle() throws Exception {
		BonjourDiscovery discovery = new SupportBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber1 = new TestSubscriber<>();
		TestSubscriber<BonjourEvent> subscriber2 = new TestSubscriber<>();

		discovery.start(context, "_http._tcp").subscribe(subscriber1);
		discovery.start(context, "_http._tcp").subscribe(subscriber2);

		subscriber1.assertNoErrors();
		subscriber2.assertNoErrors();
		verify(jmdns, times(2)).addServiceListener(eq("_http._tcp.local."), any(ServiceListener.class));
		subscriber1.unsubscribe();
		verify(jmdns, times(1)).removeServiceListener(eq("_http._tcp.local."), any(ServiceListener.class));
		verify(jmdns, never()).close();
		subscriber2.unsubscribe();
		verify(jmdns, times(2)).removeServiceListener(eq("_http._tcp.local."), any(ServiceListener.class));
		verify(jmdns, times(1)).close();
	}

	@Test public void testAddAndRemoveTwoDifferentTypesCycle() throws Exception {
		BonjourDiscovery discovery = new SupportBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber1 = new TestSubscriber<>();
		TestSubscriber<BonjourEvent> subscriber2 = new TestSubscriber<>();

		discovery.start(context, "_http._tcp").subscribe(subscriber1);
		discovery.start(context, "_ssh._tcp").subscribe(subscriber2);

		subscriber1.assertNoErrors();
		subscriber2.assertNoErrors();
		verify(jmdns, times(1)).addServiceListener(eq("_http._tcp.local."), any(ServiceListener.class));
		verify(jmdns, times(1)).addServiceListener(eq("_ssh._tcp.local."), any(ServiceListener.class));
		subscriber1.unsubscribe();
		verify(jmdns, times(1)).removeServiceListener(eq("_http._tcp.local."), any(ServiceListener.class));
		verify(jmdns, never()).close();
		subscriber2.unsubscribe();
		verify(jmdns, times(1)).removeServiceListener(eq("_ssh._tcp.local."), any(ServiceListener.class));
		verify(jmdns, times(1)).close();
	}

	@Test public void testStaleContext() throws Exception {
		BonjourDiscovery discovery = new SupportBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

		discovery.start(null, "_http._tcp").subscribe(subscriber);

		subscriber.assertError(StaleContextException.class);
	}

	// TODO Fill with more tests
}
