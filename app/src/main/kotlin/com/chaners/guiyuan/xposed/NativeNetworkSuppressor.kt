package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.graphics.drawable.Icon
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

internal object NativeNetworkSuppressor {
    private const val WIFI_BINDING_CLASS =
        "com.android.systemui.statusbar.pipeline.wifi.ui.binder.MiuiWifiViewBinder\$bind\$2"
    private const val MOBILE_BINDING_CLASS =
        "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder\$bind\$2"
    private const val HOME_MANAGER_CLASS =
        "com.android.systemui.statusbar.phone.ui.DarkIconManager"
    private const val STATUS_BAR_ICON_CONTROLLER_IMPL_CLASS =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl"
    private const val STATUS_BAR_ICON_VIEW_CLASS =
        "com.android.systemui.statusbar.StatusBarIconView"
    private const val MODERN_BINDING_INTERFACE =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewBinding"
    private const val DARK_ICON_DISPATCHER_CLASS =
        "com.android.systemui.plugins.DarkIconDispatcher"

    private const val WIFI_VISIBILITY_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.wifiVisibility"
    private const val MOBILE_VISIBILITY_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.mobileVisibility"
    private const val HOME_ICON_ADDED_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.homeIconAdded"
    private const val HOME_MANAGER_REGISTERED_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.homeManagerRegistered"
    private const val AIRPLANE_VISIBILITY_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.airplaneVisibility"

    private val noTargetSlots = emptySet<String>()
    private val wifiOnlyTargetSlots = setOf("wifi")
    private val mobileOnlyTargetSlots = setOf("mobile")
    private val wifiAndMobileTargetSlots = setOf("wifi", "mobile")
    private val observableTargetSlots = setOf("wifi", "mobile", NO_SIM_SLOT)
    private val representedTintSlots =
        setOf("combined_status", "wifi", "mobile", "stacked_mobile", NO_SIM_SLOT, AIRPLANE_SLOT)

    private val installedHandles = mutableListOf<HookHandle>()
    private var activeManager: Any? = null
    private var activeGroup: WeakReference<ViewGroup>? = null
    private var pendingObservationHost: WeakReference<View>? = null
    private var pendingObservationSource: String? = null
    private var observationAttachedSink: ((String) -> Unit)? = null
    private var eventSink: ((String) -> Unit)? = null
    private var statusPresentationSink:
        ((PresentationStore.StatusIconPresentation) -> Unit)? = null
    private var airplaneSlotAccessor: Method? = null
    private var statusIconVisibleAccessor: Method? = null
    private var statusIconSourceAccessor: Method? = null
    private var statusIconStaticColorAccessor: Method? = null
    private var lastStatusPresentation =
        PresentationStore.StatusIconPresentation()

    // The island peer mirror samples native icon state from onLayout. These
    // reflective contracts are stable for the SystemUI process lifetime, so
    // cache discovery by concrete class and keep the per-layout path to invoke
    // + direct Field.get only.
    private val transitionStateAccessorCache = ConcurrentHashMap<Class<*>, Method>()
    private val transitionStateAccessorMissing = ConcurrentHashMap.newKeySet<Class<*>>()
    private val transitionStateFieldCache =
        ConcurrentHashMap<Class<*>, TransitionStateFields>()

    @Volatile
    private var suppressedBindings: Array<WeakReference<Any>> = emptyArray()

    @Volatile
    private var mobileVisualMasks: Array<MobileVisualMaskState> = emptyArray()

    @Volatile
    private var wifiSuppressionEnabled = false

    @Volatile
    private var mobileSuppressionEnabled = false

    @Volatile
    private var airplaneSuppressionEnabled = false

    @Volatile
    private var noSimSuppressionEnabled = false

    @Volatile
    private var observationOnly = false

    val installedHookCount: Int
        @Synchronized get() = installedHandles.size

