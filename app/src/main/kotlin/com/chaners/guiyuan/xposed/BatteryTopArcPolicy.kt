package com.chaners.guiyuan.xposed

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal object BatteryTopArcPolicy {
    internal data class Arc(
        val startDegrees: Float,
        val sweepDegrees: Float,
    )

    internal data class Gap(
        val centerDegrees: Float,
        val sweepDegrees: Float,
    )

    internal data class Segments(
        val active: List<Arc>,
        val inactive: List<Arc>,
    )

    fun resolveGap(
        contentLeft: Float,
        contentTop: Float,
        contentRight: Float,
        contentBottom: Float,
        ringCenterX: Float,
        ringCenterY: Float,
        ringRadius: Float,
        ringStroke: Float,
        visualClearance: Float,
        startDegrees: Float,
        maxSweep: Float,
    ): Gap {
        if (
            !contentLeft.isFinite() ||
            !contentTop.isFinite() ||
            !contentRight.isFinite() ||
            !contentBottom.isFinite() ||
            !ringCenterX.isFinite() ||
            !ringCenterY.isFinite() ||
            !ringRadius.isFinite() ||
            ringRadius <= 0f ||
            !startDegrees.isFinite() ||
            !maxSweep.isFinite() ||
            maxSweep <= 0f
        ) {
            return Gap(TOP_DEGREES, 0f)
        }

        val clearance =
            visualClearance.coerceAtLeast(0f) +
                ringStroke.coerceAtLeast(0f) / 2f
        val left = contentLeft - clearance
        val top = contentTop - clearance
        val right = contentRight + clearance
        val bottom = contentBottom + clearance
        val ringTop = ringCenterY - ringRadius
        val ringBottom = ringCenterY + ringRadius

        // If the visible content envelope cannot intersect the ring at all,
        // no ring cutout is needed.
        if (
            top > bottom ||
            bottom <= ringTop ||
            top >= ringBottom ||
            right <= ringCenterX - ringRadius ||
            left >= ringCenterX + ringRadius
        ) {
            return Gap(TOP_DEGREES, 0f)
        }

        if (right <= ringCenterX || left >= ringCenterX) {
            return resolveOffCenterGap(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                ringCenterX = ringCenterX,
                ringCenterY = ringCenterY,
                ringRadius = ringRadius,
                startDegrees = startDegrees,
                maxSweep = maxSweep,
            )
        }

        val availableLeft =
            (TOP_DEGREES - startDegrees)
                .coerceAtLeast(0f)
        val availableRight =
            (startDegrees + maxSweep - TOP_DEGREES)
                .coerceAtLeast(0f)

        val leftRatio =
            ((ringCenterX - left) / ringRadius)
                .coerceIn(0f, 1f)
        val rightRatio =
            ((right - ringCenterX) / ringRadius)
                .coerceIn(0f, 1f)
        val verticalRatio =
            ((ringCenterY - bottom) / ringRadius)
                .coerceIn(-1f, 1f)

        val verticalHalf =
            Math.toDegrees(acos(verticalRatio).toDouble()).toFloat()
        val leftHalf =
            min(
                Math.toDegrees(asin(leftRatio).toDouble()).toFloat(),
                verticalHalf,
            ).coerceAtMost(availableLeft)
        val rightHalf =
            min(
                Math.toDegrees(asin(rightRatio).toDouble()).toFloat(),
                verticalHalf,
            ).coerceAtMost(availableRight)

        if (leftHalf <= 0f && rightHalf <= 0f) {
            return Gap(TOP_DEGREES, 0f)
        }

        val gapStart = TOP_DEGREES - leftHalf
        val gapEnd = TOP_DEGREES + rightHalf
        return Gap(
            centerDegrees = (gapStart + gapEnd) / 2f,
            sweepDegrees = (gapEnd - gapStart).coerceAtLeast(0f),
        )
    }

    private fun resolveOffCenterGap(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        ringCenterX: Float,
        ringCenterY: Float,
        ringRadius: Float,
        startDegrees: Float,
        maxSweep: Float,
    ): Gap {
        val arcStart = startDegrees
        val arcEnd = startDegrees + maxSweep
        val boundaries = ArrayList<Float>(10)
        boundaries += arcStart
        boundaries += arcEnd

        fun addEquivalentAngles(rawDegrees: Float) {
            if (!rawDegrees.isFinite()) return
            for (turn in -2..2) {
                val candidate = rawDegrees + turn * FULL_TURN_DEGREES
                if (candidate >= arcStart - ANGLE_EPSILON && candidate <= arcEnd + ANGLE_EPSILON) {
                    boundaries += candidate.coerceIn(arcStart, arcEnd)
                }
            }
        }

        fun addCosBoundary(x: Float) {
            val ratio = ((x - ringCenterX) / ringRadius)
            if (ratio < -1f || ratio > 1f) return
            val base =
                Math.toDegrees(
                    acos(ratio.coerceIn(-1f, 1f)).toDouble(),
                ).toFloat()
            addEquivalentAngles(base)
            addEquivalentAngles(FULL_TURN_DEGREES - base)
        }

        fun addSinBoundary(y: Float) {
            val ratio = ((y - ringCenterY) / ringRadius)
            if (ratio < -1f || ratio > 1f) return
            val base =
                Math.toDegrees(
                    asin(ratio.coerceIn(-1f, 1f)).toDouble(),
                ).toFloat()
            addEquivalentAngles(base)
            addEquivalentAngles(HALF_TURN_DEGREES - base)
        }

        addCosBoundary(left)
        addCosBoundary(right)
        addSinBoundary(top)
        addSinBoundary(bottom)

        val ordered =
            boundaries
                .sorted()
                .fold(ArrayList<Float>()) { result, value ->
                    if (result.isEmpty() || kotlin.math.abs(result.last() - value) > ANGLE_EPSILON) {
                        result += value
                    }
                    result
                }
        if (ordered.size < 2) return Gap(TOP_DEGREES, 0f)

        val covered = ArrayList<Arc>()
        for (index in 0 until ordered.lastIndex) {
            val segmentStart = ordered[index]
            val segmentEnd = ordered[index + 1]
            if (segmentEnd - segmentStart <= ANGLE_EPSILON) continue
            val midpoint = (segmentStart + segmentEnd) / 2f
            val radians = Math.toRadians(midpoint.toDouble())
            val x = ringCenterX + ringRadius * cos(radians).toFloat()
            val y = ringCenterY + ringRadius * sin(radians).toFloat()
            if (
                x >= left - POSITION_EPSILON &&
                x <= right + POSITION_EPSILON &&
                y >= top - POSITION_EPSILON &&
                y <= bottom + POSITION_EPSILON
            ) {
                covered += Arc(segmentStart, segmentEnd - segmentStart)
            }
        }
        if (covered.isEmpty()) return Gap(TOP_DEGREES, 0f)

        val gapStart = covered.first().startDegrees
        val gapEnd =
            covered.last().let { arc ->
                arc.startDegrees + arc.sweepDegrees
            }
        return Gap(
            centerDegrees = (gapStart + gapEnd) / 2f,
            sweepDegrees = (gapEnd - gapStart).coerceAtLeast(0f),
        )
    }

    fun mergeGaps(
        gaps: List<Gap>,
    ): Gap {
        val visible =
            gaps.filter { gap ->
                gap.centerDegrees.isFinite() &&
                    gap.sweepDegrees.isFinite() &&
                    gap.sweepDegrees > 0f
            }
        if (visible.isEmpty()) return Gap(TOP_DEGREES, 0f)

        var start = Float.POSITIVE_INFINITY
        var end = Float.NEGATIVE_INFINITY
        visible.forEach { gap ->
            val half = gap.sweepDegrees / 2f
            start = kotlin.math.min(start, gap.centerDegrees - half)
            end = kotlin.math.max(end, gap.centerDegrees + half)
        }
        if (!start.isFinite() || !end.isFinite() || end <= start) {
            return Gap(TOP_DEGREES, 0f)
        }
        return Gap(
            centerDegrees = (start + end) / 2f,
            sweepDegrees = end - start,
        )
    }

    fun drawableArcs(
        startDegrees: Float,
        maxSweep: Float,
        gapCenterDegrees: Float,
        gapSweepDegrees: Float,
    ): List<Arc> {
        if (maxSweep <= 0f) return emptyList()

        val endDegrees = startDegrees + maxSweep
        val halfGap = gapSweepDegrees.coerceAtLeast(0f) / 2f
        val gapStart = (gapCenterDegrees - halfGap).coerceIn(startDegrees, endDegrees)
        val gapEnd = (gapCenterDegrees + halfGap).coerceIn(startDegrees, endDegrees)

        val drawable = ArrayList<Arc>(2)
        if (gapStart > startDegrees) drawable += Arc(startDegrees, gapStart - startDegrees)
        if (gapEnd < endDegrees) drawable += Arc(gapEnd, endDegrees - gapEnd)
        return drawable
    }

    fun resolve(
        batteryPercent: Int,
        startDegrees: Float,
        maxSweep: Float,
        gapCenterDegrees: Float,
        gapSweepDegrees: Float,
    ): Segments {
        val drawable =
            drawableArcs(
                startDegrees = startDegrees,
                maxSweep = maxSweep,
                gapCenterDegrees = gapCenterDegrees,
                gapSweepDegrees = gapSweepDegrees,
            )
        if (drawable.isEmpty()) return Segments(emptyList(), emptyList())

        val totalVisibleSweep = drawable.sumOf { it.sweepDegrees.toDouble() }.toFloat()
        var activeRemaining = totalVisibleSweep * batteryPercent.coerceIn(0, 100) / 100f
        val active = ArrayList<Arc>(2)
        val inactive = ArrayList<Arc>(2)

        drawable.forEach { arc ->
            val activeSweep = min(arc.sweepDegrees, activeRemaining.coerceAtLeast(0f))
            if (activeSweep > 0f) active += Arc(arc.startDegrees, activeSweep)
            val inactiveSweep = arc.sweepDegrees - activeSweep
            if (inactiveSweep > 0f) {
                inactive += Arc(arc.startDegrees + activeSweep, inactiveSweep)
            }
            activeRemaining -= activeSweep
        }

        return Segments(active = active, inactive = inactive)
    }

    private const val TOP_DEGREES = 270f
    private const val HALF_TURN_DEGREES = 180f
    private const val FULL_TURN_DEGREES = 360f
    private const val ANGLE_EPSILON = 0.0001f
    private const val POSITION_EPSILON = 0.0001f
}
