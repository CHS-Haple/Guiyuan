package com.chaners.guiyuan.xposed

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
        val posted = runCatching { post(task) }.getOrDefault(false)
        if (!posted && phase.compareAndSet(Phase.Queued, Phase.Cancelled)) {
            cancel(task)
            return false
        }

        var interrupted = false
        try {
            val completed =
                try {
                    posted && done.await(waitMs, TimeUnit.MILLISECONDS)
                } catch (_: InterruptedException) {
                    interrupted = true
                    false
                }
            if (!completed && phase.compareAndSet(Phase.Queued, Phase.Cancelled)) {
                cancel(task)
                return false
            }

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
            failure?.let { throw IllegalStateException("old-generation-retirement-failed", it) }
            return true
        } finally {
            if (interrupted) Thread.currentThread().interrupt()
        }
    }
}
