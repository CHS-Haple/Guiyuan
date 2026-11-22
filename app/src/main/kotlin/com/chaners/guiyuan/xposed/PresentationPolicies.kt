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

// CC visibility is not the same as its motion fraction. A slow close can
// reach the zero endpoint before native onVisibleChanged(false) arrives.
// Home must already be allowed at that native endpoint, and a late CC ready
// callback must not hide it again while the fraction remains zero.
internal object CcHomeHandoffPolicy {
    fun yieldToProjection(
        nativePanelVisible: Boolean,
        nativeExpansion: Float,
        projectionReady: Boolean,
    ): Boolean =
        nativePanelVisible &&
            nativeExpansion.isFinite() &&
            nativeExpansion > 0f &&
            projectionReady
}

internal object VisualMaskPolicy {
    fun shouldAdoptExistingNativeLayout(
        deferVisualMaskUntilLayout: Boolean,
        laidOut: Boolean,
        layoutRequested: Boolean,
        width: Int,
        height: Int,
    ): Boolean =
        deferVisualMaskUntilLayout &&
            laidOut &&
            !layoutRequested &&
            width > 0 &&
            height > 0
}

// For QS_FAKE, SystemUI owns the host width. Both normal and native
// end-overhanging layouts are valid once the native layout has completed.
internal object FakeCarrierNativeGeometryPolicy {
    fun isSettled(
        nativeWidthPx: Int,
        laidOutWidthPx: Int,
        attached: Boolean,
        layoutRequested: Boolean,
        exclusiveParent: Boolean,
        endAnchored: Boolean,
    ): Boolean =
        nativeWidthPx > 0 &&
            nativeWidthPx == laidOutWidthPx &&
            attached && !layoutRequested && exclusiveParent && endAnchored
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

    // Native fake host settlement and its status-icon row measure are
    // independent: a cached measureViews list is not authoritative while
    // the peer row itself still requests a layout traversal.
    fun fakeNativePeerMeasureReady(
        carrierSettled: Boolean,
        peerRowLayoutRequested: Boolean,
    ): Boolean = carrierSettled && !peerRowLayoutRequested

    // Bound the actual peer row; never infer capacity from changing host width.
    fun fitsFakeNativePeerRow(
        rowWidthPx: Int,
        contentWidthPx: Int,
        targetPaddingEndPx: Int,
    ): Boolean =
        rowWidthPx > 0 &&
            contentWidthPx >= 0 &&
            targetPaddingEndPx >= 0 &&
            contentWidthPx.toLong() + targetPaddingEndPx <= rowWidthPx.toLong()

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

    @Suppress("TooGenericExceptionCaught")
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
            try {
                restore(target, applied)
            } catch (cleanup: Throwable) {
                SysUiPresentationRuntime.throwIfFatal(cleanup)
                if (cleanup !== error) error.addSuppressed(cleanup)
            }
            throw error
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun <T> restore(
        target: MutableList<T>,
        ownedEntries: List<T>,
    ) {
        var failure: Throwable? = null
        ownedEntries.asReversed().forEach { entry ->
            try {
                target.remove(entry)
            } catch (error: Throwable) {
                SysUiPresentationRuntime.throwIfFatal(error)
                if (failure == null) {
                    failure = error
                } else if (failure !== error) {
                    failure?.addSuppressed(error)
                }
            }
        }
        failure?.let { throw it }
    }
}
