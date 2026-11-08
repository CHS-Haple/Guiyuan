package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.serialization.json.Json
import org.junit.Test

class VisualCfgTest {

    @Test
    fun invalidSingleRowTwoSidedStyleNormalizesToCurved() {
        val invalid = VisualCfg(singleRowDotArrangement = SignalDotArrangement.BOTH_SIDES)
        assertEquals(SignalDotArrangement.CURVED, invalid.normalized().singleRowDotArrangement)
        assertEquals(SignalDotArrangement.BOTH_SIDES, invalid.dualRowDotArrangement)
    }

    @Test
    fun bothSidesKeepsReleasedStyleWireIdentity() {
        assertEquals("left_right", SignalDotArrangement.BOTH_SIDES.persistedValue)
        assertEquals(SignalDotArrangement.BOTH_SIDES, SignalDotArrangement.fromPersisted("left_right"))
        assertEquals(
            "\"LEFT_RIGHT\"",
            Json.encodeToString(SignalDotArrangement.serializer(), SignalDotArrangement.BOTH_SIDES),
        )
    }

    @Test
    fun finiteBoundPreservesValidInputsAndRejectsNonFinite() {
        for (invalid in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(1f, invalid.finiteIn(0.4f, 1.25f, 1f), 0f)
        }
        assertEquals(0.4f, (-2f).finiteIn(0.4f, 1.25f, 1f), 0f)
        assertEquals(1.25f, 2f.finiteIn(0.4f, 1.25f, 1f), 0f)
        assertEquals(0.8f, 0.8f.finiteIn(0.4f, 1.25f, 1f), 0f)
        for (layout in ContentLayout.entries) {
            assertEquals(
                topTextUiScaleDefault(layout),
                Float.NaN.finiteIn(
                    BATTERY_TOP_TEXT_UI_SCALE_MIN,
                    BATTERY_TOP_TEXT_UI_SCALE_MAX,
                    topTextUiScaleDefault(layout),
                ),
                0f,
            )
            assertEquals(
                topChargingIconUiScaleDefault(layout),
                Float.NaN.finiteIn(
                    BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN,
                    BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX,
                    topChargingIconUiScaleDefault(layout),
                ),
                0f,
            )
        }
    }

    @Test
    fun newGeometryControlsUseBoundedDefaults() {
        val settings = VisualCfg()
        assertEquals(1f, settings.combinedScale, 0.0001f)
        assertEquals(1f, settings.outerWeightScale, 0.0001f)
        assertEquals(1f, settings.wifiScale, 0.0001f)
        assertEquals(0.85f, BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT, 0.0001f)
        assertEquals(BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT, settings.bothSidesActivityScale, 0.0001f)
        assertEquals(1f, settings.copy(bothSidesActivityScale = 1f).normalized().bothSidesActivityScale, 0.0001f)
        assertEquals(0f, settings.wifiOffsetY, 0f)
        assertEquals(0f, settings.mobileTypeOffsetY, 0f)
        assertEquals(0f, settings.activeSignalDotOffsetY, 0f)
        assertEquals(1f, settings.airplaneScale, 0.0001f)
        assertEquals(1f, settings.noSimScale, 0.0001f)
        assertEquals(1f, settings.mobileTypeScale, 0.0001f)
        assertEquals(600, settings.mobileTypeWeight)
        assertEquals(600, settings.topTextWeight)
        assertEquals(1, BATTERY_TOP_TEXT_WEIGHT_MIN)
        assertEquals(1000, BATTERY_TOP_TEXT_WEIGHT_MAX)
        assertEquals(true, settings.ccTintTransition)
        assertEquals(true, settings.fillFollowsRetract)
        assertEquals(false, settings.showTopReadout)
        assertEquals(true, VisualCfg(layout = ContentLayout.BATTERY_CENTER).showTopReadout)
        assertEquals(false, VisualCfg(layout = ContentLayout.BATTERY_CENTER, showTopReadout = false).showTopReadout)

    }

