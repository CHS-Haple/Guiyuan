package com.chaners.guiyuan.ui.components.liquid

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke

class LiquidNavBarLightTest {
    @Test
    fun zeroTiltKeepsExistingPanelAndPillDirections() {
        val tilt = GlassTilt((-PI / 2).toFloat(), 0f, 0f)
        val panel = litHighlight(tilt, -45f).style as BloomStroke
        val pill = litHighlight(tilt, 90f).style as BloomStroke
        val panelAngle = (-3 * PI / 4).toFloat()
        assertEquals(0.5f + cos(panelAngle), panel.primaryLight.position.x, 0.0001f)
        assertEquals(0.7f + sin(panelAngle), panel.primaryLight.position.y, 0.0001f)
        assertEquals(1.5f, pill.primaryLight.position.x, 0.0001f)
        assertEquals(0.7f, pill.primaryLight.position.y, 0.0001f)
        assertEquals(0.4f, panel.secondaryLight.intensity, 0f)
    }

    @Test
    fun pitchRotatesHighlightInOppositeDirections() {
        val front = (litHighlight(GlassTilt(0f, 0.5f, 0f), -45f).style as BloomStroke).primaryLight.position
        val back = (litHighlight(GlassTilt(0f, -0.5f, 0f), -45f).style as BloomStroke).primaryLight.position
        val plus = (-PI / 4 + 0.2).toFloat()
        val minus = (-PI / 4 - 0.2).toFloat()
        assertEquals(0.5f + cos(plus), front.x, 0.0001f)
        assertEquals(0.7f + sin(plus), front.y, 0.0001f)
        assertEquals(0.5f + cos(minus), back.x, 0.0001f)
        assertEquals(0.7f + sin(minus), back.y, 0.0001f)
    }

    @Test
    fun rollOnlyMovesHighlightHorizontally() {
        val baseline = (litHighlight(GlassTilt(0f, 0f, 0f), -45f).style as BloomStroke).primaryLight.position
        val rolled = (litHighlight(GlassTilt(0f, 0f, 0.5f), -45f).style as BloomStroke).primaryLight.position
        assertEquals(baseline.x + 0.06f, rolled.x, 0.0001f)
        assertEquals(baseline.y, rolled.y, 0.0001f)
    }

    @Test
    fun extremePitchAndRollAreBounded() {
        val upright = (litHighlight(GlassTilt(0f, 0f, 0f), 90f).style as BloomStroke).primaryLight.position
        val pitched = (litHighlight(GlassTilt(0f, 20f, 0f), 90f).style as BloomStroke).primaryLight.position
        val rolled = (litHighlight(GlassTilt(0f, 0f, -20f), 90f).style as BloomStroke).primaryLight.position
        val limitAngle = (PI / 2 + 12 * PI / 180).toFloat()
        assertEquals(0.5f + cos(limitAngle), pitched.x, 0.0001f)
        assertEquals(0.7f + sin(limitAngle), pitched.y, 0.0001f)
        assertEquals(upright.x - 0.12f, rolled.x, 0.0001f)
        assertEquals(upright.y, rolled.y, 0.0001f)
        assertTrue(pitched.x.isFinite() && pitched.y.isFinite())
    }
}
