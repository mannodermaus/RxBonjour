package rxbonjour.model;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * Resolved Bonjour service detected within the device's local network.
 *
 * @author marcel
 */
public class BonjourService {

	private String mName;
	private String mType;
	private Inet4Address mV4Host;
	private Inet6Address mV6Host;
	private int mPort;
	private Bundle mTxtRecords;

	private BonjourService(String name, String type, Inet4Address v4Host, Inet6Address v6Host, int port, Bundle txtRecords) {
		mName = name;
		mType = type;
		mV4Host = v4Host;
		mV6Host = v6Host;
		mPort = port;
		mTxtRecords = txtRecords;
	}

	/**
	 * @return The service's display name, e.g. 'Office Printer'
	 */
	public @NonNull String getName() {
		return mName;
	}

	/**
	 * @return The service's type, e.g. '_http._tcp.local.'
	 */
	public @NonNull String getType() {
		return mType;
	}

	/**
	 * Obtains the host address of the service.
	 * For services with both an IPv4 <strong>and</strong> an IPv6 address, the former address takes precedence over the latter,
	 * so that it always favors the v4 address over the v6 one.
	 * 
	 * If you need to access specific addresses, consider using {@link #getV4Host()} and {@link #getV6Host()}, respectively.
	 *
	 * @return A host address of the service
	 */
	public @NonNull InetAddress getHost() {
		return (mV4Host != null) ? mV4Host : mV6Host;
	}

	/**
	 * @return The IPv4 host address of the service, or null if it doesn't provide any
	 */
	public @Nullable Inet4Address getV4Host() {
		return mV4Host;
	}

	/**
	 * @return The IPv6 host address of the service, or null if it doesn't provide any
	 */
	public @Nullable Inet6Address getV6Host() {
		return mV6Host;
	}

	/**
	 * @return The port on which the service is being broadcast
	 */
	public int getPort() {
		return mPort;
	}

	/**
	 * @return The number of TXT records associated with the service
	 */
	public int getTxtRecordCount() {
		return mTxtRecords.size();
	}

	/**
	 * Returns a Bundle containing all TXT records associated with the service, stored as &lt;String, String&gt; key-value pairs.
	 * 
	 * If the service doesn't have any TXT records, or none could be resolved, this returns an empty Bundle
	 *
	 * @return A Bundle containing the service's TXT records
	 */
	public @NonNull Bundle getTxtRecords() {
		return mTxtRecords;
	}

	/**
	 * Returns the specific TXT record with the provided key, falling back to the default value if this TXT record doesn't exist.
	 *
	 * @param key          Key of the TXT record
	 * @param defaultValue Value to return if the TXT record isn't contained in the service's records
	 * @return The associated value for the provided key, or the default value if absent
	 */
	public String getTxtRecord(String key, String defaultValue) {
		String value = mTxtRecords.getString(key);
		return value != null ? value : defaultValue;
	}

	/**
	 * Returns the specific TXT record with the provided key, or null if no such mapping exists.
	 * This method is a shorthand for <pre>getTxtRecord(key, null)</pre>
	 *
	 * @param key Key of the TXT record
	 * @return The associated value for the provided key, or null if absent
	 */
	public @Nullable String getTxtRecord(String key) {
		return getTxtRecord(key, null);
	}

	@Override public String toString() {
		return "BonjourService{" +
				"mName='" + mName + '\'' +
				", mType='" + mType + '\'' +
				", mV4Host=" + mV4Host +
				", mV6Host=" + mV6Host +
				", mPort=" + mPort +
				'}';
	}

	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BonjourService)) return false;

		BonjourService that = (BonjourService) o;

		if (mPort != that.mPort) return false;
		if (!mName.equals(that.mName)) return false;
		if (!mType.equals(that.mType)) return false;
		if (mV4Host != null ? !mV4Host.equals(that.mV4Host) : that.mV4Host != null) return false;
		return !(mV6Host != null ? !mV6Host.equals(that.mV6Host) : that.mV6Host != null);
	}

	@Override public int hashCode() {
		int result = mName != null ? mName.hashCode() : 0;
		result = 31 * result + (mType != null ? mType.hashCode() : 0);
		result = 31 * result + (mV4Host != null ? mV4Host.hashCode() : 0);
		result = 31 * result + (mV6Host != null ? mV6Host.hashCode() : 0);
		result = 31 * result + mPort;
		return result;
	}

	/* Begin static */

	public static final class Builder {

		private String mName;
		private String mType;
		private Inet4Address mHostv4;
		private Inet6Address mHostv6;
		private int mPort;
		private Bundle mTxtRecords;

		public Builder(String name, String type) {
			mName = name;
			mType = type;
		}

		public Builder addAddress(InetAddress address) {
			if (address instanceof Inet4Address) {
				mHostv4 = (Inet4Address) address;

			} else if (address instanceof Inet6Address) {
				mHostv6 = (Inet6Address) address;
			}
			return this;
		}

		public Builder setPort(int port) {
			mPort = port;
			return this;
		}

		public Builder addTxtRecord(String key, String value) {
			if (mTxtRecords == null) mTxtRecords = new Bundle();
			mTxtRecords.putString(key, value);
			return this;
		}

		public BonjourService build() {
			if (mTxtRecords == null) mTxtRecords = new Bundle(0);
			return new BonjourService(mName, mType, mHostv4, mHostv6, mPort, mTxtRecords);
		}
	}
}
