package rxbonjour.model;

import android.os.Bundle;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Map;

/**
 * @author marcel
 */
public class BonjourService {

	private String mName;
	private String mType;
	private Inet4Address mHostv4;
	private Inet6Address mHostv6;
	private int mPort;
	private Bundle mTxtRecords;

	public BonjourService(String name, String type, Inet4Address hostv4, Inet6Address hostv6, int port, Bundle txtRecords) {
		mName = name;
		mType = type;
		mHostv4 = hostv4;
		mHostv6 = hostv6;
		mPort = port;
		mTxtRecords = txtRecords;
	}

	public String getName() {
		return mName;
	}

	public String getType() {
		return mType;
	}

	@Deprecated
	public InetAddress getHost() {
		if (mHostv4 != null) {
			return mHostv4;
		} else {
			return mHostv6;
		}
	}

	public Inet4Address getmHostv4() {
		return mHostv4;
	}

	public Inet6Address getmHostv6() {
		return mHostv6;
	}

	public int getPort() {
		return mPort;
	}

	public int getTxtRecordCount() {
		return mTxtRecords.size();
	}

	public Bundle getTxtRecords() {
		return mTxtRecords;
	}

	public String getTxtRecord(String key, String defaultValue) {
		String value = mTxtRecords.getString(key);
		return value != null ? value : defaultValue;
	}

	@Override public String toString() {
		return "BonjourService{" +
				"mName='" + mName + '\'' +
				", mType='" + mType + '\'' +
				", mHostv4=" + mHostv4 +
				", mHostv6=" + mHostv6 +
				", mPort=" + mPort +
				'}';
	}

	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BonjourService)) return false;

		BonjourService that = (BonjourService) o;
		return mName.equals(that.getName());

	}

	@Override public int hashCode() {
		int result = mName != null ? mName.hashCode() : 0;
		result = 31 * result + (mType != null ? mType.hashCode() : 0);
		result = 31 * result + (mHostv4 != null ? mHostv4.hashCode() : 0);
		result = 31 * result + (mHostv6 != null ? mHostv6.hashCode() : 0);
		result = 31 * result + mPort;
		return result;
	}
}
