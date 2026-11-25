package com.chaners.guiyuan.xposed

import android.graphics.Rect
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.xposed.prefs.FeaturePrefsOwner
import com.chaners.guiyuan.xposed.prefs.VisualPrefsOwner
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SysUiCarrierMetrics
import java.lang.ref.WeakReference

internal object HomeRenderSession {
    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val STATUS_ICON_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"

    private var current: Session? = null
    private val retirement = RetirementGate<Session>()

    private fun stop(
        session: Session,
        removeVisual: Boolean = true,
        notifyReadinessOnStop: Boolean = true,
    ) {
        retirement.release(session) {
            session.stop(removeVisual, notifyReadinessOnStop)
            0
        }
    }

    // Callbacks remain bound to the attaching generation; no new runtime writer.
    internal class Sinks(
        val onEvent: (String) -> Unit,
        val onLatencySample: ((RuntimeRenderLatencySample) -> Unit)? = null,
        val isDetailedDiagnosticsEnabled: () -> Boolean = { true },
        val onPresentationReadinessChanged: ((Boolean, String) -> Unit)? = null,
        val onHomeFrameDrawn: (() -> Unit)? = null,
        val onBatteryDrawn: ((RenderModel, Long) -> Unit)? = null,
    )

    /** Resolved host identity is transient; Session retains only weak references. */
    private class HomeNativeViews(
        val host: ViewGroup,
        val batteryContainer: ViewGroup,
        val statusIcons: ViewGroup,
        val batteryView: ViewGroup,
        val batteryCarrier: View,
    )

    /** Initial native/feature facts are sampled once at attachment, never replayed as live state. */
    private class HomeSessionSeed(
        val nativeHandoffActive: Boolean,
        val controlCenterAllowsHome: Boolean,
        val tintState: TintState?,
        val allowLiveTintSeed: Boolean,
        val featureEnabled: Boolean,
    )

