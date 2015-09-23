package rxbonjour.internal;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;

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
import static org.mockito.Mockito.mock;
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
		Looper mainLooper = mock(Looper.class);
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

	@Test public void testAddAndRemoveCycle() throws Exception {
		BonjourDiscovery discovery = new SupportBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

		discovery.start(context, "_http._tcp").subscribe(subscriber);

		subscriber.assertNoErrors();
		verify(jmdns, times(1)).addServiceListener(anyString(), any(ServiceListener.class));
		subscriber.unsubscribe();
		verify(jmdns, times(1)).removeServiceListener(anyString(), any(ServiceListener.class));
		verify(jmdns, times(1)).close();
	}

	@Test public void testStaleContext() throws Exception {
		BonjourDiscovery discovery = new SupportBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

		discovery.start(null, "_http._tcp").subscribe(subscriber);

		subscriber.assertError(StaleContextException.class);
	}
}
