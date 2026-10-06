package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalParserTest {
    @Test
    fun mobileSignalLevelsCoverZeroThroughFour() {
        for (level in 0..4) {
            assertEquals(
                SignalStrength.Level(level),
                SignalParser.mobile(
                    "com.android.systemui:drawable/stat_sys_signal_" + level,
                ),
            )
        }
    }

    @Test
    fun mobileNullSignalIsUnavailable() {
        assertEquals(
            SignalStrength.Unavailable,
            SignalParser.mobile(
                "com.android.systemui:drawable/stat_sys_signal_null",
            ),
        )
    }

    @Test
    fun wifiSignalLevelsCoverZeroThroughThree() {
        for (level in 0..3) {
            assertEquals(
                SignalStrength.Level(level),
                SignalParser.wifi(
                    "com.android.systemui:drawable/stat_sys_wifi_signal_" + level,
                ),
            )
        }
    }

    @Test
    fun wifiVariantResourcesPreserveSignalLevel() {
        assertEquals(
            SignalStrength.Level(2),
            SignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_wifi_signal_2_unavailable",
            ),
        )
        assertEquals(
            SignalStrength.Level(1),
            SignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_wifi_signal_unavailable_1",
            ),
        )
    }

    @Test
    fun hotspotWifiKeepsNativeSemantics() {
        assertEquals(
            SignalStrength.Level(2),
            SignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_hotspot_signal_2",
            ),
        )
        assertEquals(
            false,
            SignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_hotspot_signal_2_unavailable",
            ),
        )
    }

    @Test
    fun wifiInternetHintUsesSysUiVariant() {
        assertEquals(
            true,
            SignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_3",
            ),
        )
        assertEquals(
            false,
            SignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_2_unavailable",
            ),
        )
        assertEquals(
            false,
            SignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_no_internet_1",
            ),
        )
        assertEquals(
            null,
            SignalParser.wifiInternetValidated(
                "com.android.systemui:drawable/stat_sys_wifi_signal_2_dark",
            ),
        )
    }

    @Test
    fun unknownResourcesStayUnknown() {
        assertEquals(
            SignalStrength.Unknown,
            SignalParser.mobile(
                "com.android.systemui:drawable/stat_sys_signal_roaming",
            ),
        )
        assertEquals(
            SignalStrength.Unknown,
            SignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_wifi_unavailable",
            ),
        )
        assertEquals(SignalStrength.Unknown, SignalParser.wifi(null))
    }
    @Test
    fun hotspotFamilyDiffersFromWifi() {
        assertEquals(
            true,
            SignalParser.isHotspotWifiResource(
                "com.android.systemui:drawable/stat_sys_hotspot_signal_3",
            ),
        )
        assertEquals(
            false,
            SignalParser.isHotspotWifiResource(
                "com.android.systemui:drawable/stat_sys_wifi_signal_3",
            ),
        )
    }

    @Test
    fun noInternetVariantStaysWifi() {
        val resource =
            "com.android.systemui:drawable/stat_sys_wifi_signal_2_no_internet"

        assertEquals(true, SignalParser.isWifiFamilyResource(resource))
        assertEquals(SignalStrength.Level(2), SignalParser.wifi(resource))
        assertEquals(false, SignalParser.wifiInternetValidated(resource))
    }

    @Test
    fun hotspotNoInternetKeepsSemantics() {
        val resource =
            "com.android.systemui:drawable/stat_sys_hotspot_signal_3_unavailable"

        assertEquals(true, SignalParser.isWifiFamilyResource(resource))
        assertEquals(true, SignalParser.isHotspotWifiResource(resource))
        assertEquals(SignalStrength.Level(3), SignalParser.wifi(resource))
        assertEquals(false, SignalParser.wifiInternetValidated(resource))
    }

    @Test
    fun hotspotFallbackNeedsNewTag() {
        val hidden = StatusStateStore.WifiState.Hidden

        assertEquals(
            true,
            NetworkStateSource.shouldUseAppliedHotspotFallback(
                semanticState = hidden,
                taggedResId = 100,
                previousTaggedResId = 99,
                taggedResource =
                    "com.android.systemui:drawable/stat_sys_hotspot_signal_3",
            ),
        )
        assertEquals(
            false,
            NetworkStateSource.shouldUseAppliedHotspotFallback(
                semanticState = hidden,
                taggedResId = 100,
                previousTaggedResId = 100,
                taggedResource =
                    "com.android.systemui:drawable/stat_sys_hotspot_signal_3",
            ),
        )
        assertEquals(
            false,
            NetworkStateSource.shouldUseAppliedHotspotFallback(
                semanticState = hidden,
                taggedResId = 101,
                previousTaggedResId = 100,
                taggedResource =
                    "com.android.systemui:drawable/stat_sys_wifi_signal_3",
            ),
        )
    }

}
