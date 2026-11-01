package com.chaners.guiyuan.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideActionContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val light = maxOf(a.luminance(), b.luminance())
        val dark = minOf(a.luminance(), b.luminance())
        return (light + 0.05f) / (dark + 0.05f)
    }

    @Test
    fun themeInversionIsRetainedForShaderOffFallback() {
        val samples = listOf(
            Triple(Color.White, Color.Black, Color(0xff6f398f)),
            Triple(Color.Black, Color.White, Color(0xffc1a8db)),
            Triple(Color(0xff161616), Color(0xffebebeb), Color(0xffb08aca)),
        )
        for ((surface, onSurface, logoTint) in samples) {
            val style = guideActionPalette(surface, onSurface, logoTint)
            assertEquals(surface, style.content)
            assertEquals(onSurface, style.fallback)
            assertTrue(contrast(style.fallback, style.content) >= 4.5f)
        }
    }

    @Test
    fun logoTonedScrimStaysTranslucentSoBackdropCanRemainVisible() {
        val style = guideActionPalette(Color.White, Color.Black, Color(0xff6f398f))
        assertTrue(style.scrim.alpha > 0f && style.scrim.alpha < 0.5f)
        assertTrue(style.scrim != style.fallback)
    }
}
