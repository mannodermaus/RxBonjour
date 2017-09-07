package de.mannodermaus.rxbonjour.util;

import rx.Observable;
import rx.schedulers.Schedulers;

public class TestSchedulers {

    public static <T> Observable.Transformer<T, T> immediateSchedulers() {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> obs) {
                return obs
                        .subscribeOn(Schedulers.immediate())
                        .observeOn(Schedulers.immediate());
            }
        };
    }

    public static <T> Observable.Transformer<T, T> backlogSchedulers() {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> obs) {
                return obs
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation());
            }
        };
    }
}
