package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StatusStateStoreBatteryTest {
    @Test
    fun batterySnapshotRetainsNativeSemanticStateAndColor() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(
                percent = 61,
                charging = false,
                semanticState = BatterySemanticState.PERFORMANCE,
                systemSemanticColor = 0xff3482ff.toInt(),
            ),
        )

        val restored =
            StatusStateStore.snapshot().battery
                ?: error("expected battery state")

        assertEquals(61, restored.percent)
        assertFalse(restored.charging)
        assertEquals(BatterySemanticState.PERFORMANCE, restored.semanticState)
        assertEquals(0xff3482ff.toInt(), restored.systemSemanticColor)
    }

    @Test
    fun invalidChargingIconDoesNotClearCurrentIcon() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(
                percent = 80,
                charging = true,
                chargingIconResId = 7,
            ),
        )

        assertNull(StatusStateStore.updateBatteryChargingIcon(0))
        assertEquals(7, StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun chargingSampleWithoutIconKeepsCommittedGlyph() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 42),
        )

        assertNull(
            StatusStateStore.updateBattery(
                StatusStateStore.BatteryState(percent = 80, charging = true),
            ),
        )
        assertEquals(42, StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun freshGlyphReplacesPreviousResource() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 7),
        )
        StatusStateStore.updateBatteryChargingIcon(42)

        assertEquals(42, StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun freshBatterySampleReplacesCachedGlyph() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 7),
        )
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 42),
        )

        assertEquals(42, StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun chargingExitClearsGlyph() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 42),
        )
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = false),
        )

        assertNull(StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun chargingStateRefreshKeepsLastGlyph() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 42),
        )
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 81, charging = true),
        )

        assertEquals(81, StatusStateStore.snapshot().battery?.percent)
        assertEquals(42, StatusStateStore.snapshot().battery?.chargingIconResId)
    }
}
