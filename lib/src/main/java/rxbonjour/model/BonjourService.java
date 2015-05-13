package rxbonjour.model;

import android.os.Bundle;

import java.net.InetAddress;

/**
 * @author marcel
 */
public class BonjourService {

	private String mName;
	private String mType;
	private InetAddress mHost;
	private int mPort;
	private Bundle mTxtRecords;

	public BonjourService(String name, String type, InetAddress host, int port, Bundle txtRecords) {
		mName = name;
		mType = type;
		mHost = host;
		mPort = port;
		mTxtRecords = txtRecords;
	}

	public String getName() {
		return mName;
	}

	public String getType() {
		return mType;
	}

	public InetAddress getHost() {
		return mHost;
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
				", mHost=" + mHost +
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
		result = 31 * result + (mHost != null ? mHost.hashCode() : 0);
		result = 31 * result + mPort;
		return result;
	}
}
