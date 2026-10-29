package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class StatusBarStableSessionTest {
    @Test
    fun failedAnchorEvidenceDoesNotBlockIndependentMetrics() {
        var delivered = 0
        StatusBarStableSession.emitDiagnostic {
            throw IllegalStateException("anchor-log-unavailable")
        }
        StatusBarStableSession.emitDiagnostic { delivered++ }
        assertEquals(1, delivered)
    }

    @Test
    fun fatalDiagnosticFailureRetainsOriginalIdentity() {
        val fatal = ThreadDeath()
        assertSame(
            fatal,
            runCatching {
                StatusBarStableSession.emitDiagnostic { throw fatal }
            }.exceptionOrNull(),
        )

        val reflected = OutOfMemoryError("reflected")
        assertSame(
            reflected,
            runCatching {
                StatusBarStableSession.emitDiagnostic {
                    throw InvocationTargetException(reflected)
                }
            }.exceptionOrNull(),
        )
        StatusBarStableSession.emitDiagnostic {
            throw InvocationTargetException(IllegalStateException("ordinary"))
        }
    }
}
