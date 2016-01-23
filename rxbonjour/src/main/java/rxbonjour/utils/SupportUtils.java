package rxbonjour.utils;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jmdns.JmDNS;
import javax.jmdns.impl.DNSStatefulObject;

/**
 * Helper class to acquire some of the support implementation's common objects.
 */
public final class SupportUtils extends BonjourUtils<JmDNS> {

	private static SupportUtils instance;

	private JmDNS jmdnsInstance;

	/** Synchronization lock on the JmDNS instance */
	private final Object jmdnsLock = new Object();

	/** Number of subscribers using JmDNS */
	private final AtomicInteger jmdnsSubscriberCount = new AtomicInteger(0);

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
			if (jmdnsInstance == null || !isAvailable()) {
				WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				InetAddress inetAddress = getInetAddress(wifiManager);
				jmdnsInstance = JmDNS.create(inetAddress, inetAddress.toString());
				jmdnsSubscriberCount.set(0);
			}
			return jmdnsInstance;
		}
	}

	/**
	 * Returns whether the JmDNS instance is not closing or closed.
     */
	private boolean isAvailable() {
		if (jmdnsInstance != null) {
			DNSStatefulObject dso = (DNSStatefulObject) jmdnsInstance;
			return !(dso.isClosing() || dso.isClosed());
		}

		return false;
	}

	/**
	 * Increments the count of JmDNS subscribers.
	 *
	 * @return The updated subscriber count
     */
	public int incrementSubscriberCount() {
		if (isAvailable()) {
			return jmdnsSubscriberCount.incrementAndGet();
		}
		return 0;
	}

	/**
	 * Decrements the count of JmDNS subscribers.
	 *
	 * @return The updated subscriber count
     */
	public int decrementSubscriberCount() {
		if (isAvailable()) {
			return jmdnsSubscriberCount.decrementAndGet();
		}
		return 0;
	}

	/**
	 * Closes the JmDNS instance if there are no longer any subscribers.
	 */
	public void closeIfNecessary() {
		if (jmdnsInstance != null) {
			if (jmdnsSubscriberCount.get() <= 0) {
				close();
			}
		}
	}

	/**
	 * Closes the JmDNS instance.
	 */
	public void close() {
		if (jmdnsInstance != null) {
			try {
				jmdnsInstance.close();
			} catch (IOException ignored) {
			} finally {
				jmdnsSubscriberCount.set(0);
			}
		}
	}
}
