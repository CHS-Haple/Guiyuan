package com.chaners.guiyuan.xposed

import java.util.concurrent.atomic.AtomicReference

// One runtime owner for cross-scene failure; never persists or rewrites user settings.
internal class RuntimeFailNativeGate {
    private enum class State {
        ACTIVE,
        SUSPENDED,
        RESET_ARMED,
        RESTART_REQUIRED,
    }

    private val state = AtomicReference(State.ACTIVE)

    val suspended: Boolean get() = state.get() != State.ACTIVE
    val restartRequired: Boolean get() = state.get() == State.RESTART_REQUIRED

    fun beginGlobalFailure(): Boolean {
        while (true) {
            val previous = state.get()
            if (previous == State.SUSPENDED || previous == State.RESTART_REQUIRED) return false
            if (state.compareAndSet(previous, State.SUSPENDED)) return true
        }
    }

    fun globalCleanupCompleted(complete: Boolean) {
        if (!complete) state.set(State.RESTART_REQUIRED)
    }

    // Only an explicit *complete* manual off arms a fresh takeover attempt.
    fun manualDisableCompleted(cleanupComplete: Boolean) {
        if (cleanupComplete) {
            state.compareAndSet(State.SUSPENDED, State.RESET_ARMED)
        } else if (suspended) {
            state.set(State.RESTART_REQUIRED)
        }
    }

    fun allowManualEnable(): Boolean =
        when (state.get()) {
            State.ACTIVE -> true
            State.RESET_ARMED -> state.compareAndSet(State.RESET_ARMED, State.ACTIVE)
            State.SUSPENDED, State.RESTART_REQUIRED -> false
        }
}
