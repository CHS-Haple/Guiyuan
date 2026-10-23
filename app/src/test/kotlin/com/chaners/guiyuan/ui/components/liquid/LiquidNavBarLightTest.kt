package com.chaners.guiyuan.ui.components.liquid

import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke

class LiquidNavBarLightTest {
    @Test
    fun zeroTiltPreservesExistingPrimaryLightDirection() {
        val tilt = GlassTilt((-PI / 2).toFloat(), 0f, 0f)
        val panel = litHighlight(tilt, -45f).style as BloomStroke
        val pill = litHighlight(tilt, 90f).style as BloomStroke
        assertEquals(0.5f + kotlin.math.cos((-3 * PI / 4).toFloat()), panel.primaryLight.position.x, 0.0001f)
        assertEquals(0.7f + kotlin.math.sin((-3 * PI / 4).toFloat()), panel.primaryLight.position.y, 0.0001f)
        assertEquals(1.5f, pill.primaryLight.position.x, 0.0001f)
        assertEquals(0.7f, pill.primaryLight.position.y, 0.0001f)
    }

    @Test
    fun pitchAndRollMoveOnlyPrimaryLightWithoutChangingItsIntensity() {
        val upright = litHighlight(GlassTilt(0f, 0f, 0f), -45f).style as BloomStroke
        val tilted = litHighlight(GlassTilt(0f, 0.5f, 0.5f), -45f).style as BloomStroke
        assertEquals(upright.primaryLight.position.x + 0.06f, tilted.primaryLight.position.x, 0.0001f)
        assertEquals(upright.primaryLight.position.y - 0.06f, tilted.primaryLight.position.y, 0.0001f)
        assertEquals(upright.primaryLight.intensity, tilted.primaryLight.intensity, 0f)
        assertEquals(upright.secondaryLight, tilted.secondaryLight)
    }

    @Test
    fun extremeTiltIsClampedAndRemainsFinite() {
        val upright = (litHighlight(GlassTilt(0f, 0f, 0f), 90f).style as BloomStroke).primaryLight.position
        val extreme = (litHighlight(GlassTilt(0f, 20f, -20f), 90f).style as BloomStroke).primaryLight.position
        assertEquals(upright.x - 0.12f, extreme.x, 0.0001f)
        assertEquals(upright.y - 0.12f, extreme.y, 0.0001f)
        assertTrue(extreme.x.isFinite() && extreme.y.isFinite())
    }
}
