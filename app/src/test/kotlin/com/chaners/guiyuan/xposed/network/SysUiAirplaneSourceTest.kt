package com.chaners.guiyuan.xposed.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiAirplaneSourceTest {
    @Test
    fun failedDeliveryRetriesTheSameObservedState() {
        SysUiAirplaneSource.detach()
        try {
            val failed = IllegalStateException("renderer")
            assertSame(
                failed,
                runCatching {
                    SysUiAirplaneSource.deliverState(true, false) { throw failed }
                }.exceptionOrNull(),
            )

            var deliveries = 0
            assertTrue(SysUiAirplaneSource.deliverState(true, false) { deliveries++ })
            assertFalse(SysUiAirplaneSource.deliverState(true, false) { deliveries++ })
            assertEquals(1, deliveries)
        } finally {
            SysUiAirplaneSource.detach()
        }
    }

    @Test
    fun reattachedSourceSeedsTheSameAirplaneState() {
        SysUiAirplaneSource.detach()
        try {
            var deliveries = 0
            assertTrue(SysUiAirplaneSource.deliverState(false, false) { deliveries++ })
            assertTrue(SysUiAirplaneSource.deliverState(false, true) { deliveries++ })
            assertEquals(2, deliveries)
        } finally {
            SysUiAirplaneSource.detach()
        }
    }

    @Test
    fun failingOlderSeedCannotUndoNewerSameStateDelivery() {
        SysUiAirplaneSource.detach()
        try {
            val failure = IllegalStateException("old")
            assertSame(
                failure,
                runCatching {
                    SysUiAirplaneSource.deliverState(true, true) {
                        assertTrue(SysUiAirplaneSource.deliverState(true, true) {})
                        throw failure
                    }
                }.exceptionOrNull(),
            )
            assertFalse(SysUiAirplaneSource.deliverState(true, false) {})
        } finally {
            SysUiAirplaneSource.detach()
        }
    }
}
