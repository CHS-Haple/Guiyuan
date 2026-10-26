package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun fatalMutationFailurePreservesOriginalCause() {
        val threadDeath = ThreadDeath()
        assertSame(
            threadDeath,
            runCatching {
                SysUiPresentationOwner.rethrowFatal(InvocationTargetException(threadDeath))
            }.exceptionOrNull(),
        )

        val vmError = OutOfMemoryError("synthetic")
        assertSame(
            vmError,
            runCatching { SysUiPresentationOwner.rethrowFatal(vmError) }.exceptionOrNull(),
        )
        assertNull(
            runCatching {
                SysUiPresentationOwner.rethrowFatal(
                    InvocationTargetException(IllegalStateException("ordinary")),
                )
            }.exceptionOrNull(),
        )
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
