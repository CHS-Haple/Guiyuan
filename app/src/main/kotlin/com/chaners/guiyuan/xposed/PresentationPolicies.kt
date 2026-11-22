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

// Native Control Center owns the screen while visible with positive
// expansion, even if the QS_FAKE projection has locally failed native.
// Only native fraction zero returns Home ownership.
internal object CcHomeHandoffPolicy {
    // The native hidden callback may precede Home's first drawn replacement.
    fun retainFakeUntilHomeDraw(
        panelHidden: Boolean,
        homeClaimed: Boolean,
        homeFrameReady: Boolean,
    ): Boolean = panelHidden && homeClaimed && !homeFrameReady

    // Repeated native zero callbacks after Home is already restored must not
    // hide and reveal the overlay in the same callback.
    fun awaitNativeZeroCommit(
        nativePanelVisible: Boolean,
        previousFraction: Float,
        currentFraction: Float,
        ccOwnedBeforeCallback: Boolean,
    ): Boolean =
        nativePanelVisible &&
            currentFraction == 0f &&
            (previousFraction > 0f || ccOwnedBeforeCallback)

    fun yieldToNativePanel(
        nativePanelVisible: Boolean,
        nativeExpansion: Float,
        nativeZeroAwaitingCommit: Boolean = false,
    ): Boolean =
        nativePanelVisible &&
            (nativeZeroAwaitingCommit ||
                (nativeExpansion.isFinite() && nativeExpansion > 0f))
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

    // Limit optional transition padding to the available measured native row.
    // A non-fitting compact baseline still requires native fallback.
    fun boundedFakeNativeTransitionPadding(
        rowWidthPx: Int,
        contentWidthPx: Int,
        requestedPaddingEndPx: Int,
        minimumPaddingEndPx: Int,
    ): Int? {
        if (rowWidthPx <= 0 || contentWidthPx < 0 ||
            minimumPaddingEndPx < 0 || requestedPaddingEndPx < minimumPaddingEndPx
        ) return null
        val available = rowWidthPx.toLong() - contentWidthPx.toLong()
        if (available < minimumPaddingEndPx) return null
        return minOf(requestedPaddingEndPx.toLong(), available).toInt()
    }

    // A bounded native reservation grants less space than requested by the
    // transition. Convert the applied padding delta back to slot width.
    fun appliedTransitionSlotWidth(
        requestedSlotWidthPx: Int,
        requestedReservationDeltaPx: Int,
        appliedReservationDeltaPx: Int,
    ): Int = (
        requestedSlotWidthPx.toLong() -
            (requestedReservationDeltaPx.toLong() - appliedReservationDeltaPx.toLong())
        ).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    // Native fake host settlement and its status-icon row measure are
    // independent: a cached measureViews list is not authoritative while
    // the peer row itself still requests a layout traversal.
    fun fakeNativePeerMeasureReady(
        carrierSettled: Boolean,
        peerRowLayoutRequested: Boolean,
    ): Boolean = carrierSettled && !peerRowLayoutRequested

    // Native FAKE row capacity is strict. Native measuredWidth already
    // includes child padding, so the corrected peer measure must fit.
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
