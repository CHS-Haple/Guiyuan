package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

private const val TEST_SLOT_OVERLAY = "overlay"
private const val TEST_SLOT_MASK = "mask"
private const val TEST_SLOT_RESERVATION = "reservation"
private const val TEST_SLOT_OBSERVER = "observer"

class CcTransitionOwnerTest {

    @Test
    fun releaseContinuesAfterOrdinaryFailureAndKeepsFirstCause() {
        val visited = mutableListOf<String>()
        val first = IllegalStateException(TEST_SLOT_RESERVATION)
        val second = IllegalArgumentException(TEST_SLOT_OBSERVER)
        val failure =
            runCatching {
                releaseAll(
                    { visited += TEST_SLOT_RESERVATION; throw first },
                    { visited += TEST_SLOT_OBSERVER; throw second },
                    { visited += TEST_SLOT_OVERLAY },
                    { visited += TEST_SLOT_MASK },
                )
            }.exceptionOrNull()

        assertSame(first, failure)
        assertEquals(listOf(TEST_SLOT_RESERVATION, TEST_SLOT_OBSERVER, TEST_SLOT_OVERLAY, TEST_SLOT_MASK), visited)
        assertEquals(1, requireNotNull(failure).suppressed.size)
        assertSame(second, failure.suppressed.single())
    }

    @Test
    fun repeatedFailureInstanceDoesNotReplacePrimaryCause() {
        val shared = IllegalStateException("shared-release-failure")
        var laterReleaseRan = false
        val failure =
            runCatching {
                releaseAll(
                    { throw shared },
                    { throw shared },
                    { laterReleaseRan = true },
                )
            }.exceptionOrNull()

        assertSame(shared, failure)
        assertEquals(true, laterReleaseRan)
        assertEquals(0, shared.suppressed.size)
    }

    @Test
    fun nestedListenerFailureStillReleasesOverlayAndMask() {
        val visited = mutableListOf<String>()
        val failure =
            runCatching {
                releaseAll(
                    {
                        releaseAll(
                            { visited += "global"; error("global-unregister") },
                            { visited += "pre-draw" },
                            { visited += "live-global" },
                            { visited += "live-pre-draw" },
                        )
                    },
                    { visited += TEST_SLOT_OVERLAY },
                    { visited += TEST_SLOT_MASK },
                )
            }.exceptionOrNull()

        assertEquals("global-unregister", failure?.message)
        assertEquals(
            listOf("global", "pre-draw", "live-global", "live-pre-draw", TEST_SLOT_OVERLAY, TEST_SLOT_MASK),
            visited,
        )
    }

    @Test
    fun releaseDoesNotSwallowReflectivelyWrappedFatalFailure() {
        val fatal = OutOfMemoryError("transition-release")
        var laterReleaseRan = false
        val failure =
            runCatching {
                releaseAll(
                    { throw InvocationTargetException(fatal) },
                    { laterReleaseRan = true },
                )
            }.exceptionOrNull()

        assertSame(fatal, failure)
        assertFalse(laterReleaseRan)
    }

    @Test
    fun nativeCcOptionalReadOnlyHidesOrdinaryFailure() {
        val fatal = ThreadDeath()
        assertSame(
            fatal,
            runCatching {
                CcTransitionOwner.readOrNull<Unit> { throw InvocationTargetException(fatal) }
            }.exceptionOrNull(),
        )
        val oom = OutOfMemoryError("cc-native-read")
        assertSame(
            oom,
            runCatching { CcTransitionOwner.readOrNull<Unit> { throw oom } }.exceptionOrNull(),
        )
        assertNull(
            CcTransitionOwner.readOrNull<Unit> {
                throw InvocationTargetException(IllegalStateException("native-unavailable"))
            },
        )
    }
}
