package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusVisualSettingsTest {
    @Test
    fun newGeometryControlsUseBoundedDefaults() {
        val settings = CombinedStatusVisualSettings()
        assertEquals(1f, settings.combinedScale, 0.0001f)
        assertEquals(1f, settings.outerWeightScale, 0.0001f)
        assertEquals(1f, settings.wifiSizeScale, 0.0001f)
        assertEquals(1f, settings.airplaneSizeScale, 0.0001f)
        assertEquals(1f, settings.noSimSizeScale, 0.0001f)
        assertEquals(1f, settings.mobileTypeSizeScale, 0.0001f)
        assertEquals(900, settings.mobileTypeWeight)
        assertEquals(true, settings.controlCenterTintTransitionEnabled)

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
            CombinedStatusBatteryColorPreset.HYPEROS,
            CombinedStatusVisualSettings().batteryColorPreset,
        )
        assertEquals(
            CombinedStatusBatteryColorPreset.HYPEROS,
            CombinedStatusBatteryColorPreset.fromPersisted(null),
        )
    }

    @Test
    fun missingPresetAlwaysDefaultsToHyperos() {
        assertEquals(
            CombinedStatusBatteryColorPreset.HYPEROS,
            batteryColorPresetForMissingKey(hadPreviousVisualSchema = true),
        )
        assertEquals(
            CombinedStatusBatteryColorPreset.HYPEROS,
            batteryColorPresetForMissingKey(hadPreviousVisualSchema = false),
        )
    }

    @Test
    fun legacyStoredCustomColorInfersCustomMode() {
        assertEquals(
            CombinedStatusBatteryColorMode.CUSTOM,
            batteryColorModeFromPersisted(
                persistedMode = null,
                hasStoredColor = true,
            ),
        )
        assertEquals(
            CombinedStatusBatteryColorMode.PRESET,
            batteryColorModeFromPersisted(
                persistedMode = null,
                hasStoredColor = false,
            ),
        )
        assertEquals(
            CombinedStatusBatteryColorMode.FOLLOW_SYSTEM,
            batteryColorModeFromPersisted(
                persistedMode = "follow_system",
                hasStoredColor = true,
            ),
        )
    }

    @Test
    fun recommendedPaletteUsesMutedSemanticDefaults() {
        assertEquals(0xFF3FA760.toInt(), CombinedStatusRecommendedBatteryPalette.CHARGING)
        assertEquals(0xFFD5A623.toInt(), CombinedStatusRecommendedBatteryPalette.POWER_SAVE)
        assertEquals(0xFF4A7FC1.toInt(), CombinedStatusRecommendedBatteryPalette.PERFORMANCE)
        assertEquals(0xFFD8752C.toInt(), CombinedStatusRecommendedBatteryPalette.SUPER_POWER_SAVE)
        assertEquals(0xFFD64A4A.toInt(), CombinedStatusRecommendedBatteryPalette.LOW)
        assertEquals(
            null,
            CombinedStatusRecommendedBatteryPalette.colorFor(
                CombinedStatusBatteryColorSlot.NORMAL,
            ),
        )
    }

    @Test
    fun hyperosPaletteUsesPinnedSystemUiSemanticDefaults() {
        assertEquals(0xFF1DCD3A.toInt(), CombinedStatusHyperOsBatteryPalette.CHARGING)
        assertEquals(0xFFFF9F05.toInt(), CombinedStatusHyperOsBatteryPalette.POWER_SAVE)
        assertEquals(0xFF3482FF.toInt(), CombinedStatusHyperOsBatteryPalette.PERFORMANCE)
        assertEquals(0xFFFF9F05.toInt(), CombinedStatusHyperOsBatteryPalette.SUPER_POWER_SAVE)
        assertEquals(0xFFFA382E.toInt(), CombinedStatusHyperOsBatteryPalette.LOW)
        assertEquals(
            null,
            CombinedStatusHyperOsBatteryPalette.colorFor(CombinedStatusBatteryColorSlot.NORMAL),
        )
    }

    @Test
    fun iosStylePaletteUsesExpectedSemanticDefaults() {
        assertEquals(0xFF34C759.toInt(), CombinedStatusIosStyleBatteryPalette.CHARGING)
        assertEquals(0xFFFFCC00.toInt(), CombinedStatusIosStyleBatteryPalette.POWER_SAVE)
        assertEquals(0xFF007AFF.toInt(), CombinedStatusIosStyleBatteryPalette.PERFORMANCE)
        assertEquals(0xFFFF9500.toInt(), CombinedStatusIosStyleBatteryPalette.SUPER_POWER_SAVE)
        assertEquals(0xFFFF3B30.toInt(), CombinedStatusIosStyleBatteryPalette.LOW)
        assertEquals(
            null,
            CombinedStatusIosStyleBatteryPalette.colorFor(CombinedStatusBatteryColorSlot.NORMAL),
        )
    }

    @Test
    fun customColorOverridesAreForcedOpaque() {
        val overrides =
            CombinedStatusBatteryColorOverrides().withColor(
                CombinedStatusBatteryColorSlot.CHARGING,
                0x0034C759,
            )
        assertEquals(0xFF34C759.toInt(), overrides.charging)
    }

    @Test
    fun previousPhysicalPlusThreeIsTheNewUserFacingZero() {
        assertEquals(
            0f,
            batteryTopVerticalOffsetUi(3f),
            0.0001f,
        )
        assertEquals(
            3f,
            batteryTopVerticalOffsetRaw(0f),
            0.0001f,
        )
    }

    @Test
    fun userFacingOffsetRangeIsPlusMinusTenAroundPhysicalReference() {
        assertEquals(-10f, BATTERY_TOP_VERTICAL_OFFSET_UI_MIN, 0.0001f)
        assertEquals(10f, BATTERY_TOP_VERTICAL_OFFSET_UI_MAX, 0.0001f)
        assertEquals(
            -7f,
            batteryTopVerticalOffsetRaw(-10f),
            0.0001f,
        )
        assertEquals(
            13f,
            batteryTopVerticalOffsetRaw(10f),
            0.0001f,
        )
    }

    @Test
    fun offsetMappingClampsOnlyAtVisibleSliderEnds() {
        assertEquals(
            -10f,
            batteryTopVerticalOffsetUi(-30f),
            0.0001f,
        )
        assertEquals(
            10f,
            batteryTopVerticalOffsetUi(30f),
            0.0001f,
        )
    }

    @Test
    fun normalizedRuntimeOffsetUsesThePhysicalRangeBehindTheVisibleSlider() {
        val high =
            CombinedStatusVisualSettings(
                batteryTopVerticalOffset = 30f,
            ).normalized()
        val low =
            CombinedStatusVisualSettings(
                batteryTopVerticalOffset = -30f,
            ).normalized()

        assertEquals(13f, high.batteryTopVerticalOffset, 0.0001f)
        assertEquals(-7f, low.batteryTopVerticalOffset, 0.0001f)
    }


    @Test
    fun clearNotificationParticipatesInVisualRuntimeSync() {
        assertEquals(true, isCombinedStatusVisualPreferenceKey(null))
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
            )

        keys.forEach { key ->
            assertEquals(true, isCombinedStatusVisualPreferenceKey(key))
        }
    }


    @Test
    fun layoutProfilesUseIndependentPersistedKeys() {
        assertEquals(
            "network_center.battery_top_text_scale",
            combinedStatusProfileKey(
                CombinedStatusContentLayout.NETWORK_CENTER,
                BATTERY_TOP_TEXT_SCALE_KEY,
            ),
        )
        assertEquals(
            "battery_center.battery_top_text_scale",
            combinedStatusProfileKey(
                CombinedStatusContentLayout.BATTERY_CENTER,
                BATTERY_TOP_TEXT_SCALE_KEY,
            ),
        )
    }

    @Test
    fun networkStateSizeControlsUseIndependentLayoutProfileKeys() {
        assertEquals(
            "network_center.airplane_size_scale",
            combinedStatusProfileKey(
                CombinedStatusContentLayout.NETWORK_CENTER,
                AIRPLANE_SIZE_SCALE_KEY,
            ),
        )
        assertEquals(
            "battery_center.airplane_size_scale",
            combinedStatusProfileKey(
                CombinedStatusContentLayout.BATTERY_CENTER,
                AIRPLANE_SIZE_SCALE_KEY,
            ),
        )
        assertEquals(
            "network_center.no_sim_size_scale",
            combinedStatusProfileKey(
                CombinedStatusContentLayout.NETWORK_CENTER,
                NO_SIM_SIZE_SCALE_KEY,
            ),
        )
        assertEquals(
            "battery_center.no_sim_size_scale",
            combinedStatusProfileKey(
                CombinedStatusContentLayout.BATTERY_CENTER,
                NO_SIM_SIZE_SCALE_KEY,
            ),
        )
    }

    @Test
    fun profileKeysParticipateInRuntimeSync() {
        CombinedStatusContentLayout.entries.forEach { layout ->
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
                    isCombinedStatusVisualPreferenceKey(
                        combinedStatusProfileKey(layout, baseKey),
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
            assertEquals(true, isCombinedStatusVisualPreferenceKey(key))
        }
    }

    @Test
    fun newBatteryVisualControlsKeepRequestedDefaults() {
        val settings = CombinedStatusVisualSettings()

        assertEquals(CombinedStatusContentLayout.NETWORK_CENTER, settings.contentLayout)
        assertEquals(true, settings.batteryTopTextFollowsBatteryColor)
        assertEquals(true, settings.batteryTopChargingIconEnabled)
        assertEquals(true, settings.batteryTopChargingIconFollowsBatteryColor)
    }

    @Test
    fun persistedLayoutFallsBackToNetworkCenter() {
        assertEquals(
            CombinedStatusContentLayout.NETWORK_CENTER,
            CombinedStatusContentLayout.fromPersisted("unknown"),
        )
        assertEquals(
            CombinedStatusContentLayout.BATTERY_CENTER,
            CombinedStatusContentLayout.fromPersisted("battery_center"),
        )
    }


    @Test
    fun batteryCenteredProfileUsesRequestedTopDefaults() {
        assertEquals(
            1.4f,
            batteryTopTextUiScaleDefault(CombinedStatusContentLayout.BATTERY_CENTER),
            0.0001f,
        )
        assertEquals(
            1.2f,
            batteryTopChargingIconUiScaleDefault(CombinedStatusContentLayout.BATTERY_CENTER),
            0.0001f,
        )
        assertEquals(
            1.4f,
            batteryTopTextUiScale(
                batteryTopTextScaleDefault(CombinedStatusContentLayout.BATTERY_CENTER),
            ),
            0.0001f,
        )
        assertEquals(
            1.2f,
            batteryTopChargingIconUiScale(
                batteryTopChargingIconScaleDefault(CombinedStatusContentLayout.BATTERY_CENTER),
            ),
            0.0001f,
        )
        assertEquals(
            0.8f,
            mobileTypeSizeScaleDefault(CombinedStatusContentLayout.BATTERY_CENTER),
            0.0001f,
        )
    }


    @Test
    fun directBatteryCenteredSettingsConstructionUsesProfileDefaults() {
        val settings =
            CombinedStatusVisualSettings(
                contentLayout = CombinedStatusContentLayout.BATTERY_CENTER,
            )

        assertEquals(
            1.4f,
            batteryTopTextUiScale(settings.batteryTopTextScale),
            0.0001f,
        )
        assertEquals(
            1.2f,
            batteryTopChargingIconUiScale(settings.batteryTopChargingIconScale),
            0.0001f,
        )
        assertEquals(0.8f, settings.mobileTypeSizeScale, 0.0001f)
    }

    @Test
    fun networkCenteredProfileUsesRequestedTopDefaults() {
        assertEquals(
            1.2f,
            batteryTopTextUiScaleDefault(CombinedStatusContentLayout.NETWORK_CENTER),
            0.0001f,
        )
        assertEquals(
            1f,
            batteryTopChargingIconUiScaleDefault(CombinedStatusContentLayout.NETWORK_CENTER),
            0.0001f,
        )
        assertEquals(
            1f,
            mobileTypeSizeScaleDefault(CombinedStatusContentLayout.NETWORK_CENTER),
            0.0001f,
        )
    }

    @Test
    fun batteryTopScaleRangesAreFortyToOneHundredSixtyPercent() {
        assertEquals(0.4f, BATTERY_TOP_TEXT_UI_SCALE_MIN, 0.0001f)
        assertEquals(1.6f, BATTERY_TOP_TEXT_UI_SCALE_MAX, 0.0001f)
        assertEquals(0.4f, BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN, 0.0001f)
        assertEquals(1.6f, BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX, 0.0001f)

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
