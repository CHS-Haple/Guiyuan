package com.chaners.guiyuan.xposed

// Stateless decisions live here; the runtime owner keeps hooks, sessions and mutable state.

internal enum class KeyguardFamilySurface(
    val surfaceName: String,
    val eventPrefix: String,
) {
    KEYGUARD("keyguard", "keyguardPresentation"),
    AOD("aod", "aodPresentation"),
}

/** The same structural checks gate Home, Keyguard and CC projections. */
internal object ProjectionReadinessPolicy {
    fun structuralReady(
        featureEnabled: Boolean,
        modelReady: Boolean,
        tintReady: Boolean,
        layoutReady: Boolean,
        hostAttached: Boolean,
    ): Boolean =
        featureEnabled &&
            modelReady &&
            tintReady &&
            layoutReady &&
            hostAttached
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

// Native FAKE width must settle before an optional, receipt-scoped capacity
// lease. Native end-overhang must not be narrowed by the module.
internal object FakeCarrierNativeGeometryPolicy {
    // Any overwrite ends this generation of a temporary width receipt.
    // Native layout (including orientation reflow) is authoritative.
    fun nativeReplacedLease(appliedWidthPx: Int, liveWidthPx: Int): Boolean =
        appliedWidthPx != liveWidthPx

    fun leaseWidth(nativeWidthPx: Int, parentContentWidthPx: Int, clearMargins: Boolean): Int? =
        parentContentWidthPx.takeIf { clearMargins && nativeWidthPx > 0 && it > nativeWidthPx }

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

    // FAKE host width and its native peer-row layout settle separately.
    // Do not cut over while either native layout is still pending.
    fun fakeNativePeerMeasureReady(
        carrierSettled: Boolean,
        peerRowLayoutRequested: Boolean,
    ): Boolean = carrierSettled && !peerRowLayoutRequested
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