    @Test
    fun newGeometryControlsClampOutOfRangeInputs() {
        val settings = VisualCfg()
        val normalized =
            settings.copy(
                combinedScale = 9f,
                outerWeightScale = 9f,
                wifiScale = 9f,
                wifiOffsetY = -99f,
                mobileTypeOffsetY = 99f,
                curvedSignalDotOffsetY = -99f,
                horizontalSignalDotOffsetY = 99f,
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
        assertEquals(NETWORK_TOP_OFFSET_MIN, normalized.wifiOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MAX, normalized.mobileTypeOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MIN, normalized.curvedSignalDotOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MAX, normalized.horizontalSignalDotOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MIN, normalized.activeSignalDotOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MAX, normalized.copy(singleRowDotArrangement = SignalDotArrangement.HORIZONTAL).activeSignalDotOffsetY, 0f)
        val invalidOffsets = settings.copy(
            curvedSignalDotOffsetY = Float.NaN,
            horizontalSignalDotOffsetY = Float.POSITIVE_INFINITY,
        ).normalized()
        assertEquals(NETWORK_TOP_OFFSET_DEFAULT, invalidOffsets.curvedSignalDotOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_DEFAULT, invalidOffsets.horizontalSignalDotOffsetY, 0f)
        assertEquals(AIRPLANE_SIZE_SCALE_MAX, normalized.airplaneScale, 0.0001f)
        assertEquals(NO_SIM_SIZE_SCALE_MAX, normalized.noSimScale, 0.0001f)
        assertEquals(MOBILE_TYPE_SIZE_SCALE_MAX, normalized.mobileTypeScale, 0.0001f)
        assertEquals(MOBILE_TYPE_WEIGHT_MAX, normalized.mobileTypeWeight)
        assertEquals(1, MOBILE_TYPE_WEIGHT_MIN)
        assertEquals(600, MOBILE_TYPE_WEIGHT_DEFAULT)
        assertEquals(1000, MOBILE_TYPE_WEIGHT_MAX)
        assertEquals(0.60f, COMBINED_SCALE_MIN, 0.0001f)
        assertEquals(0.40f, WIFI_SIZE_SCALE_MIN, 0.0001f)
        assertEquals(WIFI_SIZE_SCALE_MIN, AIRPLANE_SIZE_SCALE_MIN, 0.0001f)
        assertEquals(WIFI_SIZE_SCALE_MAX, AIRPLANE_SIZE_SCALE_MAX, 0.0001f)
        assertEquals(WIFI_SIZE_SCALE_MIN, NO_SIM_SIZE_SCALE_MIN, 0.0001f)
        assertEquals(WIFI_SIZE_SCALE_MAX, NO_SIM_SIZE_SCALE_MAX, 0.0001f)
        assertEquals(0.40f, MOBILE_TYPE_SIZE_SCALE_MIN, 0.0001f)
    }

    @Test
    fun mobileCenteredIsAnIndependentLayoutWithOwnDefaults() {
        val centered = VisualCfg(layout = ContentLayout.MOBILE_CENTER)
        assertTrue(centered.centeredMobileSignal)
        assertEquals(SignalDotArrangement.BOTH_SIDES, centered.dualRowDotArrangement)
        assertEquals(SignalDotArrangement.CURVED, centered.singleRowDotArrangement)
        assertEquals(0.85f, centered.mobileTypeScale, 0f)
        assertEquals(-3f, centered.mobileTypeBottomOffsetY, 0f)
        assertEquals(false, centered.copy(layout = ContentLayout.NETWORK_CENTER).centeredMobileSignal)
        assertEquals(NETWORK_TOP_OFFSET_MAX,
            centered.copy(mobileTypeBottomOffsetY = 99f).normalized().mobileTypeBottomOffsetY, 0f)
        assertEquals(-3f,
            centered.copy(mobileTypeBottomOffsetY = Float.NaN).normalized().mobileTypeBottomOffsetY, 0f)
    }

    @Test
    fun outsideSingleAndDualRowsHaveIndependentStyleChoices() {
        val singleCurvedDualFlat = VisualCfg(
            layout = ContentLayout.MOBILE_CENTER,
            dualRowDotArrangement = SignalDotArrangement.HORIZONTAL,
            singleRowDotArrangement = SignalDotArrangement.CURVED,
        )
        assertEquals(SignalDotArrangement.CURVED, singleCurvedDualFlat.singleRowDotArrangement)
        assertEquals(SignalDotArrangement.HORIZONTAL, singleCurvedDualFlat.dualRowDotArrangement)
        val singleFlatDualCurved = singleCurvedDualFlat.copy(
            dualRowDotArrangement = SignalDotArrangement.CURVED,
            singleRowDotArrangement = SignalDotArrangement.HORIZONTAL,
        )
        assertEquals(SignalDotArrangement.HORIZONTAL, singleFlatDualCurved.singleRowDotArrangement)
        assertEquals(SignalDotArrangement.CURVED, singleFlatDualCurved.dualRowDotArrangement)
        assertTrue(singleFlatDualCurved.centeredMobileSignal)
    }

    @Test
    fun lowerOpeningChoiceAppliesToAllSignalDotArrangements() {
        for (layout in ContentLayout.entries) {
            for (singleStyle in SignalDotArrangement.entries) {
                for (dualStyle in SignalDotArrangement.entries) {
                    val config = VisualCfg(
                        layout = layout,
                        dualSignal = true,
                        singleRowDotArrangement = singleStyle,
                        dualRowDotArrangement = dualStyle,
                    )
                    assertEquals(false, config.fixedLowerOpening)
                    assertEquals(true, config.copy(fixedLowerOpening = true).fixedLowerOpening)
                }
            }
        }
    }

    @Test
    fun outsideRowsUseTheActualRowStyleForGeometryAndOffset() {
        for (dual in listOf(false, true)) {
            for (singleStyle in SignalDotArrangement.entries) {
                for (dualStyle in SignalDotArrangement.entries) {
                    val visual = VisualCfg(
                        layout = ContentLayout.MOBILE_CENTER,
                        dualSignal = dual,
                        dualRowDotArrangement = dualStyle,
                        singleRowDotArrangement = singleStyle,
                        curvedSignalDotOffsetY = 2f,
                        horizontalSignalDotOffsetY = 5f,
                        bothSidesSignalDotOffsetY = 7f,
                    )
                    assertEquals(singleStyle, visual.singleRowDotArrangement)
                    assertEquals(dualStyle, visual.dualRowDotArrangement)
                    assertEquals(
                        when (dualStyle) {
                            SignalDotArrangement.HORIZONTAL -> 5f
                            SignalDotArrangement.BOTH_SIDES -> 0f
                            SignalDotArrangement.CURVED -> 2f
                        },
                        visual.dualRowDotOffsetY,
                        0f,
                    )
                    assertEquals(
                        if (singleStyle == SignalDotArrangement.HORIZONTAL) 5f else 2f,
                        visual.singleRowDotOffsetY,
                        0f,
                    )
                    assertEquals(
                        if (dual) visual.dualRowDotOffsetY
                        else visual.singleRowDotOffsetY,
                        visual.activeSignalDotOffsetY,
                        0f,
                    )
                }
            }
        }
        assertEquals(ContentLayout.MOBILE_CENTER, ContentLayout.fromPersisted("mobile_center"))
        assertEquals(ContentLayout.BATTERY_CENTER, ContentLayout.fromPersisted("battery_center"))
    }

    @Test
    fun nonFiniteGeometryFallsBackToLayoutDefaults() {
        for (invalid in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val raw =
                VisualCfg(layout = ContentLayout.BATTERY_CENTER).copy(
                    topTextScale = invalid,
                    topOffsetY = invalid,
                    wifiOffsetY = invalid,
                    mobileTypeOffsetY = invalid,
                    curvedSignalDotOffsetY = invalid,
                    horizontalSignalDotOffsetY = invalid,
                    bothSidesSignalDotOffsetY = invalid,
                    topChargingIconScale = invalid,
                    combinedScale = invalid,
                    outerWeightScale = invalid,
                    wifiScale = invalid,
                    airplaneScale = invalid,
                    noSimScale = invalid,
                    mobileTypeScale = invalid,
                )
            val normalized = raw.normalized()
            assertEquals(topTextScaleDefault(raw.layout), normalized.topTextScale, 0f)
            assertEquals(BATTERY_TOP_VERTICAL_OFFSET_DEFAULT, normalized.topOffsetY, 0f)
            assertEquals(NETWORK_TOP_OFFSET_DEFAULT, normalized.wifiOffsetY, 0f)
            assertEquals(NETWORK_TOP_OFFSET_DEFAULT, normalized.mobileTypeOffsetY, 0f)
            assertEquals(NETWORK_TOP_OFFSET_DEFAULT, normalized.curvedSignalDotOffsetY, 0f)
            assertEquals(NETWORK_TOP_OFFSET_DEFAULT, normalized.horizontalSignalDotOffsetY, 0f)
            assertEquals(NETWORK_TOP_OFFSET_DEFAULT, normalized.bothSidesSignalDotOffsetY, 0f)
            assertEquals(topChargingIconScaleDefault(raw.layout), normalized.topChargingIconScale, 0f)
            assertEquals(COMBINED_SCALE_DEFAULT, normalized.combinedScale, 0f)
            assertEquals(OUTER_WEIGHT_SCALE_DEFAULT, normalized.outerWeightScale, 0f)
            assertEquals(WIFI_SIZE_SCALE_DEFAULT, normalized.wifiScale, 0f)
            assertEquals(AIRPLANE_SIZE_SCALE_DEFAULT, normalized.airplaneScale, 0f)
            assertEquals(NO_SIM_SIZE_SCALE_DEFAULT, normalized.noSimScale, 0f)
            assertEquals(mobileTypeScaleDefault(raw.layout), normalized.mobileTypeScale, 0f)
        }
    }

    @Test
    fun bottomActivityAndChargingSettingsRejectInvalidBackupValues() {
        val defaults = VisualCfg()
        for (invalid in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val restored = defaults.copy(
                bothSidesActivityScale = invalid,
                bothSidesActivityOffsetY = invalid,
                bothSidesChargingOffsetY = invalid,
            ).normalized()
            assertEquals(BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT, restored.bothSidesActivityScale, 0f)
            assertEquals(NETWORK_TOP_OFFSET_DEFAULT, restored.bothSidesActivityOffsetY, 0f)
            assertEquals(NETWORK_TOP_OFFSET_DEFAULT, restored.bothSidesChargingOffsetY, 0f)
        }
        val above = defaults.copy(
            bothSidesActivityScale = Float.MAX_VALUE,
            bothSidesActivityOffsetY = Float.MAX_VALUE,
            bothSidesChargingOffsetY = Float.MAX_VALUE,
        ).normalized()
        assertEquals(BOTH_SIDES_BOTTOM_INFO_SCALE_MAX, above.bothSidesActivityScale, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MAX, above.bothSidesActivityOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MAX, above.bothSidesChargingOffsetY, 0f)

        val below = defaults.copy(
            bothSidesActivityScale = -Float.MAX_VALUE,
            bothSidesActivityOffsetY = -Float.MAX_VALUE,
            bothSidesChargingOffsetY = -Float.MAX_VALUE,
        ).normalized()
        assertEquals(BOTH_SIDES_BOTTOM_INFO_SCALE_MIN, below.bothSidesActivityScale, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MIN, below.bothSidesActivityOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MIN, below.bothSidesChargingOffsetY, 0f)
    }

}
