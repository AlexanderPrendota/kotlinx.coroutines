/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmMultifileClass
@file:JvmName("BuildersKt")

package kotlinx.coroutines.experimental

import kotlinx.atomicfu.*
import kotlinx.coroutines.experimental.internal.*
import kotlinx.coroutines.experimental.intrinsics.*
import kotlinx.coroutines.experimental.selects.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

// --------------- launch ---------------

/**
 * Launches new coroutine without blocking current thread and returns a reference to the coroutine as a [Job].
 * The coroutine is cancelled when the resulting job is [cancelled][Job.cancel].
 *
 * Coroutine context is inherited from a [CoroutineScope], additional context elements can be specified with [context] argument.
 * If the context does not have any dispatcher nor any other [ContinuationInterceptor], then [Dispatchers.Default] is used.
 * The parent job is inherited from a [CoroutineScope] as well, but it can also be overridden
 * with corresponding [coroutineContext] element.
 *
 * By default, the coroutine is immediately scheduled for execution.
 * Other start options can be specified via `start` parameter. See [CoroutineStart] for details.
 * An optional [start] parameter can be set to [CoroutineStart.LAZY] to start coroutine _lazily_. In this case,
 * the coroutine [Job] is created in _new_ state. It can be explicitly started with [start][Job.start] function
 * and will be started implicitly on the first invocation of [join][Job.join].
 *
 * Uncaught exceptions in this coroutine cancel parent job in the context by default
 * (unless [CoroutineExceptionHandler] is explicitly specified), which means that when `launch` is used with
 * the context of another coroutine, then any uncaught exception leads to the cancellation of parent coroutine.
 *
 * See [newCoroutineContext] for a description of debugging facilities that are available for newly created coroutine.
 *
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block the coroutine code which will be invoked in the context of the provided scope.
 **/
public fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val newContext = newCoroutineContext(context)
    val coroutine = if (start.isLazy)
        LazyStandaloneCoroutine(newContext, block) else
        StandaloneCoroutine(newContext, active = true)
    coroutine.start(start, coroutine, block)
    return coroutine
}

/**
 * @suppress **Deprecated**: onCompletion parameter is deprecated.
 */
@Deprecated("onCompletion parameter is deprecated")
public fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onCompletion: CompletionHandler? = null,
    block: suspend CoroutineScope.() -> Unit
): Job =
    launch(context, start, block).also { if (onCompletion != null) it.invokeOnCompletion(onCompletion) }

/**
 * Launches new coroutine without blocking current thread and returns a reference to the coroutine as a [Job].
 * @suppress **Deprecated** Use [CoroutineScope.launch] instead.
 */
@Deprecated(
    message = "Standalone coroutine builders are deprecated, use extensions on CoroutineScope instead",
    replaceWith = ReplaceWith("GlobalScope.launch(context, start, onCompletion, block)", imports = ["kotlinx.coroutines.experimental.*"])
)
public fun launch(
    context: CoroutineContext = Dispatchers.Default,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onCompletion: CompletionHandler? = null,
    block: suspend CoroutineScope.() -> Unit
): Job =
    GlobalScope.launch(context, start, onCompletion, block)

/**
 * Launches new coroutine without blocking current thread and returns a reference to the coroutine as a [Job].
 * @suppress **Deprecated** Use [CoroutineScope.launch] instead.
 */
@Deprecated(
    message = "Standalone coroutine builders are deprecated, use extensions on CoroutineScope instead",
    replaceWith = ReplaceWith("GlobalScope.launch(context + parent, start, onCompletion, block)", imports = ["kotlinx.coroutines.experimental.*"])
)
public fun launch(
    context: CoroutineContext = Dispatchers.Default,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    parent: Job? = null, // nullable for binary compatibility
    onCompletion: CompletionHandler? = null,
    block: suspend CoroutineScope.() -> Unit
): Job =
    GlobalScope.launch(context + (parent ?: EmptyCoroutineContext), start, onCompletion, block)

/** @suppress **Deprecated**: Binary compatibility */
@Deprecated(message = "Binary compatibility", level = DeprecationLevel.HIDDEN)
public fun launch(
    context: CoroutineContext = Dispatchers.Default,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    parent: Job? = null,
    block: suspend CoroutineScope.() -> Unit
): Job =
    GlobalScope.launch(context + (parent ?: EmptyCoroutineContext), start, block = block)