    @Synchronized
    fun attach(
        host: Any,
        sinks: Sinks,
        initialNativeHandoffActive: Boolean = false,
        initialControlCenterAllowsHome: Boolean = true,
        initialTintState: TintState? = null,
        allowLiveTintSeed: Boolean = true,
    ): String? {
        if (retirement.pending != null) return "home-render-release-incomplete"
        val hostView = host as? ViewGroup
            ?: return "host-not-view-group"
        val batteryContainer = hostView.directNativeChild(BATTERY_CONTAINER_CLASS_NAME) as? ViewGroup
            ?: return "battery-container-missing"
        val statusIcons = batteryContainer.directNativeChild(STATUS_ICON_CONTAINER_CLASS_NAME) as? ViewGroup
            ?: return "status-icons-missing"
        val batteryView = batteryContainer.directNativeChild(BATTERY_VIEW_CLASS_NAME) as? ViewGroup
            ?: return "battery-view-missing"
        val batteryCarrier =
            SysUiCarrierMetrics.resolveView(batteryView)
                ?: return "battery-core-carrier-missing"

        val native = HomeNativeViews(hostView, batteryContainer, statusIcons, batteryView, batteryCarrier)
        val existing = current
        if (existing?.matches(native) == true) {
            existing.update(StatusStateStore.snapshot())
            return null
        }

        existing?.let { stop(it) }
        val seed = HomeSessionSeed(
            nativeHandoffActive = initialNativeHandoffActive,
            controlCenterAllowsHome = initialControlCenterAllowsHome,
            tintState = initialTintState,
            allowLiveTintSeed = allowLiveTintSeed,
            featureEnabled = FeaturePrefsOwner.current().enabled,
        )
        val session = Session(native = native, sinks = sinks, seed = seed)
        current = session
        session.start()
        session.update(StatusStateStore.snapshot())
        return null
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
    fun onTintUpdate(update: SysUiTintSource.TintUpdate) {
        current?.updateTint(update)
    }

    @Synchronized
    fun onStatusIconTintUpdate(statusIconTint: Int?) {
        current?.updateStatusIconTint(statusIconTint)
    }

    @Synchronized
    fun onFeatureCfgChanged(cfg: FeatureCfg) {
        current?.setFeatureEnabled(cfg.enabled)
    }

    @Synchronized
    fun onVisualCfgChanged(visual: VisualCfg) {
        current?.updateVisualCfg(visual)
    }

    @Synchronized
    fun onControlCenterAuthorityChanged(homeEligible: Boolean) {
        current?.updateControlCenterHomeEligibility(
            homeEligible = homeEligible,
            source = "source-availability",
        )
    }

    @Synchronized
    fun isOverlayShown(): Boolean = current?.isOverlayShown() == true

    // Native owner readiness is insufficient during scene return: the Home
    // projection must also have valid live content and actual visibility.
    @Synchronized
    fun canDrawHome(): Boolean = current?.canDrawHome() == true

    @Synchronized
    fun visibilityWitness(): String = current?.visibilityWitness() ?: "home-session-unbound"

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
    fun detach(
        preserveVisual: Boolean = false,
        notifyReadinessOnStop: Boolean = true,
    ) {
        val session = current ?: retirement.pending ?: return
        stop(
            session,
            removeVisual = !preserveVisual,
            notifyReadinessOnStop = notifyReadinessOnStop,
        )
        if (current === session) current = null
    }

    internal fun resolveOverlayVisible(
        featureEnabled: Boolean,
        controlCenterAllowsHome: Boolean,
        nativeHandoffActive: Boolean,
    ): Boolean =
        featureEnabled &&
            controlCenterAllowsHome &&
            !nativeHandoffActive

    internal fun resolveOwnerReady(
        featureEnabled: Boolean,
        modelReady: Boolean,
        tintReady: Boolean,
        layoutReady: Boolean,
        hostAttached: Boolean,
    ): Boolean =
        ProjectionReadinessPolicy.structuralReady(
            featureEnabled, modelReady, tintReady, layoutReady, hostAttached,
        )

    // A child onLayout can run before SystemUI's parent finishes its traversal.
    // Retire an already verified Home owner only on actual detach, or at the
    // complete native pre-draw boundary after pending layout has settled.
    internal fun shouldRetireUnresolvedAnchor(
        wasReady: Boolean,
        hostAttached: Boolean,
        carrierAttached: Boolean,
        nativePreDraw: Boolean,
        layoutPending: Boolean,
    ): Boolean =
        wasReady &&
            (!hostAttached || !carrierAttached || (nativePreDraw && !layoutPending))

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
            transferred?.takeIf { it.isVisible }
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
                ?.takeIf { it.isVisible }
                ?: return null
        return InitialTintSeed(
            state = liveValid,
            source = "seed",
        )
    }


