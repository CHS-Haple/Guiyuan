package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.system.rethrowUserOperationFatal

/**
 * Converts ordinary user-operation failures into feedback without swallowing coroutine
 * cancellation or JVM fatal errors. Used only for explicit UI-triggered I/O actions.
 */
internal suspend fun <T> runCatchingUiOperation(block: suspend () -> T): Result<T> =
    runCatching { block() }.onFailure(::rethrowUserOperationFatal)
