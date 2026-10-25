package com.chaners.guiyuan.xposed

import org.junit.Assert.assertSame
import org.junit.Test

class SysUiKeyguardAodExceptionTest {
    @Test
    fun fatalNativeFieldReadErrorRemainsFatal() {
        val fatal = OutOfMemoryError("simulated")
        val actual =
            runCatching { SysUiKeyguardAodSource.rethrowFatal(fatal) }
                .exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun threadDeathIsNotConvertedToUnavailableState() {
        val fatal = ThreadDeath()
        val actual =
            runCatching { SysUiKeyguardAodSource.rethrowFatal(fatal) }
                .exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun unsupportedOptionalStyleRemainsRecoverable() {
        SysUiKeyguardAodSource.rethrowFatal(NoSuchFieldException("optional"))
    }
}
