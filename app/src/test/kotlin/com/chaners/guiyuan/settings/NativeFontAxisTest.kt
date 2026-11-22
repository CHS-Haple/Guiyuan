package com.chaners.guiyuan.settings

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

private const val TEST_VERIFIED_SYSTEM_UI_PACKAGE = "com.android.systemui"

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
        assertEquals(true, NativeFontAxisPolicy.isSystemUiSender(10224, TEST_VERIFIED_SYSTEM_UI_PACKAGE, 10224))
        assertEquals(true, NativeFontAxisPolicy.isSystemUiSender(1000, TEST_VERIFIED_SYSTEM_UI_PACKAGE, 1000))
        assertEquals(false, NativeFontAxisPolicy.isSystemUiSender(10224, TEST_VERIFIED_SYSTEM_UI_PACKAGE, 1000))
        assertEquals(false, NativeFontAxisPolicy.isSystemUiSender(10224, "com.example.app", 10224))
        assertEquals(false, NativeFontAxisPolicy.isSystemUiSender(-1, TEST_VERIFIED_SYSTEM_UI_PACKAGE, -1))
        assertEquals(false, NativeFontAxisPolicy.isSystemUiSender(10224, null, 10224))
        assertEquals(false, NativeFontAxisPolicy.isSystemUiSender(10224, TEST_VERIFIED_SYSTEM_UI_PACKAGE, null))
    }

    @Test
    fun acceptsActualVariableAxisRange() {
        val axis = FontWeightAxis(150f, 330f, 700f)
        assertEquals(axis, NativeFontAxisPolicy.checked(axis))
        assertEquals(FontWeightAxis(100f, 400f, 1000f),
            NativeFontAxisPolicy.checked(FontWeightAxis(100f, 400f, 1000f)))
    }

    @Test
    fun distinguishesPendingAndUnavailableReports() {
        val axis = FontWeightAxis(150f, 330f, 700f)
        assertEquals(NativeFontAxis.State.Pending, NativeFontAxisPolicy.state(false, null))
        assertEquals(NativeFontAxis.State.Pending, NativeFontAxisPolicy.state(false, axis))
        assertEquals(NativeFontAxis.State.Unavailable, NativeFontAxisPolicy.state(true, null))
        assertEquals(
            NativeFontAxis.State.Variable(axis),
            NativeFontAxisPolicy.state(true, axis),
        )
        assertEquals(
            NativeFontAxis.State.Unavailable,
            NativeFontAxisPolicy.state(true, FontWeightAxis(0f, 330f, 700f)),
        )
    }

    @Test
    fun rejectsUnverifiedAndUnsafeAxis() {
        assertNull(NativeFontAxisPolicy.checked(null))
        assertNull(NativeFontAxisPolicy.checked(FontWeightAxis(Float.NaN, 330f, 700f)))
        assertNull(NativeFontAxisPolicy.checked(FontWeightAxis(0f, 330f, 700f)))
        assertNull(NativeFontAxisPolicy.checked(FontWeightAxis(150f, 800f, 700f)))
        assertNull(NativeFontAxisPolicy.checked(FontWeightAxis(150f, 330f, 1100f)))
        assertNull(NativeFontAxisPolicy.checked(FontWeightAxis(700f, 700f, 700f)))
    }
}
