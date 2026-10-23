package com.chaners.guiyuan.xposed

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainThreadRetireTest {
    @Test
    fun rejectedPostLeavesOldGenerationUntouched() {
        var called = false
        val retired =
            MainThreadRetire(
                post = { false },
                cancel = {},
            ).run { called = true }

        assertFalse(retired)
        assertFalse(called)
    }

    @Test
    fun dispatchReportsFailureAfterQueueingCannotRetireLater() {
        var queued: Runnable? = null
        var cleaned = false
        var canceled = false
        val retired =
            MainThreadRetire(
                post = { queued = it; false },
                cancel = { canceled = true },
            ).run { cleaned = true }

        assertFalse(retired)
        assertTrue(canceled)
        queued!!.run()
        assertFalse(cleaned)
    }

    @Test
    fun alreadyCompletedTaskWinsOverFalsePostResult() {
        var cleaned = false
        val accepted =
            MainThreadRetire(
                post = { it.run(); false },
                cancel = {},
            ).run { cleaned = true }

        assertTrue(accepted)
        assertTrue(cleaned)
    }

    @Test
    fun alreadyCompletedTaskWinsOverPostException() {
        var cleaned = false
        val accepted =
            MainThreadRetire(
                post = { it.run(); error("post reported failure") },
                cancel = {},
            ).run { cleaned = true }

        assertTrue(accepted)
        assertTrue(cleaned)
    }

    @Test
    fun cancelledQueueDoesNotRunAfterRefusal() {
        var queued: Runnable? = null
        val changed = AtomicInteger()
        val retired =
            MainThreadRetire(
                post = { queued = it; true },
                cancel = {},
                waitMs = 10,
            ).run { changed.incrementAndGet() }

        assertFalse(retired)
        queued!!.run()
        assertEquals(0, changed.get())
    }

    @Test
    fun completedRetirementIsConfirmedBeforeAccepting() {
        val retired =
            MainThreadRetire(
                post = { it.run(); true },
                cancel = {},
            ).run {}

        assertTrue(retired)
    }

    @Test
    fun startedRetirementCannotBeCancelledAfterDeadline() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val changes = AtomicInteger()
        var accepted = false

        val caller =
            Thread {
                accepted =
                    MainThreadRetire(
                        post = { task -> Thread(task).start(); true },
                        cancel = {},
                        waitMs = 10,
                    ).run {
                        started.countDown()
                        check(release.await(2, TimeUnit.SECONDS))
                        changes.incrementAndGet()
                    }
                finished.countDown()
            }
        caller.start()
        try {
            assertTrue(started.await(2, TimeUnit.SECONDS))
            assertFalse(finished.await(50, TimeUnit.MILLISECONDS))
        } finally {
            release.countDown()
        }
        assertTrue(finished.await(2, TimeUnit.SECONDS))
        caller.join(2_000)
        assertTrue(accepted)
        assertEquals(1, changes.get())
    }

    @Test
    fun interruptedWhileQueuedCancelsRetirementWithoutLateViewMutation() {
        val posted = CountDownLatch(1)
        val pending = AtomicReference<Runnable>()
        val cancelled = AtomicInteger()
        val changes = AtomicInteger()
        val accepted = AtomicBoolean(true)
        val interrupted = AtomicBoolean()

        val caller = Thread {
            accepted.set(
                MainThreadRetire(
                    post = { task ->
                        pending.set(task)
                        posted.countDown()
                        true
                    },
                    cancel = { cancelled.incrementAndGet() },
                ).run { changes.incrementAndGet() },
            )
            interrupted.set(Thread.currentThread().isInterrupted)
        }
        caller.start()
        try {
            assertTrue(posted.await(2, TimeUnit.SECONDS))
            caller.interrupt()
            caller.join(2_000)
            assertFalse(caller.isAlive)
            assertFalse(accepted.get())
            assertTrue(interrupted.get())
            assertEquals(1, cancelled.get())
            pending.get().run()
            assertEquals(0, changes.get())
        } finally {
            caller.interrupt()
            caller.join(2_000)
        }
    }

    @Test
    fun interruptedAfterRetirementStartsStillWaitsForCleanup() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val changes = AtomicInteger()
        val cancellations = AtomicInteger()
        val accepted = AtomicBoolean()
        val interrupted = AtomicBoolean()
        val caller = Thread {
            accepted.set(
                MainThreadRetire(
                    post = { task -> Thread(task).start(); true },
                    cancel = { cancellations.incrementAndGet() },
                ).run {
                    started.countDown()
                    check(release.await(2, TimeUnit.SECONDS))
                    changes.incrementAndGet()
                },
            )
            interrupted.set(Thread.currentThread().isInterrupted)
            finished.countDown()
        }
        caller.start()
        try {
            assertTrue(started.await(2, TimeUnit.SECONDS))
            caller.interrupt()
            assertFalse(finished.await(40, TimeUnit.MILLISECONDS))
        } finally {
            release.countDown()
        }
        assertTrue(finished.await(2, TimeUnit.SECONDS))
        caller.join(2_000)
        assertTrue(accepted.get())
        assertTrue(interrupted.get())
        assertEquals(0, cancellations.get())
        assertEquals(1, changes.get())
    }

    @Test
    fun cleanupFailureDoesNotReportSuccess() {
        try {
            MainThreadRetire(
                post = { it.run(); true },
                cancel = {},
            ).run { error("cleanup failed") }
            error("expected a retirement failure")
        } catch (error: IllegalStateException) {
            assertEquals("old-generation-retirement-failed", error.message)
        }
    }
}
