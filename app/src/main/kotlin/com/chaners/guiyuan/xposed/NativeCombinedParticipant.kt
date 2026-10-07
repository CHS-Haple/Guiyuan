package com.chaners.guiyuan.xposed

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.VisualCfg
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ArrayList
import java.util.Collections
import java.util.WeakHashMap

internal object NativeCombinedParticipant {
    const val SLOT = "combined_status"

    private const val CONTROLLER_IMPL =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl"
    private const val REGISTRY_IMPL =
        "com.android.systemui.statusbar.pipeline.icons.shared.BindableIconsRegistryImpl"
    private const val BINDABLE_ICON =
        "com.android.systemui.statusbar.pipeline.icons.shared.model.BindableIcon"
    private const val CREATOR =
        "com.android.systemui.statusbar.pipeline.icons.shared.model.ModernStatusBarViewCreator"
    private const val MODERN_VIEW =
        "com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView"
    private const val STATUS_BAR_ICON_VIEW =
        "com.android.systemui.statusbar.StatusBarIconView"
    private const val BINDING =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewBinding"
    private const val BINDABLE_HOLDER =
        "com.android.systemui.statusbar.phone.StatusBarIconHolder\$BindableIconHolder"
    private const val FUNCTION0 = "kotlin.jvm.functions.Function0"
    private const val BATTERY_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val STATUS_ICON_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val CONSTRUCTOR_HOOK_ID =
        "combinedstatus.nativeCombinedParticipant.constructor"
    private const val VISUAL_BOUNDS_HOOK_ID =
        "combinedstatus.nativeCombinedParticipant.visualBounds"
    private const val FOLME_VIEW_STATE =
        "com.android.systemui.statusbar.anim.MiuiStatusBarFolmeViewState"
    private const val NEW_STATUS_ICON_STATE =
        "com.android.systemui.statusbar.views.NewStatusIconState"
    private const val SLOT_TRANSLATION_HOOK_ID =
        "combinedstatus.nativeCombinedParticipant.slotTranslation"
    private const val HOOK_COUNT = 3

    private var constructorHook: HookHandle? = null
    private var visualBoundsHook: HookHandle? = null
    private var slotTranslationHook: HookHandle? = null
    private var rootRef: WeakReference<FrameLayout>? = null
    private var renderViewRef: WeakReference<RenderView>? = null
    private var renderController: RenderController? = null
    private var hostRef: WeakReference<ViewGroup>? = null
    private var eventSink: ((String) -> Unit)? = null
    private var nativeStateIcon: Int? = null
    private var nativeStateDot: Int? = null
    private var nativeStateHidden: Int? = null
    private var nativeSetRemoveMethod: Method? = null
    private var nativeGetRemoveFlagMethod: Method? = null
    private var transitionProbeEnabled: (() -> Boolean)? = null
    private val bindingStates =
        Collections.synchronizedMap(
            WeakHashMap<FrameLayout, BindingState>(),
        )
    private var targetBindingState: BindingState? = null
    private var batteryRef: WeakReference<View>? = null
    private var nativeBatteryLayoutHidden = false
    private var activeSlotBoundaryWidth = 0
    private var activeSlotTranslationX: Float? = null
    private var activeSlotWidth = 0
    private var activeSlotHeight = 0
    private var handoffSink: ((Boolean) -> Boolean)? = null
    private var pendingPreDrawRoot: WeakReference<View>? = null
    private var pendingPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null
    private var modelReady = false
    private var tintReady = false
    private var currentSurface = SysUiSceneSource.Surface.UNKNOWN
    // Re-entry guard: suppression callbacks can synchronously re-enter reconciliation.
    private var handoffPending = false
    // Current visible handoff ownership; this may be released while validation stays valid.
    private var handoffCommitted = false
    // Sticky runtime proof that the native set/remove contract has succeeded at least once.
    private var handoffValidated = false
    private var modelReadyLogged = false
    private var unlockedGeometryLogged = false
    private var visualBoundsLogged = false
    private var slotTranslationCorrectionLogged = false
    private var featureEnabled = false
    private var registryRestored = false
    private var injected = false
    private var failureReason: String? = null

