package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlCenterTransitionIslandTest {
    @Test
    fun batteryIslandRingExitUsesLogicalStartWhileNativeTargetRowReflows() {
        val direction =
            ControlCenterTransitionOwner.Policy
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
            ControlCenterTransitionOwner.Policy
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
    fun nonBatteryIslandRingExitKeepsLiveBuild544GeometryDirection() {
        BatteryRingTransitionPolicy.ExitDirection.entries.forEach { live ->
            assertEquals(
                live,
                ControlCenterTransitionOwner.Policy
                    .batteryRingExitDirection(
                        liveCenterDirection = live,
                        nativeBatteryIslandActive = false,
                        targetRowRtl = false,
                    ),
            )
        }
    }

    @Test
    fun islandScenesKeepSemanticReservationForGuiyuanExpansion() {
        assertTrue(
            ControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = true,
                ),
        )
        assertTrue(
            ControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = false,
                ),
        )
        assertTrue(
            ControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = null,
                ),
        )
        assertTrue(
            ControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(
                    sourceScene = CombinedStatusSourceScene.KEYGUARD,
                    charging = true,
                    nativeBatteryIslandActive = true,
                ),
        )
    }

    @Test
    fun nativeIslandCollisionDoesNotDisableGuiyuanPaddingReflow() {
        assertTrue(
            ControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            ControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.KEYGUARD,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            ControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    genericIslandShowing = false,
                ),
        )
        assertTrue(
            ControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    genericIslandShowing = null,
                ),
        )
        assertTrue(
            !ControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.UNKNOWN,
                    genericIslandShowing = true,
                ),
        )
    }

    @Test
    fun verifiedSourceScenesKeepSemanticReservationThroughProjection() {
        assertTrue(
            ControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(CombinedStatusSourceScene.HOME),
        )
        assertTrue(
            ControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(CombinedStatusSourceScene.KEYGUARD),
        )
        assertTrue(
            !ControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(CombinedStatusSourceScene.UNKNOWN),
        )
    }

    @Test
    fun nativeFinalAppearanceConsumesOnlyRemainingOutwardDistance() {
        assertEquals(
            0.696f,
            ControlCenterTransitionOwner.Policy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.2f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.9316f,
            ControlCenterTransitionOwner.Policy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            ControlCenterTransitionOwner.Policy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 1f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.62f,
            ControlCenterTransitionOwner.Policy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = false,
            ),
            0.0001f,
        )
    }
}
