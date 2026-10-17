package com.chaners.guiyuan.xposed

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

// Only a queued cleanup may be cancelled; one already mutating Views must finish.
internal class MainThreadRetire(
    private val post: (Runnable) -> Boolean,
    private val cancel: (Runnable) -> Unit,
    private val waitMs: Long = 5_000L,
) {
    fun run(cleanup: () -> Unit): Boolean {
        val phase = AtomicInteger(0) // queued / running / cancelled
        val done = CountDownLatch(1)
        var failure: Throwable? = null
        val task =
            Runnable {
                if (!phase.compareAndSet(0, 1)) return@Runnable
                try {
                    cleanup()
                } catch (error: Throwable) {
                    failure = error
                } finally {
                    done.countDown()
                }
            }
        if (!post(task)) return false

        var interrupted = false
        try {
            val completed =
                try {
                    done.await(waitMs, TimeUnit.MILLISECONDS)
                } catch (_: InterruptedException) {
                    interrupted = true
                    false
                }
            if (!completed && phase.compareAndSet(0, 2)) {
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
