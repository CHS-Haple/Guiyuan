package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertSame
import org.junit.Test

class SettingsEntryHookTest {
    @Test
    fun fatalReflectionCauseIsNotConvertedToHeaderFailure() {
        val fatal = OutOfMemoryError("reflected")
        val actual =
            runCatching {
                SettingsEntryHook.rethrowFatal(InvocationTargetException(fatal))
            }.exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun directThreadDeathRemainsFatal() {
        val fatal = ThreadDeath()
        val actual =
            runCatching { SettingsEntryHook.rethrowFatal(fatal) }
                .exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun ordinaryReflectionFailureRemainsRecoverable() {
        SettingsEntryHook.rethrowFatal(
            InvocationTargetException(IllegalStateException("unsupported")),
        )
    }
}
