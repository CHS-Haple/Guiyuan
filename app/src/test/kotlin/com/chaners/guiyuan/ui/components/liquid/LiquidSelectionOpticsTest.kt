package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Contract shared with the accepted portrait Liquid selection surface. */
class LiquidSelectionOpticsTest {
    @Test fun idleHasNoRefractionRimOrInnerShadow() {
        val optics = liquidSelectionOptics(0f)
        assertEquals(0.dp, optics.lensHeight)
        assertEquals(0.dp, optics.lensAmount)
        assertEquals(0f, optics.rimAlpha, 0f)
        assertEquals(0.dp, optics.shadowRadius)
        assertEquals(0f, optics.shadowAlpha, 0f)
        assertEquals(1f, optics.restAlpha, 0f)
    }

    @Test fun pressedUsesPortraitRefractionAndShadow() {
        val optics = liquidSelectionOptics(1f)
        assertEquals(10.dp, optics.lensHeight)
        assertEquals(14.dp, optics.lensAmount)
        assertEquals(8.dp, optics.shadowRadius)
        assertEquals(1f, optics.rimAlpha, 0f)
        assertEquals(1f, optics.shadowAlpha, 0f)
        assertEquals(0f, optics.restAlpha, 0f)
        assertEquals(0.03f, optics.pressedTintAlpha, 0f)
    }

    @Test fun outsideProgressIsClampedAndHalfwayInterpolates() {
        assertEquals(liquidSelectionOptics(0f), liquidSelectionOptics(-0.5f))
        assertEquals(liquidSelectionOptics(1f), liquidSelectionOptics(1.5f))
        val half = liquidSelectionOptics(0.5f)
        assertEquals(5.dp, half.lensHeight)
        assertEquals(7.dp, half.lensAmount)
        assertEquals(0.5f, half.rimAlpha, 0f)
    }
}
