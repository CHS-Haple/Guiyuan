package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class SysUiPresentationOwnerTest {
    @Test
    fun diagnosticFailureCannotInterruptLayoutCallbacks() {
        val nativeResult = Any()
        val observed = run {
            SysUiPresentationOwner.emitDiagnostic {
                throw IllegalStateException("diagnostic sink failed")
            }
            nativeResult
        }
        assertSame(nativeResult, observed)
    }

    @Test
    fun fatalDiagnosticErrorsStillPropagate() {
        val fatal = ThreadDeath()
        val actual =
            runCatching {
                SysUiPresentationOwner.emitDiagnostic { throw fatal }
            }.exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun controlCenterPresentationFailureIsNoOpWithoutActiveSession() {
        assertFalse(
            SysUiPresentationOwner.failCcPresentation(
                "unit-test-no-session",
            ),
        )
    }
}
