package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionReservationTest {
@Test
    fun nativeChargeRetargetPinsVisualsAndConvergesAtBothNativeEndpoints() {
        val p = 0.6f
        val peerShift = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = p,
            nativeDelta = -30f,
        )
        val batteryShift = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = p,
            nativeDelta = -15f,
        )
        assertEquals(30f, peerShift, 0.0001f)
        assertEquals(15f, batteryShift, 0.0001f)
        assertEquals(0f, CcTransitionPolicy.retargetWeight(0f, p), 0.0001f)
        assertEquals(1f, CcTransitionPolicy.retargetWeight(p, p), 0.0001f)
        assertEquals(0.5f, CcTransitionPolicy.retargetWeight(0.8f, p), 0.0001f)
        assertEquals(0f, CcTransitionPolicy.retargetWeight(1f, p), 0.0001f)
    }

    @Test
    fun rapidChargeReversalsAtHeldFractionDoNotAccumulateDisplacement() {
        val plugged = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = 0.6f,
            nativeDelta = -30f,
        )
        val unplugged = CcTransitionPolicy.retargetShift(
            priorShift = plugged, priorAnchor = 0.6f, progress = 0.6f,
            nativeDelta = 30f,
        )
        assertEquals(0f, unplugged, 0.0001f)
    }

    @Test
    fun nativeFakeRowChargeResizeDoesNotFabricateSourceMotion() {
        val root = floatArrayOf(912f, 0f, 1f, 0f, 0f, 1f)
        val chargingRow = floatArrayOf(1522f, 0f, 1f, 0f, 0f, 1f)
        val unpluggedRow = floatArrayOf(1567f, 0f, 1f, 0f, 0f, 1f)
        val offset = chargingRow[0] - root[0]
        val before = CcTransitionPolicy.rootAnchoredCarrier(chargingRow, root, offset)
        val after = CcTransitionPolicy.rootAnchoredCarrier(unpluggedRow, root, offset)
        assertEquals(before[0], after[0], 0.0001f)

        val held = 0.6f
        val correction = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null,
            progress = held, nativeDelta = 30f,
        )
        assertEquals(0f, 30f + correction, 0.0001f)
        assertEquals(0f, correction *
            CcTransitionPolicy.retargetWeight(0f, held), 0.0001f)
        assertEquals(0f, correction *
            CcTransitionPolicy.retargetWeight(1f, held), 0.0001f)
    }

    @Test
    fun dualRowMorphKeepsItsOwnPhasesAcrossNativeLayoutRetarget() {
        val source = floatArrayOf(0f, 0f, 2f, 0f, 0f, 2f)
        val original = floatArrayOf(100f, 0f, 2f, 0f, 0f, 2f)
        val target = original.copyOf().apply { this[0] += 30f }
        val correction = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = 0.6f,
            nativeDelta = 30f,
        )
        target[0] += correction
        for (p in listOf(0.3f, 0.6f, 0.85f, 1f)) {
            val before = CcTransitionPolicy.dualMobileRowMotionGeometry(
                source = source,
                target = original,
                groupTravelX = 100f,
                groupTravelY = 0f,
                progress = p,
                scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
            )
            val after = CcTransitionPolicy.dualMobileRowMotionGeometry(
                source = source,
                target = target,
                groupTravelX = 130f + correction,
                groupTravelY = 0f,
                progress = p,
                scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
            )
            assertEquals(before[0], after[0], 0.0001f)
        }
    }

    @Test
    fun chargingFollowerReceivesRebasedBatteryNumberTarget() {
        val follower = floatArrayOf(10f, 0f, 2f, 0f, 0f, 2f)
        val numberSource = floatArrayOf(0f, 0f, 2f, 0f, 0f, 2f)
        val nativeTarget = floatArrayOf(100f, 0f, 2f, 0f, 0f, 2f)
        val correction = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = 0.6f,
            nativeDelta = 15f,
        )
        val rebasedTarget = nativeTarget.copyOf().apply {
            this[0] += 15f + correction
        }
        val before = requireNotNull(CcTransitionPolicy.followAnchorGeometry(
            follower = follower,
            sourceAnchor = numberSource,
            currentAnchor = nativeTarget,
        ))
        val after = requireNotNull(CcTransitionPolicy.followAnchorGeometry(
            follower = follower,
            sourceAnchor = numberSource,
            currentAnchor = rebasedTarget,
        ))
        assertEquals(before[0], after[0], 0.0001f)
    }

    @Test
    fun finalRowLayoutMayMoveInTwoPassesAfterBatteryMeasurement() {
        val p = 0.6f
        val first = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = p,
            nativeDelta = 15f,
        )
        val second = CcTransitionPolicy.retargetShift(
            priorShift = first, priorAnchor = p, progress = p,
            nativeDelta = 15f,
        )
        assertEquals(-30f, second, 0.0001f)
    }
}
