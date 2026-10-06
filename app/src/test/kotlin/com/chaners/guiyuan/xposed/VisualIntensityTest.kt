package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualIntensityTest {
    @Test
    fun fullStrengthCanvasUsesSystemTintAlpha() {
        assertEquals(
            191,
            VisualIntensity.resolveCanvasAlpha(
                color = 0xbf123456.toInt(),
                semanticAlpha = 255,
                opacity = 1f,
            ),
        )
    }

    @Test
    fun semanticDimmingMultipliesTint() {
        assertEquals(
            35,
            VisualIntensity.resolveCanvasAlpha(
                color = 0xbf123456.toInt(),
                semanticAlpha = 48,
                opacity = 1f,
            ),
        )
    }

    @Test
    fun transitionOpacityIsIndependent() {
        assertEquals(
            95,
            VisualIntensity.resolveCanvasAlpha(
                color = 0xbf123456.toInt(),
                semanticAlpha = 255,
                opacity = 0.5f,
            ),
        )
        assertEquals(
            128,
            VisualIntensity.resolveDrawableAlpha(0.5f),
        )
    }
}
