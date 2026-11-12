package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CcTransitionClipGeometryTest {
@Test
    fun exactBarTargetCompensatesInsideSimilarityBasis() {
        val outerScale =
            MobileSignalTargetGeometry.outerSimilarityScale(
                targetWidthRatio = 1.5f,
                targetHeightRatio = 2f,
            )
        assertEquals(1f, outerScale, 0.0001f)
        assertEquals(
            1.5f,
            MobileSignalTargetGeometry.exactTargetAxisCompensation(
                targetAxisRatio = 1.5f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
        assertEquals(
            2f,
            MobileSignalTargetGeometry.exactTargetAxisCompensation(
                targetAxisRatio = 2f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
    }

    @Test
    fun exactBarTargetPreservesUniformOuterShrinkAndCompensatesAxes() {
        val outerScale =
            MobileSignalTargetGeometry.outerSimilarityScale(
                targetWidthRatio = 0.75f,
                targetHeightRatio = 0.5f,
            )
        assertEquals(0.5f, outerScale, 0.0001f)
        assertEquals(
            1.5f,
            MobileSignalTargetGeometry.exactTargetAxisCompensation(
                targetAxisRatio = 0.75f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            MobileSignalTargetGeometry.exactTargetAxisCompensation(
                targetAxisRatio = 0.5f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
    }

    @Test
    fun unmatchedComponentsFadeOutFastWithoutChangingTheirScale() {
        assertEquals(
            1f,
            CcTransitionPolicy.unmatchedExitOpacity(0f),
            0.0001f,
        )
        assertEquals(
            0.125f,
            CcTransitionPolicy.unmatchedExitOpacity(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            CcTransitionPolicy.unmatchedExitOpacity(1f),
            0.0001f,
        )
    }

    @Test
    fun horizontalClipKeepsPixelsOpaqueAndAnchorsTowardChosenEdge() {
        val policy = CcTransitionPolicy
        val bounds = StatusPainter.TransitionBounds(0f, 10f, 100f, 30f)
        val rightAnchored =
            policy.horizontalClipBounds(
                bounds = bounds,
                visibleFraction = 0.25f,
                anchorRight = true,
            )
        requireNotNull(rightAnchored)
        assertEquals(75f, rightAnchored[0], 0.0001f)
        assertEquals(100f, rightAnchored[2], 0.0001f)

        val leftAnchored =
            policy.horizontalClipBounds(
                bounds = bounds,
                visibleFraction = 0.25f,
                anchorRight = false,
            )
        requireNotNull(leftAnchored)
        assertEquals(0f, leftAnchored[0], 0.0001f)
        assertEquals(25f, leftAnchored[2], 0.0001f)

        assertNull(
            policy.horizontalClipBounds(
                bounds = StatusPainter.TransitionBounds(0f, 0f, 100f, 20f),
                visibleFraction = 0f,
                anchorRight = true,
            ),
        )
    }

    @Test
    fun latentMobileClipEnvelopeCoversExactTargetAxisExpansion() {
        val source =
            StatusPainter.TransitionBounds(
                left = 10f,
                top = 20f,
                right = 50f,
                bottom = 60f,
            )
        val expanded =
            CcTransitionPolicy.expandedClipBounds(
                bounds = source,
                widthScale = 1.5f,
                heightScale = 1.25f,
            )

        assertEquals(0f, expanded.left, 0.0001f)
        assertEquals(15f, expanded.top, 0.0001f)
        assertEquals(60f, expanded.right, 0.0001f)
        assertEquals(65f, expanded.bottom, 0.0001f)

        val unchanged =
            CcTransitionPolicy.expandedClipBounds(
                bounds = source,
                widthScale = 0.75f,
                heightScale = Float.NaN,
            )
        assertEquals(source, unchanged)
    }

    @Test
    fun nativePeerTintNeverFallsBackToBatteryTintAuthority() {
        val policy = CcTintPolicy
        val peer = 0xffe0e0e0.toInt()
        val cached = 0xffdddddd.toInt()

        assertEquals(peer, policy.selectNativeTransitionTint(peer, cached))
        assertEquals(cached, policy.selectNativeTransitionTint(null, cached))
        assertNull(policy.selectNativeTransitionTint(null, null))
    }
}
