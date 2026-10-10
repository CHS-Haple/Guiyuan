package com.chaners.guiyuan.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.xposed.prefs.FeaturePrefsOwner
import com.chaners.guiyuan.xposed.prefs.VisualPrefsOwner
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SysUiCarrierMetrics
import java.lang.ref.WeakReference

internal object KeyguardRenderSession {
    private var current: Session? = null

    @Synchronized
    fun attach(
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
        sceneEligible: Boolean,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean = { true },
        onPresentationReadinessChanged: ((Boolean) -> Unit)? = null,
    ): String? =
        attachFamily(
            resolved = resolved,
            scene = Scene.KEYGUARD,
            sceneEligible = sceneEligible,
            onEvent = onEvent,
            isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
            onPresentationReadinessChanged = onPresentationReadinessChanged,
        )

    @Synchronized
    fun attachAod(
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
        sceneEligible: Boolean,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean = { true },
        onPresentationReadinessChanged: ((Boolean) -> Unit)? = null,
    ): String? =
        attachFamily(
            resolved = resolved,
            scene = Scene.AOD,
            sceneEligible = sceneEligible,
            onEvent = onEvent,
            isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
            onPresentationReadinessChanged = onPresentationReadinessChanged,
        )

    private fun attachFamily(
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
        scene: Scene,
        sceneEligible: Boolean,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onPresentationReadinessChanged: ((Boolean) -> Unit)?,
    ): String? {
        val settings = FeaturePrefsOwner.current()
        if (!sceneEligible) {
            return if (scene == Scene.AOD) "aod-not-active" else "keyguard-not-active"
        }
        val featureEnabled =
            resolveFamilyFeatureEnabled(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguard,
                aodEnabled = settings.aod,
                sceneIsAod = scene == Scene.AOD,
            )

        val existing = current
        if (existing?.matches(resolved) == true) {
            existing.retarget(
                scene = scene,
                featureEnabled = featureEnabled,
                sceneEligible = sceneEligible,
                onPresentationReadinessChanged = onPresentationReadinessChanged,
            )
            existing.update(StatusStateStore.snapshot())
            return null
        }

        existing?.stop()
        val session =
            Session(
                resolved = resolved,
                onEvent = onEvent,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                scene = scene,
                initialFeatureEnabled = featureEnabled,
                initialSceneEligible = sceneEligible,
                onPresentationReadinessChanged = onPresentationReadinessChanged,
            )
        current = session
        session.start()
        session.update(StatusStateStore.snapshot())
        return null
    }

    @Synchronized
    fun onState(snapshot: StatusStateStore.Snapshot) {
        current?.update(snapshot)
    }

    @Synchronized
    fun onPresentationStateChanged() {
        current?.update(StatusStateStore.snapshot())
    }

    @Synchronized
    fun onTintUpdate(update: SysUiTintSource.TintUpdate) {
        current?.updateTint(update)
    }

    @Synchronized
    fun onFeatureCfgChanged(cfg: FeatureCfg) {
        current?.setFeatureCfg(cfg)
    }

    @Synchronized
    fun onVisualCfgChanged(visual: VisualCfg) {
        current?.updateVisualCfg(visual)
    }

    @Synchronized
    fun onAodState(update: SysUiKeyguardAodSource.AodUpdate) {
        current?.updateAodState(update)
    }

    @Synchronized
    fun setNativeHandoffActive(active: Boolean) {
        current
            ?.takeIf { session -> session.isScene(Scene.KEYGUARD) }
            ?.setNativeHandoffActive(active)
    }

    @Synchronized
    fun setAodNativeHandoffActive(active: Boolean) {
        current
            ?.takeIf { session -> session.isScene(Scene.AOD) }
            ?.setNativeHandoffActive(active)
    }

    @Synchronized
    fun visualState(): String? = current?.visualState()

    @Synchronized
    fun currentTransitionSourceWitness(): TransitionSourceWitness? =
        current?.transitionSourceWitness()

    @Synchronized
    fun detach() {
        val session = current ?: return
        if (!session.isScene(Scene.KEYGUARD)) return
        current = null
        session.stop()
    }

    @Synchronized
    fun detachAod() {
        val session = current ?: return
        if (!session.isScene(Scene.AOD)) return
        current = null
        session.stop()
    }

