package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualSettingsTest {
    @Test
    fun newGeometryControlsUseBoundedDefaults() {
        val settings = VisualSettings()
        assertEquals(1f, settings.combinedScale, 0.0001f)
        assertEquals(1f, settings.outerWeightScale, 0.0001f)
        assertEquals(1f, settings.wifiSizeScale, 0.0001f)
        assertEquals(1f, settings.airplaneSizeScale, 0.0001f)
        assertEquals(1f, settings.noSimSizeScale, 0.0001f)
        assertEquals(1f, settings.mobileTypeSizeScale, 0.0001f)
        assertEquals(900, settings.mobileTypeWeight)
        assertEquals(true, settings.ccTintTransitionEnabled)

        val normalized =
            settings.copy(
                combinedScale = 9f,
                outerWeightScale = 9f,
                wifiSizeScale = 9f,
                airplaneSizeScale = 9f,
                noSimSizeScale = 9f,
                mobileTypeSizeScale = 9f,
                mobileTypeWeight = 5000,
            ).normalized()
        assertEquals(COMBINED_SCALE_DEFAULT, COMBINED_SCALE_MAX, 0.0001f)
        assertEquals(COMBINED_SCALE_MAX, normalized.combinedScale, 0.0001f)
        assertEquals(
            COMBINED_SCALE_MIN,
            settings.copy(combinedScale = -1f).normalized().combinedScale,
            0.0001f,
        )
        assertEquals(OUTER_WEIGHT_SCALE_MAX, normalized.outerWeightScale, 0.0001f)
        assertEquals(WIFI_SIZE_SCALE_MAX, normalized.wifiSizeScale, 0.0001f)
        assertEquals(AIRPLANE_SIZE_SCALE_MAX, normalized.airplaneSizeScale, 0.0001f)
        assertEquals(NO_SIM_SIZE_SCALE_MAX, normalized.noSimSizeScale, 0.0001f)
        assertEquals(MOBILE_TYPE_SIZE_SCALE_MAX, normalized.mobileTypeSizeScale, 0.0001f)
        assertEquals(MOBILE_TYPE_WEIGHT_MAX, normalized.mobileTypeWeight)
        assertEquals(400, MOBILE_TYPE_WEIGHT_MIN)
        assertEquals(900, MOBILE_TYPE_WEIGHT_DEFAULT)
        assertEquals(1400, MOBILE_TYPE_WEIGHT_MAX)
        assertEquals(0.60f, COMBINED_SCALE_MIN, 0.0001f)
        assertEquals(0.40f, WIFI_SIZE_SCALE_MIN, 0.0001f)
        assertEquals(WIFI_SIZE_SCALE_MIN, AIRPLANE_SIZE_SCALE_MIN, 0.0001f)
        assertEquals(WIFI_SIZE_SCALE_MAX, AIRPLANE_SIZE_SCALE_MAX, 0.0001f)
        assertEquals(WIFI_SIZE_SCALE_MIN, NO_SIM_SIZE_SCALE_MIN, 0.0001f)
        assertEquals(WIFI_SIZE_SCALE_MAX, NO_SIM_SIZE_SCALE_MAX, 0.0001f)
        assertEquals(0.40f, MOBILE_TYPE_SIZE_SCALE_MIN, 0.0001f)
    }

    @Test
    fun hyperosIsTheDefaultPreset() {
        assertEquals(
            BatteryColorPreset.HYPEROS,
            VisualSettings().batteryColorPreset,
        )
        assertEquals(
            BatteryColorPreset.HYPEROS,
            BatteryColorPreset.fromPersisted(null),
        )
    }

    @Test
    fun missingPresetAlwaysDefaultsToHyperos() {
        assertEquals(
            BatteryColorPreset.HYPEROS,
            batteryColorPresetForMissingKey(hadPreviousVisualSchema = true),
        )
        assertEquals(
            BatteryColorPreset.HYPEROS,
            batteryColorPresetForMissingKey(hadPreviousVisualSchema = false),
        )
    }

    @Test
    fun legacyStoredCustomColorInfersCustomMode() {
        assertEquals(
            BatteryColorMode.CUSTOM,
            batteryColorModeFromPersisted(
                persistedMode = null,
                hasStoredColor = true,
            ),
        )
        assertEquals(
            BatteryColorMode.PRESET,
            batteryColorModeFromPersisted(
                persistedMode = null,
                hasStoredColor = false,
            ),
        )
        assertEquals(
            BatteryColorMode.FOLLOW_SYSTEM,
            batteryColorModeFromPersisted(
                persistedMode = "follow_system",
                hasStoredColor = true,
            ),
        )
    }

    @Test
    fun recommendedPaletteUsesMutedDefaults() {
        assertEquals(0xFF3FA760.toInt(), RecommendedBatteryPalette.CHARGING)
        assertEquals(0xFFD5A623.toInt(), RecommendedBatteryPalette.POWER_SAVE)
        assertEquals(0xFF4A7FC1.toInt(), RecommendedBatteryPalette.PERFORMANCE)
        assertEquals(0xFFD8752C.toInt(), RecommendedBatteryPalette.SUPER_POWER_SAVE)
        assertEquals(0xFFD64A4A.toInt(), RecommendedBatteryPalette.LOW)
        assertEquals(
            null,
            RecommendedBatteryPalette.colorFor(
                BatteryColorSlot.NORMAL,
            ),
        )
    }

    @Test
    fun hyperOsPaletteMatchesSysUi() {
        assertEquals(0xFF1DCD3A.toInt(), HyperOsBatteryPalette.CHARGING)
        assertEquals(0xFFFF9F05.toInt(), HyperOsBatteryPalette.POWER_SAVE)
        assertEquals(0xFF3482FF.toInt(), HyperOsBatteryPalette.PERFORMANCE)
        assertEquals(0xFFFF9F05.toInt(), HyperOsBatteryPalette.SUPER_POWER_SAVE)
        assertEquals(0xFFFA382E.toInt(), HyperOsBatteryPalette.LOW)
        assertEquals(
            null,
            HyperOsBatteryPalette.colorFor(BatteryColorSlot.NORMAL),
        )
    }

    @Test
    fun iosPaletteMatchesDefaults() {
        assertEquals(0xFF34C759.toInt(), IosStyleBatteryPalette.CHARGING)
        assertEquals(0xFFFFCC00.toInt(), IosStyleBatteryPalette.POWER_SAVE)
        assertEquals(0xFF007AFF.toInt(), IosStyleBatteryPalette.PERFORMANCE)
        assertEquals(0xFFFF9500.toInt(), IosStyleBatteryPalette.SUPER_POWER_SAVE)
        assertEquals(0xFFFF3B30.toInt(), IosStyleBatteryPalette.LOW)
        assertEquals(
            null,
            IosStyleBatteryPalette.colorFor(BatteryColorSlot.NORMAL),
        )
    }

    @Test
    fun customColorOverridesAreForcedOpaque() {
        val overrides =
            BatteryColorOverrides().withColor(
                BatteryColorSlot.CHARGING,
                0x0034C759,
            )
        assertEquals(0xFF34C759.toInt(), overrides.charging)
    }

    @Test
    fun legacyOffsetMapsToZero() {
        assertEquals(
            0f,
            topOffsetUi(3f),
            0.0001f,
        )
        assertEquals(
            3f,
            topOffsetRaw(0f),
            0.0001f,
        )
    }

    @Test
    fun visibleOffsetRangeIsPlusMinusTen() {
        assertEquals(-10f, TOP_OFFSET_UI_MIN, 0.0001f)
        assertEquals(10f, TOP_OFFSET_UI_MAX, 0.0001f)
        assertEquals(
            -7f,
            topOffsetRaw(-10f),
            0.0001f,
        )
        assertEquals(
            13f,
            topOffsetRaw(10f),
            0.0001f,
        )
    }

    @Test
    fun offsetClampsAtSliderEnds() {
        assertEquals(
            -10f,
            topOffsetUi(-30f),
            0.0001f,
        )
        assertEquals(
            10f,
            topOffsetUi(30f),
            0.0001f,
        )
    }

    @Test
    fun runtimeOffsetUsesPhysicalRange() {
        val high =
            VisualSettings(
                topOffset = 30f,
            ).normalized()
        val low =
            VisualSettings(
                topOffset = -30f,
            ).normalized()

        assertEquals(13f, high.topOffset, 0.0001f)
        assertEquals(-7f, low.topOffset, 0.0001f)
    }


    @Test
    fun clearNotificationSyncsVisuals() {
        assertEquals(true, isVisualPreferenceKey(null))
    }

    @Test
    fun allNewVisualKeysParticipateInRuntimeSync() {
        val keys =
            listOf(
                CONTENT_LAYOUT_KEY,
                TOP_TEXT_FOLLOWS_BATTERY_KEY,
                CHARGING_ICON_ENABLED_KEY,
                CHARGING_ICON_FOLLOWS_BATTERY_KEY,
                CC_TINT_TRANSITION_KEY,
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
                TOP_TEXT_SCALE_KEY,
            ),
        )
        assertEquals(
            "battery_center.battery_top_text_scale",
            visualProfileKey(
                ContentLayout.BATTERY_CENTER,
                TOP_TEXT_SCALE_KEY,
            ),
        )
    }

    @Test
    fun networkSizesUseProfileKeys() {
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
                TOP_READOUT_KEY,
                TOP_TEXT_FOLLOWS_BATTERY_KEY,
                CHARGING_ICON_ENABLED_KEY,
                CHARGING_ICON_FOLLOWS_BATTERY_KEY,
                TOP_TEXT_SCALE_KEY,
                TOP_TEXT_WEIGHT_KEY,
                TOP_OFFSET_KEY,
                CHARGING_ICON_SCALE_KEY,
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
    fun batteryColorKeysSyncRuntime() {
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

    @Test
    fun batteryControlsKeepDefaults() {
        val settings = VisualSettings()

        assertEquals(ContentLayout.NETWORK_CENTER, settings.contentLayout)
        assertEquals(true, settings.topTextFollowsBattery)
        assertEquals(true, settings.chargingIconEnabled)
        assertEquals(true, settings.chargingIconFollowsBattery)
    }

    @Test
    fun persistedLayoutFallsBackToNetworkCenter() {
        assertEquals(
            ContentLayout.NETWORK_CENTER,
            ContentLayout.fromPersisted("unknown"),
        )
        assertEquals(
            ContentLayout.BATTERY_CENTER,
            ContentLayout.fromPersisted("battery_center"),
        )
    }


    @Test
    fun batteryProfileUsesTopDefaults() {
        assertEquals(
            1.4f,
            topTextUiDefault(ContentLayout.BATTERY_CENTER),
            0.0001f,
        )
        assertEquals(
            1.2f,
            chargingIconUiDefault(ContentLayout.BATTERY_CENTER),
            0.0001f,
        )
        assertEquals(
            1.4f,
            batteryTopTextUiScale(
                topTextScaleDefault(ContentLayout.BATTERY_CENTER),
            ),
            0.0001f,
        )
        assertEquals(
            1.2f,
            batteryTopChargingIconUiScale(
                chargingIconScaleDefault(ContentLayout.BATTERY_CENTER),
            ),
            0.0001f,
        )
        assertEquals(
            0.8f,
            mobileTypeSizeScaleDefault(ContentLayout.BATTERY_CENTER),
            0.0001f,
        )
    }


    @Test
    fun batterySettingsUseProfileDefaults() {
        val settings =
            VisualSettings(
                contentLayout = ContentLayout.BATTERY_CENTER,
            )

        assertEquals(
            1.4f,
            batteryTopTextUiScale(settings.batteryTopTextScale),
            0.0001f,
        )
        assertEquals(
            1.2f,
            batteryTopChargingIconUiScale(settings.chargingIconScale),
            0.0001f,
        )
        assertEquals(0.8f, settings.mobileTypeSizeScale, 0.0001f)
    }

    @Test
    fun networkProfileUsesTopDefaults() {
        assertEquals(
            1.2f,
            topTextUiDefault(ContentLayout.NETWORK_CENTER),
            0.0001f,
        )
        assertEquals(
            1f,
            chargingIconUiDefault(ContentLayout.NETWORK_CENTER),
            0.0001f,
        )
        assertEquals(
            1f,
            mobileTypeSizeScaleDefault(ContentLayout.NETWORK_CENTER),
            0.0001f,
        )
    }

    @Test
    fun topScaleRangeIsFortyTo160() {
        assertEquals(0.4f, TOP_TEXT_UI_MIN, 0.0001f)
        assertEquals(1.6f, TOP_TEXT_UI_MAX, 0.0001f)
        assertEquals(0.4f, CHARGING_ICON_UI_MIN, 0.0001f)
        assertEquals(1.6f, CHARGING_ICON_UI_MAX, 0.0001f)

        assertEquals(
            0.4f,
            batteryTopTextUiScale(0f),
            0.0001f,
        )
        assertEquals(
            1.6f,
            batteryTopTextUiScale(Float.MAX_VALUE),
            0.0001f,
        )
        assertEquals(
            0.4f,
            batteryTopChargingIconUiScale(0f),
            0.0001f,
        )
        assertEquals(
            1.6f,
            batteryTopChargingIconUiScale(Float.MAX_VALUE),
            0.0001f,
        )
    }
}
