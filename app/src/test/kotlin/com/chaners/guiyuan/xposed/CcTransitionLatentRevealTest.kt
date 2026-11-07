package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionLatentRevealTest {
@Test
    fun latentRevealRequiresRealVisualReservationAndTargetProximity() {
        val target = transitionGeometry(centerX = 100f, centerY = 100f, width = 20f, height = 20f)

        assertEquals(
            0f,
            CcTransitionPolicy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 95f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 0f,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            CcTransitionPolicy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 79f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 90f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 93f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 0.35f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy.latentRevealVisibleFraction(
                current = target,
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
    }

    @Test
    fun latentRevealAcceleratesAfterOccupancyUnlockWithoutChangingTheGate() {
        assertEquals(
            0f,
            CcTransitionPolicy
                .acceleratedLatentRevealProgress(0f),
            0.0001f,
        )
        assertTrue(
            CcTransitionPolicy
                .acceleratedLatentRevealProgress(0.2f) > 0.5f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy
                .acceleratedLatentRevealProgress(0.35f),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy
                .acceleratedLatentRevealProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun latentReservationProgressTracksVisibleEnvelopeCoverage() {
        assertEquals(
            0f,
            CcTransitionPolicy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 100,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
        assertEquals(
            0.5f,
            CcTransitionPolicy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 150,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 200,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
    }
}
