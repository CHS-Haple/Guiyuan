package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class MobileTypeSizingPolicyTest {
    @Test
    fun mobileTypeWeightInterpolatesWithinRequestedAndNativeBounds() {
        val policy = MobileTypeTransitionPolicy
        assertEquals(200, policy.resolveWeight(200, 660, 0f))
        assertEquals(1000, policy.resolveWeight(1400, 660, 0f))
        assertEquals(830, policy.resolveWeight(1400, 660, 0.5f))
        assertEquals(660, policy.resolveWeight(1400, 660, 1f))
        assertEquals(830, policy.resolveWeight(660, 1400, 0.5f))
        assertEquals(1000, policy.resolveWeight(660, 1400, 1f))
        // The native CC target may be lighter than the minimum user setting.
        assertEquals(375, policy.resolveWeight(600, 150, 0.5f))
        assertEquals(150, policy.resolveWeight(600, 150, 1f))
        assertEquals(600, CenterGeometry.DEFAULT_MOBILE_TYPE_WEIGHT)
    }

    @Test
    fun mobileTypeKeepsHostCompensationButFollowsCombinedScale() {
        val base = 39f
        val hostScale = 1.5f

        fun physicalSize(combinedScale: Float): Float {
            val canvasScale = hostScale * combinedScale
            val local =
                MobileTypeScalePolicy.localValue(
                    baseValue = base,
                    canvasScale = canvasScale,
                    combinedScale = combinedScale,
                    scaleWithCanvas = false,
                )
            return local * canvasScale
        }

        assertEquals(base, physicalSize(1f), 0.0001f)
        assertEquals(base * 0.6f, physicalSize(0.6f), 0.0001f)
    }

    @Test
    fun previewCanvasScalingKeepsExistingDirectScalePath() {
        val base = 39f
        assertEquals(
            base,
            MobileTypeScalePolicy.localValue(
                baseValue = base,
                canvasScale = 0.6f,
                combinedScale = 0.6f,
                scaleWithCanvas = true,
            ),
            0f,
        )
    }
}
