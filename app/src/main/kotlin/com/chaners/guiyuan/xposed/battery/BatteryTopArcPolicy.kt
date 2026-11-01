package com.chaners.guiyuan.xposed.battery

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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
        if (
            !clearance.isFinite() ||
            contentLeft >= contentRight ||
            contentTop >= contentBottom
        ) {
            return Gap(TOP_DEGREES, 0f)
        }

        // The expanded rectangle is only a conservative early-out. Its square
        // corners are not part of the requested optical clearance.
        if (
            contentBottom + clearance <= ringCenterY - ringRadius ||
            contentTop - clearance >= ringCenterY + ringRadius ||
            contentRight + clearance <= ringCenterX - ringRadius ||
            contentLeft - clearance >= ringCenterX + ringRadius
        ) {
            return Gap(TOP_DEGREES, 0f)
        }

        return resolveRoundedRectangleGap(
            left = contentLeft,
            top = contentTop,
            right = contentRight,
            bottom = contentBottom,
            clearance = clearance,
            ringCenterX = ringCenterX,
            ringCenterY = ringCenterY,
            ringRadius = ringRadius,
            startDegrees = startDegrees,
            maxSweep = maxSweep,
        )
    }

    private fun resolveRoundedRectangleGap(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        clearance: Float,
        ringCenterX: Float,
        ringCenterY: Float,
        ringRadius: Float,
        startDegrees: Float,
        maxSweep: Float,
    ): Gap {
        val arcEnd = startDegrees + maxSweep
        val radiusSquared = ringRadius * ringRadius
        val boundaries = ArrayList<Float>(32)
        boundaries += startDegrees
        boundaries += arcEnd

        fun addAngle(x: Float, y: Float) {
            val angle =
                Math.toDegrees(
                    atan2((y - ringCenterY).toDouble(), (x - ringCenterX).toDouble()),
                ).toFloat()
            for (turn in -2..2) {
                val candidate = angle + turn * FULL_TURN_DEGREES
                if (candidate >= startDegrees - ANGLE_EPSILON &&
                    candidate <= arcEnd + ANGLE_EPSILON
                ) {
                    boundaries += candidate.coerceIn(startDegrees, arcEnd)
                }
            }
        }

        fun addVerticalEdge(x: Float) {
            val dx = x - ringCenterX
            val remaining = radiusSquared - dx * dx
            if (remaining < 0f) return
            val dy = sqrt(remaining)
            val y1 = ringCenterY - dy
            val y2 = ringCenterY + dy
            if (y1 >= top - POSITION_EPSILON && y1 <= bottom + POSITION_EPSILON) {
                addAngle(x, y1)
            }
            if (y2 >= top - POSITION_EPSILON && y2 <= bottom + POSITION_EPSILON) {
                addAngle(x, y2)
            }
        }

        fun addHorizontalEdge(y: Float) {
            val dy = y - ringCenterY
            val remaining = radiusSquared - dy * dy
            if (remaining < 0f) return
            val dx = sqrt(remaining)
            val x1 = ringCenterX - dx
            val x2 = ringCenterX + dx
            if (x1 >= left - POSITION_EPSILON && x1 <= right + POSITION_EPSILON) {
                addAngle(x1, y)
            }
            if (x2 >= left - POSITION_EPSILON && x2 <= right + POSITION_EPSILON) {
                addAngle(x2, y)
            }
        }

        fun addCorner(x: Float, y: Float) {
            if (clearance <= 0f) return
            val dx = x - ringCenterX
            val dy = y - ringCenterY
            val distance = sqrt(dx * dx + dy * dy)
            if (distance <= POSITION_EPSILON ||
                distance > ringRadius + clearance ||
                distance + clearance < ringRadius ||
                distance + ringRadius < clearance
            ) return

            // Circle-circle intersections are the round-corner boundaries.
            val projection =
                (radiusSquared - clearance * clearance + distance * distance) /
                    (2f * distance)
            val heightSquared = radiusSquared - projection * projection
            if (heightSquared < -POSITION_EPSILON) return
            val height = sqrt(heightSquared.coerceAtLeast(0f))
            val unitX = dx / distance
            val unitY = dy / distance
            val baseX = ringCenterX + projection * unitX
            val baseY = ringCenterY + projection * unitY
            addAngle(baseX - height * unitY, baseY + height * unitX)
            addAngle(baseX + height * unitY, baseY - height * unitX)
        }

        addVerticalEdge(left - clearance)
        addVerticalEdge(right + clearance)
        addHorizontalEdge(top - clearance)
        addHorizontalEdge(bottom + clearance)
        addCorner(left, top)
        addCorner(right, top)
        addCorner(left, bottom)
        addCorner(right, bottom)

        val ordered =
            boundaries
                .sorted()
                .fold(ArrayList<Float>(boundaries.size)) { result, angle ->
                    if (result.isEmpty() || kotlin.math.abs(result.last() - angle) > ANGLE_EPSILON) {
                        result += angle
                    }
                    result
                }

        var gapStart = Float.POSITIVE_INFINITY
        var gapEnd = Float.NEGATIVE_INFINITY
        for (index in 0 until ordered.lastIndex) {
            val segmentStart = ordered[index]
            val segmentEnd = ordered[index + 1]
            if (segmentEnd - segmentStart <= ANGLE_EPSILON) continue
            val radians = Math.toRadians(((segmentStart + segmentEnd) / 2f).toDouble())
            val x = ringCenterX + ringRadius * cos(radians).toFloat()
            val y = ringCenterY + ringRadius * sin(radians).toFloat()
            val dx = kotlin.math.max(left - x, 0f, x - right)
            val dy = kotlin.math.max(top - y, 0f, y - bottom)
            if (dx * dx + dy * dy <= clearance * clearance + POSITION_EPSILON) {
                gapStart = kotlin.math.min(gapStart, segmentStart)
                gapEnd = kotlin.math.max(gapEnd, segmentEnd)
            }
        }

        if (!gapStart.isFinite() || !gapEnd.isFinite() || gapEnd <= gapStart) {
            return Gap(TOP_DEGREES, 0f)
        }
        // Preserve one intentional top notch even when separate glyph parts
        // intersect both shoulders, rather than drawing a third arc above Wi-Fi.
        return Gap(
            centerDegrees = (gapStart + gapEnd) / 2f,
            sweepDegrees = gapEnd - gapStart,
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
