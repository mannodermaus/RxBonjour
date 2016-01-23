package rxbonjour.broadcast;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSStatefulObject;

import rx.observers.TestSubscriber;
import rxbonjour.base.BaseTest;
import rxbonjour.exc.StaleContextException;
import rxbonjour.model.BonjourEvent;

import static junit.framework.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({ JmDNS.class, SupportBonjourBroadcast.class })
public class SupportBonjourBroadcastTest extends BaseTest {

    private TestJmDNS jmdns;

    abstract class TestJmDNS extends JmDNS implements DNSStatefulObject {
    }

    @Override protected void setupMocks() throws Exception {
        WifiManager wifiManager = mock(WifiManager.class);
        WifiInfo wifiInfo = mock(WifiInfo.class);
        WifiManager.MulticastLock lock = mock(WifiManager.MulticastLock.class);
        jmdns = mock(TestJmDNS.class);

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

    private void setJmDNSMockClosed() {
        when(jmdns.isClosing()).thenReturn(true);
        when(jmdns.isClosed()).thenReturn(true);
    }

    @Test public void testAddAndRemoveOneCycle() throws Exception {
        BonjourBroadcastBuilder builder = PowerMockito.spy(SupportBonjourBroadcast.newBuilder("_http._tcp"));
        BonjourBroadcast broadcast = new SupportBonjourBroadcast(builder);
        TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();
        ArgumentCaptor<ServiceInfo> captor = ArgumentCaptor.forClass(ServiceInfo.class);

        broadcast.start(context).subscribe(subscriber);
        subscriber.assertNoErrors();

        verify(jmdns, times(1)).registerService(captor.capture());
        ServiceInfo serviceInfo = captor.getValue();
        assertEquals(serviceInfo.getType(), "_http._tcp.local.");

        subscriber.unsubscribe();
        verify(jmdns, times(1)).unregisterService(serviceInfo);
        verify(jmdns, times(1)).close();
        setJmDNSMockClosed();
    }

    @Test public void testAddAndRemoveOneCycleWithLocalDomain() throws Exception {
        BonjourBroadcastBuilder builder = PowerMockito.spy(SupportBonjourBroadcast.newBuilder("_http._tcp.local."));
        BonjourBroadcast broadcast = new SupportBonjourBroadcast(builder);
        TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();
        ArgumentCaptor<ServiceInfo> captor = ArgumentCaptor.forClass(ServiceInfo.class);

        broadcast.start(context).subscribe(subscriber);
        subscriber.assertNoErrors();

        verify(jmdns, times(1)).registerService(captor.capture());
        ServiceInfo serviceInfo = captor.getValue();
        assertEquals(serviceInfo.getType(), "_http._tcp.local.");

        subscriber.unsubscribe();
        verify(jmdns, times(1)).unregisterService(serviceInfo);
        verify(jmdns, times(1)).close();
        setJmDNSMockClosed();
    }

    @Test public void testAddAndRemoveTwoDifferentBroadcast() throws Exception {
        BonjourBroadcastBuilder bd1 = PowerMockito.spy(SupportBonjourBroadcast.newBuilder("_http._tcp"));
        BonjourBroadcast bc1 = new SupportBonjourBroadcast(bd1);

        BonjourBroadcastBuilder bd2 = PowerMockito.spy(SupportBonjourBroadcast.newBuilder("_ftp._tcp"));
        BonjourBroadcast bc2 = new SupportBonjourBroadcast(bd2);

        TestSubscriber<BonjourEvent> subscriber1 = new TestSubscriber<>();
        TestSubscriber<BonjourEvent> subscriber2 = new TestSubscriber<>();
        ArgumentCaptor<ServiceInfo> captor = ArgumentCaptor.forClass(ServiceInfo.class);

        bc1.start(context).subscribe(subscriber1);
        subscriber1.assertNoErrors();

        verify(jmdns, times(1)).registerService(captor.capture());
        ServiceInfo si1 = captor.getValue();
        assertEquals(si1.getType(), "_http._tcp.local.");

        bc2.start(context).subscribe(subscriber2);
        subscriber2.assertNoErrors();

        verify(jmdns, times(2)).registerService(captor.capture());
        ServiceInfo si2 = captor.getValue();
        assertEquals(si2.getType(), "_ftp._tcp.local.");

        subscriber1.unsubscribe();
        verify(jmdns, times(1)).unregisterService(si1);
        verify(jmdns, never()).close();

        subscriber2.unsubscribe();
        verify(jmdns, times(1)).unregisterService(si2);
        verify(jmdns, times(1)).close();
        setJmDNSMockClosed();
    }

    @Test public void testStaleContext() throws Exception {
        BonjourBroadcastBuilder builder = PowerMockito.spy(SupportBonjourBroadcast.newBuilder("_http._tcp.local."));
        BonjourBroadcast broadcast = new SupportBonjourBroadcast(builder);
        TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

        broadcast.start(null).subscribe(subscriber);

        subscriber.assertError(StaleContextException.class);
    }
}
