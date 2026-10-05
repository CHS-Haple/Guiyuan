package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlCenterTransitionIslandTest {
    @Test
    fun batteryIslandRingExitUsesLogicalStartWhileNativeTargetRowReflows() {
        val direction =
            ControlCenterTransitionPolicy
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
            ControlCenterTransitionPolicy
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
                ControlCenterTransitionPolicy
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
            ControlCenterTransitionPolicy
                .usesSemanticTransitionReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = true,
                ),
        )
        assertTrue(
            ControlCenterTransitionPolicy
                .usesSemanticTransitionReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = false,
                ),
        )
        assertTrue(
            ControlCenterTransitionPolicy
                .usesSemanticTransitionReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = null,
                ),
        )
        assertTrue(
            ControlCenterTransitionPolicy
                .usesSemanticTransitionReservation(
                    sourceScene = SourceScene.KEYGUARD,
                    charging = true,
                    nativeBatteryIslandActive = true,
                ),
        )
    }

    @Test
    fun nativeIslandCollisionDoesNotDisableGuiyuanPaddingReflow() {
        assertTrue(
            ControlCenterTransitionPolicy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            ControlCenterTransitionPolicy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.KEYGUARD,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            ControlCenterTransitionPolicy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = false,
                ),
        )
        assertTrue(
            ControlCenterTransitionPolicy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = null,
                ),
        )
        assertTrue(
            !ControlCenterTransitionPolicy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.UNKNOWN,
                    genericIslandShowing = true,
                ),
        )
    }

    @Test
    fun verifiedSourceScenesKeepSemanticReservationThroughProjection() {
        assertTrue(
            ControlCenterTransitionPolicy
                .usesSemanticTransitionReservation(SourceScene.HOME),
        )
        assertTrue(
            ControlCenterTransitionPolicy
                .usesSemanticTransitionReservation(SourceScene.KEYGUARD),
        )
        assertTrue(
            !ControlCenterTransitionPolicy
                .usesSemanticTransitionReservation(SourceScene.UNKNOWN),
        )
    }

    @Test
    fun nativeFinalAppearanceConsumesOnlyRemainingOutwardDistance() {
        assertEquals(
            0.696f,
            ControlCenterTransitionPolicy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.2f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.9316f,
            ControlCenterTransitionPolicy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            ControlCenterTransitionPolicy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 1f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.62f,
            ControlCenterTransitionPolicy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = false,
            ),
            0.0001f,
        )
    }
}
