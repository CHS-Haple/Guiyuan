package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.InternetState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileSignalMorphPolicyTest {
    private val morph = MobileSignalMorphPolicy
    private val stages = MobileSignalStagePolicy

    @Test
    fun switchingWifiAndMobileTypeRelocatesDotsWithoutSnapping() {
        val mobile = CenterIndicator.MobileType("5G", false, internet = InternetState.VALIDATED)
        val wifi = CenterIndicator.Wifi(3, InternetState.VALIDATED)
        val policy = MobileDotRelocationPolicy
        assertTrue(policy.shouldInterpolate(mobile, wifi, true, false))
        assertTrue(policy.shouldInterpolate(wifi, mobile, false, true))
        assertFalse(policy.shouldInterpolate(mobile, mobile, true, true))
        assertFalse(policy.shouldInterpolate(null, wifi, false, false))
        assertEquals(30f, policy.coordinate(30f, 90f, 0f), 0f)
        assertEquals(60f, policy.coordinate(30f, 90f, 0.5f), 0f)
        assertEquals(90f, policy.coordinate(30f, 90f, 1f), 0f)
        assertEquals(30f, policy.coordinate(90f, 30f, 1f), 0f)
        assertEquals(30f, policy.coordinate(30f, 90f, -1f), 0f)
        assertEquals(90f, policy.coordinate(30f, 90f, 2f), 0f)
    }


    @Test
    fun bothSidesCollapseDoesNotSendVisibleDotsInTheOppositeDirection() {
        // The shrinking outer dot remains in place while visible; it is
        // packed into the survivor's anchor only once its radius has gone to zero.
        for (step in 0..99) {
            assertEquals(-90f, morph.bothSidesDotX(
                -90f, -40f, -130f, -150f, step / 100f, 0f, 0f,
            ), 0f)
        }
        assertEquals(-40f, morph.bothSidesDotX(-90f, -40f, -130f, -150f, 1f, 0f, 0f), 0f)
        assertEquals(-130f, morph.bothSidesDotX(-90f, -40f, -130f, -150f, 1f, 1f, 0f), 0f)
        assertEquals(-150f, morph.bothSidesDotX(-90f, -40f, -130f, -150f, 1f, 1f, 1f), 0f)
    }

    @Test
    fun onlyNativeAggregateAnchorsFormVerticalPairAndSpreadHorizontally() {
        val aggregateX = 25f
        val upperY = 11f
        val lowerY = 33f
        for (source in listOf(-40f, 40f)) {
            assertEquals(aggregateX, morph.bothSidesDotX(
                source, source, aggregateX, aggregateX, 0f, 1f, 0f,
            ), 0f)
        }
        for (step in 0..20) {
            val spread = step / 20f
            // The per-dot target Y may differ: that must not tilt the spread.
            for (targetY in listOf(5f, 22f, 45f)) {
                assertEquals(upperY, morph.bothSidesDotY(
                    8f, 8f, upperY, targetY, 0f, 1f, 0f,
                ), 0f)
                assertEquals(lowerY, morph.bothSidesDotY(
                    36f, 36f, lowerY, targetY, 0f, 1f, 0f,
                ), 0f)
            }
            assertEquals(aggregateX + (70f - aggregateX) * spread,
                morph.bothSidesDotX(-40f, -40f, aggregateX, 70f, 0f, 1f, spread), 0.0001f)
        }
        // Native final ink may have different Y; match it only as bars morph.
        assertEquals(22f, morph.bothSidesDotY(8f, 8f, upperY, 22f, 0f, 1f, 1f), 0f)
        // Separate SIM targets retain independent anchors, no shared-X coercion.
        assertEquals(-60f, morph.bothSidesDotX(-40f, -40f, -60f, -75f, 0f, 1f, 0f), 0f)
        assertEquals(60f, morph.bothSidesDotX(40f, 40f, 60f, 75f, 0f, 1f, 0f), 0f)
    }

    @Test
    fun collapseEndsAtThirtyPercentWithoutChangingLaterPhases() {
        fun phase(p: Float): Float = stages.handoffProgress(p * p)
        assertEquals(0f, stages.trimProgress(phase(0f)), 0f)
        assertEquals(0.5f, stages.trimProgress(phase(0.15f)), 0.0001f)
        assertEquals(1f, stages.trimProgress(phase(0.30f)), 0.0001f)
        assertEquals(0f, stages.moveProgress(phase(0.30f)), 0.0001f)
        assertEquals(0.5f, stages.moveProgress(phase(0.435f)), 0.0001f)
        assertEquals(1f, stages.moveProgress(phase(0.57f)), 0.0001f)
        assertEquals(0f, stages.rowProgress(phase(0.57f)), 0.0001f)
        assertEquals(0.5f, stages.rowProgress(phase(0.645f)), 0.0001f)
        assertEquals(1f, stages.rowProgress(phase(0.72f)), 0.0001f)
        assertEquals(0f, stages.barProgress(phase(0.72f)), 0.0001f)
        assertEquals(1f, stages.barProgress(phase(1f)), 0f)
    }

    @Test
    fun dotsCollapseInReverseRevealOrderWithoutMovingTheAnchor() {
        for (i in 0..3) assertEquals(1f, morph.dotScale(i, morph.dotTrim(i, 0f), 0f), 0f)
        assertEquals(0f, morph.dotTrim(0, 1f), 0f)
        assertEquals(1f, morph.dotScale(0, morph.dotTrim(0, 1f), 0f), 0f)

        // Each third of the trim stage removes just one dot, right-to-left.
        val first = 1f / 6f
        val middle = 0.5f
        val last = 5f / 6f
        assertEquals(0.5f, morph.dotTrim(3, first), 0.0001f)
        assertEquals(0f, morph.dotTrim(2, first), 0f)
        assertEquals(0f, morph.dotTrim(1, first), 0f)
        assertEquals(1f, morph.dotTrim(3, middle), 0f)
        assertEquals(0.5f, morph.dotTrim(2, middle), 0.0001f)
        assertEquals(0f, morph.dotTrim(1, middle), 0f)
        assertEquals(1f, morph.dotTrim(2, last), 0f)
        assertEquals(0.5f, morph.dotTrim(1, last), 0.0001f)

        for (i in 1..3) {
            assertEquals(0f, morph.dotScale(i, morph.dotTrim(i, 1f), 0f), 0f)
        }
        // Position and radius use the same local trim; untouched dots stay still.
        assertEquals(
            40f,
            morph.dotCoordinate(70f, 10f, 100f, 130f, morph.dotTrim(3, first), 0f, 0f),
            0.0001f,
        )
        assertEquals(
            70f,
            morph.dotCoordinate(70f, 10f, 100f, 130f, morph.dotTrim(2, first), 0f, 0f),
            0f,
        )
        assertEquals(10f, morph.dotCoordinate(10f, 10f, 100f, 100f, 0f, 0f, 0f), 0f)
    }

    @Test
    fun collapseUsesTheExactReverseOfDotReveal() {
        for (index in 1..3) {
            for (step in 0..12) {
                val progress = step / 12f
                val shrinking =
                    morph.dotScale(index, morph.dotTrim(index, progress), 0f)
                val revealing =
                    morph.dotScale(index, 1f, 1f - progress)
                assertEquals(revealing, shrinking, 0.0001f)
            }
        }
    }

    @Test
    fun bothSimRowsReuseTheSingleDotAnchorPathInEitherDirection() {
        // These distinct row geometries still follow the same four movement stages.
        val rows = listOf(
            listOf(40f, 10f, 100f, 130f),
            listOf(62f, 28f, 205f, 236f),
        )
        val phases = listOf(0f, 0.30f, 0.57f, 0.72f)
        for (row in rows) {
            val (source, sourceAnchor, targetAnchor, target) = row
            val expected = listOf(source, sourceAnchor, targetAnchor, target)
            val actual = phases.map { progress ->
                morph.dotCoordinate(
                    source, sourceAnchor, targetAnchor, target,
                    stages.trimProgress(progress),
                    stages.moveProgress(progress),
                    stages.rowProgress(progress),
                )
            }
            expected.zip(actual).forEach { (value, position) ->
                assertEquals(value, position, 0.0001f)
            }
            assertEquals(expected.reversed(), actual.reversed())
        }
        assertEquals(0f, stages.barProgress(0.72f), 0f)
        assertEquals(1f, stages.barProgress(1f), 0f)
    }

    @Test
    fun oneDotMovesThenExpandsBeforeBarsGrow() {
        assertEquals(100f, morph.dotCoordinate(70f, 10f, 100f, 130f, 1f, 1f, 0f), 0f)
        assertEquals(130f, morph.dotCoordinate(70f, 10f, 100f, 130f, 1f, 1f, 1f), 0f)
        for (i in 0..3) assertEquals(1f, morph.dotScale(i, 1f, 1f), 0f)
        assertTrue(morph.dotScale(1, 1f, 0.2f) > morph.dotScale(2, 1f, 0.2f))
        assertEquals(0f, stages.handoffProgress(Float.NaN), 0f)
    }

    @Test
    fun neighboringStagesMeetWithoutAJumpOnReverse() {
        val stageVectors = listOf(
            Triple(0f, 0f, 0f), Triple(1f, 0f, 0f),
            Triple(1f, 1f, 0f), Triple(1f, 1f, 1f),
        )
        val expected = listOf(70f, 10f, 100f, 130f)
        stageVectors.forEachIndexed { index, (trim, move, spread) ->
            assertEquals(
                expected[index],
                morph.dotCoordinate(70f, 10f, 100f, 130f, trim, move, spread),
                0.0001f,
            )
        }
        assertEquals(0f, stages.rowProgress(0.57f), 0f)
        assertEquals(1f, stages.rowProgress(0.72f), 0.0001f)
    }
}
