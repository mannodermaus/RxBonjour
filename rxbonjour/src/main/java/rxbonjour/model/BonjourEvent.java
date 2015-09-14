package rxbonjour.model;

/**
 * @author marcel
 */
public class BonjourEvent {

	public enum Type {
		ADDED,
		REMOVED;
	}

	private Type mType;
	private BonjourService mService;

	public BonjourEvent(Type type, BonjourService service) {
		mType = type;
		mService = service;
	}

	public Type getType() {
		return mType;
	}

	public BonjourService getService() {
		return mService;
	}

	@Override public String toString() {
		return "BonjourEvent{" +
				"mType=" + mType +
				", mService=" + mService +
				'}';
	}

	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BonjourEvent)) return false;

		BonjourEvent that = (BonjourEvent) o;

		if (mType != that.mType) return false;
		return !(mService != null ? !mService.equals(that.mService) : that.mService != null);

	}

	@Override public int hashCode() {
		int result = mType != null ? mType.hashCode() : 0;
		result = 31 * result + (mService != null ? mService.hashCode() : 0);
		return result;
	}
}
