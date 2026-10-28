package com.chaners.guiyuan.xposed

// Run sibling cleanup independently; rethrow the first nonfatal failure afterward.
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
