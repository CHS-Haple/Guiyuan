package com.chaners.guiyuan.xposed.network

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.graphics.drawable.Icon
import com.chaners.guiyuan.xposed.NativeParticipantAccess
import com.chaners.guiyuan.xposed.PresentationStore
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap

internal object NativeStatusObserver {
    internal fun rethrowFatal(error: Throwable) {
        val cause = (error as? InvocationTargetException)?.targetException ?: error
        if (cause is VirtualMachineError || cause is ThreadDeath) throw cause
    }

    private inline fun <T> readOrNull(block: () -> T): T? =
        try {
            block()
        } catch (error: Throwable) {
            rethrowFatal(error)
            null
        }

    private const val HOME_MANAGER_CLASS =
        "com.android.systemui.statusbar.phone.ui.DarkIconManager"
    private const val STATUS_BAR_ICON_CONTROLLER_IMPL_CLASS =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl"
    private const val STATUS_BAR_ICON_VIEW_CLASS =
        "com.android.systemui.statusbar.StatusBarIconView"
    private const val DARK_ICON_DISPATCHER_CLASS =
        "com.android.systemui.plugins.DarkIconDispatcher"

    private const val HOME_ICON_ADDED_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.homeIconAdded"
    private const val HOME_MANAGER_REGISTERED_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.homeManagerRegistered"
    // Retain the existing Hook ID even though this is now observation-only.
    private const val STATUS_ICON_VISIBILITY_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.airplaneVisibility"

    private val observableTargetSlots = setOf("wifi", "mobile", NO_SIM_SLOT)
    private val representedTintSlots =
        setOf("combined_status", "wifi", "mobile", "stacked_mobile", NO_SIM_SLOT, AIRPLANE_SLOT)

    private val installedHandles = mutableListOf<HookHandle>()
    private var installIncomplete = false
    private val failedObservations = mutableSetOf<String>()
    private var activeManager: Any? = null
    private var activeGroup: WeakReference<ViewGroup>? = null
    private var pendingObservationHost: WeakReference<View>? = null
    private var pendingObservationSource: String? = null
    private var observationAttachedSink: ((String) -> Unit)? = null
    private var eventSink: ((String) -> Unit)? = null
    private var diagnosticsEnabled: () -> Boolean = { false }
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

    // Cache reflective lookups, not the live SystemUI tint values.
    private val tintFieldNames =
        setOf("mDarkIconDispatcher", "mTintAreas", "mIconTint", "mColor")
    private val tintFieldsByClass =
        ConcurrentHashMap<Class<*>, List<Field>>()
    private val tintMethodByManagerClass =
        ConcurrentHashMap<Class<*>, Method>()

