package rxbonjour.utils;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.InetAddress;

public abstract class BonjourUtils<M> {

	abstract M getManager(Context context) throws IOException;

	/**
	 * Returns the current connection's IP address.
	 * This implementation is taken from http://stackoverflow.com/a/13677686/1143172
	 * and takes note of a JmDNS issue with resolved IP addresses.
	 *
	 * @param wifiManager WifiManager to look up the IP address from
	 * @return The InetAddress of the current connection
	 * @throws IOException In case the InetAddress can't be resolved
	 */
	public InetAddress getInetAddress(WifiManager wifiManager) throws IOException {
		int intaddr = wifiManager.getConnectionInfo().getIpAddress();

		byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
				(byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
		return InetAddress.getByAddress(byteaddr);
	}
}
