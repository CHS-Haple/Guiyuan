package com.chaners.guiyuan.xposed.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryTopArcPolicyTest {
    @Test
    fun halfBatteryFillsFirstVisibleShoulderBeforeGap() {
        val result =
            BatteryTopArcPolicy.resolve(
                batteryPercent = 50,
                startDegrees = 150f,
                maxSweep = 240f,
                gapCenterDegrees = 270f,
                gapSweepDegrees = 60f,
            )

        assertEquals(
            listOf(BatteryTopArcPolicy.Arc(150f, 90f)),
            result.active,
        )
        assertEquals(
            listOf(BatteryTopArcPolicy.Arc(300f, 90f)),
            result.inactive,
        )
    }

    @Test
    fun widerVisibleReadoutRequestsOnlyTheExtraWidthItNeeds() {
        val narrow =
            gapFor(
                left = 51f,
                top = 4f,
                right = 69f,
                bottom = 20f,
            )
        val wide =
            gapFor(
                left = 36f,
                top = 4f,
                right = 84f,
                bottom = 20f,
            )

        assertTrue(wide.sweepDegrees > narrow.sweepDegrees)
        assertEquals(270f, narrow.centerDegrees, 0.0001f)
        assertEquals(270f, wide.centerDegrees, 0.0001f)
    }

    @Test
    fun movingReadoutUpShrinksGapAndCanRemoveItEntirely() {
        val low =
            gapFor(
                left = 20f,
                top = 12f,
                right = 100f,
                bottom = 30f,
            )
        val high =
            gapFor(
                left = 20f,
                top = -8f,
                right = 100f,
                bottom = 10f,
            )
        val clear =
            gapFor(
                left = 20f,
                top = -20f,
                right = 100f,
                bottom = 2f,
            )

        assertTrue(low.sweepDegrees > high.sweepDegrees)
        assertEquals(0f, clear.sweepDegrees, 0.0001f)
    }

    @Test
    fun extraVisibleHeightOrLowerPositionWidensGapUntilWidthBecomesLimit() {
        val compact =
            gapFor(
                left = 25f,
                top = 0f,
                right = 95f,
                bottom = 14f,
            )
        val taller =
            gapFor(
                left = 25f,
                top = 0f,
                right = 95f,
                bottom = 26f,
            )

        assertTrue(taller.sweepDegrees > compact.sweepDegrees)
    }

    @Test
    fun componentAwareWifiGapDoesNotReserveEmptyEnvelopeCorners() {
        val envelope =
            gapFor(
                left = 30f,
                top = -6f,
                right = 90f,
                bottom = 20f,
            )
        val componentAware =
            BatteryTopArcPolicy.mergeGaps(
                listOf(
                    gapFor(
                        left = 30f,
                        top = -6f,
                        right = 90f,
                        bottom = 4f,
                    ),
                    gapFor(
                        left = 40f,
                        top = 4f,
                        right = 80f,
                        bottom = 12f,
                    ),
                    gapFor(
                        left = 50f,
                        top = 10f,
                        right = 70f,
                        bottom = 20f,
                    ),
                ),
            )

        assertTrue(componentAware.sweepDegrees > 0f)
        assertTrue(componentAware.sweepDegrees < envelope.sweepDegrees)
    }

    @Test
    fun rightSideBadgeRequestsItsOwnRightShoulderGap() {
        val badge =
            gapFor(
                left = 70f,
                top = 2f,
                right = 92f,
                bottom = 24f,
            )

        assertTrue(badge.sweepDegrees > 0f)
        assertTrue(badge.centerDegrees > 270f)
    }

    @Test
    fun leftSideBadgeRequestsItsOwnLeftShoulderGap() {
        val badge =
            gapFor(
                left = 28f,
                top = 2f,
                right = 50f,
                bottom = 24f,
            )

        assertTrue(badge.sweepDegrees > 0f)
        assertTrue(badge.centerDegrees < 270f)
    }

    @Test
    fun wifiBadgeExtendsOnlyTheShoulderItActuallyOccupies() {
        val center =
            gapFor(
                left = 42f,
                top = 0f,
                right = 78f,
                bottom = 16f,
            )
        val rightBadge =
            gapFor(
                left = 72f,
                top = 2f,
                right = 92f,
                bottom = 24f,
            )
        val merged =
            BatteryTopArcPolicy.mergeGaps(
                listOf(center, rightBadge),
            )

        val centerStart = center.centerDegrees - center.sweepDegrees / 2f
        val centerEnd = center.centerDegrees + center.sweepDegrees / 2f
        val mergedStart = merged.centerDegrees - merged.sweepDegrees / 2f
        val mergedEnd = merged.centerDegrees + merged.sweepDegrees / 2f

        assertEquals(centerStart, mergedStart, 0.0001f)
        assertTrue(mergedEnd > centerEnd)
    }

    @Test
    fun singleComponentGapMergePreservesExistingGeometry() {
        val original =
            gapFor(
                left = 42f,
                top = 2f,
                right = 92f,
                bottom = 22f,
            )
        val merged =
            BatteryTopArcPolicy.mergeGaps(
                listOf(original),
            )

        assertEquals(original.centerDegrees, merged.centerDegrees, 0.0001f)
        assertEquals(original.sweepDegrees, merged.sweepDegrees, 0.0001f)
    }

    @Test
    fun asymmetricVisibleEnvelopeMovesGapCenterInsteadOfAddingDeadPadding() {
        val result =
            gapFor(
                left = 42f,
                top = 2f,
                right = 92f,
                bottom = 22f,
            )

        assertTrue(result.centerDegrees > 270f)
        assertTrue(result.sweepDegrees > 0f)
    }

    @Test
    fun roundedClearanceKeepsTheSingleNotchButDoesNotReserveSquareCorners() {
        val gap =
            gapFor(
                left = 43.4f,
                top = 15.7f,
                right = 79.1f,
                bottom = 26.2f,
            )

        // The rectangular expansion cuts a much larger left shoulder here.
        assertTrue(gap.centerDegrees > 285f)
        assertTrue(gap.sweepDegrees in 2f..12f)
    }

    @Test
    fun squareOnlyCornerIntersectionDoesNotCutTheRing() {
        val gap =
            gapFor(
                left = 8f,
                top = 30.16f,
                right = 11.05f,
                bottom = 33.52f,
            )

        assertEquals(0f, gap.sweepDegrees, 0.0001f)
    }

    @Test
    fun roundedGapEndpointsRetainTheRequestedOpticalClearance() {
        val left = 43.4f
        val top = 15.7f
        val right = 79.1f
        val bottom = 26.2f
        val gap = gapFor(left, top, right, bottom)
        val angles =
            listOf(
                gap.centerDegrees - gap.sweepDegrees / 2f,
                gap.centerDegrees + gap.sweepDegrees / 2f,
            )

        angles.forEach { degrees ->
            val radians = Math.toRadians(degrees.toDouble())
            val x = 60f + 50f * kotlin.math.cos(radians).toFloat()
            val y = 58f + 50f * kotlin.math.sin(radians).toFloat()
            val dx = maxOf(left - x, 0f, x - right)
            val dy = maxOf(top - y, 0f, y - bottom)
            // 2px optical gap plus half of the 4px ring stroke.
            assertEquals(4f, kotlin.math.sqrt(dx * dx + dy * dy), 0.02f)
        }
    }

    private fun gapFor(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ): BatteryTopArcPolicy.Gap =
        BatteryTopArcPolicy.resolveGap(
            contentLeft = left,
            contentTop = top,
            contentRight = right,
            contentBottom = bottom,
            ringCenterX = 60f,
            ringCenterY = 58f,
            ringRadius = 50f,
            ringStroke = 4f,
            visualClearance = 2f,
            startDegrees = 150f,
            maxSweep = 240f,
        )
}
