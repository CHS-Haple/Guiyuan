package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SysUiPresentationRuntimeTest {
    @Test
    fun optionalNativeReadDistinguishesAbsenceFromSuccessfulLookup() {
        assertEquals(
            "native-field",
            SysUiPresentationRuntime.optionalNativeRead { "native-field" },
        )
        assertNull(
            SysUiPresentationRuntime.optionalNativeRead<String> {
                throw NoSuchFieldException("missing")
            },
        )
        assertNull(
            SysUiPresentationRuntime.optionalNativeRead<String> {
                throw InvocationTargetException(IllegalStateException("optional"))
            },
        )
    }

    @Test
    fun optionalGeometryQueryFallsBackWithoutTreatingFatalAsFalse() {
        val known = SysUiPresentationRuntime.optionalNativeRead { true } == true
        val unavailable =
            SysUiPresentationRuntime.optionalNativeRead<Boolean> {
                throw IllegalStateException("location-unavailable")
            } == true
        assertEquals(true, known)
        assertEquals(false, unavailable)

        val fatal = OutOfMemoryError("geometry")
        assertSame(
            fatal,
            runCatching {
                SysUiPresentationRuntime.optionalNativeRead<Boolean> {
                    throw InvocationTargetException(fatal)
                } == true
            }.exceptionOrNull(),
        )
    }

    @Test
    fun optionalNativeReadPreservesDirectAndReflectedFatalErrors() {
        for (fatal in listOf<Throwable>(ThreadDeath(), OutOfMemoryError("native"))) {
            assertSame(
                fatal,
                runCatching {
                    SysUiPresentationRuntime.optionalNativeRead<String> { throw fatal }
                }.exceptionOrNull(),
            )
            assertSame(
                fatal,
                runCatching {
                    SysUiPresentationRuntime.optionalNativeRead<String> {
                        throw InvocationTargetException(fatal)
                    }
                }.exceptionOrNull(),
            )
        }
    }

    @Test
    fun ordinaryInstallFailureAllowsFeatureNativeFallback() {
        SysUiPresentationRuntime.throwIfFatal(IllegalStateException("missing-contract"))
    }

    @Test
    fun fatalInstallErrorsKeepTheirIdentity() {
        val errors = listOf<Throwable>(ThreadDeath(), OutOfMemoryError("fatal-install"))
        errors.forEach { error ->
            val direct =
                runCatching { SysUiPresentationRuntime.throwIfFatal(error) }.exceptionOrNull()
            val wrapped =
                runCatching {
                    SysUiPresentationRuntime.throwIfFatal(InvocationTargetException(error))
                }.exceptionOrNull()
            assertSame(error, direct)
            assertSame(error, wrapped)
        }
        SysUiPresentationRuntime.throwIfFatal(
            InvocationTargetException(IllegalStateException("ordinary-install")),
        )
    }
}
