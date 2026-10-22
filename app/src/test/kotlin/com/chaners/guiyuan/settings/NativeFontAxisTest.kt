package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeFontAxisTest {
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
