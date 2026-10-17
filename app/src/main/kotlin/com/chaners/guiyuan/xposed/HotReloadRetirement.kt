package com.chaners.guiyuan.xposed

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

// A rejected reload must never leave a queued old-generation teardown behind.
internal object HotReloadRetirement {
    sealed interface Result {
        data object Complete : Result
        data object Unavailable : Result
        data class Failed(val cause: Throwable) : Result
    }

    private enum class Phase { QUEUED, RUNNING, FINISHED, CANCELLED }

    fun run(
        post: (Runnable) -> Boolean,
        retire: () -> Unit,
        queueWaitMs: Long = 1_000L,
    ): Result {
        val phase = AtomicReference(Phase.QUEUED)
        val started = CountDownLatch(1)
        val done = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>(null)
        val task = Runnable {
            if (!phase.compareAndSet(Phase.QUEUED, Phase.RUNNING)) return@Runnable
            started.countDown()
            try {
                retire()
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                phase.set(Phase.FINISHED)
                done.countDown()
            }
        }

        val posted =
            try {
                post(task)
            } catch (_: RuntimeException) {
                false
            }
        if (!posted) {
            phase.compareAndSet(Phase.QUEUED, Phase.CANCELLED)
            return Result.Unavailable
        }

        var interrupted = false
        val startedInTime =
            try {
                started.await(queueWaitMs, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                interrupted = true
                false
            }
        if (!startedInTime && phase.compareAndSet(Phase.QUEUED, Phase.CANCELLED)) {
            if (interrupted) Thread.currentThread().interrupt()
            return Result.Unavailable
        }

        // Once cleanup starts, refusing reload would let it finish after rejection.
        while (true) {
            try {
                done.await()
                break
            } catch (_: InterruptedException) {
                interrupted = true
            }
        }
        if (interrupted) Thread.currentThread().interrupt()
        return failure.get()?.let(Result::Failed) ?: Result.Complete
    }
}
