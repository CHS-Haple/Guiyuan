package com.chaners.guiyuan.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.chaners.guiyuan.settings.FeatureSettings
import com.chaners.guiyuan.settings.VisualSettings
import java.lang.ref.WeakReference

internal object HomeRenderSession {
    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val STATUS_ICON_CONTAINER_CLASS =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"

    private var current: Session? = null

    @Synchronized
    fun attach(
        host: Any,
        onEvent: (String) -> Unit,
        onLatencySample: ((RenderLatencySample) -> Unit)? = null,
        isDetailedDiagnosticsEnabled: () -> Boolean = { true },
        initialNativeHandoffActive: Boolean = false,
        initialTintState: TintState? = null,
        allowLiveTintSeed: Boolean = true,
        onPresentationReadinessChanged: ((Boolean) -> Unit)? = null,
    ): AttachResult {
        val hostView = host as? ViewGroup
            ?: return AttachResult.Failure("host-not-view-group")
        val batteryContainer = hostView.directChild(BATTERY_CONTAINER_CLASS_NAME)
            ?: return AttachResult.Failure("battery-container-missing")
        val statusIcons = batteryContainer.directChild(STATUS_ICON_CONTAINER_CLASS)
            ?: return AttachResult.Failure("status-icons-missing")
        val batteryView = batteryContainer.directChild(BATTERY_VIEW_CLASS_NAME)
            ?: return AttachResult.Failure("battery-view-missing")
        val batteryCarrier =
            HomeCarrierMetrics.resolveCarrierView(batteryView)
                ?: return AttachResult.Failure("battery-core-carrier-missing")

        val existing = current
        if (
            existing?.matches(
                hostView,
                batteryContainer,
                statusIcons,
                batteryView,
                batteryCarrier,
            ) == true
        ) {
            existing.update(StatusStateStore.snapshot())
            return AttachResult.Ready
        }

        existing?.stop()
        val session = Session(
            host = hostView,
            batteryContainer = batteryContainer,
            statusIcons = statusIcons,
            batteryView = batteryView,
            batteryCarrier = batteryCarrier,
            onEvent = onEvent,
            onLatencySample = onLatencySample,
            isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
            initialNativeHandoffActive = initialNativeHandoffActive,
            initialTintState = initialTintState,
            allowLiveTintSeed = allowLiveTintSeed,
            initialFeatureEnabled =
                FeaturePrefsOwner.currentSettings().enabled,
            onPresentationReadinessChanged = onPresentationReadinessChanged,
        )
        current = session
        session.start()
        session.update(StatusStateStore.snapshot())
        return AttachResult.Ready
    }

    @Synchronized
    fun onState(
        snapshot: StatusStateStore.Snapshot,
        trace: RuntimeRenderTrace? = null,
    ) {
        current?.update(snapshot, trace)
    }

    @Synchronized
    fun onPresentationStateChanged(trace: RuntimeRenderTrace? = null) {
        current?.update(StatusStateStore.snapshot(), trace)
    }

    @Synchronized
    fun onTintUpdate(update: TintSource.TintUpdate) {
        current?.updateTint(update)
    }

    @Synchronized
    fun onStatusIconTintUpdate(statusIconTint: Int?) {
        current?.updateStatusIconTint(statusIconTint)
    }

    @Synchronized
    fun onFeatureSettingsChanged(settings: FeatureSettings) {
        current?.setFeatureEnabled(settings.enabled)
    }

    @Synchronized
    fun onVisualSettingsChanged(settings: VisualSettings) {
        current?.updateVisualSettings(settings)
    }

    @Synchronized
    fun onControlCenterAuthorityChanged(homeEligible: Boolean) {
        current?.updateCcHomeEligibility(
            homeEligible = homeEligible,
            source = "source-availability",
        )
    }

    @Synchronized
    fun setNativeHandoffActive(active: Boolean) {
        current?.setNativeHandoffActive(active)
    }

    @Synchronized
    fun currentTintState(): TintState? = current?.currentTintState()

    @Synchronized
    fun currentTransitionSourceWitness(): TransitionSourceWitness? =
        current?.transitionSourceWitness()

    @Synchronized
    fun detach(preserveVisual: Boolean = false) {
        current?.stop(removeVisual = !preserveVisual)
        current = null
    }

    internal fun resolveOverlayVisible(
        featureEnabled: Boolean,
        ccAllowsHome: Boolean,
        nativeHandoffActive: Boolean,
    ): Boolean =
        featureEnabled &&
            ccAllowsHome &&
            !nativeHandoffActive

