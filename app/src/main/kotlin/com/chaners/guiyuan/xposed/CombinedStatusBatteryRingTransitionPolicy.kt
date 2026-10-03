package com.chaners.guiyuan.xposed

import kotlin.math.max
import kotlin.math.min

internal object CombinedStatusBatteryRingTransitionPolicy {
    private const val TRANSITION_COMPLETE_PROGRESS = 0.45f
    private const val FRONT_LOAD = 0.92f

    internal enum class ExitDirection { NONE, LEFT, RIGHT }

    fun transitionProgress(progress: Float): Float {
        val p =
            progress
                .takeIf(Float::isFinite)
                ?.coerceIn(0f, 1f)
                ?: 0f
        return (p / TRANSITION_COMPLETE_PROGRESS).coerceIn(0f, 1f)
    }

    internal data class Segments(
        val background: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        val active: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        val remainingFraction: Float,
    )

    fun remainingFraction(progress: Float): Float {
        val p = progress.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f
        val warped =
            (p + FRONT_LOAD * p * (1f - p))
                .coerceIn(0f, 1f)
        val eased = warped * warped * (3f - 2f * warped)
        return 1f - eased
    }

    fun isTerminalCapDominated(
        remainingFraction: Float,
        totalSweepDegrees: Float,
        radiusPx: Float,
        strokeWidthPx: Float,
    ): Boolean {
        val remaining =
            remainingFraction
                .takeIf(Float::isFinite)
                ?.coerceIn(0f, 1f)
                ?: return false
        val sweep =
            totalSweepDegrees
                .takeIf(Float::isFinite)
                ?.coerceAtLeast(0f)
                ?: return false
        val radius =
            radiusPx
                .takeIf(Float::isFinite)
                ?.takeIf { it > 0f }
                ?: return false
        val stroke =
            strokeWidthPx
                .takeIf(Float::isFinite)
                ?.takeIf { it > 0f }
                ?: return false
        if (remaining <= 0f || sweep <= 0f) return true
        val remainingArcLengthPx =
            Math.toRadians((sweep * remaining).toDouble()).toFloat() * radius
        return remainingArcLengthPx <= stroke
    }

    fun resolve(
        drawableArcs: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        batteryPercent: Int,
        progress: Float,
        exitDirection: ExitDirection = ExitDirection.NONE,
        followRetractEndpoint: Boolean = false,
    ): Segments {
        val remaining = remainingFraction(progress)
        val totalSweep = drawableArcs.sumOf { it.sweepDegrees.coerceAtLeast(0f).toDouble() }.toFloat()
        if (totalSweep <= 0f || remaining <= 0f) {
            return Segments(emptyList(), emptyList(), remaining)
        }

        val retainedSweep = totalSweep * remaining
        val activeSweep =
            totalSweep *
                batteryPercent.coerceIn(0, 100) /
                100f *
                remaining
        return when (exitDirection) {
            ExitDirection.LEFT -> {
                val retainedStart = totalSweep - retainedSweep
                val retainedEnd = totalSweep
                val originalActiveSweep =
                    totalSweep *
                        batteryPercent.coerceIn(0, 100) /
                        100f
                val activeStart: Float
                val activeEnd: Float
                if (followRetractEndpoint) {
                    activeStart = retainedStart
                    activeEnd =
                        retainedStart +
                            min(originalActiveSweep, retainedSweep)
                } else {
                    activeStart = max(retainedStart, 0f)
                    activeEnd = min(retainedEnd, originalActiveSweep)
                }
                Segments(
                    background = slice(drawableArcs, retainedStart, retainedEnd),
                    active =
                        slice(
                            drawableArcs,
                            activeStart,
                            activeEnd,
                        ),
                    remainingFraction = remaining,
                )
            }

            ExitDirection.NONE,
            ExitDirection.RIGHT,
            -> Segments(
                background = slice(drawableArcs, 0f, retainedSweep),
                active = slice(drawableArcs, 0f, activeSweep),
                remainingFraction = remaining,
            )
        }
    }

    private fun slice(
        arcs: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        rangeStart: Float,
        rangeEnd: Float,
    ): List<CombinedStatusBatteryTopArcPolicy.Arc> {
        if (rangeEnd <= rangeStart) return emptyList()
        var cursor = 0f
        val result = ArrayList<CombinedStatusBatteryTopArcPolicy.Arc>(arcs.size)
        arcs.forEach { arc ->
            val sweep = arc.sweepDegrees.coerceAtLeast(0f)
            val arcStart = cursor
            val arcEnd = cursor + sweep
            val visibleStart = max(arcStart, rangeStart)
            val visibleEnd = min(arcEnd, rangeEnd)
            if (visibleEnd > visibleStart) {
                result += CombinedStatusBatteryTopArcPolicy.Arc(
                    startDegrees = arc.startDegrees + visibleStart - arcStart,
                    sweepDegrees = visibleEnd - visibleStart,
                )
            }
            cursor = arcEnd
        }
        return result
    }
}
