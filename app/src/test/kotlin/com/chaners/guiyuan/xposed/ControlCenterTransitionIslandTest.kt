package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlCenterTransitionIslandTest {
    @Test
    fun batteryIslandRingExitUsesLogicalStartWhileNativeTargetRowReflows() {
        val direction =
            ControlCenterTransition
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
            ControlCenterTransition
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
    fun nonBatteryIslandRingExitKeepsLiveBuild544GeometryDirection() {
        BatteryRingTransition.ExitDirection.entries.forEach { live ->
            assertEquals(
                live,
                ControlCenterTransition
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
            ControlCenterTransition
                .usesSemanticTransitionReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = true,
                ),
        )
        assertTrue(
            ControlCenterTransition
                .usesSemanticTransitionReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = false,
                ),
        )
        assertTrue(
            ControlCenterTransition
                .usesSemanticTransitionReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = null,
                ),
        )
        assertTrue(
            ControlCenterTransition
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
            ControlCenterTransition
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            ControlCenterTransition
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.KEYGUARD,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            ControlCenterTransition
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = false,
                ),
        )
        assertTrue(
            ControlCenterTransition
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = null,
                ),
        )
        assertTrue(
            !ControlCenterTransition
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.UNKNOWN,
                    genericIslandShowing = true,
                ),
        )
    }

    @Test
    fun verifiedSourceScenesKeepSemanticReservationThroughProjection() {
        assertTrue(
            ControlCenterTransition
                .usesSemanticTransitionReservation(SourceScene.HOME),
        )
        assertTrue(
            ControlCenterTransition
                .usesSemanticTransitionReservation(SourceScene.KEYGUARD),
        )
        assertTrue(
            !ControlCenterTransition
                .usesSemanticTransitionReservation(SourceScene.UNKNOWN),
        )
    }

    @Test
    fun nativeFinalAppearanceConsumesOnlyRemainingOutwardDistance() {
        assertEquals(
            0.696f,
            ControlCenterTransition.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.2f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.9316f,
            ControlCenterTransition.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            ControlCenterTransition.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 1f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.62f,
            ControlCenterTransition.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = false,
            ),
            0.0001f,
        )
    }
}
