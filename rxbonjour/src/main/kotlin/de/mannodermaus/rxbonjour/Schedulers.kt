package de.mannodermaus.rxbonjour

import io.reactivex.CompletableTransformer
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers

class BonjourSchedulers private constructor() {
    companion object {
        @JvmStatic
        fun completableAsync(): CompletableTransformer =
                CompletableTransformer {
                    it.subscribeOn(Schedulers.computation())
                            .observeOn(Schedulers.computation())
                }

        @JvmStatic
        fun <T> observableAsync(): ObservableTransformer<T, T> =
                ObservableTransformer {
                    it.subscribeOn(Schedulers.computation())
                            .observeOn(Schedulers.computation())
                }
    }
}
