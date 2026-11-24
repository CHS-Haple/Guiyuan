package com.chaners.guiyuan.xposed.network

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.graphics.drawable.Icon
import com.chaners.guiyuan.xposed.SysUiFatal.rethrow as rethrowFatal
import com.chaners.guiyuan.xposed.NativeParticipantAccess
import com.chaners.guiyuan.xposed.NativeStatusSlots
import com.chaners.guiyuan.xposed.PresentationStore
import com.chaners.guiyuan.xposed.unhookFailed
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import com.chaners.guiyuan.xposed.tintHexDigits
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method

// Native slot identities are shared by the observer and its tint resolver.
internal const val AIRPLANE_SLOT = "airplane"
internal const val NO_SIM_SLOT = "no_sim"

internal object NativeStatusObserver {

    private inline fun reportEvent(message: () -> String) {
        SysUiPresentationRuntime.emitPassiveDiagnostic {
            eventSink?.takeIf { diagnosticsEnabled() }?.invoke(message())
        }
    }

    private fun tintText(color: Int?): String =
        color?.let(::tintHexDigits) ?: "none"

    private const val HOME_MANAGER_CLASS =
        "com.android.systemui.statusbar.phone.ui.DarkIconManager"
    private const val STATUS_BAR_ICON_CONTROLLER_IMPL_CLASS =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl"
    private const val STATUS_BAR_ICON_VIEW_CLASS =
        "com.android.systemui.statusbar.StatusBarIconView"
    private const val HOME_MANAGER_REGISTERED_SOURCE = "homeManagerRegistered"

    private const val HOME_ICON_ADDED_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.homeIconAdded"
    private const val HOME_MANAGER_REGISTERED_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.homeManagerRegistered"
    // Retain the existing Hook ID even though this is now observation-only.
    private const val STATUS_ICON_VISIBILITY_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.airplaneVisibility"

    private val observableTargetSlots = setOf("wifi", "mobile", NO_SIM_SLOT)
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
    // Receipt identity prevents older failures from erasing newer equal-valued delivery.
    private class Delivery(val presentation: PresentationStore.StatusIconPresentation)
    private var lastDelivery = Delivery(PresentationStore.StatusIconPresentation())

    val installedHookCount: Int
        @Synchronized get() = installedHandles.size

    private fun bindSinks(
        onEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onObservationAttached: ((String) -> Unit)?,
        onStatusPresentationChanged: ((PresentationStore.StatusIconPresentation) -> Unit)?,
    ) {
        eventSink = onEvent
        diagnosticsEnabled = isDetailedDiagnosticsEnabled
        observationAttachedSink = onObservationAttached
        statusPresentationSink = onStatusPresentationChanged
    }

    private fun resolveStatusIconVisibility(classLoader: ClassLoader): Method {
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
        return airplaneVisibility
    }

