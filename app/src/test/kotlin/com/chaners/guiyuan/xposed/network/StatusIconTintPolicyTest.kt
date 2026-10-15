package com.chaners.guiyuan.xposed.network

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusIconTintPolicyTest {
    @Test
    fun locationAwareTintWinsOverPeerManagerAndCachedFallback() {
        assertEquals(
            0xe6ffffff.toInt(),
            StatusIconTintPolicy.statusIconTint(
                locationAwareTint = 0xe6ffffff.toInt(),
                peerAppliedTint = 0xbf000000.toInt(),
                managerFallbackTint = 0xbf000000.toInt(),
                fallbackTint = 0xbf000000.toInt(),
            ),
        )
    }

    @Test
    fun peerTintWinsWhenLocationAwareTintIsUnavailable() {
        assertEquals(
            0xfff2f2f2.toInt(),
            StatusIconTintPolicy.statusIconTint(
                locationAwareTint = null,
                peerAppliedTint = 0xfff2f2f2.toInt(),
                managerFallbackTint = 0xdee5e5e5.toInt(),
                fallbackTint = 0xe6ffffff.toInt(),
            ),
        )
        assertEquals(
            0xdee5e5e5.toInt(),
            StatusIconTintPolicy.statusIconTint(
                locationAwareTint = null,
                peerAppliedTint = 0x00ffffff,
                managerFallbackTint = 0xdee5e5e5.toInt(),
                fallbackTint = 0xe6ffffff.toInt(),
            ),
        )
    }
}
