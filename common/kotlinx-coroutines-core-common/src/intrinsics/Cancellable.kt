/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.intrinsics

import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

/**
 * Use this function to start coroutine in a cancellable way, so that it can be cancelled
 * while waiting to be dispatched.
 */
public fun <T> (suspend () -> T).startCoroutineCancellable(completion: Continuation<T>) =
    isolateCoroutine(completion) { createCoroutineUnintercepted(it) }.resumeCancellable(Unit)

/**
 * Use this function to start coroutine in a cancellable way, so that it can be cancelled
 * while waiting to be dispatched.
 */
public fun <R, T> (suspend (R) -> T).startCoroutineCancellable(receiver: R, completion: Continuation<T>) =
    isolateCoroutine(completion) { createCoroutineUnintercepted(receiver, it) }.resumeCancellable(Unit)
