package com.chaners.guiyuan.xposed

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
            val actual =
                runCatching { SysUiPresentationRuntime.throwIfFatal(error) }.exceptionOrNull()
            assertSame(error, actual)
        }
    }
}