    internal fun resolveFamilyFeatureEnabled(
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        sceneIsAod: Boolean,
    ): Boolean =
        featureEnabled && if (sceneIsAod) aodEnabled else keyguardEnabled

    private fun readViewField(
        owner: Any,
        name: String,
    ): View? {
        var type: Class<*>? = owner.javaClass
        while (type != null) {
            val current = type
            val field =
                runCatching {
                    current.getDeclaredField(name).apply { isAccessible = true }
                }.getOrNull()
            if (field != null) {
                return runCatching { field.get(owner) as? View }.getOrNull()
            }
            type = current.superclass
        }
        return null
    }

    private fun visualChainSummary(view: View?): String {
        if (view == null) return "none"
        var current: View? = view
        var effectiveAlpha = 1f
        var allVisible = true
        val chain = ArrayList<String>(6)
        var depth = 0
        while (current != null && depth < 8) {
            effectiveAlpha *= current.alpha
            allVisible = allVisible && current.visibility == View.VISIBLE
            chain +=
                current.javaClass.simpleName +
                    "(v=" + current.visibility +
                    ",a=" + current.alpha +
                    ",shown=" + current.isShown + ")"
            current = current.parent as? View
            depth += 1
        }
        return "effectiveAlpha=" + effectiveAlpha +
            ",allVisible=" + allVisible +
            ",leafShown=" + view.isShown +
            ",chain=" + chain.joinToString(">")
    }

    private fun resolveSceneOverlayVisible(
        featureEnabled: Boolean,
        nativeHandoffActive: Boolean,
        sceneEligible: Boolean,
    ): Boolean =
        featureEnabled && !nativeHandoffActive && sceneEligible

    internal fun resolveOverlayVisible(
        featureEnabled: Boolean,
        nativeHandoffActive: Boolean,
        aodBlocked: Boolean,
    ): Boolean =
        resolveSceneOverlayVisible(
            featureEnabled = featureEnabled,
            nativeHandoffActive = nativeHandoffActive,
            sceneEligible = !aodBlocked,
        )

    private fun resolveSceneOwnerReady(
        featureEnabled: Boolean,
        modelReady: Boolean,
        tintReady: Boolean,
        layoutReady: Boolean,
        hostAttached: Boolean,
        sceneEligible: Boolean,
    ): Boolean =
        featureEnabled &&
            modelReady &&
            tintReady &&
            layoutReady &&
            hostAttached &&
            sceneEligible

    internal fun resolveOwnerReady(
        featureEnabled: Boolean,
        modelReady: Boolean,
        tintReady: Boolean,
        layoutReady: Boolean,
        hostAttached: Boolean,
        aodBlocked: Boolean,
    ): Boolean =
        resolveSceneOwnerReady(
            featureEnabled = featureEnabled,
            modelReady = modelReady,
            tintReady = tintReady,
            layoutReady = layoutReady,
            hostAttached = hostAttached,
            sceneEligible = !aodBlocked,
        )

    private enum class Scene(
        val logPrefix: String,
        val aodOwned: Boolean,
    ) {
        KEYGUARD("keyguardRender", false),
        AOD("aodRender", true),
    }

    private enum class AodDraw {
        IDLE,
        WAITING,
        DRAWN,
    }

