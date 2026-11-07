package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualCfgPersistenceKeysTest {
    @Test
    fun layoutVisualKeysAreSharedAcrossSimModes() {
        assertEquals("content_layout", visualModeKey(false, CONTENT_LAYOUT_KEY))
        assertEquals("dual.content_layout", visualModeKey(true, CONTENT_LAYOUT_KEY))
        assertEquals("network_center.combined_scale", visualProfileKey(ContentLayout.NETWORK_CENTER, COMBINED_SCALE_KEY))
        assertEquals(false, VisualCfg().dualSignal)
    }

    @Test
    fun clearNotificationParticipatesInVisualRuntimeSync() {
        assertEquals(true, isVisualPreferenceKey(null))
    }

    @Test
    fun allNewVisualKeysParticipateInRuntimeSync() {
        val keys =
            listOf(
                CONTENT_LAYOUT_KEY,
                BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY,
                BATTERY_TOP_CHARGING_ICON_ENABLED_KEY,
                BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY,
                CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY,
                WIFI_VERTICAL_OFFSET_KEY,
                MOBILE_TYPE_VERTICAL_OFFSET_KEY,
            )

        keys.forEach { key ->
            assertEquals(true, isVisualPreferenceKey(key))
        }
    }


    @Test
    fun layoutProfilesUseIndependentPersistedKeys() {
        assertEquals(
            "network_center.battery_top_text_scale",
            visualProfileKey(
                ContentLayout.NETWORK_CENTER,
                BATTERY_TOP_TEXT_SCALE_KEY,
            ),
        )
        assertEquals(
            "battery_center.battery_top_text_scale",
            visualProfileKey(
                ContentLayout.BATTERY_CENTER,
                BATTERY_TOP_TEXT_SCALE_KEY,
            ),
        )
    }

    @Test
    fun networkStateSizeControlsUseIndependentLayoutProfileKeys() {
        assertEquals(
            "network_center.airplane_size_scale",
            visualProfileKey(
                ContentLayout.NETWORK_CENTER,
                AIRPLANE_SIZE_SCALE_KEY,
            ),
        )
        assertEquals(
            "battery_center.airplane_size_scale",
            visualProfileKey(
                ContentLayout.BATTERY_CENTER,
                AIRPLANE_SIZE_SCALE_KEY,
            ),
        )
        assertEquals(
            "network_center.no_sim_size_scale",
            visualProfileKey(
                ContentLayout.NETWORK_CENTER,
                NO_SIM_SIZE_SCALE_KEY,
            ),
        )
        assertEquals(
            "battery_center.no_sim_size_scale",
            visualProfileKey(
                ContentLayout.BATTERY_CENTER,
                NO_SIM_SIZE_SCALE_KEY,
            ),
        )
    }

    @Test
    fun profileKeysParticipateInRuntimeSync() {
        ContentLayout.entries.forEach { layout ->
            listOf(
                MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
                CENTER_FOLLOWS_BATTERY_COLOR_KEY,
                BATTERY_TOP_READOUT_ENABLED_KEY,
                BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY,
                BATTERY_TOP_CHARGING_ICON_ENABLED_KEY,
                BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY,
                BATTERY_TOP_TEXT_SCALE_KEY,
                BATTERY_TOP_TEXT_WEIGHT_KEY,
                BATTERY_TOP_VERTICAL_OFFSET_KEY,
                BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                COMBINED_SCALE_KEY,
                OUTER_WEIGHT_SCALE_KEY,
                WIFI_SIZE_SCALE_KEY,
                AIRPLANE_SIZE_SCALE_KEY,
                NO_SIM_SIZE_SCALE_KEY,
                MOBILE_TYPE_SIZE_SCALE_KEY,
                MOBILE_TYPE_WEIGHT_KEY,
            ).forEach { baseKey ->
                assertEquals(
                    true,
                    isVisualPreferenceKey(
                        visualProfileKey(layout, baseKey),
                    ),
                )
            }
        }
    }

    @Test
    fun globalBatteryColorKeysParticipateInRuntimeSync() {
        listOf(
            BATTERY_COLOR_PRESET_KEY,
            BATTERY_COLOR_MODE_NORMAL_KEY,
            BATTERY_COLOR_MODE_POWER_SAVE_KEY,
            BATTERY_COLOR_MODE_PERFORMANCE_KEY,
            BATTERY_COLOR_MODE_SUPER_POWER_SAVE_KEY,
            BATTERY_COLOR_MODE_CHARGING_KEY,
            BATTERY_COLOR_MODE_LOW_KEY,
            BATTERY_COLOR_NORMAL_KEY,
            BATTERY_COLOR_POWER_SAVE_KEY,
            BATTERY_COLOR_PERFORMANCE_KEY,
            BATTERY_COLOR_SUPER_POWER_SAVE_KEY,
            BATTERY_COLOR_CHARGING_KEY,
            BATTERY_COLOR_LOW_KEY,
        ).forEach { key ->
            assertEquals(true, isVisualPreferenceKey(key))
        }
    }
}