    val installedHookCount: Int
        @Synchronized get() = installedHandles.size

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        isDetailedDiagnosticsEnabled: () -> Boolean = { false },
        onObservationAttached: ((String) -> Unit)? = null,
        onStatusPresentationChanged:
            ((PresentationStore.StatusIconPresentation) -> Unit)? = null,
    ): String? {
        if (installIncomplete) return "partial-hook-state"
        if (installedHandles.isNotEmpty()) {
            eventSink = onEvent
            diagnosticsEnabled = isDetailedDiagnosticsEnabled
            observationAttachedSink = onObservationAttached
            statusPresentationSink = onStatusPresentationChanged
            return null
        }

        val created = mutableListOf<HookHandle>()
        return runCatching {
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
                            method.parameterTypes[0] == Integer.TYPE &&
                            method.parameterTypes[1] == String::class.java &&
                            method.parameterTypes[2] == java.lang.Boolean.TYPE
                    }
                    ?: error("home-icon-manager-onIconAdded-missing")
            onIconAdded.isAccessible = true

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
                    .setId(STATUS_ICON_VISIBILITY_HOOK_ID)
                    .intercept(statusIconVisibilityHooker())

            installedHandles.clear()
            installedHandles.addAll(created)
            installIncomplete = false
            eventSink = onEvent
            diagnosticsEnabled = isDetailedDiagnosticsEnabled
            observationAttachedSink = onObservationAttached
            statusPresentationSink = onStatusPresentationChanged
            null
        }.getOrElse { error ->
            val remaining = created.filter { handle ->
                try {
                    handle.unhook()
                    false
                } catch (failure: Throwable) {
                    if (failure is VirtualMachineError || failure is ThreadDeath) throw failure
                    true
                }
            }
            installedHandles.clear()
            installedHandles.addAll(remaining)
            installIncomplete = remaining.isNotEmpty()
            airplaneSlotAccessor = null
            statusIconVisibleAccessor = null
            statusIconSourceAccessor = null
            statusIconStaticColorAccessor = null
            activeManager = null
            activeGroup = null
            pendingObservationHost = null
            pendingObservationSource = null
            eventSink = onEvent
            diagnosticsEnabled = isDetailedDiagnosticsEnabled
            observationAttachedSink = onObservationAttached
            statusPresentationSink = onStatusPresentationChanged
            if (error is VirtualMachineError || error is ThreadDeath) throw error
            val reason = error.message ?: error.javaClass.simpleName
            if (installIncomplete) "$reason-hook-cleanup-failed" else reason
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
            NativeParticipantAccess.managerFor(hostView)
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
            NativeParticipantAccess.groupFor(hostView)
                ?: return Result.Failure("status-icon-group-missing")
        if (manager.javaClass.name != HOME_MANAGER_CLASS) {
            return Result.Failure("home-manager-mismatch")
        }

        if (activeManager !== manager || activeGroup?.get() !== group) {
            clearSessionLocked()
        }

        activeManager = manager
        activeGroup = WeakReference(group)
        refreshStatusPresentationLocked("observerAttach")

        eventSink?.takeIf { diagnosticsEnabled() }?.invoke(
            "nativeNetworkSuppression observerOnly source=" + source + " " +
                "manager=" + manager.javaClass.name +
                " group=" + group.javaClass.name,
        )
        return Result.Active
    }

    @Synchronized
    fun refreshObservation(source: String) {
        if (activeGroup?.get() != null) {
            refreshStatusPresentationLocked(source)
        }
    }

    @Synchronized
    fun deactivate(): Result {
        pendingObservationHost = null
        pendingObservationSource = null
        clearSessionLocked()
        return Result.Inactive
    }

    @Synchronized
    fun resetRuntimeState() {
        deactivate()
        installedHandles.clear()
        installIncomplete = false
        eventSink = null
        diagnosticsEnabled = { false }
        observationAttachedSink = null
        statusPresentationSink = null
        airplaneSlotAccessor = null
        statusIconVisibleAccessor = null
        statusIconSourceAccessor = null
        statusIconStaticColorAccessor = null
        transitionStateAccessorCache.clear()
        transitionStateAccessorMissing.clear()
        transitionStateFieldCache.clear()
        tintFieldsByClass.clear()
        tintMethodByManagerClass.clear()
        lastStatusPresentation =
            PresentationStore.StatusIconPresentation()
    }

    // The native method has already run. Observation cannot replace its return
    // or make a diagnostic/renderer callback fail the SystemUI caller.
    internal inline fun <T> observeAfterNative(
        result: T,
        onFailure: (Throwable) -> Unit,
        observe: () -> Unit,
    ): T {
        try {
            observe()
        } catch (error: Throwable) {
            if (error is VirtualMachineError || error is ThreadDeath) throw error
            try {
                onFailure(error)
            } catch (failure: Throwable) {
                if (failure is VirtualMachineError || failure is ThreadDeath) throw failure
            }
        }
        return result
    }

    private fun reportObservationFailure(source: String, error: Throwable) {
        if (!synchronized(this) { failedObservations.add(source) }) return
        Log.w("Guiyuan[NativeObserver]", "Home observation failed: $source", error)
    }

    private fun statusIconVisibilityHooker(): Hooker =
        Hooker { chain ->
            val result = chain.proceed()
            observeAfterNative(result, { reportObservationFailure("noSimVisibility", it) }) {
                val view = chain.thisObject as? View
                val slot =
                    readOrNull {
                        airplaneSlotAccessor?.invoke(chain.thisObject) as? String
                    }
                val homeGroup = activeGroup?.get()
                if (
                    slot == NO_SIM_SLOT &&
                    result is Boolean &&
                    view != null &&
                    homeGroup != null &&
                    isDescendantOf(view, homeGroup)
                ) {
                    synchronized(this) {
                        if (homeGroup === activeGroup?.get()) {
                            refreshStatusPresentationLocked(
                                source = "visibility:no_sim",
                                observedNoSimView = view,
                                observedNoSimVisible = result,
                            )
                        }
                    }
                }
            }
        }

    private fun homeManagerRegisteredHooker(): Hooker =
        Hooker { chain ->
            val result = chain.proceed()
            observeAfterNative(result, { reportObservationFailure("managerRegistered", it) }) {
                val manager = chain.getArg(0)
                if (manager?.javaClass?.name == HOME_MANAGER_CLASS) {
                    synchronized(this) {
                        val pendingHost = pendingObservationHost?.get()
                        val pendingSource = pendingObservationSource
                        if (
                            pendingHost != null &&
                            NativeParticipantAccess.managerFor(pendingHost) === manager
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
                                    eventSink?.takeIf { diagnosticsEnabled() }?.invoke(
                                        "statusIconObservation ready source=" +
                                            (pendingSource ?: "homeManagerRegistered") +
                                            " trigger=homeManagerRegistered",
                                    )
                                }
                                is Result.Failure ->
                                    eventSink?.takeIf { diagnosticsEnabled() }?.invoke(
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
            }
        }

    private fun homeIconAddedHooker(): Hooker =
        Hooker { chain ->
            val result = chain.proceed()
            observeAfterNative(result, { reportObservationFailure("iconAdded", it) }) {
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
                        }
                    }
                }
            }
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
                        readOrNull {
                            Class.forName(
                                groupClass.name + "\$Companion",
                                false,
                                groupClass.classLoader,
                            )
                        }
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
        return readOrNull {
            accessor.invoke(null, view)
        }
    }

    private fun transitionStateFields(stateClass: Class<*>): TransitionStateFields =
        transitionStateFieldCache[stateClass]
            ?: TransitionStateFields(
                visibleState = resolveCachedField(stateClass, "visibleState"),
                inIslandState = resolveCachedField(stateClass, "inIslandState"),
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
            readOrNull { resolved.get(target) }
        }

    internal data class IslandVisibilityState(
        val visibleState: Int?,
        val inIslandState: Int?,
    )

    private data class TransitionStateFields(
        val visibleState: Field?,
        val inIslandState: Field?,
    )

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
        return StatusIconTintPolicy.statusIconTint(
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
                        NativeParticipantAccess.slotOf(child) == NO_SIM_SLOT
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
            StatusIconTintPolicy.statusIconTint(
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
        if (presentation != lastStatusPresentation) {
            lastStatusPresentation = presentation
            statusPresentationSink?.invoke(presentation)
            eventSink?.takeIf { diagnosticsEnabled() }?.invoke(
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
                    (tintAnchor?.let(NativeParticipantAccess::slotOf) ?: "none") +
                    " tintAnchorClass=" +
                    (tintAnchor?.javaClass?.simpleName ?: "none") +
                    " noSimVisible=" + presentation.noSimVisible +
                    " noSimResource=" +
                    (
                        presentation.noSimIcon?.packageName +
                            ":" +
                            presentation.noSimIcon?.resourceId
                    ),
            )
        }

    }

    private fun isNativeStatusIconVisible(view: View): Boolean {
        if (view.visibility != View.VISIBLE) {
            return false
        }
        val accessor = statusIconVisibleAccessor ?: return true
        return readOrNull {
            accessor.invoke(view) as? Boolean
        } ?: true
    }

    private fun resolveNativeIconResource(
        view: View,
    ): PresentationStore.NativeIconResource? {
        val accessor = statusIconSourceAccessor ?: return null
        val icon =
            readOrNull {
                accessor.invoke(view) as? Icon
            } ?: return null
        if (icon.type != Icon.TYPE_RESOURCE) {
            return null
        }
        val packageName =
            icon.resPackage
                .takeIf(String::isNotBlank)
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
                ?.takeIf(StatusIconTintPolicy::isVisibleTint)
                ?: return null
        val tintAreas =
            readObjectField(dispatcher, "mTintAreas")
                as? Collection<*>
                ?: return null

        return readOrNull {
            val managerClass = manager.javaClass
            val getTint =
                tintMethodByManagerClass[managerClass]
                    ?: Class.forName(
                        DARK_ICON_DISPATCHER_CLASS,
                        false,
                        managerClass.classLoader,
                    ).methods.firstOrNull { method ->
                        method.name == "getTint" &&
                            method.parameterCount == 3 &&
                            View::class.java.isAssignableFrom(
                                method.parameterTypes[1],
                            ) &&
                            method.parameterTypes[2] ==
                                Int::class.javaPrimitiveType
                    }?.also { tintMethodByManagerClass[managerClass] = it }
                    ?: return@readOrNull null
            (getTint.invoke(null, tintAreas, anchorView, iconTint) as? Number)
                ?.toInt()
                ?.takeIf(StatusIconTintPolicy::isVisibleTint)
        }
    }

    private fun resolveManagerFallbackTint(manager: Any?): Int? {
        manager ?: return null

        readIntField(manager, "mColor")
            ?.takeIf(StatusIconTintPolicy::isVisibleTint)
            ?.let { return it }

        val dispatcher =
            readObjectField(manager, "mDarkIconDispatcher")
                ?: return null
        return readIntField(dispatcher, "mIconTint")
            ?.takeIf(StatusIconTintPolicy::isVisibleTint)
    }

    private fun resolveTintAnchorView(group: ViewGroup): View? {
        for (index in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(index)
            if (
                isTintAuthorityCandidate(
                    slot = NativeParticipantAccess.slotOf(child),
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
            slot = NativeParticipantAccess.slotOf(view),
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

    private fun tintFields(clazz: Class<*>): List<Field> =
        tintFieldsByClass.computeIfAbsent(clazz) {
            generateSequence(clazz) { it.superclass }
                .flatMap { it.declaredFields.asSequence() }
                .filter { it.name in tintFieldNames }
                .toList()
        }

    private fun readObjectField(
        target: Any,
        name: String,
    ): Any? {
        val field =
            tintFields(target.javaClass)
                .firstOrNull { it.name == name }
                ?: return null
        return readOrNull {
            field.isAccessible = true
            field.get(target)
        }
    }

    private fun readIntField(
        target: Any,
        name: String,
    ): Int? {
        val field =
            tintFields(target.javaClass)
                .firstOrNull {
                    it.name == name &&
                        (it.type == Int::class.javaPrimitiveType ||
                            it.type == Int::class.javaObjectType)
                }
                ?: return null
        return readOrNull {
            field.isAccessible = true
            (field.get(target) as? Number)?.toInt()
        }
    }

    private fun resolveStaticDrawableColor(view: View): Int? {
        val accessor = statusIconStaticColorAccessor ?: return null
        if (!accessor.declaringClass.isInstance(view)) {
            return null
        }
        return readOrNull {
            (accessor.invoke(view) as? Number)?.toInt()
        }?.takeIf { color -> (color ushr 24) != 0 }
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

    private fun clearSessionLocked() {
        failedObservations.clear()
        activeManager = null
        activeGroup = null
        lastStatusPresentation =
            PresentationStore.StatusIconPresentation()
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

    private const val AIRPLANE_SLOT = "airplane"
    private const val NO_SIM_SLOT = "no_sim"
}
