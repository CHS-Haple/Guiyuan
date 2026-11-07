package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
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

    private open class NativeHeaderBase {
        @JvmField val inherited: Long = 11L
    }

    private class NativeHeaderChild : NativeHeaderBase() {
        @JvmField val own: Long = 22L
    }

    @Test
    fun declaredHeaderFieldResolvesOnItsOwnerClass() {
        val field = SettingsEntryHook.run { NativeHeaderChild::class.java.findField("own") }
        assertEquals(NativeHeaderChild::class.java, field.declaringClass)
    }

    @Test
    fun inheritedHeaderFieldRemainsDiscoverableAfterSubclassMiss() {
        val field = SettingsEntryHook.run { NativeHeaderChild::class.java.findField("inherited") }
        assertEquals(NativeHeaderBase::class.java, field.declaringClass)
    }

    @Test
    fun completelyAbsentHeaderFieldStillFailsClosed() {
        val failure = assertThrows(IllegalStateException::class.java) {
            SettingsEntryHook.run { NativeHeaderChild::class.java.findField("absent") }
        }
        assertEquals("Missing Header field: absent", failure.message)
    }
}
