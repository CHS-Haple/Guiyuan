package com.chaners.guiyuan.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.xposed.prefs.FeaturePrefsOwner
import com.chaners.guiyuan.xposed.prefs.VisualPrefsOwner
import java.lang.ref.WeakReference
import java.util.ArrayDeque

internal object CcRenderSession {
    private const val FAKE_ROOT_CLASS_NAME =
        "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons"
    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val STATUS_ICON_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val MAX_PREARM_LAYOUT_ATTEMPTS = 2

    private var current: Session? = null
    private var pendingPrearm: PendingPrearm? = null
    private var sceneEligible = false

    internal data class AttachFailure(
        val reason: String,
        val retryAfterLayout: Boolean = false,
    )

    @Synchronized
    fun prearmAfterNextNativeLayout(
        host: ViewGroup,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onProjectionReadinessChanged: (Boolean) -> Unit,
    ): PrearmResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return PrearmResult.Failure("main-thread-required")
        }
        if (host.javaClass.name != FAKE_ROOT_CLASS_NAME) {
            return PrearmResult.Failure("fake-root-type-mismatch")
        }
        if (!host.isAttachedToWindow) {
            return PrearmResult.Failure("fake-root-not-attached")
        }

        val existing = pendingPrearm
        if (existing?.matches(host) == true) {
            return PrearmResult.Scheduled(reused = true)
        }

        existing?.cancel()
        val pending =
            PendingPrearm(
                host = host,
                onEvent = onEvent,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                onProjectionReadinessChanged = onProjectionReadinessChanged,
            )
        pendingPrearm = pending
        pending.start()
        return PrearmResult.Scheduled(reused = false)
    }

    @Synchronized
    fun attach(
        host: ViewGroup,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onProjectionReadinessChanged: (Boolean) -> Unit,
    ): AttachFailure? {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return AttachFailure("main-thread-required")
        }
        if (host.javaClass.name != FAKE_ROOT_CLASS_NAME) {
            return AttachFailure("fake-root-type-mismatch")
        }
        val statusBarArea =
            host.uniqueDescendant(BATTERY_CONTAINER_CLASS_NAME)
                ?: return AttachFailure(
                    "fake-status-bar-area-unresolved",
                    retryAfterLayout = true,
                )
        val statusIcons =
            statusBarArea.directChild(STATUS_ICON_CONTAINER_CLASS_NAME) as? ViewGroup
                ?: return AttachFailure("status-icons-missing", retryAfterLayout = true)
        val battery =
            statusBarArea.directChild(BATTERY_VIEW_CLASS_NAME) as? ViewGroup
                ?: return AttachFailure("battery-view-missing", retryAfterLayout = true)
        val carrier =
            SysUiCarrierMetrics.resolveView(battery)
                ?: return AttachFailure(
                    "battery-core-carrier-missing",
                    retryAfterLayout = true,
                )
        val existing = current
        if (
            existing?.matches(
                host = host,
                statusBarArea = statusBarArea,
                statusIcons = statusIcons,
                battery = battery,
                carrier = carrier,
            ) == true
        ) {
            existing.setSceneEligible(sceneEligible)
            existing.refresh()
            return existing.prepareNativePresentation(reused = true)
        }

        if (existing != null) {
            SysUiPresentationOwner.deactivateCc("host-replaced")
            existing.stop("host-replaced")
        }

        val session =
            Session(
                host = host,
                statusBarArea = statusBarArea,
                statusIcons = statusIcons,
                battery = battery,
                carrier = carrier,
                onEvent = onEvent,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                onProjectionReadinessChanged = onProjectionReadinessChanged,
                initialSceneEligible = sceneEligible,
            )
        current = session
        session.start()
        return session.prepareNativePresentation(reused = false)
    }

    @Synchronized
    fun beginVisibleCycle(): Boolean =
        SysUiPresentationOwner.onCcVisibilityChanged(true)

    @Synchronized
    fun setRequestedVisible(visible: Boolean): Boolean =
        current?.setRequestedVisible(visible) ?: false

    @Synchronized
    fun setSceneEligible(eligible: Boolean) {
        sceneEligible = eligible
        val session = current
        session?.setSceneEligible(eligible)
        if (!eligible) {
            SysUiPresentationOwner.deactivateCc("scene-ineligible")
        } else {
            session?.prepareNativePresentation(reused = true)
        }
    }

    @Synchronized
    fun hotReloadHost(): ViewGroup? =
        current?.attachedHost()
            ?: pendingPrearm?.host()?.takeIf { candidate -> candidate.isAttachedToWindow }

    @Synchronized
    fun nativeReadyForHotReload(): Boolean =
        current?.nativeReadyForHotReload() == true

    @Synchronized
    fun restoreAfterHotReload(
        host: ViewGroup,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onProjectionReadinessChanged: (Boolean) -> Unit,
        transferredCompactReady: Boolean = false,
    ): AttachFailure? {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return AttachFailure("main-thread-required")
        }
        if (
            !canRestoreAfterReload(
                attached = host.isAttachedToWindow,
                inLayout = host.isInLayout,
                width = host.width,
                height = host.height,
            )
        ) {
            return AttachFailure("fake-root-hot-reload-layout-unavailable")
        }

        pendingPrearm?.cancel()
        pendingPrearm = null
        val result =
            attach(
                host = host,
                onEvent = onEvent,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                onProjectionReadinessChanged = onProjectionReadinessChanged,
            )
        if (
            result == null &&
            transferredCompactReady
        ) {
            when (SysUiPresentationOwner.adoptCcLayoutAfterHotReload()) {
                is SysUiPresentationOwner.Result.Active -> {
                    if (isDetailedDiagnosticsEnabled()) {
                        onEvent(
                            "controlCenterProjection hotReloadRestore state=adopted-compact " +
                                "source=transferred-laid-out-fake-root " +
                                "next=native-status-icons-layout-refresh",
                        )
                    }
                    return null
                }

                else -> Unit
            }
        }
        if (result == null && isDetailedDiagnosticsEnabled()) {
            onEvent(
                "controlCenterProjection hotReloadRestore state=prepared " +
                    "source=transferred-laid-out-fake-root " +
                    "transferredCompactReady=" + transferredCompactReady +
                    " layoutRequestBoundary=outside-native-layout " +
                    "next=native-status-icons-layout",
            )
        }
        return result
    }

    internal fun canRestoreAfterReload(
        attached: Boolean,
        inLayout: Boolean,
        width: Int,
        height: Int,
    ): Boolean =
        attached && !inLayout && width > 0 && height > 0

    @Synchronized
    fun onState(snapshot: StatusStateStore.Snapshot) {
        current?.update(snapshot)
    }

    @Synchronized
    fun onPresentationStateChanged() {
        current?.refresh()
    }

    @Synchronized
    fun onTintUpdate(update: SysUiTintSource.TintUpdate) {
        current?.updateTint(update)
    }

    @Synchronized
    fun transitionSourceSnapshot(): TransitionSourceSnapshot? =
        current?.transitionSourceSnapshot()

    @Synchronized
    fun onFeatureCfgChanged(cfg: FeatureCfg) {
        val session = current
        session?.setFeatureEnabled(cfg.enabled)
        if (!cfg.enabled || !sceneEligible) {
            SysUiPresentationOwner.deactivateCc(
                if (!cfg.enabled) "feature-disabled" else "scene-ineligible",
            )
        } else {
            session?.prepareNativePresentation(reused = true)
        }
    }

    @Synchronized
    fun onVisualCfgChanged(visual: VisualCfg) {
        current?.updateVisualCfg(visual)
    }

    @Synchronized
    fun detach(
        source: String = "detach",
        releaseNativePresentation: Boolean = true,
    ) {
        pendingPrearm?.cancel()
        pendingPrearm = null
        if (releaseNativePresentation) {
            SysUiPresentationOwner.deactivateCc(source)
        }
        current?.stop(source)
        current = null
    }

    @Synchronized
    private fun onPendingPrearmLayout(pending: PendingPrearm) {
        if (pendingPrearm !== pending) return
        val host = pending.host()
        if (host == null || !host.isAttachedToWindow) {
            pending.cancel()
            pendingPrearm = null
            return
        }

        val attempt = pending.nextAttempt()
        val failure =
            attach(
                host = host,
                onEvent = pending.onEvent,
                isDetailedDiagnosticsEnabled = pending.isDetailedDiagnosticsEnabled,
                onProjectionReadinessChanged = pending.onProjectionReadinessChanged,
            )
        if (failure == null) {
            pending.cancel()
            pendingPrearm = null
            pending.emit(
                "controlCenterProjection prearm state=armed " +
                    "source=fake-root-first-layout attempt=" + attempt,
            )
            return
        }

        val retry =
            failure.retryAfterLayout &&
                attempt < MAX_PREARM_LAYOUT_ATTEMPTS
        if (retry) {
            pending.emit(
                "controlCenterProjection prearm state=deferred " +
                    "source=fake-root-first-layout attempt=" + attempt +
                    " reason=" + failure.reason +
                    " next=native-root-layout",
            )
            host.requestLayout()
        } else {
            pending.cancel()
            pendingPrearm = null
            pending.emit(
                "controlCenterProjection prearm state=failed " +
                    "source=fake-root-first-layout attempt=" + attempt +
                    " reason=" + failure.reason +
                    " fallback=native-qs-fake",
            )
        }
    }

    @Synchronized
    private fun onPendingPrearmDetached(pending: PendingPrearm) {
        if (pendingPrearm !== pending) return
        pending.cancel()
        pendingPrearm = null
    }

    private class PendingPrearm(
        host: ViewGroup,
        val onEvent: (String) -> Unit,
        val isDetailedDiagnosticsEnabled: () -> Boolean,
        val onProjectionReadinessChanged: (Boolean) -> Unit,
    ) : View.OnLayoutChangeListener,
        View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private var started = false
        private var attempts = 0

        fun host(): ViewGroup? = host.get()

        fun nextAttempt(): Int {
            attempts += 1
            return attempts
        }

        fun matches(candidate: ViewGroup): Boolean =
            host.get() === candidate

        fun start() {
            if (started) return
            started = true
            val root = host.get() ?: return
            root.addOnLayoutChangeListener(this)
            root.addOnAttachStateChangeListener(this)
            // Cold start will normally hit the initial native layout naturally.
            // Hot Reload may restore an already-laid-out root, so request exactly
            // one native layout cycle through the same lifecycle boundary.
            root.requestLayout()
            emit(
                "controlCenterProjection prearm state=scheduled " +
                    "source=fake-root-attached next=native-root-layout",
            )
        }

        fun cancel() {
            if (!started) return
            started = false
            host.get()?.let { root ->
                root.removeOnLayoutChangeListener(this)
                root.removeOnAttachStateChangeListener(this)
            }
        }

        fun emit(message: String) {
            if (isDetailedDiagnosticsEnabled()) onEvent(message)
        }

        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int,
        ) {
            CcRenderSession.onPendingPrearmLayout(this)
        }

        override fun onViewAttachedToWindow(view: View) = Unit

        override fun onViewDetachedFromWindow(view: View) {
            CcRenderSession.onPendingPrearmDetached(this)
        }
    }

    private fun geometrySummary(view: View?): String {
        if (view == null) return "none"
        val location = IntArray(2)
        val hasLocation =
            runCatching {
                view.getLocationOnScreen(location)
                true
            }.getOrDefault(false)
        return view.javaClass.simpleName +
            "(v=" + view.visibility +
            ",a=" + view.alpha +
            ",l=" + view.left +
            ",t=" + view.top +
            ",w=" + view.width +
            ",h=" + view.height +
            ",tx=" + view.translationX +
            ",ty=" + view.translationY +
            ",sx=" + (if (hasLocation) location[0] else "na") +
            ",sy=" + (if (hasLocation) location[1] else "na") +
            ",parent=" + (view.parent?.javaClass?.simpleName ?: "none") +
            ")"
    }

    internal fun resolveProjectionReady(
        featureEnabled: Boolean,
        sceneEligible: Boolean,
        modelReady: Boolean,
        tintReady: Boolean,
        layoutReady: Boolean,
        hostAttached: Boolean,
        nativePresentationReady: Boolean,
    ): Boolean =
        featureEnabled &&
            sceneEligible &&
            modelReady &&
            tintReady &&
            layoutReady &&
            hostAttached &&
            nativePresentationReady

    private class Session(
        host: ViewGroup,
        statusBarArea: ViewGroup,
        statusIcons: ViewGroup,
        battery: ViewGroup,
        carrier: View,
        private val onEvent: (String) -> Unit,
        private val isDetailedDiagnosticsEnabled: () -> Boolean,
        private val onProjectionReadinessChanged: (Boolean) -> Unit,
        initialSceneEligible: Boolean,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val statusBarArea = WeakReference(statusBarArea)
        private val statusIcons = WeakReference(statusIcons)
        private val battery = WeakReference(battery)
        private val carrier = WeakReference(carrier)
        private val renderView = RenderView(host.context)
        private val renderController = RenderController(renderView)
        private val anchorRect = Rect()
        private val hostLocationScratch = IntArray(2)
        private val statusAreaLocationScratch = IntArray(2)

        private var currentModel: RenderModel? = null
        private var currentTint: TintState? = null
        private var visual = VisualPrefsOwner.current()
        private var transitionStateVersion = 0L
        private var cachedTransitionSourceSnapshot: TransitionSourceSnapshot? = null
        private var cachedTransitionVersion = Long.MIN_VALUE

        private var requestedVisible = false
        private var featureEnabled = FeaturePrefsOwner.current().enabled
        private var sceneEligible = initialSceneEligible
        private var modelReady = false
        private var tintReady = false
        private var layoutReady = false
        private var nativePresentationReady = false
        private var lastProjectionReady: Boolean? = null

        private val hostLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProjection()
            }
        private val statusAreaLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProjection()
            }
        private val carrierLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProjection()
            }

        fun matches(
            host: ViewGroup,
            statusBarArea: ViewGroup,
            statusIcons: ViewGroup,
            battery: ViewGroup,
            carrier: View,
        ): Boolean =
            this.host.get() === host &&
                this.statusBarArea.get() === statusBarArea &&
                this.statusIcons.get() === statusIcons &&
                this.battery.get() === battery &&
                this.carrier.get() === carrier

        fun attachedHost(): ViewGroup? =
            host.get()?.takeIf { candidate -> candidate.isAttachedToWindow }

        fun nativeReadyForHotReload(): Boolean =
            nativePresentationReady && attachedHost() != null

        fun transitionSourceSnapshot(): TransitionSourceSnapshot? {
            if (!projectionReady()) return null
            cachedTransitionSourceSnapshot
                ?.takeIf { cachedTransitionVersion == transitionStateVersion }
                ?.let { return it }

            val anchorView = carrier.get() ?: return null
            val model = currentModel ?: return null
            val tint = currentTint ?: return null
            return TransitionSourceSnapshot(
                view = renderView,
                anchorView = anchorView,
                model = model,
                colors =
                    ColorPolicy.resolve(
                        model = model,
                        tintState = tint,
                        visualSettings = visual,
                    ),
                visual = visual,
                stateVersion = transitionStateVersion,
            ).also { snapshot ->
                cachedTransitionSourceSnapshot = snapshot
                cachedTransitionVersion = transitionStateVersion
            }
        }

        fun geometryDiagnostic(): String =
            "projection={" +
                "root=" + geometrySummary(host.get()) +
                ",area=" + geometrySummary(statusBarArea.get()) +
                ",statusIcons=" + geometrySummary(statusIcons.get()) +
                ",battery=" + geometrySummary(battery.get()) +
                ",carrier=" + geometrySummary(carrier.get()) +
                ",render=" + geometrySummary(renderView) +
                "}"

        fun start() {
            val hostView = host.get() ?: return
            hostView.addOnAttachStateChangeListener(this)
            hostView.addOnLayoutChangeListener(hostLayoutListener)
            statusBarArea.get()?.addOnLayoutChangeListener(statusAreaLayoutListener)
            carrier.get()?.addOnLayoutChangeListener(carrierLayoutListener)
            renderView.visibility = View.GONE
            hostView.overlay.add(renderView)
            renderController.updateVisualCfg(visual)
            update(StatusStateStore.snapshot())
            refreshTint()
            layoutProjection()
            dispatchReadiness("start")
        }

        fun stop(source: String) {
            val hostView = host.get()
            hostView?.removeOnAttachStateChangeListener(this)
            hostView?.removeOnLayoutChangeListener(hostLayoutListener)
            statusBarArea.get()?.removeOnLayoutChangeListener(statusAreaLayoutListener)
            carrier.get()?.removeOnLayoutChangeListener(carrierLayoutListener)
            hostView?.overlay?.remove(renderView)
            requestedVisible = false
            layoutReady = false
            nativePresentationReady = false
            if (lastProjectionReady == true) {
                lastProjectionReady = false
                onProjectionReadinessChanged(false)
            }
            emitEvent {
                "controlCenterProjection cleanup source=" + source
            }
        }

        fun setRequestedVisible(visible: Boolean): Boolean {
            if (
                !requestedVisible &&
                visible &&
                !SysUiPresentationOwner.onCcVisibilityChanged(true)
            ) {
                syncPresentation("visibility-visible-cycle-failed")
                return false
            }
            if (requestedVisible && !visible) {
                SysUiPresentationOwner.onCcVisibilityChanged(false)
            }
            requestedVisible = visible
            syncPresentation("visibility")
            return projectionReady()
        }

        fun prepareNativePresentation(reused: Boolean): AttachFailure? {
            if (!featureEnabled || !sceneEligible) {
                nativePresentationReady = false
                syncPresentation(
                    if (!featureEnabled) "feature-ineligible" else "scene-ineligible",
                )
                return null
            }
            val statusArea =
                statusBarArea.get()
                    ?: return AttachFailure("fake-status-bar-area-released")
            val statusIconGroup =
                statusIcons.get()
                    ?: return AttachFailure("status-icons-released")
            val batteryView =
                battery.get()
                    ?: return AttachFailure("battery-view-released")
            val carrierView =
                carrier.get()
                    ?: return AttachFailure("battery-core-carrier-released")

            return when (
                val result =
                    SysUiPresentationOwner.activateCc(
                        host = statusArea,
                        statusIcons = statusIconGroup,
                        batteryContainer = statusArea,
                        battery = batteryView,
                        batteryCarrier = carrierView,
                        onEvent = onEvent,
                        isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                        onFailNative = { reason ->
                            setNativePresentationReady(
                                ready = false,
                                maskedViews = 0,
                                source = "fail-native:" + reason,
                            )
                        },
                        onReady = { active ->
                            setNativePresentationReady(
                                ready = true,
                                maskedViews = active.maskedViews,
                                source = "native-layout",
                            )
                        },
                    )
            ) {
                is SysUiPresentationOwner.Result.Active -> {
                    setNativePresentationReady(
                        ready = true,
                        maskedViews = result.maskedViews,
                        source = if (reused) "prearm-reuse" else "prearm-activation",
                    )
                    null
                }

                is SysUiPresentationOwner.Result.Prepared -> {
                    emitEvent {
                        "controlCenterProjection prearm state=prepared " +
                            "reused=" + reused +
                            " requestedVisible=" + requestedVisible
                    }
                    null
                }

                is SysUiPresentationOwner.Result.Failure -> {
                    setNativePresentationReady(
                        ready = false,
                        maskedViews = 0,
                        source = "prepare-failed:" + result.reason,
                    )
                    AttachFailure(
                        reason = result.reason,
                        retryAfterLayout = result.retryAfterLayout,
                    )
                }

                is SysUiPresentationOwner.Result.Inactive -> {
                    setNativePresentationReady(
                        ready = false,
                        maskedViews = 0,
                        source = "prepare-inactive",
                    )
                    AttachFailure("compact-presentation-inactive")
                }
            }
        }

        fun setNativePresentationReady(
            ready: Boolean,
            maskedViews: Int,
            source: String,
        ) {
            nativePresentationReady = ready
            if (ready) {
                layoutProjection()
            }
            applyVisibility()
            emitEvent {
                "controlCenterProjection compact ready=" + ready +
                    " source=" + source +
                    " maskedViews=" + maskedViews
            }
            dispatchReadiness("compact:" + source)
        }

        fun update(snapshot: StatusStateStore.Snapshot) {
            val result = renderController.update(snapshot)
            if (currentModel != result.model) {
                currentModel = result.model
                transitionStateVersion += 1
            }
            modelReady = result.model != null
            refreshTint()
            syncPresentation("state")
        }

        fun refresh() =
            update(StatusStateStore.snapshot())

        fun updateTint(update: SysUiTintSource.TintUpdate) {
            val batteryView = battery.get() ?: return
            if (update.sourceView !== batteryView) return
            applyTint(update.state, "battery")
        }

        fun setSceneEligible(eligible: Boolean) {
            sceneEligible = eligible
            if (!eligible) {
                nativePresentationReady = false
            }
            syncPresentation("scene")
        }

        fun setFeatureEnabled(enabled: Boolean) {
            featureEnabled = enabled
            if (!enabled) {
                nativePresentationReady = false
            }
            syncPresentation("feature")
        }

        fun updateVisualCfg(next: VisualCfg) {
            if (visual != next) {
                visual = next
                transitionStateVersion += 1
            }
            renderController.updateVisualCfg(next)
        }

        private fun refreshTint() {
            val batteryView = battery.get() ?: return
            val state = SysUiTintSource.currentState(batteryView) ?: return
            applyTint(state, "surface")
        }

        private fun applyTint(
            batteryState: TintState,
            source: String,
        ) {
            val peerTint =
                statusIcons.get()?.let(
                    NativeNetworkSuppressor::currentStatusIconTint,
                )
            val resolved =
                TintAuthority.resolveBatteryEvent(
                    batteryState,
                    peerTint,
                )
            val tintUpdate = renderController.updateTint(resolved)
            if (currentTint != tintUpdate.resolved) {
                currentTint = tintUpdate.resolved
                transitionStateVersion += 1
            }
            tintReady = tintUpdate.resolved != null
            syncPresentation("tint:" + source)
        }

        private fun layoutProjection() {
            val hostView = host.get() ?: return markLayoutUnavailable()
            val statusArea = statusBarArea.get() ?: return markLayoutUnavailable()
            val carrierView = carrier.get() ?: return markLayoutUnavailable()
            val carrierWidth =
                SysUiCarrierMetrics.resolveWidthPx(carrierView)
                    ?: return markLayoutUnavailable()
            val resolved =
                HomeLayoutResolver.resolve(
                    hostWidthPx = statusArea.width,
                    hostHeightPx = statusArea.height,
                    baseCarrierWidthPx = carrierWidth,
                    isRtl = statusArea.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                ) ?: return markLayoutUnavailable()
            if (!resolved.renderCombined) return markLayoutUnavailable()

            hostView.getLocationInWindow(hostLocationScratch)
            statusArea.getLocationInWindow(statusAreaLocationScratch)
            val offsetX = statusAreaLocationScratch[0] - hostLocationScratch[0]
            val offsetY = statusAreaLocationScratch[1] - hostLocationScratch[1]
            val left = offsetX + resolved.slotLeftPx.toInt()
            val right = offsetX + resolved.slotRightPx.toInt()
            val top = offsetY
            val bottom = offsetY + statusArea.height
            anchorRect.set(left, top, right, bottom)
            if (anchorRect.width() <= 0 || anchorRect.height() <= 0) {
                return markLayoutUnavailable()
            }

            if (
                renderView.measuredWidth != anchorRect.width() ||
                renderView.measuredHeight != anchorRect.height()
            ) {
                renderView.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        anchorRect.width(),
                        View.MeasureSpec.EXACTLY,
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                        anchorRect.height(),
                        View.MeasureSpec.EXACTLY,
                    ),
                )
            }
            renderView.layout(
                anchorRect.left,
                anchorRect.top,
                anchorRect.right,
                anchorRect.bottom,
            )

            val firstReady = !layoutReady
            layoutReady = true
            syncPresentation("layout")
            if (firstReady) {
                emitEvent {
                    "controlCenterProjection attached carrier=ControlCenterFakeStatusIcons.overlay " +
                        "geometrySource=MiuiStatusBatteryContainer bounds=" +
                        anchorRect.left + "," + anchorRect.top + "-" +
                        anchorRect.right + "," + anchorRect.bottom +
                        " target=stable-battery-slot motion=root-alpha-translation-inherited"
                }
            }
        }

        private fun markLayoutUnavailable() {
            if (!layoutReady) return
            layoutReady = false
            renderView.visibility = View.GONE

            val hostAttached = host.get()?.isAttachedToWindow == true
            val retainNativePresentation =
                hostAttached && nativePresentationReady
            if (!retainNativePresentation) {
                nativePresentationReady = false
                SysUiPresentationOwner.deactivateCc(
                    "projection-layout-unavailable-detached",
                )
            }
            emitEvent {
                "controlCenterProjection layoutUnavailable action=pause-render " +
                    "compactPresentationRetained=" + retainNativePresentation +
                    " hostAttached=" + hostAttached
            }
            dispatchReadiness("layout-unavailable")
        }

        private fun projectionReady(): Boolean =
            resolveProjectionReady(
                featureEnabled = featureEnabled,
                sceneEligible = sceneEligible,
                modelReady = modelReady,
                tintReady = tintReady,
                layoutReady = layoutReady,
                hostAttached = host.get()?.isAttachedToWindow == true,
                nativePresentationReady = nativePresentationReady,
            )

        private fun syncPresentation(source: String) {
            applyVisibility()
            dispatchReadiness(source)
        }

        private fun applyVisibility() {
            val visible = requestedVisible && projectionReady()
            renderView.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                renderView.invalidate()
            } else {
                renderView.clearPendingLatency()
            }
        }

        private fun dispatchReadiness(source: String) {
            val ready = requestedVisible && projectionReady()
            if (ready == lastProjectionReady) return
            lastProjectionReady = ready
            emitEvent {
                "controlCenterProjection readiness source=" + source +
                    " ready=" + ready +
                    " requestedVisible=" + requestedVisible +
                    " sceneEligible=" + sceneEligible +
                    " modelReady=" + modelReady +
                    " tintReady=" + tintReady +
                    " layoutReady=" + layoutReady +
                    " nativePresentationReady=" + nativePresentationReady
            }
            onProjectionReadinessChanged(ready)
        }

        override fun onViewAttachedToWindow(view: View) {
            layoutProjection()
            refreshTint()
            syncPresentation("attach")
        }

        override fun onViewDetachedFromWindow(view: View) {
            requestedVisible = false
            layoutReady = false
            nativePresentationReady = false
            renderView.visibility = View.GONE
            SysUiPresentationOwner.deactivateCc(
                "fake-root-detached",
            )
            dispatchReadiness("detach")
        }

        private inline fun emitEvent(message: () -> String) {
            if (isDetailedDiagnosticsEnabled()) onEvent(message())
        }
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) return child
        }
        return null
    }

    private fun ViewGroup.uniqueDescendant(className: String): ViewGroup? {
        var found: ViewGroup? = null
        val queue = ArrayDeque<ViewGroup>()
        queue.add(this)
        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            for (index in 0 until parent.childCount) {
                val child = parent.getChildAt(index)
                if (child is ViewGroup) {
                    if (child.javaClass.name == className) {
                        if (found != null && found !== child) return null
                        found = child
                    }
                    queue.add(child)
                }
            }
        }
        return found
    }

    internal data class TransitionSourceSnapshot(
        val view: View,
        val anchorView: View,
        val model: RenderModel,
        val colors: RenderColors,
        val visual: VisualCfg,
        val stateVersion: Long,
    )

    internal sealed interface PrearmResult {
        data class Scheduled(
            val reused: Boolean,
        ) : PrearmResult

        data class Failure(
            val reason: String,
        ) : PrearmResult
    }

}