    private fun resolveHomeHookMethods(classLoader: ClassLoader): Pair<Method, Method> {
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
        return onIconAdded to addIconGroup
    }

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
            bindSinks(onEvent, isDetailedDiagnosticsEnabled, onObservationAttached, onStatusPresentationChanged)
            return null
        }

        val created = mutableListOf<HookHandle>()
        return runCatching {
            val airplaneVisibility = resolveStatusIconVisibility(classLoader)
            val (onIconAdded, addIconGroup) = resolveHomeHookMethods(classLoader)

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
            bindSinks(onEvent, isDetailedDiagnosticsEnabled, onObservationAttached, onStatusPresentationChanged)
            null
        }.getOrElse { error ->
            rollbackInstall(created)
            bindSinks(onEvent, isDetailedDiagnosticsEnabled, onObservationAttached, onStatusPresentationChanged)
            rethrowFatal(error)
            val reason = error.message ?: error.javaClass.simpleName
            if (installIncomplete) "$reason-hook-cleanup-failed" else reason
        }
    }

    // Preserve any hook whose unhook failed; a partial install must not be retried.
    @Suppress("TooGenericExceptionCaught")
    private fun rollbackInstall(created: List<HookHandle>) {
        val remaining = created.filter { it.unhookFailed() }
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

        reportEvent {
            "nativeNetworkSuppression observerOnly source=" + source + " " +
                "manager=" + manager.javaClass.name +
                " group=" + group.javaClass.name
        }
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
        NativeIslandEvidence.resetCaches()
        NativeStatusTintResolver.resetCaches()
        lastDelivery = Delivery(PresentationStore.StatusIconPresentation())
    }

    // The native method has already run. Observation cannot replace its return
    // or make a diagnostic/renderer callback fail the SystemUI caller.
    @Suppress("TooGenericExceptionCaught")
    internal inline fun <T> observeAfterNative(
        result: T,
        onFailure: (Throwable) -> Unit,
        observe: () -> Unit,
    ): T {
        try {
            observe()
        } catch (error: Throwable) {
            rethrowFatal(error)
            try {
                onFailure(error)
            } catch (failure: Throwable) {
                rethrowFatal(failure)
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
                    optionalNativeRead {
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
                                        source = pendingSource ?: HOME_MANAGER_REGISTERED_SOURCE,
                                    )
                            ) {
                                is Result.Active -> {
                                    observationAttachedSink?.invoke(
                                        pendingSource ?: HOME_MANAGER_REGISTERED_SOURCE,
                                    )
                                    reportEvent {
                                        "statusIconObservation ready source=" +
                                            (pendingSource ?: HOME_MANAGER_REGISTERED_SOURCE) +
                                            " trigger=homeManagerRegistered"
                                    }
                                }
                                is Result.Failure ->
                                    reportEvent {
                                        "statusIconObservation unavailable " +
                                            "source=" + (pendingSource ?: HOME_MANAGER_REGISTERED_SOURCE) +
                                            " reason=" + state.reason
                                    }
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

    @Synchronized
    fun currentAppliedStatusIconTint(anchorView: View? = null): Int? {
        val group = activeGroup?.get()
        val peerTint = group?.let(::resolveAppliedStatusIconTint)
        val resolvedAnchor =
            anchorView
                ?.takeIf(NativeStatusTintResolver::isTintAuthorityCandidate)
                ?: group?.let(NativeStatusTintResolver::resolveTintAnchorView)
        val locationAwareTint =
            NativeStatusTintResolver.resolveLocationAwareManagerTint(
                manager = activeManager,
                anchorView = resolvedAnchor,
            )
        val managerFallbackTint =
            NativeStatusTintResolver.resolveManagerFallbackTint(activeManager)
        return StatusIconTintPolicy.statusIconTint(
            locationAwareTint = locationAwareTint,
            peerAppliedTint = peerTint,
            managerFallbackTint = managerFallbackTint,
            fallbackTint = lastDelivery.presentation.appliedTint,
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
                ?: (0 until group.childCount).firstNotNullOfOrNull { index ->
                    group.getChildAt(index).takeIf { child ->
                        NativeStatusSlots.slotOf(child) == NO_SIM_SLOT
                    }
                }
        val noSimVisible =
            observedNoSimVisible
                ?: (noSimView?.let(::isNativeStatusIconVisible) == true)
        val noSimIcon =
            noSimView?.takeIf { noSimVisible }?.let(::resolveNativeIconResource)
        val peerTint = resolveAppliedStatusIconTint(group)
        val tintAnchor = NativeStatusTintResolver.resolveTintAnchorView(group)
        val locationAwareTint =
            NativeStatusTintResolver.resolveLocationAwareManagerTint(
                manager = activeManager,
                anchorView = tintAnchor,
            )
        val managerFallbackTint =
            NativeStatusTintResolver.resolveManagerFallbackTint(activeManager)
        val appliedTint =
            StatusIconTintPolicy.statusIconTint(
                locationAwareTint = locationAwareTint,
                peerAppliedTint = peerTint,
                managerFallbackTint = managerFallbackTint,
                fallbackTint = lastDelivery.presentation.appliedTint,
            )
        val presentation =
            PresentationStore.StatusIconPresentation(
                appliedTint = appliedTint,
                noSimVisible = noSimVisible,
                noSimIcon = noSimIcon,
            )
        val sink = statusPresentationSink
        if (sink != null && deliverPresentation(presentation, sink)) {
            reportEvent {
                val authority = when {
                    locationAwareTint != null -> "dispatcher-location-aware"
                    peerTint != null -> "peer-static-applied"
                    managerFallbackTint != null -> "manager-global-fallback"
                    else -> "cached-fallback"
                }
                "statusIconPresentation source=$source tint=${tintText(presentation.appliedTint)}" +
                    " tintAuthority=$authority locationAwareTint=${tintText(locationAwareTint)}" +
                    " peerTint=${tintText(peerTint)} managerFallbackTint=${tintText(managerFallbackTint)}" +
                    " tintAnchorSlot=${tintAnchor?.let(NativeStatusSlots::slotOf) ?: "none"}" +
                    " tintAnchorClass=${tintAnchor?.javaClass?.simpleName ?: "none"}" +
                    " noSimVisible=${presentation.noSimVisible}" +
                    " noSimResource=${presentation.noSimIcon?.packageName}:${presentation.noSimIcon?.resourceId}"
            }
        }

    }

    @Synchronized
    @Suppress("TooGenericExceptionCaught")
    internal fun deliverPresentation(
        presentation: PresentationStore.StatusIconPresentation,
        deliver: (PresentationStore.StatusIconPresentation) -> Unit,
    ): Boolean {
        if (presentation == lastDelivery.presentation) return false

        val previous = lastDelivery
        val pending = Delivery(presentation)
        lastDelivery = pending
        try {
            deliver(presentation)
        } catch (error: Throwable) {
            if (lastDelivery === pending) lastDelivery = previous
            throw error
        }
        return true
    }

    private fun isNativeStatusIconVisible(view: View): Boolean {
        if (view.visibility != View.VISIBLE) {
            return false
        }
        val accessor = statusIconVisibleAccessor ?: return true
        return optionalNativeRead {
            accessor.invoke(view) as? Boolean
        } ?: true
    }

    private fun resolveNativeIconResource(
        view: View,
    ): PresentationStore.NativeIconResource? {
        val accessor = statusIconSourceAccessor ?: return null
        val icon =
            optionalNativeRead {
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

    private fun resolveStaticDrawableColor(view: View): Int? {
        val accessor = statusIconStaticColorAccessor ?: return null
        if (!accessor.declaringClass.isInstance(view)) {
            return null
        }
        return optionalNativeRead {
            (accessor.invoke(view) as? Number)?.toInt()
        }?.takeIf { color -> (color ushr 24) != 0 }
    }

    @Synchronized
    internal fun currentStatusIconTint(group: ViewGroup): Int? =
        resolveAppliedStatusIconTint(group)

    private fun resolveAppliedStatusIconTint(group: ViewGroup): Int? {
        for (index in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(index)
            if (!NativeStatusTintResolver.isTintAuthorityCandidate(child)) continue
            findAppliedTint(child)?.let { return it }
        }
        for (index in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(index)
            if (!NativeStatusTintResolver.isTintAuthorityCandidate(child)) continue
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
        lastDelivery = Delivery(PresentationStore.StatusIconPresentation())
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

}
