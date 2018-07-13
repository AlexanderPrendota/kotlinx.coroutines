/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// This file was automatically generated from coroutines-guide.md by Knit tool. Do not edit.
package kotlinx.coroutines.guide.context05

import kotlinx.coroutines.*
import kotlin.coroutines.*

fun main(args: Array<String>) = runBlocking<Unit> {
    println("My job is ${coroutineContext[Job]}")
}
