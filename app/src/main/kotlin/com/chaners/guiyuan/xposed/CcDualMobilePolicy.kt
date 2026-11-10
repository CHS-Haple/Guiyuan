package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator

internal object CcDualMobilePolicy {
    internal enum class DualMobileTarget {
        NONE,
        AGGREGATED,
        SEPARATE,
    }

    // The target group needs no row identity; separate native slots do.
    fun dualMobileTarget(
        activeSubIds: Set<Int>,
        targetSubIds: List<Int?>,
        aggregateProven: Boolean,
    ): DualMobileTarget {
        if (activeSubIds.size != 2 || activeSubIds.any { it < 0 }) {
            return DualMobileTarget.NONE
        }
        return when (targetSubIds.size) {
            1 -> {
                val subId = targetSubIds.single()
                if (aggregateProven && (subId == null || subId in activeSubIds)) {
                    DualMobileTarget.AGGREGATED
                } else {
                    DualMobileTarget.NONE
                }
            }
            2 -> if (targetSubIds.all { it != null && it in activeSubIds } &&
                targetSubIds.toSet() == activeSubIds
            ) DualMobileTarget.SEPARATE else DualMobileTarget.NONE
            else -> DualMobileTarget.NONE
        }
    }

    // Both native views have already been sampled into the same root coordinates.
    fun dualMobileGroupGeometry(parts: List<FloatArray>): FloatArray? {
        if (parts.size !in 1..2 || parts.any { part ->
                part.size != 6 || part.any { !it.isFinite() } ||
                    kotlin.math.hypot(part[2], part[3]) <= 0f ||
                    kotlin.math.hypot(part[4], part[5]) <= 0f
            }
        ) return null
        if (parts.size == 1) return parts.single().copyOf()

        // Keep the native root axes: merging unrelated rotations would invent geometry.
        val first = parts[0]
        if (parts.any { part ->
                kotlin.math.abs(part[2]) <= 0f ||
                    kotlin.math.abs(part[5]) <= 0f ||
                    kotlin.math.abs(part[3]) > kotlin.math.abs(part[2]) * 0.05f ||
                    kotlin.math.abs(part[4]) > kotlin.math.abs(part[5]) * 0.05f ||
                    (part[2] < 0f) != (first[2] < 0f) ||
                    (part[5] < 0f) != (first[5] < 0f)
            }
        ) return null

        val left = parts.minOf { it[0] - kotlin.math.abs(it[2]) / 2f }
        val top = parts.minOf { it[1] - kotlin.math.abs(it[5]) / 2f }
        val right = parts.maxOf { it[0] + kotlin.math.abs(it[2]) / 2f }
        val bottom = parts.maxOf { it[1] + kotlin.math.abs(it[5]) / 2f }
        if (right <= left || bottom <= top) return null
        return floatArrayOf(
            (left + right) / 2f, (top + bottom) / 2f,
            (right - left) * if (first[2] < 0f) -1f else 1f,
            0f, 0f,
            (bottom - top) * if (first[5] < 0f) -1f else 1f,
        )
    }

    // One aggregate endpoint has no SIM identity. Its paired anchors share X;
    // native ink determines each Y, so bar growth starts at the bar's bottom.
    fun aggregateAnchors(
        bounds: StatusPainter.TransitionBounds,
        top: ParticipantVisualSnapshot.NormalizedRect,
        bottom: ParticipantVisualSnapshot.NormalizedRect,
        topShape: ParticipantVisualSnapshot.RowShape,
        bottomShape: ParticipantVisualSnapshot.RowShape,
        widthCompensation: Float,
        heightCompensation: Float,
        dotDiameter: Float,
    ): FloatArray {
        fun y(
            rect: ParticipantVisualSnapshot.NormalizedRect,
            shape: ParticipantVisualSnapshot.RowShape,
        ): Float {
            val normalized = if (shape == ParticipantVisualSnapshot.RowShape.DOTS) {
                rect.centerY
            } else {
                rect.bottom
            }
            val position = bounds.centerY +
                (normalized - 0.5f) * bounds.height * heightCompensation
            return if (shape == ParticipantVisualSnapshot.RowShape.BARS) {
                position - dotDiameter / 2f
            } else position
        }
        return floatArrayOf(
            bounds.centerX +
                ((top.centerX + bottom.centerX) / 2f - 0.5f) *
                    bounds.width * widthCompensation,
            y(top, topShape),
            y(bottom, bottomShape),
        )
    }

