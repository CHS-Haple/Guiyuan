package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object SysUiCcSource {
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

    private var probe = ProbeState()
    @Volatile
    private var homeEligible: Boolean? = null
    private var anchorContract: AnchorContract? = null
    private var headerRef = WeakReference<Any>(null)
    private var fakeIslandContractRootRef = WeakReference<ViewGroup>(null)

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onUpdate: ((Update) -> Unit)? = null,
        onFakePresentationAttached: ((ViewGroup) -> Unit)? = null,
        onRuntimeFailure: ((Throwable) -> Unit)? = null,
        onEvent: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { false },
        includeDiagnostics: Boolean = true,
    ): List<HookHandle> {
        val ccClass =
            Class.forName(CONTROL_CENTER_CLASS, false, classLoader)
        val visibleMethod =
            ccClass
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
        val expansionMethod =
            headerCallbackClass
                .getDeclaredMethod(
                    CONTROL_CENTER_EXPANSION_METHOD,
                    Float::class.javaPrimitiveType,
                )
                .apply { isAccessible = true }
        val appearanceMethod =
            headerCallbackClass
                .getDeclaredMethod(
                    CONTROL_CENTER_APPEARANCE_METHOD,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                )
                .apply { isAccessible = true }

        anchorContract =
            AnchorContract.resolve(
                classLoader = classLoader,
                delegateClass = ccClass,
            )

        val handles =
            ArrayList<HookHandle>(
                expectedHookCount(includeDiagnostics),
            )
        try {
            handles +=
                module
                    .hook(visibleMethod)
                    .setId(CONTROL_CENTER_VISIBLE_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val visible = chain.getArg(0) as? Boolean
                            val result = chain.proceed()
                            homeEligible =
                                allowsHome(visible)
                            val presentationHost =
                                if (visible == true) {
                                    resolveFakePresentationHost(chain.thisObject)
                                } else {
                                    null
                                }
                            val sourceScene =
                                if (visible == true) {
                                    resolveSourceScene(chain.thisObject)
                                } else {
                                    null
                                }
                            val transitionEndpoints =
                                if (visible == true) {
                                    resolveTransitionEndpoints(chain.thisObject)
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
                                    presentationHost = presentationHost,
                                    sourceScene = sourceScene,
                                    transitionEndpoints = transitionEndpoints,
                                    batteryIslandActive =
                                        if (visible == true) {
                                            resolveBatteryIslandActive(chain.thisObject)
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

            // Native visibility is the CC runtime authority.
            // Notification Shade keeps the native Home carrier lifecycle.
            if (homeEligible == null) {
                homeEligible = true
            }

            handles +=
                module
                    .hook(expansionMethod)
                    .setId(CONTROL_CENTER_EXPANSION_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val fraction =
                                nativeFraction(
                                    (chain.getArg(0) as? Number)?.toFloat(),
                                )
                            val transitionEndpoints =
                                anchorContract
                                    ?.transitionEndpointsFromCallback(chain.thisObject)
                            val batteryIslandActive =
                                anchorContract
                                    ?.batteryIslandFromCallback(chain.thisObject)
                            val preNativeUpdate =
                                Update(
                                    source = Source.CONTROL_CENTER,
                                    fraction = fraction,
                                    expanded = null,
                                    tracking = null,
                                    visible = null,
                                    transitionEndpoints = transitionEndpoints,
                                    batteryIslandActive = batteryIslandActive,
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
                            chain.proceed()
                        },
                    )

            handles +=
                module
                    .hook(appearanceMethod)
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
                                    appearance = first,
                                    appearanceAnimated = second,
                                    transitionEndpoints =
                                        anchorContract
                                            ?.transitionEndpointsFromCallback(chain.thisObject),
                                    batteryIslandActive =
                                        anchorContract
                                            ?.batteryIslandFromCallback(chain.thisObject),
                                )
                            dispatchRuntimeCallback(
                                callback = onUpdate?.let { callback -> { callback(update) } },
                                onFailure = onRuntimeFailure,
                            )
                            result
                        },
                    )

            return handles
        } catch (error: Throwable) {
            handles.asReversed().forEach { handle ->
                runCatching { handle.unhook() }
            }
            homeEligible = false
            throw error
        }
    }

    fun resetRuntimeState() {
        synchronized(this) {
            probe = ProbeState()
            homeEligible = null
            anchorContract = null
            headerRef = WeakReference(null)
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

    internal fun expectedHookCount(includeDiagnostics: Boolean): Int =
        CONTROL_CENTER_RUNTIME_HOOK_COUNT +
            if (includeDiagnostics) CONTROL_CENTER_DIAGNOSTIC_HOOK_COUNT else 0

    fun currentHomeEligibility(): Boolean? =
        homeEligible

    @Synchronized
    fun restoreHomeEligibility(eligible: Boolean?) {
        if (eligible != null) {
            homeEligible = eligible
        }
    }

    internal fun allowsHome(visible: Boolean?): Boolean =
        visible == false

    private fun resolveHeader(delegate: Any?): Any? {
        delegate ?: return null
        val contract = anchorContract ?: return null
        return headerRef.get()
            ?: contract.resolveHeader(delegate)?.also { resolved ->
                headerRef = WeakReference(resolved)
            }
    }

    private fun resolveFakePresentationHost(
        delegate: Any?,
    ): ViewGroup? {
        val contract = anchorContract ?: return null
        val header = resolveHeader(delegate) ?: return null
        return contract.fakePresentationRoot(header)
    }

    private fun captureAnchor(delegate: Any?): AnchorSnapshot? {
        val contract = anchorContract ?: return null
        val header = resolveHeader(delegate) ?: return null
        return contract.snapshot(header)
    }

    private fun resolveTransitionEndpoints(
        delegate: Any?,
    ): TransitionEndpoints? {
        val contract = anchorContract ?: return null
        val header =
            resolveHeader(delegate)
                ?: return null
        return contract.transitionEndpoints(header)
    }

    private fun resolveBatteryIslandActive(
        delegate: Any?,
    ): Boolean? {
        val contract = anchorContract ?: return null
        val header =
            resolveHeader(delegate)
                ?: return null
        return contract.batteryIslandActive(header)
    }

    private fun resolveSourceScene(delegate: Any?): SourceScene {
        val contract = anchorContract ?: return SourceScene.UNKNOWN
        val header =
            resolveHeader(delegate)
                ?: return SourceScene.UNKNOWN
        val realSystemIcons =
            contract.realSystemIcons(header)
                ?: return SourceScene.UNKNOWN
        return classifySourceScene(
            homeIdentityMatches =
                SysUiPresentationOwner.ownsBatteryContainer(realSystemIcons),
            structuralScene =
                SysUiSceneSource.steadySourceScene(realSystemIcons),
        )
    }

    internal fun classifySourceScene(
        homeIdentityMatches: Boolean,
        structuralScene: SourceScene,
    ): SourceScene =
        if (homeIdentityMatches) {
            SourceScene.HOME
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
        val probe = probe
        val expandedChanged =
            update.expanded != null && update.expanded != probe.expanded
        val trackingChanged =
            update.tracking != null && update.tracking != probe.tracking
        val visibleChanged =
            update.visible != null && update.visible != probe.visible
        val sourceSceneChanged =
            update.sourceScene != null &&
                update.sourceScene != probe.sourceScene
        val batteryIslandChanged =
            update.batteryIslandActive != null &&
                update.batteryIslandActive != probe.batteryIsland

        if (update.expanded != null) probe.expanded = update.expanded
        if (update.tracking != null) probe.tracking = update.tracking
        if (update.visible != null) probe.visible = update.visible
        if (update.sourceScene != null) {
            probe.sourceScene = update.sourceScene
        }
        if (update.batteryIslandActive != null) {
            probe.batteryIsland = update.batteryIslandActive
        }

        if (
            !DiagnosticPolicy.shouldReport(
                expandedChanged = expandedChanged,
                trackingChanged = trackingChanged,
                visibleChanged = visibleChanged,
                sourceSceneChanged = sourceSceneChanged,
                batteryIslandChanged = batteryIslandChanged,
            )
        ) {
            return
        }

        val sourceSceneSummary =
            update.sourceScene?.let { " sourceScene=" + it.name }.orEmpty()
        val batteryIslandSummary =
            update.batteryIslandActive?.let { " batteryIsland=" + it }.orEmpty()
        dispatchRuntimeCallback(
            callback = {
                onEvent(
                    "panelTransition source=" + update.source.logName +
                        " fraction=" + (update.fraction ?: "none") +
                        " expanded=" + (update.expanded ?: probe.expanded ?: "none") +
                        " tracking=" + (update.tracking ?: probe.tracking ?: "none") +
                        " visible=" + (update.visible ?: probe.visible ?: "none") +
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
        val presentationHost: ViewGroup? = null,
        val sourceScene: SourceScene? = null,
        val appearance: Boolean? = null,
        val appearanceAnimated: Boolean? = null,
        val transitionEndpoints: TransitionEndpoints? = null,
        val batteryIslandActive: Boolean? = null,
        val anchor: AnchorSnapshot? = null,
    )

    internal enum class Source(
        val logName: String,
    ) {
        CONTROL_CENTER("control-center"),
    }

    internal data class TransitionEndpoints(
        val fakeRoot: ViewGroup,
        val finalRoot: ViewGroup,
    )

    internal data class FakePresentationSnapshot(
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

    internal data class AnchorSnapshot(
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
        val expanding: Boolean?,
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
                    ",expanding=" + (expanding ?: "unknown") +
                    "}"
    }

    private class AnchorContract(
        private val callbackClass: Class<*>,
        private val callbacksField: Field,
        private val callbackOuterField: Field,
        private val statusBarAnchorField: Field,
        private val normalStatusBarTranslationXField: Field,
        private val normalStatusIconsTranslationXField: Field,
        private val batteryWidthDiffField: Field,
        private val addBatteryIslandField: Field,
        private val expandingField: Field,
        private val realSystemIconsField: Field,
        private val headerControllerField: Field,
        private val lazyGetMethod: Method,
        private val fakeStatusBarField: Field,
        private val statusBarField: Field,
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
        ): TransitionEndpoints? {
            val fakeRoot = fakeStatusBar(header) ?: return null
            val finalRoot = finalStatusBar(header) ?: return null
            return TransitionEndpoints(
                fakeRoot = fakeRoot,
                finalRoot = finalRoot,
            )
        }

        fun transitionEndpointsFromCallback(
            callback: Any?,
        ): TransitionEndpoints? {
            val header = headerFromCallback(callback) ?: return null
            return transitionEndpoints(header)
        }

        fun batteryIslandFromCallback(callback: Any?): Boolean? {
            val header = headerFromCallback(callback) ?: return null
            return batteryIslandActive(header)
        }

        fun batteryIslandActive(header: Any): Boolean? =
            readBoolean(addBatteryIslandField, header)

        fun snapshotFromCallback(callback: Any?): AnchorSnapshot? {
            val header = headerFromCallback(callback) ?: return null
            return snapshot(header)
        }

        fun fakePresentationFromCallback(
            callback: Any?,
        ): FakePresentationSnapshot? {
            val header = headerFromCallback(callback) ?: return null
            val fakeRoot = fakeStatusBar(header) ?: return null
            val delegate =
                runCatching { fakeDelegateField.get(fakeRoot) }
                    .getOrNull()
                    ?: return null
            val statusBarArea =
                runCatching { fakeStatusBarAreaField.get(delegate) as? View }
                    .getOrNull()
            return FakePresentationSnapshot(
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
                fakeStatusBarField.get(combinedHeader) as? ViewGroup
            }.getOrNull()
        }

        private fun finalStatusBar(header: Any): ViewGroup? {
            val combinedHeader = combinedHeader(header) ?: return null
            return runCatching {
                statusBarField.get(combinedHeader) as? ViewGroup
            }.getOrNull()
        }

        fun snapshot(header: Any): AnchorSnapshot? {
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
            return AnchorSnapshot(
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
                expanding =
                    readBoolean(expandingField, header),
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
            ): AnchorContract? =
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
                    AnchorContract(
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
                        expandingField =
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
                        fakeStatusBarField =
                            combinedHeaderClass
                                .getDeclaredField("controlCenterFakeStatusBar")
                                .accessible(),
                        statusBarField =
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

    internal object DiagnosticPolicy {
        fun shouldReport(
            expandedChanged: Boolean,
            trackingChanged: Boolean,
            visibleChanged: Boolean,
            sourceSceneChanged: Boolean,
            batteryIslandChanged: Boolean,
        ): Boolean =
            expandedChanged ||
                trackingChanged ||
                visibleChanged ||
                sourceSceneChanged ||
                batteryIslandChanged

    }

    private data class ProbeState(
        var expanded: Boolean? = null,
        var tracking: Boolean? = null,
        var visible: Boolean? = null,
        var sourceScene: SourceScene? = null,
        var batteryIsland: Boolean? = null,
    )
}
