package de.mannodermaus.rxbonjour.internal;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.schedulers.Schedulers;

public class BonjourSchedulers {

    public static <T> ObservableTransformer<T, T> backlogSchedulers() {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io());
            }
        };
    }

    public static <T> CompletableTransformer cleanupSchedulers() {
        return new CompletableTransformer() {
            @Override public CompletableSource apply(Completable upstream) {
                return upstream
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation());
            }
        };
    }
}
