package com.chaners.guiyuan.xposed.network

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertSame
import org.junit.Test

class SysUiConnectivitySourceTest {
    @Test
    fun reflectedFatalNetworkFailurePropagatesOriginalCause() {
        val fatal = OutOfMemoryError("connectivity-fatal")
        val actual =
            runCatching {
                SysUiConnectivitySource.rethrowFatal(InvocationTargetException(fatal))
            }.exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun directThreadDeathFromCallbackRemainsFatal() {
        val fatal = ThreadDeath()
        val actual =
            runCatching { SysUiConnectivitySource.rethrowFatal(fatal) }.exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun ordinaryReflectedFailureRemainsRecoverable() {
        SysUiConnectivitySource.rethrowFatal(
            InvocationTargetException(IllegalStateException("callback-detach")),
        )
    }
}
