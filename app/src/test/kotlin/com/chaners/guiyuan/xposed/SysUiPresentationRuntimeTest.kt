package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SysUiPresentationRuntimeTest {
    @Test
    fun passiveDiagnosticSuppressesNonfatalButRetainsWrappedFatal() {
        SysUiPresentationRuntime.emitPassiveDiagnostic {
            throw IllegalStateException("diagnostic-logging-failed")
        }
        val fatal = OutOfMemoryError("diagnostic-fatal")
        assertSame(
            fatal,
            runCatching {
                SysUiPresentationRuntime.emitPassiveDiagnostic {
                    throw InvocationTargetException(fatal)
                }
            }.exceptionOrNull(),
        )
    }

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
    fun sharedFatalRethrowPreservesDirectAndReflectedOriginalErrors() {
        for (fatal in listOf<Throwable>(ThreadDeath(), OutOfMemoryError("source-fatal"))) {
            assertSame(
                fatal,
                runCatching { SysUiFatal.rethrow(fatal) }.exceptionOrNull(),
            )
            assertSame(
                fatal,
                runCatching {
                    SysUiFatal.rethrow(InvocationTargetException(fatal))
                }.exceptionOrNull(),
            )
        }
        SysUiFatal.rethrow(InvocationTargetException(NoSuchMethodException("optional")))
    }

    @Test
    fun hookInstallFallbackRunsOnceAndNeverSwallowsFatalErrors() {
        var fallbackCalls = 0
        fun resolve(result: Result<Int>): Int =
            with(SysUiPresentationRuntime) {
                result.hookCountOrNativeFallback {
                    fallbackCalls++
                    0
                }
            }

        assertEquals(3, resolve(runCatching { 3 }))
        assertEquals(0, fallbackCalls)
        assertEquals(0, resolve(runCatching<Int> { error("missing-contract") }))
        assertEquals(1, fallbackCalls)

        for (fatal in listOf<Throwable>(ThreadDeath(), OutOfMemoryError("fatal-install"))) {
            for (error in listOf(fatal, InvocationTargetException(fatal))) {
                assertSame(
                    fatal,
                    runCatching { resolve(Result.failure(error)) }.exceptionOrNull(),
                )
            }
        }
        assertEquals(1, fallbackCalls)
    }
}
