package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.math.floor

internal object SystemUiPanelTransitionSource {
    const val CONTROL_CENTER_RUNTIME_HOOK_COUNT = 4
    const val CONTROL_CENTER_DIAGNOSTIC_HOOK_COUNT = 0
    const val HOOK_COUNT =
        CONTROL_CENTER_RUNTIME_HOOK_COUNT +
            CONTROL_CENTER_DIAGNOSTIC_HOOK_COUNT

    private const val CONTROL_CENTER_CLASS =
        "com.miui.systemui.controlcenter.container.ControlCenterExpandControllerDelegate"
    private const val CONTROL_CENTER_EXPANSION_METHOD = "onExpansionChanged"
    private const val CONTROL_CENTER_APPEARANCE_METHOD = "onAppearanceChanged"
    private const val CONTROL_CENTER_VISIBLE_METHOD = "onVisibleChanged"
    private const val CONTROL_CENTER_HEADER_CALLBACK_CLASS =
        "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController\$controlCenterCallback\$1"
    private const val CONTROL_CENTER_HEADER_CLASS =
        "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController"
    private const val COMBINED_HEADER_CLASS =
        "com.android.systemui.controlcenter.shade.CombinedHeaderController"
    private const val CONTROL_CENTER_FAKE_STATUS_BAR_CLASS =
        "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons"
    private const val CC_FAKE_STATUS_BAR_ICONS_CLASS =
        "com.android.systemui.controlcenter.header.CcFakeStatusBarIcons"
    private const val DAGGER_LAZY_CLASS = "dagger.Lazy"
    private const val STATUS_BAR_ANCHOR_CLASS =
        "com.android.systemui.controlcenter.shade.StatusBarAnchorBounds"

    private const val CONTROL_CENTER_EXPANSION_HOOK_ID =
        "combinedstatus.panel.control-center.expansion"
    private const val CONTROL_CENTER_APPEARANCE_HOOK_ID =
        "combinedstatus.panel.control-center.appearance"
    private const val CONTROL_CENTER_VISIBLE_HOOK_ID =
        "combinedstatus.panel.control-center.visible"
    private const val CONTROL_CENTER_FAKE_ATTACHED_HOOK_ID =
        "combinedstatus.panel.control-center.fake-attached"

