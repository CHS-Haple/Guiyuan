package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUiSignalParserTest {
    @Test
    fun mobileSignalLevelsCoverZeroThroughFour() {
        for (level in 0..4) {
            assertEquals(
                SignalStrength.Level(level),
                SystemUiSignalParser.mobile(
                    "com.android.systemui:drawable/stat_sys_signal_" + level,
                ),
            )
        }
    }

    @Test
    fun mobileNullSignalIsUnavailable() {
        assertEquals(
            SignalStrength.Unavailable,
            SystemUiSignalParser.mobile(
                "com.android.systemui:drawable/stat_sys_signal_null",
            ),
        )
    }

    @Test
    fun wifiSignalLevelsCoverZeroThroughThree() {
        for (level in 0..3) {
            assertEquals(
                SignalStrength.Level(level),
                SystemUiSignalParser.wifi(
                    "com.android.systemui:drawable/stat_sys_wifi_signal_" + level,
                ),
            )
        }
    }

    @Test
    fun wifiVariantResourcesPreserveSignalLevel() {
        assertEquals(
            SignalStrength.Level(2),
            SystemUiSignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_wifi_signal_2_unavailable",
            ),
        )
        assertEquals(
            SignalStrength.Level(1),
            SystemUiSignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_wifi_signal_unavailable_1",
            ),
        )
    }

    @Test
    fun hotspotWifiFamilyPreservesNativeSignalLevelAndInternetVariant() {
        assertEquals(
            SignalStrength.Level(2),
            SystemUiSignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_hotspot_signal_2",
            ),
        )
        assertEquals(
            false,
            SystemUiSignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_hotspot_signal_2_unavailable",
            ),
        )
    }

    @Test
    fun wifiInternetHintComesFromSystemUiResourceVariant() {
        assertEquals(
            true,
            SystemUiSignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_3",
            ),
        )
        assertEquals(
            false,
            SystemUiSignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_2_unavailable",
            ),
        )
        assertEquals(
            false,
            SystemUiSignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_no_internet_1",
            ),
        )
        assertEquals(
            null,
            SystemUiSignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_2_dark",
            ),
        )
    }

    @Test
    fun unknownResourcesStayUnknown() {
        assertEquals(
            SignalStrength.Unknown,
            SystemUiSignalParser.mobile(
                "com.android.systemui:drawable/stat_sys_signal_roaming",
            ),
        )
        assertEquals(
            SignalStrength.Unknown,
            SystemUiSignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_wifi_unavailable",
            ),
        )
        assertEquals(SignalStrength.Unknown, SystemUiSignalParser.wifi(null))
    }
    @Test
    fun hotspotResourceFamilyIsDistinguishedFromRegularWifi() {
        assertEquals(
            true,
            SystemUiSignalParser.isHotspotWifiResource(
                "com.android.systemui:drawable/stat_sys_hotspot_signal_3",
            ),
        )
        assertEquals(
            false,
            SystemUiSignalParser.isHotspotWifiResource(
                "com.android.systemui:drawable/stat_sys_wifi_signal_3",
            ),
        )
    }

    @Test
    fun noInternetWifiVariantRemainsInsideNativeWifiFamily() {
        val resource =
            "com.android.systemui:drawable/stat_sys_wifi_signal_2_no_internet"

        assertEquals(true, SystemUiSignalParser.isWifiFamilyResource(resource))
        assertEquals(SignalStrength.Level(2), SystemUiSignalParser.wifi(resource))
        assertEquals(false, SystemUiSignalParser.wifiInternetValidated(resource))
    }

    @Test
    fun hotspotNoInternetVariantRetainsSignalAndInternetSemantics() {
        val resource =
            "com.android.systemui:drawable/stat_sys_hotspot_signal_3_unavailable"

        assertEquals(true, SystemUiSignalParser.isWifiFamilyResource(resource))
        assertEquals(true, SystemUiSignalParser.isHotspotWifiResource(resource))
        assertEquals(SignalStrength.Level(3), SystemUiSignalParser.wifi(resource))
        assertEquals(false, SystemUiSignalParser.wifiInternetValidated(resource))
    }

    @Test
    fun appliedHotspotFallbackRequiresANewHotspotTag() {
        val hidden = StatusStateStore.WifiState.Hidden

        assertEquals(
            true,
            SystemUiNetworkStateSource.shouldUseAppliedHotspotFallback(
                semanticState = hidden,
                taggedResId = 100,
                previousTaggedResId = 99,
                taggedResource =
                    "com.android.systemui:drawable/stat_sys_hotspot_signal_3",
            ),
        )
        assertEquals(
            false,
            SystemUiNetworkStateSource.shouldUseAppliedHotspotFallback(
                semanticState = hidden,
                taggedResId = 100,
                previousTaggedResId = 100,
                taggedResource =
                    "com.android.systemui:drawable/stat_sys_hotspot_signal_3",
            ),
        )
        assertEquals(
            false,
            SystemUiNetworkStateSource.shouldUseAppliedHotspotFallback(
                semanticState = hidden,
                taggedResId = 101,
                previousTaggedResId = 100,
                taggedResource =
                    "com.android.systemui:drawable/stat_sys_wifi_signal_3",
            ),
        )
    }

}
