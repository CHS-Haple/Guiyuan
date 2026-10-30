package com.chaners.guiyuan.settings

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class NativeFontAxisTest {
    @Test
    fun fatalPreferenceReadErrorsPreserveOriginalCause() {
        val vm = OutOfMemoryError("native-axis")
        val wrapped = runCatching {
            NativeFontAxis.rethrowFatal(InvocationTargetException(vm))
        }.exceptionOrNull()
        assertSame(vm, wrapped)
        val thread = ThreadDeath()
        assertSame(thread, runCatching { NativeFontAxis.rethrowFatal(thread) }.exceptionOrNull())
        NativeFontAxis.rethrowFatal(IllegalStateException("ordinary-unavailable"))
    }

    @Test
    fun senderMustOwnInstalledSystemUiPackage() {
        assertEquals(true, NativeFontAxis.isSystemUiSender(10224, "com.android.systemui", 10224))
        assertEquals(true, NativeFontAxis.isSystemUiSender(1000, "com.android.systemui", 1000))
        assertEquals(false, NativeFontAxis.isSystemUiSender(10224, "com.android.systemui", 1000))
        assertEquals(false, NativeFontAxis.isSystemUiSender(10224, "com.example.app", 10224))
        assertEquals(false, NativeFontAxis.isSystemUiSender(-1, "com.android.systemui", -1))
        assertEquals(false, NativeFontAxis.isSystemUiSender(10224, null, 10224))
        assertEquals(false, NativeFontAxis.isSystemUiSender(10224, "com.android.systemui", null))
    }

    @Test
    fun acceptsActualVariableAxisRange() {
        val axis = FontWeightAxis(150f, 330f, 700f)
        assertEquals(axis, NativeFontAxis.checked(axis))
        assertEquals(FontWeightAxis(100f, 400f, 1000f),
            NativeFontAxis.checked(FontWeightAxis(100f, 400f, 1000f)))
    }

    @Test
    fun distinguishesPendingAndUnavailableReports() {
        val axis = FontWeightAxis(150f, 330f, 700f)
        assertEquals(NativeFontAxis.State.Pending, NativeFontAxis.state(false, null))
        assertEquals(NativeFontAxis.State.Pending, NativeFontAxis.state(false, axis))
        assertEquals(NativeFontAxis.State.Unavailable, NativeFontAxis.state(true, null))
        assertEquals(
            NativeFontAxis.State.Variable(axis),
            NativeFontAxis.state(true, axis),
        )
        assertEquals(
            NativeFontAxis.State.Unavailable,
            NativeFontAxis.state(true, FontWeightAxis(0f, 330f, 700f)),
        )
    }

    @Test
    fun rejectsUnverifiedAndUnsafeAxis() {
        assertNull(NativeFontAxis.checked(null))
        assertNull(NativeFontAxis.checked(FontWeightAxis(Float.NaN, 330f, 700f)))
        assertNull(NativeFontAxis.checked(FontWeightAxis(0f, 330f, 700f)))
        assertNull(NativeFontAxis.checked(FontWeightAxis(150f, 800f, 700f)))
        assertNull(NativeFontAxis.checked(FontWeightAxis(150f, 330f, 1100f)))
        assertNull(NativeFontAxis.checked(FontWeightAxis(700f, 700f, 700f)))
    }
}
