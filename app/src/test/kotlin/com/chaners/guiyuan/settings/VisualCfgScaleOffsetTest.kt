package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualCfgScaleOffsetTest {
    @Test
    fun nativeWeightDefaultsStayAligned() {
        assertEquals(BATTERY_TOP_TEXT_WEIGHT_DEFAULT, MOBILE_TYPE_WEIGHT_DEFAULT)
        assertEquals(1, MOBILE_TYPE_WEIGHT_MIN)
        assertEquals(1000, MOBILE_TYPE_WEIGHT_MAX)
    }

    @Test
    fun nativeWeightBoundsPreserveValidProfiles() {
        val previous = VisualCfg(
            mobileTypeWeight = 1400,
            wifiWeight = WIFI_WEIGHT_MAX,
            topTextWeight = 1400,
        )
        val updated = previous.normalized()
        assertEquals(1000, updated.mobileTypeWeight)
        assertEquals(WIFI_WEIGHT_MAX, updated.wifiWeight)
        assertEquals(1000, updated.topTextWeight)
        assertEquals(1, previous.copy(topTextWeight = -1).normalized().topTextWeight)
        assertEquals(1000, previous.copy(topTextWeight = 1001).normalized().topTextWeight)
        assertEquals(900, previous.copy(topTextWeight = 900).normalized().topTextWeight)
        assertEquals(200, previous.copy(topTextWeight = 200).normalized().topTextWeight)
        assertEquals(1, previous.copy(mobileTypeWeight = -1).normalized().mobileTypeWeight)
        assertEquals(1000, previous.copy(mobileTypeWeight = 1001).normalized().mobileTypeWeight)
        assertEquals(200, previous.copy(mobileTypeWeight = 200).normalized().mobileTypeWeight)
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
        assertEquals(8f, topOffsetYRaw(5f), 0.0001f)
    }

    @Test
    fun networkCenteredTopOffsetDefaultsToPlusThreeWithoutChangingBatteryCenteredReference() {
        val network = VisualCfg()
        assertEquals(ContentLayout.NETWORK_CENTER, network.layout)
        assertEquals(false, network.showTopReadout)
        assertEquals(3f, topOffsetYUi(network.topOffsetY), 0f)
        assertEquals(6f, network.topOffsetY, 0f)

        val battery = VisualCfg(layout = ContentLayout.BATTERY_CENTER)
        assertEquals(0f, topOffsetYUi(battery.topOffsetY), 0f)
        assertEquals(3f, battery.topOffsetY, 0f)
        assertEquals(6f, Float.NaN.finiteIn(
            BATTERY_TOP_VERTICAL_OFFSET_MIN,
            BATTERY_TOP_VERTICAL_OFFSET_MAX,
            topOffsetYDefault(ContentLayout.NETWORK_CENTER),
        ), 0f)
        assertEquals(6f, network.copy(topOffsetY = Float.NaN).normalized().topOffsetY, 0f)
        assertEquals(3f, battery.copy(topOffsetY = Float.NaN).normalized().topOffsetY, 0f)
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
}
