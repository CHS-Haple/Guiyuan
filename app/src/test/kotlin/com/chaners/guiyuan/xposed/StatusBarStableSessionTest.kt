package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class StatusBarStableSessionTest {
    @Test
    fun failedPassiveEvidenceDoesNotBlockIndependentSnapshots() {
        var delivered = 0
        SysUiPresentationRuntime.emitPassiveDiagnostic {
            throw IllegalStateException("anchor-log-unavailable")
        }
        SysUiPresentationRuntime.emitPassiveDiagnostic { delivered++ }
        assertEquals(1, delivered)
    }

    @Test
    fun fatalDiagnosticFailureRetainsOriginalIdentity() {
        val fatal = ThreadDeath()
        assertSame(
            fatal,
            runCatching {
                SysUiPresentationRuntime.emitPassiveDiagnostic { throw fatal }
            }.exceptionOrNull(),
        )

        val reflected = OutOfMemoryError("reflected")
        assertSame(
            reflected,
            runCatching {
                SysUiPresentationRuntime.emitPassiveDiagnostic {
                    throw InvocationTargetException(reflected)
                }
            }.exceptionOrNull(),
        )
        SysUiPresentationRuntime.emitPassiveDiagnostic {
            throw InvocationTargetException(IllegalStateException("ordinary"))
        }
    }
}
