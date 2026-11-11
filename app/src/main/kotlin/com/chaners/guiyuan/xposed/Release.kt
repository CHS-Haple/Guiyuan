package com.chaners.guiyuan.xposed

// Run sibling cleanup independently; rethrow the first nonfatal failure afterward.
@Suppress("TooGenericExceptionCaught")
internal fun releaseAll(vararg actions: () -> Unit) {
    val failures = ArrayList<Throwable>(1)
    actions.forEach { action ->
        try {
            action()
        } catch (error: Throwable) {
            SysUiPresentationRuntime.throwIfFatal(error)
            failures.add(error)
        }
    }
    failures.firstOrNull()?.let { error ->
        failures.drop(1).filter { it !== error }.forEach(error::addSuppressed)
        throw error
    }
}

// Preserve each caller's labelled failure while collecting sibling cleanup errors.
internal fun throwIfCleanupIncomplete(reason: String, failures: List<Throwable>) {
    if (failures.isEmpty()) return
    val error = IllegalStateException(reason, failures.first())
    failures.drop(1).forEach(error::addSuppressed)
    throw error
}
