package com.p2lem8dev.testcameraapp.common.livenavigation

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData

class LiveNavigationImpl<T> : LiveNavigation<T> {
    private val liveData = MutableLiveData<SingleLiveEvent<(T) -> Unit>>()
    private val events = mutableListOf<(T) -> Unit>()

    @MainThread
    override fun observe(owner: LifecycleOwner, listener: ((T) -> Unit) -> Unit) {
        liveData.observe(owner) { it?.get(listener) }
    }

    fun post(method: (T) -> Unit) {
        events.add(method)
        if (events.size == 1) {
            liveData.postValue(SingleLiveEvent { onProceedEvent(it) })
        }
    }

    fun call(method: T.() -> Unit) = post(method)

    private fun onProceedEvent(worker: ((T) -> Unit) -> Unit) {
        while (events.isNotEmpty()) worker(events.removeAt(0))
    }

    private class SingleLiveEvent<V>(private val listener: ((V) -> Unit) -> Unit) {
        private var proceed = false
        operator fun get(handler: (V) -> Unit): SingleLiveEvent<V> {
            if (!proceed) {
                proceed = true
                listener(handler)
            }
            return this
        }
    }
}