/** @suppress **Deprecated**: Binary compatibility */
@Deprecated(message = "Binary compatibility", level = DeprecationLevel.HIDDEN)
public fun launch(
    context: CoroutineContext = Dispatchers.Default,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job =
    GlobalScope.launch(context, start, block = block)

// --------------- async ---------------

/**
 * Creates new coroutine and returns its future result as an implementation of [Deferred].
 * The running coroutine is cancelled when the resulting deferred is [cancelled][Job.cancel].
 *
 * Coroutine context is inherited from a [CoroutineScope], additional context elements can be specified with [context] argument.
 * If the context does not have any dispatcher nor any other [ContinuationInterceptor], then [Dispatchers.Default] is used.
 * The parent job is inherited from a [CoroutineScope] as well, but it can also be overridden
 * with corresponding [coroutineContext] element.
 *
 * By default, the coroutine is immediately scheduled for execution.
 * Other options can be specified via `start` parameter. See [CoroutineStart] for details.
 * An optional [start] parameter can be set to [CoroutineStart.LAZY] to start coroutine _lazily_. In this case,,
 * the resulting [Deferred] is created in _new_ state. It can be explicitly started with [start][Job.start]
 * function and will be started implicitly on the first invocation of [join][Job.join], [await][Deferred.await] or [awaitAll].
 *
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block the coroutine code.
 */
public fun <T> CoroutineScope.async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    val newContext = newCoroutineContext(context)
    val coroutine = if (start.isLazy)
        LazyDeferredCoroutine(newContext, block) else
        DeferredCoroutine<T>(newContext, active = true)
    coroutine.start(start, coroutine, block)
    return coroutine
}

@Suppress("UNCHECKED_CAST")
private open class DeferredCoroutine<T>(
    parentContext: CoroutineContext,
    active: Boolean
) : AbstractCoroutine<T>(parentContext, active), Deferred<T>, SelectClause1<T> {
    override fun getCompleted(): T = getCompletedInternal() as T
    override suspend fun await(): T = awaitInternal() as T
    override val onAwait: SelectClause1<T> get() = this
    override fun <R> registerSelectClause1(select: SelectInstance<R>, block: suspend (T) -> R) =
        registerSelectClause1Internal(select, block)
}

private class LazyDeferredCoroutine<T>(
    parentContext: CoroutineContext,
    private val block: suspend CoroutineScope.() -> T
) : DeferredCoroutine<T>(parentContext, active = false) {
    override fun onStart() {
        block.startCoroutineCancellable(this, this)
    }
}

// --------------- withContext ---------------

/**
 * @suppress **Deprecated**: Use `start = CoroutineStart.XXX` parameter
 */
@Deprecated(message = "Use `start = CoroutineStart.XXX` parameter",
    replaceWith = ReplaceWith("launch(context, if (start) CoroutineStart.DEFAULT else CoroutineStart.LAZY, block)"))
