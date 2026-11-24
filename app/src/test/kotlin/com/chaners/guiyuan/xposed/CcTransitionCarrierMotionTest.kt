package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CcTransitionCarrierMotionTest {
    @Test
    fun carriedSourceUsesNativeCarrierMotionBeforeRootTargetInterpolation() {
        val source =
            floatArrayOf(75f, 70f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 100f)
        val currentCarrier =
            floatArrayOf(130f, 120f, 140f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 0.5f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(195f, result[0], 0.0001f)
        assertEquals(145f, result[1], 0.0001f)
        assertEquals(15f, result[2], 0.0001f)
        assertEquals(15f, result[5], 0.0001f)
    }

    @Test
    fun carriedSourceDoesNotRescaleSourceOffsetAtGestureStart() {
        val source =
            floatArrayOf(75f, 70f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(60f, 65f, 140f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 0f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(85f, result[0], 0.0001f)
        assertEquals(85f, result[1], 0.0001f)
        assertEquals(10f, result[2], 0.0001f)
        assertEquals(10f, result[5], 0.0001f)
    }

    @Test
    fun carriedSourceFollowsLiveFakeCarrierAtStart() {
        val source =
            floatArrayOf(75f, 50f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(60f, 58f, 140f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 0f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(85f, result[0], 0.0001f)
        assertEquals(58f, result[1], 0.0001f)
        assertEquals(source[2], result[2], 0.0001f)
        assertEquals(source[5], result[5], 0.0001f)
    }

    @Test
    fun carriedSourceLandsOnAbsoluteRootTargetEvenWhenCarriersDoNotConverge() {
        val source =
            floatArrayOf(75f, 50f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(154f, 136f, 140f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 1f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(target[0], result[0], 0.0001f)
        assertEquals(target[1], result[1], 0.0001f)
        assertEquals(target[2], result[2], 0.0001f)
        assertEquals(target[5], result[5], 0.0001f)
    }

    @Test
    fun latentSingleIconCanLandOnRootTargetWithoutGrowingToLargeSlotBox() {
        val source =
            floatArrayOf(75f, 50f, 20f, 0f, 0f, 20f)
        val target =
            floatArrayOf(235f, 150f, 75f, 0f, 0f, 75f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(154f, 136f, 140f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 1f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
                )

        assertEquals(target[0], result[0], 0.0001f)
        assertEquals(target[1], result[1], 0.0001f)
        assertEquals(source[2], result[2], 0.0001f)
        assertEquals(source[5], result[5], 0.0001f)
    }

    @Test
    fun latentAdditionalMobileKeepsShrinkOnlyPathBasis() {
        val source = transitionGeometry(width = 20f, height = 20f)
        val target = transitionGeometry(width = 75f, height = 75f)

        val result =
            CcTransitionMorphPolicy.interpolateSimilarityGeometry(
                    source = source,
                    target = target,
                    progress = 1f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
                )

        assertEquals(20f, result[2], 0.0001f)
        assertEquals(20f, result[5], 0.0001f)
    }

    @Test
    fun nativeTargetAxisRatiosPreserveIndependentWidthAndHeightScales() {
        val target = transitionGeometry(width = 20f, height = 50f)
        val current = transitionGeometry(width = 10f, height = 20f)
        assertEquals(
            2f,
            CcTransitionSpatialPolicy.relativeGeometryWidth(target, current) ?: -1f,
            0.0001f,
        )
        assertEquals(
            2.5f,
            CcTransitionSpatialPolicy.relativeGeometryHeight(target, current) ?: -1f,
            0.0001f,
        )
        val zeroWidth = transitionGeometry(width = 0f, height = 20f)
        assertNull(CcTransitionSpatialPolicy.relativeGeometryWidth(target, zeroWidth))
        assertEquals(
            2.5f,
            CcTransitionSpatialPolicy.relativeGeometryHeight(target, zeroWidth) ?: -1f,
            0.0001f,
        )
    }


    @Test
    fun steadyTransitionSourceUsesHostEndSlotInsteadOfInnerBatteryCenter() {
        val host = floatArrayOf(300f, 54f, 600f, 0f, 0f, 108f)

        val ltr =
            CcCarrierGeometryPolicy.endAnchoredSlotGeometry(
                hostGeometry = host,
                hostWidth = 600,
                hostHeight = 108,
                slotWidth = 105,
                isRtl = false,
            )
        requireNotNull(ltr)
        assertEquals(547.5f, ltr[0], 0.0001f)
        assertEquals(54f, ltr[1], 0.0001f)
        assertEquals(105f, ltr[2], 0.0001f)
        assertEquals(108f, ltr[5], 0.0001f)

        val rtl =
            CcCarrierGeometryPolicy.endAnchoredSlotGeometry(
                hostGeometry = host,
                hostWidth = 600,
                hostHeight = 108,
                slotWidth = 105,
                isRtl = true,
            )
        requireNotNull(rtl)
        assertEquals(52.5f, rtl[0], 0.0001f)
    }

    @Test
    fun sourceGeometryKeepsNativePositionButUsesStableRenderBasis() {
        val nativePosition = floatArrayOf(100f, 200f, 60f, 0f, 0f, 40f)
        val stableRenderBasis = floatArrayOf(900f, 900f, 105f, 0f, 0f, 169f)

        val result =
            CcCarrierGeometryPolicy.composeSourceGeometry(
                positionAuthority = nativePosition,
                basisAuthority = stableRenderBasis,
            )

        assertEquals(100f, result[0], 0.0001f)
        assertEquals(200f, result[1], 0.0001f)
        assertEquals(105f, result[2], 0.0001f)
        assertEquals(169f, result[5], 0.0001f)
    }

    @Test
    fun missingNativeMobileTextHasNoGuessedSignalSlotEndpoint() {
        assertNull(
            CcTransitionClipPolicy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_type_single", "mobile_type"),
                isRtl = false,
            ),
        )
        assertNull(
            CcTransitionClipPolicy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_type"),
                isRtl = true,
            ),
        )

        val signal = CcTransitionClipPolicy.semanticFallbackBounds(
            preferredChildEntries = listOf("mobile_signal"),
            isRtl = false,
        )
        val rtlSignal = CcTransitionClipPolicy.semanticFallbackBounds(
            preferredChildEntries = listOf("mobile_signal"),
            isRtl = true,
        )
        requireNotNull(signal)
        requireNotNull(rtlSignal)
        assertEquals(0.48f, signal.left, 0.0001f)
        assertEquals(0.52f, rtlSignal.right, 0.0001f)
    }

}
