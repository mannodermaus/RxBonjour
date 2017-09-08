package de.mannodermaus.rxbonjour

import io.reactivex.CompletableTransformer
import io.reactivex.schedulers.Schedulers

class BonjourSchedulers private constructor() {
    companion object {
        @JvmStatic
        fun cleanupSchedulers(): CompletableTransformer =
                CompletableTransformer {
                    it.subscribeOn(Schedulers.computation())
                            .observeOn(Schedulers.computation())
                }
    }
}
