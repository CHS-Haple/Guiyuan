package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.SignalDotArrangement
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OuterGeometryArrangementTest {
    @Test
    fun horizontalArrangementKeepsDotSizesAndSpacingWithFlatRows() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)
            for (row in 0..1) {
                val baseline = geometry.dualDotY(0, row, SignalDotArrangement.HORIZONTAL)
                for (index in 1..3) {
                    assertEquals(baseline, geometry.dualDotY(index, row, SignalDotArrangement.HORIZONTAL), 0.0001f)
                    assertEquals(
                        geometry.horizontalDotSpan / 3f,
                        geometry.horizontalDotX(index) - geometry.horizontalDotX(index - 1),
                        0.0001f,
                    )
                }
                assertTrue(58f + baseline + geometry.dualMobileDotRadius <= 117.9001f)
            }
            assertEquals(
                geometry.dualRowGap,
                geometry.dualDotY(0, 1, SignalDotArrangement.HORIZONTAL) -
                    geometry.dualDotY(0, 0, SignalDotArrangement.HORIZONTAL),
                0.0002f,
            )
        }
    }

    @Test
    fun bothDualStylesApplyTheSameSignedVerticalOffset() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)
            for (style in SignalDotArrangement.entries) {
                assertDualVerticalOffset(geometry, style)
            }
        }
    }

    private fun assertDualVerticalOffset(
        geometry: OuterGeometry.Resolved,
        style: SignalDotArrangement,
    ) {
        for (row in 0..1) {
            for (index in 0..3) {
                val original = geometry.dualDotY(index, row, style)
                assertEquals(original - 8f, geometry.dualDotY(index, row, style, 8f), 0.0001f)
                assertEquals(original + 10f, geometry.dualDotY(index, row, style, -10f), 0.0001f)
            }
        }
    }

    @Test
    fun dualRowSourceCoordinatesRemainSharedAcrossWeights() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)
            val positions = (0 until 4).map(geometry::horizontalDotX)
            assertEquals(-positions[3], positions[0], 0.0001f)
            assertEquals(-positions[2], positions[1], 0.0001f)
            assertEquals(-geometry.horizontalDotSpan / 2f, positions.first(), 0.0001f)
            assertEquals(geometry.horizontalDotSpan / 2f, positions.last(), 0.0001f)

            for (index in 0 until 4) {
                val x = positions[index]
                val baseline =
                    sqrt(OuterGeometry.RING_RADIUS * OuterGeometry.RING_RADIUS - x * x) -
                        geometry.dualOpticalRise
                assertEquals(
                    baseline - geometry.dualRowGap / 2f,
                    geometry.dualDotY(index, 0),
                    0.0001f,
                )
                assertEquals(
                    baseline + geometry.dualRowGap / 2f,
                    geometry.dualDotY(index, 1),
                    0.0001f,
                )
            }
            assertTrue(
                geometry.dualDotY(0, 0) < geometry.dualDotY(1, 0) &&
                    geometry.dualDotY(1, 1) > geometry.dualDotY(0, 1),
            )
        }
    }

    @Test
    fun outerWeightScalesRingDotsAndUnavailableMarkAsOneVisualFamily() {
        val base =
            OuterGeometry.resolve(
                OuterGeometry.DEFAULT_WEIGHT_SCALE,
            )
        val heavier = OuterGeometry.resolve(1.20f)

        assertEquals(base.ringStroke * 1.20f, heavier.ringStroke, 0.0001f)
        assertEquals(base.mobileDotRadius * 1.20f, heavier.mobileDotRadius, 0.0001f)
        assertEquals(
            base.unavailableMarkStroke * 1.20f,
            heavier.unavailableMarkStroke,
            0.0001f,
        )
        assertEquals(
            base.unavailableMarkHalfExtent * 1.20f,
            heavier.unavailableMarkHalfExtent,
            0.0001f,
        )
    }

    @Test
    fun ringAndDotsScaleProportionally() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)

            assertEquals(
                OuterGeometry.BASE_RING_STROKE * scale,
                geometry.ringStroke,
                0.0001f,
            )
            assertEquals(
                OuterGeometry.BASE_MOBILE_DOT_RADIUS * scale,
                geometry.mobileDotRadius,
                0.0001f,
            )
        }
    }

    @Test
    fun unavailableMarkScalesWithOuterWeightWithoutChangingItsIndependentProportions() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)

            assertEquals(
                OuterGeometry.BASE_UNAVAILABLE_MARK_STROKE * scale,
                geometry.unavailableMarkStroke,
                0.0001f,
            )
            assertEquals(
                OuterGeometry.BASE_UNAVAILABLE_MARK_HALF_EXTENT * scale,
                geometry.unavailableMarkHalfExtent,
                0.0001f,
            )
        }
    }

    @Test
    fun invalidOrOutOfRangeWeightScaleFallsBackOrClampsSafely() {
        assertEquals(
            OuterGeometry.DEFAULT_WEIGHT_SCALE,
            OuterGeometry.normalizeWeightScale(Float.NaN),
            0f,
        )
        assertEquals(
            OuterGeometry.MIN_WEIGHT_SCALE,
            OuterGeometry.normalizeWeightScale(0f),
            0f,
        )
        assertEquals(
            OuterGeometry.MAX_WEIGHT_SCALE,
            OuterGeometry.normalizeWeightScale(5f),
            0f,
        )
    }

    private companion object {
        val TEST_SCALES =
            listOf(
                0.60f,
                1.00f,
                OuterGeometry.DEFAULT_WEIGHT_SCALE,
                1.50f,
                2.00f,
            )
    }
}
