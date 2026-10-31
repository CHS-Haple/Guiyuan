package com.chaners.guiyuan.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.chaners.guiyuan.ui.screens.about.BgEffectConfig
import com.chaners.guiyuan.ui.screens.about.DeviceType
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
    fun respectsTheActualGuideBackdropColorsInBothThemesAndFormFactors() {
        val themes = listOf(
            Triple(Color.White, Color.Black, Color(0xff6f398f)),
            Triple(Color(0xffe6d3f1), Color(0xff25172e), Color(0xff6f398f)),
            Triple(Color.Black, Color.White, Color(0xffc1a8db)),
            Triple(Color(0xff161616), Color(0xffebebeb), Color(0xffb08aca)),
        )
        for (type in listOf(DeviceType.PHONE, DeviceType.PAD)) {
            for ((surface, onSurface, logoTint) in themes) {
                val preset = BgEffectConfig.get(type, surface.luminance() < 0.5f)
                val backdrop = guideBackdropColors(preset)
                assertEquals(12, backdrop.size)
                val action = resolveGuideActionColors(surface, onSurface, logoTint, backdrop)
                assertTrue(action.container.alpha in 0f..1f)
                assertTrue(action.materialAlpha in 0f..0.12f)
                val underlays = backdrop.map { it.compositeOver(surface) } + surface
                for (position in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                    val stop = androidx.compose.ui.graphics.lerp(action.container, action.gradientEnd, position)
                    for (background in underlays) {
                        val resolved = stop.compositeOver(background)
                        for (pigment in listOf(Color.Black, Color.White)) {
                            val mixed = pigment.copy(alpha = action.materialAlpha).compositeOver(resolved)
                            assertTrue(
                                "Contrast below 4.5 for $type with surface=$surface at $position",
                                contrast(action.content, mixed) >= 4.5f,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun representativeLightAndDarkPhonePalettesProduceVisibleBrandColorSeparation() {
        for ((surface, onSurface, logoTint) in listOf(
            Triple(Color.White, Color.Black, Color(0xff6f398f)),
            Triple(Color(0xff161616), Color(0xffebebeb), Color(0xffb08aca)),
        )) {
            val palette = guideBackdropColors(
                BgEffectConfig.get(DeviceType.PHONE, surface.luminance() < 0.5f),
            )
            val action = resolveGuideActionColors(surface, onSurface, logoTint, palette)
            val delta = maxOf(
                kotlin.math.abs(action.container.red - action.gradientEnd.red),
                kotlin.math.abs(action.container.green - action.gradientEnd.green),
                kotlin.math.abs(action.container.blue - action.gradientEnd.blue),
            ) * action.container.alpha
            assertTrue("Brand gradient collapsed to a flat color", delta >= 0.09f)
            assertTrue("The MIUIX Logo material should remain visible", action.materialAlpha > 0f)
            assertTrue("The branded base must retain translucency", action.container.alpha < 1f)
            assertEquals(surface, action.content)
        }
    }
}
