package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualRemoteMirrorTest {
    @Test
    fun oldRemoteDualBankRemainsReadableUntilTheSharedMirrorIsPublished() {
        val prefs = VisualTestPreferences()
        val layout = ContentLayout.NETWORK_CENTER
        val sharedScale = visualProfileKey(layout, WIFI_SIZE_SCALE_KEY)
        val oldDualScale = visualModeKey(true, sharedScale)
        val sharedStyle = visualProfileKey(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY)
        val oldDualStyle = visualModeKey(true, sharedStyle)
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putFloat(sharedScale, 0.71f)
            .putFloat(oldDualScale, 1.13f)
            .putFloat(visualProfileKey(layout, COMBINED_SCALE_KEY), 0.75f)
            .putInt(visualModeKey(true, visualProfileKey(layout, WIFI_WEIGHT_KEY)), 120)
            .putString(sharedStyle, SignalDotArrangement.CURVED.persistedValue)
            .putString(oldDualStyle, SignalDotArrangement.HORIZONTAL.persistedValue)
            .apply()

        // SystemUI can read the untouched old remote store before app startup.
        val before = prefs.all.toMap()
        val legacy = prefs.readVisualCfg()
        assertEquals(1.13f, legacy.wifiScale, 0f)
        assertEquals(120, legacy.wifiWeight)
        assertEquals(SignalDotArrangement.HORIZONTAL, legacy.dualRowDotArrangement)
        assertEquals(COMBINED_SCALE_DEFAULT, legacy.combinedScale, 0f)
        assertEquals(0.71f, prefs.readVisualCfg(dual = false).wifiScale, 0f)
        assertEquals(before, prefs.all)

        // The same writer mirrors new settings and their schema atomically.
        prefs.edit().putVisualCfg(
            VisualCfg(
                layout = layout,
                dualSignal = true,
                wifiScale = 0.89f,
                wifiWeight = 86,
                combinedScale = 0.83f,
                dualRowDotArrangement = SignalDotArrangement.CURVED,
            ),
        ).apply()
        assertEquals(4, prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0))
        assertEquals(1, prefs.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0))
        assertTrue(prefs.contains(oldDualScale))
        for (dual in listOf(false, true)) {
            val current = prefs.readVisualCfg(dual = dual, layoutOverride = layout)
            assertEquals(0.89f, current.wifiScale, 0f)
            assertEquals(86, current.wifiWeight)
            assertEquals(0.83f, current.combinedScale, 0f)
            assertEquals(SignalDotArrangement.CURVED, current.dualRowDotArrangement)
        }
    }

    @Test
    fun freshSharedPreferencesDoNotRequireAnOldDualBank() {
        val prefs = VisualTestPreferences()
        val key = visualProfileKey(ContentLayout.NETWORK_CENTER, WIFI_SIZE_SCALE_KEY)
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putFloat(key, 0.92f)
            .apply()
        assertEquals(0.92f, prefs.readVisualCfg().wifiScale, 0f)
    }

    @Test
    fun remoteMirrorPreservesSharedColorsWhileSwitchingGeometry() {
        val local = VisualTestPreferences()
        val remote = VisualTestPreferences()
        val single = VisualCfg(
            combinedScale = 0.8f,
            batteryColorPreset = BatteryColorPreset.RECOMMENDED,
        )
        val dual = VisualCfg(
            dualSignal = true,
            combinedScale = 1.2f,
            batteryColorPreset = BatteryColorPreset.IOS_STYLE,
        )
        local.edit().putVisualCfg(single).apply()
        local.edit().putVisualCfg(dual).apply()
        remote.edit().putVisualCfg(local.readVisualCfg()).apply()
        assertEquals(dual.normalized(), remote.readVisualCfg())

        local.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        remote.edit().putVisualCfg(local.readVisualCfg()).apply()
        assertEquals(1f, remote.readVisualCfg().combinedScale, 0.001f)
        assertEquals(BatteryColorPreset.IOS_STYLE, remote.readVisualCfg().batteryColorPreset)
        assertEquals(BatteryColorPreset.IOS_STYLE, remote.readVisualCfg(dual = true).batteryColorPreset)

        local.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        remote.edit().putVisualCfg(local.readVisualCfg()).apply()
        assertEquals(dual.normalized(), remote.readVisualCfg())
        assertEquals(BatteryColorPreset.IOS_STYLE, remote.readVisualCfg(dual = false).batteryColorPreset)
    }

    @Test
    fun missingDualKeysCannotBorrowLegacySingleValues() {
        val prefs = VisualTestPreferences()
        prefs.edit()
            .putFloat(COMBINED_SCALE_KEY, 0.75f)
            .putFloat(WIFI_VERTICAL_OFFSET_KEY, -3f)
            .putString(BATTERY_COLOR_PRESET_KEY, BatteryColorPreset.IOS_STYLE.persistedValue)
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .apply()
        assertEquals(0.75f, prefs.readVisualCfg().combinedScale, 0.0001f)
        assertEquals(-3f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(-3f, prefs.readVisualCfg(dual = false).wifiOffsetY, 0f)
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg().batteryColorPreset)
        assertEquals(0.75f, prefs.readVisualCfg(dual = false).combinedScale, 0.0001f)
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = false).batteryColorPreset)
        assertTrue(prefs.dualSignalEnabled())
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        assertFalse(prefs.dualSignalEnabled())
    }

    @Test
    fun partialLegacyDualBankDoesNotBorrowUnscopedDotStyles() {
        val prefs = VisualTestPreferences()
        val layout = ContentLayout.NETWORK_CENTER
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putFloat(
                visualModeKey(true, visualProfileKey(layout, WIFI_SIZE_SCALE_KEY)),
                0.87f,
            )
            .putString(DUAL_ROW_DOT_ARRANGEMENT_KEY, SignalDotArrangement.HORIZONTAL.persistedValue)
            .putString(SINGLE_ROW_DOT_STYLE_KEY, SignalDotArrangement.HORIZONTAL.persistedValue)
            .apply()

        val before = prefs.readVisualCfg(dual = true, layoutOverride = layout)
        assertEquals(SignalDotArrangement.BOTH_SIDES, before.dualRowDotArrangement)
        assertEquals(SignalDotArrangement.CURVED, before.singleRowDotArrangement)
        assertEquals(0.87f, before.wifiScale, 0f)

        migrateVisualPrefsIfNeeded(prefs)
        val after = prefs.readVisualCfg(dual = true, layoutOverride = layout)
        assertEquals(before.dualRowDotArrangement, after.dualRowDotArrangement)
        assertEquals(before.singleRowDotArrangement, after.singleRowDotArrangement)
        assertEquals(before.wifiScale, after.wifiScale, 0f)

        val unbanked = VisualTestPreferences()
        unbanked.edit().putString(
            SINGLE_ROW_DOT_STYLE_KEY,
            SignalDotArrangement.HORIZONTAL.persistedValue,
        ).apply()
        assertEquals(
            SignalDotArrangement.HORIZONTAL,
            unbanked.readVisualCfg(dual = false, layoutOverride = layout).singleRowDotArrangement,
        )
    }

    @Test
    fun oldBatteryCenteredOffsetIsInheritedUntilEachNewSliderIsSaved() {
        val prefs = VisualTestPreferences()
        val layout = ContentLayout.BATTERY_CENTER
        prefs.edit()
            .putString(visualModeKey(false, CONTENT_LAYOUT_KEY), layout.persistedValue)
            .putFloat(visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY), topOffsetYRaw(5f))
            .apply()

        val initial = prefs.readVisualCfg()
        assertEquals(5f, initial.wifiOffsetY, 0f)
        assertEquals(5f, initial.mobileTypeOffsetY, 0f)
        assertEquals(topOffsetYRaw(5f), initial.topOffsetY, 0f)

        prefs.edit()
            .putFloat(visualProfileKey(layout, WIFI_VERTICAL_OFFSET_KEY), -1f)
            .apply()
        assertEquals(-1f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(5f, prefs.readVisualCfg().mobileTypeOffsetY, 0f)

        val result = prefs.readVisualCfg().copy(mobileTypeOffsetY = 3f)
        prefs.edit().putVisualCfg(result).apply()
        assertEquals(-1f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(3f, prefs.readVisualCfg().mobileTypeOffsetY, 0f)
    }

    @Test
    fun legacyNetworkCenteredReadoutDoesNotAffectNewNetworkOffsets() {
        val prefs = VisualTestPreferences()
        prefs.edit()
            .putFloat(visualProfileKey(ContentLayout.NETWORK_CENTER, BATTERY_TOP_VERTICAL_OFFSET_KEY), topOffsetYRaw(6f))
            .apply()
        assertEquals(0f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(0f, prefs.readVisualCfg().mobileTypeOffsetY, 0f)
        assertEquals(topOffsetYRaw(6f), prefs.readVisualCfg().topOffsetY, 0f)
    }
}
