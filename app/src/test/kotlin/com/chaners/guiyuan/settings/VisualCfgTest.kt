package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualCfgTest {
    @Test
    fun newGeometryControlsUseBoundedDefaults() {
        val settings = VisualCfg()
        assertEquals(1f, settings.combinedScale, 0.0001f)
        assertEquals(1f, settings.outerWeightScale, 0.0001f)
        assertEquals(1f, settings.wifiScale, 0.0001f)
        assertEquals(1f, settings.airplaneScale, 0.0001f)
        assertEquals(1f, settings.noSimScale, 0.0001f)
        assertEquals(1f, settings.mobileTypeScale, 0.0001f)
        assertEquals(900, settings.mobileTypeWeight)
        assertEquals(true, settings.controlCenterTintTransitionEnabled)

        val normalized =
            settings.copy(
                combinedScale = 9f,
                outerWeightScale = 9f,
                wifiScale = 9f,
                airplaneScale = 9f,
                noSimScale = 9f,
                mobileTypeScale = 9f,
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
        assertEquals(WIFI_SIZE_SCALE_MAX, normalized.wifiScale, 0.0001f)
        assertEquals(AIRPLANE_SIZE_SCALE_MAX, normalized.airplaneScale, 0.0001f)
        assertEquals(NO_SIM_SIZE_SCALE_MAX, normalized.noSimScale, 0.0001f)
        assertEquals(MOBILE_TYPE_SIZE_SCALE_MAX, normalized.mobileTypeScale, 0.0001f)
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
            VisualCfg().batteryColorPreset,
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
    fun recommendedPaletteUsesMutedSemanticDefaults() {
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
    fun hyperosPaletteUsesPinnedSystemUiSemanticDefaults() {
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
    fun iosStylePaletteUsesExpectedSemanticDefaults() {
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
    fun previousPhysicalPlusThreeIsTheNewUserFacingZero() {
        assertEquals(
            0f,
            topOffsetYUi(3f),
            0.0001f,
        )
        assertEquals(
            3f,
            topOffsetYRaw(0f),
            0.0001f,
        )
    }

    @Test
    fun userFacingOffsetRangeIsPlusMinusTenAroundPhysicalReference() {
        assertEquals(-10f, BATTERY_TOP_VERTICAL_OFFSET_UI_MIN, 0.0001f)
        assertEquals(10f, BATTERY_TOP_VERTICAL_OFFSET_UI_MAX, 0.0001f)
        assertEquals(
            -7f,
            topOffsetYRaw(-10f),
            0.0001f,
        )
        assertEquals(
            13f,
            topOffsetYRaw(10f),
            0.0001f,
        )
    }

    @Test
    fun offsetMappingClampsOnlyAtVisibleSliderEnds() {
        assertEquals(
            -10f,
            topOffsetYUi(-30f),
            0.0001f,
        )
        assertEquals(
            10f,
            topOffsetYUi(30f),
            0.0001f,
        )
    }

    @Test
    fun normalizedRuntimeOffsetUsesThePhysicalRangeBehindTheVisibleSlider() {
        val high =
            VisualCfg(
                topOffsetY = 30f,
            ).normalized()
        val low =
            VisualCfg(
                topOffsetY = -30f,
            ).normalized()

        assertEquals(13f, high.topOffsetY, 0.0001f)
        assertEquals(-7f, low.topOffsetY, 0.0001f)
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

    @Test
    fun newBatteryVisualControlsKeepRequestedDefaults() {
        val settings = VisualCfg()

        assertEquals(ContentLayout.NETWORK_CENTER, settings.layout)
        assertEquals(true, settings.topTextFollowsBatteryColor)
        assertEquals(true, settings.showTopChargingIcon)
        assertEquals(true, settings.topChargingIconFollowsBatteryColor)
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
    fun batteryCenteredProfileUsesRequestedTopDefaults() {
        assertEquals(
            1.4f,
            topTextUiScaleDefault(ContentLayout.BATTERY_CENTER),
            0.0001f,
        )
        assertEquals(
            1.2f,
            topChargingIconUiScaleDefault(ContentLayout.BATTERY_CENTER),
            0.0001f,
        )
        assertEquals(
            1.4f,
            topTextUiScale(
                topTextScaleDefault(ContentLayout.BATTERY_CENTER),
            ),
            0.0001f,
        )
        assertEquals(
            1.2f,
            topChargingIconUiScale(
                topChargingIconScaleDefault(ContentLayout.BATTERY_CENTER),
            ),
            0.0001f,
        )
        assertEquals(
            0.8f,
            mobileTypeScaleDefault(ContentLayout.BATTERY_CENTER),
            0.0001f,
        )
    }


    @Test
    fun directBatteryCenteredSettingsConstructionUsesProfileDefaults() {
        val settings =
            VisualCfg(
                layout = ContentLayout.BATTERY_CENTER,
            )

        assertEquals(
            1.4f,
            topTextUiScale(settings.topTextScale),
            0.0001f,
        )
        assertEquals(
            1.2f,
            topChargingIconUiScale(settings.topChargingIconScale),
            0.0001f,
        )
        assertEquals(0.8f, settings.mobileTypeScale, 0.0001f)
    }

    @Test
    fun networkCenteredProfileUsesRequestedTopDefaults() {
        assertEquals(
            1.2f,
            topTextUiScaleDefault(ContentLayout.NETWORK_CENTER),
            0.0001f,
        )
        assertEquals(
            1f,
            topChargingIconUiScaleDefault(ContentLayout.NETWORK_CENTER),
            0.0001f,
        )
        assertEquals(
            1f,
            mobileTypeScaleDefault(ContentLayout.NETWORK_CENTER),
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
            topTextUiScale(0f),
            0.0001f,
        )
        assertEquals(
            1.6f,
            topTextUiScale(Float.MAX_VALUE),
            0.0001f,
        )
        assertEquals(
            0.4f,
            topChargingIconUiScale(0f),
            0.0001f,
        )
        assertEquals(
            1.6f,
            topChargingIconUiScale(Float.MAX_VALUE),
            0.0001f,
        )
    }
}