    internal fun resolveOwnerReady(
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

    internal data class InitialTintSeed(
        val state: TintState,
        val source: String,
    )

    internal fun resolveInitialTintSeed(
        transferred: TintState?,
        allowLiveSeed: Boolean,
        liveState: () -> TintState?,
    ): InitialTintSeed? {
        val transferredValid =
            transferred?.takeIf(PresentationPolicy::isValidTint)
        if (transferredValid != null) {
            return InitialTintSeed(
                state = transferredValid,
                source = "hotReloadTransfer",
            )
        }
        if (!allowLiveSeed) {
            return null
        }

        val liveValid =
            liveState()
                ?.takeIf(PresentationPolicy::isValidTint)
                ?: return null
        return InitialTintSeed(
            state = liveValid,
            source = "seed",
        )
    }

    private fun ViewGroup.directChild(className: String): ViewGroup? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) {
                return child as? ViewGroup
            }
        }
        return null
    }

    private class Session(
        host: ViewGroup,
        batteryContainer: ViewGroup,
        statusIcons: ViewGroup,
        batteryView: ViewGroup,
        batteryCarrier: View,
        private val onEvent: (String) -> Unit,
        private val onLatencySample: ((RenderLatencySample) -> Unit)?,
        private val isDetailedDiagnosticsEnabled: () -> Boolean,
        initialNativeHandoffActive: Boolean,
        private val initialTintState: TintState?,
        private val allowLiveTintSeed: Boolean,
        initialFeatureEnabled: Boolean,
        private val onPresentationReadinessChanged: ((Boolean) -> Unit)?,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val batteryContainer = WeakReference(batteryContainer)
        private val statusIcons = WeakReference(statusIcons)
        private val batteryView = WeakReference(batteryView)
        private val batteryCarrier = WeakReference(batteryCarrier)
        private val probeView =
            RenderView(host.context) { latencyMs, committedOnMainThread, sample ->
                if (sample != null && onLatencySample != null) {
                    onLatencySample.invoke(sample)
                } else {
                    emitEvent {
                        "homeRenderLatency stateToDrawMs=" + latencyMs +
                            " commitMainThread=" + committedOnMainThread +
                            " scheduling=sameFramePreferred"
                    }
                }
            }
        private val renderController = RenderController(probeView)
        private var readyLogged = false
        private var layoutLogged = false
        private var deferredStateLogged = false
        private var rejectedTintLogged = false
        // Control Center handoff is coordinator-owned. Source visibility is
        // diagnostic context only until a projected carrier is ready.
        private var ccAllowsHome = true
        private var nativeHandoffActive = initialNativeHandoffActive
        private var featureEnabled = initialFeatureEnabled
        private var modelReady = false
        private var tintReady = false
        private var layoutReady = false
        private var lastPresentationReady = false
        private val anchorRect = Rect()

        private val overlayHostLayoutListener =
            View.OnLayoutChangeListener {
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                ->
                layoutProbe()
            }

        private val carrierLayoutListener =
            View.OnLayoutChangeListener {
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                ->
                layoutProbe()
            }

        fun matches(
            host: ViewGroup,
            batteryContainer: ViewGroup,
            statusIcons: ViewGroup,
            batteryView: ViewGroup,
            batteryCarrier: View,
        ): Boolean =
            this.host.get() === host &&
                this.batteryContainer.get() === batteryContainer &&
                this.statusIcons.get() === statusIcons &&
                this.batteryView.get() === batteryView &&
                this.batteryCarrier.get() === batteryCarrier

        fun start() {
            host.get() ?: return
            val overlayHost = batteryContainer.get() ?: return
            val battery = batteryView.get() ?: return
            val carrier = batteryCarrier.get() ?: return

            overlayHost.addOnAttachStateChangeListener(this)
            overlayHost.addOnLayoutChangeListener(overlayHostLayoutListener)
            carrier.addOnLayoutChangeListener(carrierLayoutListener)
            probeView.visibility = View.GONE
            (probeView.parent as? ViewGroup)?.removeView(probeView)
            overlayHost.addView(
                probeView,
                ViewGroup.LayoutParams(0, 0),
            )
            renderController.updateVisualSettings(
                VisualPrefsOwner.currentSettings(),
            )
            resolveInitialTintSeed(
                transferred = initialTintState,
                allowLiveSeed = allowLiveTintSeed,
                liveState = {
                    TintSource.currentState(battery)
                },
            )?.let { seed ->
                applyTintState(seed.state, seed.source)
            }
            layoutProbe()
        }

        fun currentTintState(): TintState? =
            renderController.currentTintState()

        fun transitionSourceWitness(): TransitionSourceWitness? {
            val motion = statusIcons.get() ?: return null
            val render = probeView
            if (
                !ScenePolicy.hasRetainedSourceWitness(
                    widthPx = render.width,
                    heightPx = render.height,
                    hostAttached =
                        batteryContainer.get()?.isAttachedToWindow == true &&
                            motion.isAttachedToWindow,
                )
            ) {
                return null
            }
            if (
                motion.width <= 0 ||
                motion.height <= 0
            ) {
                return null
            }
            return TransitionSourceWitness(
                renderView = render,
                logicalLeftPx = 0,
                logicalTopPx = render.currentLogicalViewportTopInsetPx(),
                logicalWidthPx = render.currentLogicalViewportWidthPx(),
                logicalHeightPx = render.currentLogicalViewportHeightPx(),
                positionHost = batteryContainer.get() ?: return null,
                motionCarrier = motion,
                representedSlots =
                    HomePresentation.homeOwnedSlots(),
            )
        }

        fun stop(removeVisual: Boolean = true) {
            layoutReady = false
            dispatchPresentationReadiness("stop")
            batteryContainer.get()?.removeOnAttachStateChangeListener(this)
            batteryContainer.get()?.removeOnLayoutChangeListener(overlayHostLayoutListener)
            batteryCarrier.get()?.removeOnLayoutChangeListener(carrierLayoutListener)
            if (removeVisual) {
                (probeView.parent as? ViewGroup)?.removeView(probeView)
            }
        }

        fun updateCcHomeEligibility(
            homeEligible: Boolean,
            source: String,
            detail: String = "",
        ) {
            if (Looper.myLooper() !== Looper.getMainLooper()) {
                host.get()?.post {
                    updateCcHomeEligibility(
                        homeEligible = homeEligible,
                        source = source,
                        detail = detail,
                    )
                }
                return
            }
            if (ccAllowsHome == homeEligible) {
                return
            }
            ccAllowsHome = homeEligible
            val visible = applyResolvedVisibility()
            emitEvent {
                "homeRenderControlCenterEligibility" +
                    " source=" + source +
                    detail +
                    " homeEligible=" + ccAllowsHome +
                    " visible=" + visible +
                    " nativeGeometryWrites=0"
            }
            dispatchPresentationReadiness("control-center:" + source)
        }

        fun setFeatureEnabled(enabled: Boolean) {
            if (Looper.myLooper() !== Looper.getMainLooper()) {
                host.get()?.post {
                    setFeatureEnabled(enabled)
                }
                return
            }
            if (featureEnabled == enabled) {
                return
            }
            featureEnabled = enabled
            val visible = applyResolvedVisibility()
            emitEvent {
                "homeRenderFeature enabled=" + featureEnabled +
                    " overlayVisible=" + visible +
                    " nativeHandoffActive=" + nativeHandoffActive +
                    " nativeGeometryWrites=0"
            }
            dispatchPresentationReadiness("feature")
        }

        fun setNativeHandoffActive(active: Boolean) {
            if (nativeHandoffActive == active) {
                return
            }
            nativeHandoffActive = active
            val visible = applyResolvedVisibility()
            emitEvent {
                "homeRenderHandoff nativeActive=" + nativeHandoffActive +
                    " overlayVisible=" + visible +
                    " nativeGeometryWrites=0"
            }
        }

        fun updateVisualSettings(settings: VisualSettings) {
            renderController.updateVisualSettings(settings)
            layoutProbe()
        }

        fun updateTint(update: TintSource.TintUpdate) {
            val battery = batteryView.get() ?: return
            if (update.sourceView !== battery) {
                return
            }
            applyTintState(update.state, "darkReceiver")
        }

        fun updateStatusIconTint(statusIconTint: Int?) {
            val resolved =
                TintAuthority.resolveStatusIconEvent(
                    previous = renderController.currentTintState(),
                    liveStatusIconTint = statusIconTint,
                ) ?: return
            applyTintState(resolved, "statusIcons")
        }

        private fun applyTintState(
            state: TintState,
            source: String,
        ) {
            val update = renderController.updateTint(state)
            if (update.resolved != null) {
                tintReady = true
            }

            if (update.rejectedInvalidCandidate && !rejectedTintLogged) {
                rejectedTintLogged = true
                emitEvent {
                    "homeRenderTint deferred source=" + source +
                        " applied=#" +
                        state.appliedTint.toUInt().function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }(16).padStart(8, '0') +
                        " reason=transparent retainStable=true"
                }
            }

            if (update.changed) {
                val resolved = update.resolved
                if (resolved != null) {
                    emitEvent {
                        "homeRenderTint source=" + source +
                            " applied=#" +
                            resolved.appliedTint.toUInt().function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }(16).padStart(8, '0') +
                            " statusIcon=#" +
                            (
                                resolved.statusIconTint
                                    ?.toUInt()
                                    ?.function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }(16)
                                    ?.padStart(8, '0')
                                    ?: "none"
                            ) +
                            " eventDriven=true stable=true"
                    }
                }
            }
            dispatchPresentationReadiness("tint:" + source)
        }

        fun update(
            snapshot: StatusStateStore.Snapshot,
            trace: RuntimeRenderTrace? = null,
        ) {
            val visibleTrace =
                trace?.takeIf {
                    layoutLogged &&
                        ccAllowsHome
                }
            val update =
                renderController.update(
                    snapshot = snapshot,
                    trace = visibleTrace,
                )

            if (!update.candidateComplete) {
                if (update.retainedStable && !deferredStateLogged) {
                    deferredStateLogged = true
                    emitEvent {
                        "homeRenderState deferred incomplete=true " +
                            "retainStable=true"
                    }
                }
                return
            }

            val model = update.model
            if (update.candidateComplete && model != null) {
                modelReady = true
            }
            if (update.changed) {
                layoutProbe()
            }
            if (model != null && !readyLogged) {
                readyLogged = true
                emitEvent {
                    "homeRenderProbe ready " +
                        "battery=" + model.batteryPercent +
                        " charging=" + model.charging +
                        " center=" + model.centerIndicator.javaClass.simpleName +
                        " mobileLevel=" + (model.mobileLevel ?: -1) +
                        " effectiveDataSubId=" + model.effectiveDataSubscriptionId +
                        " defaultDataSubId=" + update.defaultDataSubscriptionId
                }
            }
            dispatchPresentationReadiness("model")
        }

        override fun onViewAttachedToWindow(view: View) {
            layoutProbe()
        }

        override fun onViewDetachedFromWindow(view: View) {
            if (layoutReady) {
                layoutReady = false
                dispatchPresentationReadiness("host-detached")
            }
        }

        private fun layoutProbe() {
            if (!resolveNativeAnchor(anchorRect)) {
                if (layoutReady) {
                    layoutReady = false
                    dispatchPresentationReadiness("layout-unavailable")
                }
                return
            }
            val topOverflowPx =
                probeView.requiredTopOverflowPx(
                    logicalWidthPx = anchorRect.width(),
                    logicalHeightPx = anchorRect.height(),
                )
            applyAnchorBounds(
                bounds = anchorRect,
                topOverflowPx = topOverflowPx,
            )
            layoutReady = true

            if (!layoutLogged) {
                layoutLogged = true
                emitEvent {
                    "homeRenderProbe attached " +
                        "slot=homeSystemIconsOverflow anchor=carrierEnd " +
                        "carrier=MiuiStatusBatteryContainer.child " +
                        "carrierAuthority=battery_icon_container " +
                        "motion=system-ui-inherited " +
                        "bounds=" + anchorRect.left + "," + anchorRect.top + "-" +
                        anchorRect.right + "," + anchorRect.bottom +
                        " logicalSize=" + anchorRect.width() + "x" + anchorRect.height() +
                        " physicalSize=" + probeView.width + "x" + probeView.height +
                        " topOverflowPx=" + probeView.currentLogicalViewportTopInsetPx() +
                        " opacity=" + RENDER_OPACITY +
                        " nativeVisibilityInherited=true nativeAlphaInherited=true " +
                        "originalsHidden=false nativeGeometryWrites=0"
                }
            }
            dispatchPresentationReadiness("layout")
        }

        private fun applyResolvedVisibility(): Boolean {
            val visible =
                resolveOverlayVisible(
                    featureEnabled = featureEnabled,
                    ccAllowsHome = ccAllowsHome,
                    nativeHandoffActive = nativeHandoffActive,
                )
            probeView.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                probeView.invalidate()
            } else {
                probeView.clearPendingLatency()
            }
            return visible
        }

        private fun dispatchPresentationReadiness(source: String) {
            val ownerReady =
                resolveOwnerReady(
                    featureEnabled = featureEnabled,
                    modelReady = modelReady,
                    tintReady = tintReady,
                    layoutReady = layoutReady,
                    hostAttached = batteryContainer.get()?.isAttachedToWindow == true,
                )
            if (ownerReady == lastPresentationReady) {
                return
            }
            lastPresentationReady = ownerReady
            emitEvent {
                "homeRenderReadiness source=" + source +
                    " ownerReady=" + ownerReady +
                    " overlayEligible=" +
                    ccAllowsHome +
                    " modelReady=" + modelReady +
                    " tintReady=" + tintReady +
                    " layoutReady=" + layoutReady +
                    " controlCenterHomeEligible=" + ccAllowsHome +
                    " featureEnabled=" + featureEnabled +
                    " nativeGeometryWrites=0"
            }
            onPresentationReadinessChanged?.invoke(ownerReady)
        }

        private inline fun emitEvent(message: () -> String) {
            if (isDetailedDiagnosticsEnabled()) {
                onEvent(message())
            }
        }

        private fun resolveNativeAnchor(out: Rect): Boolean {
            val overlayHost = batteryContainer.get() ?: return false
            val carrier = batteryCarrier.get() ?: return false
            val hostWidth = overlayHost.width
            val hostHeight = overlayHost.height
            val baseCarrierWidth =
                HomeCarrierMetrics
                    .resolveCarrierWidthPx(carrier)
                    ?.coerceAtMost(hostWidth)
                    ?: return false
            if (
                !overlayHost.isLaidOut ||
                hostWidth <= 0 ||
                hostHeight <= 0 ||
                baseCarrierWidth <= 0
            ) {
                return false
            }

            val rtl = overlayHost.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val resolved =
                HomeLayoutResolver.resolve(
                    hostWidthPx = hostWidth,
                    hostHeightPx = hostHeight,
                    baseCarrierWidthPx = baseCarrierWidth,
                    isRtl = rtl,
                ) ?: return false
            if (!resolved.renderCombined) {
                return false
            }

            val left =
                if (rtl) {
                    0
                } else {
                    resolved.slotLeftPx.toInt()
                }
            val right =
                if (rtl) {
                    resolved.slotRightPx.toInt()
                } else {
                    resolved.slotRightPx.toInt()
                }
            out.set(left, 0, right, hostHeight)
            return out.width() > 0 && out.height() > 0
        }

        private fun applyAnchorBounds(
            bounds: Rect,
            topOverflowPx: Int,
        ) {
            val physical =
                VerticalOverflowPolicy.resolve(
                    logicalTopPx = bounds.top,
                    logicalHeightPx = bounds.height(),
                    requestedTopOverflowPx = topOverflowPx,
                )
            probeView.setLogicalViewport(
                widthPx = bounds.width(),
                heightPx = bounds.height(),
                topInsetPx = physical.logicalTopInsetPx,
            )
            if (
                probeView.measuredWidth != bounds.width() ||
                probeView.measuredHeight != physical.physicalHeightPx
            ) {
                val widthSpec = View.MeasureSpec.makeMeasureSpec(
                    bounds.width(),
                    View.MeasureSpec.EXACTLY,
                )
                val heightSpec = View.MeasureSpec.makeMeasureSpec(
                    physical.physicalHeightPx,
                    View.MeasureSpec.EXACTLY,
                )
                probeView.measure(widthSpec, heightSpec)
            }
            probeView.layout(
                bounds.left,
                physical.physicalTopPx,
                bounds.right,
                bounds.bottom,
            )
        }
    }

    internal sealed interface AttachResult {
        data object Ready : AttachResult

        data class Failure(
            val reason: String,
        ) : AttachResult
    }

    private const val RENDER_OPACITY = 1f
}


internal object VerticalOverflowPolicy {
    internal data class Resolved(
        val physicalTopPx: Int,
        val physicalHeightPx: Int,
        val logicalTopInsetPx: Int,
    )

    fun resolve(
        logicalTopPx: Int,
        logicalHeightPx: Int,
        requestedTopOverflowPx: Int,
    ): Resolved {
        val logicalHeight = logicalHeightPx.coerceAtLeast(0)
        val overflow = requestedTopOverflowPx.coerceAtLeast(0)
        return Resolved(
            physicalTopPx = logicalTopPx - overflow,
            physicalHeightPx = logicalHeight + overflow,
            logicalTopInsetPx = overflow,
        )
    }
}