    private var controlProbe = ProbeState()
    @Volatile
    private var controlCenterHomeEligible: Boolean? = null
    private var controlAnchorContract: ControlCenterAnchorContract? = null
    private var controlHeaderRef = WeakReference<Any>(null)
    private var fakeIslandContractRootRef = WeakReference<ViewGroup>(null)

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onUpdate: ((Update) -> Unit)? = null,
        onFakePresentationAttached: ((ViewGroup) -> Unit)? = null,
        onRuntimeFailure: ((Throwable) -> Unit)? = null,
        onEvent: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { false },
        includeControlCenterDiagnostics: Boolean = true,
    ): List<HookHandle> {
        val controlClass =
            Class.forName(CONTROL_CENTER_CLASS, false, classLoader)
        val controlVisibleMethod =
            controlClass
                .getDeclaredMethod(
                    CONTROL_CENTER_VISIBLE_METHOD,
                    Boolean::class.javaPrimitiveType,
                )
                .apply { isAccessible = true }
        val fakeStatusBarClass =
            Class.forName(
                CONTROL_CENTER_FAKE_STATUS_BAR_CLASS,
                false,
                classLoader,
            )
        val fakeAttachedMethod =
            fakeStatusBarClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "onAttachedToWindow" &&
                        method.parameterCount == 0
                }
                ?.apply { isAccessible = true }
                ?: error("control-center-fake-attached-method-missing")
        val headerCallbackClass =
            Class.forName(
                CONTROL_CENTER_HEADER_CALLBACK_CLASS,
                false,
                classLoader,
            )
        val controlExpansionMethod =
            headerCallbackClass
                .getDeclaredMethod(
                    CONTROL_CENTER_EXPANSION_METHOD,
                    Float::class.javaPrimitiveType,
                )
                .apply { isAccessible = true }
        val controlAppearanceMethod =
            headerCallbackClass
                .getDeclaredMethod(
                    CONTROL_CENTER_APPEARANCE_METHOD,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                )
                .apply { isAccessible = true }

        controlAnchorContract =
            ControlCenterAnchorContract.resolve(
                classLoader = classLoader,
                delegateClass = controlClass,
            )

        val handles =
            ArrayList<HookHandle>(
                expectedHookCount(includeControlCenterDiagnostics),
            )
        try {
            handles +=
                module
                    .hook(controlVisibleMethod)
                    .setId(CONTROL_CENTER_VISIBLE_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val visible = chain.getArg(0) as? Boolean
                            val result = chain.proceed()
                            controlCenterHomeEligible =
                                controlCenterAllowsHome(visible)
                            val controlCenterPresentationHost =
                                if (visible == true) {
                                    resolveControlCenterFakePresentationHost(chain.thisObject)
                                } else {
                                    null
                                }
                            val controlCenterSourceScene =
                                if (visible == true) {
                                    resolveControlCenterSourceScene(chain.thisObject)
                                } else {
                                    null
                                }
                            val transitionEndpoints =
                                if (visible == true) {
                                    resolveControlCenterTransitionEndpoints(chain.thisObject)
                                } else {
                                    null
                                }
                            val update =
                                Update(
                                    source = Source.CONTROL_CENTER,
                                    fraction = null,
                                    expanded = null,
                                    tracking = null,
                                    visible = visible,
                                    controlCenterPresentationHost = controlCenterPresentationHost,
                                    controlCenterSourceScene = controlCenterSourceScene,
                                    controlCenterTransitionEndpoints = transitionEndpoints,
                                    controlCenterBatteryIslandActive =
                                        if (visible == true) {
                                            resolveControlCenterBatteryIslandActive(chain.thisObject)
                                        } else {
                                            null
                                        },
                                )
                            dispatchRuntimeCallback(
                                callback = onUpdate?.let { callback -> { callback(update) } },
                                onFailure = onRuntimeFailure,
                            )
                            emitDiagnostic(
                                update = update,
                                onEvent = onEvent,
                                isProbeEnabled = isProbeEnabled,
                            )
                            result
                        },
                    )

            handles +=
                module
                    .hook(fakeAttachedMethod)
                    .setId(CONTROL_CENTER_FAKE_ATTACHED_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val result = chain.proceed()
                            val root = chain.thisObject as? ViewGroup
                            if (root != null) {
                                dispatchRuntimeCallback(
                                    callback =
                                        onFakePresentationAttached?.let { callback ->
                                            { callback(root) }
                                        },
                                    onFailure = onRuntimeFailure,
                                )
                                if (onEvent != null && isProbeEnabled()) {
                                    dispatchRuntimeCallback(
                                        callback = {
                                            onEvent(
                                                "controlCenterFakeLifecycle attached=true " +
                                                    "root=" + root.javaClass.name +
                                                    " attachedToWindow=" +
                                                    root.isAttachedToWindow +
                                                    " readOnly=true nativeGeometryWrites=0",
                                            )
                                            describeFakeIslandContractOnce(root)?.let(onEvent)
                                        },
                                    )
                                }
                            }
                            result
                        },
                    )

            // Control Center visibility is the only panel runtime authority.
            // Notification Shade inherits the native Home carrier lifecycle.
            if (controlCenterHomeEligible == null) {
                controlCenterHomeEligible = true
            }

            handles +=
                module
                    .hook(controlExpansionMethod)
                    .setId(CONTROL_CENTER_EXPANSION_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val fraction =
                                nativeFraction(
                                    (chain.getArg(0) as? Number)?.toFloat(),
                                )
                            val transitionEndpoints =
                                controlAnchorContract
                                    ?.transitionEndpointsFromCallback(chain.thisObject)
                            val batteryIslandActive =
                                controlAnchorContract
                                    ?.batteryIslandFromCallback(chain.thisObject)
                            val preNativeUpdate =
                                Update(
                                    source = Source.CONTROL_CENTER,
                                    fraction = fraction,
                                    expanded = null,
                                    tracking = null,
                                    visible = null,
                                    controlCenterTransitionEndpoints = transitionEndpoints,
                                    controlCenterBatteryIslandActive = batteryIslandActive,
                                )
                            // Reservation/source projection must be committed before
                            // HyperOS consumes this expansion sample. Drawing still
                            // happens on the normal traversal after the native callback.
                            dispatchRuntimeCallback(
                                callback =
                                    onUpdate?.let { callback ->
                                        { callback(preNativeUpdate) }
                                    },
                                onFailure = onRuntimeFailure,
                            )
                            val result = chain.proceed()
                            val anchorSnapshot =
                                if (
                                    onEvent != null &&
                                    isProbeEnabled() &&
                                    shouldCaptureControlAnchor(fraction)
                                ) {
                                    controlAnchorContract
                                        ?.snapshotFromCallback(chain.thisObject)
                                } else {
                                    null
                                }
                            emitDiagnostic(
                                update =
                                    preNativeUpdate.copy(
                                        controlCenterAnchor = anchorSnapshot,
                                        homeMotion =
                                            if (anchorSnapshot != null) {
                                                SystemUiIslandMotionSource.currentOwnerSnapshot()
                                            } else {
                                                null
                                            },
                                    ),
                                onEvent = onEvent,
                                isProbeEnabled = isProbeEnabled,
                            )
                            result
                        },
                    )

            handles +=
                module
                    .hook(controlAppearanceMethod)
                    .setId(CONTROL_CENTER_APPEARANCE_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val first = chain.getArg(0) as? Boolean
                            val second = chain.getArg(1) as? Boolean
                            val result = chain.proceed()
                            val update =
                                Update(
                                    source = Source.CONTROL_CENTER,
                                    fraction = null,
                                    expanded = null,
                                    tracking = null,
                                    visible = null,
                                    controlCenterAppearance = first,
                                    controlCenterAppearanceAnimated = second,
                                    controlCenterTransitionEndpoints =
                                        controlAnchorContract
                                            ?.transitionEndpointsFromCallback(chain.thisObject),
                                    controlCenterBatteryIslandActive =
                                        controlAnchorContract
                                            ?.batteryIslandFromCallback(chain.thisObject),
                                )
                            dispatchRuntimeCallback(
                                callback = onUpdate?.let { callback -> { callback(update) } },
                                onFailure = onRuntimeFailure,
                            )
                            if (onEvent != null && isProbeEnabled()) {
                                dispatchRuntimeCallback(
                                    callback = {
                                        onEvent(
                                            appearanceDiagnostic(
                                                first = first,
                                                second = second,
                                                snapshot =
                                                    controlAnchorContract
                                                        ?.snapshotFromCallback(chain.thisObject),
                                                fakePresentation =
                                                    controlAnchorContract
                                                        ?.fakePresentationFromCallback(
                                                            chain.thisObject,
                                                        ),
                                            ),
                                        )
                                    },
                                )
                            }
                            result
                        },
                    )

            return handles
        } catch (error: Throwable) {
            handles.asReversed().forEach { handle ->
                runCatching { handle.unhook() }
            }
            controlCenterHomeEligible = false
            throw error
        }
    }

    fun resetRuntimeState() {
        synchronized(this) {
            controlProbe = ProbeState()
            controlCenterHomeEligible = null
            controlAnchorContract = null
            controlHeaderRef = WeakReference(null)
            fakeIslandContractRootRef = WeakReference(null)
        }
    }

    @Synchronized
    private fun describeFakeIslandContractOnce(root: ViewGroup): String? {
        if (fakeIslandContractRootRef.get() === root) return null
        fakeIslandContractRootRef = WeakReference(root)

        val fieldNames =
            listOf(
                "slot",
                "visibleState",
                "inIslandState",
                "beforeInIslandState",
                "islandChanged",
                "supportAnim",
                "forceAppear",
                "layoutTranslationX",
            )
        val views = ArrayList<View>()
        fun collect(
            view: View,
            depth: Int,
        ) {
            if (views.size >= 64 || depth > 6) return
            views += view
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    collect(view.getChildAt(index), depth + 1)
                    if (views.size >= 64) return
                }
            }
        }
        collect(root, 0)

        fun hierarchy(type: Class<*>): List<Class<*>> {
            val result = ArrayList<Class<*>>()
            var current: Class<*>? = type
            while (current != null && current != Any::class.java) {
                result += current
                current = current.superclass
            }
            return result
        }

        fun field(
            type: Class<*>,
            name: String,
        ): Field? =
            hierarchy(type)
                .firstNotNullOfOrNull { owner ->
                    runCatching {
                        owner.getDeclaredField(name).apply { isAccessible = true }
                    }.getOrNull()
                }

        val contractViews =
            views.mapNotNull { view ->
                val resolvedFields =
                    fieldNames.mapNotNull { name ->
                        field(view.javaClass, name)?.let { resolved -> name to resolved }
                    }
                val methodNames =
                    hierarchy(view.javaClass)
                        .flatMap { owner -> owner.declaredMethods.asList() }
                        .map { method -> method.name }
                        .filter { name ->
                            name.contains("island", ignoreCase = true) ||
                                name.contains("forceAppear", ignoreCase = true) ||
                                name.contains("visibleState", ignoreCase = true)
                        }
                        .distinct()
                        .sorted()
                if (resolvedFields.none { (name, _) -> name == "inIslandState" } &&
                    methodNames.none { name -> name.contains("island", ignoreCase = true) }
                ) {
                    return@mapNotNull null
                }

                val location = IntArray(2)
                view.getLocationOnScreen(location)
                val values =
                    resolvedFields.joinToString(",") { (name, resolved) ->
                        val value =
                            runCatching { resolved.get(view) }
                                .getOrNull()
                                ?.toString()
                                ?: "unavailable"
                        name + "=" + value
                    }
                view.javaClass.name +
                    "(x=" + location[0] +
                    ",y=" + location[1] +
                    ",w=" + view.width +
                    ",h=" + view.height +
                    ",fields={" + values +
                    "},methods=[" + methodNames.take(16).joinToString(",") +
                    "])"
            }
                .distinct()
                .take(8)

        return "controlCenterFakeIslandContract root=" + root.javaClass.name +
            " candidates=" + contractViews.size +
            " entries=[" + contractViews.joinToString("|") +
            "] readOnly=true bounded=true nativeGeometryWrites=0"
    }

    internal fun dispatchRuntimeCallback(
        callback: (() -> Unit)?,
        onFailure: ((Throwable) -> Unit)? = null,
    ): Boolean {
        callback ?: return true
        return try {
            callback()
            true
        } catch (error: Throwable) {
            if (error is VirtualMachineError || error is ThreadDeath) {
                throw error
            }
            if (onFailure != null) {
                try {
                    onFailure(error)
                } catch (failure: Throwable) {
                    if (failure is VirtualMachineError || failure is ThreadDeath) {
                        throw failure
                    }
                }
            }
            false
        }
    }

    internal fun nativeFraction(value: Float?): Float? =
        value?.takeIf { it.isFinite() }

    internal fun expectedHookCount(includeControlCenterDiagnostics: Boolean): Int =
        CONTROL_CENTER_RUNTIME_HOOK_COUNT +
            if (includeControlCenterDiagnostics) CONTROL_CENTER_DIAGNOSTIC_HOOK_COUNT else 0

    internal fun appearanceDiagnostic(
        first: Boolean?,
        second: Boolean?,
        snapshot: ControlCenterAnchorSnapshot?,
        fakePresentation: ControlCenterFakePresentationSnapshot? = null,
    ): String =
        "controlCenterAppearance first=" + (first ?: "unknown") +
            " second=" + (second ?: "unknown") +
            " expanding=" + (snapshot?.controlCenterExpanding ?: "unknown") +
            " addBatteryIsland=" + (snapshot?.addBatteryIsland ?: "unknown") +
            " batteryWidthDiff=" + (snapshot?.batteryWidthDiff ?: "unknown") +
            " fakePresentation=" + (fakePresentation?.summary ?: "unknown") +
            " readOnly=true nativeGeometryWrites=0"

    fun currentControlCenterHomeEligibility(): Boolean? =
        controlCenterHomeEligible

    @Synchronized
    fun restoreControlCenterHomeEligibility(eligible: Boolean?) {
        if (eligible != null) {
            controlCenterHomeEligible = eligible
        }
    }

    internal fun controlCenterAllowsHome(visible: Boolean?): Boolean =
        visible == false

    internal fun diagnosticBucket(fraction: Float?): Int? =
        fraction?.let { rawValue ->
            val value = rawValue.coerceIn(0f, 1f)
            floor(value * DIAGNOSTIC_BUCKETS)
                .toInt()
                .coerceIn(0, DIAGNOSTIC_BUCKETS)
        }

    internal fun isBoundaryDiagnosticBucket(bucket: Int?): Boolean =
        bucket == 0 || bucket == 1 || bucket == 7 || bucket == 8

    @Synchronized
    private fun shouldCaptureControlAnchor(fraction: Float?): Boolean {
        val bucket = diagnosticBucket(fraction)
        return isBoundaryDiagnosticBucket(bucket) &&
            bucket != controlProbe.bucket
    }

    private fun resolveControlCenterHeader(delegate: Any?): Any? {
        delegate ?: return null
        val contract = controlAnchorContract ?: return null
        return controlHeaderRef.get()
            ?: contract.resolveHeader(delegate)?.also { resolved ->
                controlHeaderRef = WeakReference(resolved)
            }
    }

    private fun resolveControlCenterFakePresentationHost(
        delegate: Any?,
    ): ViewGroup? {
        val contract = controlAnchorContract ?: return null
        val header = resolveControlCenterHeader(delegate) ?: return null
        return contract.fakePresentationRoot(header)
    }

    private fun captureControlCenterAnchor(delegate: Any?): ControlCenterAnchorSnapshot? {
        val contract = controlAnchorContract ?: return null
        val header = resolveControlCenterHeader(delegate) ?: return null
        return contract.snapshot(header)
    }

    private fun resolveControlCenterTransitionEndpoints(
        delegate: Any?,
    ): ControlCenterTransitionEndpoints? {
        val contract = controlAnchorContract ?: return null
        val header =
            resolveControlCenterHeader(delegate)
                ?: return null
        return contract.transitionEndpoints(header)
    }

    private fun resolveControlCenterBatteryIslandActive(
        delegate: Any?,
    ): Boolean? {
        val contract = controlAnchorContract ?: return null
        val header =
            resolveControlCenterHeader(delegate)
                ?: return null
        return contract.batteryIslandActive(header)
    }

    private fun resolveControlCenterSourceScene(delegate: Any?): CombinedStatusSourceScene {
        val contract = controlAnchorContract ?: return CombinedStatusSourceScene.UNKNOWN
        val header =
            resolveControlCenterHeader(delegate)
                ?: return CombinedStatusSourceScene.UNKNOWN
        val realSystemIcons =
            contract.realSystemIcons(header)
                ?: return CombinedStatusSourceScene.UNKNOWN
        return classifyControlCenterSourceScene(
            homeIdentityMatches =
                SystemUiHomePresentationOwner.ownsBatteryContainer(realSystemIcons),
            structuralScene =
                SystemUiSceneStateSource.steadySourceScene(realSystemIcons),
        )
    }

    internal fun classifyControlCenterSourceScene(
        homeIdentityMatches: Boolean,
        structuralScene: CombinedStatusSourceScene,
    ): CombinedStatusSourceScene =
        if (homeIdentityMatches) {
            CombinedStatusSourceScene.HOME
        } else {
            structuralScene
        }

    @Synchronized
    private fun emitDiagnostic(
        update: Update,
        onEvent: ((String) -> Unit)?,
        isProbeEnabled: () -> Boolean,
    ) {
        if (onEvent == null || !isProbeEnabled()) {
            return
        }
        val probe = controlProbe
        val bucket = diagnosticBucket(update.fraction)
        val changed =
            (bucket != null && bucket != probe.bucket) ||
                (update.expanded != null && update.expanded != probe.expanded) ||
                (update.tracking != null && update.tracking != probe.tracking) ||
                (update.visible != null && update.visible != probe.visible)
        if (!changed) {
            return
        }
        if (bucket != null) {
            probe.bucket = bucket
        }
        if (update.expanded != null) {
            probe.expanded = update.expanded
        }
        if (update.tracking != null) {
            probe.tracking = update.tracking
        }
        if (update.visible != null) {
            probe.visible = update.visible
        }
        val anchorSummary =
            update.controlCenterAnchor?.let { snapshot ->
                " controlAnchor=" + snapshot.summary
            }.orEmpty()
        val homeMotionSummary =
            update.homeMotion?.let { snapshot ->
                " homeMotion=" + snapshot.summary
            }.orEmpty()
        val sourceSceneSummary =
            update.controlCenterSourceScene?.let { sourceScene ->
                " sourceScene=" + sourceScene.name
            }.orEmpty()
        val batteryIslandSummary =
            update.controlCenterBatteryIslandActive?.let { active ->
                " batteryIsland=" + active
            }.orEmpty()
        dispatchRuntimeCallback(
            callback = {
                onEvent(
                    "panelTransition source=" + update.source.logName +
                        " fraction=" + (update.fraction ?: "none") +
                        " bucket=" + (bucket ?: probe.bucket) + "/" + DIAGNOSTIC_BUCKETS +
                        " expanded=" + (update.expanded ?: probe.expanded ?: "none") +
                        " tracking=" + (update.tracking ?: probe.tracking ?: "none") +
                        " visible=" + (update.visible ?: probe.visible ?: "none") +
                        anchorSummary +
                        homeMotionSummary +
                        sourceSceneSummary +
                        batteryIslandSummary +
                        " authority=hyperos-native-callback" +
                        " nativeGeometryWrites=0",
                )
            },
        )
    }

    internal data class Update(
        val source: Source,
        val fraction: Float?,
        val expanded: Boolean?,
        val tracking: Boolean?,
        val visible: Boolean?,
        val controlCenterPresentationHost: ViewGroup? = null,
        val controlCenterSourceScene: CombinedStatusSourceScene? = null,
        val controlCenterAppearance: Boolean? = null,
        val controlCenterAppearanceAnimated: Boolean? = null,
        val controlCenterTransitionEndpoints: ControlCenterTransitionEndpoints? = null,
        val controlCenterBatteryIslandActive: Boolean? = null,
        val controlCenterAnchor: ControlCenterAnchorSnapshot? = null,
        val homeMotion: SystemUiIslandMotionSource.OwnerSnapshot? = null,
    )

    internal enum class Source(
        val logName: String,
    ) {
        CONTROL_CENTER("control-center"),
    }

    internal data class ControlCenterTransitionEndpoints(
        val fakeRoot: ViewGroup,
        val finalRoot: ViewGroup,
    )

    internal data class ControlCenterFakePresentationSnapshot(
        val rootClassName: String?,
        val rootVisibility: Int?,
        val rootAlpha: Float?,
        val rootWidth: Int?,
        val rootHeight: Int?,
        val statusBarAreaClassName: String?,
        val statusBarAreaVisibility: Int?,
        val statusBarAreaAlpha: Float?,
        val statusBarAreaWidth: Int?,
        val statusBarAreaHeight: Int?,
    ) {
        val summary: String
            get() =
                "{root=" + (rootClassName ?: "unknown") +
                    "(v=" + (rootVisibility ?: "unknown") +
                    ",a=" + (rootAlpha ?: "unknown") +
                    ",w=" + (rootWidth ?: "unknown") +
                    ",h=" + (rootHeight ?: "unknown") +
                    "),statusBarArea=" + (statusBarAreaClassName ?: "unknown") +
                    "(v=" + (statusBarAreaVisibility ?: "unknown") +
                    ",a=" + (statusBarAreaAlpha ?: "unknown") +
                    ",w=" + (statusBarAreaWidth ?: "unknown") +
                    ",h=" + (statusBarAreaHeight ?: "unknown") +
                    ")}"
    }

    internal data class ControlCenterAnchorSnapshot(
        val systemIconsX: Int?,
        val systemIconsWidth: Int?,
        val statusIconsX: Int?,
        val statusIconsWidth: Int?,
        val batteryWidth: Int?,
        val realSystemIconsWidth: Int?,
        val normalStatusBarTranslationX: Int?,
        val normalStatusIconsTranslationX: Int?,
        val batteryWidthDiff: Int?,
        val addBatteryIsland: Boolean?,
        val controlCenterExpanding: Boolean?,
    ) {
        val summary: String
            get() =
                "{" +
                    "systemIconsX=" + (systemIconsX ?: "unknown") +
                    ",systemIconsWidth=" + (systemIconsWidth ?: "unknown") +
                    ",statusIconsX=" + (statusIconsX ?: "unknown") +
                    ",statusIconsWidth=" + (statusIconsWidth ?: "unknown") +
                    ",batteryWidth=" + (batteryWidth ?: "unknown") +
                    ",realSystemIconsWidth=" + (realSystemIconsWidth ?: "unknown") +
                    ",normalStatusBarTx=" + (normalStatusBarTranslationX ?: "unknown") +
                    ",normalStatusIconsTx=" + (normalStatusIconsTranslationX ?: "unknown") +
                    ",batteryWidthDiff=" + (batteryWidthDiff ?: "unknown") +
                    ",addBatteryIsland=" + (addBatteryIsland ?: "unknown") +
                    ",expanding=" + (controlCenterExpanding ?: "unknown") +
                    "}"
    }

    private class ControlCenterAnchorContract(
        private val callbackClass: Class<*>,
        private val callbacksField: Field,
        private val callbackOuterField: Field,
        private val statusBarAnchorField: Field,
        private val normalStatusBarTranslationXField: Field,
        private val normalStatusIconsTranslationXField: Field,
        private val batteryWidthDiffField: Field,
        private val addBatteryIslandField: Field,
        private val controlCenterExpandingField: Field,
        private val realSystemIconsField: Field,
        private val headerControllerField: Field,
        private val lazyGetMethod: Method,
        private val controlCenterFakeStatusBarField: Field,
        private val controlCenterStatusBarField: Field,
        private val fakeDelegateField: Field,
        private val fakeStatusBarAreaField: Field,
        private val systemIconsLocationField: Field,
        private val systemIconsWidthField: Field,
        private val statusIconsLocationField: Field,
        private val statusIconsWidthField: Field,
        private val batteryWidthField: Field,
    ) {
        fun resolveHeader(delegate: Any): Any? {
            val callbacks =
                runCatching { callbacksField.get(delegate) as? Iterable<*> }
                    .getOrNull()
                    ?: return null
            val callback =
                callbacks.firstOrNull { candidate ->
                    candidate != null && callbackClass.isInstance(candidate)
                } ?: return null
            return runCatching { callbackOuterField.get(callback) }.getOrNull()
        }

        fun realSystemIcons(header: Any): ViewGroup? =
            runCatching { realSystemIconsField.get(header) as? ViewGroup }
                .getOrNull()

        fun fakePresentationRoot(header: Any): ViewGroup? =
            fakeStatusBar(header)

        fun transitionEndpoints(
            header: Any,
        ): ControlCenterTransitionEndpoints? {
            val fakeRoot = fakeStatusBar(header) ?: return null
            val finalRoot = finalStatusBar(header) ?: return null
            return ControlCenterTransitionEndpoints(
                fakeRoot = fakeRoot,
                finalRoot = finalRoot,
            )
        }

        fun transitionEndpointsFromCallback(
            callback: Any?,
        ): ControlCenterTransitionEndpoints? {
            val header = headerFromCallback(callback) ?: return null
            return transitionEndpoints(header)
        }

        fun batteryIslandFromCallback(callback: Any?): Boolean? {
            val header = headerFromCallback(callback) ?: return null
            return batteryIslandActive(header)
        }

        fun batteryIslandActive(header: Any): Boolean? =
            readBoolean(addBatteryIslandField, header)

        fun snapshotFromCallback(callback: Any?): ControlCenterAnchorSnapshot? {
            val header = headerFromCallback(callback) ?: return null
            return snapshot(header)
        }

        fun fakePresentationFromCallback(
            callback: Any?,
        ): ControlCenterFakePresentationSnapshot? {
            val header = headerFromCallback(callback) ?: return null
            val fakeRoot = fakeStatusBar(header) ?: return null
            val delegate =
                runCatching { fakeDelegateField.get(fakeRoot) }
                    .getOrNull()
                    ?: return null
            val statusBarArea =
                runCatching { fakeStatusBarAreaField.get(delegate) as? View }
                    .getOrNull()
            return ControlCenterFakePresentationSnapshot(
                rootClassName = fakeRoot.javaClass.name,
                rootVisibility = fakeRoot.visibility,
                rootAlpha = fakeRoot.alpha,
                rootWidth = fakeRoot.width,
                rootHeight = fakeRoot.height,
                statusBarAreaClassName = statusBarArea?.javaClass?.name,
                statusBarAreaVisibility = statusBarArea?.visibility,
                statusBarAreaAlpha = statusBarArea?.alpha,
                statusBarAreaWidth = statusBarArea?.width,
                statusBarAreaHeight = statusBarArea?.height,
            )
        }

        private fun headerFromCallback(callback: Any?): Any? =
            callback
                ?.let { candidate ->
                    runCatching { callbackOuterField.get(candidate) }
                        .getOrNull()
                }

        private fun combinedHeader(header: Any): Any? {
            val lazy =
                runCatching { headerControllerField.get(header) }
                    .getOrNull()
                    ?: return null
            return runCatching { lazyGetMethod.invoke(lazy) }.getOrNull()
        }

        private fun fakeStatusBar(header: Any): ViewGroup? {
            val combinedHeader = combinedHeader(header) ?: return null
            return runCatching {
                controlCenterFakeStatusBarField.get(combinedHeader) as? ViewGroup
            }.getOrNull()
        }

        private fun finalStatusBar(header: Any): ViewGroup? {
            val combinedHeader = combinedHeader(header) ?: return null
            return runCatching {
                controlCenterStatusBarField.get(combinedHeader) as? ViewGroup
            }.getOrNull()
        }

        fun snapshot(header: Any): ControlCenterAnchorSnapshot? {
            val anchor =
                runCatching { statusBarAnchorField.get(header) }
                    .getOrNull()
                    ?: return null
            val realSystemIcons =
                runCatching { realSystemIconsField.get(header) as? View }
                    .getOrNull()
            val systemLocation =
                runCatching { systemIconsLocationField.get(anchor) as? IntArray }
                    .getOrNull()
            val statusLocation =
                runCatching { statusIconsLocationField.get(anchor) as? IntArray }
                    .getOrNull()
            return ControlCenterAnchorSnapshot(
                systemIconsX = systemLocation?.getOrNull(0),
                systemIconsWidth = readInt(systemIconsWidthField, anchor),
                statusIconsX = statusLocation?.getOrNull(0),
                statusIconsWidth = readInt(statusIconsWidthField, anchor),
                batteryWidth = readInt(batteryWidthField, anchor),
                realSystemIconsWidth = realSystemIcons?.width,
                normalStatusBarTranslationX =
                    readInt(normalStatusBarTranslationXField, header),
                normalStatusIconsTranslationX =
                    readInt(normalStatusIconsTranslationXField, header),
                batteryWidthDiff = readInt(batteryWidthDiffField, header),
                addBatteryIsland = readBoolean(addBatteryIslandField, header),
                controlCenterExpanding =
                    readBoolean(controlCenterExpandingField, header),
            )
        }

        private fun readInt(field: Field, target: Any): Int? =
            runCatching { field.getInt(target) }.getOrNull()

        private fun readBoolean(field: Field, target: Any): Boolean? =
            runCatching { field.getBoolean(target) }.getOrNull()

        companion object {
            fun resolve(
                classLoader: ClassLoader,
                delegateClass: Class<*>,
            ): ControlCenterAnchorContract? =
                runCatching {
                    val callbackClass =
                        Class.forName(
                            CONTROL_CENTER_HEADER_CALLBACK_CLASS,
                            false,
                            classLoader,
                        )
                    val headerClass =
                        Class.forName(
                            CONTROL_CENTER_HEADER_CLASS,
                            false,
                            classLoader,
                        )
                    val combinedHeaderClass =
                        Class.forName(
                            COMBINED_HEADER_CLASS,
                            false,
                            classLoader,
                        )
                    val fakeStatusBarClass =
                        Class.forName(
                            CONTROL_CENTER_FAKE_STATUS_BAR_CLASS,
                            false,
                            classLoader,
                        )
                    val fakeStatusBarIconsClass =
                        Class.forName(
                            CC_FAKE_STATUS_BAR_ICONS_CLASS,
                            false,
                            classLoader,
                        )
                    val lazyClass =
                        Class.forName(
                            DAGGER_LAZY_CLASS,
                            false,
                            classLoader,
                        )
                    val anchorClass =
                        Class.forName(
                            STATUS_BAR_ANCHOR_CLASS,
                            false,
                            classLoader,
                        )
                    ControlCenterAnchorContract(
                        callbackClass = callbackClass,
                        callbacksField =
                            delegateClass.getDeclaredField("callbacks").accessible(),
                        callbackOuterField =
                            callbackClass.getDeclaredField("this\$0").accessible(),
                        statusBarAnchorField =
                            headerClass.getDeclaredField("statusBarAnchor").accessible(),
                        normalStatusBarTranslationXField =
                            headerClass
                                .getDeclaredField("normalControlStatusBarTranslationX")
                                .accessible(),
                        normalStatusIconsTranslationXField =
                            headerClass
                                .getDeclaredField("normalControlStatusIconsTranslationX")
                                .accessible(),
                        batteryWidthDiffField =
                            headerClass.getDeclaredField("batteryWidthDiff").accessible(),
                        addBatteryIslandField =
                            headerClass.getDeclaredField("isAddBatteryIsland").accessible(),
                        controlCenterExpandingField =
                            headerClass
                                .getDeclaredField("isControlCenterExpanding")
                                .accessible(),
                        realSystemIconsField =
                            headerClass.getDeclaredField("realSystemIcons").accessible(),
                        headerControllerField =
                            headerClass.getDeclaredField("headerController").accessible(),
                        lazyGetMethod =
                            lazyClass.getDeclaredMethod("get").apply {
                                isAccessible = true
                            },
                        controlCenterFakeStatusBarField =
                            combinedHeaderClass
                                .getDeclaredField("controlCenterFakeStatusBar")
                                .accessible(),
                        controlCenterStatusBarField =
                            combinedHeaderClass
                                .getDeclaredField("controlCenterStatusBar")
                                .accessible(),
                        fakeDelegateField =
                            fakeStatusBarClass.getDeclaredField("delegate").accessible(),
                        fakeStatusBarAreaField =
                            fakeStatusBarIconsClass.getDeclaredField("statusBarArea").accessible(),
                        systemIconsLocationField =
                            anchorClass
                                .getDeclaredField("systemIconsLocationOnScreen")
                                .accessible(),
                        systemIconsWidthField =
                            anchorClass.getDeclaredField("systemIconsWidth").accessible(),
                        statusIconsLocationField =
                            anchorClass
                                .getDeclaredField("statusIconsLocationInWindow")
                                .accessible(),
                        statusIconsWidthField =
                            anchorClass.getDeclaredField("statusIconsWidth").accessible(),
                        batteryWidthField =
                            anchorClass.getDeclaredField("batteryWidth").accessible(),
                    )
                }.getOrNull()
        }
    }

    private fun Field.accessible(): Field =
        apply { isAccessible = true }

    private data class ProbeState(
        var bucket: Int = -1,
        var expanded: Boolean? = null,
        var tracking: Boolean? = null,
        var visible: Boolean? = null,
    )

    private const val DIAGNOSTIC_BUCKETS = 8
}
