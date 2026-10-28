package com.chaners.guiyuan.xposed.network

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertSame
import org.junit.Test

class SysUiNetworkSourceTest {
    @Test
    fun fatalReflectionCauseIsNotReportedAsMissingNativeState() {
        val fatal = OutOfMemoryError("simulated")
        val actual = runCatching {
            SysUiNetworkSource.rethrowFatal(InvocationTargetException(fatal))
        }.exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun threadDeathPropagatesWithoutWrapping() {
        val fatal = ThreadDeath()
        val actual = runCatching { SysUiNetworkSource.rethrowFatal(fatal) }.exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun ordinaryReflectionFailureRemainsRecoverable() {
        SysUiNetworkSource.rethrowFatal(InvocationTargetException(NoSuchMethodException("optional")))
    }
}
