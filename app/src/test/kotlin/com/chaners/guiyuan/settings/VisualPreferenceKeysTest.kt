package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualPreferenceKeysTest {
    @Test
    fun acceptsLayoutGlobalAndLegacyKeys() {
        assertTrue(isVisualPreferenceKey(null)) // SharedPreferences.clear()
        assertTrue(isVisualPreferenceKey(CONTENT_LAYOUT_KEY))
        assertTrue(isVisualPreferenceKey(DUAL_SIGNAL_ENABLED_KEY))
        assertTrue(isVisualPreferenceKey(SHARED_CONTENT_STYLE_SCHEMA_KEY))
        assertTrue(isVisualPreferenceKey(MOBILE_SIGNAL_CENTERED_KEY))
        assertTrue(isVisualPreferenceKey(MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY))
        assertTrue(isVisualPreferenceKey(DUAL_PROFILES_INITIALIZED_KEY))
        assertTrue(isVisualPreferenceKey(visualModeKey(true, CONTENT_LAYOUT_KEY)))
        assertTrue(isVisualPreferenceKey(visualModeKey(true, BATTERY_COLOR_PRESET_KEY)))
        assertTrue(isVisualPreferenceKey(visualProfileKey(ContentLayout.NETWORK_CENTER, COMBINED_SCALE_KEY, true)))
        assertTrue(isVisualPreferenceKey(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY))
        assertTrue(isVisualPreferenceKey(BATTERY_TOP_TEXT_SCALE_KEY))
        assertTrue(isVisualPreferenceKey("network_center.battery_top_text_scale"))
        assertTrue(isVisualPreferenceKey("battery_center.battery_top_text_scale"))
        assertTrue(isVisualPreferenceKey("network_center.mobile_follows_battery_color"))
        assertTrue(isVisualPreferenceKey("battery_center.mobile_follows_battery_color"))
    }

    @Test
    fun sharedBatteryColorsStayInRuntimeAndBackupKeyScopes() {
        assertTrue(isVisualPreferenceKey(SHARED_BATTERY_COLORS_SCHEMA_KEY))
        assertEquals(1 + 2 * BatteryColorSlot.entries.size, BATTERY_COLOR_KEYS.size)
        for (key in BATTERY_COLOR_KEYS) {
            assertTrue("Missing global color key $key", isVisualPreferenceKey(key))
            assertTrue("Missing legacy dual key $key", isBackupVisualKey(visualModeKey(true, key)))
        }
    }

    @Test
    fun ignoresUnrelatedAndUnrecognizedProfileKeys() {
        assertFalse(isVisualPreferenceKey("diagnostics_level"))
        assertFalse(isVisualPreferenceKey("combined_status_enabled"))
        assertFalse(isVisualPreferenceKey("unknown.battery_top_text_scale"))
        assertFalse(isVisualPreferenceKey("network_center.unknown"))
        assertFalse(isVisualPreferenceKey("battery_center.battery_top_text_scale.extra"))
    }
}
