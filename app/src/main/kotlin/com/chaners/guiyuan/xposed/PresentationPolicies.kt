package com.chaners.guiyuan.xposed

// Stateless decisions live here; the runtime owner keeps hooks, sessions and mutable state.

internal enum class KeyguardFamilySurface(
    val surfaceName: String,
    val eventPrefix: String,
) {
    KEYGUARD("keyguard", "keyguardPresentation"),
    AOD("aod", "aodPresentation"),
}

internal object SteadyPeerMirrorPolicy {
    private const val VISIBLE_STATE_HIDDEN = 2
    private const val ISLAND_STATE_HIDDEN = 10

    fun isIslandHidden(
        visibleState: Int?,
        inIslandState: Int?,
    ): Boolean =
        visibleState == VISIBLE_STATE_HIDDEN &&
            inIslandState == ISLAND_STATE_HIDDEN

    fun exposeFakeIslandShowing(
        nativeIslandShowing: Boolean,
        steadyMirrorActive: Boolean,
    ): Boolean =
        nativeIslandShowing && !steadyMirrorActive
}

internal object VisualMaskPolicy {
    fun shouldAdoptExistingNativeLayout(
        deferVisualMaskUntilLayout: Boolean,
        laidOut: Boolean,
        layoutRequested: Boolean,
        capacityLeaseAwaitingLayout: Boolean = false,
        width: Int,
        height: Int,
    ): Boolean =
        deferVisualMaskUntilLayout &&
            laidOut &&
            !layoutRequested &&
            !capacityLeaseAwaitingLayout &&
            width > 0 &&
            height > 0
}

internal object FakeCarrierCapacityLeasePolicy {
    enum class ExistingLeaseAction {
        REUSE,
        ADOPT_HIDDEN_NATIVE,
        FAIL_WRITER_CONFLICT,
    }

    fun resolveExistingLeaseAction(
        visibleCycleActive: Boolean,
        liveWidthPx: Int,
        appliedWidthPx: Int,
        currentParentContentWidthPx: Int,
        leasedParentContentWidthPx: Int?,
    ): ExistingLeaseAction {
        if (
            leasedParentContentWidthPx == null ||
            leasedParentContentWidthPx != currentParentContentWidthPx
        ) {
            return ExistingLeaseAction.FAIL_WRITER_CONFLICT
        }
        if (liveWidthPx == appliedWidthPx) {
            return ExistingLeaseAction.REUSE
        }
        if (
            !visibleCycleActive &&
            liveWidthPx > 0 &&
            liveWidthPx <= currentParentContentWidthPx
        ) {
            return ExistingLeaseAction.ADOPT_HIDDEN_NATIVE
        }
        return ExistingLeaseAction.FAIL_WRITER_CONFLICT
    }
}

internal object EndReservationPolicy {
    fun resolveRequestedSlotWidth(
        compactSlotWidthPx: Int,
        transitionRequestedSlotWidthPx: Int?,
    ): Int {
        val compact = compactSlotWidthPx.coerceAtLeast(0)
        return transitionRequestedSlotWidthPx
            ?.coerceAtLeast(compact)
            ?: compact
    }

    fun resolvePaddingEndDelta(
        nativeHide: Boolean,
        actualBatteryWidthPx: Int,
        requestedSlotWidthPx: Int,
    ): Int {
        val requested = requestedSlotWidthPx.coerceAtLeast(0)
        val actual = actualBatteryWidthPx.coerceAtLeast(0)
        return if (nativeHide) {
            requested
        } else {
            requested - actual
        }
    }

    fun resolveCapacityBoundedReservationDelta(
        nativeHide: Boolean,
        compactSlotWidthPx: Int,
        requestedReservationDeltaPx: Int,
        capacityDeltaPx: Int,
    ): Int {
        val compact = compactSlotWidthPx.coerceAtLeast(0)
        val capacity = capacityDeltaPx.coerceAtLeast(0)
        val maxNativeReservation =
            if (nativeHide) {
                compact + capacity
            } else {
                capacity
            }
        return requestedReservationDeltaPx.coerceAtMost(maxNativeReservation)
    }

    fun resolveFakeCarrierCapacityRequirement(
        nativeHide: Boolean,
        compactSlotWidthPx: Int,
        reservationDeltaPx: Int,
    ): Int {
        val reservation = reservationDeltaPx.coerceAtLeast(0)
        return if (nativeHide) {
            (reservation - compactSlotWidthPx.coerceAtLeast(0)).coerceAtLeast(0)
        } else {
            reservation
        }
    }

    fun resolveFakeCarrierCapacityDelta(
        nativeCarrierWidthPx: Int,
        parentContentWidthPx: Int,
    ): Int? {
        if (nativeCarrierWidthPx <= 0 || parentContentWidthPx <= 0) return null
        if (nativeCarrierWidthPx > parentContentWidthPx) return null
        return parentContentWidthPx - nativeCarrierWidthPx
    }
}

internal object OwnedEntries {
    fun <T> delta(
        existing: Collection<T>,
        requested: Collection<T>,
    ): List<T> =
        requested.filterNot(existing::contains)

    fun <T> restoreTarget(
        live: List<T>,
        ownedEntries: Collection<T>,
    ): List<T> {
        val owned = ownedEntries.toHashSet()
        return live.filterNot(owned::contains)
    }

    fun <T> add(
        target: MutableList<T>,
        entries: Collection<T>,
    ): List<T> {
        val added = entries.filterNot(target::contains)
        val applied = mutableListOf<T>()
        return try {
            added.forEach { entry ->
                target.add(entry)
                applied += entry
            }
            applied
        } catch (error: Throwable) {
            applied.asReversed().forEach(target::remove)
            throw error
        }
    }

    fun <T> restore(
        target: MutableList<T>,
        ownedEntries: List<T>,
    ) {
        ownedEntries.asReversed().forEach(target::remove)
    }

    fun <T, R> withTemporary(
        target: MutableList<T>,
        entries: Collection<T>,
        block: () -> R,
    ): R {
        val owned = add(target, entries)
        return try {
            block()
        } finally {
            restore(target, owned)
        }
    }
}
