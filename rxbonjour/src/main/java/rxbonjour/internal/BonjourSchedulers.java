package rxbonjour.internal;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BonjourSchedulers {

	public static <T> Observable.Transformer<T, T> backlogSchedulers() {
		return new Observable.Transformer<T, T>() {
			@Override public Observable<T> call(Observable<T> obs) {
				return obs
						.subscribeOn(Schedulers.io())
						.observeOn(Schedulers.io());
			}
		};
	}

	public static <T> Observable.Transformer<T, T> cleanupSchedulers() {
		return new Observable.Transformer<T, T>() {
			@Override public Observable<T> call(Observable<T> obs) {
				return obs
						.subscribeOn(Schedulers.computation())
						.observeOn(Schedulers.computation());
			}
		};
	}

	public static <T> Observable.Transformer<T, T> startSchedulers() {
		return new Observable.Transformer<T, T>() {
			@Override public Observable<T> call(Observable<T> obs) {
				return obs
						.subscribeOn(Schedulers.io())
						.observeOn(AndroidSchedulers.mainThread());
			}
		};
	}
}
