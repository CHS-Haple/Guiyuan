package com.chaners.guiyuan.xposed.network

import org.junit.Assert.assertEquals
import org.junit.Test

class SysUiMobileSignalParserTest {
    @Test
    fun mobileSignalLevelsCoverZeroThroughFour() {
        for (level in 0..4) {
            assertEquals(
                SignalStrength.Level(level),
                SysUiSignalParser.mobile(
                    "com.android.systemui:drawable/stat_sys_signal_" + level,
                ),
            )
        }
    }

    @Test
    fun mobileNullSignalIsUnavailable() {
        assertEquals(
            SignalStrength.Unavailable,
            SysUiSignalParser.mobile(
                "com.android.systemui:drawable/stat_sys_signal_null",
            ),
        )
    }
}
