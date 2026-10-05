package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_MIN
import com.chaners.guiyuan.settings.ContentLayout
import com.chaners.guiyuan.settings.batteryTopVerticalOffsetRaw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CenterGeometryTest {
    @Test
    fun wifiSizeChangesWithoutResizingMobileTypeOrNativePeers() {
        val base =
            CenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val enlarged =
            CenterGeometry.resolve(
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
            CenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val adjusted =
            CenterGeometry.resolve(
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
            CenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val enlarged =
            CenterGeometry.resolve(
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
            CenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 600,
            )
        val heavy =
            CenterGeometry.resolve(
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
            CenterGeometry.resolve(
                wifiSizeScale = Float.NaN,
                mobileTypeSizeScale = Float.NaN,
                mobileTypeWeight = Int.MIN_VALUE,
            )
        assertEquals(
            CenterGeometry.DEFAULT_WIFI_SIZE_SCALE,
            fallback.wifiSizeScale,
            0f,
        )
        assertEquals(
            CenterGeometry.DEFAULT_MOBILE_TYPE_SIZE_SCALE,
            fallback.mobileTypeSizeScale,
            0f,
        )
        assertEquals(
            CenterGeometry.MIN_MOBILE_TYPE_WEIGHT,
            fallback.mobileTypeWeight,
        )

        val clamped =
            CenterGeometry.resolve(
                wifiSizeScale = 5f,
                mobileTypeSizeScale = 5f,
                mobileTypeWeight = 5000,
            )
        assertEquals(
            CenterGeometry.MAX_WIFI_SIZE_SCALE,
            clamped.wifiSizeScale,
            0f,
        )
        assertEquals(
            CenterGeometry.MAX_MOBILE_TYPE_SIZE_SCALE,
            clamped.mobileTypeSizeScale,
            0f,
        )
        assertEquals(
            CenterGeometry.MAX_MOBILE_TYPE_WEIGHT,
            clamped.mobileTypeWeight,
        )
    }

    @Test
    fun rendererRangeClampsMatchPersistedVisualRanges() {
        assertEquals(0.40f, CenterGeometry.MIN_WIFI_SIZE_SCALE, 0f)
        assertEquals(0.40f, CenterGeometry.MIN_AIRPLANE_SIZE_SCALE, 0f)
        assertEquals(1.25f, CenterGeometry.MAX_AIRPLANE_SIZE_SCALE, 0f)
        assertEquals(0.40f, CenterGeometry.MIN_NO_SIM_SIZE_SCALE, 0f)
        assertEquals(1.25f, CenterGeometry.MAX_NO_SIM_SIZE_SCALE, 0f)
        assertEquals(0.40f, CenterGeometry.MIN_MOBILE_TYPE_SIZE_SCALE, 0f)

        val clamped =
            CenterGeometry.resolve(
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
                MobileTypeScalePolicy.localValue(
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
            MobileTypeScalePolicy.localValue(
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
            TopInfoOffsetPolicy.readoutRequestedOffset(
                layout = ContentLayout.NETWORK_CENTER,
                rawOffset = raw,
            ),
            0f,
        )
        assertEquals(
            0f,
            TopInfoOffsetPolicy.networkTranslationDelta(
                layout = ContentLayout.NETWORK_CENTER,
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
            TopInfoOffsetPolicy.readoutRequestedOffset(
                layout = ContentLayout.BATTERY_CENTER,
                rawOffset = raw,
            ),
            0f,
        )
        assertEquals(
            -5f,
            TopInfoOffsetPolicy.networkTranslationDelta(
                layout = ContentLayout.BATTERY_CENTER,
                rawOffset = raw,
            ),
            0.0001f,
        )
    }

    @Test
    fun fiveGaAccessSuffixUsesLowerRightVerticalDirection() {
        assertEquals(
            8f,
            MobileTypeSuffixPolicy.verticalOffset(
                suffix = "A",
                magnitude = 8f,
            ),
            0f,
        )
        assertEquals(
            -8f,
            MobileTypeSuffixPolicy.verticalOffset(
                suffix = "++",
                magnitude = 8f,
            ),
            0f,
        )
    }

    @Test
    fun nativePeerDefaultsRemainOpticallyMatchedButDoNotFollowWifiScaling() {
        val base =
            CenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val enlargedWifi =
            CenterGeometry.resolve(
                wifiSizeScale = 1.2f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )

        assertEquals(base.wifiMaxWidth, base.airplaneMaxSize, 0f)
        assertEquals(base.airplaneMaxSize, enlargedWifi.airplaneMaxSize, 0f)
        assertEquals(base.noSimMaxSize, enlargedWifi.noSimMaxSize, 0f)
    }
}