    val mobileSuppressionActive: Boolean
        @Synchronized get() = activeManager != null && mobileSuppressionEnabled

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        onObservationAttached: ((String) -> Unit)? = null,
        onStatusPresentationChanged:
            ((PresentationStore.StatusIconPresentation) -> Unit)? = null,
    ): String? {
        if (installedHandles.isNotEmpty()) {
            eventSink = onEvent
            observationAttachedSink = onObservationAttached
            statusPresentationSink = onStatusPresentationChanged
            return null
        }

        val created = mutableListOf<HookHandle>()
        return runCatching {
            val wifiVisibility =
                resolveVisibilityMethod(
                    classLoader = classLoader,
                    className = WIFI_BINDING_CLASS,
                )
            val mobileVisibility =
                resolveVisibilityMethod(
                    classLoader = classLoader,
                    className = MOBILE_BINDING_CLASS,
                )
            val statusBarIconView =
                Class.forName(
                    STATUS_BAR_ICON_VIEW_CLASS,
                    false,
                    classLoader,
                )
            val airplaneVisibility =
                statusBarIconView
                    .getDeclaredMethod("isIconVisible")
                    .apply {
                        check(returnType == java.lang.Boolean.TYPE) {
                            "airplane-visibility-return-type-mismatch"
                        }
                        isAccessible = true
                    }
            statusIconVisibleAccessor = airplaneVisibility
            statusIconSourceAccessor =
                statusBarIconView.methods
                    .firstOrNull { method ->
                        method.name == "getSourceIcon" &&
                            method.parameterCount == 0 &&
                            method.returnType == Icon::class.java
                    }
                    ?.apply { isAccessible = true }
            statusIconStaticColorAccessor =
                statusBarIconView.methods
                    .firstOrNull { method ->
                        method.name == "getStaticDrawableColor" &&
                            method.parameterCount == 0 &&
                            (
                                method.returnType == Int::class.javaPrimitiveType ||
                                    method.returnType == Int::class.java
                            )
                    }
                    ?.apply { isAccessible = true }
            airplaneSlotAccessor =
                generateSequence(statusBarIconView) { clazz -> clazz.superclass }
                    .flatMap { clazz -> clazz.declaredMethods.asSequence() }
                    .firstOrNull { method ->
                        method.name == "getSlot" &&
                            method.parameterCount == 0 &&
                            method.returnType == String::class.java
                    }
                    ?.apply { isAccessible = true }
                    ?: error("airplane-slot-accessor-missing")

            val darkIconManager =
                Class.forName(
                    HOME_MANAGER_CLASS,
                    false,
                    classLoader,
                )
            val statusBarIconController =
                Class.forName(
                    STATUS_BAR_ICON_CONTROLLER_IMPL_CLASS,
                    false,
                    classLoader,
                )
            val addIconGroup =
                statusBarIconController.declaredMethods
                    .filter { method ->
                        method.name == "addIconGroup" &&
                            method.parameterCount == 1 &&
                            method.parameterTypes[0].name.contains("IconManager")
                    }
                    .sortedBy { method -> method.parameterTypes[0].name }
                    .firstOrNull()
                    ?: error("home-icon-manager-registration-method-missing")
            addIconGroup.isAccessible = true

            val onIconAdded =
                darkIconManager.declaredMethods
                    .firstOrNull { method ->
                        method.name == "onIconAdded" &&
                            method.parameterCount == 4 &&
                            method.parameterTypes.getOrNull(0) == Integer.TYPE &&
                            method.parameterTypes.getOrNull(1) == String::class.java &&
                            method.parameterTypes.getOrNull(2) == java.lang.Boolean.TYPE
                    }
                    ?: error("home-icon-manager-onIconAdded-missing")
            onIconAdded.isAccessible = true

            created +=
                module
                    .hook(wifiVisibility)
                    .setId(WIFI_VISIBILITY_HOOK_ID)
                    .intercept(visibilityHooker())
            created +=
                module
                    .hook(mobileVisibility)
                    .setId(MOBILE_VISIBILITY_HOOK_ID)
                    .intercept(visibilityHooker())
            created +=
                module
                    .hook(onIconAdded)
                    .setId(HOME_ICON_ADDED_HOOK_ID)
                    .intercept(homeIconAddedHooker())
            created +=
                module
                    .hook(addIconGroup)
                    .setId(HOME_MANAGER_REGISTERED_HOOK_ID)
                    .intercept(homeManagerRegisteredHooker())
            created +=
                module
                    .hook(airplaneVisibility)
                    .setId(AIRPLANE_VISIBILITY_HOOK_ID)
                    .intercept(airplaneVisibilityHooker())

            installedHandles.clear()
            installedHandles.addAll(created)
            eventSink = onEvent
            observationAttachedSink = onObservationAttached
            statusPresentationSink = onStatusPresentationChanged
            null
        }.getOrElse { error ->
            created.forEach { handle ->
                runCatching { handle.unhook() }
            }
            installedHandles.clear()
            airplaneSlotAccessor = null
            statusIconVisibleAccessor = null
            statusIconSourceAccessor = null
            statusIconStaticColorAccessor = null
            suppressedBindings = emptyArray()
            mobileVisualMasks = emptyArray()
            activeManager = null
            activeGroup = null
            pendingObservationHost = null
            pendingObservationSource = null
            eventSink = onEvent
            observationAttachedSink = onObservationAttached
            statusPresentationSink = onStatusPresentationChanged
            error.message ?: error.javaClass.simpleName
        }
    }

    @Synchronized
    fun attachObserver(
        host: Any,
        source: String = "observerAttach",
    ): Result {
        val hostView =
            host as? View
                ?: return Result.Failure("host-not-view")
        val manager =
            NativeParticipantRuntimeAccess.managerFor(hostView)
                ?: run {
                    // MiuiNotificationStatusContainer finishes inflation before the parent
                    // MiuiPhoneStatusBarView necessarily owns its Home DarkIconManager.
                    // Keep a single weak pending host and complete observation from the
                    // authoritative StatusBarIconControllerImpl.addIconGroup registration.
                    pendingObservationHost = WeakReference(hostView)
                    pendingObservationSource = source
                    return Result.Pending("home-dark-icon-manager-registration")
                }
        pendingObservationHost = null
        pendingObservationSource = null
        val group =
            NativeParticipantRuntimeAccess.groupFor(hostView)
                ?: return Result.Failure("status-icon-group-missing")
        if (manager.javaClass.name != HOME_MANAGER_CLASS) {
            return Result.Failure("home-manager-mismatch")
        }

        if (
            activeManager !== manager ||
            activeGroup?.get() !== group ||
            !observationOnly
        ) {
            restoreMobileVisualMasksLocked()
            clearSessionLocked(
                requestLayout = false,
                restoreVisualMasks = false,
            )
        }

        activeManager = manager
        activeGroup = WeakReference(group)
        observationOnly = true
        wifiSuppressionEnabled = false
        mobileSuppressionEnabled = false
        airplaneSuppressionEnabled = false
        noSimSuppressionEnabled = false
        suppressedBindings = emptyArray()
        refreshStatusPresentationLocked("observerAttach")

        eventSink?.invoke(
            "nativeNetworkSuppression observerOnly source=" + source + " " +
                "manager=" + manager.javaClass.name +
                " group=" + group.javaClass.name,
        )
        return Result.Active
    }

    @Synchronized
    fun refreshObservation(source: String) {
        if (observationOnly && activeGroup?.get() != null) {
            refreshStatusPresentationLocked(source)
        }
    }

    @Synchronized
    fun activate(
        host: Any,
        suppressWifi: Boolean,
        suppressMobile: Boolean,
    ): Result {
        if (installedHandles.size != EXPECTED_HOOK_COUNT) {
            return Result.Failure("hooks-not-ready")
        }

        val handles =
            when (val resolution = NativeParticipantRuntimeAccess.resolve(host)) {
                is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                    resolution.handles
                is NativeParticipantRuntimeAccess.ResolveResult.Failure ->
                    return Result.Failure(resolution.reason)
            }

        if (handles.manager.javaClass.name != HOME_MANAGER_CLASS) {
            return Result.Failure("home-manager-mismatch")
        }

        activeManager = handles.manager
        activeGroup = WeakReference(handles.group)
        observationOnly = false
        wifiSuppressionEnabled = suppressWifi
        mobileSuppressionEnabled = suppressMobile
        airplaneSuppressionEnabled = true
        refreshStatusPresentationLocked("handoff")

        val snapshot = refreshBindingsLocked("handoff")
        if (snapshot.failureReason != null) {
            clearSessionLocked(requestLayout = true)
            return Result.Failure(snapshot.failureReason)
        }

        eventSink?.invoke(snapshot.logLine)
        return Result.Active
    }

    @Synchronized
    fun updatePolicy(
        suppressWifi: Boolean,
        suppressMobile: Boolean,
        source: String,
        forceRevalidate: Boolean = false,
    ): Result? {
        val policyChanged =
            wifiSuppressionEnabled != suppressWifi ||
                mobileSuppressionEnabled != suppressMobile
        if (activeManager != null && activeGroup?.get() != null) {
            refreshStatusPresentationLocked(source)
        }
        if (!policyChanged && !forceRevalidate) {
            return null
        }
        wifiSuppressionEnabled = suppressWifi
        mobileSuppressionEnabled = suppressMobile
        if (activeManager == null || activeGroup?.get() == null) {
            return null
        }

        val snapshot = refreshBindingsLocked(source)
        if (snapshot.failureReason != null) {
            clearSessionLocked(requestLayout = true)
            return Result.Failure(snapshot.failureReason)
        }

        eventSink?.invoke(snapshot.logLine)
        return Result.Active
    }

    @Synchronized
    fun deactivate(source: String): Result {
        pendingObservationHost = null
        pendingObservationSource = null
        val group = activeGroup?.get()
        val previousCount = suppressedBindings.count { reference -> reference.get() != null }
        val restoredVisualMasks = restoreMobileVisualMasksLocked()
        clearSessionLocked(
            requestLayout = false,
            restoreVisualMasks = false,
        )
        group?.requestLayout()
        if (previousCount > 0) {
            eventSink?.invoke(
                "nativeNetworkSuppression inactive source=" + source +
                    " restoredBindings=" + previousCount +
                    " restoredMobileVisualMasks=" + restoredVisualMasks,
            )
        }
        return Result.Inactive
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivate(source)
        installedHandles.clear()
        eventSink = null
        observationAttachedSink = null
        statusPresentationSink = null
        statusIconVisibleAccessor = null
        statusIconSourceAccessor = null
        statusIconStaticColorAccessor = null
        lastStatusPresentation =
            PresentationStore.StatusIconPresentation()
    }

    private fun visibilityHooker(): Hooker =
        Hooker { chain ->
            val binding = chain.thisObject
            if (isSuppressed(binding)) {
                false
            } else {
                chain.proceed()
            }
        }

    private fun airplaneVisibilityHooker(): Hooker =
        Hooker { chain ->
            val view = chain.thisObject as? View
            val slot =
                runCatching {
                    airplaneSlotAccessor?.invoke(chain.thisObject) as? String
                }.getOrNull()
            val homeGroup = activeGroup?.get()
            val belongsToActiveHomeGroup =
                view != null &&
                    homeGroup != null &&
                    isDescendantOf(
                        view = view,
                        ancestor = homeGroup,
                    )

            if (
                slot == NO_SIM_SLOT &&
                view != null &&
                belongsToActiveHomeGroup
            ) {
                val nativeVisible = chain.proceed() as? Boolean ?: false
                synchronized(this) {
                    if (homeGroup === activeGroup?.get()) {
                        refreshStatusPresentationLocked(
                            source = "visibility:no_sim",
                            observedNoSimView = view,
                            observedNoSimVisible = nativeVisible,
                        )
                    }
                }
                if (noSimSuppressionEnabled) {
                    false
                } else {
                    nativeVisible
                }
            } else {
                val suppress =
                    shouldSuppressStaticSlot(
                        slot = slot,
                        airplaneSuppressionActive = airplaneSuppressionEnabled,
                        noSimSuppressionActive = noSimSuppressionEnabled,
                        belongsToActiveHomeGroup = belongsToActiveHomeGroup,
                    )
                if (suppress) {
                    false
                } else {
                    chain.proceed()
                }
            }
        }

    private fun homeManagerRegisteredHooker(): Hooker =
        Hooker { chain ->
            val manager = chain.getArg(0)
            val result = chain.proceed()

            if (manager?.javaClass?.name == HOME_MANAGER_CLASS) {
                synchronized(this) {
                    val pendingHost = pendingObservationHost?.get()
                    val pendingSource = pendingObservationSource
                    if (
                        pendingHost != null &&
                        NativeParticipantRuntimeAccess.managerFor(pendingHost) === manager
                    ) {
                        when (
                            val state =
                                attachObserver(
                                    host = pendingHost,
                                    source = pendingSource ?: "homeManagerRegistered",
                                )
                        ) {
                            is Result.Active -> {
                                observationAttachedSink?.invoke(
                                    pendingSource ?: "homeManagerRegistered",
                                )
                                eventSink?.invoke(
                                    "statusIconObservation ready source=" +
                                        (pendingSource ?: "homeManagerRegistered") +
                                        " trigger=homeManagerRegistered",
                                )
                            }
                            is Result.Failure ->
                                eventSink?.invoke(
                                    "statusIconObservation unavailable " +
                                        "source=" + (pendingSource ?: "homeManagerRegistered") +
                                        " reason=" + state.reason,
                                )
                            is Result.Pending,
                            is Result.Inactive,
                            -> Unit
                        }
                    }
                }
            }

            result
        }

    private fun homeIconAddedHooker(): Hooker =
        Hooker { chain ->
            val result = chain.proceed()
            val manager = chain.thisObject
            val slot = chain.getArg(1) as? String
            if (
                manager === activeManager &&
                slot != null &&
                slot in observableTargetSlots
            ) {
                synchronized(this) {
                    if (manager === activeManager) {
                        refreshStatusPresentationLocked("iconAdded:" + slot)
                        if (!observationOnly) {
                            val snapshot = refreshBindingsLocked("iconAdded:" + slot)
                            eventSink?.invoke(snapshot.logLine)
                            if (snapshot.failureReason != null) {
                                clearSessionLocked(requestLayout = true)
                                eventSink?.invoke(
                                    "nativeNetworkSuppression failNative source=iconAdded:" + slot +
                                        " reason=" + snapshot.failureReason,
                                )
                            }
                        }
                    }
                }
            }
            result
        }

    private fun isSuppressed(binding: Any): Boolean =
        suppressedBindings.any { reference ->
            reference.get() === binding
        }

    @Synchronized
    fun currentTransitionTargetGeometry(): TransitionTargetGeometry? {
        val group = activeGroup?.get() ?: return null
        val children =
            (0 until group.childCount)
                .map(group::getChildAt)
        val mobile =
            selectTransitionTarget(
                children.filter { child ->
                    NativeParticipantRuntimeAccess.slotOf(child) == "mobile"
                },
            )
        val wifi =
            selectTransitionTarget(
                children.filter { child ->
                    NativeParticipantRuntimeAccess.slotOf(child) == "wifi"
                },
            )
        val combined =
            selectTransitionTarget(
                children.filter { child ->
                    NativeParticipantRuntimeAccess.slotOf(child) == "combined_status"
                },
            )
        val battery =
            (group.parent as? ViewGroup)
                ?.let { parent ->
                    (0 until parent.childCount)
                        .map(parent::getChildAt)
                        .firstOrNull { child ->
                            child.javaClass.name ==
                                "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
                        }
                }

        return TransitionTargetGeometry(
            group = transitionViewGeometry(group),
            mobile = mobile?.let(::transitionViewGeometry),
            wifi = wifi?.let(::transitionViewGeometry),
            battery = battery?.let(::transitionViewGeometry),
            combined = combined?.let(::transitionViewGeometry),
        )
    }

    private fun selectTransitionTarget(candidates: List<View>): View? =
        candidates.firstOrNull { view ->
            view.visibility == View.VISIBLE &&
                view.width > 0 &&
                view.height > 0
        }
            ?: candidates.firstOrNull { view ->
                view.measuredWidth > 0 &&
                    view.measuredHeight > 0
            }
            ?: candidates.firstOrNull()

    @Synchronized
    fun currentTransitionStateSnapshot(): TransitionStateSnapshot? {
        val group = activeGroup?.get() ?: return null
        val animatorController = readObjectField(group, "animatorController")
        val notificationPanelExpand =
            animatorController
                ?.let { controller -> readObjectField(controller, "notificationPanelExpand") }
                as? Boolean
        val controlPanelExpand =
            animatorController
                ?.let { controller -> readObjectField(controller, "controlPanelExpand") }
                as? Boolean

        val children =
            (0 until group.childCount)
                .map(group::getChildAt)
        fun stateFor(slot: String): TransitionIconState? {
            val view =
                selectTransitionTarget(
                    children.filter { child ->
                        NativeParticipantRuntimeAccess.slotOf(child) == slot
                    },
                ) ?: return null
            return readTransitionIconState(group, view)
        }

        return TransitionStateSnapshot(
            notificationPanelExpand = notificationPanelExpand,
            controlPanelExpand = controlPanelExpand,
            combined = stateFor("combined_status"),
            wifi = stateFor("wifi"),
            mobile = stateFor("mobile"),
        )
    }

    internal fun readTransitionIconState(
        group: ViewGroup,
        view: View,
    ): TransitionIconState? {
        val state = transitionStateObject(group, view) ?: return null
        val fields = transitionStateFields(state.javaClass)
        return TransitionIconState(
            slot = readCachedField(fields.slot, state) as? String,
            visibleState = (readCachedField(fields.visibleState, state) as? Number)?.toInt(),
            inIslandState = (readCachedField(fields.inIslandState, state) as? Number)?.toInt(),
            beforeInIslandState =
                (readCachedField(fields.beforeInIslandState, state) as? Number)?.toInt(),
            islandChanged = readCachedField(fields.islandChanged, state) as? Boolean,
            supportAnim = readCachedField(fields.supportAnim, state) as? Boolean,
            forceAppear = readCachedField(fields.forceAppear, state) as? Boolean,
            layoutTranslationX =
                (readCachedField(fields.layoutTranslationX, state) as? Number)?.toFloat(),
        )
    }

    internal fun readIslandVisibilityState(
        group: ViewGroup,
        view: View,
    ): IslandVisibilityState? {
        val state = transitionStateObject(group, view) ?: return null
        val fields = transitionStateFields(state.javaClass)
        return IslandVisibilityState(
            visibleState = (readCachedField(fields.visibleState, state) as? Number)?.toInt(),
            inIslandState = (readCachedField(fields.inIslandState, state) as? Number)?.toInt(),
        )
    }

    private fun transitionStateObject(
        group: ViewGroup,
        view: View,
    ): Any? {
        val groupClass = group.javaClass
        val accessor =
            transitionStateAccessorCache[groupClass]
                ?: run {
                    if (groupClass in transitionStateAccessorMissing) {
                        return null
                    }
                    val companionClass =
                        runCatching {
                            Class.forName(
                                groupClass.name + "\$Companion",
                                false,
                                groupClass.classLoader,
                            )
                        }.getOrNull()
                    val resolved =
                        companionClass
                            ?.declaredMethods
                            ?.firstOrNull { method ->
                                method.name == "access\$getViewStateFromChild" &&
                                    method.parameterTypes.contentEquals(arrayOf(View::class.java))
                            }
                    if (resolved == null) {
                        transitionStateAccessorMissing += groupClass
                        return null
                    }
                    resolved.isAccessible = true
                    transitionStateAccessorCache.putIfAbsent(groupClass, resolved) ?: resolved
                }
        return runCatching {
            accessor.invoke(null, view)
        }.getOrNull()
    }

    private fun transitionStateFields(stateClass: Class<*>): TransitionStateFields =
        transitionStateFieldCache[stateClass]
            ?: TransitionStateFields(
                slot = resolveCachedField(stateClass, "slot"),
                visibleState = resolveCachedField(stateClass, "visibleState"),
                inIslandState = resolveCachedField(stateClass, "inIslandState"),
                beforeInIslandState = resolveCachedField(stateClass, "beforeInIslandState"),
                islandChanged = resolveCachedField(stateClass, "islandChanged"),
                supportAnim = resolveCachedField(stateClass, "supportAnim"),
                forceAppear = resolveCachedField(stateClass, "forceAppear"),
                layoutTranslationX = resolveCachedField(stateClass, "layoutTranslationX"),
            ).let { resolved ->
                transitionStateFieldCache.putIfAbsent(stateClass, resolved) ?: resolved
            }

    private fun resolveCachedField(
        clazz: Class<*>,
        name: String,
    ): Field? =
        generateSequence(clazz) { candidate -> candidate.superclass }
            .mapNotNull { candidate ->
                candidate.declaredFields.firstOrNull { field -> field.name == name }
            }
            .firstOrNull()
            ?.apply { isAccessible = true }

    private fun readCachedField(
        field: Field?,
        target: Any,
    ): Any? =
        field?.let { resolved ->
            runCatching { resolved.get(target) }.getOrNull()
        }

    private fun transitionViewGeometry(view: View): TransitionViewGeometry {
        val location = IntArray(2)
        val located =
            runCatching {
                view.getLocationOnScreen(location)
                true
            }.getOrDefault(false)
        return TransitionViewGeometry(
            className = view.javaClass.simpleName,
            screenX = if (located) location[0] else Int.MIN_VALUE,
            screenY = if (located) location[1] else Int.MIN_VALUE,
            width = view.width,
            height = view.height,
            measuredWidth = view.measuredWidth,
            measuredHeight = view.measuredHeight,
            visibility = view.visibility,
            alpha = view.alpha,
        )
    }

    internal data class TransitionStateSnapshot(
        val notificationPanelExpand: Boolean?,
        val controlPanelExpand: Boolean?,
        val combined: TransitionIconState?,
        val wifi: TransitionIconState?,
        val mobile: TransitionIconState?,
    ) {
        val summary: String
            get() =
                "notificationPanelExpand=" + (notificationPanelExpand ?: "unknown") +
                    " controlPanelExpand=" + (controlPanelExpand ?: "unknown") +
                    " combined=" + (combined?.summary ?: "missing") +
                    " wifi=" + (wifi?.summary ?: "missing") +
                    " mobile=" + (mobile?.summary ?: "missing")
    }

    internal data class IslandVisibilityState(
        val visibleState: Int?,
        val inIslandState: Int?,
    )

    private data class TransitionStateFields(
        val slot: Field?,
        val visibleState: Field?,
        val inIslandState: Field?,
        val beforeInIslandState: Field?,
        val islandChanged: Field?,
        val supportAnim: Field?,
        val forceAppear: Field?,
        val layoutTranslationX: Field?,
    )

    internal data class TransitionIconState(
        val slot: String?,
        val visibleState: Int?,
        val inIslandState: Int?,
        val beforeInIslandState: Int?,
        val islandChanged: Boolean?,
        val supportAnim: Boolean?,
        val forceAppear: Boolean?,
        val layoutTranslationX: Float?,
    ) {
        val summary: String
            get() =
                "{" +
                    "slot=" + (slot ?: "unknown") +
                    ",visibleState=" + (visibleState ?: "unknown") +
                    ",inIslandState=" + (inIslandState ?: "unknown") +
                    ",beforeInIslandState=" + (beforeInIslandState ?: "unknown") +
                    ",islandChanged=" + (islandChanged ?: "unknown") +
                    ",supportAnim=" + (supportAnim ?: "unknown") +
                    ",forceAppear=" + (forceAppear ?: "unknown") +
                    ",layoutTranslationX=" + (layoutTranslationX ?: "unknown") +
                    "}"
    }

    internal data class TransitionTargetGeometry(
        val group: TransitionViewGeometry,
        val mobile: TransitionViewGeometry?,
        val wifi: TransitionViewGeometry?,
        val battery: TransitionViewGeometry?,
        val combined: TransitionViewGeometry?,
    ) {
        val summary: String
            get() =
                "group=" + group.summary +
                    " mobile=" + (mobile?.summary ?: "missing") +
                    " wifi=" + (wifi?.summary ?: "missing") +
                    " battery=" + (battery?.summary ?: "missing") +
                    " combined=" + (combined?.summary ?: "missing")
    }

    internal data class TransitionViewGeometry(
        val className: String,
        val screenX: Int,
        val screenY: Int,
        val width: Int,
        val height: Int,
        val measuredWidth: Int,
        val measuredHeight: Int,
        val visibility: Int,
        val alpha: Float,
    ) {
        val summary: String
            get() =
                className +
                    "{x=" + screenX +
                    ",y=" + screenY +
                    ",size=" + width + "x" + height +
                    ",measured=" + measuredWidth + "x" + measuredHeight +
                    ",visibility=" + visibility +
                    ",alpha=" + alpha +
                    "}"
    }

    @Synchronized
    fun currentAppliedStatusIconTint(anchorView: View? = null): Int? {
        val group = activeGroup?.get()
        val peerTint = group?.let(::resolveAppliedStatusIconTint)
        val resolvedAnchor =
            anchorView
                ?.takeIf(::isTintAuthorityCandidate)
                ?: group?.let(::resolveTintAnchorView)
        val locationAwareTint =
            resolveLocationAwareManagerTint(
                manager = activeManager,
                anchorView = resolvedAnchor,
            )
        val managerFallbackTint =
            resolveManagerFallbackTint(activeManager)
        return NativeNetworkSuppressionPolicy.statusIconTint(
            locationAwareTint = locationAwareTint,
            peerAppliedTint = peerTint,
            managerFallbackTint = managerFallbackTint,
            fallbackTint = lastStatusPresentation.appliedTint,
        )
    }

    private fun refreshStatusPresentationLocked(
        source: String,
        observedNoSimView: View? = null,
        observedNoSimVisible: Boolean? = null,
    ) {
        val group = activeGroup?.get() ?: return
        val noSimView =
            observedNoSimView
                ?: (0 until group.childCount)
                    .map(group::getChildAt)
                    .firstOrNull { child ->
                        NativeParticipantRuntimeAccess.slotOf(child) == NO_SIM_SLOT
                    }
        val noSimVisible =
            observedNoSimVisible
                ?: (noSimView?.let(::isNativeStatusIconVisible) == true)
        val noSimIcon =
            if (noSimVisible) {
                noSimView?.let(::resolveNativeIconResource)
            } else {
                null
            }
        val peerTint = resolveAppliedStatusIconTint(group)
        val tintAnchor = resolveTintAnchorView(group)
        val locationAwareTint =
            resolveLocationAwareManagerTint(
                manager = activeManager,
                anchorView = tintAnchor,
            )
        val managerFallbackTint =
            resolveManagerFallbackTint(activeManager)
        val appliedTint =
            NativeNetworkSuppressionPolicy.statusIconTint(
                locationAwareTint = locationAwareTint,
                peerAppliedTint = peerTint,
                managerFallbackTint = managerFallbackTint,
                fallbackTint = lastStatusPresentation.appliedTint,
            )
        val presentation =
            PresentationStore.StatusIconPresentation(
                appliedTint = appliedTint,
                noSimVisible = noSimVisible,
                noSimIcon = noSimIcon,
            )
        val previousNoSimSuppression = noSimSuppressionEnabled
        noSimSuppressionEnabled =
            !observationOnly &&
                presentation.noSimVisible &&
                presentation.noSimIcon != null

        if (presentation != lastStatusPresentation) {
            lastStatusPresentation = presentation
            statusPresentationSink?.invoke(presentation)
            eventSink?.invoke(
                "statusIconPresentation source=" + source +
                    " tint=" +
                    (presentation.appliedTint
                        ?.toUInt()
                        ?.toString(16)
                        ?.padStart(8, '0')
                        ?: "none") +
                    " tintAuthority=" +
                    when {
                        locationAwareTint != null -> "dispatcher-location-aware"
                        peerTint != null -> "peer-static-applied"
                        managerFallbackTint != null -> "manager-global-fallback"
                        else -> "cached-fallback"
                    } +
                    " locationAwareTint=" +
                    (locationAwareTint
                        ?.toUInt()
                        ?.toString(16)
                        ?.padStart(8, '0')
                        ?: "none") +
                    " peerTint=" +
                    (peerTint
                        ?.toUInt()
                        ?.toString(16)
                        ?.padStart(8, '0')
                        ?: "none") +
                    " managerFallbackTint=" +
                    (managerFallbackTint
                        ?.toUInt()
                        ?.toString(16)
                        ?.padStart(8, '0')
                        ?: "none") +
                    " tintAnchorSlot=" +
                    (tintAnchor?.let(NativeParticipantRuntimeAccess::slotOf) ?: "none") +
                    " tintAnchorClass=" +
                    (tintAnchor?.javaClass?.simpleName ?: "none") +
                    " noSimVisible=" + presentation.noSimVisible +
                    " noSimResource=" +
                    (
                        presentation.noSimIcon?.packageName +
                            ":" +
                            presentation.noSimIcon?.resourceId
                    ) +
                    " noSimSuppressed=" + noSimSuppressionEnabled,
            )
        }

        if (previousNoSimSuppression != noSimSuppressionEnabled) {
            noSimView?.invalidate()
            group.requestLayout()
        }
    }

    private fun isNativeStatusIconVisible(view: View): Boolean {
        if (view.visibility != View.VISIBLE) {
            return false
        }
        val accessor = statusIconVisibleAccessor ?: return true
        return runCatching {
            accessor.invoke(view) as? Boolean
        }.getOrNull() ?: true
    }

    private fun resolveNativeIconResource(
        view: View,
    ): PresentationStore.NativeIconResource? {
        val accessor = statusIconSourceAccessor ?: return null
        val icon =
            runCatching {
                accessor.invoke(view) as? Icon
            }.getOrNull() ?: return null
        if (icon.type != Icon.TYPE_RESOURCE) {
            return null
        }
        val packageName =
            icon.resPackage
                ?.takeIf(String::isNotBlank)
                ?: view.context.packageName
        val resourceId = icon.resId
        if (resourceId == 0) {
            return null
        }
        return PresentationStore.NativeIconResource(
            packageName = packageName,
            resourceId = resourceId,
        )
    }

    private fun resolveLocationAwareManagerTint(
        manager: Any?,
        anchorView: View?,
    ): Int? {
        manager ?: return null
        anchorView ?: return null

        val dispatcher =
            readObjectField(manager, "mDarkIconDispatcher")
                ?: return null
        val iconTint =
            readIntField(dispatcher, "mIconTint")
                ?.takeIf(NativeNetworkSuppressionPolicy::isVisibleTint)
                ?: return null
        val tintAreas =
            readObjectField(dispatcher, "mTintAreas")
                as? Collection<*>
                ?: return null

        return runCatching {
            val dispatcherType =
                Class.forName(
                    DARK_ICON_DISPATCHER_CLASS,
                    false,
                    manager.javaClass.classLoader,
                )
            val getTint =
                dispatcherType.methods.firstOrNull { method ->
                    method.name == "getTint" &&
                        method.parameterCount == 3 &&
                        View::class.java.isAssignableFrom(
                            method.parameterTypes.getOrNull(1),
                        ) &&
                        method.parameterTypes.getOrNull(2) ==
                            Int::class.javaPrimitiveType
                } ?: return@runCatching null
            (getTint.invoke(null, tintAreas, anchorView, iconTint) as? Number)
                ?.toInt()
                ?.takeIf(NativeNetworkSuppressionPolicy::isVisibleTint)
        }.getOrNull()
    }

    private fun resolveManagerFallbackTint(manager: Any?): Int? {
        manager ?: return null

        readIntField(manager, "mColor")
            ?.takeIf(NativeNetworkSuppressionPolicy::isVisibleTint)
            ?.let { return it }

        val dispatcher =
            readObjectField(manager, "mDarkIconDispatcher")
                ?: return null
        return readIntField(dispatcher, "mIconTint")
            ?.takeIf(NativeNetworkSuppressionPolicy::isVisibleTint)
    }

    private fun resolveTintAnchorView(group: ViewGroup): View? {
        for (index in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(index)
            if (
                isTintAuthorityCandidate(
                    slot = NativeParticipantRuntimeAccess.slotOf(child),
                    visible = child.visibility == View.VISIBLE,
                    width = child.width,
                    height = child.height,
                )
            ) {
                return child
            }
        }
        return null
    }

    private fun isTintAuthorityCandidate(view: View): Boolean =
        isTintAuthorityCandidate(
            slot = NativeParticipantRuntimeAccess.slotOf(view),
            visible = view.visibility == View.VISIBLE,
            width = view.width,
            height = view.height,
        )

    internal fun isTintAuthorityCandidate(
        slot: String?,
        visible: Boolean,
        width: Int,
        height: Int,
    ): Boolean =
        slot !in representedTintSlots &&
            visible &&
            width > 0 &&
            height > 0

    private fun readObjectField(
        target: Any,
        name: String,
    ): Any? {
        val field =
            generateSequence(target.javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { candidate ->
                        candidate.name == name
                    }
                }
                .firstOrNull()
                ?: return null
        return runCatching {
            field.isAccessible = true
            field.get(target)
        }.getOrNull()
    }

    private fun readIntField(
        target: Any,
        name: String,
    ): Int? {
        val field =
            generateSequence(target.javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { candidate ->
                        candidate.name == name &&
                            (
                                candidate.type == Int::class.javaPrimitiveType ||
                                    candidate.type == Int::class.java
                            )
                    }
                }
                .firstOrNull()
                ?: return null
        return runCatching {
            field.isAccessible = true
            field.getInt(target)
        }.getOrNull()
    }

    private fun resolveStaticDrawableColor(view: View): Int? {
        val accessor = statusIconStaticColorAccessor ?: return null
        if (!accessor.declaringClass.isInstance(view)) {
            return null
        }
        return runCatching {
            (accessor.invoke(view) as? Number)?.toInt()
        }.getOrNull()
            ?.takeIf { color -> (color ushr 24) != 0 }
    }

    @Synchronized
    internal fun currentStatusIconTint(group: ViewGroup): Int? =
        resolveAppliedStatusIconTint(group)

    private fun resolveAppliedStatusIconTint(group: ViewGroup): Int? {
        for (index in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(index)
            if (!isTintAuthorityCandidate(child)) continue
            findAppliedTint(child)?.let { return it }
        }
        for (index in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(index)
            if (!isTintAuthorityCandidate(child)) continue
            resolveStaticDrawableColor(child)?.let { return it }
        }
        return null
    }

    private fun findAppliedTint(view: View): Int? {
        if (view is ImageView) {
            view.imageTintList
                ?.defaultColor
                ?.takeIf { color -> (color ushr 24) != 0 }
                ?.let { return it }
        }
        val group = view as? ViewGroup ?: return null
        for (index in 0 until group.childCount) {
            findAppliedTint(group.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun refreshBindingsLocked(source: String): BindingSnapshot {
        val group =
            activeGroup?.get()
                ?: return BindingSnapshot.failure(
                    source = source,
                    reason = "home-status-icon-group-missing",
                )

        val targetSlots =
            when {
                wifiSuppressionEnabled && mobileSuppressionEnabled ->
                    wifiAndMobileTargetSlots
                wifiSuppressionEnabled ->
                    wifiOnlyTargetSlots
                mobileSuppressionEnabled ->
                    mobileOnlyTargetSlots
                else ->
                    noTargetSlots
            }
        val targetViews = mutableListOf<Pair<String, View>>()
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            val slot = NativeParticipantRuntimeAccess.slotOf(child) ?: continue
            if (slot in targetSlots) {
                targetViews += slot to child
            }
        }

        val bindings = mutableListOf<Any>()
        val resolvedSlots = mutableListOf<String>()
        targetViews.forEach { (slot, view) ->
            val binding = bindingOf(view)
            if (binding != null) {
                bindings += binding
                resolvedSlots += slot
            }
        }

        if (targetViews.isNotEmpty() && bindings.size != targetViews.size) {
            return BindingSnapshot.failure(
                source = source,
                reason = "binding-resolution-incomplete",
                targetViews = targetViews.size,
                bindings = bindings.size,
                slots = resolvedSlots,
            )
        }

        suppressedBindings =
            bindings
                .distinctBy { binding -> System.identityHashCode(binding) }
                .map(::WeakReference)
                .toTypedArray()

        val visualMaskResult =
            refreshMobileVisualMasksLocked(
                targetViews = targetViews,
                source = source,
            )
        if (visualMaskResult.failureReason != null) {
            return BindingSnapshot.failure(
                source = source,
                reason = visualMaskResult.failureReason,
                targetViews = targetViews.size,
                bindings = bindings.size,
                slots = resolvedSlots,
            )
        }

        group.requestLayout()

        return BindingSnapshot.ready(
            source = source,
            targetViews = targetViews.size,
            bindings = bindings.size,
            slots = resolvedSlots,
            wifiSuppressed = wifiSuppressionEnabled,
            mobileSuppressed = mobileSuppressionEnabled,
            mobileVisualMasks = visualMaskResult.maskCount,
        )
    }

    private fun refreshMobileVisualMasksLocked(
        targetViews: List<Pair<String, View>>,
        source: String,
    ): VisualMaskSnapshot {
        val previous = mobileVisualMasks
        val next = mutableListOf<MobileVisualMaskState>()

        if (mobileSuppressionEnabled) {
            targetViews
                .filter { (slot, _) -> slot == "mobile" }
                .forEach { (_, root) ->
                    val container =
                        findViewByResourceEntry(
                            root = root,
                            entryName = MOBILE_SIGNAL_CONTAINER_RESOURCE_ENTRY,
                        )
                            ?: return VisualMaskSnapshot.failure(
                                source = source,
                                reason = "mobile-signal-container-missing",
                            )

                    val existing =
                        previous.firstOrNull { state ->
                            state.view.get() === container
                        }
                    next +=
                        MobileVisualMaskState(
                            view = WeakReference(container),
                            nativeAlpha = existing?.nativeAlpha ?: container.alpha,
                        )
                }
        }

        val nextViews =
            next
                .mapNotNull { state -> state.view.get() }
                .toSet()
        previous
            .filter { state ->
                val view = state.view.get()
                view != null && view !in nextViews
            }
            .forEach { state ->
                restoreMobileVisualMaskLocked(state)
            }

        mobileVisualMasks =
            next
                .distinctBy { state ->
                    state.view.get()?.let(System::identityHashCode)
                }
                .toTypedArray()

        var masked = 0
        mobileVisualMasks.forEach { state ->
            val view = state.view.get() ?: return@forEach
            val targetAlpha =
                NativeNetworkSuppressionPolicy.mobileVisualMaskAlpha(
                    nativeAlpha = state.nativeAlpha,
                    suppressionActive = true,
                )
            if (view.alpha != targetAlpha) {
                view.alpha = targetAlpha
            }
            if (view.alpha == targetAlpha) {
                masked += 1
            }
        }

        return VisualMaskSnapshot.ready(
            source = source,
            maskCount = masked,
        )
    }

    @Synchronized
    fun preMaskMobileSignal(image: ImageView): Boolean {
        val homeGroup = activeGroup?.get()
        if (
            !NativeNetworkSuppressionPolicy.shouldPreMaskMobileSignal(
                suppressionActive =
                    activeManager != null &&
                        mobileSuppressionEnabled,
                belongsToActiveHomeGroup =
                    homeGroup != null &&
                        isDescendantOf(
                            view = image,
                            ancestor = homeGroup,
                        ),
            )
        ) {
            return false
        }

        val container =
            findAncestorByResourceEntry(
                view = image,
                entryName = MOBILE_SIGNAL_CONTAINER_RESOURCE_ENTRY,
            ) ?: return false

        val existing =
            mobileVisualMasks.firstOrNull { state ->
                state.view.get() === container
            }
        if (existing == null && container.alpha == 0f) {
            return true
        }

        val state =
            existing
                ?: MobileVisualMaskState(
                    view = WeakReference(container),
                    nativeAlpha = container.alpha,
                )
                    .also { created ->
                        mobileVisualMasks =
                            (
                                mobileVisualMasks.asList() +
                                    created
                            )
                                .distinctBy { mask ->
                                    mask.view.get()?.let(System::identityHashCode)
                                }
                                .toTypedArray()
                    }

        val changed = container.alpha != 0f
        if (changed) {
            container.alpha = 0f
        }
        if (changed || existing == null) {
            eventSink?.invoke(
                "nativeNetworkSuppression preMaskMobileSignal " +
                    "view=" + image.javaClass.simpleName +
                    " nativeAlpha=" + state.nativeAlpha +
                    " appliedAlpha=" + container.alpha +
                    " source=mobile-signal-beforeProceed " +
                    "",
            )
        }
        return container.alpha == 0f
    }

    private fun isDescendantOf(
        view: View,
        ancestor: ViewGroup,
    ): Boolean {
        var current: View? = view
        while (current != null) {
            if (current === ancestor) {
                return true
            }
            current = current.parent as? View
        }
        return false
    }

    private fun findAncestorByResourceEntry(
        view: View,
        entryName: String,
    ): View? {
        var current: View? = view
        while (current != null) {
            if (
                current.id != View.NO_ID &&
                runCatching {
                    current.resources.getResourceEntryName(current.id)
                }.getOrNull() == entryName
            ) {
                return current
            }
            current = current.parent as? View
        }
        return null
    }

    private fun restoreMobileVisualMasksLocked(): Int {
        val states = mobileVisualMasks
        mobileVisualMasks = emptyArray()
        var restored = 0
        states.forEach { state ->
            if (restoreMobileVisualMaskLocked(state)) {
                restored += 1
            }
        }
        return restored
    }

    private fun restoreMobileVisualMaskLocked(state: MobileVisualMaskState): Boolean {
        val view = state.view.get() ?: return false
        return runCatching {
            if (view.alpha != state.nativeAlpha) {
                view.alpha = state.nativeAlpha
            }
            view.alpha == state.nativeAlpha
        }.getOrDefault(false)
    }

    private fun findViewByResourceEntry(
        root: View,
        entryName: String,
    ): View? {
        if (
            root.id != View.NO_ID &&
            runCatching { root.resources.getResourceEntryName(root.id) }.getOrNull() == entryName
        ) {
            return root
        }

        val group = root as? ViewGroup ?: return null
        for (index in 0 until group.childCount) {
            findViewByResourceEntry(
                root = group.getChildAt(index),
                entryName = entryName,
            )?.let {
                return it
            }
        }
        return null
    }

    internal fun shouldSuppressStaticSlot(
        slot: String?,
        airplaneSuppressionActive: Boolean,
        noSimSuppressionActive: Boolean,
        belongsToActiveHomeGroup: Boolean,
    ): Boolean {
        if (!belongsToActiveHomeGroup) {
            return false
        }
        return when (slot) {
            AIRPLANE_SLOT -> airplaneSuppressionActive
            NO_SIM_SLOT -> noSimSuppressionActive
            else -> false
        }
    }

    private fun bindingOf(view: View): Any? {
        val getter =
            generateSequence<Class<*>>(view.javaClass) { clazz -> clazz.superclass }
                .flatMap { clazz -> clazz.declaredMethods.asSequence() }
                .firstOrNull { method ->
                    method.parameterCount == 0 &&
                        method.name.startsWith("getBinding\$") &&
                        method.returnType.name == MODERN_BINDING_INTERFACE
                }
                ?: return null
        return runCatching {
            getter.isAccessible = true
            getter.invoke(view)
        }.getOrNull()
    }

    private fun resolveVisibilityMethod(
        classLoader: ClassLoader,
        className: String,
    ): Method {
        val clazz =
            Class.forName(
                className,
                false,
                classLoader,
            )
        return clazz
            .getDeclaredMethod("getShouldIconBeVisible")
            .apply {
                check(returnType == java.lang.Boolean.TYPE) {
                    "visibility-return-type-mismatch:" + className
                }
                isAccessible = true
            }
    }

    private fun clearSessionLocked(
        requestLayout: Boolean,
        restoreVisualMasks: Boolean = true,
    ) {
        val group = activeGroup?.get()
        if (restoreVisualMasks) {
            restoreMobileVisualMasksLocked()
        }
        activeManager = null
        activeGroup = null
        suppressedBindings = emptyArray()
        wifiSuppressionEnabled = false
        mobileSuppressionEnabled = false
        airplaneSuppressionEnabled = false
        noSimSuppressionEnabled = false
        observationOnly = false
        lastStatusPresentation =
            PresentationStore.StatusIconPresentation()
        if (requestLayout) {
            group?.requestLayout()
        }
    }

    internal sealed interface Result {
        val summary: String

        data object Active : Result {
            override val summary = "active"
        }

        data class Pending(
            val reason: String,
        ) : Result {
            override val summary: String
                get() = "pending:" + reason
        }

        data object Inactive : Result {
            override val summary = "inactive"
        }

        data class Failure(
            val reason: String,
        ) : Result {
            override val summary: String
                get() = "failed:" + reason
        }
    }

    private data class BindingSnapshot(
        val source: String,
        val targetViewCount: Int,
        val bindingCount: Int,
        val slots: List<String>,
        val wifiSuppressed: Boolean,
        val mobileSuppressed: Boolean,
        val mobileVisualMaskCount: Int,
        val failureReason: String?,
    ) {
        val logLine: String
            get() =
                "nativeNetworkSuppression " +
                    if (failureReason == null) {
                        "active"
                    } else {
                        "unavailable"
                    } +
                    " source=" + source +
                    " targetViews=" + targetViewCount +
                    " bindings=" + bindingCount +
                    " slots=" + slots.joinToString(",") +
                    " wifiSuppressed=" + wifiSuppressed +
                    " mobileSuppressed=" + mobileSuppressed +
                    " mobileVisualMasks=" + mobileVisualMaskCount +
                    " visualMask=mobile_signal_container.alpha " +
                    " reason=" + (failureReason ?: "none")

        companion object {
            fun ready(
                source: String,
                targetViews: Int,
                bindings: Int,
                slots: List<String>,
                wifiSuppressed: Boolean,
                mobileSuppressed: Boolean,
                mobileVisualMasks: Int,
            ): BindingSnapshot =
                BindingSnapshot(
                    source = source,
                    targetViewCount = targetViews,
                    bindingCount = bindings,
                    slots = slots.distinct(),
                    wifiSuppressed = wifiSuppressed,
                    mobileSuppressed = mobileSuppressed,
                    mobileVisualMaskCount = mobileVisualMasks,
                    failureReason = null,
                )

            fun failure(
                source: String,
                reason: String,
                targetViews: Int = 0,
                bindings: Int = 0,
                slots: List<String> = emptyList(),
            ): BindingSnapshot =
                BindingSnapshot(
                    source = source,
                    targetViewCount = targetViews,
                    bindingCount = bindings,
                    slots = slots.distinct(),
                    wifiSuppressed = false,
                    mobileSuppressed = false,
                    mobileVisualMaskCount = 0,
                    failureReason = reason,
                )
        }
    }

    private data class MobileVisualMaskState(
        val view: WeakReference<View>,
        val nativeAlpha: Float,
    )

    private data class VisualMaskSnapshot(
        val source: String,
        val maskCount: Int,
        val failureReason: String?,
    ) {
        companion object {
            fun ready(
                source: String,
                maskCount: Int,
            ): VisualMaskSnapshot =
                VisualMaskSnapshot(
                    source = source,
                    maskCount = maskCount,
                    failureReason = null,
                )

            fun failure(
                source: String,
                reason: String,
            ): VisualMaskSnapshot =
                VisualMaskSnapshot(
                    source = source,
                    maskCount = 0,
                    failureReason = reason,
                )
        }
    }

    private const val MOBILE_SIGNAL_CONTAINER_RESOURCE_ENTRY = "mobile_signal_container"
    private const val AIRPLANE_SLOT = "airplane"
    private const val NO_SIM_SLOT = "no_sim"
    private const val EXPECTED_HOOK_COUNT = 5
}
