package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SandboxCcTransitionTest {
    private val real =
        RenderModel(
            batteryPercent = 43,
            charging = false,
            centerIndicator = CenterIndicator.Empty,
            mobileLevel = 3,
            effectiveDataSubscriptionId = 4,
        )

    @Test
    fun equalComponentCanKeepNativeTransition() {
        for (part in StatusPainter.TransitionComponent.entries) {
            assertTrue(CcTransitionPolicy.sameNativeEndpoint(part, real, real))
        }
    }

    @Test
    fun mismatchedPreviewFadesOnlyAffectedComponents() {
        val preview = real.copy(
            batteryPercent = 87,
            centerIndicator = CenterIndicator.Airplane,
            mobileLevel = 1,
            effectiveDataSubscriptionId = -1,
        )
        val compatibility = StatusPainter.TransitionComponent.entries.associateWith {
            CcTransitionPolicy.sameNativeEndpoint(it, preview, real)
        }
        assertFalse(compatibility.getValue(StatusPainter.TransitionComponent.BATTERY))
        assertFalse(compatibility.getValue(StatusPainter.TransitionComponent.BATTERY_NUMBER))
        assertFalse(compatibility.getValue(StatusPainter.TransitionComponent.CENTER))
        assertFalse(compatibility.getValue(StatusPainter.TransitionComponent.MOBILE))
        assertTrue(compatibility.getValue(StatusPainter.TransitionComponent.CHARGING_ICON))
    }

    @Test
    fun unavailableNativeSourceNeverBecomesTarget() {
        for (part in StatusPainter.TransitionComponent.entries) {
            assertFalse(CcTransitionPolicy.sameNativeEndpoint(part, real, null))
        }
    }

    @Test
    fun chargingMismatchDoesNotFlyTowardStaleNativeGlyph() {
        assertFalse(
            CcTransitionPolicy.sameNativeEndpoint(
                StatusPainter.TransitionComponent.CHARGING_ICON,
                real.copy(charging = true),
                real,
            ),
        )
    }
}
