package rxbonjour.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.nsd.NsdManager;

import org.junit.Test;

import rx.observers.TestSubscriber;
import rxbonjour.base.BaseTest;
import rxbonjour.exc.StaleContextException;
import rxbonjour.model.BonjourEvent;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressLint("NewApi")
public class JBBonjourDiscoveryTest extends BaseTest {

	private NsdManager nsdManager;

	@Override protected void setupMocks() throws Exception {
		nsdManager = mock(NsdManager.class);

		// Wire default return values
		when(context.getSystemService(Context.NSD_SERVICE)).thenReturn(nsdManager);
	}

	@Test public void testAddAndRemoveOneCycle() throws Exception {
		BonjourDiscovery discovery = new JBBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

		discovery.start(context, "_http._tcp").subscribe(subscriber);

		subscriber.assertNoErrors();
		verify(nsdManager, times(1)).discoverServices(eq("_http._tcp"), anyInt(), any(NsdManager.DiscoveryListener.class));
		subscriber.unsubscribe();
		verify(nsdManager, times(1)).stopServiceDiscovery(any(NsdManager.DiscoveryListener.class));
	}

	@Test public void testAddAndRemoveOneCycleWithLocalDomain() throws Exception {
		BonjourDiscovery discovery = new JBBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

		discovery.start(context, "_http._tcp.local.").subscribe(subscriber);

		subscriber.assertNoErrors();
		verify(nsdManager, times(1)).discoverServices(eq("_http._tcp.local."), anyInt(), any(NsdManager.DiscoveryListener.class));
		subscriber.unsubscribe();
		verify(nsdManager, times(1)).stopServiceDiscovery(any(NsdManager.DiscoveryListener.class));
	}

	@Test public void testAddAndRemoveTwoCycle() throws Exception {
		BonjourDiscovery discovery = new JBBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber1 = new TestSubscriber<>();
		TestSubscriber<BonjourEvent> subscriber2 = new TestSubscriber<>();

		discovery.start(context, "_http._tcp").subscribe(subscriber1);
		discovery.start(context, "_http._tcp").subscribe(subscriber2);

		subscriber1.assertNoErrors();
		subscriber2.assertNoErrors();
		verify(nsdManager, times(2)).discoverServices(eq("_http._tcp"), anyInt(), any(NsdManager.DiscoveryListener.class));
		subscriber1.unsubscribe();
		verify(nsdManager, times(1)).stopServiceDiscovery(any(NsdManager.DiscoveryListener.class));
		subscriber2.unsubscribe();
		verify(nsdManager, times(2)).stopServiceDiscovery(any(NsdManager.DiscoveryListener.class));
	}

	@Test public void testAddAndRemoveTwoDifferentTypesCycle() throws Exception {
		BonjourDiscovery discovery = new JBBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber1 = new TestSubscriber<>();
		TestSubscriber<BonjourEvent> subscriber2 = new TestSubscriber<>();

		discovery.start(context, "_http._tcp").subscribe(subscriber1);
		discovery.start(context, "_ssh._tcp").subscribe(subscriber2);

		subscriber1.assertNoErrors();
		subscriber2.assertNoErrors();
		verify(nsdManager, times(1)).discoverServices(eq("_http._tcp"), anyInt(), any(NsdManager.DiscoveryListener.class));
		verify(nsdManager, times(1)).discoverServices(eq("_ssh._tcp"), anyInt(), any(NsdManager.DiscoveryListener.class));
		subscriber1.unsubscribe();
		verify(nsdManager, times(1)).stopServiceDiscovery(any(NsdManager.DiscoveryListener.class));
		subscriber2.unsubscribe();
		verify(nsdManager, times(2)).stopServiceDiscovery(any(NsdManager.DiscoveryListener.class));
	}

	@Test public void testStaleContext() throws Exception {
		BonjourDiscovery discovery = new JBBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

		discovery.start(null, "_http._tcp").subscribe(subscriber);

		subscriber.assertError(StaleContextException.class);
	}

	// TODO Fill with more tests
}
