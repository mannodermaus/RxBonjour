package rxbonjour.broadcast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import rx.observers.TestSubscriber;
import rxbonjour.base.BaseTest;
import rxbonjour.exc.StaleContextException;
import rxbonjour.model.BonjourEvent;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressLint("NewApi")
@PrepareForTest({ JBBonjourBroadcast.class })
public class JBBonjourBroadcastTest extends BaseTest {

    private NsdManager nsdManager;

    @Override protected void setupMocks() throws Exception {
        WifiManager wifiManager = mock(WifiManager.class);
        WifiInfo wifiInfo = mock(WifiInfo.class);
        WifiManager.MulticastLock lock = mock(WifiManager.MulticastLock.class);
        nsdManager = mock(NsdManager.class);

        // Wire default return values
        when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);
        when(context.getSystemService(Context.NSD_SERVICE)).thenReturn(nsdManager);
        when(wifiManager.createMulticastLock(anyString())).thenReturn(lock);
        when(wifiInfo.getIpAddress()).thenReturn(0);
        when(wifiManager.getConnectionInfo()).thenReturn(wifiInfo);
    }

    @Test public void testAddAndRemoveOneCycle() throws Exception {
        BonjourBroadcastBuilder builder = PowerMockito.spy(JBBonjourBroadcast.newBuilder("_http._tcp"));
        BonjourBroadcast<?> broadcast = builder.build();
        TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

        broadcast.start(context).subscribe(subscriber);
        subscriber.assertNoErrors();

        verify(nsdManager).registerService(any(NsdServiceInfo.class), anyInt(), any(NsdManager.RegistrationListener.class));

        subscriber.unsubscribe();
        verify(nsdManager).unregisterService(any(NsdManager.RegistrationListener.class));
    }

    @Test public void testStaleContext() throws Exception {
        BonjourBroadcastBuilder builder = PowerMockito.spy(JBBonjourBroadcast.newBuilder("_http._tcp"));
        BonjourBroadcast<?> broadcast = new JBBonjourBroadcast(builder);
        TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

        broadcast.start(null).subscribe(subscriber);

        subscriber.assertError(StaleContextException.class);
    }
}
