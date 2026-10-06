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
import com.chaners.guiyuan.settings.FeatureSettings
import com.chaners.guiyuan.settings.VisualSettings
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

internal object NativeParticipantUi {
    const val SLOT = "combined_status"
    private const val ZERO_SLOT_WIDTH = 0
    private const val MAX_NATIVE_VISIBLE_STATE_PROBE = 8

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
    private var currentSurface = SceneSource.Surface.UNKNOWN
    private var handoffPending = false
    private var handoffCommitted = false
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
        onSlotOrderResult: ((StatusSlotReservation.Result) -> Unit)? = null,
        isTransitionProbeEnabled: () -> Boolean = { false },
    ): InstallResult {
        if (installedHookCount == HOOK_COUNT) {
            eventSink = onEvent
            transitionProbeEnabled = isTransitionProbeEnabled
            return InstallResult.AlreadyInstalled
        }
        if (installedHookCount != 0) {
            return InstallResult.Failure("partial-hook-state")
        }
        eventSink = onEvent
        transitionProbeEnabled = isTransitionProbeEnabled

        val controllerClass =
            classOrNull(CONTROLLER_IMPL, classLoader)
                ?: return InstallResult.Failure("controller-class-missing")
        val registryClass =
            classOrNull(REGISTRY_IMPL, classLoader)
                ?: return InstallResult.Failure("registry-class-missing")
        val bindableIconClass =
            classOrNull(BINDABLE_ICON, classLoader)
                ?: return InstallResult.Failure("bindable-icon-class-missing")
        val creatorClass =
            classOrNull(CREATOR, classLoader)
                ?: return InstallResult.Failure("creator-class-missing")
        val modernViewClass =
            classOrNull(MODERN_VIEW, classLoader)
                ?: return InstallResult.Failure("modern-view-class-missing")
        val bindingClass =
            classOrNull(BINDING, classLoader)
                ?: return InstallResult.Failure("binding-class-missing")
        val statusBarIconViewClass =
            classOrNull(STATUS_BAR_ICON_VIEW, classLoader)
                ?: return InstallResult.Failure("status-bar-icon-view-class-missing")
        val function0Class =
            classOrNull(FUNCTION0, classLoader)
                ?: return InstallResult.Failure("function0-class-missing")
        val statusIconContainerClass =
            classOrNull(STATUS_ICON_CONTAINER, classLoader)
                ?: return InstallResult.Failure("status-icon-container-class-missing")
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
                ?: return InstallResult.Failure("status-icon-container-on-layout-missing")
        val folmeViewStateClass =
            classOrNull(FOLME_VIEW_STATE, classLoader)
                ?: return InstallResult.Failure("folme-view-state-class-missing")
        val newStatusIconStateClass =
            classOrNull(NEW_STATUS_ICON_STATE, classLoader)
                ?: return InstallResult.Failure("new-status-icon-state-class-missing")
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
                ?: return InstallResult.Failure("folme-apply-to-view-missing")
        val translationXField =
            runCatching {
                folmeViewStateClass.getDeclaredField("translationX").apply {
                    check(type == Float::class.javaPrimitiveType) {
                        "folme-translation-x-type-mismatch"
                    }
                    isAccessible = true
                }
            }.getOrNull()
                ?: return InstallResult.Failure("folme-translation-x-field-missing")
        val layoutTranslationXField =
            runCatching {
                newStatusIconStateClass.getDeclaredField("layoutTranslationX").apply {
                    check(type == Float::class.javaPrimitiveType) {
                        "layout-translation-x-type-mismatch"
                    }
                    isAccessible = true
                }
            }.getOrNull()
                ?: return InstallResult.Failure("layout-translation-x-field-missing")

        if (
            !bindableIconClass.isInterface ||
            !creatorClass.isInterface ||
            !bindingClass.isInterface ||
            !function0Class.isInterface
        ) {
            return InstallResult.Failure("proxy-contract-mismatch")
        }

        val visibilityStateMethod =
            bindingClass.methods
                .firstOrNull { method ->
                    method.name == "onVisibilityStateChanged" &&
                        method.parameterTypes.size == 1 &&
                        method.parameterTypes[0] == Int::class.javaPrimitiveType &&
                        method.returnType == Void.TYPE
                }
                ?: return InstallResult.Failure(
                    "binding-visibility-state-contract-missing",
                )
        val resolvedVisibilityStates =
            resolveNativeVisibilityStates(statusBarIconViewClass)
                ?: return InstallResult.Failure(
                    "status-bar-visible-state-contract-missing",
                )
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
                ?: return InstallResult.Failure(
                    "modern-view-set-remove-contract-missing",
                )
        val getRemoveFlagMethod =
            modernViewClass.methods
                .firstOrNull { method ->
                    method.name == "getRemoveFlag" &&
                        method.parameterTypes.isEmpty() &&
                        method.returnType == Boolean::class.javaPrimitiveType
                }
                ?.apply { isAccessible = true }
                ?: return InstallResult.Failure(
                    "modern-view-get-remove-flag-contract-missing",
                )
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
                "nativeGeometryWrites=0",
        )

        val function Object() { [native code] } =
            controllerClass.declaredConstructors
                .firstOrNull { it.parameterTypes.lastOrNull() == registryClass }
                ?: return InstallResult.Failure("controller-registry-constructor-missing")
        val iconListParameterIndex =
            function Object() { [native code] }.parameterTypes.indexOfFirst { type ->
                type.name == StatusSlotReservation.STATUS_BAR_ICON_LIST
            }
        if (iconListParameterIndex < 0) {
            return InstallResult.Failure("controller-icon-list-parameter-missing")
        }
        val registryField =
            registryClass.declaredFields
                .firstOrNull { it.name == "bindableIcons" }
                ?: return InstallResult.Failure("registry-list-field-missing")
        registryField.isAccessible = true

        val viewConstructor =
            modernViewClass.declaredConstructors
                .firstOrNull {
                    it.parameterTypes.map { type -> type.name } ==
                        listOf("android.content.Context", "android.util.AttributeSet")
                }
                ?: return InstallResult.Failure("modern-view-constructor-missing")
        viewConstructor.isAccessible = true

        val initView =
            modernViewClass.declaredMethods
                .firstOrNull {
                    it.name == "initView" &&
                        it.parameterTypes.map { type -> type.name } ==
                        listOf("java.lang.String", FUNCTION0)
                }
                ?: return InstallResult.Failure("modern-view-init-missing")
        initView.isAccessible = true
        function Object() { [native code] }.isAccessible = true

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
                return InstallResult.Failure(
                    "visual-bounds-hook-" +
                        (error.message ?: error.javaClass.simpleName),
                )
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
                                        " nativeTranslationWriter=HyperOS " +
                                        "moduleViewTranslationWrites=0 peerNativeGeometryWrites=0",
                                )
                            }
                            chain.proceed()
                        },
                    )
            }.getOrElse { error ->
                runCatching { visualBoundsHandle.unhook() }
                return InstallResult.Failure(
                    "slot-translation-hook-" +
                        (error.message ?: error.javaClass.simpleName),
                )
            }

        val handle =
            runCatching {
                module
                    .hook(function Object() { [native code] })
                    .setId(CONSTRUCTOR_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val registry =
                                chain.getArg(function Object() { [native code] }.parameterCount - 1)
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
                                        StatusSlotReservation.reserveTail(
                                            iconList = iconList,
                                            slot = SLOT,
                                        )
                                ) {
                                    is StatusSlotReservation.ReservationResult.Ready ->
                                        result

                                    is StatusSlotReservation.ReservationResult.Failure -> {
                                        onSlotOrderResult?.invoke(result.result)
                                        onEvent?.invoke(result.result.logLine)
                                        recordFailure("slot-predeclare-" + result.result.reason)
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
                                    StatusSlotReservation.Result.Failure(
                                        if (slotRolledBack) {
                                            "transaction-aborted-registry-replacement"
                                        } else {
                                            "transaction-aborted-registry-replacement-slot-rollback-failed"
                                        },
                                    )
                                onSlotOrderResult?.invoke(slotFailure)
                                onEvent?.invoke(slotFailure.logLine)
                                recordFailure("registry-replacement-failed")
                                return@Hooker chain.proceed()
                            }

                            injected = true
                            failureReason = null
                            var controllerCreated = false
                            try {
                                val result = chain.proceed()
                                controllerCreated = true
                                onSlotOrderResult?.invoke(slotPreparation.result)
                                onEvent?.invoke(slotPreparation.result.logLine)
                                onEvent?.invoke(
                                    "nativeCombinedParticipant injected slot=" + SLOT +
                                        " registryOriginal=" + original.size +
                                        " registryExtended=" + extended.size +
                                        " visible=false nativeGeometryWrites=0",
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
                                        StatusSlotReservation.Result.Failure(
                                            if (slotRolledBack) {
                                                "transaction-aborted-controller-construction"
                                            } else {
                                                "transaction-aborted-controller-construction-slot-rollback-failed"
                                            },
                                        )
                                    onSlotOrderResult?.invoke(slotFailure)
                                    onEvent?.invoke(slotFailure.logLine)
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
                                        " slotReserved=" + controllerCreated +
                                        " nativeGeometryWrites=0",
                                )
                            }
                        },
                    )
            }.getOrElse {
                runCatching { slotTranslationHandle.unhook() }
                runCatching { visualBoundsHandle.unhook() }
                return InstallResult.Failure(
                    "constructor-hook-" + (it.message ?: it.javaClass.simpleName),
                )
            }

        visualBoundsHook = visualBoundsHandle
        slotTranslationHook = slotTranslationHandle
        constructorHook = handle
        return InstallResult.Installed
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
        currentSurface = SceneSource.Surface.UNKNOWN
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
            when (val resolution = ParticipantAccess.resolve(host)) {
                is ParticipantAccess.ResolveResult.Ready ->
                    resolution.handles
                is ParticipantAccess.ResolveResult.Failure ->
                    return HotReloadAdoptResult.Failure(resolution.reason)
            }
        val holder =
            ParticipantAccess.iconHolder(
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
            ParticipantAccess.removal(handles.controller.javaClass)
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
                ParticipantAccess.invokeRemoval(
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
            ParticipantAccess.clearBindableEntries(
                handles = handles,
                slot = SLOT,
                expectedHolder = holder,
            )
        if (
            ParticipantAccess.findSlotView(handles.group, SLOT) != null ||
            ParticipantAccess.iconHolder(handles, SLOT) != null
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
                    StatusSlotReservation.reserveTail(
                        iconList = iconList,
                        slot = SLOT,
                    )
            ) {
                is StatusSlotReservation.ReservationResult.Ready ->
                    result
                is StatusSlotReservation.ReservationResult.Failure ->
                    return HotReloadAdoptResult.Failure(
                        "slot-reservation-" + result.result.reason,
                    )
            }

        val rebound =
            runCatching {
                initializerField.set(holder, creator)
                slotField.set(holder, SLOT)
                visibleField.setBoolean(holder, true)
                ParticipantAccess.invokeSetIconHolder(
                    handles = handles,
                    slot = SLOT,
                    holder = holder,
                )
                ParticipantAccess.iconHolder(handles, SLOT) === holder &&
                    ParticipantAccess.findSlotView(handles.group, SLOT) != null
            }.getOrDefault(false)
        if (!rebound) {
            runCatching {
                ParticipantAccess.invokeRemoval(
                    handles = handles,
                    removal = removal,
                    slot = SLOT,
                )
            }
            ParticipantAccess.clearBindableEntries(
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
                " viewReady=true managerEntriesRefreshed=true " +
                "clearedManagerEntries=" + cleared +
                " mainThread=true nativeGeometryWrites=0",
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
        val resolution = ParticipantAccess.resolve(host)
        val handles =
            when (resolution) {
                is ParticipantAccess.ResolveResult.Ready ->
                    resolution.handles
                is ParticipantAccess.ResolveResult.Failure ->
                    return AttachResult.Failure(resolution.reason)
            }

        val root =
            ParticipantAccess.findSlotView(handles.group, SLOT) as? FrameLayout
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
            StatusBarSession.currentSlotMetrics(host)
        val stableStatusIconsWidth =
            StatusSlotGeometry.resolveCapturedOrLiveChildWidth(
                capturedWidth = stableSlotMetrics?.statusIconsWidth,
                layoutWidth = statusIcons.width,
                measuredWidth = statusIcons.measuredWidth,
            ) ?: return AttachResult.Failure("status-icons-width-not-ready")
        val stablePrivacyWidth =
            privacy
                ?.takeIf { view -> view.visibility == View.VISIBLE }
                ?.let { view ->
                    StatusSlotGeometry.resolveStableChildWidth(
                        layoutWidth = view.width,
                        measuredWidth = view.measuredWidth,
                    )
                }
                ?: 0
        val slotGeometry =
            StatusSlotGeometry.resolve(
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
            resolveNativeSlotTranslationX(
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
                " slotTranslationX=" + activeSlotTranslationX +
                " readOnly=true nativeGeometryWrites=0",
        )

        val rootLayoutParams =
            root.layoutParams
                ?: return AttachResult.Failure("native-root-layout-params-missing")
        val targetShellWidth =
            resolveNativeSlotOccupancyWidth(
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
                " customShellHeightWrite=" + (originalShellHeight != activeSlotHeight) +
                " peerNativeGeometryWrites=0",
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
        renderController?.updateVisualSettings(
            VisualPrefsOwner.currentSettings(),
        )
        featureEnabled =
            FeaturePrefsOwner.currentSettings().enabled

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
            TintSource.currentState(battery)
        val tintUpdate =
            batteryTintState?.let { batteryTint ->
                renderController?.updateTint(
                    mergeNativeParticipantTint(
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
                    colorHex(batteryTintState?.appliedTint) +
                    " nativeGeometryWrites=0",
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
            SceneSource.currentState(battery)?.surface
                ?: SceneSource.Surface.UNKNOWN
        reconcileVisibleHandoff("attach")

        return AttachResult.Ready(
            registryRestored = registryRestored,
            rootClass = root.javaClass.name,
            rootVisibility = visibilityName(root.visibility),
            iconVisible = ParticipantAccess.iconVisible(root),
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
        if (
            update?.model != null &&
            !modelReadyLogged
        ) {
            modelReadyLogged = true
            val render = renderViewRef?.get()
            val root = rootRef?.get()
            eventSink?.invoke(
                "nativeCombinedParticipant rendererReady " +
                    "modelReady=true" +
                    " candidateComplete=" + update.candidateComplete +
                    " render=" +
                    (render?.measuredWidth ?: -1) + "x" +
                    (render?.measuredHeight ?: -1) +
                    " rootVisibility=" +
                    (root?.let { visibilityName(it.visibility) } ?: "none") +
                    " iconVisible=" +
                    (root?.let { ParticipantAccess.iconVisible(it) } ?: "none") +
                    " visible=" + handoffCommitted +
                    " nativeGeometryWrites=0",
            )
        }
        reconcileVisibleHandoff("state")
    }

    @Synchronized
    fun onSceneUpdate(update: SceneSource.SceneUpdate) {
        val sourceBattery = batteryRef?.get()
        if (sourceBattery != null && update.sourceView !== sourceBattery) {
            return
        }
        currentSurface = update.surface
        reconcileVisibleHandoff("scene-" + update.surface.name)
        if (
            update.surface != SceneSource.Surface.UNLOCKED_STATUS_BAR ||
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
                (rootRef?.get()?.let { visibilityName(it.visibility) } ?: "none") +
                " visible=false nativeGeometryWrites=0",
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
    fun onFeatureSettingsChanged(settings: FeatureSettings) {
        val root = rootRef?.get()
        if (
            root != null &&
            Looper.myLooper() !== Looper.getMainLooper()
        ) {
            root.post {
                onFeatureSettingsChanged(settings)
            }
            return
        }
        if (featureEnabled == settings.enabled) {
            return
        }
        featureEnabled = settings.enabled
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
            !isActiveSlotHandoffReady(
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
                resolveNativeFeatureRemoveFlag(
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
                        "source=feature-enabled reason=suppression-transaction-failed " +
                        "failNative=true nativeGeometryWrites=0",
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
                        "source=feature-enabled reason=set-remove-failed " +
                        "failNative=true nativeGeometryWrites=0",
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
                    "source=feature-enabled validated=true " +
                    "mode=native-remove-lifecycle rootShown=" + root.isShown +
                    " visibilityAuthority=binding+removeFlag" +
                    " nativeRemoveFlag=" + readNativeRemoveFlag(root) +
                    " nativeGeometryWrites=0",
            )
            return true
        } finally {
            handoffPending = false
        }
    }

    @Synchronized
    fun onVisualSettingsChanged(settings: VisualSettings) {
        renderController?.updateVisualSettings(settings)
    }

    @Synchronized
    fun onTintUpdate(update: TintSource.TintUpdate) {
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
                mergeNativeParticipantTint(
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
    private fun onNativeBindingVisibilityStateChanged(
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
            resolveNativeContentVisibility(
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
                    (dot?.let { visibilityName(it.visibility) } ?: "none") +
                    " nativeGeometryWrites=0",
            )
            root?.postOnAnimation {
                eventSink?.invoke(
                    "nativeCombinedParticipant visibilityStateFrame " +
                        "state=" + state +
                        " rootAlpha=" + root.alpha +
                        " rootScale=" + root.scaleX + "x" + root.scaleY +
                        " rootTranslation=" +
                        root.translationX + "," + root.translationY +
                        " nativeGeometryWrites=0",
                )
            }
        }
    }

    internal fun resolveNativeContentVisibility(
        state: Int,
        iconState: Int?,
        dotState: Int?,
        hiddenState: Int?,
    ): NativeContentVisibility? {
        if (iconState != null && state == iconState) {
            return NativeContentVisibility(
                renderVisibility = View.VISIBLE,
                dotVisibility = View.GONE,
            )
        }
        if (dotState != null && state == dotState) {
            return NativeContentVisibility(
                renderVisibility = View.INVISIBLE,
                dotVisibility = View.VISIBLE,
            )
        }
        if (hiddenState != null && state == hiddenState) {
            return NativeContentVisibility(
                renderVisibility = View.INVISIBLE,
                dotVisibility = View.INVISIBLE,
            )
        }
        return null
    }

    private fun resolveNativeVisibilityStates(
        statusBarIconViewClass: Class<*>,
    ): NativeVisibilityStates? {
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
        return resolveNativeVisibilityStates { candidate ->
            runCatching {
                stateNameMethod.invoke(null, candidate) as? String
            }.getOrNull()
        }
    }

    internal fun resolveNativeVisibilityStates(
        stateName: (Int) -> String?,
    ): NativeVisibilityStates? {
        var icon: Int? = null
        var dot: Int? = null
        var hidden: Int? = null

        for (candidate in 0..MAX_NATIVE_VISIBLE_STATE_PROBE) {
            when (stateName(candidate)?.trim()?.uppercase()) {
                "ICON" -> icon = candidate
                "DOT" -> dot = candidate
                "HIDDEN" -> hidden = candidate
            }
            if (icon != null && dot != null && hidden != null) {
                break
            }
        }

        val resolvedIcon = icon ?: return null
        val resolvedDot = dot ?: return null
        val resolvedHidden = hidden ?: return null
        if (setOf(resolvedIcon, resolvedDot, resolvedHidden).size != 3) {
            return null
        }
        return NativeVisibilityStates(
            icon = resolvedIcon,
            dot = resolvedDot,
            hidden = resolvedHidden,
        )
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
                    "tint=#" + tint.toUInt().function toString() { [native code] }(16).padStart(8, '0') +
                    " parameterCount=" + parameterCount +
                    " nativeGeometryWrites=0",
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
            val slot = ParticipantAccess.slotOf(child) ?: continue
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
                        " reason=native-restore-transaction-failed " +
                        "failNative=true nativeGeometryWrites=0",
                )
                return
            }

            val bindingVisibilityChanged = bindingState?.visible == true
            if (bindingVisibilityChanged) {
                bindingState?.visible = false
            }

            val nativeRemoveFlag =
                resolveNativeFeatureRemoveFlag(
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
                        " enabled=false" +
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
                        (root?.layoutParams?.width ?: Int.MIN_VALUE) +
                        " nativeGeometryWrites=0 peerNativeGeometryWrites=0",
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
                "#" + value.toUInt().function toString() { [native code] }(16).padStart(8, '0')
            }
            ?: "none"

    internal fun mergeNativeParticipantTint(
        batteryTint: TintState,
        nativeTint: Int?,
    ): TintState {
        val resolvedNativeTint =
            nativeTint
                ?.takeIf { color -> (color ushr 24) != 0 }
        return if (resolvedNativeTint != null) {
            TintState(
                appliedTint = resolvedNativeTint,
                statusIconTint = resolvedNativeTint,
            )
        } else {
            TintState(
                appliedTint = batteryTint.appliedTint,
            )
        }
    }

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
            resolveHandoffMode(
                surface = currentSurface,
                rootShown = root.isShown,
            )
        if (
            handoffPending ||
            !modelReady ||
            !tintReady ||
            handoffMode == HandoffMode.BLOCKED ||
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
                " rootShownBefore=" + root.isShown +
                " bootstrapVisibilityRelease=true nativeGeometryWrites=0",
        )

        val listener =
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    removePendingPreDraw()
                    synchronized(this@NativeParticipantUi) {
                        try {
                            if (
                            rootRef?.get() !== root ||
                            targetBindingState !== bindingState
                        ) {
                            return@synchronized
                        }

                        val iconVisible =
                            ParticipantAccess.iconVisible(root) == true
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
                            isZeroSlotHandoffReady(
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
                            resolveHandoffMode(
                                surface = currentSurface,
                                rootShown = root.isShown,
                            )
                        val ready =
                            modelReady &&
                                tintReady &&
                                resolvedMode != HandoffMode.BLOCKED &&
                                bindingState.visible &&
                                iconVisible &&
                                root.isAttachedToWindow &&
                                bridgeReady

                        if (ready) {
                            // The native battery slot is the single end-side occupancy owner.
                            // Keep the replacement shell zero-width and draw into that slot.
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
                                        "reason=suppression-transaction-failed " +
                                        "failNative=true nativeGeometryWrites=0",
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
                                    (root.layoutParams?.width ?: Int.MIN_VALUE) + " " +
                                    "iconVisible=true overlayActive=false " +
                                    "peerNativeGeometryWrites=0",
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
                                    " bridgeReady=" + bridgeReady +
                                    " overlayActive=true nativeGeometryWrites=0",
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

    internal fun resolveNativeSlotTranslationX(
        statusIconsWidth: Int,
        rootLeft: Int,
    ): Float? =
        (statusIconsWidth - rootLeft)
            .takeIf { translation -> translation >= 0 }
            ?.toFloat()

    private fun currentNativeSlotTranslationX(root: View): Float? =
        activeSlotTranslationX
            ?: activeSlotBoundaryWidth
                .takeIf { width -> width > 0 }
                ?.let { boundaryWidth ->
                    resolveNativeSlotTranslationX(
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
            resolveNativeSlotTranslationX(
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

    internal fun isZeroSlotHandoffReady(
        rootMeasuredWidth: Int,
        rootMeasuredHeight: Int,
        nativeBatteryHidden: Boolean = false,
        renderMeasuredWidth: Int,
        renderMeasuredHeight: Int,
        expectedVisualWidth: Int,
        expectedVisualHeight: Int,
        parentClipsChildren: Boolean,
        rootScreenX: Int,
        slotAnchorScreenX: Int,
        renderLeft: Int,
        renderRight: Int,
    ): Boolean =
        rootMeasuredWidth ==
            resolveNativeSlotOccupancyWidth(
                nativeBatteryHidden = nativeBatteryHidden,
                visualWidth = expectedVisualWidth,
            ) &&
            rootMeasuredHeight > 0 &&
            renderMeasuredWidth == expectedVisualWidth &&
            renderMeasuredHeight == expectedVisualHeight &&
            expectedVisualWidth > 0 &&
            expectedVisualHeight > 0 &&
            !parentClipsChildren &&
            rootScreenX == slotAnchorScreenX &&
            renderLeft == 0 &&
            renderRight == expectedVisualWidth

    internal fun isActiveSlotHandoffReady(
        rootLayoutWidth: Int,
        rootLayoutHeight: Int,
        nativeBatteryHidden: Boolean = false,
        renderMeasuredWidth: Int,
        renderMeasuredHeight: Int,
        expectedVisualWidth: Int,
        expectedVisualHeight: Int,
        parentClipsChildren: Boolean,
        rootScreenX: Int,
        slotAnchorScreenX: Int,
        renderLeft: Int,
        renderRight: Int,
    ): Boolean =
        rootLayoutWidth ==
            resolveNativeSlotOccupancyWidth(
                nativeBatteryHidden = nativeBatteryHidden,
                visualWidth = expectedVisualWidth,
            ) &&
            rootLayoutHeight == expectedVisualHeight &&
            renderMeasuredWidth == expectedVisualWidth &&
            renderMeasuredHeight == expectedVisualHeight &&
            expectedVisualWidth > 0 &&
            expectedVisualHeight > 0 &&
            !parentClipsChildren &&
            rootScreenX == slotAnchorScreenX &&
            renderLeft == 0 &&
            renderRight == expectedVisualWidth

    internal enum class HandoffMode {
        BLOCKED,
        VISIBLE_HOME,
        PREARMED_KEYGUARD,
    }

    internal fun resolveHandoffMode(
        surface: SceneSource.Surface,
        rootShown: Boolean,
    ): HandoffMode =
        when {
            surface == SceneSource.Surface.UNLOCKED_STATUS_BAR ->
                HandoffMode.VISIBLE_HOME
            surface == SceneSource.Surface.KEYGUARD && !rootShown ->
                HandoffMode.PREARMED_KEYGUARD
            else ->
                HandoffMode.BLOCKED
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
            resolveNativeSlotOccupancyWidth(
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
                " moduleOwnedRootWidthWrite=true peerNativeGeometryWrites=0",
        )
        return root.layoutParams?.width == targetWidth
    }

    internal fun resolveNativeSlotOccupancyWidth(
        nativeBatteryHidden: Boolean,
        visualWidth: Int,
    ): Int? =
        visualWidth
            .takeIf { width -> width > 0 }
            ?.let { width ->
                if (nativeBatteryHidden) {
                    width
                } else {
                    ZERO_SLOT_WIDTH
                }
            }

    internal fun resolvePostLayoutVisualWidth(
        layoutWidth: Int,
        measuredWidth: Int,
        visualWidth: Int,
        nativeBatteryHidden: Boolean = false,
    ): Int? {
        val occupancyWidth =
            resolveNativeSlotOccupancyWidth(
                nativeBatteryHidden = nativeBatteryHidden,
                visualWidth = visualWidth,
            ) ?: return null
        return visualWidth.takeIf {
            layoutWidth == occupancyWidth &&
                measuredWidth == occupancyWidth
        }
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
            resolvePostLayoutVisualWidth(
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
            resolveNativeSlotOccupancyWidth(
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
                    " moduleVisualBoundsWrites=1 peerNativeGeometryWrites=0",
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

    internal fun resolveNativeFeatureRemoveFlag(
        featureEnabled: Boolean,
        handoffValidated: Boolean,
    ): Boolean? =
        if (handoffValidated) {
            !featureEnabled
        } else {
            null
        }

    private fun resolveCurrentHandles(): ParticipantAccess.Handles? {
        val host = hostRef?.get() ?: return null
        return when (val resolution = ParticipantAccess.resolve(host)) {
            is ParticipantAccess.ResolveResult.Ready ->
                resolution.handles
            is ParticipantAccess.ResolveResult.Failure ->
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
    fun detach(): DetachResult {
        val handles =
            resolveCurrentHandles()
                ?: return reset(DetachResult.NotAttached)
        val removal =
            ParticipantAccess.removal(handles.controller.javaClass)
                ?: return reset(DetachResult.Failure("removal-contract-missing"))

        val result =
            runCatching {
                renderViewRef?.get()?.let { render ->
                    (render.parent as? ViewGroup)?.removeView(render)
                }
                ParticipantAccess.invokeRemoval(
                    handles = handles,
                    removal = removal,
                    slot = SLOT,
                )
                ParticipantAccess.clearBindableEntries(
                    handles = handles,
                    slot = SLOT,
                )
                DetachResult.Ready
            }.getOrElse {
                DetachResult.Failure(
                    "remove-" + (it.message ?: it.javaClass.simpleName),
                )
            }
        return reset(result)
    }

    @Synchronized
    fun resetRuntimeState() {
        constructorHook = null
        visualBoundsHook = null
        slotTranslationHook = null
        reset(Unit)
    }

    private fun <T> reset(result: T): T {
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
        currentSurface = SceneSource.Surface.UNKNOWN
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
        return result
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
                                onNativeBindingVisibilityStateChanged(
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
            else -> visibility.function toString() { [native code] }()
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

    internal sealed interface InstallResult {
        data object Installed : InstallResult
        data object AlreadyInstalled : InstallResult
        data class Failure(val reason: String) : InstallResult
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
                                " geometryWrites=0",
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

        class TrackedView private function Object() { [native code] }(
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

    internal data class NativeContentVisibility(
        val renderVisibility: Int,
        val dotVisibility: Int,
    )

    internal data class NativeVisibilityStates(
        val icon: Int,
        val dot: Int,
        val hidden: Int,
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

    internal sealed interface DetachResult {
        data object Ready : DetachResult
        data object NotAttached : DetachResult
        data class Failure(val reason: String) : DetachResult
    }
}
