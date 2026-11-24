package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VisualBatteryColorPersistenceTest {
    @Test
    fun everyBatterySlotReadsItsOwnModeAndColorKeys() {
        for (slot in BatteryColorSlot.entries) {
            val prefs = VisualTestPreferences()
            val color = 0xFF123456.toInt()
            prefs.edit()
                .putString(batteryColorModeKey(slot), BatteryColorMode.FOLLOW_SYSTEM.persistedValue)
                .putInt(batteryColorOverrideKey(slot), color)
                .apply()
            val visual = prefs.readVisualCfg()
            for (other in BatteryColorSlot.entries) {
                assertEquals(
                    if (other == slot) BatteryColorMode.FOLLOW_SYSTEM else BatteryColorMode.PRESET,
                    visual.batteryColorModes.modeFor(other),
                )
                assertEquals(
                    if (other == slot) color else null,
                    visual.batteryColorOverrides.colorFor(other),
                )
            }
        }
    }

    @Test
    fun legacyBatteryColorWithoutExplicitModeKeepsCustomFallbackForEverySlot() {
        for (slot in BatteryColorSlot.entries) {
            val prefs = VisualTestPreferences()
            prefs.edit().putInt(batteryColorOverrideKey(slot), 0xFF123456.toInt()).apply()
            val visual = prefs.readVisualCfg()
            assertEquals(BatteryColorMode.CUSTOM, visual.batteryColorModes.modeFor(slot))
            assertEquals(0xFF123456.toInt(), visual.batteryColorOverrides.colorFor(slot))
        }
    }

    @Test
    fun singleAndDualKeepSeparateGeometryButShareBatteryColors() {
        val prefs = VisualTestPreferences()
        val single = VisualCfg(
            layout = ContentLayout.BATTERY_CENTER,
            combinedScale = 0.9f,
            wifiOffsetY = -3f,
            batteryColorPreset = BatteryColorPreset.RECOMMENDED,
            batteryColorModes = BatteryColorModes(charging = BatteryColorMode.CUSTOM),
            batteryColorOverrides = BatteryColorOverrides(charging = 0xFF123456.toInt()),
        )
        val dual = VisualCfg(
            layout = ContentLayout.BATTERY_CENTER,
            dualSignal = true,
            combinedScale = 1.2f,
            wifiOffsetY = 4f,
            batteryColorPreset = BatteryColorPreset.IOS_STYLE,
            batteryColorModes = BatteryColorModes(powerSave = BatteryColorMode.FOLLOW_SYSTEM),
        )
        prefs.edit().putVisualCfg(single).apply()
        prefs.edit().putVisualCfg(dual).apply()

        assertEquals(dual.normalized(), prefs.readVisualCfg(dual = true))
        assertEquals(dual.normalized().copy(dualSignal = false), prefs.readVisualCfg(dual = false))
        assertEquals(ContentLayout.BATTERY_CENTER, prefs.readLayout())
        assertFalse(prefs.all.keys.any { it.startsWith("dual.battery_color_") })

        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg().batteryColorPreset)
        assertEquals(1f, prefs.readVisualCfg().combinedScale, 0.001f)
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg().batteryColorPreset)
        assertEquals(1f, prefs.readVisualCfg().combinedScale, 0.001f)
    }
}
