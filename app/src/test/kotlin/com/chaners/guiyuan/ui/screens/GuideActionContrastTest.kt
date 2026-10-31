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
            Triple(Color.White, Color.Black, Color(0xff4c8c61)),
            Triple(Color(0xfffaf8f5), Color(0xff242426), Color(0xff598a67)),
            Triple(Color.Black, Color.White, Color(0xff4c8c61)),
            Triple(Color(0xff161616), Color(0xffebebeb), Color(0xff3bc960)),
        )
        for ((surface, onSurface, accent) in themes) {
            val action = resolveGuideActionColors(surface, onSurface, accent)
            assertEquals(1f, action.container.alpha, 0.00001f)
            assertTrue(action.materialAlpha in 0f..0.30f)
            for (extreme in listOf(Color.Black, Color.White)) {
                val mixed = extreme.copy(alpha = action.materialAlpha).compositeOver(action.container)
                assertTrue(
                    "Foreground contrast is below 4.5 in theme $surface",
                    contrast(action.content, mixed) >= 4.5f,
                )
            }
        }
    }

    @Test
    fun accessibleThemeCanShowBrandedMixWithoutChangingSolidForeground() {
        val action = resolveGuideActionColors(Color.White, Color.Black, Color(0xff4c8c61))
        assertTrue(action.materialAlpha > 0f)
        assertEquals(Color.White, action.content)
    }
}
