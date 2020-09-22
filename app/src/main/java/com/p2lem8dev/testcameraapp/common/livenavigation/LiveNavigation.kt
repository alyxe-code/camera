package com.p2lem8dev.testcameraapp.common.livenavigation

import androidx.lifecycle.LifecycleOwner

interface LiveNavigation<T> {
    fun observe(owner: LifecycleOwner, listener: ((T) -> Unit) -> Unit)
    fun observe(owner: LifecycleOwner, executor: T): Unit = observe(owner) { it(executor) }
}
