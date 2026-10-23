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
    fun neutralTiltKeepsExistingPanelAndPillDirections() {
        val neutral = (PI / 4).toFloat()
        val tilt = GlassTilt((-PI / 2).toFloat(), neutral, 0f)
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
    fun forwardAndBackwardTiltRotateRimInOppositeDirections() {
        val neutral = (PI / 4).toFloat()
        val delta = (PI / 12).toFloat()
        val towardUpright = (litHighlight(GlassTilt(0f, neutral + delta, 0f), -45f).style as BloomStroke).primaryLight.position
        val towardFlat = (litHighlight(GlassTilt(0f, neutral - delta, 0f), -45f).style as BloomStroke).primaryLight.position
        val plus = (-PI / 4 + 10 * PI / 180).toFloat()
        val minus = (-PI / 4 - 10 * PI / 180).toFloat()
        assertEquals(0.5f + cos(plus), towardUpright.x, 0.0001f)
        assertEquals(0.7f + sin(plus), towardUpright.y, 0.0001f)
        assertEquals(0.5f + cos(minus), towardFlat.x, 0.0001f)
        assertEquals(0.7f + sin(minus), towardFlat.y, 0.0001f)
    }

    @Test
    fun orientationSignDoesNotFlipThePitchHighlight() {
        val positive = (litHighlight(GlassTilt(0f, 1f, 0f), -45f).style as BloomStroke).primaryLight.position
        val negative = (litHighlight(GlassTilt(0f, -1f, 0f), -45f).style as BloomStroke).primaryLight.position
        assertEquals(positive, negative)
    }

    @Test
    fun rollOnlyMovesHighlightHorizontally() {
        val neutral = (PI / 4).toFloat()
        val baseline = (litHighlight(GlassTilt(0f, neutral, 0f), -45f).style as BloomStroke).primaryLight.position
        val rolled = (litHighlight(GlassTilt(0f, neutral, 0.5f), -45f).style as BloomStroke).primaryLight.position
        assertEquals(baseline.x + 0.06f, rolled.x, 0.0001f)
        assertEquals(baseline.y, rolled.y, 0.0001f)
    }

    @Test
    fun nearUprightPitchStillChangesWithoutEarlySaturation() {
        val angle75 = (75 * PI / 180).toFloat()
        val angle85 = (85 * PI / 180).toFloat()
        val at75 = (litHighlight(GlassTilt(0f, angle75, 0f), -45f).style as BloomStroke).primaryLight.position
        val at85 = (litHighlight(GlassTilt(0f, angle85, 0f), -45f).style as BloomStroke).primaryLight.position
        assertTrue(kotlin.math.abs(at75.x - at85.x) > 0.01f)
    }

    @Test
    fun fullRangePitchAndRollAreBounded() {
        val upright = (litHighlight(GlassTilt(0f, 0f, 0f), 90f).style as BloomStroke).primaryLight.position
        val pitched = (litHighlight(GlassTilt(0f, 20f, 0f), 90f).style as BloomStroke).primaryLight.position
        val rolled = (litHighlight(GlassTilt(0f, 0f, -20f), 90f).style as BloomStroke).primaryLight.position
        val limitAngle = (PI / 2 + 30 * PI / 180).toFloat()
        assertEquals(0.5f + cos(limitAngle), pitched.x, 0.0001f)
        assertEquals(0.7f + sin(limitAngle), pitched.y, 0.0001f)
        assertEquals(upright.x - 0.12f, rolled.x, 0.0001f)
        assertEquals(upright.y, rolled.y, 0.0001f)
        assertTrue(pitched.x.isFinite() && pitched.y.isFinite())
    }
}
