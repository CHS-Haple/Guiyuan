package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionIslandTest {
    @Test
    fun batteryIslandExitFollowsLogicalStart() {
        val direction =
            CcTransitionPolicy
                .batteryRingExitDirection(
                    liveCenterDirection =
                        BatteryRingTransitionPolicy.ExitDirection.RIGHT,
                    nativeBatteryIslandActive = true,
                    targetRowRtl = false,
                )
        assertEquals(
            BatteryRingTransitionPolicy.ExitDirection.LEFT,
            direction,
        )

        val rtlDirection =
            CcTransitionPolicy
                .batteryRingExitDirection(
                    liveCenterDirection =
                        BatteryRingTransitionPolicy.ExitDirection.LEFT,
                    nativeBatteryIslandActive = true,
                    targetRowRtl = true,
                )
        assertEquals(
            BatteryRingTransitionPolicy.ExitDirection.RIGHT,
            rtlDirection,
        )
    }

    @Test
    fun nonBatteryIslandUsesLiveDirectionOrRowFallback() {
        val policy = CcTransitionPolicy
        val exit = BatteryRingTransitionPolicy.ExitDirection
        listOf(exit.LEFT, exit.RIGHT).forEach { live ->
            assertEquals(
                live,
                policy.batteryRingExitDirection(
                    liveCenterDirection = live,
                    nativeBatteryIslandActive = false,
                    targetRowRtl = false,
                ),
            )
        }

        assertEquals(
            exit.LEFT,
            policy.batteryRingExitDirection(
                liveCenterDirection = exit.NONE,
                nativeBatteryIslandActive = false,
                targetRowRtl = false,
            ),
        )
        assertEquals(
            exit.RIGHT,
            policy.batteryRingExitDirection(
                liveCenterDirection = exit.NONE,
                nativeBatteryIslandActive = false,
                targetRowRtl = true,
            ),
        )
    }

    @Test
    fun semanticReservationRequiresKnownSourceScene() {
        assertTrue(CcTransitionPolicy.usesSemanticTransitionReservation(SourceScene.HOME))
        assertTrue(CcTransitionPolicy.usesSemanticTransitionReservation(SourceScene.KEYGUARD))
        assertTrue(!CcTransitionPolicy.usesSemanticTransitionReservation(SourceScene.UNKNOWN))
    }

    @Test
    fun nativeFinalAppearanceConsumesOnlyRemainingOutwardDistance() {
        assertEquals(
            0.696f,
            CcTransitionPolicy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.2f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.9316f,
            CcTransitionPolicy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 1f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.62f,
            CcTransitionPolicy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = false,
            ),
            0.0001f,
        )
    }
}
