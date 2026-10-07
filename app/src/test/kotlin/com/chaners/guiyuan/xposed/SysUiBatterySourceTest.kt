package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SysUiBatterySourceTest {
    @Test
    fun usesFreshNativeIcon() {
        assertEquals(
            42,
            SysUiBatterySource.resolveChargingIconId(
                charging = true,
                nativeId = 42,
                lastId = 7,
            ),
        )
    }

    @Test
    fun keepsLastIconDuringChargingGap() {
        assertEquals(
            7,
            SysUiBatterySource.resolveChargingIconId(
                charging = true,
                nativeId = null,
                lastId = 7,
            ),
        )
        assertEquals(
            7,
            SysUiBatterySource.resolveChargingIconId(
                charging = true,
                nativeId = 0,
                lastId = 7,
            ),
        )
    }

    @Test
    fun clearsIconWhenChargingStops() {
        assertNull(
            SysUiBatterySource.resolveChargingIconId(
                charging = false,
                nativeId = 42,
                lastId = 7,
            ),
        )
    }

    @Test
    fun keepsFreshIconBeforeBatteryStateArrives() {
        assertEquals(
            42,
            SysUiBatterySource.resolveChargingIconId(
                charging = null,
                nativeId = 42,
                lastId = null,
            ),
        )
    }
}
