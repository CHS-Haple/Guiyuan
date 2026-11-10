package com.chaners.guiyuan.xposed.network

import com.chaners.guiyuan.xposed.StatusStateStore
import org.junit.Assert.assertEquals
import org.junit.Test

private const val TEST_NATIVE_WIFI_LEVEL_3_RESOURCE = "com.android.systemui:drawable/stat_sys_wifi_signal_3"
private const val TEST_NATIVE_HOTSPOT_LEVEL_3_RESOURCE = "com.android.systemui:drawable/stat_sys_hotspot_signal_3"

class SysUiSignalParserTest {
    @Test
    fun wifiSignalLevelsCoverZeroThroughThree() {
        for (level in 0..3) {
            assertEquals(
                SignalStrength.Level(level),
                SysUiSignalParser.wifi(
                    "com.android.systemui:drawable/stat_sys_wifi_signal_" + level,
                ),
            )
        }
    }

    @Test
    fun wifiVariantResourcesPreserveSignalLevel() {
        assertEquals(
            SignalStrength.Level(2),
            SysUiSignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_wifi_signal_2_unavailable",
            ),
        )
        assertEquals(
            SignalStrength.Level(1),
            SysUiSignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_wifi_signal_unavailable_1",
            ),
        )
    }

    @Test
    fun hotspotWifiFamilyPreservesNativeSignalLevelAndInternetVariant() {
        assertEquals(
            SignalStrength.Level(2),
            SysUiSignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_hotspot_signal_2",
            ),
        )
        assertEquals(
            false,
            SysUiSignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_hotspot_signal_2_unavailable",
            ),
        )
    }

    @Test
    fun wifiInternetHintComesFromSystemUiResourceVariant() {
        assertEquals(
            true,
            SysUiSignalParser.wifiInternetValidated(
                TEST_NATIVE_WIFI_LEVEL_3_RESOURCE,
            ),
        )
        assertEquals(
            false,
            SysUiSignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_2_unavailable",
            ),
        )
        assertEquals(
            false,
            SysUiSignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_no_internet_1",
            ),
        )
        assertEquals(
            null,
            SysUiSignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_2_dark",
            ),
        )
    }

    @Test
    fun unknownResourcesStayUnknown() {
        assertEquals(
            SignalStrength.Unknown,
            SysUiSignalParser.mobile(
                "com.android.systemui:drawable/stat_sys_signal_roaming",
            ),
        )
        assertEquals(
            SignalStrength.Unknown,
            SysUiSignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_wifi_unavailable",
            ),
        )
        assertEquals(SignalStrength.Unknown, SysUiSignalParser.wifi(null))
    }
    @Test
    fun hotspotResourceFamilyIsDistinguishedFromRegularWifi() {
        assertEquals(
            true,
            SysUiSignalParser.isHotspotWifiResource(
                TEST_NATIVE_HOTSPOT_LEVEL_3_RESOURCE,
            ),
        )
        assertEquals(
            false,
            SysUiSignalParser.isHotspotWifiResource(
                TEST_NATIVE_WIFI_LEVEL_3_RESOURCE,
            ),
        )
    }

    @Test
    fun noInternetWifiVariantRemainsInsideNativeWifiFamily() {
        val resource =
            "com.android.systemui:drawable/stat_sys_wifi_signal_2_no_internet"

        assertEquals(true, SysUiSignalParser.isWifiFamilyResource(resource))
        assertEquals(SignalStrength.Level(2), SysUiSignalParser.wifi(resource))
        assertEquals(false, SysUiSignalParser.wifiInternetValidated(resource))
    }

    @Test
    fun hotspotNoInternetVariantRetainsSignalAndInternetSemantics() {
        val resource =
            "com.android.systemui:drawable/stat_sys_hotspot_signal_3_unavailable"

        assertEquals(true, SysUiSignalParser.isWifiFamilyResource(resource))
        assertEquals(true, SysUiSignalParser.isHotspotWifiResource(resource))
        assertEquals(SignalStrength.Level(3), SysUiSignalParser.wifi(resource))
        assertEquals(false, SysUiSignalParser.wifiInternetValidated(resource))
    }

    @Test
    fun appliedHotspotFallbackRequiresANewHotspotTag() {
        val hidden = StatusStateStore.WifiState.Hidden

        assertEquals(
            true,
            SysUiNetworkSource.shouldUseAppliedHotspotFallback(
                semanticState = hidden,
                taggedResId = 100,
                previousTaggedResId = 99,
                taggedResource =
                    TEST_NATIVE_HOTSPOT_LEVEL_3_RESOURCE,
            ),
        )
        assertEquals(
            false,
            SysUiNetworkSource.shouldUseAppliedHotspotFallback(
                semanticState = hidden,
                taggedResId = 100,
                previousTaggedResId = 100,
                taggedResource =
                    TEST_NATIVE_HOTSPOT_LEVEL_3_RESOURCE,
            ),
        )
        assertEquals(
            false,
            SysUiNetworkSource.shouldUseAppliedHotspotFallback(
                semanticState = hidden,
                taggedResId = 101,
                previousTaggedResId = 100,
                taggedResource =
                    TEST_NATIVE_WIFI_LEVEL_3_RESOURCE,
            ),
        )
    }

}
