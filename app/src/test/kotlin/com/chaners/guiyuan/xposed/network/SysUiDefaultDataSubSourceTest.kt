package com.chaners.guiyuan.xposed.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiDefaultDataSubSourceTest {
    @Test
    fun failureRetainsNativeSubscriptionButRetriesDelivery() {
        SysUiDefaultDataSubSource.detach()
        try {
            val failure = IllegalStateException("refresh interrupted")
            assertSame(
                failure,
                runCatching {
                    SysUiDefaultDataSubSource.deliverSubscription(12, false) { throw failure }
                }.exceptionOrNull(),
            )
            assertEquals(12, SysUiDefaultDataSubSource.currentSubscriptionId())

            var count = 0
            assertTrue(SysUiDefaultDataSubSource.deliverSubscription(12, false) { count++ })
            assertFalse(SysUiDefaultDataSubSource.deliverSubscription(12, false) { count++ })
            assertEquals(1, count)
        } finally {
            SysUiDefaultDataSubSource.detach()
        }
    }

    @Test
    fun seedCanRedeliverAnUnchangedSubscription() {
        SysUiDefaultDataSubSource.detach()
        try {
            var count = 0
            assertTrue(SysUiDefaultDataSubSource.deliverSubscription(9, true) { count++ })
            assertTrue(SysUiDefaultDataSubSource.deliverSubscription(9, true) { count++ })
            assertEquals(2, count)
            assertEquals(9, SysUiDefaultDataSubSource.currentSubscriptionId())
        } finally {
            SysUiDefaultDataSubSource.detach()
        }
    }

    @Test
    fun oldDeliveryFailureDoesNotEraseNewerSuccess() {
        SysUiDefaultDataSubSource.detach()
        try {
            val error = IllegalStateException("stale")
            assertSame(
                error,
                runCatching {
                    SysUiDefaultDataSubSource.deliverSubscription(3, false) {
                        assertTrue(SysUiDefaultDataSubSource.deliverSubscription(4, false) {})
                        throw error
                    }
                }.exceptionOrNull(),
            )
            assertEquals(4, SysUiDefaultDataSubSource.currentSubscriptionId())
            assertFalse(SysUiDefaultDataSubSource.deliverSubscription(4, false) {})
        } finally {
            SysUiDefaultDataSubSource.detach()
        }
    }
}
