package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class CenterResourcePolicyTest {
    @Test
    fun baseResourceMapsToTintVariant() {
        assertEquals(
            "stat_sys_wifi_signal_3_tint",
            CenterResourcePolicy.tintEntryName(
                "stat_sys_wifi_signal_3",
            ),
        )
    }

    @Test
    fun darkResourceNormalizesBeforeTintSelection() {
        assertEquals(
            "stat_sys_wifi_signal_3_tint",
            CenterResourcePolicy.tintEntryName(
                "stat_sys_wifi_signal_3_darkmode",
            ),
        )
    }

    @Test
    fun existingTintResourceRemainsStable() {
        assertEquals(
            "stat_sys_wifi_signal_unavailable_2_tint",
            CenterResourcePolicy.tintEntryName(
                "stat_sys_wifi_signal_unavailable_2_tint",
            ),
        )
    }

    @Test
    fun alreadyDarkModeInputStillResolvesOpaqueTintMask() {
        assertEquals(
            "stat_sys_wifi_signal_1_tint",
            CenterResourcePolicy.tintEntryName(
                "stat_sys_wifi_signal_1_darkmode",
            ),
        )
    }

    @Test
    fun hotspotFamilyUsesSamePresentationSuffixContract() {
        assertEquals(
            "stat_sys_hotspot_signal_3_tint",
            CenterResourcePolicy.tintEntryName(
                "stat_sys_hotspot_signal_3",
            ),
        )
    }
}
