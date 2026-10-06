package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class TintAuthorityTest {
    @Test
    fun liveStatusIconTintWinsForBatteryEvent() {
        val resolved =
            TintAuthority.resolveBatteryEvent(
                batteryState =
                    TintState(
                        appliedTint = 0xbf000000.toInt(),
                        statusIconTint = 0xe6ffffff.toInt(),
                    ),
                liveStatusIconTint = 0xbf112233.toInt(),
            )

        assertEquals(0xbf000000.toInt(), resolved.appliedTint)
        assertEquals(0xbf112233.toInt(), resolved.statusIconTint)
    }

    @Test
    fun batteryTintIsFailNativeFallback() {
        val resolved =
            TintAuthority.resolveBatteryEvent(
                batteryState =
                    TintState(
                        appliedTint = 0xbf223344.toInt(),
                        statusIconTint = null,
                    ),
                liveStatusIconTint = null,
            )

        assertEquals(0xbf223344.toInt(), resolved.statusIconTint)
    }


    @Test
    fun batteryEventRejectsStaleStatusTint() {
        val resolved =
            TintAuthority.resolveBatteryEvent(
                batteryState =
                    TintState(
                        appliedTint = 0xbf112233.toInt(),
                        statusIconTint = 0xe6ffffff.toInt(),
                    ),
                liveStatusIconTint = null,
            )

        assertEquals(0xbf112233.toInt(), resolved.statusIconTint)
    }

    @Test
    fun statusIconEventUpdatesOnlyStatusAuthority() {
        val previous =
            TintState(
                appliedTint = 0xbf000000.toInt(),
                statusIconTint = 0xe6ffffff.toInt(),
            )

        val resolved =
            TintAuthority.resolveStatusIconEvent(
                previous = previous,
                liveStatusIconTint = 0xbf556677.toInt(),
            )

        assertEquals(0xbf000000.toInt(), resolved?.appliedTint)
        assertEquals(0xbf556677.toInt(), resolved?.statusIconTint)
    }

    @Test
    fun statusIconCanSeedTint() {
        val resolved =
            TintAuthority.resolveStatusIconEvent(
                previous = null,
                liveStatusIconTint = 0xe6ffffff.toInt(),
            )

        assertEquals(0xe6ffffff.toInt(), resolved?.appliedTint)
        assertEquals(0xe6ffffff.toInt(), resolved?.statusIconTint)
    }

    @Test
    fun reloadTintRebasesToLiveAuthority() {
        val transferred =
            TintState(
                appliedTint = 0xbf000000.toInt(),
                statusIconTint = 0xe6ffffff.toInt(),
            )

        val resolved =
            TintAuthority.rebaseTransferred(
                transferred = transferred,
                liveStatusIconTint = 0xbf334455.toInt(),
            )

        assertEquals(0xbf000000.toInt(), resolved.appliedTint)
        assertEquals(0xbf334455.toInt(), resolved.statusIconTint)
    }

    @Test
    fun transferredTintIsFallback() {
        val transferred =
            TintState(
                appliedTint = 0xbf000000.toInt(),
                statusIconTint = 0xe6ffffff.toInt(),
            )

        val resolved =
            TintAuthority.rebaseTransferred(
                transferred = transferred,
                liveStatusIconTint = null,
            )

        assertEquals(0xe6ffffff.toInt(), resolved.statusIconTint)
    }
}
