package com.chaners.guiyuan.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualPreferenceKeysTest {
    @Test
    fun acceptsLayoutGlobalAndLegacyKeys() {
        assertTrue(isVisualPreferenceKey(null)) // SharedPreferences.clear()
        assertTrue(isVisualPreferenceKey(CONTENT_LAYOUT_KEY))
        assertTrue(isVisualPreferenceKey(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY))
        assertTrue(isVisualPreferenceKey(BATTERY_TOP_TEXT_SCALE_KEY))
        assertTrue(isVisualPreferenceKey("network_center.battery_top_text_scale"))
        assertTrue(isVisualPreferenceKey("battery_center.battery_top_text_scale"))
        assertTrue(isVisualPreferenceKey("network_center.mobile_follows_battery_color"))
        assertTrue(isVisualPreferenceKey("battery_center.mobile_follows_battery_color"))
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
