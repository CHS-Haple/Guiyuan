package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
import com.chaners.guiyuan.settings.ContentLayout
import com.chaners.guiyuan.settings.topOffsetYRaw
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.InternetState
import org.junit.Assert.assertEquals
import org.junit.Test

class CenterGeometryPlacementTest {
    @Test
    fun centeredMobileTypeUsesMeasuredInkBoundsForLowerOpening() {
        val policy = CenteredMobileTypePositionPolicy
        fun translation(requested: Float) = policy.translation(
            sourceTop = 45f, mobileBottom = 80f,
            requested = requested, clearance = 2f,
        )
        assertEquals(43f, translation(43f), 0f)
        assertEquals(53f, translation(53f), 0f)
        assertEquals(37f, translation(20f), 0f)
        assertEquals(82f, 45f + translation(20f), 0f)
        // The paint may extend past the canonical bottom; the physical View must expand.
        assertEquals(128f, 75f + translation(53f), 0f)
        assertEquals(
            42f,
            policy.translation(40f, 80f, 20f, 2f),
            0f,
        )
    }

    @Test
    fun batteryCenteredWifiAndMobileTypeHaveIndependentTopOffsets() {
        val raw = topOffsetYRaw(5f)
        val wifi = CenterIndicator.Wifi(segments = 3, internet = InternetState.UNKNOWN)
        val mobile = CenterIndicator.MobileType(
            label = "5G",
            enhanced = false,
            internet = InternetState.UNKNOWN,
        )
        fun shift(indicator: CenterIndicator?) =
            TopInfoOffsetPolicy.networkTranslationDelta(
                layout = ContentLayout.BATTERY_CENTER,
                rawOffset = raw,
                indicator = indicator,
                wifiOffset = -2f,
                mobileTypeOffset = 4f,
            )

        assertEquals(2f, shift(wifi), 0f)
        assertEquals(-4f, shift(mobile), 0f)
        assertEquals(-5f, shift(CenterIndicator.Airplane), 0f)
        assertEquals(-5f, shift(null), 0f)
        assertEquals(
            0f,
            TopInfoOffsetPolicy.networkTranslationDelta(
                layout = ContentLayout.NETWORK_CENTER,
                rawOffset = raw,
                indicator = wifi,
                wifiOffset = -2f,
            ),
            0f,
        )
    }

    @Test
    fun topInfoOffsetTargetsReadoutOnlyInNetworkCenter() {
        val raw = topOffsetYRaw(5f)

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
        val raw = topOffsetYRaw(5f)

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
    fun fiveGaSuffixMorphNeedsARealNativeTarget() {
        fun fraction(main: String, suffix: String, available: Boolean, progress: Float) =
            MobileTypeSuffixPolicy.morphProgress(main, suffix, available, progress)

        assertEquals(0f, fraction("5G", "A", true, 0f), 0f)
        assertEquals(0.5f, fraction("5G", "A", true, 0.5f), 0f)
        assertEquals(1f, fraction("5G", "A", true, 1f), 0f)
        assertEquals(0f, fraction("5G", "A", false, 1f), 0f)
        assertEquals(0f, fraction("5G", "++", true, 1f), 0f)
        assertEquals(0f, fraction("4G", "+", true, 1f), 0f)
        assertEquals(0f, fraction("5G", "A", true, Float.NaN), 0f)
    }
}
