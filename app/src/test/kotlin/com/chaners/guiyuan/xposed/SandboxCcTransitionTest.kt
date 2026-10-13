package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.InternetState
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
    fun identicalComponentsKeepNativeTargets() {
        for (part in StatusPainter.TransitionComponent.entries) {
            assertTrue(CcTransitionPolicy.canMatchNative(part, real, real))
        }
    }

    @Test
    fun differentBatteryPercentOnlySeparatesNumberFromRing() {
        val preview = real.copy(batteryPercent = 87)
        assertTrue(CcTransitionPolicy.canMatchNative(StatusPainter.TransitionComponent.BATTERY, preview, real))
        assertFalse(CcTransitionPolicy.canMatchNative(StatusPainter.TransitionComponent.BATTERY_NUMBER, preview, real))
    }

    @Test
    fun batteryModeColorDoesNotRemoveRingTarget() {
        val preview = real.copy(batterySemanticState = com.chaners.guiyuan.xposed.battery.BatterySemanticState.POWER_SAVE)
        assertTrue(CcTransitionPolicy.canMatchNative(StatusPainter.TransitionComponent.BATTERY, preview, real))
    }

    @Test
    fun wifiSignalVariationKeepsSameWifiTarget() {
        val native = real.copy(centerIndicator = CenterIndicator.Wifi(3, InternetState.VALIDATED, 11))
        val preview = native.copy(centerIndicator = CenterIndicator.Wifi(1, InternetState.NO_INTERNET, 13))
        assertTrue(CcTransitionPolicy.canMatchNative(StatusPainter.TransitionComponent.CENTER, preview, native))
    }

    @Test
    fun incompatibleCenterTypesDoNotShareNativeTarget() {
        val native = real.copy(centerIndicator = CenterIndicator.MobileType("5G", false, InternetState.VALIDATED))
        val preview = native.copy(centerIndicator = CenterIndicator.Wifi(2, InternetState.VALIDATED))
        assertFalse(CcTransitionPolicy.canMatchNative(StatusPainter.TransitionComponent.CENTER, preview, native))
    }

    @Test
    fun mobileBarsCanTravelToActualSignalAtDifferentLevels() {
        val preview = real.copy(mobileLevel = 1, effectiveDataSubscriptionId = -1)
        assertTrue(CcTransitionPolicy.canMatchNative(StatusPainter.TransitionComponent.MOBILE, preview, real))
        assertFalse(
            CcTransitionPolicy.canMatchNative(
                StatusPainter.TransitionComponent.MOBILE,
                preview.copy(mobileLevel = null),
                real,
            ),
        )
    }

    @Test
    fun missingNativeModelNeverProvidesAnEndpoint() {
        for (part in StatusPainter.TransitionComponent.entries) {
            assertFalse(CcTransitionPolicy.canMatchNative(part, real, null))
        }
    }

    @Test
    fun chargingMismatchDoesNotUseStaleNativeGlyph() {
        assertFalse(
            CcTransitionPolicy.canMatchNative(
                StatusPainter.TransitionComponent.CHARGING_ICON,
                real.copy(charging = true),
                real,
            ),
        )
    }
}