    fun matchesDualMobileTypeTarget(
        dataSubId: Int,
        targetSubId: Int?,
        source: CenterIndicator.MobileType,
        target: NativePresentationResolver.NetworkType?,
        verifiedAggregateRoot: Boolean = false,
    ): Boolean =
        dataSubId >= 0 &&
            (targetSubId == dataSubId || verifiedAggregateRoot) &&
            mobileTypeMatches(source.label, source.enhanced, target) == true

    // The visible aggregate root has presentation ownership, not data-SIM ownership.
    fun verifiedAggregateTypeRoot(
        presentation: NativePresentationResolver.Snapshot?,
        dataSubId: Int,
        targetSubId: Int?,
        soleNativeRoot: Boolean,
    ): Boolean {
        val state = presentation ?: return false
        if (state.mode != NativePresentationResolver.Mode.DUAL_AGGREGATED ||
            !state.activeSubsAuthoritative || state.visibleRoots != 1 ||
            !soleNativeRoot
        ) return false

        val active = state.activeSubscriptionIds.toSet()
        return active.size == 2 &&
            active == state.platformActiveSubscriptionIds &&
            dataSubId in active &&
            targetSubId != null && targetSubId in active &&
            state.effectiveDataSubscriptionId == dataSubId &&
            state.networkTypeSubscriptionId == dataSubId &&
            state.presentationRootSubscriptionId == targetSubId
    }

    // A sole verified aggregate root may belong to the other SIM, even when
    // its native activity image represents the current data subscription.
    fun mobileActivityRootMatches(
        presentation: NativePresentationResolver.Snapshot?,
        dataSubId: Int,
        targetSubId: Int?,
        soleVisibleRoot: Boolean,
    ): Boolean {
        if (dataSubId < 0) return false
        if (targetSubId == dataSubId) return true
        val state = presentation ?: return false
        if (!soleVisibleRoot) return false
        if (targetSubId == null) {
            return dataSubId in state.activeSubscriptionIds &&
                (state.activeSubscriptionIds.size == 1 ||
                    state.mode == NativePresentationResolver.Mode.DUAL_AGGREGATED)
        }
        return verifiedAggregateTypeRoot(state, dataSubId, targetSubId, soleVisibleRoot)
    }

    // Order optical roots by the platform's physical SIM slots, never by View order.
    fun dualRootRowIndices(
        mode: NativePresentationResolver.Mode,
        slots: Map<Int, Int>,
        rootSubIds: List<Int?>,
    ): List<Int>? {
        if (mode != NativePresentationResolver.Mode.DUAL_SEPARATE ||
            slots.size != 2 || rootSubIds.size != 2
        ) return null
        val first = slots[0] ?: return null
        val second = slots[1] ?: return null
        if (first == second || rootSubIds.toSet() != setOf(first, second)) return null
        return listOf(rootSubIds.indexOf(first), rootSubIds.indexOf(second))
    }

    fun mobileTypeMatches(
        sourceLabel: String,
        sourceEnhanced: Boolean,
        target: NativePresentationResolver.NetworkType?,
    ): Boolean? {
        target ?: return null
        fun shown(label: String, enhanced: Boolean): String {
            val value = label.trim().uppercase().replace("-", "").replace("_", "")
            return if (value == "5G" && enhanced) "5G++" else value
        }
        return shown(sourceLabel, sourceEnhanced) ==
            shown(target.label, target.enhanced)
    }
}