    val installedHookCount: Int
        @Synchronized
        get() =
            listOfNotNull(
                constructorHook,
                visualBoundsHook,
                slotTranslationHook,
            ).size

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        onSlotOrderResult: ((NativeSlotOrder.Result) -> Unit)? = null,
        isTransitionProbeEnabled: () -> Boolean = { false },
    ): String? {
        if (installedHookCount == HOOK_COUNT) {
            eventSink = onEvent
            transitionProbeEnabled = isTransitionProbeEnabled
            return null
        }
        if (installedHookCount != 0) {
            return "partial-hook-state"
        }
        eventSink = onEvent
        transitionProbeEnabled = isTransitionProbeEnabled

        val controllerClass =
            classOrNull(CONTROLLER_IMPL, classLoader)
                ?: return "controller-class-missing"
        val registryClass =
            classOrNull(REGISTRY_IMPL, classLoader)
                ?: return "registry-class-missing"
        val bindableIconClass =
            classOrNull(BINDABLE_ICON, classLoader)
                ?: return "bindable-icon-class-missing"
        val creatorClass =
            classOrNull(CREATOR, classLoader)
                ?: return "creator-class-missing"
        val modernViewClass =
            classOrNull(MODERN_VIEW, classLoader)
                ?: return "modern-view-class-missing"
        val bindingClass =
            classOrNull(BINDING, classLoader)
                ?: return "binding-class-missing"
        val statusBarIconViewClass =
            classOrNull(STATUS_BAR_ICON_VIEW, classLoader)
                ?: return "status-bar-icon-view-class-missing"
        val function0Class =
            classOrNull(FUNCTION0, classLoader)
                ?: return "function0-class-missing"
        val statusIconContainerClass =
            classOrNull(STATUS_ICON_CONTAINER, classLoader)
                ?: return "status-icon-container-class-missing"
        val statusIconContainerOnLayout =
            statusIconContainerClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "onLayout" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(
                                Boolean::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                            ),
                        ) &&
                        method.returnType == Void.TYPE
                }
                ?.apply { isAccessible = true }
                ?: return "status-icon-container-on-layout-missing"
        val folmeViewStateClass =
            classOrNull(FOLME_VIEW_STATE, classLoader)
                ?: return "folme-view-state-class-missing"
        val newStatusIconStateClass =
            classOrNull(NEW_STATUS_ICON_STATE, classLoader)
                ?: return "new-status-icon-state-class-missing"
        val applyToViewMethod =
            folmeViewStateClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "applyToView" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(
                                View::class.java,
                                Boolean::class.javaPrimitiveType,
                            ),
                        ) &&
                        method.returnType == Void.TYPE
                }
                ?.apply { isAccessible = true }
                ?: return "folme-apply-to-view-missing"
        val translationXField =
            runCatching {
                folmeViewStateClass.getDeclaredField("translationX").apply {
                    check(type == Float::class.javaPrimitiveType) {
                        "folme-translation-x-type-mismatch"
                    }
                    isAccessible = true
                }
            }.getOrNull()
                ?: return "folme-translation-x-field-missing"
        val layoutTranslationXField =
            runCatching {
                newStatusIconStateClass.getDeclaredField("layoutTranslationX").apply {
                    check(type == Float::class.javaPrimitiveType) {
                        "layout-translation-x-type-mismatch"
                    }
                    isAccessible = true
                }
            }.getOrNull()
                ?: return "layout-translation-x-field-missing"

        if (
            !bindableIconClass.isInterface ||
            !creatorClass.isInterface ||
            !bindingClass.isInterface ||
            !function0Class.isInterface
        ) {
            return "proxy-contract-mismatch"
        }

        val visibilityStateMethod =
            bindingClass.methods
                .firstOrNull { method ->
                    method.name == "onVisibilityStateChanged" &&
                        method.parameterTypes.size == 1 &&
                        method.parameterTypes[0] == Int::class.javaPrimitiveType &&
                        method.returnType == Void.TYPE
                }
                ?: return "binding-visibility-state-contract-missing"
        val resolvedVisibilityStates =
            resolveNativeVisibilityStates(statusBarIconViewClass)
                ?: return "status-bar-visible-state-contract-missing"
        val setRemoveMethod =
            modernViewClass.methods
                .firstOrNull { method ->
                    method.name == "setRemove" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(Boolean::class.javaPrimitiveType),
                        ) &&
                        method.returnType == Void.TYPE
                }
                ?.apply { isAccessible = true }
                ?: return "modern-view-set-remove-contract-missing"
        val getRemoveFlagMethod =
            modernViewClass.methods
                .firstOrNull { method ->
                    method.name == "getRemoveFlag" &&
                        method.parameterTypes.isEmpty() &&
                        method.returnType == Boolean::class.javaPrimitiveType
                }
                ?.apply { isAccessible = true }
                ?: return "modern-view-get-remove-flag-contract-missing"
        nativeSetRemoveMethod = setRemoveMethod
        nativeGetRemoveFlagMethod = getRemoveFlagMethod

        val resolvedStateIcon = resolvedVisibilityStates.icon
        val resolvedStateDot = resolvedVisibilityStates.dot
        val resolvedStateHidden = resolvedVisibilityStates.hidden
        nativeStateIcon = resolvedStateIcon
        nativeStateDot = resolvedStateDot
        nativeStateHidden = resolvedStateHidden
        eventSink?.invoke(
            "nativeCombinedParticipant bindingContract " +
                "visibilityMethod=" + visibilityStateMethod.name +
                "(int):void" +
                " iconState=" + resolvedStateIcon +
                " dotState=" + resolvedStateDot +
                " hiddenState=" + resolvedStateHidden +
                " removeLifecycle=setRemove(boolean)+getRemoveFlag() " +
                "",
        )

        val constructor =
            controllerClass.declaredConstructors
                .firstOrNull { it.parameterTypes.lastOrNull() == registryClass }
                ?: return "controller-registry-constructor-missing"
        val iconListParameterIndex =
            constructor.parameterTypes.indexOfFirst { type ->
                type.name == NativeSlotOrder.STATUS_BAR_ICON_LIST
            }
        if (iconListParameterIndex < 0) {
            return "controller-icon-list-parameter-missing"
        }
        val registryField =
            registryClass.declaredFields
                .firstOrNull { it.name == "bindableIcons" }
                ?: return "registry-list-field-missing"
        registryField.isAccessible = true

        val viewConstructor =
            modernViewClass.declaredConstructors
                .firstOrNull {
                    it.parameterTypes.map { type -> type.name } ==
                        listOf("android.content.Context", "android.util.AttributeSet")
                }
                ?: return "modern-view-constructor-missing"
        viewConstructor.isAccessible = true

        val initView =
            modernViewClass.declaredMethods
                .firstOrNull {
                    it.name == "initView" &&
                        it.parameterTypes.map { type -> type.name } ==
                        listOf("java.lang.String", FUNCTION0)
                }
                ?: return "modern-view-init-missing"
        initView.isAccessible = true
        constructor.isAccessible = true

        val visualBoundsHandle =
            runCatching {
                module
                    .hook(statusIconContainerOnLayout)
                    .setId(VISUAL_BOUNDS_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val result = chain.proceed()
                            synchronized(this) {
                                applyPostLayoutVisualBounds(chain.thisObject)
                            }
                            result
                        },
                    )
            }.getOrElse { error ->
                return "visual-bounds-hook-" +
                        (error.message ?: error.javaClass.simpleName)
            }

        val slotTranslationHandle =
            runCatching {
                module
                    .hook(applyToViewMethod)
                    .setId(SLOT_TRANSLATION_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val root =
                                rootRef?.get()
                                    ?: return@Hooker chain.proceed()
                            val target =
                                chain.getArg(0) as? View
                                    ?: return@Hooker chain.proceed()
                            if (
                                target !== root ||
                                !newStatusIconStateClass.isInstance(chain.thisObject)
                            ) {
                                return@Hooker chain.proceed()
                            }
                            val desired =
                                currentNativeSlotTranslationX(root)
                                    ?: return@Hooker chain.proceed()
                            val battery = batteryRef?.get()
                            val state = chain.thisObject
                            val previousTranslation =
                                runCatching {
                                    translationXField.getFloat(state)
                                }.getOrNull()
                                    ?: return@Hooker chain.proceed()
                            val previousLayoutTranslation =
                                runCatching {
                                    layoutTranslationXField.getFloat(state)
                                }.getOrNull()
                                    ?: return@Hooker chain.proceed()

                            translationXField.setFloat(state, desired)
                            layoutTranslationXField.setFloat(state, desired)

                            if (
                                !slotTranslationCorrectionLogged &&
                                (
                                    kotlin.math.abs(previousTranslation - desired) >= 0.5f ||
                                        kotlin.math.abs(previousLayoutTranslation - desired) >= 0.5f
                                )
                            ) {
                                slotTranslationCorrectionLogged = true
                                eventSink?.invoke(
                                    "nativeCombinedParticipant slotTranslation " +
                                        "authority=native-end-side-slot-boundary " +
                                        "previousTranslationX=" + previousTranslation +
                                        " previousLayoutTranslationX=" + previousLayoutTranslation +
                                        " correctedTranslationX=" + desired +
                                        " statusIconsWidth=" +
                                        ((root.parent as? View)?.width ?: Int.MIN_VALUE) +
                                        " rootLeft=" + root.left +
                                        " batteryLeft=" + (battery?.left ?: Int.MIN_VALUE) +
                                        " batteryWidth=" + (battery?.width ?: Int.MIN_VALUE) +
                                        " batteryMotionTranslationX=" + (battery?.translationX ?: Float.NaN) +
                                        " nativeTranslationWriter=HyperOS",
                                )
                            }
                            chain.proceed()
                        },
                    )
            }.getOrElse { error ->
                runCatching { visualBoundsHandle.unhook() }
                return "slot-translation-hook-" +
                        (error.message ?: error.javaClass.simpleName)
            }

        val handle =
            runCatching {
                module
                    .hook(constructor)
                    .setId(CONSTRUCTOR_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val registry =
                                chain.getArg(constructor.parameterCount - 1)
                            val iconList = chain.getArg(iconListParameterIndex)
                            val context = chain.getArg(0) as? Context
                            if (
                                registry == null ||
                                iconList == null ||
                                context == null ||
                                !registryClass.isInstance(registry)
                            ) {
                                recordFailure("constructor-args-missing")
                                return@Hooker chain.proceed()
                            }

                            @Suppress("UNCHECKED_CAST")
                            val original =
                                runCatching {
                                    registryField.get(registry) as? List<Any?>
                                }.getOrNull()
                                    ?: run {
                                        recordFailure("registry-list-unreadable")
                                        return@Hooker chain.proceed()
                                    }

                            if (original.any { slotOfBindableIcon(it) == SLOT }) {
                                recordFailure("slot-already-present")
                                return@Hooker chain.proceed()
                            }

                            val preflight =
                                runCatching {
                                    createRoot(
                                        context = context,
                                        classLoader = classLoader,
                                        modernViewClass = modernViewClass,
                                        bindingClass = bindingClass,
                                        function0Class = function0Class,
                                        viewConstructor = viewConstructor,
                                        initView = initView,
                                    )
                                }.getOrElse { error ->
                                    recordFailure(
                                        "creator-preflight-" +
                                            (error.message ?: error.javaClass.simpleName),
                                    )
                                    return@Hooker chain.proceed()
                                }
                            preflight.removeAllViews()

                            val creator =
                                runCatching {
                                    createCreatorProxy(
                                        classLoader = classLoader,
                                        creatorClass = creatorClass,
                                        modernViewClass = modernViewClass,
                                        bindingClass = bindingClass,
                                        function0Class = function0Class,
                                        viewConstructor = viewConstructor,
                                        initView = initView,
                                    )
                                }.getOrElse { error ->
                                    recordFailure(
                                        "creator-proxy-" +
                                            (error.message ?: error.javaClass.simpleName),
                                    )
                                    return@Hooker chain.proceed()
                                }
                            val bindable =
                                runCatching {
                                    createBindableIconProxy(
                                        classLoader = classLoader,
                                        bindableIconClass = bindableIconClass,
                                        creator = creator,
                                    )
                                }.getOrElse { error ->
                                    recordFailure(
                                        "bindable-proxy-" +
                                            (error.message ?: error.javaClass.simpleName),
                                    )
                                    return@Hooker chain.proceed()
                                }
                            val slotPreparation =
                                when (
                                    val result =
                                        NativeSlotOrder.reserveTail(
                                            iconList = iconList,
                                            slot = SLOT,
                                        )
                                ) {
                                    is NativeSlotOrder.Result.Ready ->
                                        result

                                    is NativeSlotOrder.Result.Failure -> {
                                        onSlotOrderResult?.invoke(result)
                                        recordFailure("slot-predeclare-" + result.reason)
                                        return@Hooker chain.proceed()
                                    }
                                }
                            val slotReservation = slotPreparation.reservation

                            val extended =
                                ArrayList<Any?>(original.size + 1).apply {
                                    addAll(original)
                                    add(bindable)
                                }

                            val replaced =
                                runCatching {
                                    registryField.set(registry, extended)
                                    registryField.get(registry) === extended
                                }.getOrDefault(false)
                            if (!replaced) {
                                val slotRolledBack = slotReservation.rollback()
                                val slotFailure =
                                    NativeSlotOrder.Result.Failure(
                                        if (slotRolledBack) {
                                            "transaction-aborted-registry-replacement"
                                        } else {
                                            "transaction-aborted-registry-replacement-slot-rollback-failed"
                                        },
                                    )
                                onSlotOrderResult?.invoke(slotFailure)
                                recordFailure("registry-replacement-failed")
                                return@Hooker chain.proceed()
                            }

                            injected = true
                            failureReason = null
                            var controllerCreated = false
                            try {
                                val result = chain.proceed()
                                controllerCreated = true
                                onSlotOrderResult?.invoke(slotPreparation)
                                onEvent?.invoke(
                                    "nativeCombinedParticipant injected slot=" + SLOT +
                                        " registryOriginal=" + original.size +
                                        " registryExtended=" + extended.size,
                                )
                                result
                            } finally {
                                registryRestored =
                                    runCatching {
                                        registryField.set(registry, original)
                                        registryField.get(registry) === original
                                    }.getOrDefault(false)
                                if (!controllerCreated) {
                                    val slotRolledBack = slotReservation.rollback()
                                    val slotFailure =
                                        NativeSlotOrder.Result.Failure(
                                            if (slotRolledBack) {
                                                "transaction-aborted-controller-construction"
                                            } else {
                                                "transaction-aborted-controller-construction-slot-rollback-failed"
                                            },
                                        )
                                    onSlotOrderResult?.invoke(slotFailure)
                                    injected = false
                                    failureReason =
                                        if (slotRolledBack) {
                                            "controller-construction-failed"
                                        } else {
                                            "controller-construction-failed-slot-rollback-failed"
                                        }
                                } else if (!registryRestored) {
                                    failureReason = "registry-restore-failed"
                                }
                                onEvent?.invoke(
                                    "nativeCombinedParticipant constructorComplete slot=" + SLOT +
                                        " registryRestored=" + registryRestored +
                                        " slotReserved=" + controllerCreated,
                                )
                            }
                        },
                    )
            }.getOrElse {
                runCatching { slotTranslationHandle.unhook() }
                runCatching { visualBoundsHandle.unhook() }
                return "constructor-hook-" + (it.message ?: it.javaClass.simpleName)
            }

        visualBoundsHook = visualBoundsHandle
        slotTranslationHook = slotTranslationHandle
        constructorHook = handle
        return null
    }

    @Synchronized
    fun releaseGenerationForHotReload(): Boolean {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return false
        }

        removePendingPreDraw()
        rootRef = null
        renderViewRef = null
        renderController = null
        hostRef = null
        batteryRef = null
        nativeBatteryLayoutHidden = false
        activeSlotBoundaryWidth = 0
        activeSlotTranslationX = null
        activeSlotWidth = 0
        activeSlotHeight = 0
        targetBindingState = null
        handoffSink = null
        bindingStates.clear()
        modelReady = false
        tintReady = false
        currentSurface = SysUiSceneSource.Surface.UNKNOWN
        handoffPending = false
        handoffCommitted = false
        modelReadyLogged = false
        unlockedGeometryLogged = false
        visualBoundsLogged = false
        slotTranslationCorrectionLogged = false
        featureEnabled = false
        eventSink = null
        return true
    }

    @Synchronized
    fun adoptAfterHotReload(
        host: Any,
    ): HotReloadAdoptResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return HotReloadAdoptResult.Failure("main-thread-required")
        }

        val handles =
            when (val resolution = NativeParticipantAccess.resolve(host)) {
                is NativeParticipantAccess.ResolveResult.Ready ->
                    resolution.handles
                is NativeParticipantAccess.ResolveResult.Failure ->
                    return HotReloadAdoptResult.Failure(resolution.reason)
            }
        val holder =
            NativeParticipantAccess.iconHolder(
                handles = handles,
                slot = SLOT,
            ) ?: return HotReloadAdoptResult.NotPresent
        if (holder.javaClass.name != BINDABLE_HOLDER) {
            return HotReloadAdoptResult.Failure("native-holder-type-mismatch")
        }

        val initializerField =
            fieldOrNull(holder, "initializer")
                ?: return HotReloadAdoptResult.Failure("native-initializer-field-missing")
        val slotField =
            fieldOrNull(holder, "slot")
                ?: return HotReloadAdoptResult.Failure("native-slot-field-missing")
        val visibleField =
            fieldOrNull(holder, "isVisible")
                ?: return HotReloadAdoptResult.Failure("native-visible-field-missing")
        val removal =
            NativeParticipantAccess.removal(handles.controller.javaClass)
                ?: return HotReloadAdoptResult.Failure("removal-contract-missing")
        val iconList =
            readField(handles.controller, "mStatusBarIconList")
                ?: return HotReloadAdoptResult.Failure("status-bar-icon-list-missing")
        val creator =
            runCatching {
                createRuntimeCreator(handles.classLoader)
            }.getOrElse { error ->
                return HotReloadAdoptResult.Failure(
                    error.message ?: error.javaClass.simpleName,
                )
            }

        val removed =
            runCatching {
                NativeParticipantAccess.invokeRemoval(
                    handles = handles,
                    removal = removal,
                    slot = SLOT,
                )
                true
            }.getOrDefault(false)
        if (!removed) {
            return HotReloadAdoptResult.Failure("native-removal-failed")
        }

        val cleared =
            NativeParticipantAccess.clearBindableEntries(
                handles = handles,
                slot = SLOT,
                expectedHolder = holder,
            )
        if (
            NativeParticipantAccess.findSlotView(handles.group, SLOT) != null ||
            NativeParticipantAccess.iconHolder(handles, SLOT) != null
        ) {
            return HotReloadAdoptResult.Failure("native-removal-verification-failed")
        }

        val sanitized =
            runCatching {
                initializerField.set(holder, null)
                initializerField.get(holder) == null
            }.getOrDefault(false)
        if (!sanitized) {
            return HotReloadAdoptResult.Failure("native-holder-sanitize-failed")
        }

        val slotPreparation =
            when (
                val result =
                    NativeSlotOrder.reserveTail(
                        iconList = iconList,
                        slot = SLOT,
                    )
            ) {
                is NativeSlotOrder.Result.Ready ->
                    result
                is NativeSlotOrder.Result.Failure ->
                    return HotReloadAdoptResult.Failure(
                        "slot-reservation-" + result.reason,
                    )
            }

        val rebound =
            runCatching {
                initializerField.set(holder, creator)
                slotField.set(holder, SLOT)
                visibleField.setBoolean(holder, true)
                NativeParticipantAccess.invokeSetIconHolder(
                    handles = handles,
                    slot = SLOT,
                    holder = holder,
                )
                NativeParticipantAccess.iconHolder(handles, SLOT) === holder &&
                    NativeParticipantAccess.findSlotView(handles.group, SLOT) != null
            }.getOrDefault(false)
        if (!rebound) {
            runCatching {
                NativeParticipantAccess.invokeRemoval(
                    handles = handles,
                    removal = removal,
                    slot = SLOT,
                )
            }
            NativeParticipantAccess.clearBindableEntries(
                handles = handles,
                slot = SLOT,
                expectedHolder = holder,
            )
            runCatching { initializerField.set(holder, null) }
            slotPreparation.reservation.rollback()
            recordFailure("hot-reload-adoption-failed")
            return HotReloadAdoptResult.Failure("native-rebind-failed")
        }

        injected = true
        registryRestored = true
        failureReason = null
        eventSink?.invoke(
            "nativeCombinedParticipant hotReloadAdopt slot=" + SLOT +
                " clearedManagerEntries=" + cleared,
        )
        return HotReloadAdoptResult.Ready(
            clearedManagerEntries = cleared,
        )
    }

    @Synchronized
    fun attachHidden(
        host: Any,
        onHandoffStateChanged: ((Boolean) -> Boolean)? = null,
    ): AttachResult {
        if (!injected) {
            return AttachResult.Failure(failureReason ?: "participant-not-injected")
        }
        val resolution = NativeParticipantAccess.resolve(host)
        val handles =
            when (resolution) {
                is NativeParticipantAccess.ResolveResult.Ready ->
                    resolution.handles
                is NativeParticipantAccess.ResolveResult.Failure ->
                    return AttachResult.Failure(resolution.reason)
            }

        val root =
            NativeParticipantAccess.findSlotView(handles.group, SLOT) as? FrameLayout
                ?: return AttachResult.Failure("native-root-missing")
        val bindingState =
            bindingStates[root]
                ?: return AttachResult.Failure("native-binding-state-missing")
        val hostView =
            host as? ViewGroup
                ?: return AttachResult.Failure("host-not-view-group")
        val batteryContainer =
            hostView.directChild(BATTERY_CONTAINER) as? ViewGroup
                ?: return AttachResult.Failure("battery-container-missing")
        val battery =
            batteryContainer.directChild(BATTERY_VIEW)
                ?: return AttachResult.Failure("battery-view-missing")
        val statusIcons =
            batteryContainer.directChild(STATUS_ICON_CONTAINER)
                ?: return AttachResult.Failure("status-icons-missing")
        val privacy =
            readField(batteryContainer, "mHomePrivacyContainer") as? View
        val stableSlotMetrics =
            StatusBarStableSession.currentSlotMetrics(host)
        val stableStatusIconsWidth =
            NativeSlotGeometry.resolveCapturedOrLiveChildWidth(
                capturedWidth = stableSlotMetrics?.statusIconsWidth,
                layoutWidth = statusIcons.width,
                measuredWidth = statusIcons.measuredWidth,
            ) ?: return AttachResult.Failure("status-icons-width-not-ready")
        val stablePrivacyWidth =
            privacy
                ?.takeIf { view -> view.visibility == View.VISIBLE }
                ?.let { view ->
                    NativeSlotGeometry.resolveStableChildWidth(
                        layoutWidth = view.width,
                        measuredWidth = view.measuredWidth,
                    )
                }
                ?: 0
        val slotGeometry =
            NativeSlotGeometry.resolve(
                containerWidth =
                    batteryContainer.width
                        .takeIf { width -> width > 0 }
                        ?: batteryContainer.measuredWidth,
                containerPaddingStart = batteryContainer.paddingStart,
                containerPaddingEnd = batteryContainer.paddingEnd,
                statusIconsMeasuredWidth = stableStatusIconsWidth,
                privacyMeasuredWidth = stablePrivacyWidth,
                containerHeight =
                    batteryContainer.height
                        .takeIf { height -> height > 0 }
                        ?: batteryContainer.measuredHeight,
            ) ?: return AttachResult.Failure("native-slot-geometry-not-ready")
        if (battery.width <= 0 || battery.height <= 0) {
            return AttachResult.Failure("battery-geometry-not-ready")
        }
        activeSlotBoundaryWidth = stableStatusIconsWidth
        activeSlotWidth = slotGeometry.slotWidth
        activeSlotHeight = slotGeometry.slotHeight
        activeSlotTranslationX =
            NativeCombinedPolicy.slotTranslationX(
                statusIconsWidth = activeSlotBoundaryWidth,
                rootLeft = root.left,
            ) ?: return AttachResult.Failure("native-slot-translation-anchor-not-ready")
        eventSink?.invoke(
            "nativeCombinedParticipant slotGeometry " +
                "authority=MiuiStatusBatteryContainer.layout-boundary " +
                "container=" + slotGeometry.containerWidth + "x" + slotGeometry.slotHeight +
                " stableCaptureStatusIconsWidth=" +
                (stableSlotMetrics?.statusIconsWidth ?: Int.MIN_VALUE) +
                " statusIconsLayoutWidth=" + statusIcons.width +
                " statusIconsMeasuredWidth=" + statusIcons.measuredWidth +
                " resolvedStatusIconsWidth=" + slotGeometry.statusIconsMeasuredWidth +
                " stableSlotBoundaryWidth=" + activeSlotBoundaryWidth +
                " privacyLayoutWidth=" + (privacy?.width ?: 0) +
                " privacyMeasuredWidth=" + (privacy?.measuredWidth ?: 0) +
                " resolvedPrivacyWidth=" + slotGeometry.privacyMeasuredWidth +
                " batteryView=" + battery.width + "x" + battery.height +
                " resolvedSlot=" + slotGeometry.slotWidth + "x" + slotGeometry.slotHeight +
                " slotTranslationX=" + activeSlotTranslationX,
        )

        val rootLayoutParams =
            root.layoutParams
                ?: return AttachResult.Failure("native-root-layout-params-missing")
        val targetShellWidth =
            NativeCombinedPolicy.slotOccupancyWidth(
                nativeBatteryHidden = nativeBatteryLayoutHidden,
                visualWidth = activeSlotWidth,
            ) ?: return AttachResult.Failure("native-root-occupancy-width-invalid")
        val originalShellWidth = rootLayoutParams.width
        val originalShellHeight = rootLayoutParams.height
        val shellGeometryAdjusted =
            if (
                originalShellWidth != targetShellWidth ||
                originalShellHeight != activeSlotHeight
            ) {
                runCatching {
                    rootLayoutParams.width = targetShellWidth
                    rootLayoutParams.height = activeSlotHeight
                    root.layoutParams = rootLayoutParams
                    root.layoutParams?.width == targetShellWidth &&
                        root.layoutParams?.height == activeSlotHeight
                }.getOrDefault(false)
            } else {
                true
            }
        if (!shellGeometryAdjusted) {
            return AttachResult.Failure("native-root-geometry-adjustment-failed")
        }
        eventSink?.invoke(
            "nativeCombinedParticipant shellGeometry " +
                "originalWidth=" + originalShellWidth +
                " targetWidth=" + targetShellWidth +
                " nativeBatteryHidden=" + nativeBatteryLayoutHidden +
                " originalHeight=" + originalShellHeight +
                " targetHeight=" + activeSlotHeight +
                " moduleOwnedSlotWidthWrite=" + (originalShellWidth != targetShellWidth) +
                " customShellHeightWrite=" + (originalShellHeight != activeSlotHeight),
        )

        root.clipChildren = false
        root.clipToPadding = false
        bindingState.visible = false
        root.visibility = View.GONE

        val existing = renderViewRef?.get()
        val render =
            if (existing != null && existing.parent === root) {
                existing
            } else {
                val attached =
                    (0 until root.childCount)
                        .asSequence()
                        .map { index -> root.getChildAt(index) }
                        .filterIsInstance<RenderView>()
                        .firstOrNull()
                attached
                    ?: RenderView(root.context).also { child ->
                        root.addView(
                            child,
                            FrameLayout.LayoutParams(
                                activeSlotWidth,
                                activeSlotHeight,
                            ),
                        )
                    }
            }
        val renderLayoutParams =
            (render.layoutParams as? FrameLayout.LayoutParams)
                ?: FrameLayout.LayoutParams(
                    activeSlotWidth,
                    activeSlotHeight,
                )
        renderLayoutParams.width = activeSlotWidth
        renderLayoutParams.height = activeSlotHeight
        renderLayoutParams.gravity = Gravity.NO_GRAVITY
        render.layoutParams = renderLayoutParams

        renderViewRef = WeakReference(render)
        renderController =
            renderController ?: RenderController(render)
        renderController?.updateVisualCfg(
            VisualPrefsOwner.current(),
        )
        featureEnabled =
            FeaturePrefsOwner.current().enabled

        render.measure(
            View.MeasureSpec.makeMeasureSpec(activeSlotWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(activeSlotHeight, View.MeasureSpec.EXACTLY),
        )
        val shellHeight =
            root.layoutParams
                ?.height
                ?.takeIf { height -> height > 0 }
                ?: activeSlotHeight
        val renderTop = (shellHeight - activeSlotHeight) / 2
        render.layout(
            0,
            renderTop,
            activeSlotWidth,
            renderTop + activeSlotHeight,
        )
        val modelUpdate =
            renderController?.update(StatusStateStore.snapshot())
        val batteryTintState =
            SysUiTintSource.currentState(battery)
        val tintUpdate =
            batteryTintState?.let { batteryTint ->
                renderController?.updateTint(
                    NativeCombinedPolicy.mergeTint(
                        batteryTint = batteryTint,
                        nativeTint = bindingState.iconTint,
                    ),
                )
            }
        if (detailedTintReady(bindingState.iconTint, batteryTintState)) {
            eventSink?.invoke(
                "nativeCombinedParticipant tintSeed " +
                    "authority=" +
                    if (bindingState.iconTint != null) {
                        "native-binding"
                    } else {
                        "battery-fallback"
                    } +
                    " nativeTint=" + colorHex(bindingState.iconTint) +
                    " batteryFallback=" +
                    colorHex(batteryTintState?.appliedTint),
            )
        }

        rootRef = WeakReference(root)
        hostRef = WeakReference(hostView)
        batteryRef = WeakReference(battery)
        targetBindingState = bindingState
        handoffSink = onHandoffStateChanged
        modelReady =
            modelUpdate?.model != null &&
                modelUpdate.candidateComplete
        tintReady = tintUpdate?.resolved != null
        currentSurface =
            SysUiSceneSource.currentState(battery)?.surface
                ?: SysUiSceneSource.Surface.UNKNOWN
        reconcileVisibleHandoff("attach")

        return AttachResult.Ready(
            registryRestored = registryRestored,
            rootClass = root.javaClass.name,
            rootVisibility = visibilityName(root.visibility),
            iconVisible = NativeParticipantAccess.iconVisible(root),
            layoutWidth = root.layoutParams?.width ?: Int.MIN_VALUE,
            layoutHeight = root.layoutParams?.height ?: Int.MIN_VALUE,
            renderWidth = render.measuredWidth,
            renderHeight = render.measuredHeight,
            renderTop = render.top,
            renderBottom = render.bottom,
            managerEntry =
                (readField(handles.manager, "mBindableIcons") as? Map<*, *>)
                    ?.containsKey(SLOT) == true,
            modelReady = modelReady,
            tintReady = tintReady,
        )
    }

    @Synchronized
    fun onState(
        snapshot: StatusStateStore.Snapshot,
        trace: RuntimeRenderTrace? = null,
    ) {
        val update = renderController?.update(snapshot, trace)
        if (update?.model != null && update.candidateComplete) {
            modelReady = true
        }
        if (modelReady && !modelReadyLogged) {
            modelReadyLogged = true
            val render = renderViewRef?.get()
            val root = rootRef?.get()
            eventSink?.invoke(
                "nativeCombinedParticipant rendererReady " +
                    "render=" +
                    (render?.measuredWidth ?: -1) + "x" +
                    (render?.measuredHeight ?: -1) +
                    " rootVisibility=" +
                    (root?.let { visibilityName(it.visibility) } ?: "none") +
                    " iconVisible=" +
                    (root?.let { NativeParticipantAccess.iconVisible(it) } ?: "none") +
                    " visible=" + handoffCommitted,
            )
        }
        reconcileVisibleHandoff("state")
    }

    @Synchronized
    fun onSceneUpdate(update: SysUiSceneSource.SceneUpdate) {
        val sourceBattery = batteryRef?.get()
        if (sourceBattery != null && update.sourceView !== sourceBattery) {
            return
        }
        currentSurface = update.surface
        reconcileVisibleHandoff("scene-" + update.surface.name)
        if (
            update.surface != SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR ||
            unlockedGeometryLogged
        ) {
            return
        }
        val host = hostRef?.get() ?: return
        val batteryContainer =
            host.directChild(BATTERY_CONTAINER) as? ViewGroup
                ?: return
        val statusIcons =
            batteryContainer.directChild(STATUS_ICON_CONTAINER)
                ?: return
        val battery =
            batteryContainer.directChild(BATTERY_VIEW)
                ?: return

        unlockedGeometryLogged = true
        eventSink?.invoke(
            "nativeCombinedParticipant unlockedGeometry " +
                "statusIconsWidth=" + statusIcons.width +
                " statusIconsRight=" + statusIcons.right +
                " batteryWidth=" + battery.width +
                " batteryBounds=" + battery.left + "-" + battery.right +
                " adjacentGap=" + (battery.left - statusIcons.right) +
                " rootVisibility=" +
                (rootRef?.get()?.let { visibilityName(it.visibility) } ?: "none"),
        )
    }

    @Synchronized
    fun onPresentationStateChanged(trace: RuntimeRenderTrace? = null) {
        val update =
            renderController?.update(
                StatusStateStore.snapshot(),
                trace,
            )
        if (update?.model != null && update.candidateComplete) {
            modelReady = true
        }
        reconcileVisibleHandoff("presentation")
    }

    @Synchronized
    fun onFeatureCfgChanged(cfg: FeatureCfg) {
        val root = rootRef?.get()
        if (
            root != null &&
            Looper.myLooper() !== Looper.getMainLooper()
        ) {
            root.post {
                onFeatureCfgChanged(cfg)
            }
            return
        }
        if (featureEnabled == cfg.enabled) {
            return
        }
        featureEnabled = cfg.enabled
        if (featureEnabled) {
            if (!resumeValidatedHandoff()) {
                reconcileVisibleHandoff("feature-enabled")
            }
        } else {
            suspendVisibleHandoff("feature-disabled")
        }
    }

    private fun resumeValidatedHandoff(): Boolean {
        if (!handoffValidated || !modelReady || !tintReady) {
            return false
        }
        val root = rootRef?.get() ?: return false
        val bindingState = targetBindingState ?: return false
        val render = renderViewRef?.get() ?: return false
        val parent = root.parent as? ViewGroup ?: return false
        if (!root.isAttachedToWindow || render.measuredWidth <= 0 || render.measuredHeight <= 0) {
            return false
        }

        val battery = batteryRef?.get() ?: return false
        if (!battery.isAttachedToWindow) {
            return false
        }
        val rootLocation = IntArray(2)
        val batteryLocation = IntArray(2)
        root.getLocationOnScreen(rootLocation)
        battery.getLocationOnScreen(batteryLocation)
        val slotAnchorScreenX =
            resolveNativeSlotScreenX(root)
                ?: return false
        if (
            !NativeCombinedPolicy.activeSlotReady(
                rootLayoutWidth = root.layoutParams?.width ?: Int.MIN_VALUE,
                rootLayoutHeight = root.layoutParams?.height ?: Int.MIN_VALUE,
                nativeBatteryHidden = nativeBatteryLayoutHidden,
                renderMeasuredWidth = render.measuredWidth,
                renderMeasuredHeight = render.measuredHeight,
                expectedVisualWidth = activeSlotWidth,
                expectedVisualHeight = activeSlotHeight,
                parentClipsChildren = parent.clipChildren,
                rootScreenX = rootLocation[0],
                slotAnchorScreenX = slotAnchorScreenX,
                renderLeft = render.left,
                renderRight = render.right,
            )
        ) {
            return false
        }

        removePendingPreDraw()
        // Keep synchronous presentation callbacks produced by suppression inside
        // this already-owned handoff transaction. Without this gate a callback
        // can re-enter reconcileVisibleHandoff() before handoffCommitted becomes
        // true, arm a bootstrap pre-draw, and roll back the warm native APPEAR.
        handoffPending = true
        try {
            val removeFlag =
                NativeCombinedPolicy.featureRemoveFlag(
                    featureEnabled = true,
                    handoffValidated = handoffValidated,
                ) ?: return false

            // HyperOS native mobile participants pair their semantic visibility with
            // ModernStatusBarView.setRemove(...). Reuse the same contract so the
            // container owns APPEAR/MOVE instead of treating this as measurement-only.
            val suppressionCommitted = handoffSink?.invoke(true) == true
            if (!suppressionCommitted) {
                eventSink?.invoke(
                    "nativeCombinedParticipant handoffResumeFail " +
                        "source=feature-enabled reason=suppression-transaction-failed",
                )
                return false
            }
            root.visibility = View.VISIBLE
            if (!setNativeRemoveFlag(root, removeFlag)) {
                bindingState.visible = false
                root.visibility = View.GONE
                handoffSink?.invoke(false)
                requestNativeLayout(root)
                eventSink?.invoke(
                    "nativeCombinedParticipant handoffResumeFail " +
                        "source=feature-enabled reason=set-remove-failed",
                )
                return false
            }
            bindingState.visible = true
            handoffCommitted = true
            applyOwnVisualBounds(root)
            requestNativeLayout(root)
            startMasterSwitchTransitionProbe(
                direction = "enable",
                root = root,
            )
            eventSink?.invoke(
                "nativeCombinedParticipant handoffResume " +
                    "source=feature-enabled mode=native-remove-lifecycle rootShown=" + root.isShown +
                    " visibilityAuthority=binding+removeFlag" +
                    " nativeRemoveFlag=" + readNativeRemoveFlag(root),
            )
            return true
        } finally {
            handoffPending = false
        }
    }

    @Synchronized
    fun onVisualCfgChanged(visual: VisualCfg) {
        renderController?.updateVisualCfg(visual)
    }

    @Synchronized
    fun onTintUpdate(update: SysUiTintSource.TintUpdate) {
        val battery = batteryRef?.get() ?: return
        if (update.sourceView !== battery) {
            return
        }
        val nativeTint =
            targetBindingState?.iconTint
                ?.takeIf { color -> (color ushr 24) != 0 }
        if (nativeTint != null) {
            return
        }

        val tintUpdate =
            renderController?.updateTint(
                NativeCombinedPolicy.mergeTint(
                    batteryTint = update.state,
                    nativeTint = null,
                ),
            )
        if (tintUpdate?.resolved != null) {
            tintReady = true
        }
        reconcileVisibleHandoff("tint-fallback")
    }

    @Synchronized
    private fun onBindingVisibilityChanged(
        bindingState: BindingState,
        state: Int,
        parameterCount: Int,
    ) {
        val previous = bindingState.visibleState
        bindingState.visibleState = state

        val root =
            bindingStates.entries
                .firstOrNull { (_, candidate) -> candidate === bindingState }
                ?.key
        val render =
            root?.let { candidateRoot ->
                renderViewRef
                    ?.get()
                    ?.takeIf { candidate -> candidate.parent === candidateRoot }
                    ?: (0 until candidateRoot.childCount)
                        .asSequence()
                        .map(candidateRoot::getChildAt)
                        .filterIsInstance<RenderView>()
                        .firstOrNull()
            }
        val dot =
            root?.let { candidateRoot ->
                (0 until candidateRoot.childCount)
                    .asSequence()
                    .map(candidateRoot::getChildAt)
                    .firstOrNull { child ->
                        child.javaClass.name == STATUS_BAR_ICON_VIEW
                    }
            }

        val resolved =
            NativeCombinedPolicy.contentVisibility(
                state = state,
                iconState = nativeStateIcon,
                dotState = nativeStateDot,
                hiddenState = nativeStateHidden,
            )
        if (resolved != null) {
            render?.visibility = resolved.renderVisibility
            dot?.visibility = resolved.dotVisibility
        } else {
            render?.visibility = View.INVISIBLE
            dot?.visibility = View.INVISIBLE
        }

        if (previous != state) {
            eventSink?.invoke(
                "nativeCombinedParticipant visibilityState " +
                    "state=" + state +
                    " previous=" + (previous ?: "none") +
                    " parameterCount=" + parameterCount +
                    " rootAlpha=" + (root?.alpha ?: -1f) +
                    " rootScale=" +
                    (root?.scaleX ?: -1f) + "x" + (root?.scaleY ?: -1f) +
                    " rootTranslation=" +
                    (root?.translationX ?: Float.NaN) + "," +
                    (root?.translationY ?: Float.NaN) +
                    " renderVisibility=" +
                    (render?.let { visibilityName(it.visibility) } ?: "none") +
                    " dotVisibility=" +
                    (dot?.let { visibilityName(it.visibility) } ?: "none"),
            )
            root?.postOnAnimation {
                eventSink?.invoke(
                    "nativeCombinedParticipant visibilityStateFrame " +
                        "state=" + state +
                        " rootAlpha=" + root.alpha +
                        " rootScale=" + root.scaleX + "x" + root.scaleY +
                        " rootTranslation=" +
                        root.translationX + "," + root.translationY,
                )
            }
        }
    }

    private fun resolveNativeVisibilityStates(
        statusBarIconViewClass: Class<*>,
    ): NativeCombinedPolicy.VisibilityStates? {
        val stateNameMethod =
            statusBarIconViewClass.methods
                .firstOrNull { method ->
                    method.name == "getVisibleStateString" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(Int::class.javaPrimitiveType),
                        ) &&
                        method.returnType == String::class.java &&
                        java.lang.reflect.Modifier.isStatic(method.modifiers)
                }
                ?: return null
        stateNameMethod.isAccessible = true
        return NativeCombinedPolicy.visibilityStates { candidate ->
            runCatching {
                stateNameMethod.invoke(null, candidate) as? String
            }.getOrNull()
        }
    }

    @Synchronized
    private fun onNativeBindingTintChanged(
        bindingState: BindingState,
        tint: Int,
        parameterCount: Int,
    ) {
        if ((tint ushr 24) == 0) {
            return
        }
        val previous = bindingState.iconTint
        bindingState.iconTint = tint
        val sink = eventSink
        if (!bindingState.tintEventLogged && sink != null) {
            bindingState.tintEventLogged = true
            sink.invoke(
                "nativeCombinedParticipant tint " +
                    "authority=ModernStatusBarViewBinding.onIconTintChanged " +
                    "tint=#" + tint.toUInt().toString(16).padStart(8, '0') +
                    " parameterCount=" + parameterCount,
            )
        }
        if (previous == tint || targetBindingState !== bindingState) {
            return
        }

        val update =
            renderController?.updateTint(
                TintState(
                    appliedTint = tint,
                    statusIconTint = tint,
                ),
            )
        if (update?.resolved != null) {
            tintReady = true
        }
        reconcileVisibleHandoff("native-tint")
    }

    private fun startMasterSwitchTransitionProbe(
        direction: String,
        root: View,
    ) {
        val sink = eventSink ?: return
        val enabled = transitionProbeEnabled ?: return
        if (!enabled()) {
            return
        }
        val group = root.parent as? ViewGroup ?: return
        val tracked = mutableListOf(TransitionDiagnosticProbe.TrackedView.create(SLOT, root))
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            if (child === root) {
                continue
            }
            val slot = NativeParticipantAccess.slotOf(child) ?: continue
            if (slot == "wifi" || slot == "mobile" || slot == "stacked_mobile") {
                tracked += TransitionDiagnosticProbe.TrackedView.create(slot, child)
            }
        }
        TransitionDiagnosticProbe.start(
            direction = direction,
            tracked = tracked,
            onEvent = sink,
            isProbeEnabled = enabled,
        )
    }

    private fun suspendVisibleHandoff(source: String) {
        removePendingPreDraw()
        if (handoffPending) {
            return
        }

        val root = rootRef?.get()
        val bindingState = targetBindingState
        val wasCommitted = handoffCommitted

        handoffPending = true
        try {
            if (wasCommitted && handoffSink?.invoke(false) != true) {
                eventSink?.invoke(
                    "nativeCombinedParticipant featureGateFail source=" + source +
                        " reason=native-restore-transaction-failed",
                )
                return
            }

            val bindingVisibilityChanged = bindingState?.visible == true
            if (bindingVisibilityChanged) {
                bindingState?.visible = false
            }

            val nativeRemoveFlag =
                NativeCombinedPolicy.featureRemoveFlag(
                    featureEnabled = false,
                    handoffValidated = handoffValidated,
                )
            val nativeRemoveApplied =
                if (
                    root != null &&
                    bindingState != null &&
                    nativeRemoveFlag != null
                ) {
                    setNativeRemoveFlag(root, nativeRemoveFlag)
                } else {
                    false
                }

            var bootstrapRootChanged = false
            if (
                root != null &&
                (
                    nativeRemoveFlag == null ||
                        !nativeRemoveApplied
                )
            ) {
                if (root.visibility != View.GONE) {
                    root.visibility = View.GONE
                    bootstrapRootChanged = true
                }
            }

            handoffCommitted = false
            if (root != null && nativeRemoveApplied) {
                applyOwnVisualBounds(root)
            }
            if (
                root != null &&
                (
                    bindingVisibilityChanged ||
                        nativeRemoveApplied ||
                        bootstrapRootChanged
                )
            ) {
                requestNativeLayout(root)
            }
            if (root != null && source == "feature-disabled") {
                startMasterSwitchTransitionProbe(
                    direction = "disable",
                    root = root,
                )
            }

            if (
                wasCommitted ||
                bindingVisibilityChanged ||
                nativeRemoveApplied ||
                bootstrapRootChanged
            ) {
                eventSink?.invoke(
                    "nativeCombinedParticipant featureGate source=" + source +
                        " previousHandoff=" + wasCommitted +
                        " visibilityAuthority=" +
                        (
                            if (nativeRemoveApplied) {
                                "binding+removeFlag"
                            } else {
                                "bootstrap-root"
                            }
                        ) +
                        " nativeRemoveFlag=" +
                        (root?.let(::readNativeRemoveFlag) ?: "none") +
                        " shellLayoutWidth=" +
                        (root?.layoutParams?.width ?: Int.MIN_VALUE),
                )
            }
        } finally {
            handoffPending = false
        }
    }

    private fun detailedTintReady(
        nativeTint: Int?,
        batteryTint: TintState?,
    ): Boolean =
        nativeTint != null || batteryTint != null

    private fun colorHex(color: Int?): String =
        color
            ?.let { value ->
                "#" + value.toUInt().toString(16).padStart(8, '0')
            }
            ?: "none"

    private fun reconcileVisibleHandoff(source: String) {
        if (!featureEnabled) {
            suspendVisibleHandoff(source)
            return
        }

        val root = rootRef?.get() ?: return
        val bindingState = targetBindingState ?: return
        if (handoffCommitted) {
            if (!bindingState.visible) {
                bindingState.visible = true
                requestNativeLayout(root)
            }
            return
        }

        val handoffMode =
            NativeCombinedPolicy.handoffMode(
                surface = currentSurface,
                rootShown = root.isShown,
            )
        if (
            handoffPending ||
            !modelReady ||
            !tintReady ||
            handoffMode == NativeCombinedPolicy.HandoffMode.BLOCKED ||
            !root.isAttachedToWindow ||
            root.parent == null
        ) {
            return
        }

        bindingState.visible = true
        root.visibility = View.VISIBLE
        handoffPending = true
        requestNativeLayout(root)
        eventSink?.invoke(
            "nativeCombinedParticipant handoffPrepare source=" + source +
                " modelReady=" + modelReady +
                " tintReady=" + tintReady +
                " scene=" + currentSurface.name +
                " mode=" + handoffMode.name +
                " rootShownBefore=" + root.isShown,
        )

        val listener =
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    removePendingPreDraw()
                    synchronized(this@NativeCombinedParticipant) {
                        try {
                            if (
                            rootRef?.get() !== root ||
                            targetBindingState !== bindingState
                        ) {
                            return@synchronized
                        }

                        val iconVisible =
                            NativeParticipantAccess.iconVisible(root) == true
                        val render = renderViewRef?.get()
                        val battery = batteryRef?.get()
                        val parent = root.parent as? ViewGroup
                        val rootLocation = IntArray(2)
                        val batteryLocation = IntArray(2)
                        if (root.isAttachedToWindow) {
                            root.getLocationOnScreen(rootLocation)
                        }
                        if (battery?.isAttachedToWindow == true) {
                            battery.getLocationOnScreen(batteryLocation)
                        }
                        val slotAnchorScreenX =
                            if (battery != null) {
                                resolveNativeSlotScreenX(root)
                            } else {
                                null
                            }
                        val bridgeReady =
                            NativeCombinedPolicy.zeroSlotReady(
                                rootMeasuredWidth = root.measuredWidth,
                                rootMeasuredHeight = root.measuredHeight,
                                nativeBatteryHidden = nativeBatteryLayoutHidden,
                                renderMeasuredWidth = render?.measuredWidth ?: -1,
                                renderMeasuredHeight = render?.measuredHeight ?: -1,
                                expectedVisualWidth = activeSlotWidth,
                                expectedVisualHeight = activeSlotHeight,
                                parentClipsChildren = parent?.clipChildren ?: true,
                                rootScreenX = rootLocation[0],
                                slotAnchorScreenX = slotAnchorScreenX ?: Int.MIN_VALUE,
                                renderLeft = render?.left ?: Int.MIN_VALUE,
                                renderRight = render?.right ?: Int.MIN_VALUE,
                            )
                        val resolvedMode =
                            NativeCombinedPolicy.handoffMode(
                                surface = currentSurface,
                                rootShown = root.isShown,
                            )
                        val ready =
                            modelReady &&
                                tintReady &&
                                resolvedMode != NativeCombinedPolicy.HandoffMode.BLOCKED &&
                                bindingState.visible &&
                                iconVisible &&
                                root.isAttachedToWindow &&
                                bridgeReady

                        if (ready) {
                            // The native battery slot remains the single end-side occupancy
                            // owner. Keep the Combined Status shell zero-width and render the
                            // verified visual geometry into that preserved native slot.
                            bindingState.visible = false
                            root.visibility = View.GONE
                            val suppressionCommitted = handoffSink?.invoke(true) == true
                            if (!suppressionCommitted) {
                                handoffSink?.invoke(false)
                                bindingState.visible = false
                                root.visibility = View.GONE
                                requestNativeLayout(root)
                                eventSink?.invoke(
                                    "nativeCombinedParticipant handoffRollback " +
                                        "reason=suppression-transaction-failed",
                                )
                                return@synchronized
                            }
                            bindingState.visible = true
                            root.visibility = View.VISIBLE
                            handoffCommitted = true
                            handoffValidated = true
                            applyOwnVisualBounds(root)
                            requestNativeLayout(root)
                            eventSink?.invoke(
                                "nativeCombinedParticipant handoffCommit " +
                                    "mode=" + resolvedMode.name +
                                    " rootShown=" + root.isShown +
                                    " hiddenAncestor=" + firstHiddenAncestor(root) +
                                    " measured=" + root.measuredWidth + "x" +
                                    root.measuredHeight +
                                    " renderMeasured=" +
                                    (render?.measuredWidth ?: -1) + "x" +
                                    (render?.measuredHeight ?: -1) +
                                    " rootScreenX=" + rootLocation[0] +
                                    " batteryScreenX=" + batteryLocation[0] +
                                    " renderBounds=" +
                                    (render?.left ?: Int.MIN_VALUE) + "-" +
                                    (render?.right ?: Int.MIN_VALUE) +
                                    " parentClipChildren=" +
                                    (parent?.clipChildren ?: true) +
                                    " bridge=preserved-native-battery-slot " +
                                    "shellLayoutWidth=" +
                                    (root.layoutParams?.width ?: Int.MIN_VALUE) +
                                    " iconVisible=" + iconVisible,
                            )
                        } else {
                            bindingState.visible = false
                            root.visibility = View.GONE
                            requestNativeLayout(root)
                            eventSink?.invoke(
                                "nativeCombinedParticipant handoffRollback " +
                                    "modelReady=" + modelReady +
                                    " tintReady=" + tintReady +
                                    " scene=" + currentSurface.name +
                                    " mode=" + resolvedMode.name +
                                    " rootShown=" + root.isShown +
                                    " hiddenAncestor=" + firstHiddenAncestor(root) +
                                    " iconVisible=" + iconVisible +
                                    " measured=" + root.measuredWidth + "x" +
                                    root.measuredHeight +
                                    " renderMeasured=" +
                                    (render?.measuredWidth ?: -1) + "x" +
                                    (render?.measuredHeight ?: -1) +
                                    " rootScreenX=" + rootLocation[0] +
                                    " batteryScreenX=" + batteryLocation[0] +
                                    " renderBounds=" +
                                    (render?.left ?: Int.MIN_VALUE) + "-" +
                                    (render?.right ?: Int.MIN_VALUE) +
                                    " parentClipChildren=" +
                                    (parent?.clipChildren ?: true) +
                                    " bridgeReady=" + bridgeReady,
                            )
                        }
                        } finally {
                            handoffPending = false
                        }
                    }
                    return true
                }
            }
        pendingPreDrawRoot = WeakReference(root)
        pendingPreDrawListener = listener
        root.viewTreeObserver.addOnPreDrawListener(listener)
    }

    private fun currentNativeSlotTranslationX(root: View): Float? =
        activeSlotTranslationX
            ?: activeSlotBoundaryWidth
                .takeIf { width -> width > 0 }
                ?.let { boundaryWidth ->
                    NativeCombinedPolicy.slotTranslationX(
                        statusIconsWidth = boundaryWidth,
                        rootLeft = root.left,
                    )
                }

    private fun refreshNativeSlotTranslationX(
        root: View,
        statusIcons: View,
    ): Boolean {
        val boundaryWidth =
            activeSlotBoundaryWidth
                .takeIf { width -> width > 0 }
                ?: statusIcons.width
                    .takeIf { width -> width > 0 }
                ?: return false
        val resolved =
            NativeCombinedPolicy.slotTranslationX(
                statusIconsWidth = boundaryWidth,
                rootLeft = root.left,
            ) ?: return false
        activeSlotTranslationX = resolved
        return true
    }

    private fun resolveNativeSlotScreenX(root: View): Int? {
        val statusIcons = root.parent as? View ?: return null
        val translation = currentNativeSlotTranslationX(root) ?: return null
        val statusIconsLocation = IntArray(2)
        statusIcons.getLocationOnScreen(statusIconsLocation)
        return kotlin.math.round(
            statusIconsLocation[0] + root.left + translation,
        ).toInt()
    }

    private fun firstHiddenAncestor(view: View): String {
        var current = view.parent as? View
        while (current != null) {
            if (current.visibility != View.VISIBLE || !current.isShown) {
                return current.javaClass.simpleName +
                    "{visibility=" + visibilityName(current.visibility) +
                    ",shown=" + current.isShown +
                    ",alpha=" + current.alpha + "}"
            }
            current = current.parent as? View
        }
        return "none"
    }

    private fun requestNativeLayout(root: View) {
        root.requestLayout()
        (root.parent as? View)?.requestLayout()
    }

    @Synchronized
    fun onNativeBatteryLayoutHideChanged(hidden: Boolean): Boolean {
        nativeBatteryLayoutHidden = hidden
        val root = rootRef?.get() ?: return true
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return root.post {
                onNativeBatteryLayoutHideChanged(hidden)
            }
        }
        val visualWidth =
            renderViewRef
                ?.get()
                ?.measuredWidth
                ?.takeIf { width -> width > 0 }
                ?: activeSlotWidth
        val targetWidth =
            NativeCombinedPolicy.slotOccupancyWidth(
                nativeBatteryHidden = hidden,
                visualWidth = visualWidth,
            ) ?: return false
        val layoutParams = root.layoutParams ?: return false
        val previousWidth = layoutParams.width
        if (previousWidth == targetWidth) {
            return true
        }
        layoutParams.width = targetWidth
        root.layoutParams = layoutParams
        requestNativeLayout(root)
        eventSink?.invoke(
            "nativeCombinedParticipant slotOccupancy " +
                "authority=MiuiStatusBatteryContainer.setIsHideBattery " +
                "nativeBatteryHidden=" + hidden +
                " previousLayoutWidth=" + previousWidth +
                " targetLayoutWidth=" + targetWidth +
                " visualWidth=" + visualWidth +
                " rootWidthOwner=module ",
        )
        return root.layoutParams?.width == targetWidth
    }

    private fun applyPostLayoutVisualBounds(container: Any): Boolean {
        val root = rootRef?.get() ?: return false
        if (root.parent !== container) {
            return false
        }
        val statusIcons = container as? View ?: return false
        if (!refreshNativeSlotTranslationX(root, statusIcons)) {
            return false
        }
        return applyOwnVisualBounds(root)
    }

    private fun applyOwnVisualBounds(root: View): Boolean {
        val visualWidth =
            renderViewRef
                ?.get()
                ?.measuredWidth
                ?.takeIf { width -> width > 0 }
                ?: activeSlotWidth
        val visualHeight =
            renderViewRef
                ?.get()
                ?.measuredHeight
                ?.takeIf { height -> height > 0 }
                ?: activeSlotHeight
        val resolvedWidth =
            NativeCombinedPolicy.postLayoutVisualWidth(
                layoutWidth = root.layoutParams?.width ?: Int.MIN_VALUE,
                measuredWidth = root.measuredWidth,
                visualWidth = visualWidth,
                nativeBatteryHidden = nativeBatteryLayoutHidden,
            ) ?: return false
        if (visualHeight <= 0) {
            return false
        }

        val left = root.left
        val top = root.top
        val right = left + resolvedWidth
        val bottom = top + visualHeight
        if (
            root.width != resolvedWidth ||
            root.height != visualHeight ||
            root.right != right ||
            root.bottom != bottom
        ) {
            root.layout(left, top, right, bottom)
        }
        val expectedOccupancyWidth =
            NativeCombinedPolicy.slotOccupancyWidth(
                nativeBatteryHidden = nativeBatteryLayoutHidden,
                visualWidth = resolvedWidth,
            ) ?: return false
        val applied =
            root.layoutParams?.width == expectedOccupancyWidth &&
                root.measuredWidth == expectedOccupancyWidth &&
                root.width == resolvedWidth &&
                root.height == visualHeight
        if (applied && !visualBoundsLogged) {
            visualBoundsLogged = true
            eventSink?.invoke(
                "nativeCombinedParticipant visualBounds " +
                    "authority=post-MiuiStatusIconContainer.onLayout " +
                    "layoutWidth=" + (root.layoutParams?.width ?: Int.MIN_VALUE) +
                    " measuredWidth=" + root.measuredWidth +
                    " actualWidth=" + root.width +
                    " actualHeight=" + root.height +
                    " translationX=" + root.translationX +
                    " visualBoundsOwner=module ",
            )
        }
        return applied
    }

    private fun setNativeRemoveFlag(
        root: View,
        remove: Boolean,
    ): Boolean {
        if (rootRef?.get() === root && !applyOwnVisualBounds(root)) {
            return false
        }
        val method = nativeSetRemoveMethod ?: return false
        if (!method.declaringClass.isInstance(root)) {
            return false
        }
        return runCatching {
            method.invoke(root, remove)
            readNativeRemoveFlag(root) == remove
        }.getOrDefault(false)
    }

    private fun readNativeRemoveFlag(root: View): Boolean? {
        val method = nativeGetRemoveFlagMethod ?: return null
        if (!method.declaringClass.isInstance(root)) {
            return null
        }
        return runCatching {
            method.invoke(root) as? Boolean
        }.getOrNull()
    }

    private fun resolveCurrentHandles(): NativeParticipantAccess.Handles? {
        val host = hostRef?.get() ?: return null
        return when (val resolution = NativeParticipantAccess.resolve(host)) {
            is NativeParticipantAccess.ResolveResult.Ready ->
                resolution.handles
            is NativeParticipantAccess.ResolveResult.Failure ->
                null
        }
    }

    private fun removePendingPreDraw() {
        val root = pendingPreDrawRoot?.get()
        val listener = pendingPreDrawListener
        if (root != null && listener != null) {
            val observer = root.viewTreeObserver
            if (observer.isAlive) {
                observer.removeOnPreDrawListener(listener)
            }
        }
        pendingPreDrawRoot = null
        pendingPreDrawListener = null
    }

    @Synchronized
    fun resetRuntimeState() {
        constructorHook = null
        visualBoundsHook = null
        slotTranslationHook = null
        reset()
    }

    private fun reset() {
        removePendingPreDraw()
        TransitionDiagnosticProbe.stop()
        if (handoffCommitted) {
            handoffSink?.invoke(false)
        }
        targetBindingState?.visible = false
        renderViewRef?.get()?.let { render ->
            (render.parent as? ViewGroup)?.removeView(render)
        }
        bindingStates.clear()
        rootRef = null
        renderViewRef = null
        hostRef = null
        batteryRef = null
        nativeBatteryLayoutHidden = false
        activeSlotBoundaryWidth = 0
        activeSlotTranslationX = null
        activeSlotWidth = 0
        activeSlotHeight = 0
        handoffSink = null
        targetBindingState = null
        modelReady = false
        tintReady = false
        currentSurface = SysUiSceneSource.Surface.UNKNOWN
        handoffPending = false
        handoffCommitted = false
        eventSink = null
        nativeStateIcon = null
        nativeStateDot = null
        nativeStateHidden = null
        nativeSetRemoveMethod = null
        nativeGetRemoveFlagMethod = null
        transitionProbeEnabled = null
        modelReadyLogged = false
        unlockedGeometryLogged = false
        visualBoundsLogged = false
        slotTranslationCorrectionLogged = false
        renderController = null
        injected = false
        registryRestored = false
        failureReason = null
    }

    private fun createRuntimeCreator(classLoader: ClassLoader): Any {
        val creatorClass =
            classOrNull(CREATOR, classLoader)
                ?: error("creator-class-missing")
        val modernViewClass =
            classOrNull(MODERN_VIEW, classLoader)
                ?: error("modern-view-class-missing")
        val bindingClass =
            classOrNull(BINDING, classLoader)
                ?: error("binding-class-missing")
        val function0Class =
            classOrNull(FUNCTION0, classLoader)
                ?: error("function0-class-missing")
        check(
            creatorClass.isInterface &&
                bindingClass.isInterface &&
                function0Class.isInterface,
        ) {
            "proxy-contract-mismatch"
        }
        val viewConstructor =
            modernViewClass.declaredConstructors
                .firstOrNull {
                    it.parameterTypes.map { type -> type.name } ==
                        listOf("android.content.Context", "android.util.AttributeSet")
                } ?: error("modern-view-constructor-missing")
        viewConstructor.isAccessible = true
        val initView =
            modernViewClass.declaredMethods
                .firstOrNull {
                    it.name == "initView" &&
                        it.parameterTypes.map { type -> type.name } ==
                        listOf("java.lang.String", FUNCTION0)
                } ?: error("modern-view-init-missing")
        initView.isAccessible = true
        return createCreatorProxy(
            classLoader = classLoader,
            creatorClass = creatorClass,
            modernViewClass = modernViewClass,
            bindingClass = bindingClass,
            function0Class = function0Class,
            viewConstructor = viewConstructor,
            initView = initView,
        )
    }

    private fun createBindableIconProxy(
        classLoader: ClassLoader,
        bindableIconClass: Class<*>,
        creator: Any,
    ): Any =
        Proxy.newProxyInstance(
            classLoader,
            arrayOf(bindableIconClass),
        ) { proxy, method, args ->
            when (method.name) {
                "getSlot" -> SLOT
                "getShouldBindIcon" -> true
                "getInitializer" -> creator
                "toString" -> "CombinedStatusNativeParticipant(slot=$SLOT)"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> defaultValue(method.returnType)
            }
        }

    private fun createCreatorProxy(
        classLoader: ClassLoader,
        creatorClass: Class<*>,
        modernViewClass: Class<*>,
        bindingClass: Class<*>,
        function0Class: Class<*>,
        viewConstructor: java.lang.reflect.Constructor<*>,
        initView: Method,
    ): Any =
        Proxy.newProxyInstance(
            classLoader,
            arrayOf(creatorClass),
        ) { proxy, method, args ->
            when (method.name) {
                "createAndBind" -> {
                    val context =
                        args?.firstOrNull() as? Context
                            ?: error("creator-context-missing")
                    createRoot(
                        context = context,
                        classLoader = classLoader,
                        modernViewClass = modernViewClass,
                        bindingClass = bindingClass,
                        function0Class = function0Class,
                        viewConstructor = viewConstructor,
                        initView = initView,
                    )
                }
                "toString" -> "CombinedStatusNativeParticipantCreator"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> defaultValue(method.returnType)
            }
        }

    private fun createRoot(
        context: Context,
        classLoader: ClassLoader,
        modernViewClass: Class<*>,
        bindingClass: Class<*>,
        function0Class: Class<*>,
        viewConstructor: java.lang.reflect.Constructor<*>,
        initView: Method,
    ): FrameLayout {
        val bindingState = BindingState()
        val binding =
            Proxy.newProxyInstance(
                classLoader,
                arrayOf(bindingClass),
            ) { proxy, method, args ->
                when (method.name) {
                    "getShouldIconBeVisible" -> bindingState.visible
                    "isCollecting" -> true
                    "onVisibilityStateChanged" -> {
                        (args?.firstOrNull() as? Number)
                            ?.toInt()
                            ?.let { state ->
                                onBindingVisibilityChanged(
                                    bindingState = bindingState,
                                    state = state,
                                    parameterCount = method.parameterCount,
                                )
                            }
                        null
                    }
                    "onIconTintChanged" -> {
                        (args?.firstOrNull() as? Number)
                            ?.toInt()
                            ?.let { tint ->
                                onNativeBindingTintChanged(
                                    bindingState = bindingState,
                                    tint = tint,
                                    parameterCount = method.parameterCount,
                                )
                            }
                        null
                    }
                    "toString" -> "CombinedStatusNativeParticipantBinding"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    else -> defaultValue(method.returnType)
                }
            }
        val bindingFactory =
            Proxy.newProxyInstance(
                classLoader,
                arrayOf(function0Class),
            ) { proxy, method, args ->
                when (method.name) {
                    "invoke" -> binding
                    "toString" -> "CombinedStatusNativeParticipantBindingFactory"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    else -> defaultValue(method.returnType)
                }
            }

        val root =
            viewConstructor.newInstance(
                context,
                null as AttributeSet?,
            ) as? FrameLayout
                ?: error("modern-view-not-frame-layout")
        check(modernViewClass.isInstance(root)) {
            "modern-view-instance-mismatch"
        }
        root.clipChildren = false
        root.clipToPadding = false
        initView.invoke(root, SLOT, bindingFactory)
        bindingStates[root] = bindingState
        root.visibility = View.GONE
        return root
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) return child
        }
        return null
    }

    private fun slotOfBindableIcon(icon: Any?): String? {
        if (icon == null) return null
        val accessor =
            icon.javaClass.methods.firstOrNull {
                it.name == "getSlot" &&
                    it.parameterCount == 0 &&
                    it.returnType == String::class.java
            } ?: return null
        return runCatching { accessor.invoke(icon) as? String }.getOrNull()
    }

    private fun classOrNull(name: String, classLoader: ClassLoader): Class<*>? =
        runCatching { Class.forName(name, false, classLoader) }.getOrNull()

    private fun fieldOrNull(target: Any, name: String): Field? =
        generateSequence<Class<*>>(target.javaClass) { it.superclass }
            .mapNotNull { clazz ->
                clazz.declaredFields.firstOrNull { field -> field.name == name }
            }
            .firstOrNull()
            ?.apply { isAccessible = true }

    private fun readField(target: Any, name: String): Any? {
        val field =
            generateSequence<Class<*>>(target.javaClass) { it.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { it.name == name }
                }
                .firstOrNull()
                ?: return null
        return runCatching {
            field.isAccessible = true
            field.get(target)
        }.getOrNull()
    }

    private fun recordFailure(reason: String) {
        injected = false
        failureReason = reason
    }

    private fun defaultValue(type: Class<*>): Any? =
        when (type) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> 0.toChar()
            java.lang.Void.TYPE -> null
            else -> null
        }

    private fun visibilityName(visibility: Int): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }

    internal sealed interface HotReloadAdoptResult {
        data class Ready(
            val clearedManagerEntries: Int,
        ) : HotReloadAdoptResult
        data object NotPresent : HotReloadAdoptResult
        data class Failure(
            val reason: String,
        ) : HotReloadAdoptResult
    }

    private object TransitionDiagnosticProbe {
        private var generation = 0
        private var activeRoot = WeakReference<View>(null)
        private var listener: ViewTreeObserver.OnPreDrawListener? = null
        private var timeout: Runnable? = null

        fun start(
            direction: String,
            tracked: List<TrackedView>,
            onEvent: (String) -> Unit,
            isProbeEnabled: () -> Boolean,
        ) {
            stop()
            if (!isProbeEnabled() || tracked.isEmpty()) {
                return
            }
            generation += 1
            val currentGeneration = generation
            val root = tracked.first().view.get()?.rootView ?: return
            val observer = root.viewTreeObserver
            if (!observer.isAlive) {
                return
            }
            val startedAt = SystemClock.uptimeMillis()
            var frame = 0
            var samples = 0
            var previous = ""

            val nextListener =
                ViewTreeObserver.OnPreDrawListener {
                    if (!isProbeEnabled()) {
                        stop()
                        return@OnPreDrawListener true
                    }
                    frame += 1
                    val snapshot =
                        tracked.joinToString(" ") { item ->
                            item.snapshot()
                        }
                    if (snapshot != previous && samples < MAX_SAMPLES) {
                        previous = snapshot
                        samples += 1
                        onEvent(
                            "nativeCombinedParticipant transitionSample " +
                                "direction=" + direction +
                                " frame=" + frame +
                                " elapsedMs=" +
                                (SystemClock.uptimeMillis() - startedAt) +
                                " " + snapshot +
                                " sample=" + samples + "/" + MAX_SAMPLES +
                                "",
                        )
                    }
                    if (
                        currentGeneration == generation &&
                        SystemClock.uptimeMillis() - startedAt >= FOLLOW_DURATION_MS
                    ) {
                        stop()
                    }
                    true
                }
            val nextTimeout =
                Runnable {
                    if (currentGeneration == generation) {
                        stop()
                    }
                }

            listener = nextListener
            timeout = nextTimeout
            activeRoot = WeakReference(root)
            observer.addOnPreDrawListener(nextListener)
            root.postDelayed(nextTimeout, FOLLOW_DURATION_MS)
        }

        fun stop() {
            val root = activeRoot.get()
            val currentListener = listener
            val currentTimeout = timeout
            if (root != null) {
                if (currentListener != null) {
                    val observer = root.viewTreeObserver
                    if (observer.isAlive) {
                        observer.removeOnPreDrawListener(currentListener)
                    }
                }
                if (currentTimeout != null) {
                    root.removeCallbacks(currentTimeout)
                }
            }
            listener = null
            timeout = null
            activeRoot = WeakReference(null)
        }

        class TrackedView private constructor(
            val slot: String,
            val view: WeakReference<View>,
            private val visibleStateGetter: Method?,
            private val removeFlagGetter: Method?,
        ) {
            fun snapshot(): String {
                val target = view.get() ?: return slot + "={released}"
                val visibleState =
                    visibleStateGetter
                        ?.let { method ->
                            runCatching { method.invoke(target) as? Number }
                                .getOrNull()
                                ?.toInt()
                        }
                val removeFlag =
                    removeFlagGetter
                        ?.let { method ->
                            runCatching { method.invoke(target) as? Boolean }
                                .getOrNull()
                        }
                return slot + "={" +
                    "w=" + target.width +
                    ",h=" + target.height +
                    ",a=" + target.alpha +
                    ",sx=" + target.scaleX +
                    ",sy=" + target.scaleY +
                    ",px=" + target.pivotX +
                    ",py=" + target.pivotY +
                    ",tx=" + target.translationX +
                    ",ty=" + target.translationY +
                    ",v=" + target.visibility +
                    ",state=" + (visibleState ?: "none") +
                    ",remove=" + (removeFlag ?: "none") +
                    "}"
            }

            companion object {
                fun create(
                    slot: String,
                    view: View,
                ): TrackedView {
                    val methods =
                        generateSequence<Class<*>>(view.javaClass) { clazz ->
                            clazz.superclass
                        }
                            .flatMap { clazz -> clazz.declaredMethods.asSequence() }
                            .toList()
                    val visibleStateGetter =
                        methods
                            .firstOrNull { method ->
                                method.name == "getVisibleState" &&
                                    method.parameterCount == 0 &&
                                    Number::class.java.isAssignableFrom(
                                        method.returnType.boxed(),
                                    )
                            }
                            ?.apply { isAccessible = true }
                    val removeFlagGetter =
                        methods
                            .firstOrNull { method ->
                                method.name == "getRemoveFlag" &&
                                    method.parameterCount == 0 &&
                                    (
                                        method.returnType == Boolean::class.javaPrimitiveType ||
                                            method.returnType == Boolean::class.javaObjectType
                                    )
                            }
                            ?.apply { isAccessible = true }
                    return TrackedView(
                        slot = slot,
                        view = WeakReference(view),
                        visibleStateGetter = visibleStateGetter,
                        removeFlagGetter = removeFlagGetter,
                    )
                }

                private fun Class<*>.boxed(): Class<*> =
                    when (this) {
                        Integer.TYPE -> Integer::class.java
                        java.lang.Long.TYPE -> java.lang.Long::class.java
                        java.lang.Short.TYPE -> java.lang.Short::class.java
                        java.lang.Byte.TYPE -> java.lang.Byte::class.java
                        else -> this
                    }
            }
        }

        private const val MAX_SAMPLES = 16
        private const val FOLLOW_DURATION_MS = 900L
    }

    private class BindingState(
        @Volatile var visible: Boolean = false,
        @Volatile var iconTint: Int? = null,
        @Volatile var visibleState: Int? = null,
        @Volatile var tintEventLogged: Boolean = false,
    )

    internal sealed interface AttachResult {
        data class Ready(
            val registryRestored: Boolean,
            val rootClass: String,
            val rootVisibility: String,
            val iconVisible: Boolean?,
            val layoutWidth: Int,
            val layoutHeight: Int,
            val renderWidth: Int,
            val renderHeight: Int,
            val renderTop: Int,
            val renderBottom: Int,
            val managerEntry: Boolean,
            val modelReady: Boolean,
            val tintReady: Boolean,
        ) : AttachResult
        data class Failure(val reason: String) : AttachResult
    }

}
