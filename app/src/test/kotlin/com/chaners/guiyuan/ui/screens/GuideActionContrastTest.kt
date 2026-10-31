package com.chaners.guiyuan.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideActionContrastTest {
    private fun contrast(foreground: Color, background: Color): Float {
        val light = maxOf(foreground.luminance(), background.luminance())
        val dark = minOf(foreground.luminance(), background.luminance())
        return (light + 0.05f) / (dark + 0.05f)
    }

    @Test
    fun brandMixRespectsForegroundContrastInLightAndDarkThemes() {
        val themes = listOf(
            Triple(Color.White, Color.Black, Color(0xff6f398f)),
            Triple(Color(0xffe6d3f1), Color(0xff25172e), Color(0xff6f398f)),
            Triple(Color.Black, Color.White, Color(0xffc1a8db)),
            Triple(Color(0xff161616), Color(0xffebebeb), Color(0xffb08aca)),
        )
        for ((surface, onSurface, accent) in themes) {
            val action = resolveGuideActionColors(surface, onSurface, accent)
            // Compose Color packs non-opaque channel values; the configured 0.82f
            // may round fractionally below its source literal after readback.
            assertTrue(action.container.alpha >= 0.81f && action.container.alpha <= 1f)
            assertTrue(action.materialAlpha in 0f..0.22f)
            for (position in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                val stop = androidx.compose.ui.graphics.lerp(action.container, action.gradientEnd, position)
                for (background in listOf(Color.Black, Color.White)) {
                    val resolved = stop.compositeOver(background)
                    for (extreme in listOf(Color.Black, Color.White)) {
                        val mixed = extreme.copy(alpha = action.materialAlpha).compositeOver(resolved)
                        assertTrue(
                            "Foreground contrast is below 4.5 in theme $surface at $position",
                            contrast(action.content, mixed) >= 4.5f,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun accessibleThemeCanShowBrandedMixWithoutChangingSolidForeground() {
        val logoTint = Color(0xff6f398f)
        val action = resolveGuideActionColors(Color.White, Color.Black, logoTint)
        assertTrue(action.materialAlpha > 0f)
        assertTrue("The branded background should have visible tonal variation", action.container != action.gradientEnd)
        assertEquals("The gradient must keep the logo tint's alpha", action.container.alpha, action.gradientEnd.alpha, 0.00001f)
        assertTrue("The light-glass base must actually remain translucent", action.container.alpha < 1f)
        assertEquals(Color.White, action.content)
    }
}
