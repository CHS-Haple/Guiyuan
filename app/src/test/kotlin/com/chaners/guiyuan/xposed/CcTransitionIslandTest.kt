package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlCenterTransitionIslandTest {
    @Test
    fun batteryIslandRingExitUsesLogicalStartWhileNativeTargetRowReflows() {
        val direction =
            CcTransition
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
            CcTransition
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
                CcTransition
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
            CcTransition
                .usesSemanticTransitionReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = true,
                ),
        )
        assertTrue(
            CcTransition
                .usesSemanticTransitionReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = false,
                ),
        )
        assertTrue(
            CcTransition
                .usesSemanticTransitionReservation(
                    sourceScene = SourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = null,
                ),
        )
        assertTrue(
            CcTransition
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
            CcTransition
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            CcTransition
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.KEYGUARD,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            CcTransition
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = false,
                ),
        )
        assertTrue(
            CcTransition
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.HOME,
                    genericIslandShowing = null,
                ),
        )
        assertTrue(
            !CcTransition
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = SourceScene.UNKNOWN,
                    genericIslandShowing = true,
                ),
        )
    }

    @Test
    fun verifiedSourceScenesKeepSemanticReservationThroughProjection() {
        assertTrue(
            CcTransition
                .usesSemanticTransitionReservation(SourceScene.HOME),
        )
        assertTrue(
            CcTransition
                .usesSemanticTransitionReservation(SourceScene.KEYGUARD),
        )
        assertTrue(
            !CcTransition
                .usesSemanticTransitionReservation(SourceScene.UNKNOWN),
        )
    }

    @Test
    fun nativeFinalAppearanceConsumesOnlyRemainingOutwardDistance() {
        assertEquals(
            0.696f,
            CcTransition.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.2f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.9316f,
            CcTransition.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransition.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 1f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.62f,
            CcTransition.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = false,
            ),
            0.0001f,
        )
    }
}
