package de.mannodermaus.rxbonjour.util;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.schedulers.Schedulers;

public class TestSchedulers {

    public static <T> CompletableTransformer immediateSchedulers() {
        return new CompletableTransformer() {
            @Override public CompletableSource apply(Completable upstream) {
                return upstream
                        .subscribeOn(Schedulers.trampoline())
                        .observeOn(Schedulers.trampoline());
            }
        };
    }

    public static <T> ObservableTransformer<T, T> backlogSchedulers() {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation());
            }
        };
    }
}