    private class Session(
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
        private val onEvent: (String) -> Unit,
        private val isDetailedDiagnosticsEnabled: () -> Boolean,
        private var scene: Scene,
        initialFeatureEnabled: Boolean,
        initialSceneEligible: Boolean,
        private var onPresentationReadinessChanged: ((Boolean) -> Unit)?,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(resolved.host)
        private val systemIcons = WeakReference(resolved.systemIcons)
        private val statusIcons = WeakReference(resolved.statusIcons)
        private val batteryView = WeakReference(resolved.battery)
        private val batteryCarrier = WeakReference(resolved.batteryCarrier)
        private var aodDraw = AodDraw.IDLE
        private var aodShown = false
        private val renderView =
            RenderView(
                resolved.host.context,
                onShownChanged = ::observeAodVisibility,
                onDrawn = { view ->
                    observeAodVisibility(view)
                    if (aodDraw == AodDraw.WAITING && view.isShown) {
                        aodDraw = AodDraw.DRAWN
                        emitEvent {
                            "keyguardAodWitness source=renderView.onDraw" +
                                " shown=true statusIconsAlpha=" +
                                statusIcons.get()?.alpha +
                                " hostVisual={" + visualChainSummary(host.get()) + "}" +
                                " nativeCc=" + SysUiCcSource.nativeVisualState()
                        }
                    }
                },
            )
        private val renderController = RenderController(renderView)
        private val anchorRect = Rect()

        private var featureEnabled = initialFeatureEnabled
        private var nativeHandoffActive = true
        private var sceneEligible = initialSceneEligible
        private var modelReady = false
        private var tintReady = false
        private var layoutReady = false
        private var layoutLogged = false
        private var readyLogged = false
        private var rejectedTintLogged = false
        private var lastPresentationReady = false

        private val overlayHostLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProbe()
            }
        private val carrierLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProbe()
            }

        fun matches(resolved: SysUiKeyguardHostResolver.ResolvedHost): Boolean =
            host.get() === resolved.host &&
                systemIcons.get() === resolved.systemIcons &&
                statusIcons.get() === resolved.statusIcons &&
                batteryView.get() === resolved.battery &&
                batteryCarrier.get() === resolved.batteryCarrier

        fun isScene(candidate: Scene): Boolean = scene == candidate

        fun visualState(): String =
            scene.name.lowercase() +
                ":childShown=" + renderView.isShown +
                ":childAlpha=" + renderView.alpha +
                ":hostShown=" + (host.get()?.isShown ?: "unavailable")

        fun retarget(
            scene: Scene,
            featureEnabled: Boolean,
            sceneEligible: Boolean,
            onPresentationReadinessChanged: ((Boolean) -> Unit)?,
        ) {
            val changedScene = this.scene != scene
            this.scene = scene
            this.featureEnabled = featureEnabled
            this.sceneEligible = sceneEligible
            this.onPresentationReadinessChanged = onPresentationReadinessChanged
            if (changedScene) {
                readyLogged = false
                rejectedTintLogged = false
            }
            applyResolvedVisibility()
            dispatchPresentationReadiness(
                source = "scene-transfer",
                force = changedScene,
            )
        }

        fun transitionSourceWitness(): TransitionSourceWitness? {
            if (scene != Scene.KEYGUARD) return null
            val motion = statusIcons.get() ?: return null
            val render = renderView
            if (
                !ScenePolicy.retainedTransitionSourceWitnessAvailable(
                    widthPx = render.width,
                    heightPx = render.height,
                    hostAttached =
                        systemIcons.get()?.isAttachedToWindow == true &&
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
                positionHost = systemIcons.get() ?: return null,
                motionCarrier = motion,
                representedSlots =
                    SysUiPresentationOwner.keyguardSlots(),
            )
        }

        fun start() {
            val overlayHost = systemIcons.get() ?: return
            val battery = batteryView.get() ?: return
            val carrier = batteryCarrier.get() ?: return

            overlayHost.addOnAttachStateChangeListener(this)
            overlayHost.addOnLayoutChangeListener(overlayHostLayoutListener)
            carrier.addOnLayoutChangeListener(carrierLayoutListener)
            renderView.visibility = View.GONE
            (renderView.parent as? ViewGroup)?.removeView(renderView)
            overlayHost.addView(
                renderView,
                ViewGroup.LayoutParams(0, 0),
            )
            renderController.updateVisualCfg(
                VisualPrefsOwner.current(),
            )
            SysUiTintSource.currentState(battery)?.let { state ->
                applyTintState(
                    TintAuthority.resolveBatteryEvent(
                        batteryState = state,
                        liveStatusIconTint = null,
                    ),
                    "seed",
                )
            }
            layoutProbe()
        }

        fun stop() {
            if (aodDraw != AodDraw.IDLE) {
                emitEvent {
                    "keyguardAodWitness source=session-stop" +
                        " shownDuringAod=" + aodShown +
                        " freshVisibleDraw=" + (aodDraw == AodDraw.DRAWN) +
                        " nativeCc=" + SysUiCcSource.nativeVisualState()
                }
                aodDraw = AodDraw.IDLE
            }
            layoutReady = false
            dispatchPresentationReadiness("stop")
            systemIcons.get()?.removeOnAttachStateChangeListener(this)
            systemIcons.get()?.removeOnLayoutChangeListener(overlayHostLayoutListener)
            batteryCarrier.get()?.removeOnLayoutChangeListener(carrierLayoutListener)
            (renderView.parent as? ViewGroup)?.removeView(renderView)
        }

        fun setFeatureCfg(cfg: FeatureCfg) {
            val enabled =
                resolveFamilyFeatureEnabled(
                    featureEnabled = cfg.enabled,
                    keyguardEnabled = cfg.keyguard,
                    aodEnabled = cfg.aod,
                    sceneIsAod = scene == Scene.AOD,
                )
            setFeatureState(enabled)
        }

        private fun setFeatureState(enabled: Boolean) {
            if (Looper.myLooper() !== Looper.getMainLooper()) {
                host.get()?.post { setFeatureState(enabled) }
                return
            }
            if (featureEnabled == enabled) return
            featureEnabled = enabled
            val visible = applyResolvedVisibility()
            emitEvent {
                scene.logPrefix + "Feature enabled=" + featureEnabled +
                    " overlayVisible=" + visible +
                    " nativeHandoffActive=" + nativeHandoffActive
            }
            dispatchPresentationReadiness("feature")
        }

        fun setNativeHandoffActive(active: Boolean) {
            if (nativeHandoffActive == active) return
            nativeHandoffActive = active
            val visible = applyResolvedVisibility()
            emitEvent {
                scene.logPrefix + "Handoff nativeActive=" + nativeHandoffActive +
                    " overlayVisible=" + visible
            }
        }

        fun updateVisualCfg(visual: VisualCfg) {
            renderController.updateVisualCfg(visual)
            layoutProbe()
        }

        fun updateAodState(update: SysUiKeyguardAodSource.AodUpdate) {
            val battery = batteryView.get() ?: return
            if (update.sourceView !== battery) return
            if (
                scene == Scene.KEYGUARD &&
                update.isAodAnimate &&
                !update.toAod &&
                aodDraw == AodDraw.IDLE &&
                isDetailedDiagnosticsEnabled()
            ) {
                aodDraw = AodDraw.WAITING
                observeAodVisibility(renderView)
            }
            val visible = applyResolvedVisibility()
            emitEvent {
                scene.logPrefix + "Aod source=" + update.source +
                    " toAod=" + update.toAod +
                    " isAodAnimate=" + update.isAodAnimate +
                    " animToAod=" + (update.animToAod ?: "unavailable") +
                    " sceneEligible=" + sceneEligible +
                    " eligibilityAuthority=scene-policy" +
                    " overlayVisible=" + visible +
                    " childAlpha=" + renderView.alpha +
                    " batteryAlphaReadOnly=" + battery.alpha +
                    " statusIconsAlphaReadOnly=" + (statusIcons.get()?.alpha ?: -1f) +
                    " systemIconsAlphaReadOnly=" + (systemIcons.get()?.alpha ?: -1f) +
                    " hostVisual={" + visualChainSummary(host.get()) + "}" +
                    " contentVisual={" +
                    visualChainSummary(
                        host.get()?.let { owner ->
                            readViewField(owner, "mKeyguardStatusBarContent")
                        },
                    ) + "}" +
                    " systemIconsVisual={" + visualChainSummary(systemIcons.get()) + "}"
            }
        }

        private fun observeAodVisibility(view: View) {
            if (aodDraw == AodDraw.IDLE || aodShown || !view.isShown) return
            aodShown = true
            emitEvent {
                "keyguardAodWitness source=renderView.visibility" +
                    " shown=true statusIconsAlpha=" +
                    statusIcons.get()?.alpha +
                    " hostVisual={" + visualChainSummary(host.get()) + "}" +
                    " nativeCc=" + SysUiCcSource.nativeVisualState()
            }
        }

        fun updateTint(update: SysUiTintSource.TintUpdate) {
            val battery = batteryView.get() ?: return
            if (update.sourceView !== battery) return
            applyTintState(
                TintAuthority.resolveBatteryEvent(
                    batteryState = update.state,
                    liveStatusIconTint = null,
                ),
                if (scene == Scene.AOD) "aod-battery" else "keyguard-battery",
            )
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
                    scene.logPrefix + "Tint deferred source=" + source +
                        " reason=transparent retainStable=true"
                }
            }
            if (update.changed) {
                val resolved = update.resolved
                if (resolved != null) {
                    emitEvent {
                        scene.logPrefix + "Tint source=" + source +
                            " applied=#" +
                            resolved.appliedTint.toUInt().toString(16).padStart(8, '0') +
                            " authority=keyguard-battery"
                    }
                }
            }
            dispatchPresentationReadiness("tint:" + source)
        }

        fun update(snapshot: StatusStateStore.Snapshot) {
            val update = renderController.update(snapshot)
            if (!update.candidateComplete) return
            val model = update.model
            if (model != null) {
                modelReady = true
                if (update.changed) {
                    layoutProbe()
                }
                if (!readyLogged) {
                    readyLogged = true
                    emitEvent {
                        scene.logPrefix + " ready battery=" + model.batteryPercent +
                            " charging=" + model.charging +
                            " center=" + model.centerIndicator.javaClass.simpleName
                    }
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
                renderView.requiredTopOverflowPx(
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
                    scene.logPrefix + " attached carrier=MiuiStatusBatteryContainer.child " +
                        "carrierAuthority=battery_icon_container " +
                        "motion=keyguard-system-icons-inherited " +
                        "bounds=" + anchorRect.left + "," + anchorRect.top + "-" +
                        anchorRect.right + "," + anchorRect.bottom +
                        " logicalSize=" + anchorRect.width() + "x" + anchorRect.height() +
                        " physicalSize=" + renderView.width + "x" + renderView.height +
                        " topOverflowPx=" + renderView.currentLogicalViewportTopInsetPx()
                }
            }
            dispatchPresentationReadiness("layout")
        }

        private fun resolveNativeAnchor(out: Rect): Boolean {
            val overlayHost = systemIcons.get() ?: return false
            val carrier = batteryCarrier.get() ?: return false
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

            val resolved =
                SteadyLayoutResolver.resolve(
                    hostWidthPx = hostWidth,
                    hostHeightPx = hostHeight,
                    baseCarrierWidthPx = baseCarrierWidth,
                    isRtl = overlayHost.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                ) ?: return false

            out.set(
                resolved.left,
                0,
                resolved.right,
                hostHeight,
            )
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
            renderView.setLogicalViewport(
                widthPx = bounds.width(),
                heightPx = bounds.height(),
                topInsetPx = physical.logicalTopInsetPx,
            )
            if (
                renderView.measuredWidth != bounds.width() ||
                renderView.measuredHeight != physical.physicalHeightPx
            ) {
                renderView.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        bounds.width(),
                        View.MeasureSpec.EXACTLY,
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                        physical.physicalHeightPx,
                        View.MeasureSpec.EXACTLY,
                    ),
                )
            }
            renderView.layout(
                bounds.left,
                physical.physicalTopPx,
                bounds.right,
                bounds.bottom,
            )
        }

        private fun applyResolvedVisibility(): Boolean {
            val visible =
                resolveSceneOverlayVisible(
                    featureEnabled = featureEnabled,
                    nativeHandoffActive = nativeHandoffActive,
                    sceneEligible = sceneEligible,
                )
            if (visible) {
                // The module child lives directly under the verified system-icons
                // family carrier. Do not copy Battery's independent AOD alpha
                // animation onto the whole combined visual: HyperOS animates
                // Battery and status icons as separate children, and Battery may
                // legitimately reach alpha=0 during a family scene transfer.
                renderView.alpha = 1f
            }
            renderView.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                renderView.invalidate()
            } else {
                renderView.clearPendingLatency()
            }
            return visible
        }

        private fun dispatchPresentationReadiness(
            source: String,
            force: Boolean = false,
        ) {
            val ready =
                resolveSceneOwnerReady(
                    featureEnabled = featureEnabled,
                    modelReady = modelReady,
                    tintReady = tintReady,
                    layoutReady = layoutReady,
                    hostAttached = systemIcons.get()?.isAttachedToWindow == true,
                    sceneEligible = sceneEligible,
                )
            if (!force && ready == lastPresentationReady) return
            lastPresentationReady = ready
            emitEvent {
                scene.logPrefix + "Readiness source=" + source +
                    " ownerReady=" + ready +
                    " modelReady=" + modelReady +
                    " tintReady=" + tintReady +
                    " layoutReady=" + layoutReady +
                    " featureEnabled=" + featureEnabled +
                    " sceneEligible=" + sceneEligible +
                    " aodOwned=" + scene.aodOwned
            }
            onPresentationReadinessChanged?.invoke(ready)
        }

        private inline fun emitEvent(message: () -> String) {
            if (isDetailedDiagnosticsEnabled()) {
                onEvent(message())
            }
        }
    }

}
