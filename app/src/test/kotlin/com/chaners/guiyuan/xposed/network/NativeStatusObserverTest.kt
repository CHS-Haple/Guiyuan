package com.chaners.guiyuan.xposed.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NativeStatusObserverTest {
    @Test
    fun fatalObservationOrFailureHandlerErrorsPropagate() {
        val fatalObservation = ThreadDeath()
        try {
            NativeStatusObserver.observeAfterNative(1, { error("should not report") }) {
                throw fatalObservation
            }
            throw AssertionError("fatal observer failure was swallowed")
        } catch (error: ThreadDeath) {
            assertSame(fatalObservation, error)
        }

        val fatalReporter = OutOfMemoryError("fatal reporter")
        try {
            NativeStatusObserver.observeAfterNative(1, { throw fatalReporter }) {
                error("ordinary observer failure")
            }
            throw AssertionError("fatal reporter failure was swallowed")
        } catch (error: OutOfMemoryError) {
            assertSame(fatalReporter, error)
        }
    }

    @Test
    fun observationOrReportingFailureKeepsNativeResult() {
        val nativeResult = Any()
        val observationError = IllegalStateException("observer failed")
        var reported: Throwable? = null
        val observed = NativeStatusObserver.observeAfterNative(nativeResult, { reported = it }) {
            throw observationError
        }
        assertSame(nativeResult, observed)
        assertSame(observationError, reported)

        val value = NativeStatusObserver.observeAfterNative(false, { throw IllegalStateException("logging failed") }) {
            throw observationError
        }
        assertEquals(false, value)
        assertEquals(7, NativeStatusObserver.observeAfterNative(7, { throw it }) { })
    }

    @Test
    fun representedSlotsAreNotEligibleVisibleTintAuthorities() {
        listOf("combined_status", "wifi", "mobile", "stacked_mobile", "airplane", "no_sim").forEach { slot ->
            assertEquals(
                false,
                NativeStatusObserver.isTintAuthorityCandidate(
                    slot = slot,
                    visible = true,
                    width = 75,
                    height = 75,
                ),
            )
        }
    }

    @Test
    fun visibleNonRepresentedPeerCanAnchorHomeTint() {
        assertEquals(
            true,
            NativeStatusObserver.isTintAuthorityCandidate(
                slot = "vpn",
                visible = true,
                width = 75,
                height = 75,
            ),
        )
        assertEquals(
            false,
            NativeStatusObserver.isTintAuthorityCandidate(
                slot = "vpn",
                visible = false,
                width = 75,
                height = 75,
            ),
        )
        assertEquals(
            false,
            NativeStatusObserver.isTintAuthorityCandidate(
                slot = "vpn",
                visible = true,
                width = 0,
                height = 75,
            ),
        )
    }
}
