package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertSame
import org.junit.Test

class SysUiHotReloadFatalTest {
    @Test
    fun directFatalErrorEscapes() {
        val fatal = ThreadDeath()
        val actual = runCatching { SysUiHotReload.rethrowFatal(fatal) }.exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun reflectedFatalErrorEscapes() {
        val fatal = OutOfMemoryError("hot-reload")
        val actual =
            runCatching { SysUiHotReload.rethrowFatal(InvocationTargetException(fatal)) }
                .exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun ordinaryTransferErrorRemainsRecoverable() {
        SysUiHotReload.rethrowFatal(IllegalStateException("missing-transfer"))
    }
}
