package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualPreferenceKeysTest {
    @Test
    fun acceptsThreeLayoutsGlobalAndProfileKeys() {
        assertTrue(isVisualPreferenceKey(null)) // SharedPreferences.clear()
        assertTrue(isVisualPreferenceKey(CONTENT_LAYOUT_KEY))
        assertTrue(isVisualPreferenceKey(DUAL_SIGNAL_ENABLED_KEY))
        assertTrue(isVisualPreferenceKey(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY))
        assertFalse(isVisualPreferenceKey("battery_avoidance_mode"))
        assertFalse(isVisualPreferenceKey("mobile_center.battery_avoidance_mode"))
        assertTrue(isVisualPreferenceKey(visualProfileKey(ContentLayout.MOBILE_CENTER, SINGLE_ROW_DOT_STYLE_KEY)))
        assertFalse(isVisualPreferenceKey(visualProfileKey(ContentLayout.MOBILE_CENTER, LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY)))
        assertTrue(isVisualPreferenceKey(visualProfileKey(ContentLayout.MOBILE_CENTER, MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY)))
        assertTrue(isVisualPreferenceKey(DUAL_PROFILES_INITIALIZED_KEY))
        assertTrue(isVisualPreferenceKey(visualProfileKey(ContentLayout.NETWORK_CENTER, COMBINED_SCALE_KEY)))
        assertTrue(isVisualPreferenceKey(visualProfileKey(ContentLayout.NETWORK_CENTER, LEFT_RIGHT_HIDE_IDLE_ACTIVITY_KEY)))
        assertFalse(isVisualPreferenceKey(visualProfileKey(ContentLayout.NETWORK_CENTER, LEGACY_LEFT_RIGHT_SHOW_IDLE_ACTIVITY_KEY)))
        assertTrue(isVisualPreferenceKey(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY))
        assertTrue(isVisualPreferenceKey(BATTERY_TOP_TEXT_SCALE_KEY))
        assertTrue(isVisualPreferenceKey("mobile_center.battery_top_text_scale"))
        assertTrue(isVisualPreferenceKey("network_center.battery_top_text_scale"))
        assertTrue(isVisualPreferenceKey("battery_center.battery_top_text_scale"))
        assertTrue(isVisualPreferenceKey("network_center.mobile_follows_battery_color"))
        assertTrue(isVisualPreferenceKey("battery_center.mobile_follows_battery_color"))
    }

    @Test
    fun sharedBatteryColorsStayInRuntimeAndBackupKeyScopes() {
        assertEquals(1 + 2 * BatteryColorSlot.entries.size, BATTERY_COLOR_KEYS.size)
        for (key in BATTERY_COLOR_KEYS) {
            assertTrue("Missing global color key $key", isVisualPreferenceKey(key))
            assertFalse("Old dual color bank must stay retired $key", isBackupVisualKey(visualModeKey(true, key)))
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
