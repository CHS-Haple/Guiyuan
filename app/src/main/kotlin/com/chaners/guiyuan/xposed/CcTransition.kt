package com.chaners.guiyuan.xposed

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object CcTransition {
    // Transition drawing owns only Guiyuan geometry. HyperOS remains the
    // authority for panel progress, native tint, visibility and final layout.
    private const val STATUS_ICON_CONTAINER_CLASS =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val BATTERY_SLOT = "battery"
    private const val BATTERY_NUMBER_SLOT = "battery_number"
    private const val BATTERY_CHARGING_SLOT = "battery_charging"
    private const val AIRPLANE_SLOT = "airplane"
    private const val NO_SIM_SLOT = "no_sim"
    private const val MOBILE_SLOT = "mobile"
    private const val WIFI_SLOT = "wifi"
    private const val STACKED_MOBILE_SLOT = "stacked_mobile"
    private const val NUMBER_PROBE_MAX_VIEWS = 16
    private const val NUMBER_PROBE_MAX_DEPTH = 4
    private const val NUMBER_PROBE_MAX_PAINTS = 8
    private const val NUMBER_MIN_TEXT_SIZE_PX = 8f

    private var visible = false
    private var sceneEligible = false
    private var projectionReady = false
    private var nativeProgress: Float? = null
    private var nativeAppearance = false
    private var nativeAppearanceAnimated = false
    private var nativeBatteryIslandActive: Boolean? = null
    private var sourceScene = SourceScene.UNKNOWN
    private var endpoints: PanelTransitionSource.CcTransitionEndpoints? = null
    private var current: Session? = null
    private var lastBatteryProbeSummary: String? = null

    @Synchronized
    fun onPanelUpdate(update: PanelTransitionSource.Update) {
        update.visible?.let { nextVisible ->
            visible = nextVisible
            if (!nextVisible) {
                nativeProgress = null
                nativeAppearance = false
                nativeAppearanceAnimated = false
                nativeBatteryIslandActive = null
                sourceScene = SourceScene.UNKNOWN
                endpoints = null
            } else {
                nativeBatteryIslandActive = update.ccBatteryIslandActive
            }
        }
        update.fraction?.let {
            nativeProgress = it
            // Expansion is the per-sample authority for the native Battery-Island
            // contract. A failed read clears a stale prior value instead of
            // pretending the previous island mode still applies.
            nativeBatteryIslandActive = update.ccBatteryIslandActive
        }
        update.ccAppearance?.let { nativeAppearance = it }
        update.ccAppearanceAnimated?.let { nativeAppearanceAnimated = it }
        update.ccBatteryIslandActive?.let { nativeBatteryIslandActive = it }
        update.ccSourceScene?.let { sourceScene = it }
        update.ccEndpoints?.let { endpoints = it }
        sync("panel-update")
    }

    @Synchronized
    fun setSceneEligible(eligible: Boolean) {
        sceneEligible = eligible
        sync("scene")
    }

    @Synchronized
    fun onProjectionReadinessChanged(ready: Boolean) {
        projectionReady = ready
        sync("projection-readiness")
    }

    @Synchronized
    fun currentDiagnostic(): String =
        current?.diagnostic()
            ?: "transitionOwner=inactive appearance=" + nativeAppearance

    @Synchronized
    fun latestBatteryProbe(): String? =
        lastBatteryProbeSummary

    @Synchronized
    fun detach(source: String = "detach") {
        current?.stop(source)
        current = null
        visible = false
        projectionReady = false
        nativeProgress = null
        nativeAppearance = false
        nativeAppearanceAnimated = false
        nativeBatteryIslandActive = null
        sourceScene = SourceScene.UNKNOWN
        endpoints = null
    }

    private fun sync(source: String) {
        val progress = nativeProgress
        val endpoint = endpoints
        val shouldRun =
            visible &&
                sceneEligible &&
                projectionReady &&
                progress != null &&
                progress > 0f &&
                progress <= 1f &&
                endpoint != null &&
                endpoint.fakeRoot.isAttachedToWindow &&
                endpoint.finalRoot.isAttachedToWindow

        if (!shouldRun) {
            current?.stop("inactive:" + source)
            current = null
            return
        }

        val sourceSnapshot =
            CcSession.transitionSourceSnapshot()
                ?: run {
                    current?.stop("source-unavailable")
                    current = null
                    return
                }
        val root = endpoint.fakeRoot.rootView as? ViewGroup
            ?: run {
                current?.stop("root-view-unavailable")
                current = null
                return
            }
        if (endpoint.finalRoot.rootView !== root) {
            current?.stop("root-view-mismatch")
            current = null
            return
        }

        val steadySourceWitness =
            when (sourceScene) {
                SourceScene.HOME ->
                    HomeRenderSession.transitionSourceWitness()
                SourceScene.KEYGUARD ->
                    KeyguardRenderSession.transitionSourceWitness()
                SourceScene.UNKNOWN ->
                    null
            }

        val existing = current
        if (
            existing == null ||
            !existing.matches(
                root = root,
                fakeRoot = endpoint.fakeRoot,
                finalRoot = endpoint.finalRoot,
                sourceView = sourceSnapshot.view,
                sourceAnchor = sourceSnapshot.anchorView,
            )
        ) {
            existing?.stop("endpoints-changed")
            val created =
                Session.create(
                    root = root,
                    fakeRoot = endpoint.fakeRoot,
                    finalRoot = endpoint.finalRoot,
                    sourceSnapshot = sourceSnapshot,
                    steadySourceWitness = steadySourceWitness,
                    steadySourceLabel = sourceScene.name.lowercase(),
                ) ?: run {
                    current = null
                    return
                }
            current = created
            created.start()
        }

        current?.update(
            progress = progress,
            sourceSnapshot = sourceSnapshot,
            nativeAppearance = nativeAppearance,
            nativeAppearanceAnimated = nativeAppearanceAnimated,
            transitionReservationEnabled =
                CcTransition.usesSemanticReservation(
                    sourceScene = sourceScene,
                    charging = sourceSnapshot.model.charging,
                    nativeBatteryIslandActive = nativeBatteryIslandActive,
                ),
            sourceScene = sourceScene,
            genericIslandShowing = IslandMotionSource.currentIslandShowing(),
            nativeBatteryIslandActive = nativeBatteryIslandActive,
        )
    }

    // A session freezes only the source/target facts needed for one visible
    // cycle. Missing or ambiguous native witnesses fail back to native.
    private class Session(
        root: ViewGroup,
        fakeRoot: ViewGroup,
        finalRoot: ViewGroup,
        sourceView: View,
        sourceAnchor: View,
        sourceSnapshot: CcSession.TransitionSourceSnapshot,
        private val frozenSource: FrozenSourceGeometry?,
        private val fakeStatusIcons: ViewGroup,
        private val finalStatusIcons: ViewGroup,
        private val finalBattery: View,
    ) {
        private val rootRef = WeakReference(root)
        private val fakeRootRef = WeakReference(fakeRoot)
        private val finalRootRef = WeakReference(finalRoot)
        private val sourceViewRef = WeakReference(sourceView)
        private val sourceAnchorRef = WeakReference(sourceAnchor)
        private val painter = StatusPainter(root.context)
        private val drawable = TransitionDrawable(this)
        private val sourceMask = MaskState(
            view = WeakReference(sourceView),
            nativeClip = sourceView.clipBounds?.let(::Rect),
            appliedClip = Rect(0, 0, 0, 0),
        )
        private val mobileSubIdCache = WeakHashMap<View, Int?>()
        private val targetCache = HashMap<TargetCacheKey, TargetWitness>()
        private var frozenAdditionalMobileTargets: List<TargetWitness>? = null
        private var frozenAirplaneTarget: TargetWitness? = null
        private var airplaneTargetResolved = false
        private var frozenNoSimTarget: TargetWitness? = null
        private var noSimTargetResolved = false

        private var currentSnapshot = sourceSnapshot
        private var progress = 0f
        private var nativeAppearance = false
        private var nativeAppearanceAnimated = false
        private var started = false
        private var lastStateVersion = sourceSnapshot.stateVersion
        private var lastWitnessSummary = "pending"
        private var lastTintSourceColors: RenderColors? = null
        private var lastTintTransitionColors: RenderColors? = null
        private var lastTintBatteryTinted: Boolean? = null
        private var lastTintTransitionEnabled: Boolean? = null
        private var lastTintMotionProgress: Float? = null
        private var batteryProbeSummary = "pending"
        private var cachedNativePeerTint: Int? = null
        private var cachedNativePeerTintAuthority = "none"
        private var frozenReservationSpans: List<CcTransition.ReservationSpan>? = null
        private var lastReservationWidthPx: Int? = null
        private var lastNativeReservationWidthPx: Int? = null
        private var peerTargetEndPx: Float? = null
        private var transitionReservationEnabled = false
        private var nativePaddingExpansionAllowed = true
        private var genericIslandShowing: Boolean? = null
        private var nativeBatteryIslandActive = false
        private val islandFakeLocation = IntArray(2)
        private val islandTargetLocation = IntArray(2)

        private val preDrawListener =
            ViewTreeObserver.OnPreDrawListener {
                val rootView = rootRef.get()
                val fake = fakeRootRef.get()
                val final = finalRootRef.get()
                val source = sourceViewRef.get()
                val sourceAnchor = sourceAnchorRef.get()
                if (
                    rootView == null ||
                    fake == null ||
                    final == null ||
                    source == null ||
                    sourceAnchor == null ||
                    !rootView.isAttachedToWindow ||
                    !fake.isAttachedToWindow ||
                    !final.isAttachedToWindow ||
                    !source.isAttachedToWindow ||
                    !sourceAnchor.isAttachedToWindow
                ) {
                    CcTransition.detach("pre-draw-detached")
                    return@OnPreDrawListener true
                }

                val latest =
                    CcSession.transitionSourceSnapshot()
                if (
                    latest != null &&
                    latest.view === source &&
                    latest.anchorView === sourceAnchor
                ) {
                    currentSnapshot = latest
                    lastStateVersion = latest.stateVersion
                }
                // Native QS_FAKE peers remain the live tint authority. Their applied
                // tint can change independently from Guiyuan source stateVersion, so
                // keep this read on pre-draw; the resolver itself is allocation-free.
                refreshNativePeerTint()
                syncTransitionReservation()
                drawable.setBounds(0, 0, rootView.width, rootView.height)
                drawable.invalidateSelf()
                true
            }

        fun diagnostic(): String =
            "transitionOwner={progress=" + progress +
                ",appearance=" + nativeAppearance +
                ",appearanceAnimated=" + nativeAppearanceAnimated +
                ",nativePeers=systemui" +
                ",nativeTint=" +
                (cachedNativePeerTint?.toUInt()?.function function function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }(16)?.padStart(8, '0') ?: "none") +
                ",nativeTintAuthority=" + cachedNativePeerTintAuthority +
                ",sourceAnchor=" + (sourceAnchorRef.get()?.javaClass?.simpleName ?: "none") +
                ",sourceOrigin=" + (frozenSource?.source ?: "qs-fake-live") +
                ",sourceStateVersion=" + lastStateVersion +
                ",root=" + (rootRef.get()?.javaClass?.simpleName ?: "none") +
                ",fake=" + (fakeRootRef.get()?.javaClass?.simpleName ?: "none") +
                ",final=" + (finalRootRef.get()?.javaClass?.simpleName ?: "none") +
                ",witness=" + lastWitnessSummary +
                ",tintTransition=" + tintDiagnosticSummary() +
                ",batteryNumberProbe=" + batteryProbeSummary +
                ",reservation=" + (lastReservationWidthPx ?: -1) +
                ",nativeReservation=" + (lastNativeReservationWidthPx ?: -1) +
                ",nativePeerTargetEndOffset=" +
                (peerTargetEndPx?.function function function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() ?: "none") +
                ",batteryIsland=" + nativeBatteryIslandActive +
                ",iconCapacity=" + statusIconCapacitySummary() +
                ",nativeRows=" + nativeStatusRowSummary() +
                ",fakeCarrier=" + fakeCarrierHierarchySummary() +
                ",reservationMode=" +
                when {
                    !transitionReservationEnabled -> "native-peer-motion"
                    genericIslandShowing == true && nativePaddingExpansionAllowed ->
                        "native-progress-total-padding+native-island-collision"
                    genericIslandShowing == true -> "native-island-authority"
                    !nativePaddingExpansionAllowed -> "native-padding-guard"
                    else -> "native-progress-total-padding"
                } +
                "}"

        private fun statusIconCapacitySummary(): String {
            fun summary(group: ViewGroup): String {
                val usableWidth =
                    (group.width - group.paddingStart - group.paddingEnd)
                        .coerceAtLeast(0)
                return "width=" + group.width +
                    "/start=" + group.paddingStart +
                    "/end=" + group.paddingEnd +
                    "/usable=" + usableWidth
            }
            return "{fake=" + summary(fakeStatusIcons) +
                ",final=" + summary(finalStatusIcons) + "}"
        }

        private fun nativeStatusRowSummary(): String {
            fun rootToken(view: View?): String =
                if (view == null) {
                    "none"
                } else {
                    view.javaClass.simpleName +
                        "(v=" + view.visibility +
                        ",a=" + view.alpha +
                        ",w=" + view.width + ")"
                }

            fun groupToken(group: ViewGroup): String {
                val children =
                    buildList {
                        val limit = minOf(group.childCount, 12)
                        for (index in 0 until limit) {
                            val child = group.getChildAt(index)
                            val slot =
                                ParticipantAccess.slotOf(child)
                                    ?: ParticipantAccess.resourceEntryName(child)
                                    ?: child.javaClass.simpleName
                            add(
                                slot +
                                    "(state=" +
                                    (ParticipantAccess.visibleState(child) ?: -1) +
                                    ",icon=" +
                                    (ParticipantAccess.iconVisible(child)?.function function function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }()
                                        ?: "unknown") +
                                    ",v=" + child.visibility +
                                    ",a=" + child.alpha +
                                    ",l=" + child.left +
                                    ",r=" + child.right +
                                    ",w=" + child.width + ")",
                            )
                        }
                    }.joinToString(",")
                return "count=" + group.childCount + "/items=[" + children + "]"
            }

            return "{fakeRoot=" + rootToken(fakeRootRef.get()) +
                ",finalRoot=" + rootToken(finalRootRef.get()) +
                ",fake=" + groupToken(fakeStatusIcons) +
                ",final=" + groupToken(finalStatusIcons) + "}"
        }

        private fun fakeCarrierHierarchySummary(): String {
            fun token(view: View): String {
                val entry =
                    ParticipantAccess.resourceEntryName(view)
                        ?: "no-id"
                return view.javaClass.simpleName + ":" + entry +
                    "(l=" + view.left +
                    ",r=" + view.right +
                    ",w=" + view.width +
                    ",v=" + view.visibility + ")"
            }

            val area = fakeStatusIcons.parent as? View ?: return "{area=none}"
            val row = area.parent as? ViewGroup
            val children =
                row?.let { parent ->
                    buildList {
                        val limit = minOf(parent.childCount, 8)
                        for (index in 0 until limit) {
                            add(token(parent.getChildAt(index)))
                        }
                    }.joinToString(",")
                }.orEmpty()
            val fakeRoot = fakeRootRef.get()
            return "{root=" + (fakeRoot?.let(::token) ?: "none") +
                ",area=" + token(area) +
                ",parent=" + (row?.let(::token) ?: "none") +
                ",children=[" + children + "]}"
        }

        fun matches(
            root: ViewGroup,
            fakeRoot: ViewGroup,
            finalRoot: ViewGroup,
            sourceView: View,
            sourceAnchor: View,
        ): Boolean =
            rootRef.get() === root &&
                fakeRootRef.get() === fakeRoot &&
                finalRootRef.get() === finalRoot &&
                sourceViewRef.get() === sourceView &&
                sourceAnchorRef.get() === sourceAnchor

        fun start() {
            if (started) return
            val rootView = rootRef.get() ?: return
            val source = sourceViewRef.get() ?: return
            started = true
            batteryProbeSummary = resolveBatteryNumberProbe(finalBattery)
            lastBatteryProbeSummary = batteryProbeSummary
            source.clipBounds = sourceMask.appliedClip
            refreshNativePeerTint()
            syncTransitionReservation()
            rootView.overlay.add(drawable)
            rootView.viewTreeObserver.addOnPreDrawListener(preDrawListener)
            drawable.setBounds(0, 0, rootView.width, rootView.height)
            drawable.invalidateSelf()
        }

        fun update(
            progress: Float,
            sourceSnapshot: CcSession.TransitionSourceSnapshot,
            nativeAppearance: Boolean,
            nativeAppearanceAnimated: Boolean,
            transitionReservationEnabled: Boolean,
            sourceScene: SourceScene,
            genericIslandShowing: Boolean?,
            nativeBatteryIslandActive: Boolean?,
        ) {
            val appearanceChanged =
                this.nativeAppearance != nativeAppearance ||
                    this.nativeAppearanceAnimated != nativeAppearanceAnimated
            this.progress = progress.coerceIn(0f, 1f)
            this.currentSnapshot = sourceSnapshot
            this.nativeAppearance = nativeAppearance
            this.nativeAppearanceAnimated = nativeAppearanceAnimated
            this.transitionReservationEnabled = transitionReservationEnabled
            this.genericIslandShowing = genericIslandShowing
            this.nativeBatteryIslandActive = nativeBatteryIslandActive == true

            this.nativePaddingExpansionAllowed =
                CcTransition.allowsNativePadding(
                    sourceScene = sourceScene,
                    genericIslandShowing = genericIslandShowing,
                )
            if (appearanceChanged) {
                refreshNativePeerTint()
            }
            syncTransitionReservation()
            drawable.invalidateSelf()
        }

        fun stop(source: String) {
            HomePresentation.clearCcReservation(
                "transition-" + source,
            )
            genericIslandShowing = null
            nativeBatteryIslandActive = false
            if (!started) return
            started = false
            val rootView = rootRef.get()
            if (rootView?.viewTreeObserver?.isAlive == true) {
                rootView.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
            }
            rootView?.overlay?.remove(drawable)
            sourceViewRef.get()?.let { sourceView ->
                if (sourceView.clipBounds == sourceMask.appliedClip) {
                    sourceView.clipBounds = sourceMask.nativeClip?.let(::Rect)
                }
            }
        }

        fun draw(canvas: Canvas) {
            val rootView = rootRef.get() ?: return
            val fake = fakeRootRef.get() ?: return
            val sourceView = sourceViewRef.get() ?: return
            val sourceAnchor = sourceAnchorRef.get() ?: return
            if (sourceView.width <= 0 || sourceView.height <= 0) return

            val liveSourceParentGeometry =
                if (frozenSource == null) {
                    val sourcePositionSample =
                        sample(
                            view = sourceAnchor,
                            root = rootView,
                        ) ?: return
                    val sourceBasisSample =
                        sample(
                            view = sourceView,
                            root = rootView,
                        ) ?: return
                    CcTransition.composeSourceGeometry(
                        positionAuthority = sourcePositionSample.geometry,
                        basisAuthority = sourceBasisSample.geometry,
                    )
                } else {
                    null
                }
            val sourceParentGeometry =
                frozenSource?.geometry ?: liveSourceParentGeometry ?: return
            val sourceWidth = frozenSource?.width ?: sourceView.width
            val sourceHeight = frozenSource?.height ?: sourceView.height
            val opacity = endpointAlpha(fake)
            if (opacity <= 0f) return
            val finalOpacity =
                finalRootRef.get()
                    ?.let(::endpointAlpha)
                    ?: 0f
            val carrierFrames =
                frozenSource?.let { frozen ->
                    sample(
                        view = fakeStatusIcons,
                        root = rootView,
                    )?.geometry?.let { fullCurrentCarrier ->
                        val currentCarrier =
                            CcTransition.endAnchoredCarrierGeometry(
                                carrierGeometry = fullCurrentCarrier,
                                carrierWidth = fakeStatusIcons.width,
                                carrierHeight = fakeStatusIcons.height,
                                logicalWidth = frozen.motionCarrierWidth,
                                isRtl =
                                    fakeStatusIcons.layoutDirection ==
                                        View.LAYOUT_DIRECTION_RTL,
                            )
                        if (currentCarrier == null) {
                            null
                        } else {
                            CarrierFrames(
                                source = frozen.motionCarrierGeometry,
                                current = currentCarrier,
                            )
                        }
                    }
                }
            val model = currentSnapshot.model
            val specs =
                painter.transitionComponentSpecs(
                    width = sourceWidth,
                    height = sourceHeight,
                    model = model,
                    visualSettings = currentSnapshot.visualSettings,
                )
            if (specs.isEmpty()) return

            val nativeProgress = CcTransition.geometryProgress(progress)
            val motionProgress =
                CcTransition.handoffMotionProgress(
                    expansionProgress = nativeProgress,
                    finalAppearanceAlpha = finalOpacity,
                    finalAppearanceActive = nativeAppearance,
                )
            val mobileSignalShapeProgress =
                CcTransition.mobileSignalShapeProgress(motionProgress)
            if (opacity <= 0f) return

            val preferredMobileSubId =
                PresentationStore
                    .snapshot()
                    .mobilePresentation
                    ?.presentationRootSubscriptionId
            val liveCenterExitDirection =
                specs.firstOrNull {
                    it.component == StatusPainter.TransitionComponent.CENTER
                }?.let { centerSpec ->
                    val source =
                        CcTransition.componentGeometry(
                            parentGeometry = sourceParentGeometry,
                            parentWidth = sourceWidth,
                            parentHeight = sourceHeight,
                            bounds = centerSpec.sourceBounds,
                        ) ?: return@let null
                    val witness =
                        resolveTarget(
                            target = centerSpec.target,
                            preferredMobileSubId = preferredMobileSubId,
                        ) ?: return@let null
                    val target =
                        resolveTargetGeometry(
                            witness = witness,
                            root = rootView,
                            sourceGeometry = source,
                            targetOpticalBounds = centerSpec.targetOpticalBounds,
                        ) ?: return@let null
                    CcTransition.horizontalExitDirection(source, target)
                } ?: BatteryRingTransition.ExitDirection.NONE
            val batteryRingExitDirection =
                CcTransition.batteryRingExitDirection(
                    liveCenterDirection = liveCenterExitDirection,
                    nativeBatteryIslandActive = nativeBatteryIslandActive,
                    targetRowRtl =
                        finalStatusIcons.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                )

            val batteryTinted =
                BatteryColorPolicy.isTinted(
                    state = model.batterySemanticState,
                    settings = currentSnapshot.visualSettings,
                )
            val transitionColors =
                cachedNativePeerTint
                    ?.let { tint ->
                        fun resolveTint(
                            source: Int,
                            tinted: Boolean,
                        ): Int =
                            CcTransition.resolveTransitionTint(
                                source = source,
                                target = tint,
                                progress = motionProgress,
                                tinted = tinted,
                                transitionEnabled =
                                    currentSnapshot.visualSettings
                                        .ccTintTransitionEnabled,
                            )

                        currentSnapshot.colors.copy(
                            centerTint =
                                resolveTint(
                                    source = currentSnapshot.colors.centerTint,
                                    tinted =
                                        batteryTinted &&
                                            currentSnapshot.visualSettings
                                                .centerFollowsBattery,
                                ),
                            mobileTint =
                                resolveTint(
                                    source = currentSnapshot.colors.mobileTint,
                                    tinted =
                                        batteryTinted &&
                                            currentSnapshot.visualSettings
                                                .mobileFollowsBattery,
                                ),
                            batteryTint =
                                resolveTint(
                                    source = currentSnapshot.colors.batteryTint,
                                    tinted = batteryTinted,
                                ),
                            batteryTextTint =
                                resolveTint(
                                    source = currentSnapshot.colors.batteryTextTint,
                                    tinted =
                                        batteryTinted &&
                                            currentSnapshot.visualSettings
                                                .topTextFollowsBattery,
                                ),
                            chargingIconTint =
                                resolveTint(
                                    source = currentSnapshot.colors.chargingIconTint,
                                    tinted =
                                        batteryTinted &&
                                            currentSnapshot.visualSettings
                                                .chargingIconFollowsBattery,
                                ),
                        )
                    }
                    ?: currentSnapshot.colors

            lastTintSourceColors = currentSnapshot.colors
            lastTintTransitionColors = transitionColors
            lastTintBatteryTinted = batteryTinted
            lastTintTransitionEnabled =
                currentSnapshot.visualSettings.ccTintTransitionEnabled
            lastTintMotionProgress = motionProgress

            val refreshWitnessDiagnostic =
                lastWitnessSummary == "pending" ||
                    lastWitnessSummary.contains("unresolved") ||
                    lastWitnessSummary.contains(":0x0")
            val witnessDescriptions =
                if (refreshWitnessDiagnostic) {
                    ArrayList<String>(specs.size)
                } else {
                    null
                }

            val batterySourceCenterX =
                specs.firstOrNull {
                    it.component == StatusPainter.TransitionComponent.BATTERY
                }?.sourceBounds?.centerX

            val batteryNumberFollowerFrames =
                specs.firstOrNull {
                    it.component ==
                        StatusPainter.TransitionComponent.BATTERY_NUMBER
                }?.let { numberSpec ->
                    val numberSource =
                        CcTransition.componentGeometry(
                            parentGeometry = sourceParentGeometry,
                            parentWidth = sourceWidth,
                            parentHeight = sourceHeight,
                            bounds = numberSpec.sourceBounds,
                        ) ?: return@let null
                    val numberWitness =
                        resolveTarget(
                            target = numberSpec.target,
                            preferredMobileSubId = preferredMobileSubId,
                        )
                    val numberTarget =
                        numberWitness?.let { target ->
                            resolveTargetGeometry(
                                witness = target,
                                root = rootView,
                                sourceGeometry = numberSource,
                                targetOpticalBounds = numberSpec.targetOpticalBounds,
                            )
                        }
                    val numberCurrent =
                        if (numberTarget != null) {
                            projectedExactGeometry(
                                source = numberSource,
                                target = numberTarget,
                                progress = motionProgress,
                                carrierFrames = carrierFrames,
                            )
                        } else {
                            carriedSourceGeometry(
                                source = numberSource,
                                carrierFrames = carrierFrames,
                            )
                        }
                    Pair(numberSource, numberCurrent)
                }

            specs.forEach { spec ->
                val sourceGeometry =
                    CcTransition.componentGeometry(
                        parentGeometry = sourceParentGeometry,
                        parentWidth = sourceWidth,
                        parentHeight = sourceHeight,
                        bounds = spec.sourceBounds,
                    ) ?: return@forEach
                val witness =
                    resolveTarget(
                        target = spec.target,
                        preferredMobileSubId = preferredMobileSubId,
                    )
                val targetGeometry =
                    witness?.let { target ->
                        resolveTargetGeometry(
                            witness = target,
                            root = rootView,
                            sourceGeometry = sourceGeometry,
                            targetOpticalBounds = spec.targetOpticalBounds,
                        )
                    }
                val resolvedMobileTargetBars =
                    if (
                        spec.shapePolicy ==
                        StatusPainter.TransitionShapePolicy.MOBILE_SIGNAL &&
                        witness != null
                    ) {
                        mobileTargetBars(witness)
                    } else {
                        null
                    }
                val exactTextGeometry =
                    spec.component ==
                        StatusPainter.TransitionComponent.BATTERY_NUMBER ||
                        (
                            spec.component ==
                                StatusPainter.TransitionComponent.CENTER &&
                                model.centerIndicator is CenterIndicator.MobileType
                        )
                val chargingSourceVisibleFraction =
                    if (
                        spec.component ==
                        StatusPainter.TransitionComponent.CHARGING_ICON
                    ) {
                        StatusPainter.BatteryNumberFollowerPolicy
                            .chargingSourceVisibleFraction(motionProgress)
                    } else {
                        0f
                    }
                val chargingSourceLocked =
                    spec.component ==
                        StatusPainter.TransitionComponent.CHARGING_ICON &&
                        chargingSourceVisibleFraction > 0f
                val componentMotionProgress =
                    if (
                        spec.component ==
                        StatusPainter.TransitionComponent.CHARGING_ICON
                    ) {
                        StatusPainter.BatteryNumberFollowerPolicy
                            .chargingMotionProgress(motionProgress)
                    } else {
                        motionProgress
                    }
                val geometry =
                    when {
                        chargingSourceLocked -> {
                            batteryNumberFollowerFrames
                                ?.let { (numberSource, numberCurrent) ->
                                    CcTransition.followAnchorGeometry(
                                        follower = sourceGeometry,
                                        sourceAnchor = numberSource,
                                        currentAnchor = numberCurrent,
                                    )
                                }
                                ?: carriedSourceGeometry(
                                    source = sourceGeometry,
                                    carrierFrames = carrierFrames,
                                )
                        }

                        targetGeometry != null -> {
                            if (exactTextGeometry) {
                                projectedExactGeometry(
                                    source = sourceGeometry,
                                    target = targetGeometry,
                                    progress = componentMotionProgress,
                                    carrierFrames = carrierFrames,
                                )
                            } else {
                                projectedGeometry(
                                    source = sourceGeometry,
                                    target = targetGeometry,
                                    progress = componentMotionProgress,
                                    scalePolicy = spec.scalePolicy,
                                    carrierFrames = carrierFrames,
                                )
                            }
                        }

                        else ->
                            carriedSourceGeometry(
                                source = sourceGeometry,
                                carrierFrames = carrierFrames,
                            )
                    }
                val componentVisibleFraction =
                    if (
                        spec.component ==
                        StatusPainter.TransitionComponent.CHARGING_ICON
                    ) {
                        StatusPainter.BatteryNumberFollowerPolicy
                            .chargingVisibleFraction(
                                progress = motionProgress,
                                targetAvailable = targetGeometry != null,
                            )
                    } else if (targetGeometry != null) {
                        1f
                    } else {
                        CcTransition.unmatchedExitVisibleFraction(motionProgress)
                    }
                if (componentVisibleFraction <= 0f || opacity <= 0f) return@forEach
                val matrixBounds =
                    when {
                        spec.component ==
                                StatusPainter.TransitionComponent.CENTER &&
                            model.centerIndicator is CenterIndicator.MobileType ->
                            painter.mobileTypeTransitionBounds(
                                width = sourceWidth,
                                height = sourceHeight,
                                indicator = model.centerIndicator,
                                targetWeight = witness?.textWeight,
                                targetStyle = witness?.textStyle,
                                progress = motionProgress,
                                visualSettings = currentSnapshot.visualSettings,
                            ) ?: spec.sourceBounds

                        spec.component ==
                            StatusPainter.TransitionComponent.BATTERY_NUMBER ->
                            painter.transitionBatteryNumberCurrentBounds(
                                width = sourceWidth,
                                height = sourceHeight,
                                model = model,
                                visualSettings = currentSnapshot.visualSettings,
                                targetWeight = witness?.textWeight,
                                targetStyle = witness?.textStyle,
                                progress = motionProgress,
                            ) ?: spec.sourceBounds

                        else -> spec.sourceBounds
                    }
                val matrix =
                    matrixForBoundsGeometry(
                        geometry = geometry,
                        bounds = matrixBounds,
                    ) ?: return@forEach

                val save =
                    canvas.saveLayerAlpha(
                        null,
                        (255f * opacity.coerceIn(0f, 1f)).roundToInt(),
                    )
                canvas.concat(matrix)
                if (componentVisibleFraction < 1f) {
                    val clipAnchorRight =
                        if (
                            spec.component ==
                            StatusPainter.TransitionComponent.CHARGING_ICON &&
                            batterySourceCenterX != null
                        ) {
                            batterySourceCenterX >= matrixBounds.centerX
                        } else {
                            sourceView.layoutDirection != View.LAYOUT_DIRECTION_RTL
                        }
                    val clip =
                        CcTransition.horizontalClipBounds(
                            left = matrixBounds.left,
                            top = matrixBounds.top,
                            right = matrixBounds.right,
                            bottom = matrixBounds.bottom,
                            visibleFraction = componentVisibleFraction,
                            anchorRight = clipAnchorRight,
                        ) ?: run {
                            canvas.restoreToCount(save)
                            return@forEach
                        }
                    canvas.clipRect(clip[0], clip[1], clip[2], clip[3])
                }
                painter.drawTransitionComponent(
                    canvas = canvas,
                    width = sourceWidth,
                    height = sourceHeight,
                    model = model,
                    colors = transitionColors,
                    component = spec.component,
                    shapePolicy = spec.shapePolicy,
                    opacity = 1f,
                    visualSettings = currentSnapshot.visualSettings,
                    motionProgress = motionProgress,
                    shapeProgress =
                        when (spec.shapePolicy) {
                            StatusPainter.TransitionShapePolicy.BATTERY_RETRACT ->
                                motionProgress

                            StatusPainter.TransitionShapePolicy.MOBILE_SIGNAL ->
                                mobileSignalShapeProgress

                            StatusPainter.TransitionShapePolicy.RIGID ->
                                0f
                        },
                    mobileTargetWidthRatio =
                        if (
                            spec.shapePolicy ==
                            StatusPainter.TransitionShapePolicy.MOBILE_SIGNAL &&
                            targetGeometry != null
                        ) {
                            CcTransition.relativeGeometryWidth(
                                target = targetGeometry,
                                current = sourceGeometry,
                            )
                        } else {
                            null
                        },
                    mobileTargetHeightRatio =
                        if (
                            spec.shapePolicy ==
                            StatusPainter.TransitionShapePolicy.MOBILE_SIGNAL &&
                            targetGeometry != null
                        ) {
                            CcTransition.relativeGeometryHeight(
                                target = targetGeometry,
                                current = sourceGeometry,
                            )
                        } else {
                            null
                        },
                    mobileTargetBars = resolvedMobileTargetBars,
                    batteryNumberTargetWeight =
                        if (
                            spec.component ==
                            StatusPainter.TransitionComponent.BATTERY_NUMBER
                        ) {
                            witness?.textWeight
                        } else {
                            null
                        },
                    batteryNumberTargetStyle =
                        if (
                            spec.component ==
                            StatusPainter.TransitionComponent.BATTERY_NUMBER
                        ) {
                            witness?.textStyle
                        } else {
                            null
                        },
                    centerTargetTextWeight =
                        if (
                            spec.component ==
                                StatusPainter.TransitionComponent.CENTER &&
                            model.centerIndicator is CenterIndicator.MobileType
                        ) {
                            witness?.textWeight
                        } else {
                            null
                        },
                    centerTargetTextStyle =
                        if (
                            spec.component ==
                                StatusPainter.TransitionComponent.CENTER &&
                            model.centerIndicator is CenterIndicator.MobileType
                        ) {
                            witness?.textStyle
                        } else {
                            null
                        },
                    batteryRingExitDirection =
                        if (spec.component == StatusPainter.TransitionComponent.BATTERY) {
                            batteryRingExitDirection
                        } else {
                            BatteryRingTransition.ExitDirection.NONE
                        },
                )
                canvas.restoreToCount(save)

                witnessDescriptions?.add(
                    witness?.summary ?: (spec.component.name.lowercase() + ":unresolved"),
                )
            }

            val mobileSpec =
                specs.firstOrNull { spec ->
                    spec.component == StatusPainter.TransitionComponent.MOBILE
                }
            if (
                mobileSpec != null &&
                sourceRepresentsAny(MOBILE_SLOT, STACKED_MOBILE_SLOT)
            ) {
                val extras =
                    drawAdditionalMobileLatent(
                        canvas = canvas,
                        rootView = rootView,
                        sourceParentGeometry = sourceParentGeometry,
                        sourceWidth = sourceWidth,
                        sourceHeight = sourceHeight,
                        model = model,
                        colors = transitionColors,
                        mobileSpec = mobileSpec,
                        preferredMobileSubId = preferredMobileSubId,
                        motionProgress = motionProgress,
                        shapeProgress = mobileSignalShapeProgress,
                        opacity = opacity,
                        carrierFrames = carrierFrames,
                        collectDescription = witnessDescriptions != null,
                    )
                witnessDescriptions?.addAll(extras)
            }
            drawAirplaneReveal(
                canvas = canvas,
                rootView = rootView,
                sourceParentGeometry = sourceParentGeometry,
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                model = model,
                colors = transitionColors,
                motionProgress = motionProgress,
                opacity = opacity,
                carrierFrames = carrierFrames,
            )?.let { description ->
                witnessDescriptions?.add(description)
            }
            drawSupplementalNoSimReveal(
                canvas = canvas,
                rootView = rootView,
                sourceParentGeometry = sourceParentGeometry,
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                model = model,
                colors = transitionColors,
                motionProgress = motionProgress,
                opacity = opacity,
                carrierFrames = carrierFrames,
            )?.let { description ->
                witnessDescriptions?.add(description)
            }

            witnessDescriptions?.let { descriptions ->
                lastWitnessSummary = descriptions.joinToString("|")
            }
        }

        private fun drawAdditionalMobileLatent(
            canvas: Canvas,
            rootView: View,
            sourceParentGeometry: FloatArray,
            sourceWidth: Int,
            sourceHeight: Int,
            model: RenderModel,
            colors: RenderColors,
            mobileSpec: StatusPainter.TransitionComponentSpec,
            preferredMobileSubId: Int?,
            motionProgress: Float,
            shapeProgress: Float,
            opacity: Float,
            carrierFrames: CarrierFrames?,
            collectDescription: Boolean,
        ): List<String> {
            if (
                mobileSpec.shapePolicy !=
                StatusPainter.TransitionShapePolicy.MOBILE_SIGNAL ||
                !sourceRepresentsAny(MOBILE_SLOT, STACKED_MOBILE_SLOT)
            ) {
                return emptyList()
            }
            val sourceGeometry =
                CcTransition.componentGeometry(
                    parentGeometry = sourceParentGeometry,
                    parentWidth = sourceWidth,
                    parentHeight = sourceHeight,
                    bounds = mobileSpec.sourceBounds,
                ) ?: return emptyList()
            val primary =
                resolveTarget(
                    target = mobileSpec.target,
                    preferredMobileSubId = preferredMobileSubId,
                )
            val state = StatusStateStore.snapshot()
            val descriptions =
                if (collectDescription) {
                    ArrayList<String>()
                } else {
                    null
                }
            frozenExtraMobileTargets(primary).forEach { witness ->
                val subId = witness.subscriptionId ?: return@forEach
                val level =
                    when (val signal = state.mobile[subId]?.signal) {
                        is SignalStrength.Level -> signal.value.coerceIn(0, 4)
                        else -> return@forEach
                    }
                val targetGeometry =
                    resolveTargetGeometry(
                        witness = witness,
                        root = rootView,
                        sourceGeometry = sourceGeometry,
                        targetOpticalBounds = mobileSpec.targetOpticalBounds,
                    ) ?: return@forEach
                val targetBars = mobileTargetBars(witness)
                val targetWidthRatio =
                    CcTransition.relativeGeometryWidth(
                        target = targetGeometry,
                        current = sourceGeometry,
                    )
                val targetHeightRatio =
                    CcTransition.relativeGeometryHeight(
                        target = targetGeometry,
                        current = sourceGeometry,
                    )
                val outerSimilarityScale =
                    StatusPainter.MobileSignalMorphPolicy.outerSimilarityScale(
                        targetWidthRatio = targetWidthRatio,
                        targetHeightRatio = targetHeightRatio,
                    )
                val clipBounds =
                    CcTransition.expandedClipBounds(
                        bounds = mobileSpec.sourceBounds,
                        widthScale =
                            StatusPainter.MobileSignalMorphPolicy
                                .exactTargetAxisCompensation(
                                    targetAxisRatio = targetWidthRatio,
                                    outerScale = outerSimilarityScale,
                                ),
                        heightScale =
                            StatusPainter.MobileSignalMorphPolicy
                                .exactTargetAxisCompensation(
                                    targetAxisRatio = targetHeightRatio,
                                    outerScale = outerSimilarityScale,
                                ),
                    )
                val pathGeometry =
                    projectedGeometry(
                        source = sourceGeometry,
                        target = targetGeometry,
                        progress = motionProgress,
                        scalePolicy = mobileSpec.scalePolicy,
                        carrierFrames = carrierFrames,
                    )
                val geometry = pathGeometry
                val matrix =
                    matrixForBoundsGeometry(
                        geometry = geometry,
                        bounds = mobileSpec.sourceBounds,
                    ) ?: return@forEach
                val revealVisibleFraction =
                    latentRevealVisibleFraction(
                        currentGeometry = geometry,
                        targetGeometry = targetGeometry,
                        witness = witness,
                    )
                if (revealVisibleFraction <= 0f || opacity <= 0f) return@forEach
                val save =
                    canvas.saveLayerAlpha(
                        null,
                        (255f * opacity.coerceIn(0f, 1f)).roundToInt(),
                    )
                canvas.concat(matrix)
                val clip =
                    CcTransition.horizontalClipBounds(
                        left = clipBounds.left,
                        top = clipBounds.top,
                        right = clipBounds.right,
                        bottom = clipBounds.bottom,
                        visibleFraction = revealVisibleFraction,
                        anchorRight =
                            (sourceViewRef.get()?.layoutDirection
                                ?: View.LAYOUT_DIRECTION_LTR) !=
                                View.LAYOUT_DIRECTION_RTL,
                    ) ?: run {
                        canvas.restoreToCount(save)
                        return@forEach
                    }
                canvas.clipRect(clip[0], clip[1], clip[2], clip[3])
                painter.drawTransitionComponent(
                    canvas = canvas,
                    width = sourceWidth,
                    height = sourceHeight,
                    model =
                        model.copy(
                            mobileLevel = level,
                            mobileUnavailableMark = false,
                            effectiveDataSubscriptionId = subId,
                        ),
                    colors = colors,
                    component = StatusPainter.TransitionComponent.MOBILE,
                    shapePolicy = mobileSpec.shapePolicy,
                    opacity = 1f,
                    motionProgress = motionProgress,
                    shapeProgress = shapeProgress,
                    mobileTargetWidthRatio = targetWidthRatio,
                    mobileTargetHeightRatio = targetHeightRatio,
                    mobileTargetBars = targetBars,
                )
                canvas.restoreToCount(save)
                descriptions?.add("mobile-latent:" + witness.summary)
            }
            return descriptions ?: emptyList()
        }

        private fun drawAirplaneReveal(
            canvas: Canvas,
            rootView: View,
            sourceParentGeometry: FloatArray,
            sourceWidth: Int,
            sourceHeight: Int,
            model: RenderModel,
            colors: RenderColors,
            motionProgress: Float,
            opacity: Float,
            carrierFrames: CarrierFrames?,
        ): String? {
            if (
                StatusStateStore.snapshot().airplaneMode != true ||
                model.centerIndicator is CenterIndicator.Airplane ||
                !sourceRepresentsAny(AIRPLANE_SLOT)
            ) {
                return null
            }
            val bounds =
                painter.transitionAirplaneSourceBounds(
                    width = sourceWidth,
                    height = sourceHeight,
                    visualSettings = currentSnapshot.visualSettings,
                ) ?: return null
            val sourceGeometry =
                CcTransition.componentGeometry(
                    parentGeometry = sourceParentGeometry,
                    parentWidth = sourceWidth,
                    parentHeight = sourceHeight,
                    bounds = bounds,
                ) ?: return null
            val witness = resolveFrozenAirplaneTarget() ?: return null
            val targetGeometry =
                resolveTargetGeometry(
                    witness = witness,
                    root = rootView,
                    sourceGeometry = sourceGeometry,
                    targetOpticalBounds = null,
                ) ?: return null
            val pathGeometry =
                projectedGeometry(
                    source = sourceGeometry,
                    target = targetGeometry,
                    progress = motionProgress,
                    scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
                    carrierFrames = carrierFrames,
                )
            val geometry = pathGeometry
            val revealVisibleFraction =
                latentRevealVisibleFraction(
                    currentGeometry = geometry,
                    targetGeometry = targetGeometry,
                    witness = witness,
                )
            if (revealVisibleFraction <= 0f || opacity <= 0f) return null
            val matrix =
                matrixForBoundsGeometry(
                    geometry = geometry,
                    bounds = bounds,
                ) ?: return null
            val save =
                canvas.saveLayerAlpha(
                    null,
                    (255f * opacity.coerceIn(0f, 1f)).roundToInt(),
                )
            canvas.concat(matrix)
            val clip =
                CcTransition.horizontalClipBounds(
                    left = bounds.left,
                    top = bounds.top,
                    right = bounds.right,
                    bottom = bounds.bottom,
                    visibleFraction = revealVisibleFraction,
                    anchorRight =
                        (sourceViewRef.get()?.layoutDirection
                            ?: View.LAYOUT_DIRECTION_LTR) !=
                            View.LAYOUT_DIRECTION_RTL,
                ) ?: run {
                    canvas.restoreToCount(save)
                    return null
                }
            canvas.clipRect(clip[0], clip[1], clip[2], clip[3])
            painter.drawTransitionAirplane(
                canvas = canvas,
                width = sourceWidth,
                height = sourceHeight,
                tint = colors.centerTint,
                opacity = 1f,
                visualSettings = currentSnapshot.visualSettings,
            )
            canvas.restoreToCount(save)
            return "airplane-reveal:" + witness.summary
        }

        private fun drawSupplementalNoSimReveal(
            canvas: Canvas,
            rootView: View,
            sourceParentGeometry: FloatArray,
            sourceWidth: Int,
            sourceHeight: Int,
            model: RenderModel,
            colors: RenderColors,
            motionProgress: Float,
            opacity: Float,
            carrierFrames: CarrierFrames?,
        ): String? {
            if (
                model.centerIndicator is CenterIndicator.NoSim ||
                !sourceRepresentsAny(NO_SIM_SLOT)
            ) {
                return null
            }
            val presentation = PresentationStore.snapshot()
            val resource =
                presentation.statusIcons.noSimIcon
                    ?.takeIf { presentation.statusIcons.noSimVisible }
                    ?: return null
            val bounds =
                painter.transitionNoSimSourceBounds(
                    width = sourceWidth,
                    height = sourceHeight,
                    resource = resource,
                    visualSettings = currentSnapshot.visualSettings,
                ) ?: return null
            val sourceGeometry =
                CcTransition.componentGeometry(
                    parentGeometry = sourceParentGeometry,
                    parentWidth = sourceWidth,
                    parentHeight = sourceHeight,
                    bounds = bounds,
                ) ?: return null
            val witness = resolveFrozenNoSimTarget() ?: return null
            val targetGeometry =
                resolveTargetGeometry(
                    witness = witness,
                    root = rootView,
                    sourceGeometry = sourceGeometry,
                    targetOpticalBounds = null,
                ) ?: return null
            val pathGeometry =
                projectedGeometry(
                    source = sourceGeometry,
                    target = targetGeometry,
                    progress = motionProgress,
                    scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
                    carrierFrames = carrierFrames,
                )
            val geometry = pathGeometry
            val revealVisibleFraction =
                latentRevealVisibleFraction(
                    currentGeometry = geometry,
                    targetGeometry = targetGeometry,
                    witness = witness,
                )
            if (revealVisibleFraction <= 0f || opacity <= 0f) return null
            val matrix =
                matrixForBoundsGeometry(
                    geometry = geometry,
                    bounds = bounds,
                ) ?: return null
            val save =
                canvas.saveLayerAlpha(
                    null,
                    (255f * opacity.coerceIn(0f, 1f)).roundToInt(),
                )
            canvas.concat(matrix)
            val clip =
                CcTransition.horizontalClipBounds(
                    left = bounds.left,
                    top = bounds.top,
                    right = bounds.right,
                    bottom = bounds.bottom,
                    visibleFraction = revealVisibleFraction,
                    anchorRight =
                        (sourceViewRef.get()?.layoutDirection
                            ?: View.LAYOUT_DIRECTION_LTR) !=
                            View.LAYOUT_DIRECTION_RTL,
                ) ?: run {
                    canvas.restoreToCount(save)
                    return null
                }
            canvas.clipRect(clip[0], clip[1], clip[2], clip[3])
            painter.drawTransitionNoSim(
                canvas = canvas,
                width = sourceWidth,
                height = sourceHeight,
                resource = resource,
                tint = colors.centerTint,
                opacity = 1f,
                visualSettings = currentSnapshot.visualSettings,
            )
            canvas.restoreToCount(save)
            return "no-sim-reveal:" + witness.summary
        }

        private fun projectedExactGeometry(
            source: FloatArray,
            target: FloatArray,
            progress: Float,
            carrierFrames: CarrierFrames?,
        ): FloatArray =
            carrierFrames?.let { frames ->
                CcTransition.interpolateCarriedToRootExact(
                    source = source,
                    target = target,
                    sourceCarrier = frames.source,
                    currentCarrier = frames.current,
                    progress = progress,
                )
            } ?: CcTransition.interpolateGeometry(
                source = source,
                target = target,
                progress = CcTransition.geometryProgress(progress),
            )

        private fun projectedGeometry(
            source: FloatArray,
            target: FloatArray,
            progress: Float,
            scalePolicy: StatusPainter.TransitionScalePolicy,
            carrierFrames: CarrierFrames?,
        ): FloatArray =
            carrierFrames?.let { frames ->
                CcTransition.interpolateCarriedToRoot(
                    source = source,
                    target = target,
                    sourceCarrier = frames.source,
                    currentCarrier = frames.current,
                    progress = progress,
                    scalePolicy = scalePolicy,
                )
            } ?: CcTransition.interpolateSimilarityGeometry(
                source = source,
                target = target,
                progress = CcTransition.geometryProgress(progress),
                scalePolicy = scalePolicy,
            )

        private fun carriedSourceGeometry(
            source: FloatArray,
            carrierFrames: CarrierFrames?,
        ): FloatArray =
            carrierFrames?.let { frames ->
                CcTransition.rebaseSourceToCurrentCarrier(
                    source = source,
                    sourceCarrier = frames.source,
                    currentCarrier = frames.current,
                )
            } ?: source.copyOf()

        private fun latentRevealVisibleFraction(
            currentGeometry: FloatArray,
            targetGeometry: FloatArray,
            witness: TargetWitness,
        ): Float {
            val source = sourceViewRef.get() ?: return 0f
            val compactWidth =
                CompactReservationPolicy.resolveCenteredVisualWidth(
                    baseSlotWidthPx = (frozenSource?.width ?: source.width).coerceAtLeast(0),
                    userScale = currentSnapshot.visualSettings.combinedScale,
                )
            val currentReservation =
                lastReservationWidthPx ?: compactWidth
            val requiredReservation =
                requiredReservationWidth(witness)
                    ?: return 0f

            val targetWidth =
                sqrt(
                    targetGeometry[2] * targetGeometry[2] +
                        targetGeometry[3] * targetGeometry[3],
                )
            val targetHeight =
                sqrt(
                    targetGeometry[4] * targetGeometry[4] +
                        targetGeometry[5] * targetGeometry[5],
                )
            val visualExtent =
                maxOf(targetWidth, targetHeight)
                    .takeIf { it.isFinite() && it > 0f }
                    ?: return 0f

            val reservationProgress =
                CcTransition.latentReservationProgress(
                    compactWidthPx = compactWidth,
                    currentReservationPx = currentReservation,
                    requiredReservationPx = requiredReservation,
                    visualWidthPx = targetWidth,
                )
            return CcTransition.latentRevealVisibleFraction(
                current = currentGeometry,
                target = targetGeometry,
                visualExtent = visualExtent,
                reservationProgress = reservationProgress,
            )
        }

        private fun requiredReservationWidth(
            witness: TargetWitness,
        ): Int? {
            val source = sourceViewRef.get() ?: return null
            if (source.width <= 0 || finalBattery.width <= 0) return null
            val sourceRtl =
                source.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val targetRtl =
                finalBattery.layoutDirection == View.LAYOUT_DIRECTION_RTL
            if (sourceRtl != targetRtl) return null

            val visualView = witness.opticalView ?: witness.slotView
            if (!isReliableSemanticTarget(visualView)) return null
            val snapshot =
                VisualSnapshot.resolveView(visualView)
                    ?: return null
            val envelope = snapshot.envelope
            if (envelope.width <= 0f || envelope.height <= 0f) return null

            fun globalBounds(
                view: View,
                left: Float,
                top: Float,
                right: Float,
                bottom: Float,
            ): RectF {
                val rect = RectF(left, top, right, bottom)
                val matrix = Matrix()
                view.transformMatrixToGlobal(matrix)
                matrix.mapRect(rect)
                return rect
            }

            val visualBounds =
                globalBounds(
                    view = visualView,
                    left = envelope.left * visualView.width,
                    top = envelope.top * visualView.height,
                    right = envelope.right * visualView.width,
                    bottom = envelope.bottom * visualView.height,
                )
            val batteryBounds =
                globalBounds(
                    view = finalBattery,
                    left = 0f,
                    top = 0f,
                    right = finalBattery.width.toFloat(),
                    bottom = finalBattery.height.toFloat(),
                )
            val finalEndPhysical =
                if (targetRtl) batteryBounds.left else batteryBounds.right
            fun logicalTargetX(physicalX: Float): Float =
                (if (targetRtl) -physicalX else physicalX) -
                    (if (targetRtl) -finalEndPhysical else finalEndPhysical)

            val targetA = logicalTargetX(visualBounds.left)
            val targetB = logicalTargetX(visualBounds.right)
            val compact =
                (frozenSource?.width ?: source.width)
                    .coerceAtLeast(0)
                    .toFloat()
            val left = min(-compact, min(targetA, targetB))
            val right = maxOf(0f, maxOf(targetA, targetB))
            return kotlin.math.ceil(
                (right - left).coerceAtLeast(compact),
            ).toInt()
        }

        private fun sourceRepresentsAny(vararg slots: String): Boolean {
            val represented = frozenSource?.representedSlots ?: return false
            return slots.any(represented::contains)
        }

        private fun tintDiagnosticSummary(): String {
            val sourceColors = lastTintSourceColors ?: return "pending"
            val transitionColors = lastTintTransitionColors ?: return "pending"
            val batteryTinted = lastTintBatteryTinted ?: return "pending"
            val transitionEnabled = lastTintTransitionEnabled ?: return "pending"
            val motionProgress = lastTintMotionProgress ?: return "pending"

            fun tintHex(color: Int): String =
                color.toUInt().function function function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }(16).padStart(8, '0')

            return "{batteryTinted=" + batteryTinted +
                ",enabled=" + transitionEnabled +
                ",motion=" + motionProgress +
                ",target=" +
                (cachedNativePeerTint?.let(::tintHex) ?: "none") +
                ",battery=" +
                tintHex(sourceColors.batteryTint) + "->" +
                tintHex(transitionColors.batteryTint) +
                ",number=" +
                tintHex(sourceColors.batteryTextTint) + "->" +
                tintHex(transitionColors.batteryTextTint) +
                ",charging=" +
                tintHex(sourceColors.chargingIconTint) + "->" +
                tintHex(transitionColors.chargingIconTint) +
                ",center=" +
                tintHex(sourceColors.centerTint) + "->" +
                tintHex(transitionColors.centerTint) +
                ",mobile=" +
                tintHex(sourceColors.mobileTint) + "->" +
                tintHex(transitionColors.mobileTint) +
                ",tintPhase=" + CcTransition.transitionTintProgress(motionProgress) +
                "}"
        }

        private fun refreshNativePeerTint() {
            val peerTint =
                NativeNetworkSuppression
                    .currentStatusTint(fakeStatusIcons)
            val resolved =
                CcTransition.selectNativeTransitionTint(
                    statusIconPeerTint = peerTint,
                    cachedTint = cachedNativePeerTint,
                )
            if (resolved != null) {
                cachedNativePeerTint = resolved
                cachedNativePeerTintAuthority =
                    if (peerTint != null && resolved == peerTint) {
                        "qs-fake-visible-peer-applied"
                    } else {
                        "cached-last-valid"
                    }
            }
        }

        private fun syncTransitionReservation() {
            if (!transitionReservationEnabled) {
                if (lastNativeReservationWidthPx != null) {
                    HomePresentation.clearCcReservation(
                        "transition-source-native-peer-motion",
                    )
                }
                lastReservationWidthPx = null
                lastNativeReservationWidthPx = null
                peerTargetEndPx = null
                return
            }
            val source = sourceViewRef.get() ?: return
            if (source.width <= 0) return
            val spans =
                frozenReservationSpans
                    ?: resolveReservationSpans()
                        ?.also { resolved ->
                            frozenReservationSpans = resolved
                        }
                    ?: return
            val compactWidth =
                CompactReservationPolicy.resolveCenteredVisualWidth(
                    baseSlotWidthPx = frozenSource?.width ?: source.width,
                    userScale = currentSnapshot.visualSettings.combinedScale,
                )
            val requestedWidth =
                CcTransition.reservationWidth(
                    compactWidthPx = compactWidth,
                    spans = spans,
                    progress = progress,
                )

            // The logical transition width is Guiyuan-owned. Native status-icon
            // capacity and island collision remain HyperOS-owned.
            lastReservationWidthPx = requestedWidth

            val nativeRequestedWidth =
                if (nativeBatteryIslandActive) {
                    // Reservation spans store target X in the final-Battery end frame.
                    // HyperOS translates the visible QS_FAKE carrier independently for
                    // Battery Island, so project that target end into the current fake
                    // end frame before measuring the current projected occupancy.
                    peerTargetEndPx = null
                    val targetEndOffsetPx =
                        batteryPeerEndOffsetPx()
                            ?: run {
                                lastNativeReservationWidthPx = null
                                val presentationFailed =
                                    HomePresentation
                                        .failCcPresentation(
                                            "battery-island-peer-end-frame-unavailable",
                                        )
                                if (!presentationFailed) {
                                    CcTransition.detach(
                                        "battery-island-peer-end-frame-unavailable",
                                    )
                                }
                                return
                            }
                    peerTargetEndPx = targetEndOffsetPx
                    CcTransition.batteryPeerReservationWidth(
                        compactWidthPx = compactWidth,
                        spans = spans,
                        semanticWidthPx = requestedWidth,
                        progress = progress,
                        targetEndOffsetPx = targetEndOffsetPx,
                    )
                } else {
                    peerTargetEndPx = null
                    requestedWidth
                }

            if (!nativePaddingExpansionAllowed) {
                if (lastNativeReservationWidthPx != null) {
                    HomePresentation.clearCcReservation(
                        "transition-island-native-padding-guard",
                    )
                    lastNativeReservationWidthPx = null
                }
                return
            }

            if (lastNativeReservationWidthPx != nativeRequestedWidth) {
                val applied =
                    HomePresentation
                        .updateCcReservation(
                            requestedSlotWidthPx = nativeRequestedWidth,
                        )
                if (!applied) {
                    return
                }
                lastNativeReservationWidthPx = nativeRequestedWidth
            }
        }

        private fun batteryPeerEndOffsetPx(): Float? {
            if (
                fakeStatusIcons.width <= 0 ||
                finalBattery.width <= 0 ||
                !fakeStatusIcons.isAttachedToWindow ||
                !finalBattery.isAttachedToWindow
            ) {
                return null
            }
            val fakeRtl =
                fakeStatusIcons.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val targetRtl =
                finalBattery.layoutDirection == View.LAYOUT_DIRECTION_RTL
            if (fakeRtl != targetRtl) return null

            val located =
                runCatching {
                    fakeStatusIcons.getLocationInWindow(islandFakeLocation)
                    finalBattery.getLocationInWindow(islandTargetLocation)
                    true
                }.getOrDefault(false)
            if (!located) return null

            val fakeEndPhysical =
                if (fakeRtl) {
                    islandFakeLocation[0].toFloat()
                } else {
                    (islandFakeLocation[0] + fakeStatusIcons.width).toFloat()
                }
            val targetEndPhysical =
                if (targetRtl) {
                    islandTargetLocation[0].toFloat()
                } else {
                    (islandTargetLocation[0] + finalBattery.width).toFloat()
                }
            val fakeEndLogical =
                if (fakeRtl) -fakeEndPhysical else fakeEndPhysical
            val targetEndLogical =
                if (targetRtl) -targetEndPhysical else targetEndPhysical
            return targetEndLogical - fakeEndLogical
        }

        private fun resolveReservationSpans(): List<CcTransition.ReservationSpan>? {
            val source = sourceViewRef.get() ?: return null
            if (source.width <= 0 || source.height <= 0) return null
            if (!isUsableSlotView(finalBattery)) return null

            val specs =
                painter.transitionComponentSpecs(
                    width = source.width,
                    height = source.height,
                    model = currentSnapshot.model,
                )
            if (specs.isEmpty()) return null

            val sourceRtl =
                source.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val targetRtl =
                finalBattery.layoutDirection == View.LAYOUT_DIRECTION_RTL
            if (sourceRtl != targetRtl) return null

            val batteryLocation = IntArray(2)
            finalBattery.getLocationInWindow(batteryLocation)
            val finalEndPhysical =
                if (targetRtl) {
                    batteryLocation[0].toFloat()
                } else {
                    (batteryLocation[0] + finalBattery.width).toFloat()
                }
            fun logicalTargetX(physicalX: Float): Float =
                (if (targetRtl) -physicalX else physicalX) -
                    (if (targetRtl) -finalEndPhysical else finalEndPhysical)
            fun logicalSourceX(localX: Float): Float {
                val sourceEnd =
                    if (sourceRtl) 0f else source.width.toFloat()
                return (if (sourceRtl) -localX else localX) -
                    (if (sourceRtl) -sourceEnd else sourceEnd)
            }

            val preferredMobileSubId =
                PresentationStore
                    .snapshot()
                    .mobilePresentation
                    ?.presentationRootSubscriptionId

            val result = ArrayList<CcTransition.ReservationSpan>(specs.size)
            specs.forEach { spec ->
                val witness =
                    resolveTarget(
                        target = spec.target,
                        preferredMobileSubId = preferredMobileSubId,
                    ) ?: return@forEach
                val slot = witness.slotView
                if (!isUsableSlotView(slot)) return@forEach
                val targetLocation = IntArray(2)
                slot.getLocationInWindow(targetLocation)

                val sourceA = logicalSourceX(spec.sourceBounds.left)
                val sourceB = logicalSourceX(spec.sourceBounds.right)
                val targetA = logicalTargetX(targetLocation[0].toFloat())
                val targetB =
                    logicalTargetX(
                        (targetLocation[0] + slot.width).toFloat(),
                    )
                result +=
                    CcTransition.ReservationSpan(
                        sourceLeft = min(sourceA, sourceB),
                        sourceRight = maxOf(sourceA, sourceB),
                        targetLeft = min(targetA, targetB),
                        targetRight = maxOf(targetA, targetB),
                    )
            }
            val mobileSpec =
                specs.firstOrNull { spec ->
                    spec.component == StatusPainter.TransitionComponent.MOBILE
                }
            if (
                mobileSpec != null &&
                sourceRepresentsAny(MOBILE_SLOT, STACKED_MOBILE_SLOT)
            ) {
                val primary =
                    resolveTarget(
                        target = mobileSpec.target,
                        preferredMobileSubId = preferredMobileSubId,
                    )
                frozenExtraMobileTargets(primary).forEach { witness ->
                    val slot = witness.slotView
                    val targetLocation = IntArray(2)
                    slot.getLocationInWindow(targetLocation)
                    val sourceA = logicalSourceX(mobileSpec.sourceBounds.left)
                    val sourceB = logicalSourceX(mobileSpec.sourceBounds.right)
                    val targetA = logicalTargetX(targetLocation[0].toFloat())
                    val targetB =
                        logicalTargetX(
                            (targetLocation[0] + slot.width).toFloat(),
                        )
                    result +=
                        CcTransition.ReservationSpan(
                            sourceLeft = min(sourceA, sourceB),
                            sourceRight = maxOf(sourceA, sourceB),
                            targetLeft = min(targetA, targetB),
                            targetRight = maxOf(targetA, targetB),
                        )
                }
            }

            if (
                StatusStateStore.snapshot().airplaneMode == true &&
                currentSnapshot.model.centerIndicator !is CenterIndicator.Airplane &&
                sourceRepresentsAny(AIRPLANE_SLOT)
            ) {
                resolveFrozenAirplaneTarget()?.let { witness ->
                    val slot = witness.slotView
                    val targetLocation = IntArray(2)
                    slot.getLocationInWindow(targetLocation)
                    val collapsedEnd = logicalSourceX(source.width.toFloat())
                    val targetA = logicalTargetX(targetLocation[0].toFloat())
                    val targetB =
                        logicalTargetX(
                            (targetLocation[0] + slot.width).toFloat(),
                        )
                    result +=
                        CcTransition.ReservationSpan(
                            sourceLeft = collapsedEnd,
                            sourceRight = collapsedEnd,
                            targetLeft = min(targetA, targetB),
                            targetRight = maxOf(targetA, targetB),
                        )
                }
            }

            val presentation = PresentationStore.snapshot()
            if (
                currentSnapshot.model.centerIndicator !is CenterIndicator.NoSim &&
                presentation.statusIcons.noSimVisible &&
                presentation.statusIcons.noSimIcon != null &&
                sourceRepresentsAny(NO_SIM_SLOT)
            ) {
                resolveFrozenNoSimTarget()?.let { witness ->
                    val slot = witness.slotView
                    val targetLocation = IntArray(2)
                    slot.getLocationInWindow(targetLocation)
                    val collapsedEnd = logicalSourceX(source.width.toFloat())
                    val targetA = logicalTargetX(targetLocation[0].toFloat())
                    val targetB =
                        logicalTargetX(
                            (targetLocation[0] + slot.width).toFloat(),
                        )
                    result +=
                        CcTransition.ReservationSpan(
                            sourceLeft = collapsedEnd,
                            sourceRight = collapsedEnd,
                            targetLeft = min(targetA, targetB),
                            targetRight = maxOf(targetA, targetB),
                        )
                }
            }

            // Latent occupancy may move native peers through the reservation writer,
            // but it never becomes geometry authority. Keep every endpoint in
            // root space so reservation and rendering compare one coordinate frame.
            return result.takeIf { it.isNotEmpty() }
        }

        private fun resolveTarget(
            target: StatusPainter.TransitionTarget,
            preferredMobileSubId: Int?,
        ): TargetWitness? {
            val mobileSubId =
                preferredMobileSubId.takeIf {
                    target is StatusPainter.TransitionTarget.Slots &&
                        target.preferredSlots.any { slot ->
                            slot == MOBILE_SLOT || slot == STACKED_MOBILE_SLOT
                        }
                }
            val key =
                TargetCacheKey(
                    target = target,
                    mobileSubId = mobileSubId,
                )
            val opticalRequired =
                target is StatusPainter.TransitionTarget.Slots &&
                    target.preferredChildEntries.isNotEmpty()

            targetCache[key]
                ?.takeIf { witness ->
                    val slotStillSemantic =
                        target !is StatusPainter.TransitionTarget.Slots ||
                            witness.slotView.visibility == View.VISIBLE
                    witness.slotView.isAttachedToWindow &&
                        slotStillSemantic &&
                        (
                            !opticalRequired ||
                                witness.opticalView?.let(::isReliableSemanticTarget) == true ||
                                witness.fallbackBounds != null
                        )
                }
                ?.let { return it }

            val resolved =
                when (target) {
                    StatusPainter.TransitionTarget.BatteryIcon ->
                        TargetWitness(
                            slot = BATTERY_SLOT,
                            slotView = finalBattery,
                            opticalView = resolveBatteryIconTarget(finalBattery),
                            subscriptionId = null,
                            requiresOpticalGeometry = false,
                            fallbackBounds = null,
                            opticalSource = "battery",
                        ).takeIf { witness -> isUsableSlotView(witness.slotView) }

                    StatusPainter.TransitionTarget.BatteryNumber ->
                        findBatteryNumberTarget()

                    StatusPainter.TransitionTarget.BatteryChargingIcon ->
                        findChargingIconTarget()

                    is StatusPainter.TransitionTarget.Slots ->
                        target.preferredSlots.firstNotNullOfOrNull { slot ->
                            val slotRoot =
                                selectSlotView(
                                    candidates = slotViews(finalStatusIcons, slot),
                                    preferredMobileSubId =
                                        mobileSubId.takeIf {
                                            slot == MOBILE_SLOT ||
                                                slot == STACKED_MOBILE_SLOT
                                        },
                                ) ?: return@firstNotNullOfOrNull null
                            buildSlotWitness(
                                slot = slot,
                                slotRoot = slotRoot,
                                target = target,
                            )
                        }
                }

            if (resolved != null) {
                targetCache[key] = resolved
            } else {
                targetCache.remove(key)
            }
            return resolved
        }

        private fun buildSlotWitness(
            slot: String,
            slotRoot: View,
            target: StatusPainter.TransitionTarget.Slots,
        ): TargetWitness? {
            if (slotRoot.visibility != View.VISIBLE || !isUsableSlotView(slotRoot)) {
                return null
            }
            val singleIconOpticalRequired =
                slot == AIRPLANE_SLOT || slot == NO_SIM_SLOT
            val opticalRequired =
                target.preferredChildEntries.isNotEmpty() || singleIconOpticalRequired
            val nativeOptical =
                target.preferredChildEntries
                    .firstNotNullOfOrNull { entry ->
                        findDescendantByResourceEntry(
                            root = slotRoot,
                            entryName = entry,
                        )?.takeIf(::isReliableSemanticTarget)
                    }
            val compatibilityOptical =
                if (nativeOptical == null) {
                    compatOpticalTarget(
                        slotRoot = slotRoot,
                        preferredChildEntries = target.preferredChildEntries,
                    )
                } else {
                    null
                }
            val singleIconOptical =
                if (
                    nativeOptical == null &&
                    compatibilityOptical == null &&
                    singleIconOpticalRequired
                ) {
                    singleIconOpticalTarget(slotRoot)
                } else {
                    null
                }
            val optical =
                nativeOptical ?: compatibilityOptical?.view ?: singleIconOptical
            val fallbackBounds =
                if (
                    target.preferredChildEntries.isNotEmpty() &&
                    optical == null
                ) {
                    CcTransition.semanticFallbackBounds(
                        preferredChildEntries = target.preferredChildEntries,
                        isRtl =
                            slotRoot.layoutDirection ==
                                View.LAYOUT_DIRECTION_RTL,
                    )
                } else {
                    null
                }
            if (opticalRequired && optical == null && fallbackBounds == null) {
                return null
            }
            return TargetWitness(
                slot = slot,
                slotView = slotRoot,
                opticalView = optical,
                subscriptionId = readMobileSubId(slotRoot),
                requiresOpticalGeometry = opticalRequired,
                fallbackBounds = fallbackBounds,
                opticalSource =
                    when {
                        nativeOptical != null -> "native"
                        compatibilityOptical != null -> compatibilityOptical.source
                        singleIconOptical != null -> "native-drawable"
                        fallbackBounds != null -> "slot-estimate"
                        else -> "slot"
                    },
                textWeight = resolveNativeTextWeight(optical),
                textStyle = resolveNativeTextStyle(optical),
            )
        }

        private fun frozenExtraMobileTargets(
            primary: TargetWitness?,
        ): List<TargetWitness> {
            frozenAdditionalMobileTargets?.let { return it }
            val resolved = extraMobileTargets(primary)
            frozenAdditionalMobileTargets = resolved
            return resolved
        }

        private fun extraMobileTargets(
            primary: TargetWitness?,
        ): List<TargetWitness> {
            val target =
                StatusPainter.TransitionTarget.Slots(
                    preferredSlots = listOf(MOBILE_SLOT, STACKED_MOBILE_SLOT),
                    preferredChildEntries = listOf("mobile_signal"),
                )
            val primarySubId = primary?.subscriptionId
            return target.preferredSlots
                .flatMap { slot ->
                    slotViews(finalStatusIcons, slot).map { view -> slot to view }
                }
                .asSequence()
                .filter { (_, view) ->
                    view !== primary?.slotView &&
                        view.visibility == View.VISIBLE &&
                        isUsableSlotView(view)
                }
                .mapNotNull { (slot, view) ->
                    buildSlotWitness(
                        slot = slot,
                        slotRoot = view,
                        target = target,
                    )
                }
                .filter { witness ->
                    val subId = witness.subscriptionId
                    subId != null &&
                        (primarySubId == null || subId != primarySubId)
                }
                .distinctBy { witness -> witness.subscriptionId }
                .toList()
        }

        private fun resolveFrozenAirplaneTarget(): TargetWitness? {
            if (airplaneTargetResolved) return frozenAirplaneTarget
            airplaneTargetResolved = true
            frozenAirplaneTarget =
                resolveTarget(
                    target =
                        StatusPainter.TransitionTarget.Slots(
                            preferredSlots = listOf(AIRPLANE_SLOT),
                        ),
                    preferredMobileSubId = null,
                )
            return frozenAirplaneTarget
        }

        private fun resolveFrozenNoSimTarget(): TargetWitness? {
            if (noSimTargetResolved) return frozenNoSimTarget
            noSimTargetResolved = true
            frozenNoSimTarget =
                resolveTarget(
                    target =
                        StatusPainter.TransitionTarget.Slots(
                            preferredSlots = listOf(NO_SIM_SLOT),
                        ),
                    preferredMobileSubId = null,
                )
            return frozenNoSimTarget
        }

        private fun resolveTargetGeometry(
            witness: TargetWitness,
            root: View,
            sourceGeometry: FloatArray,
            targetOpticalBounds: StatusPainter.TransitionNormalizedBounds?,
        ): FloatArray? {
            val opticalView = witness.opticalView
            if (targetOpticalBounds == null && !witness.preferFallbackGeometry) {
                val visualView = opticalView ?: witness.slotView
                val snapshot =
                    VisualSnapshot.resolveView(visualView)
                val visualSample =
                    snapshot?.let { sample(visualView, root) }
                if (snapshot != null && visualSample != null) {
                    val envelope = snapshot.envelope
                    CcTransition.componentGeometry(
                        parentGeometry = visualSample.geometry,
                        parentWidth = visualView.width,
                        parentHeight = visualView.height,
                        bounds =
                            StatusPainter.TransitionBounds(
                                left = envelope.left * visualView.width,
                                top = envelope.top * visualView.height,
                                right = envelope.right * visualView.width,
                                bottom = envelope.bottom * visualView.height,
                            ),
                    )?.let { return it }
                }
            }
            val resolvedTargetOpticalBounds = targetOpticalBounds
            if (opticalView != null) {
                if (opticalView is ImageView) {
                    imageDrawableGeometry(
                        image = opticalView,
                        root = root,
                        targetOpticalBounds = resolvedTargetOpticalBounds,
                    )?.let { return it }
                }
                val opticalSample = sample(opticalView, root)
                if (opticalSample != null) {
                    return resolvedTargetOpticalBounds
                        ?.let { bounds ->
                            CcTransition.componentGeometry(
                                parentGeometry = opticalSample.geometry,
                                parentWidth = opticalView.width,
                                parentHeight = opticalView.height,
                                bounds =
                                    StatusPainter.TransitionBounds(
                                        left = bounds.left * opticalView.width,
                                        top = bounds.top * opticalView.height,
                                        right = bounds.right * opticalView.width,
                                        bottom = bounds.bottom * opticalView.height,
                                    ),
                            )
                        }
                        ?: opticalSample.geometry
                }

                syntheticOpticalGeometry(
                    witness = witness,
                    root = root,
                    targetOpticalBounds = resolvedTargetOpticalBounds,
                )?.let { return it }

            }

            val slot = witness.slotView
            if (slot is ImageView) {
                imageDrawableGeometry(
                    image = slot,
                    root = root,
                    targetOpticalBounds = resolvedTargetOpticalBounds,
                )?.let { return it }
            }
            val slotSample = sample(slot, root) ?: return sourceGeometry
            val contentBounds =
                StatusPainter.TransitionBounds(
                    left = slot.paddingLeft.toFloat(),
                    top = slot.paddingTop.toFloat(),
                    right = (slot.width - slot.paddingRight).toFloat(),
                    bottom = (slot.height - slot.paddingBottom).toFloat(),
                )
            val targetBounds =
                witness.fallbackBounds
                    ?.let { fallback ->
                        StatusPainter.TransitionBounds(
                            left =
                                contentBounds.left +
                                    contentBounds.width * fallback.left,
                            top =
                                contentBounds.top +
                                    contentBounds.height * fallback.top,
                            right =
                                contentBounds.left +
                                    contentBounds.width * fallback.right,
                            bottom =
                                contentBounds.top +
                                    contentBounds.height * fallback.bottom,
                        )
                    }
                    ?: if (witness.requiresOpticalGeometry) {
                        return null
                    } else {
                        contentBounds
                    }
            return CcTransition.componentGeometry(
                parentGeometry = slotSample.geometry,
                parentWidth = slot.width,
                parentHeight = slot.height,
                bounds = targetBounds,
            ) ?: slotSample.geometry
        }

        private fun mobileTargetBars(
            witness: TargetWitness,
        ): List<StatusPainter.TransitionNormalizedBounds>? {
            val visualView = witness.opticalView ?: witness.slotView
            val snapshot =
                VisualSnapshot.resolveView(visualView)
                    ?: return null
            return snapshot
                .fourVerticalBarsWithinEnvelope()
                ?.map { bar ->
                    StatusPainter.TransitionNormalizedBounds(
                        left = bar.left,
                        top = bar.top,
                        right = bar.right,
                        bottom = bar.bottom,
                    )
                }
        }

        private fun imageDrawableGeometry(
            image: ImageView,
            root: View,
            targetOpticalBounds: StatusPainter.TransitionNormalizedBounds?,
        ): FloatArray? {
            val drawable = image.drawable ?: return null
            val imageSample = sample(image, root) ?: return null
            val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: return null
            val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: return null
            val bounds = drawable.bounds
            val frame =
                RectF(
                    if (bounds.width() > 0) bounds.left.toFloat() else 0f,
                    if (bounds.height() > 0) bounds.top.toFloat() else 0f,
                    if (bounds.width() > 0) bounds.right.toFloat() else intrinsicWidth.toFloat(),
                    if (bounds.height() > 0) bounds.bottom.toFloat() else intrinsicHeight.toFloat(),
                )
            image.imageMatrix.mapRect(frame)
            frame.offset(
                image.paddingLeft.toFloat(),
                image.paddingTop.toFloat(),
            )
            if (frame.width() <= 0f || frame.height() <= 0f) return null

            val optical = targetOpticalBounds
            val localBounds =
                if (optical != null) {
                    StatusPainter.TransitionBounds(
                        left = frame.left + optical.left * frame.width(),
                        top = frame.top + optical.top * frame.height(),
                        right = frame.left + optical.right * frame.width(),
                        bottom = frame.top + optical.bottom * frame.height(),
                    )
                } else {
                    StatusPainter.TransitionBounds(
                        left = frame.left,
                        top = frame.top,
                        right = frame.right,
                        bottom = frame.bottom,
                    )
                }
            return CcTransition.componentGeometry(
                parentGeometry = imageSample.geometry,
                parentWidth = image.width,
                parentHeight = image.height,
                bounds = localBounds,
            )
        }

        private fun syntheticOpticalGeometry(
            witness: TargetWitness,
            root: View,
            targetOpticalBounds: StatusPainter.TransitionNormalizedBounds?,
        ): FloatArray? {
            val image = witness.opticalView as? ImageView ?: return null
            val drawable = image.drawable ?: return null
            val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: return null
            val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: return null
            val slot = witness.slotView
            val slotSample = sample(slot, root) ?: return null
            val contentLeft = slot.paddingLeft.toFloat()
            val contentTop = slot.paddingTop.toFloat()
            val contentRight = (slot.width - slot.paddingRight).toFloat()
            val contentBottom = (slot.height - slot.paddingBottom).toFloat()
            val contentWidth = (contentRight - contentLeft).coerceAtLeast(1f)
            val contentHeight = (contentBottom - contentTop).coerceAtLeast(1f)
            val fit =
                min(
                    contentWidth / intrinsicWidth.toFloat(),
                    contentHeight / intrinsicHeight.toFloat(),
                ).coerceAtMost(1f)
            val frameWidth = intrinsicWidth * fit
            val frameHeight = intrinsicHeight * fit
            val localCenterX =
                if (image.left != 0 || image.right != 0) {
                    image.left + frameWidth / 2f
                } else {
                    (contentLeft + contentRight) / 2f
                }
            val localCenterY =
                if (image.top != 0 || image.bottom != 0) {
                    image.top + frameHeight / 2f
                } else {
                    (contentTop + contentBottom) / 2f
                }
            val optical = targetOpticalBounds
            val frameLeft = localCenterX - frameWidth / 2f
            val frameTop = localCenterY - frameHeight / 2f
            val localBounds =
                if (optical != null) {
                    StatusPainter.TransitionBounds(
                        left = frameLeft + optical.left * frameWidth,
                        top = frameTop + optical.top * frameHeight,
                        right = frameLeft + optical.right * frameWidth,
                        bottom = frameTop + optical.bottom * frameHeight,
                    )
                } else {
                    StatusPainter.TransitionBounds(
                        left = frameLeft,
                        top = frameTop,
                        right = frameLeft + frameWidth,
                        bottom = frameTop + frameHeight,
                    )
                }
            return CcTransition.componentGeometry(
                parentGeometry = slotSample.geometry,
                parentWidth = slot.width,
                parentHeight = slot.height,
                bounds = localBounds,
            )
        }

        private fun findChargingIconTarget(): TargetWitness? {
            val chargingView =
                readViewField(finalBattery, "mBatteryChargingView") as? ImageView
                    ?: return null
            val drawable = chargingView.drawable ?: return null
            if (
                !chargingView.isAttachedToWindow ||
                chargingView.width <= 0 ||
                chargingView.height <= 0 ||
                drawable.intrinsicWidth <= 0 ||
                drawable.intrinsicHeight <= 0
            ) {
                return null
            }
            return TargetWitness(
                slot = BATTERY_CHARGING_SLOT,
                slotView = chargingView,
                opticalView = chargingView,
                subscriptionId = null,
                requiresOpticalGeometry = true,
                fallbackBounds = null,
                opticalSource = "battery-charging-view",
            )
        }

        private fun findBatteryNumberTarget(): TargetWitness? {
            val digitalView =
                readViewField(finalBattery, "mBatteryDigitalView")
                    ?: findDescendantByResourceEntry(finalBattery, "battery_icon_container")
            val visibleBatteryBody =
                resolveBatteryIconTarget(finalBattery)
                    ?.takeIf { view ->
                        view.visibility == View.VISIBLE &&
                            view.width > 0 &&
                            view.height > 0
                    }
            val legacyIconView = readViewField(finalBattery, "mBatteryIconView")
            val expected =
                currentSnapshot.model.batteryPercent
                    .coerceIn(0, 100)
                    .function function function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }()

            val textView =
                digitalView
                    ?.let(::findBatteryNumberTextView)
                    ?: findBatteryNumberTextView(finalBattery)

            // Best case: the native percentage TextView itself is laid out.
            if (
                textView != null &&
                textView.width > 0 &&
                textView.height > 0
            ) {
                val bounds = textViewBatteryNumberBounds(textView)
                if (bounds != null) {
                    return TargetWitness(
                        slot = BATTERY_NUMBER_SLOT,
                        slotView = textView,
                        opticalView = null,
                        subscriptionId = null,
                        requiresOpticalGeometry = true,
                        fallbackBounds = bounds,
                        opticalSource = "battery-number-text-layout",
                        textWeight = batteryNumberTypefaceWeight(textView.paint),
                        textStyle = captureTextStyle(textView.paint),
                        preferFallbackGeometry = true,
                    )
                }
            }

            // HyperOS hollow-battery presentation keeps the semantic percentage
            // TextView at 0x0 while a visible MiuiHollowBatteryMeterIconView owns
            // the final battery body. Prefer the visible body's own text Paint
            // when available, because it is the closest native geometry authority.
            if (visibleBatteryBody != null) {
                val nativeBodyPaint = resolveBatteryNumberPaint(visibleBatteryBody)
                if (nativeBodyPaint != null) {
                    val bounds =
                        centeredBatteryTextBounds(
                            view = visibleBatteryBody,
                            paint = nativeBodyPaint,
                            text = expected,
                        )
                    if (bounds != null) {
                        return TargetWitness(
                            slot = BATTERY_NUMBER_SLOT,
                            slotView = visibleBatteryBody,
                            opticalView = null,
                            subscriptionId = null,
                            requiresOpticalGeometry = true,
                            fallbackBounds = bounds,
                            opticalSource = "battery-number-hollow-paint",
                            textWeight = batteryNumberTypefaceWeight(nativeBodyPaint),
                            textStyle = captureTextStyle(nativeBodyPaint),
                            preferFallbackGeometry = true,
                        )
                    }
                }

                // The exact target also exposes battery_percentage_view even
                // when its parent has 0x0 layout. Its Paint still carries the
                // native number typography (textSize/typeface). Reuse that
                // typography but anchor it inside the visible hollow battery.
                if (textView != null) {
                    val bounds =
                        centeredBatteryTextBounds(
                            view = visibleBatteryBody,
                            paint = textView.paint,
                            text = expected,
                        )
                    if (bounds != null) {
                        return TargetWitness(
                            slot = BATTERY_NUMBER_SLOT,
                            slotView = visibleBatteryBody,
                            opticalView = null,
                            subscriptionId = null,
                            requiresOpticalGeometry = true,
                            fallbackBounds = bounds,
                            opticalSource = "battery-number-text-metrics-on-hollow",
                            textWeight = batteryNumberTypefaceWeight(textView.paint),
                            textStyle = captureTextStyle(textView.paint),
                            preferFallbackGeometry = true,
                        )
                    }
                }
            }

            // Compatibility fallback for variants where mBatteryIconView itself
            // remains the visible/measured presentation authority.
            val legacyIcon =
                legacyIconView
                    ?.takeIf { view -> view.width > 0 && view.height > 0 }
                    ?: return null
            val paint = resolveBatteryNumberPaint(legacyIcon) ?: return null
            val bounds =
                centeredBatteryTextBounds(
                    view = legacyIcon,
                    paint = paint,
                    text = expected,
                ) ?: return null
            return TargetWitness(
                slot = BATTERY_NUMBER_SLOT,
                slotView = legacyIcon,
                opticalView = null,
                subscriptionId = null,
                requiresOpticalGeometry = true,
                fallbackBounds = bounds,
                opticalSource = "battery-number-legacy-icon-paint",
                textWeight = batteryNumberTypefaceWeight(paint),
                textStyle = captureTextStyle(paint),
                preferFallbackGeometry = true,
            )
        }

        private fun findBatteryNumberTextView(root: View): TextView? {
            val expected =
                currentSnapshot.model.batteryPercent
                    .coerceIn(0, 100)
                    .function function function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }()
            data class Candidate(
                val view: TextView,
                val score: Int,
            )
            val candidates = ArrayList<Candidate>()
            fun collect(view: View, depth: Int) {
                if (depth > NUMBER_PROBE_MAX_DEPTH) return
                if (view is TextView && view.visibility == View.VISIBLE) {
                    val value = view.text?.function function function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }().orEmpty()
                    val digits = value.filter(Char::isDigit)
                    val entry =
                        ParticipantAccess.resourceEntryName(view)
                            ?.lowercase()
                            .orEmpty()
                    var score = 0
                    if (digits == expected) score += 12
                    if (entry.contains("percentage_view")) score += 10
                    if (entry.contains("percent")) score += 6
                    if (entry.contains("digit")) score += 5
                    if (entry.contains("battery")) score += 3
                    if (entry.contains("text")) score += 1
                    if (view.width > 0 && view.height > 0) score += 4
                    if ((view.layout?.lineCount ?: 0) > 0) score += 2
                    if (score > 0) {
                        candidates += Candidate(view, score)
                    }
                }
                val group = view as? ViewGroup ?: return
                for (index in 0 until group.childCount) {
                    collect(group.getChildAt(index), depth + 1)
                }
            }
            collect(root, 0)
            return candidates
                .maxWithOrNull(
                    compareBy<Candidate> { candidate -> candidate.score }
                        .thenBy { candidate -> candidate.view.width * candidate.view.height },
                )
                ?.view
        }

        private fun textViewBatteryNumberBounds(
            view: TextView,
        ): StatusPainter.TransitionNormalizedBounds? {
            val layout = view.layout ?: return null
            if (layout.lineCount <= 0 || view.width <= 0 || view.height <= 0) return null
            val text = view.text?.function function function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }().orEmpty()
            if (text.isEmpty()) return null
            val rect = Rect()
            view.paint.getTextBounds(text, 0, text.length, rect)
            if (rect.width() <= 0 || rect.height() <= 0) return null
            val baseline =
                view.extendedPaddingTop + layout.getLineBaseline(0)
            val left =
                view.compoundPaddingLeft + layout.getLineLeft(0) + rect.left
            val top = baseline + rect.top
            val right = left + rect.width()
            val bottom = baseline + rect.bottom
            val contentLeft = view.paddingLeft.toFloat()
            val contentTop = view.paddingTop.toFloat()
            val contentWidth =
                (view.width - view.paddingLeft - view.paddingRight)
                    .coerceAtLeast(0)
            val contentHeight =
                (view.height - view.paddingTop - view.paddingBottom)
                    .coerceAtLeast(0)
            return normalizedBounds(
                left = left - contentLeft,
                top = top.toFloat() - contentTop,
                right = right - contentLeft,
                bottom = bottom.toFloat() - contentTop,
                width = contentWidth,
                height = contentHeight,
            )
        }

        private fun batteryNumberTypefaceWeight(paint: Paint): Int? =
            runCatching { paint.typeface?.weight }
                .getOrNull()
                ?.takeIf { it > 0 }

        private fun captureTextStyle(
            paint: Paint,
        ): StatusPainter.TransitionTextStyle =
            StatusPainter.TransitionTextStyle(
                typeface = paint.typeface,
                weight = batteryNumberTypefaceWeight(paint),
                fakeBoldText = paint.isFakeBoldText,
                textScaleX = paint.textScaleX,
                textSkewX = paint.textSkewX,
                letterSpacing =
                    runCatching { paint.letterSpacing }
                        .getOrDefault(0f),
                strokeWidth = paint.strokeWidth,
                paintStyle = paint.style,
            )

        private fun resolveNativeTextStyle(
            view: View?,
        ): StatusPainter.TransitionTextStyle? {
            if (view == null) return null
            if (view is TextView) {
                return captureTextStyle(view.paint)
            }
            val descendant =
                if (view is ViewGroup) {
                    sequence {
                        for (index in 0 until view.childCount) {
                            val child = view.getChildAt(index)
                            if (child is TextView) yield(child)
                        }
                    }.firstOrNull()
                } else {
                    null
                }
            return descendant?.let { captureTextStyle(it.paint) }
        }

        private fun resolveNativeTextWeight(view: View?): Int? {
            if (view == null) return null
            if (view is TextView) {
                return runCatching { view.paint.typeface?.weight ?: view.typeface?.weight }
                    .getOrNull()
                    ?.takeIf { it > 0 }
            }
            val descendant =
                if (view is ViewGroup) {
                    sequence {
                        for (index in 0 until view.childCount) {
                            val child = view.getChildAt(index)
                            if (child is TextView) yield(child)
                        }
                    }.firstOrNull()
                } else {
                    null
                }
            return descendant
                ?.let { text ->
                    runCatching { text.paint.typeface?.weight ?: text.typeface?.weight }
                        .getOrNull()
                }
                ?.takeIf { it > 0 }
        }

        private fun resolveBatteryNumberPaint(view: View): Paint? =
            generateSequence<Class<*>>(view.javaClass) { clazz -> clazz.superclass }
                .flatMap { clazz -> clazz.declaredFields.asSequence() }
                .filter { field -> Paint::class.java.isAssignableFrom(field.type) }
                .mapNotNull { field ->
                    runCatching {
                        field.isAccessible = true
                        field.get(view) as? Paint
                    }.getOrNull()
                }
                .filter { paint -> paint.textSize > NUMBER_MIN_TEXT_SIZE_PX }
                .maxByOrNull { paint ->
                    paint.textSize +
                        (if (paint.textAlign == Paint.Align.CENTER) 8f else 0f) +
                        (if (paint.typeface != null) 4f else 0f)
                }

        private fun centeredBatteryTextBounds(
            view: View,
            paint: Paint,
            text: String,
        ): StatusPainter.TransitionNormalizedBounds? {
            if (view.width <= 0 || view.height <= 0 || text.isEmpty()) return null
            val rect = Rect()
            paint.getTextBounds(text, 0, text.length, rect)
            if (rect.width() <= 0 || rect.height() <= 0) return null
            val contentLeft = view.paddingLeft.toFloat()
            val contentTop = view.paddingTop.toFloat()
            val contentRight = (view.width - view.paddingRight).toFloat()
            val contentBottom = (view.height - view.paddingBottom).toFloat()
            if (contentRight <= contentLeft || contentBottom <= contentTop) return null
            val centerX = (contentLeft + contentRight) / 2f
            val centerY = (contentTop + contentBottom) / 2f
            val contentWidth =
                (view.width - view.paddingLeft - view.paddingRight)
                    .coerceAtLeast(0)
            val contentHeight =
                (view.height - view.paddingTop - view.paddingBottom)
                    .coerceAtLeast(0)
            return normalizedBounds(
                left = centerX - rect.width() / 2f - contentLeft,
                top = centerY - rect.height() / 2f - contentTop,
                right = centerX + rect.width() / 2f - contentLeft,
                bottom = centerY + rect.height() / 2f - contentTop,
                width = contentWidth,
                height = contentHeight,
            )
        }

        private fun normalizedBounds(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            width: Int,
            height: Int,
        ): StatusPainter.TransitionNormalizedBounds? {
            if (width <= 0 || height <= 0) return null
            val l = (left / width).coerceIn(0f, 1f)
            val t = (top / height).coerceIn(0f, 1f)
            val r = (right / width).coerceIn(0f, 1f)
            val b = (bottom / height).coerceIn(0f, 1f)
            if (r <= l || b <= t) return null
            return StatusPainter.TransitionNormalizedBounds(
                left = l,
                top = t,
                right = r,
                bottom = b,
            )
        }

        private fun resolveBatteryNumberProbe(battery: View): String {
            val digitalView = readViewField(battery, "mBatteryDigitalView")
            val iconView = readViewField(battery, "mBatteryIconView")
            val hollowView = resolveBatteryIconTarget(battery)

            fun viewToken(view: View): String {
                val entry =
                    ParticipantAccess.resourceEntryName(view)
                        ?: "no-id"
                val base =
                    view.javaClass.simpleName + ":" + entry +
                        ":" + view.width + "x" + view.height +
                        ":v=" + view.visibility
                return if (view is TextView) {
                    val weight =
                        runCatching { view.typeface?.weight }
                            .getOrNull()
                            ?: -1
                    base +
                        ":text=" + view.text.function function function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }().replace("|", "/") +
                        ":textSize=" + view.textSize +
                        ":weight=" + weight +
                        ":style=" + (view.typeface?.style ?: -1)
                } else {
                    base
                }
            }

            val descendants = ArrayList<String>()
            fun collect(view: View, depth: Int) {
                if (descendants.size >= NUMBER_PROBE_MAX_VIEWS) return
                val entry =
                    ParticipantAccess.resourceEntryName(view)
                        ?.lowercase()
                        .orEmpty()
                if (
                    view is TextView ||
                    view === digitalView ||
                    view === iconView ||
                    entry.contains("battery") ||
                    entry.contains("percent") ||
                    entry.contains("digit") ||
                    entry.contains("text")
                ) {
                    descendants += viewToken(view)
                }
                if (depth >= NUMBER_PROBE_MAX_DEPTH) return
                val group = view as? ViewGroup ?: return
                for (index in 0 until group.childCount) {
                    collect(group.getChildAt(index), depth + 1)
                    if (descendants.size >= NUMBER_PROBE_MAX_VIEWS) return
                }
            }
            collect(battery, 0)

            fun paintFields(view: View?): List<String> =
                view
                    ?.let { owner ->
                        generateSequence<Class<*>>(owner.javaClass) { clazz -> clazz.superclass }
                            .flatMap { clazz -> clazz.declaredFields.asSequence() }
                            .filter { field ->
                                Paint::class.java.isAssignableFrom(field.type)
                            }
                            .mapNotNull { field ->
                                runCatching {
                                    field.isAccessible = true
                                    val paint = field.get(owner) as? Paint ?: return@runCatching null
                                    val weight = batteryNumberTypefaceWeight(paint) ?: -1
                                    field.name +
                                        ":textSize=" + paint.textSize +
                                        ":weight=" + weight +
                                        ":style=" + (paint.typeface?.style ?: -1) +
                                        ":align=" + paint.textAlign.name
                                }.getOrNull()
                            }
                            .filterNotNull()
                            .take(NUMBER_PROBE_MAX_PAINTS)
                            .toList()
                    }
                    .orEmpty()

            return "{" +
                "digital=" + (digitalView?.let(::viewToken) ?: "none") +
                ";icon=" + (iconView?.let(::viewToken) ?: "none") +
                ";hollow=" + (hollowView?.let(::viewToken) ?: "none") +
                ";views=[" + descendants.joinToString(",") + "]" +
                ";iconPaints=[" + paintFields(iconView).joinToString(",") + "]" +
                ";hollowPaints=[" + paintFields(hollowView).joinToString(",") + "]" +
                "}"
        }


        private fun resolveBatteryIconTarget(battery: View): View? {
            val fieldNames =
                if (readIntField(battery, "mBatteryStyle") == 1) {
                    listOf("mHollowBatteryIconView", "mBatteryIconView")
                } else {
                    listOf("mBatteryIconView", "mHollowBatteryIconView")
                }
            return fieldNames.firstNotNullOfOrNull { fieldName ->
                readViewField(
                    target = battery,
                    fieldName = fieldName,
                )
            }
                ?: findDescendantByResourceEntry(battery, "battery_icon")
                ?: findDescendantByResourceEntry(battery, "battery_icon_container")
        }

        private fun readViewField(
            target: Any,
            fieldName: String,
        ): View? =
            generateSequence(target.javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { field -> field.name == fieldName }
                }
                .firstOrNull()
                ?.let { field ->
                    runCatching {
                        field.isAccessible = true
                        field.get(target) as? View
                    }.getOrNull()
                }

        private fun readIntField(
            target: Any,
            fieldName: String,
        ): Int? =
            generateSequence(target.javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { field -> field.name == fieldName }
                }
                .firstOrNull()
                ?.let { field ->
                    runCatching {
                        field.isAccessible = true
                        field.getInt(target)
                    }.getOrNull()
                }

        private fun singleIconOpticalTarget(root: View): ImageView? {
            val candidates = ArrayList<ImageView>(2)
            fun collect(view: View) {
                if (
                    view is ImageView &&
                    view.visibility == View.VISIBLE &&
                    view.width > 0 &&
                    view.height > 0 &&
                    view.drawable?.let { drawable ->
                        drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0
                    } == true
                ) {
                    candidates += view
                }
                val group = view as? ViewGroup ?: return
                for (index in 0 until group.childCount) {
                    collect(group.getChildAt(index))
                    if (candidates.size > 1) return
                }
            }
            collect(root)
            return candidates.singleOrNull()
        }

        private fun compatOpticalTarget(
            slotRoot: View,
            preferredChildEntries: List<String>,
        ): CompatibilityOpticalTarget? {
            if (!preferredChildEntries.contains("mobile_signal")) return null
            val signalContainer =
                findDescendantByResourceEntry(
                    root = slotRoot,
                    entryName = "mobile_signal_container",
                ) as? ViewGroup ?: return null

            data class Candidate(
                val view: View,
                val snapshot: VisualSnapshot.Snapshot,
                val score: Float,
            )

            val candidates = ArrayList<Candidate>()
            for (index in 0 until signalContainer.childCount) {
                val candidate = signalContainer.getChildAt(index)
                if (!isReliableSemanticTarget(candidate)) continue
                val snapshot =
                    VisualSnapshot.resolveView(candidate)
                        ?: continue
                if (snapshot.components.isEmpty()) continue
                val area =
                    snapshot.envelope.width *
                        snapshot.envelope.height
                val componentWeight =
                    1f +
                        snapshot.components.size
                            .coerceAtMost(8) * 0.05f
                candidates +=
                    Candidate(
                        view = candidate,
                        snapshot = snapshot,
                        score = area * componentWeight,
                    )
            }

            val best =
                candidates.maxByOrNull { candidate -> candidate.score }
                    ?: return null
            return CompatibilityOpticalTarget(
                view = best.view,
                source =
                    "visual-snapshot:" +
                        best.snapshot.topology.name.lowercase(),
            )
        }

        private fun readMobileSubId(view: View): Int? {
            if (mobileSubIdCache.containsKey(view)) {
                return mobileSubIdCache[view]
            }
            val resolved =
                generateSequence<Class<*>>(view.javaClass) { clazz -> clazz.superclass }
                    .mapNotNull { clazz ->
                        clazz.declaredFields.firstOrNull { field -> field.name == "subId" }
                    }
                    .firstOrNull()
                    ?.let { field ->
                        runCatching {
                            field.isAccessible = true
                            field.getInt(view)
                        }.getOrNull()
                    }
            mobileSubIdCache[view] = resolved
            return resolved
        }

        private fun selectSlotView(
            candidates: List<View>,
            preferredMobileSubId: Int?,
        ): View? {
            if (preferredMobileSubId != null) {
                candidates.firstOrNull { view ->
                    readMobileSubId(view) == preferredMobileSubId &&
                        view.visibility == View.VISIBLE &&
                        isUsableSlotView(view)
                }?.let { return it }
            }
            return candidates.firstOrNull { view ->
                view.visibility == View.VISIBLE && isUsableSlotView(view)
            }
        }

        private fun isReliableSemanticTarget(view: View): Boolean =
            view.visibility == View.VISIBLE &&
                view.isAttachedToWindow &&
                view.width > 0 &&
                view.height > 0

        private fun isUsableSlotView(view: View): Boolean =
            view.isAttachedToWindow &&
                view.width > 0 &&
                view.height > 0

        private fun findDescendantByResourceEntry(
            root: View,
            entryName: String,
        ): View? {
            if (ParticipantAccess.resourceEntryName(root) == entryName) {
                return root
            }
            val group = root as? ViewGroup ?: return null
            for (index in 0 until group.childCount) {
                findDescendantByResourceEntry(
                    root = group.getChildAt(index),
                    entryName = entryName,
                )?.let { return it }
            }
            return null
        }

        private fun endpointAlpha(view: View): Float {
            var current: View? = view
            var alpha = 1f
            val rootView = view.rootView
            while (current != null && current !== rootView) {
                if (current.visibility != View.VISIBLE) return 0f
                alpha *= current.alpha
                current = current.parent as? View
            }
            return alpha.coerceIn(0f, 1f)
        }

        private fun sample(
            view: View,
            root: View,
        ): Sample? {
            if (!view.isAttachedToWindow || view.width <= 0 || view.height <= 0) {
                return null
            }
            val matrix = Matrix()
            view.transformMatrixToGlobal(matrix)
            root.transformMatrixToLocal(matrix)
            val values = FloatArray(9)
            matrix.getValues(values)
            return Sample(
                geometry =
                    floatArrayOf(
                        ((values[Matrix.MSCALE_X] * view.width) +
                            (values[Matrix.MSKEW_X] * view.height)) / 2f +
                            values[Matrix.MTRANS_X],
                        ((values[Matrix.MSKEW_Y] * view.width) +
                            (values[Matrix.MSCALE_Y] * view.height)) / 2f +
                            values[Matrix.MTRANS_Y],
                        values[Matrix.MSCALE_X] * view.width,
                        values[Matrix.MSKEW_Y] * view.width,
                        values[Matrix.MSKEW_X] * view.height,
                        values[Matrix.MSCALE_Y] * view.height,
                    ),
            )
        }

        private fun matrixForBoundsGeometry(
            geometry: FloatArray,
            bounds: StatusPainter.TransitionBounds,
        ): Matrix? {
            if (
                geometry.size != 6 ||
                bounds.width <= 0f ||
                bounds.height <= 0f
            ) {
                return null
            }
            val values = FloatArray(9)
            values[Matrix.MSCALE_X] = geometry[2] / bounds.width
            values[Matrix.MSKEW_X] = geometry[4] / bounds.height
            values[Matrix.MTRANS_X] =
                geometry[0] -
                    values[Matrix.MSCALE_X] * bounds.centerX -
                    values[Matrix.MSKEW_X] * bounds.centerY
            values[Matrix.MSKEW_Y] = geometry[3] / bounds.width
            values[Matrix.MSCALE_Y] = geometry[5] / bounds.height
            values[Matrix.MTRANS_Y] =
                geometry[1] -
                    values[Matrix.MSKEW_Y] * bounds.centerX -
                    values[Matrix.MSCALE_Y] * bounds.centerY
            values[Matrix.MPERSP_0] = 0f
            values[Matrix.MPERSP_1] = 0f
            values[Matrix.MPERSP_2] = 1f
            return Matrix().apply { setValues(values) }
        }

        private fun matrixForGeometry(
            geometry: FloatArray,
            width: Int,
            height: Int,
        ): Matrix? {
            if (geometry.size != 6 || width <= 0 || height <= 0) return null
            val values = FloatArray(9)
            values[Matrix.MSCALE_X] = geometry[2] / width
            values[Matrix.MSKEW_X] = geometry[4] / height
            values[Matrix.MTRANS_X] =
                geometry[0] - ((geometry[2] + geometry[4]) / 2f)
            values[Matrix.MSKEW_Y] = geometry[3] / width
            values[Matrix.MSCALE_Y] = geometry[5] / height
            values[Matrix.MTRANS_Y] =
                geometry[1] - ((geometry[3] + geometry[5]) / 2f)
            values[Matrix.MPERSP_0] = 0f
            values[Matrix.MPERSP_1] = 0f
            values[Matrix.MPERSP_2] = 1f
            return Matrix().apply { setValues(values) }
        }

        private data class TargetCacheKey(
            val target: StatusPainter.TransitionTarget,
            val mobileSubId: Int?,
        )

        private data class Sample(
            val geometry: FloatArray,
        )

        private data class CarrierFrames(
            val source: FloatArray,
            val current: FloatArray,
        )

        private data class FrozenSourceGeometry(
            val width: Int,
            val height: Int,
            val geometry: FloatArray,
            val motionCarrierGeometry: FloatArray,
            val motionCarrierWidth: Int,
            val representedSlots: Set<String>,
            val source: String,
        )

        private data class CompatibilityOpticalTarget(
            val view: View,
            val source: String,
        )

        private data class TargetWitness(
            val slot: String,
            val slotView: View,
            val opticalView: View?,
            val subscriptionId: Int?,
            val requiresOpticalGeometry: Boolean,
            val fallbackBounds: StatusPainter.TransitionNormalizedBounds?,
            val opticalSource: String,
            val textWeight: Int? = null,
            val textStyle: StatusPainter.TransitionTextStyle? = null,
            val preferFallbackGeometry: Boolean = false,
        ) {
            val summary: String
                get() =
                    slot +
                        ":" +
                        slotView.width +
                        "x" +
                        slotView.height +
                        "/sub=" +
                        (subscriptionId ?: -1) +
                        "/opt=" +
                        opticalSource +
                        "/weight=" +
                        (textWeight ?: -1) +
                        "/fakeBold=" +
                        (textStyle?.fakeBoldText ?: false) +
                        "/scaleX=" +
                        (textStyle?.textScaleX ?: -1f) +
                        "/stroke=" +
                        (textStyle?.strokeWidth ?: -1f) +
                        "/style=" +
                        (textStyle?.paintStyle?.name ?: "none") +
                        ":" +
                        (
                            opticalView?.let { view ->
                                (ParticipantAccess.resourceEntryName(view)
                                    ?: view.javaClass.simpleName) +
                                    ":" + view.width + "x" + view.height
                            } ?: "none"
                        )
        }

        private data class MaskState(
            val view: WeakReference<View>,
            val nativeClip: Rect?,
            val appliedClip: Rect,
        )

        companion object {
            fun create(
                root: ViewGroup,
                fakeRoot: ViewGroup,
                finalRoot: ViewGroup,
                sourceSnapshot: CcSession.TransitionSourceSnapshot,
                steadySourceWitness: TransitionSourceWitness?,
                steadySourceLabel: String,
            ): Session? {
                val fakeStatusIcons =
                    uniqueDescendant(fakeRoot, STATUS_ICON_CONTAINER_CLASS)
                        ?: return null
                val finalStatusIcons =
                    uniqueDescendant(finalRoot, STATUS_ICON_CONTAINER_CLASS)
                        ?: return null
                val finalBattery =
                    uniqueDescendantView(finalRoot, BATTERY_VIEW_CLASS_NAME)
                        ?: return null
                val frozenSource =
                    steadySourceWitness
                        ?.takeIf { witness ->
                            witness.logicalWidthPx > 0 &&
                                witness.logicalHeightPx > 0 &&
                                witness.renderView.isAttachedToWindow &&
                                witness.positionHost.width >= witness.logicalWidthPx &&
                                witness.positionHost.height > 0 &&
                                witness.positionHost.isAttachedToWindow &&
                                witness.motionCarrier.width > 0 &&
                                witness.motionCarrier.height > 0 &&
                                witness.motionCarrier.isAttachedToWindow
                        }
                        ?.let { witness ->
                            val basisGeometry =
                                sampleGeometry(
                                    view = witness.renderView,
                                    root = root,
                                    localLeftPx = witness.logicalLeftPx,
                                    localTopPx = witness.logicalTopPx,
                                    widthPx = witness.logicalWidthPx,
                                    heightPx = witness.logicalHeightPx,
                                ) ?: return@let null
                            val positionHostGeometry =
                                sampleGeometry(
                                    view = witness.positionHost,
                                    root = root,
                                ) ?: return@let null
                            val positionGeometry =
                                CcTransition.endAnchoredSlotGeometry(
                                    hostGeometry = positionHostGeometry,
                                    hostWidth = witness.positionHost.width,
                                    hostHeight = witness.positionHost.height,
                                    slotWidth = witness.logicalWidthPx,
                                    isRtl =
                                        witness.positionHost.layoutDirection ==
                                            View.LAYOUT_DIRECTION_RTL,
                                ) ?: return@let null
                            val motionCarrierGeometry =
                                sampleGeometry(
                                    view = witness.motionCarrier,
                                    root = root,
                                ) ?: return@let null
                            FrozenSourceGeometry(
                                width = witness.logicalWidthPx,
                                height = witness.logicalHeightPx,
                                geometry =
                                    CcTransition.composeSourceGeometry(
                                        positionAuthority = positionGeometry,
                                        basisAuthority = basisGeometry,
                                    ),
                                motionCarrierGeometry = motionCarrierGeometry,
                                motionCarrierWidth = witness.motionCarrier.width,
                                representedSlots = witness.representedSlots.toSet(),
                                source =
                                    steadySourceLabel +
                                        "-steady-end-slot+" +
                                        if (
                                            witness.positionHost.rootView ===
                                            root.rootView
                                        ) {
                                            "same-root"
                                        } else {
                                            "cross-root"
                                        },
                            )
                        }
                return Session(
                    root = root,
                    fakeRoot = fakeRoot,
                    finalRoot = finalRoot,
                    sourceView = sourceSnapshot.view,
                    sourceAnchor = sourceSnapshot.anchorView,
                    sourceSnapshot = sourceSnapshot,
                    frozenSource = frozenSource,
                    fakeStatusIcons = fakeStatusIcons,
                    finalStatusIcons = finalStatusIcons,
                    finalBattery = finalBattery,
                )
            }

            private fun sampleGeometry(
                view: View,
                root: View,
                localLeftPx: Int = 0,
                localTopPx: Int = 0,
                widthPx: Int = view.width,
                heightPx: Int = view.height,
            ): FloatArray? {
                if (
                    widthPx <= 0 ||
                    heightPx <= 0 ||
                    !view.isAttachedToWindow ||
                    !root.isAttachedToWindow
                ) {
                    return null
                }
                val matrix = Matrix()
                view.transformMatrixToGlobal(matrix)
                val sourceRoot = view.rootView
                val targetRoot = root.rootView
                if (sourceRoot !== targetRoot) {
                    val sourceWindowOrigin = windowOriginOnScreen(sourceRoot) ?: return null
                    val targetWindowOrigin = windowOriginOnScreen(targetRoot) ?: return null
                    matrix.postTranslate(
                        (sourceWindowOrigin.first - targetWindowOrigin.first).toFloat(),
                        (sourceWindowOrigin.second - targetWindowOrigin.second).toFloat(),
                    )
                }
                root.transformMatrixToLocal(matrix)
                val values = FloatArray(9)
                matrix.getValues(values)
                val centerX = localLeftPx + widthPx / 2f
                val centerY = localTopPx + heightPx / 2f
                return floatArrayOf(
                    values[Matrix.MSCALE_X] * centerX +
                        values[Matrix.MSKEW_X] * centerY +
                        values[Matrix.MTRANS_X],
                    values[Matrix.MSKEW_Y] * centerX +
                        values[Matrix.MSCALE_Y] * centerY +
                        values[Matrix.MTRANS_Y],
                    values[Matrix.MSCALE_X] * widthPx,
                    values[Matrix.MSKEW_Y] * widthPx,
                    values[Matrix.MSKEW_X] * heightPx,
                    values[Matrix.MSCALE_Y] * heightPx,
                )
            }

            private fun windowOriginOnScreen(view: View): Pair<Int, Int>? {
                if (!view.isAttachedToWindow) return null
                return runCatching {
                    val screen = IntArray(2)
                    val window = IntArray(2)
                    view.getLocationOnScreen(screen)
                    view.getLocationInWindow(window)
                    (screen[0] - window[0]) to (screen[1] - window[1])
                }.getOrNull()
            }

            private fun slotViews(
                group: ViewGroup,
                slot: String,
            ): List<View> {
                val result = ArrayList<View>()
                for (index in 0 until group.childCount) {
                    val child = group.getChildAt(index)
                    if (ParticipantAccess.slotOf(child) == slot) {
                        result += child
                    }
                }
                return result
            }

            private fun uniqueDescendant(
                root: ViewGroup,
                className: String,
            ): ViewGroup? =
                uniqueDescendantView(root, className) as? ViewGroup

            private fun uniqueDescendantView(
                root: ViewGroup,
                className: String,
            ): View? {
                var found: View? = null
                val queue = ArrayDeque<ViewGroup>()
                queue.add(root)
                while (queue.isNotEmpty()) {
                    val parent = queue.removeFirst()
                    for (index in 0 until parent.childCount) {
                        val child = parent.getChildAt(index)
                        if (child.javaClass.name == className) {
                            if (found != null && found !== child) return null
                            found = child
                        }
                        if (child is ViewGroup) {
                            queue.add(child)
                        }
                    }
                }
                return found
            }
        }
    }

    private class TransitionDrawable(
        session: Session,
    ) : Drawable() {
        private val session = WeakReference(session)

        override fun draw(canvas: Canvas) {
            session.get()?.draw(canvas)
        }

        override fun setAlpha(alpha: Int) = Unit

        override fun setColorFilter(colorFilter: ColorFilter?) = Unit

        @Deprecated("Deprecated in Android")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
