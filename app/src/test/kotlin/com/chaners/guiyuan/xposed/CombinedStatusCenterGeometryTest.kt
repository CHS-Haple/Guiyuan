package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_MIN
import com.chaners.guiyuan.settings.CombinedStatusContentLayout
import com.chaners.guiyuan.settings.batteryTopVerticalOffsetRaw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusCenterGeometryTest {
    @Test
    fun wifiSizeChangesWithoutResizingMobileTypeOrNativePeers() {
        val base =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val enlarged =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1.2f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )

        assertEquals(base.wifiMaxWidth * 1.2f, enlarged.wifiMaxWidth, 0.0001f)
        assertEquals(base.wifiMaxHeight * 1.2f, enlarged.wifiMaxHeight, 0.0001f)
        assertEquals(base.mobileTypeTextSize, enlarged.mobileTypeTextSize, 0f)
        assertEquals(base.mobileTypeSuffixSize, enlarged.mobileTypeSuffixSize, 0f)
        assertEquals(base.airplaneMaxSize, enlarged.airplaneMaxSize, 0f)
        assertEquals(base.noSimMaxSize, enlarged.noSimMaxSize, 0f)
    }

    @Test
    fun airplaneAndNoSimSizesChangeIndependently() {
        val base =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val adjusted =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
                airplaneSizeScale = 1.2f,
                noSimSizeScale = 0.8f,
            )

        assertEquals(base.airplaneMaxSize * 1.2f, adjusted.airplaneMaxSize, 0.0001f)
        assertEquals(base.noSimMaxSize * 0.8f, adjusted.noSimMaxSize, 0.0001f)
        assertEquals(base.wifiMaxWidth, adjusted.wifiMaxWidth, 0f)
        assertEquals(base.mobileTypeTextSize, adjusted.mobileTypeTextSize, 0f)
    }

    @Test
    fun mobileTypeSizeChangesWithoutResizingWifiOrNativePeers() {
        val base =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val enlarged =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1.2f,
                mobileTypeWeight = 800,
            )

        assertEquals(base.mobileTypeTextSize * 1.2f, enlarged.mobileTypeTextSize, 0.0001f)
        assertEquals(base.mobileTypeSuffixSize * 1.2f, enlarged.mobileTypeSuffixSize, 0.0001f)
        assertEquals(base.mobileTypeSuffixRise * 1.2f, enlarged.mobileTypeSuffixRise, 0.0001f)
        assertEquals(base.wifiMaxWidth, enlarged.wifiMaxWidth, 0f)
        assertEquals(base.airplaneMaxSize, enlarged.airplaneMaxSize, 0f)
        assertEquals(base.noSimMaxSize, enlarged.noSimMaxSize, 0f)
    }

    @Test
    fun mobileTypeWeightChangesIndependentlyFromAllDrawableSizes() {
        val light =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 600,
            )
        val heavy =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 900,
            )

        assertEquals(light.wifiMaxWidth, heavy.wifiMaxWidth, 0f)
        assertEquals(light.airplaneMaxSize, heavy.airplaneMaxSize, 0f)
        assertEquals(light.mobileTypeTextSize, heavy.mobileTypeTextSize, 0f)
        assertTrue(heavy.mobileTypeWeight > light.mobileTypeWeight)
    }

    @Test
    fun invalidAndOutOfRangeValuesAreClampedSafely() {
        val fallback =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = Float.NaN,
                mobileTypeSizeScale = Float.NaN,
                mobileTypeWeight = Int.MIN_VALUE,
            )
        assertEquals(
            CombinedStatusCenterGeometry.DEFAULT_WIFI_SIZE_SCALE,
            fallback.wifiSizeScale,
            0f,
        )
        assertEquals(
            CombinedStatusCenterGeometry.DEFAULT_MOBILE_TYPE_SIZE_SCALE,
            fallback.mobileTypeSizeScale,
            0f,
        )
        assertEquals(
            CombinedStatusCenterGeometry.MIN_MOBILE_TYPE_WEIGHT,
            fallback.mobileTypeWeight,
        )

        val clamped =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 5f,
                mobileTypeSizeScale = 5f,
                mobileTypeWeight = 5000,
            )
        assertEquals(
            CombinedStatusCenterGeometry.MAX_WIFI_SIZE_SCALE,
            clamped.wifiSizeScale,
            0f,
        )
        assertEquals(
            CombinedStatusCenterGeometry.MAX_MOBILE_TYPE_SIZE_SCALE,
            clamped.mobileTypeSizeScale,
            0f,
        )
        assertEquals(
            CombinedStatusCenterGeometry.MAX_MOBILE_TYPE_WEIGHT,
            clamped.mobileTypeWeight,
        )
    }

    @Test
    fun rendererRangeClampsMatchPersistedVisualRanges() {
        assertEquals(0.40f, CombinedStatusCenterGeometry.MIN_WIFI_SIZE_SCALE, 0f)
        assertEquals(0.40f, CombinedStatusCenterGeometry.MIN_AIRPLANE_SIZE_SCALE, 0f)
        assertEquals(1.25f, CombinedStatusCenterGeometry.MAX_AIRPLANE_SIZE_SCALE, 0f)
        assertEquals(0.40f, CombinedStatusCenterGeometry.MIN_NO_SIM_SIZE_SCALE, 0f)
        assertEquals(1.25f, CombinedStatusCenterGeometry.MAX_NO_SIM_SIZE_SCALE, 0f)
        assertEquals(0.40f, CombinedStatusCenterGeometry.MIN_MOBILE_TYPE_SIZE_SCALE, 0f)

        val clamped =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = -1f,
                mobileTypeSizeScale = -1f,
                mobileTypeWeight = 800,
                airplaneSizeScale = -1f,
                noSimSizeScale = -1f,
                combinedScale = -1f,
            )

        assertEquals(0.40f, clamped.wifiSizeScale, 0f)
        assertEquals(0.40f, clamped.airplaneSizeScale, 0f)
        assertEquals(0.40f, clamped.noSimSizeScale, 0f)
        assertEquals(0.40f, clamped.mobileTypeSizeScale, 0f)
        assertEquals(COMBINED_SCALE_MIN, clamped.combinedScale, 0f)
    }

    @Test
    fun mobileTypeKeepsHostCompensationButFollowsCombinedScale() {
        val base = 39f
        val hostScale = 1.5f

        fun physicalSize(combinedScale: Float): Float {
            val canvasScale = hostScale * combinedScale
            val local =
                CombinedStatusMobileTypeScalePolicy.localValue(
                    baseValue = base,
                    canvasScale = canvasScale,
                    combinedScale = combinedScale,
                    scaleWithCanvas = false,
                )
            return local * canvasScale
        }

        assertEquals(base, physicalSize(1f), 0.0001f)
        assertEquals(base * 0.6f, physicalSize(0.6f), 0.0001f)
    }

    @Test
    fun previewCanvasScalingKeepsExistingDirectScalePath() {
        val base = 39f
        assertEquals(
            base,
            CombinedStatusMobileTypeScalePolicy.localValue(
                baseValue = base,
                canvasScale = 0.6f,
                combinedScale = 0.6f,
                scaleWithCanvas = true,
            ),
            0f,
        )
    }

    @Test
    fun topInfoOffsetTargetsReadoutOnlyInNetworkCenter() {
        val raw = batteryTopVerticalOffsetRaw(5f)

        assertEquals(
            raw,
            CombinedStatusTopInfoOffsetPolicy.readoutRequestedOffset(
                layout = CombinedStatusContentLayout.NETWORK_CENTER,
                rawOffset = raw,
            ),
            0f,
        )
        assertEquals(
            0f,
            CombinedStatusTopInfoOffsetPolicy.networkTranslationDelta(
                layout = CombinedStatusContentLayout.NETWORK_CENTER,
                rawOffset = raw,
            ),
            0f,
        )
    }

    @Test
    fun topInfoOffsetTargetsNetworkOnlyInBatteryCenter() {
        val raw = batteryTopVerticalOffsetRaw(5f)

        assertEquals(
            BATTERY_TOP_VERTICAL_OFFSET_DEFAULT,
            CombinedStatusTopInfoOffsetPolicy.readoutRequestedOffset(
                layout = CombinedStatusContentLayout.BATTERY_CENTER,
                rawOffset = raw,
            ),
            0f,
        )
        assertEquals(
            -5f,
            CombinedStatusTopInfoOffsetPolicy.networkTranslationDelta(
                layout = CombinedStatusContentLayout.BATTERY_CENTER,
                rawOffset = raw,
            ),
            0.0001f,
        )
    }

    @Test
    fun fiveGaAccessSuffixUsesLowerRightVerticalDirection() {
        assertEquals(
            8f,
            CombinedStatusMobileTypeSuffixPolicy.verticalOffset(
                suffix = "A",
                magnitude = 8f,
            ),
            0f,
        )
        assertEquals(
            -8f,
            CombinedStatusMobileTypeSuffixPolicy.verticalOffset(
                suffix = "++",
                magnitude = 8f,
            ),
            0f,
        )
    }

    @Test
    fun nativePeerDefaultsRemainOpticallyMatchedButDoNotFollowWifiScaling() {
        val base =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val enlargedWifi =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1.2f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )

        assertEquals(base.wifiMaxWidth, base.airplaneMaxSize, 0f)
        assertEquals(base.airplaneMaxSize, enlargedWifi.airplaneMaxSize, 0f)
        assertEquals(base.noSimMaxSize, enlargedWifi.noSimMaxSize, 0f)
    }
}
