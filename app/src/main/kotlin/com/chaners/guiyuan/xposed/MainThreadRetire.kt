package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

// Only a queued cleanup may be cancelled; one already mutating Views must finish.
internal class MainThreadRetire(
    private val post: (Runnable) -> Boolean,
    private val cancel: (Runnable) -> Unit,
    private val waitMs: Long = 5_000L,
) {
    private enum class Phase { Queued, Running, Cancelled }

    private fun fatalCause(error: Throwable): Throwable? {
        val cause = (error as? InvocationTargetException)?.targetException ?: error
        return cause.takeIf { it is VirtualMachineError || it is ThreadDeath }
    }

    fun run(cleanup: () -> Unit): Boolean {
        val phase = AtomicReference(Phase.Queued)
        val done = CountDownLatch(1)
        var failure: Throwable? = null
        val task =
            Runnable {
                if (!phase.compareAndSet(Phase.Queued, Phase.Running)) return@Runnable
                try {
                    cleanup()
                } catch (error: Throwable) {
                    failure = error
                } finally {
                    done.countDown()
                }
            }
        var dispatchFatal: Throwable? = null
        val posted =
            try {
                post(task)
            } catch (error: Throwable) {
                dispatchFatal = fatalCause(error)
                false
            }
        fun cancelQueued(): Boolean {
            if (!phase.compareAndSet(Phase.Queued, Phase.Cancelled)) return false
            try {
                cancel(task)
            } finally {
                // Fatal dispatch must not leave an old View mutation queued.
                dispatchFatal?.let { throw it }
            }
            return true
        }
        if (!posted && cancelQueued()) return false

        var interrupted = false
        try {
            val completed =
                try {
                    posted && done.await(waitMs, TimeUnit.MILLISECONDS)
                } catch (_: InterruptedException) {
                    interrupted = true
                    false
                }
            if (!completed && cancelQueued()) return false

            // Once the main thread has begun retiring Views, rejection cannot race
            // a late mutation of the old generation. Wait for that owner to finish.
            while (true) {
                try {
                    done.await()
                    break
                } catch (_: InterruptedException) {
                    interrupted = true
                }
            }
            dispatchFatal?.let { throw it }
            failure?.let { error ->
                fatalCause(error)?.let { throw it }
                throw IllegalStateException("old-generation-retirement-failed", error)
            }
            return true
        } finally {
            if (interrupted) Thread.currentThread().interrupt()
        }
    }
}