    private class Session(
        native: HomeNativeViews,
        private val sinks: Sinks,
        seed: HomeSessionSeed,
    ) : View.OnAttachStateChangeListener {
        private val initialTintState = seed.tintState
        private val allowLiveTintSeed = seed.allowLiveTintSeed
        private val host = WeakReference(native.host)
        private val batteryContainer = WeakReference(native.batteryContainer)
        private val statusIcons = WeakReference(native.statusIcons)
        private val batteryView = WeakReference(native.batteryView)
        private val batteryCarrier = WeakReference(native.batteryCarrier)
        private var lastCompletedDrawElapsedRealtimeMs = 0L
        private var lastDrawnCharging: Boolean? = null
        private var lastDrawnBatterySemantic: BatterySemanticState? = null
        private val probeView =
            RenderView(
                context = native.host.context,
                // Passive draw witness: the Overlay child may report isShown=false
                // even when the native host actually dispatches its overlay.
                onModelDrawn = { drawn ->
                    if (HomeRenderSession.current === this &&
                        (drawn.charging != lastDrawnCharging ||
                            drawn.batterySemanticState != lastDrawnBatterySemantic)
                    ) {
                        lastDrawnCharging = drawn.charging
                        lastDrawnBatterySemantic = drawn.batterySemanticState
                        val drawElapsedMs = SystemClock.elapsedRealtime()
                        SysUiPresentationRuntime.emitPassiveDiagnostic {
                            sinks.onBatteryDrawn?.invoke(drawn, drawElapsedMs)
                        }
                    }
                },
                onDrawn = {
                    lastCompletedDrawElapsedRealtimeMs = SystemClock.elapsedRealtime()
                    if (
                        !homeFrameDrawnAfterPanel &&
                        HomeRenderSession.current === this &&
                        featureEnabled &&
                        controlCenterAllowsHome &&
                        !nativeHandoffActive &&
                        modelReady &&
                        tintReady &&
                        layoutReady &&
                        this@Session.host.get()?.isShown == true
                    ) {
                        homeFrameDrawnAfterPanel = true
                        sinks.onHomeFrameDrawn?.invoke()
                    }
                },
                onStateRendered = { latencyMs, committedOnMainThread, sample ->
                    if (sample != null && sinks.onLatencySample != null) {
                        SysUiPresentationRuntime.emitPassiveDiagnostic {
                            sinks.onLatencySample.invoke(sample)
                        }
                    } else {
                        emitEvent {
                            "homeRenderLatency stateToDrawMs=" + latencyMs +
                                " commitMainThread=" + committedOnMainThread +
                                " scheduling=sameFramePreferred"
                        }
                    }
                },
            )
        private val renderController = RenderController(probeView)
        private var readyLogged = false
        private var layoutLogged = false
        private var deferredStateLogged = false
        private var rejectedTintLogged = false
        private var nativeTintObserved = false
        // Home eligibility follows native CC ownership independently of the
        // separate QS_FAKE projection readiness.
        private var controlCenterAllowsHome = seed.controlCenterAllowsHome
        private var nativeHandoffActive = seed.nativeHandoffActive
        private var featureEnabled = seed.featureEnabled
        private var modelReady = false
        private var tintReady = false
        private var layoutReady = false
        private var lastPresentationReady = false
        private var homeFrameDrawnAfterPanel = false
        private val anchorRect = Rect()
        private val observedAnchorRect = Rect()
        private val carrierWindowLocation = IntArray(2)
        private val hostWindowLocation = IntArray(2)
        private var nativeDrawObserver: ViewTreeObserver? = null
        private var deferredNativeAnchor = false

        // The privacy-sensitive icons group can relayout without its parent.
        // Listen on all three native Views, with a separate receipt for each View.
        private val nativeLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                // Child layout callbacks are not evidence that the whole
                // native status-bar frame has finished. An initial recovery
                // cutover happens at the native pre-draw boundary instead.
                if (layoutReady) layoutProbe()
            }
        // SystemUI may translate the carrier during a Control Center rotation
        // without changing this host's measured size or firing onLayout.
        // Read native geometry before drawing; only a changed anchor writes
        // our own overlay bounds.
        private val nativeDrawListener =
            ViewTreeObserver.OnPreDrawListener {
                if (
                    featureEnabled &&
                    this@Session.host.get()?.isAttachedToWindow == true
                ) {
                    val resolved = resolveNativeAnchor(observedAnchorRect)
                    // Recheck an interrupted native layout even when the
                    // previous invalidation already yielded to native.
                    // The old nativeHandoffActive guard stranded that path
                    // until a later, unrelated onLayout callback.
                    if (!resolved || !layoutReady || observedAnchorRect != anchorRect) {
                        layoutProbe("native-pre-draw")
                    }
                }
                true
            }

        private fun registerNativeDrawListener() {
            val observer = host.get()
                ?.takeIf { it.isAttachedToWindow }
                ?.viewTreeObserver
                ?.takeIf { it.isAlive }
                ?: return
            if (nativeDrawObserver === observer) return
            unregisterNativeDrawListener()
            nativeDrawObserver = observer
            observer.addOnPreDrawListener(nativeDrawListener)
        }

        private fun unregisterNativeDrawListener() {
            nativeDrawObserver?.let { observer ->
                if (observer.isAlive) observer.removeOnPreDrawListener(nativeDrawListener)
            }
            nativeDrawObserver = null
        }

