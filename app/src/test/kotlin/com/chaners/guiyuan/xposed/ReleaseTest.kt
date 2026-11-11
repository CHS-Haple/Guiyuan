package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseTest {
    @Test
    fun emptyCleanupFailuresDoNotThrow() {
        throwIfCleanupIncomplete("no-failure", emptyList())
    }

    @Test
    fun labelledCleanupFailurePreservesCauseAndSuppressedOrder() {
        val first = IllegalStateException("first")
        val second = IllegalArgumentException("second")
        val error = runCatching {
            throwIfCleanupIncomplete(
                "cleanup-failed",
                listOf(first, second, first),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("cleanup-failed", error?.message)
        assertSame(first, error?.cause)
        assertEquals(listOf(second, first), error?.suppressed?.toList())
    }
}
