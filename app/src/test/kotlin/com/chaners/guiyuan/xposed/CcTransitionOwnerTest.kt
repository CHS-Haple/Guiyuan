package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CcTransitionOwnerTest {

    @Test
    fun releaseContinuesAfterOrdinaryFailureAndKeepsFirstCause() {
        val visited = mutableListOf<String>()
        val first = IllegalStateException("reservation")
        val second = IllegalArgumentException("observer")
        val failure =
            runCatching {
                releaseAll(
                    { visited += "reservation"; throw first },
                    { visited += "observer"; throw second },
                    { visited += "overlay" },
                    { visited += "mask" },
                )
            }.exceptionOrNull()

        assertSame(first, failure)
        assertEquals(listOf("reservation", "observer", "overlay", "mask"), visited)
        assertEquals(1, requireNotNull(failure).suppressed.size)
        assertSame(second, failure.suppressed.single())
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
                    { visited += "overlay" },
                    { visited += "mask" },
                )
            }.exceptionOrNull()

        assertEquals("global-unregister", failure?.message)
        assertEquals(
            listOf("global", "pre-draw", "live-global", "live-pre-draw", "overlay", "mask"),
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
