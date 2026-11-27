package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CcNativePeerMotionTest {
    @Test
    fun nativePeerMotionReachesFinalWithoutChangingMeasuredRowOrDrifting() {
        val first = CcTransitionSpatialPolicy.nativePeerTranslationX(
            baselineX = 0f, appliedX = 0f,
            fakePeerRootX = 1100f, finalPeerRootX = 925f,
            localToRootScaleX = 1f, progress = 0.6f,
        )
        assertEquals(-105f, first!!, 0.001f)
        val held = CcTransitionSpatialPolicy.nativePeerTranslationX(
            baselineX = 0f, appliedX = first,
            fakePeerRootX = 995f, finalPeerRootX = 925f,
            localToRootScaleX = 1f, progress = 0.6f,
        )
        assertEquals(first, held!!, 0.001f)
        val reverse = CcTransitionSpatialPolicy.nativePeerTranslationX(
            baselineX = 0f, appliedX = first,
            fakePeerRootX = 995f, finalPeerRootX = 925f,
            localToRootScaleX = 1f, progress = 0.2f,
        )
        assertEquals(-35f, reverse!!, 0.001f)
        val endpoint = CcTransitionSpatialPolicy.nativePeerTranslationX(
            baselineX = 0f, appliedX = reverse,
            fakePeerRootX = 1065f, finalPeerRootX = 925f,
            localToRootScaleX = 1f, progress = 1f,
        )
        assertEquals(-175f, endpoint!!, 0.001f)
        assertEquals(
            0f,
            CcTransitionSpatialPolicy.nativePeerTranslationX(
                baselineX = 0f, appliedX = first,
                fakePeerRootX = 995f, finalPeerRootX = 925f,
                localToRootScaleX = 1f, progress = 0f,
            )!!,
            0.001f,
        )
    }

    @Test
    fun nativePeerMotionHonorsLocalScaleAndBaseline() {
        assertEquals(
            -147f,
            CcTransitionSpatialPolicy.nativePeerTranslationX(
                baselineX = 3f, appliedX = -47f,
                fakePeerRootX = 975f, finalPeerRootX = 900f,
                localToRootScaleX = 0.5f, progress = 0.75f,
            )!!,
            0.001f,
        )
        assertEquals(
            -15f,
            CcTransitionSpatialPolicy.nativePeerTranslationX(
                baselineX = 0f, appliedX = -10f,
                fakePeerRootX = 1020f, finalPeerRootX = 1060f,
                localToRootScaleX = -2f, progress = 0.5f,
            )!!,
            0.001f,
        )
    }

    @Test
    fun nativePeerMotionRejectsInvalidTravelAndScale() {
        assertNull(
            CcTransitionSpatialPolicy.nativePeerTranslationX(
                baselineX = 0f, appliedX = 0f,
                fakePeerRootX = -Float.MAX_VALUE,
                finalPeerRootX = Float.MAX_VALUE,
                localToRootScaleX = 1f, progress = 0.5f,
            ),
        )
        assertNull(
            CcTransitionSpatialPolicy.nativePeerTranslationX(
                baselineX = 0f, appliedX = 0f,
                fakePeerRootX = 1100f, finalPeerRootX = 925f,
                localToRootScaleX = 0f, progress = 0.5f,
            ),
        )
    }

    @Test
    fun nativePeerMotionRejectsEachNonFiniteGeometryInput() {
        for (invalid in floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            for (index in 0..4) {
                val inputs = floatArrayOf(0f, 0f, 1100f, 925f, 1f)
                inputs[index] = invalid
                assertNull(
                    CcTransitionSpatialPolicy.nativePeerTranslationX(
                        baselineX = inputs[0], appliedX = inputs[1],
                        fakePeerRootX = inputs[2], finalPeerRootX = inputs[3],
                        localToRootScaleX = inputs[4], progress = 0.5f,
                    ),
                )
            }
        }
    }

}
