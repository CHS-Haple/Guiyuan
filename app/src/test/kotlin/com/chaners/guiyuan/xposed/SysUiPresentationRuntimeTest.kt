package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertSame
import org.junit.Test

class SysUiPresentationRuntimeTest {
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