public fun launch(context: CoroutineContext, start: Boolean, block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(context, if (start) CoroutineStart.DEFAULT else CoroutineStart.LAZY, block = block)

/**
 * Calls the specified suspending block with a given coroutine context, suspends until it completes, and returns
 * the result.
 *
 * This function immediately applies dispatcher from the new context, shifting execution of the block into the
 * different thread inside the block, and back when it completes.
 * The specified [context] is added onto the current coroutine context for the execution of the block.
 */
public suspend fun <T> withContext(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T = suspendCoroutineUninterceptedOrReturn sc@ { uCont ->
    // compute new context
    val oldContext = uCont.context
    val newContext = oldContext + context
    // FAST PATH #1 -- new context is the same as the old one
    if (newContext === oldContext) {
        val coroutine = ScopeCoroutine(newContext, uCont) // MODE_DIRECT
        return@sc coroutine.startUndispatchedOrReturn(coroutine, block)
    }
    // FAST PATH #2 -- the new dispatcher is the same as the old one (something else changed)
    // `equals` is used by design (see equals implementation is wrapper context like ExecutorCoroutineDispatcher)
    if (newContext[ContinuationInterceptor] == oldContext[ContinuationInterceptor]) {
        val coroutine = UndispatchedCoroutine(newContext, uCont) // MODE_UNDISPATCHED
        // There are changes in the context, so this thread needs to be updated
        withCoroutineContext(newContext) {
            return@sc coroutine.startUndispatchedOrReturn(coroutine, block)
        }
    }
    // SLOW PATH -- use new dispatcher
    val coroutine = DispatchedCoroutine(newContext, uCont) // MODE_DISPATCHED
    coroutine.initParentJob()
    block.startCoroutineCancellable(coroutine, coroutine)
    coroutine.getResult()
}

/**
 * @suppress **Deprecated**: start parameter is deprecated, no replacement.
 */
@Deprecated("start parameter is deprecated, no replacement")
public suspend fun <T> withContext(
    context: CoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): T =
    withContext(context, block)

/** @suppress **Deprecated**: Binary compatibility */
@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
@JvmName("withContext")
public suspend fun <T> withContext0(
    context: CoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend () -> T
): T =
    withContext(context) { block() }

/** @suppress **Deprecated**: Renamed to [withContext]. */
@Deprecated(message = "Renamed to `withContext`", level=DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("withContext(context, start, block)"))
public suspend fun <T> run(
    context: CoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend () -> T
): T =
    withContext(context) { block() }

/** @suppress **Deprecated** */
@Deprecated(message = "It is here for binary compatibility only", level=DeprecationLevel.HIDDEN)
public suspend fun <T> run(context: CoroutineContext, block: suspend () -> T): T =
    withContext(context) { block() }

// --------------- implementation ---------------

private open class StandaloneCoroutine(
    parentContext: CoroutineContext,
    active: Boolean
) : AbstractCoroutine<Unit>(parentContext, active) {
    override val cancelsParent: Boolean get() = true
    override fun handleJobException(exception: Throwable) = handleExceptionViaHandler(parentContext, exception)
}

private class LazyStandaloneCoroutine(
    parentContext: CoroutineContext,
    private val block: suspend CoroutineScope.() -> Unit
) : StandaloneCoroutine(parentContext, active = false) {
    override fun onStart() {
        block.startCoroutineCancellable(this, this)
    }
}

// Used by withContext when context changes, but dispatcher stays the same
private class UndispatchedCoroutine<in T>(
    context: CoroutineContext,
    uCont: Continuation<T>
) : ScopeCoroutine<T>(context, uCont) {
    override val defaultResumeMode: Int get() = MODE_UNDISPATCHED
}

private const val UNDECIDED = 0
private const val SUSPENDED = 1
private const val RESUMED = 2

// Used by withContext when context dispatcher changes
private class DispatchedCoroutine<in T>(
    context: CoroutineContext,
    uCont: Continuation<T>
) : ScopeCoroutine<T>(context, uCont) {
    override val defaultResumeMode: Int get() = MODE_CANCELLABLE

    // this is copy-and-paste of a decision state machine inside AbstractionContinuation
    // todo: we may some-how abstract it via inline class
    private val _decision = atomic(UNDECIDED)

    private fun trySuspend(): Boolean {
        _decision.loop { decision ->
            when (decision) {
                UNDECIDED -> if (this._decision.compareAndSet(UNDECIDED, SUSPENDED)) return true
                RESUMED -> return false
                else -> error("Already suspended")
            }
        }
    }

    private fun tryResume(): Boolean {
        _decision.loop { decision ->
            when (decision) {
                UNDECIDED -> if (this._decision.compareAndSet(UNDECIDED, RESUMED)) return true
                SUSPENDED -> return false
                else -> error("Already resumed")
            }
        }
    }

    override fun onCompletionInternal(state: Any?, mode: Int, suppressed: Boolean) {
        if (tryResume()) return // completed before getResult invocation -- bail out
        // otherwise, getResult has already commenced, i.e. completed later or in other thread
        super.onCompletionInternal(state, mode, suppressed)
    }

    fun getResult(): Any? {
        if (trySuspend()) return COROUTINE_SUSPENDED
        // otherwise, onCompletionInternal was already invoked & invoked tryResume, and the result is in the state
        val state = this.state
        if (state is CompletedExceptionally) throw state.cause
        @Suppress("UNCHECKED_CAST")
        return state as T
    }
}
