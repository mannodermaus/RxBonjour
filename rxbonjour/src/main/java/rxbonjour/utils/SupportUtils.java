package rxbonjour.utils;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;

/**
 * Helper class to acquire some of the support implementation's common objects.
 */
public final class SupportUtils implements BonjourUtils<JmDNS> {

	private static SupportUtils instance;

	private JmDNS jmdnsInstance;
	/** Synchronization lock on the JmDNS instance */
	private final Object jmdnsLock = new Object();

	private SupportUtils() {
		//no instance
	}

	public static SupportUtils get() {
		if (instance == null) {
			instance = new SupportUtils();
		}
		return instance;
	}

	/**
	 * Returns the JmDNS shared among all subscribers for Bonjour events, creating it if necessary.
	 *
	 * @param context Context used to access the WifiManager for the device's IP address with which JmDNS is initialized
	 * @return The JmDNS instance
	 * @throws IOException In case the device's address can't be resolved
	 */
	@Override public JmDNS getManager(Context context) throws IOException {
		synchronized (jmdnsLock) {
			if (jmdnsInstance == null) {
				WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				InetAddress inetAddress = getInetAddress(wifiManager);
				jmdnsInstance = JmDNS.create(inetAddress, inetAddress.toString());
			}
			return jmdnsInstance;
		}
	}

	/**
	 * Returns the current connection's IP address.
	 * This implementation is taken from http://stackoverflow.com/a/13677686/1143172
	 * and takes note of a JmDNS issue with resolved IP addresses.
	 *
	 * @param wifiManager WifiManager to look up the IP address from
	 * @return The InetAddress of the current connection
	 * @throws IOException In case the InetAddress can't be resolved
	 */
	private InetAddress getInetAddress(WifiManager wifiManager) throws IOException {
		int intaddr = wifiManager.getConnectionInfo().getIpAddress();

		byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
				(byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
		return InetAddress.getByAddress(byteaddr);
	}
}
