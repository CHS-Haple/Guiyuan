package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionIslandTest {
    @Test
    fun islandRingExitUsesLogicalStart() {
        val direction =
            CcTransitionPolicy
                .batteryRingExitDirection(
                    liveCenterDirection =
                        BatteryRingTransition.ExitDirection.RIGHT,
                    nativeBatteryIslandActive = true,
                    targetRowRtl = false,
                )
        assertEquals(
            BatteryRingTransition.ExitDirection.LEFT,
            direction,
        )

        val rtlDirection =
            CcTransitionPolicy
                .batteryRingExitDirection(
                    liveCenterDirection =
                        BatteryRingTransition.ExitDirection.LEFT,
                    nativeBatteryIslandActive = true,
                    targetRowRtl = true,
                )
        assertEquals(
            BatteryRingTransition.ExitDirection.RIGHT,
            rtlDirection,
        )
    }

    @Test
    fun nonBatteryIslandKeepsDirection() {
        BatteryRingTransition.ExitDirection.entries.forEach { live ->
            assertEquals(
                live,
                CcTransitionPolicy
                    .batteryRingExitDirection(
                        liveCenterDirection = live,
                        nativeBatteryIslandActive = false,
                        targetRowRtl = false,
                    ),
            )
        }
    }

    @Test
    fun islandKeepsSemanticReservation() {
        assertTrue(
            CcTransitionPolicy
                .usesSemanticReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = true,
                ),
        )
        assertTrue(
            CcTransitionPolicy
                .usesSemanticReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = false,
                ),
        )
        assertTrue(
            CcTransitionPolicy
                .usesSemanticReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = null,
                ),
        )
        assertTrue(
            CcTransitionPolicy
                .usesSemanticReservation(
                    sourceScene = SourceScene.KEYGUARD,
                    charging = true,
                    nativeBatteryIslandActive = true,
                ),
        )
    }

    @Test
    fun islandCollisionKeepsPaddingReflow() {
        assertTrue(
            CcTransitionPolicy
                .allowsNativePadding(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            CcTransitionPolicy
                .allowsNativePadding(
                    sourceScene = SourceScene.KEYGUARD,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            CcTransitionPolicy
                .allowsNativePadding(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = false,
                ),
        )
        assertTrue(
            CcTransitionPolicy
                .allowsNativePadding(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = null,
                ),
        )
        assertTrue(
            !CcTransitionPolicy
                .allowsNativePadding(
                    sourceScene = SourceScene.UNKNOWN,
                    genericIslandShowing = true,
                ),
        )
    }

    @Test
    fun verifiedSceneKeepsReservation() {
        assertTrue(
            CcTransitionPolicy
                .usesSemanticReservation(SourceScene.HOME),
        )
        assertTrue(
            CcTransitionPolicy
                .usesSemanticReservation(SourceScene.KEYGUARD),
        )
        assertTrue(
            !CcTransitionPolicy
                .usesSemanticReservation(SourceScene.UNKNOWN),
        )
    }

    @Test
    fun finalAppearanceUsesRemainingDistance() {
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
