package rxbonjour.model;

import android.os.Bundle;

import java.net.InetAddress;
import java.util.Map;

/**
 * @author marcel
 */
public class BonjourService {

	/** IP versions */
	public enum IPv {
		V4, V6
	}

	private String mName;
	private String mType;
	private Map<IPv, InetAddress> mHostMap;
	private int mPort;
	private Bundle mTxtRecords;

	public BonjourService(String name, String type, Map<IPv, InetAddress> hostMap, int port, Bundle txtRecords) {
		mName = name;
		mType = type;
		mHostMap = hostMap;
		mPort = port;
		mTxtRecords = txtRecords;
	}

	public String getName() {
		return mName;
	}

	public String getType() {
		return mType;
	}

	public Map<IPv, InetAddress> getHostMap() {
		return mHostMap;
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
				", mHostMap=" + mHostMap.toString() +
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
		result = 31 * result + (mHostMap != null ? mHostMap.hashCode() : 0);
		result = 31 * result + mPort;
		return result;
	}
}
