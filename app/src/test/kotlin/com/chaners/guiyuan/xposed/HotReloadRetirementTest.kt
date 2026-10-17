package com.chaners.guiyuan.xposed

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotReloadRetirementTest {
    @Test
    fun rejectedDispatchDoesNotRetire() {
        var cleaned = false
        val result =
            HotReloadRetirement.run(
                post = { false },
                retire = { cleaned = true },
                queueWaitMs = 1L,
            )
        assertEquals(HotReloadRetirement.Result.Unavailable, result)
        assertFalse(cleaned)
    }

    @Test
    fun lateDispatchAfterRefusalCannotRetire() {
        var queued: Runnable? = null
        var cleaned = false
        val result =
            HotReloadRetirement.run(
                post = { task -> queued = task; true },
                retire = { cleaned = true },
                queueWaitMs = 1L,
            )
        assertEquals(HotReloadRetirement.Result.Unavailable, result)
        queued!!.run()
        assertFalse(cleaned)
    }

    @Test
    fun postingExceptionCannotLeaveLateCleanup() {
        var queued: Runnable? = null
        var cleaned = false
        val result =
            HotReloadRetirement.run(
                post = { task ->
                    queued = task
                    throw IllegalStateException("unavailable dispatcher")
                },
                retire = { cleaned = true },
                queueWaitMs = 1L,
            )
        assertEquals(HotReloadRetirement.Result.Unavailable, result)
        queued!!.run()
        assertFalse(cleaned)
    }

    @Test
    fun completedRetirementIsAcknowledged() {
        var cleaned = false
        val result =
            HotReloadRetirement.run(
                post = { task -> task.run(); true },
                retire = { cleaned = true },
                queueWaitMs = 1L,
            )
        assertEquals(HotReloadRetirement.Result.Complete, result)
        assertTrue(cleaned)
    }

    @Test
    fun cleanupFailureIsNotReportedAsSuccess() {
        val error = IllegalStateException("teardown failed")
        val result =
            HotReloadRetirement.run(
                post = { task -> task.run(); true },
                retire = { throw error },
                queueWaitMs = 1L,
            )
        assertEquals(HotReloadRetirement.Result.Failed(error), result)
    }

    @Test
    fun onceStartedRetirementMustCompleteBeforeAccepting() {
        val workerStarted = CountDownLatch(1)
        val releaseCleanup = CountDownLatch(1)
        val result = AtomicReference<HotReloadRetirement.Result>()
        val worker = Thread {
            result.set(
                HotReloadRetirement.run(
                    post = { task -> Thread(task).start(); true },
                    retire = {
                        workerStarted.countDown()
                        releaseCleanup.await()
                    },
                    queueWaitMs = 2_000L,
                ),
            )
        }
        worker.start()
        try {
            assertTrue(workerStarted.await(1, java.util.concurrent.TimeUnit.SECONDS))
            assertTrue(worker.isAlive)
        } finally {
            releaseCleanup.countDown()
            worker.join(2_000L)
        }
        assertFalse(worker.isAlive)
        assertEquals(HotReloadRetirement.Result.Complete, result.get())
    }
}