        fun matches(native: HomeNativeViews): Boolean =
            this.host.get() === native.host &&
                this.batteryContainer.get() === native.batteryContainer &&
                this.statusIcons.get() === native.statusIcons &&
                this.batteryView.get() === native.batteryView &&
                this.batteryCarrier.get() === native.batteryCarrier

        fun start() {
            val overlayHost = host.get() ?: return
            val nativeContainer = batteryContainer.get() ?: return
            val battery = batteryView.get() ?: return
            val carrier = batteryCarrier.get() ?: return

            overlayHost.addOnAttachStateChangeListener(this)
            overlayHost.addOnLayoutChangeListener(nativeLayoutListener)
            nativeContainer.addOnLayoutChangeListener(nativeLayoutListener)
            battery.addOnLayoutChangeListener(nativeLayoutListener)
            carrier.addOnLayoutChangeListener(nativeLayoutListener)
            statusIcons.get()?.addOnLayoutChangeListener(nativeLayoutListener)
            registerNativeDrawListener()
            probeView.visibility = View.GONE
            // Keep the visual outside the native battery child's hide/layout lifecycle.
            overlayHost.overlay.add(probeView)
            renderController.updateVisualCfg(
                VisualPrefsOwner.current(),
            )
            resolveInitialTintSeed(
                transferred = initialTintState,
                allowLiveSeed = allowLiveTintSeed,
                liveState = {
                    SysUiTintSource.currentState(battery)?.also { state ->
                        if (state.isVisible) nativeTintObserved = true
                    }
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
            return probeView.captureTransitionWitness(motion, host) {
                SysUiPresentationOwner.homeSlots()
            }
        }

        fun stop(
            removeVisual: Boolean = true,
            notifyReadinessOnStop: Boolean = true,
        ) {
            layoutReady = false
            releaseAll(
                { if (notifyReadinessOnStop) dispatchPresentationReadiness("stop") },
                { host.get()?.removeOnAttachStateChangeListener(this) },
                { host.get()?.removeOnLayoutChangeListener(nativeLayoutListener) },
                { batteryContainer.get()?.removeOnLayoutChangeListener(nativeLayoutListener) },
                { batteryView.get()?.removeOnLayoutChangeListener(nativeLayoutListener) },
                { batteryCarrier.get()?.removeOnLayoutChangeListener(nativeLayoutListener) },
                { statusIcons.get()?.removeOnLayoutChangeListener(nativeLayoutListener) },
                { unregisterNativeDrawListener() },
                { if (removeVisual) host.get()?.overlay?.remove(probeView) },
            )
        }

        fun updateControlCenterHomeEligibility(
            homeEligible: Boolean,
            source: String,
            detail: String = "",
        ) {
            if (Looper.myLooper() !== Looper.getMainLooper()) {
                host.get()?.post {
                    updateControlCenterHomeEligibility(
                        homeEligible = homeEligible,
                        source = source,
                        detail = detail,
                    )
                }
                return
            }
            if (HomeRenderSession.current !== this) return
            if (controlCenterAllowsHome == homeEligible) {
                return
            }
            controlCenterAllowsHome = homeEligible
            if (!homeEligible) homeFrameDrawnAfterPanel = false
            if (SandboxHost.active()) update(StatusStateStore.snapshot())
            if (homeEligible) layoutProbe("control-center-return")
            val visible = applyResolvedVisibility()
            emitEvent {
                "homeRenderControlCenterEligibility" +
                    " source=" + source +
                    detail +
                    " homeEligible=" + controlCenterAllowsHome +
                    " visible=" + visible
            }
            dispatchPresentationReadiness("control-center:" + source)
        }

        fun isOverlayShown(): Boolean = probeView.isShown

        fun canDrawHome(): Boolean =
            featureEnabled &&
                controlCenterAllowsHome &&
                !nativeHandoffActive &&
                modelReady &&
                tintReady &&
                layoutReady &&
                probeView.visibility == View.VISIBLE &&
                homeFrameDrawnAfterPanel &&
                host.get()?.isAttachedToWindow == true &&
                batteryContainer.get()?.isAttachedToWindow == true &&
                batteryCarrier.get()?.isAttachedToWindow == true &&
                host.get()?.isShown == true

        fun visibilityWitness(): String {
            val root = host.get()
            val nativeContainer = batteryContainer.get()
            return "renderVisibility=${probeView.visibility}" +
                " renderShown=${probeView.isShown}" +
                " renderShownSemantics=overlay-parent-unparented" +
                " lastCompletedDrawElapsedRealtimeMs=$lastCompletedDrawElapsedRealtimeMs" +
                " lastDrawAgeMs=" +
                (if (lastCompletedDrawElapsedRealtimeMs > 0L) {
                    SystemClock.elapsedRealtime() - lastCompletedDrawElapsedRealtimeMs
                } else "never") +
                " renderAlpha=${probeView.alpha}" +
                " renderBounds=${probeView.left}..${probeView.right}" +
                " overlayParent=${probeView.parent?.javaClass?.simpleName}" +
                " rootShown=${root?.isShown}" +
                " rootAlpha=${root?.alpha}" +
                " rootTranslationX=${root?.translationX}" +
                " rootBounds=${root?.left}..${root?.right}" +
                " rootAttached=${root?.isAttachedToWindow}" +
                " nativeContainerShown=${nativeContainer?.isShown}" +
                " statusIconsShown=${statusIcons.get()?.isShown}" +
                " statusIconsAlpha=${statusIcons.get()?.alpha}" +
                " nativeContainerSize=${nativeContainer?.width}x${nativeContainer?.height}" +
                " nativeBatteryWidth=${batteryView.get()?.width}" +
                " nativeHide=${SysUiPresentationOwner.homeBatteryNativeHidden()}" +
                " handoff=$nativeHandoffActive" +
                " homeEligible=$controlCenterAllowsHome" +
                " layoutReady=$layoutReady"
        }

        fun setFeatureEnabled(enabled: Boolean) {
            if (Looper.myLooper() !== Looper.getMainLooper()) {
                host.get()?.post {
                    setFeatureEnabled(enabled)
                }
                return
            }
            if (HomeRenderSession.current !== this) return
            if (featureEnabled == enabled) {
                return
            }
            featureEnabled = enabled
            val visible = applyResolvedVisibility()
            emitEvent {
                "homeRenderFeature enabled=" + featureEnabled +
                    " overlayVisible=" + visible +
                    " nativeHandoffActive=" + nativeHandoffActive
            }
            dispatchPresentationReadiness("feature")
        }

        fun setNativeHandoffActive(active: Boolean) {
            if (nativeHandoffActive == active) {
                return
            }
            nativeHandoffActive = active
            if (SandboxHost.active()) update(StatusStateStore.snapshot())
            // Privacy may already be occupying Home's right end when the module
            // first takes ownership. No subsequent native layout change is
            // guaranteed, so reread its laid-out edge before revealing Guiyuan.
            if (!active) layoutProbe()
            val visible = applyResolvedVisibility()
            emitEvent {
                "homeRenderHandoff nativeActive=" + nativeHandoffActive +
                    " overlayVisible=" + visible
            }
        }

        fun updateVisualCfg(visual: VisualCfg) {
            renderController.updateVisualCfg(visual)
            layoutProbe()
        }

        fun updateTint(update: SysUiTintSource.TintUpdate) {
            val battery = batteryView.get() ?: return
            if (update.sourceView !== battery) {
                return
            }
            if (update.state.isVisible) nativeTintObserved = true
            applyTintState(update.state, "darkReceiver")
        }

        fun updateStatusIconTint(statusIconTint: Int?) {
            val resolved =
                TintAuthority.resolveStatusIconEvent(
                    nativeTintObserved = nativeTintObserved,
                    liveStatusIconTint = statusIconTint,
                ) ?: return
            applyTintState(resolved, "statusIcons")
        }

        private fun applyTintState(
            state: TintState,
            source: String,
        ) {
            val update = renderController.updateTint(state)
            val becameReady = update.resolved != null && !tintReady
            if (becameReady) {
                tintReady = true
            }

            if (update.rejectedInvalidCandidate && !rejectedTintLogged) {
                rejectedTintLogged = true
                emitEvent {
                    "homeRenderTint deferred source=" + source +
                        " applied=#" +
                        tintHexDigits(state.appliedTint) +
                        " reason=transparent retainStable=true"
                }
            }

            if (becameReady) {
                dispatchPresentationReadiness(source)
            }
        }

        fun update(
            snapshot: StatusStateStore.Snapshot,
            trace: RuntimeRenderTrace? = null,
        ) {
            val visibleTrace =
                trace?.takeIf {
                    layoutLogged &&
                        controlCenterAllowsHome
                }
            val update =
                renderController.update(
                    snapshot = snapshot,
                    trace = visibleTrace,
                    preview = if (
                        featureEnabled && controlCenterAllowsHome && !nativeHandoffActive
                    ) SandboxHost.model(probeView.context) else null,
                )

            // Lease expiry must restore the latest real model even if this native frame is incomplete.
            if (update.changed) layoutProbe()
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
            registerNativeDrawListener()
            layoutProbe()
        }

        override fun onViewDetachedFromWindow(view: View) {
            unregisterNativeDrawListener()
            if (layoutReady) {
                layoutReady = false
                dispatchPresentationReadiness("host-detached")
            }
        }

        private fun layoutProbe(source: String = "native-layout") {
            if (!resolveNativeAnchor(observedAnchorRect)) {
                handleUnresolvedAnchor(source)
                return
            }
            if (deferredNativeAnchor) {
                deferredNativeAnchor = false
                emitEvent { "homeRenderNativeAnchor state=recovered source=" + source }
            }
            val moved = observedAnchorRect != anchorRect
            if (moved) {
                anchorRect.set(observedAnchorRect)
                if (layoutLogged) {
                    emitEvent {
                        "homeRenderNativeAnchor source=" + source +
                            " carrierLeft=" + anchorRect.left +
                            " carrierRight=" + anchorRect.right +
                            " carrierWidth=" + anchorRect.width() +
                            " hostWidth=" + (host.get()?.width ?: -1)
                    }
                }
            }
            probeView.layoutAtAnchor(anchorRect)
            layoutReady = true

            if (!layoutLogged) {
                layoutLogged = true
                emitEvent {
                    "homeRenderProbe attached " +
                        "slot=homeSystemIconsOverlay anchor=system_icon_area.end " +
                        "carrier=MiuiNotificationStatusContainer.overlay " +
                        "widthAuthority=battery_icon_container " +
                        "motion=system-ui-inherited " +
                        "bounds=" + anchorRect.left + "," + anchorRect.top + "-" +
                        anchorRect.right + "," + anchorRect.bottom +
                        " logicalSize=" + anchorRect.width() + "x" + anchorRect.height() +
                        " physicalSize=" + probeView.width + "x" + probeView.height +
                        " topOverflowPx=" + probeView.currentLogicalViewportTopInsetPx() +
                        " opacity=" + RENDER_OPACITY
                }
            }
            dispatchPresentationReadiness("layout")
        }

        private fun handleUnresolvedAnchor(source: String) {
            val nativeLayoutPending =
                host.get()?.isLayoutRequested == true ||
                    batteryContainer.get()?.isLayoutRequested == true ||
                    statusIcons.get()?.isLayoutRequested == true ||
                    batteryCarrier.get()?.isLayoutRequested == true
            val hostAttached = batteryContainer.get()?.isAttachedToWindow == true
            val carrierAttached = batteryCarrier.get()?.isAttachedToWindow == true
            if (
                shouldRetireUnresolvedAnchor(
                    wasReady = layoutReady,
                    hostAttached = hostAttached,
                    carrierAttached = carrierAttached,
                    nativePreDraw = source == "native-pre-draw",
                    layoutPending = nativeLayoutPending,
                )
            ) {
                layoutReady = false
                deferredNativeAnchor = false
                emitEvent {
                    "homeRenderNativeAnchor state=invalid source=" + source +
                        " nativeLayoutPending=" + nativeLayoutPending +
                        " hostAttached=" + hostAttached +
                        " carrierAttached=" + carrierAttached
                }
                dispatchPresentationReadiness("native-anchor-invalid")
            } else if (layoutReady && !deferredNativeAnchor) {
                deferredNativeAnchor = true
                emitEvent {
                    "homeRenderNativeAnchor state=deferred source=" + source +
                        " nativeLayoutPending=" + nativeLayoutPending
                }
            }
        }

        private fun applyResolvedVisibility(): Boolean {
            val visible =
                resolveOverlayVisible(
                    featureEnabled = featureEnabled,
                    controlCenterAllowsHome = controlCenterAllowsHome,
                    nativeHandoffActive = nativeHandoffActive,
                )
            probeView.updateRenderVisibility(visible)
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
                    controlCenterAllowsHome +
                    " modelReady=" + modelReady +
                    " tintReady=" + tintReady +
                    " layoutReady=" + layoutReady +
                    " controlCenterHomeEligible=" + controlCenterAllowsHome +
                    " featureEnabled=" + featureEnabled
            }
            sinks.onPresentationReadinessChanged?.invoke(ownerReady, source)
        }

        private inline fun emitEvent(message: () -> String) {
            SysUiPresentationRuntime.emitPassiveDiagnostic {
                if (sinks.isDetailedDiagnosticsEnabled()) sinks.onEvent(message())
            }
        }

        private fun resolveNativeAnchor(out: Rect): Boolean {
            val overlayHost = host.get() ?: return false
            val nativeContainer = batteryContainer.get() ?: return false
            val carrier = batteryCarrier.get() ?: return false
            if (!overlayHost.isAttachedToWindow || !nativeContainer.isAttachedToWindow ||
                !carrier.isAttachedToWindow
            ) return false
            val hostWidth = overlayHost.width
            val hostHeight = overlayHost.height
            val baseCarrierWidth =
                SysUiCarrierMetrics
                    .resolveWidthPx(carrier)
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

            val isRtl = overlayHost.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val privacyOccupiesEnd =
                SysUiPresentationOwner.homePrivacyOccupiesEnd(nativeContainer)
            val resolved =
                if (privacyOccupiesEnd == true) {
                    val nativeIcons = statusIcons.get() ?: return false
                    if (nativeIcons.parent !== nativeContainer ||
                        !nativeIcons.isAttachedToWindow || !nativeIcons.isLaidOut ||
                        nativeIcons.width <= 0
                    ) return false
                    nativeIcons.getLocationInWindow(carrierWindowLocation)
                    overlayHost.getLocationInWindow(hostWindowLocation)
                    val iconStart = carrierWindowLocation[0] - hostWindowLocation[0]
                    HomePrivacyAnchorPolicy.resolve(
                        hostWidthPx = hostWidth,
                        hostHeightPx = hostHeight,
                        baseCarrierWidthPx = baseCarrierWidth,
                        statusIconsEndPx =
                            if (isRtl) iconStart else iconStart + nativeIcons.width,
                        isRtl = isRtl,
                    )
                } else {
                    // Width is already resolved by resolveWidthPx above,
                    // which accepts a valid native measuredWidth while the
                    // battery_icon_container's laid-out width is transiently
                    // zero during the charging-island layout traversal.
                    // system_icon_area owns Home placement and island motion.
                    // Battery child translation/fade on island exit must not
                    // invalidate or move the independently hosted overlay.
                    SteadyLayoutResolver.resolve(
                        hostWidthPx = hostWidth,
                        hostHeightPx = hostHeight,
                        baseCarrierWidthPx = baseCarrierWidth,
                        isRtl = isRtl,
                    )
                } ?: return false
            out.set(resolved.left, 0, resolved.right, hostHeight)
            return out.width() > 0 && out.height() > 0
        }
    }

    private const val RENDER_OPACITY = 1f
}
