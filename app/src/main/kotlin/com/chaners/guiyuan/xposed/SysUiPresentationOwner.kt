package com.chaners.guiyuan.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.chaners.guiyuan.xposed.prefs.VisualPrefsOwner
import com.chaners.guiyuan.xposed.network.NativeStatusObserver
import com.chaners.guiyuan.xposed.network.SysUiCarrierMetrics
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException

// Owns mutable native presentation; SysUiPresentationRuntime only wires source hooks.
internal object SysUiPresentationOwner {
    private const val HOME_HOST =
        "com.android.systemui.statusbar.views.MiuiNotificationStatusContainer"
    private const val STATUS_ICON_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val LEGACY_SLOT = "combined_status"
    private const val MEASURE_HOOK_ID =
        "combinedstatus.homePresentation.statusIconsMeasure"
    private const val LAYOUT_HOOK_ID =
        "combinedstatus.homePresentation.statusIconsLayout"
    private const val BATTERY_HIDE_HOOK_ID =
        "combinedstatus.homePresentation.batteryHideState"
    private const val ISLAND_SHOWING_HOOK_ID =
        "combinedstatus.homePresentation.fakeIslandShowing"
    private const val CONTROL_CENTER_FAKE_SURFACE = "control-center-fake"
    private const val HOME_SURFACE = "home"
    private const val HOOK_COUNT = 4

    private val representedSlots =
        linkedSetOf("wifi", "mobile", "stacked_mobile", "airplane", "no_sim")


    private var measureHook: HookHandle? = null
    private var layoutHook: HookHandle? = null
    private var batteryHideHook: HookHandle? = null
    private var islandShowingHook: HookHandle? = null
    private var ignoredSlotsField: Field? = null
    private var addIgnoredSlotsMethod: java.lang.reflect.Method? = null
    private var setIgnoredSlotsMethod: java.lang.reflect.Method? = null
    private var batteryHideField: Field? = null
    private var current: Session? = null
    private var keyguardFamilyCurrent: Session? = null
    private var keyguardFamilySurface: KeyguardFamilySurface? = null
    private var controlCenterCurrent: Session? = null
    private var controlCenterEventSink: ((String) -> Unit)? = null
    private var controlCenterFailNativeSink: ((String) -> Unit)? = null
    private var controlCenterReadySink: ((Result.Active) -> Unit)? = null
    private var eventSink: ((String) -> Unit)? = null
    private var failNativeSink: ((String) -> Unit)? = null
    private var keyguardFamilyEventSink: ((String) -> Unit)? = null
    private var keyguardFamilyFailNativeSink: ((String) -> Unit)? = null
    private var keyguardFamilyReadySink: ((Result.Active) -> Unit)? = null
    private var controlCenterSourceScene = SourceScene.UNKNOWN
    private var steadyPeerMirrorActive = false
    private var steadyPeerMirrorHiddenSlots: Set<String> = emptySet()

    // Diagnostic sinks must not interrupt native layout or its readiness callbacks.
    internal inline fun emitDiagnostic(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            rethrowFatal(error)
        }
    }

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

    private fun guardedDiagnostic(
        sink: ((String) -> Unit)?,
    ): ((String) -> Unit)? =
        sink?.let { callback ->
            { event: String -> emitDiagnostic { callback(event) } }
        }

    val installedHookCount: Int
        @Synchronized get() =
            listOfNotNull(
                measureHook,
                layoutHook,
                batteryHideHook,
                islandShowingHook,
            ).size

    @Synchronized
    internal fun homeSlots(): Set<String> =
        current?.ownedRepresentedSlots() ?: emptySet()

    @Synchronized
    internal fun homeCarrierVisible(): Boolean =
        current?.isNativeCarrierPresentationVisible() == true

    @Synchronized
    internal fun keyguardSlots(): Set<String> =
        if (keyguardFamilySurface == KeyguardFamilySurface.KEYGUARD) {
            keyguardFamilyCurrent?.ownedRepresentedSlots() ?: emptySet()
        } else {
            emptySet()
        }

    @Synchronized
    internal fun aodSlots(): Set<String> =
        if (keyguardFamilySurface == KeyguardFamilySurface.AOD) {
            keyguardFamilyCurrent?.ownedRepresentedSlots() ?: emptySet()
        } else {
            emptySet()
        }

    @Synchronized
    internal fun keyguardClaimed(): Boolean =
        keyguardFamilySurface == KeyguardFamilySurface.KEYGUARD &&
            keyguardFamilyCurrent?.hasPresentationClaim() == true

    @Synchronized
    internal fun aodClaimed(): Boolean =
        keyguardFamilySurface == KeyguardFamilySurface.AOD &&
            keyguardFamilyCurrent?.hasPresentationClaim() == true

    @Synchronized
    internal fun aodClaimed(host: SysUiKeyguardHostResolver.ResolvedHost): Boolean =
        keyguardFamilySurface == KeyguardFamilySurface.AOD &&
            keyguardFamilyCurrent?.let { session ->
                session.hasPresentationClaim() &&
                    session.matches(
                        host = host.host,
                        statusIcons = host.statusIcons,
                        batteryContainer = host.systemIcons,
                        battery = host.battery,
                        batteryCarrier = host.batteryCarrier,
                    )
            } == true

    @Synchronized
    fun onAodIconTransition(
        host: SysUiKeyguardHostResolver.ResolvedHost,
        toLockScreen: Boolean,
    ) {
        if (keyguardFamilySurface != KeyguardFamilySurface.AOD) return
        val session = keyguardFamilyCurrent ?: return
        if (
            !session.hasPresentationClaim() ||
            !session.matches(
                host = host.host,
                statusIcons = host.statusIcons,
                batteryContainer = host.systemIcons,
                battery = host.battery,
                batteryCarrier = host.batteryCarrier,
            )
        ) return
        session.onAodIconTransition(toLockScreen)
    }

    @Synchronized
    fun releaseAodBatteryMask(host: SysUiKeyguardHostResolver.ResolvedHost) {
        if (!aodClaimed(host)) return
        keyguardFamilyCurrent?.releaseAodBatteryMask()
    }

    @Synchronized
    fun updateCcSourceScene(sourceScene: SourceScene) {
        if (controlCenterSourceScene == sourceScene) return
        controlCenterSourceScene = sourceScene

        // The steady-peer mirror samples Home native island state, so it is valid
        // only while Home is the authoritative Control Center source.
        if (sourceScene != SourceScene.HOME) {
            steadyPeerMirrorActive = false
            steadyPeerMirrorHiddenSlots = emptySet()
            controlCenterCurrent?.updateSteadyPeerMirror(
                active = false,
                hiddenSlots = emptySet(),
            )
            return
        }

        val snapshot =
            current?.captureSteadyPeerMirror()
                ?: SteadyPeerMirrorSnapshot(false, emptySet())
        steadyPeerMirrorActive = snapshot.active
        steadyPeerMirrorHiddenSlots = snapshot.hiddenSlots
        controlCenterCurrent?.updateSteadyPeerMirror(
            active = snapshot.active,
            hiddenSlots = snapshot.hiddenSlots,
        )
    }

    @Synchronized
    fun onVisualCfgChanged() {
        current?.syncEndReservation()
        keyguardFamilyCurrent?.syncEndReservation()
        controlCenterCurrent?.syncEndReservation()
    }

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        onFailNative: ((String) -> Unit)? = null,
    ): String? {
        if (installedHookCount == HOOK_COUNT && ignoredSlotsField != null) {
            eventSink = guardedDiagnostic(onEvent)
            failNativeSink = onFailNative
            return null
        }
        if (installedHookCount != 0) {
            return "partial-hook-state"
        }

        val containerClass =
            readOrNull {
                Class.forName(STATUS_ICON_CONTAINER, false, classLoader)
            } ?: return "status-icon-container-class-missing"
        val field =
            readOrNull {
                generateSequence(containerClass) { clazz -> clazz.superclass }
                    .mapNotNull { clazz ->
                        clazz.declaredFields.firstOrNull { candidate ->
                            candidate.name == "ignoredSlots" &&
                                List::class.java.isAssignableFrom(candidate.type)
                        }
                    }
                    .firstOrNull()
                    ?.apply { isAccessible = true }
            } ?: return "ignored-slots-field-missing"
        val addIgnoredSlots =
            readOrNull {
                containerClass.declaredMethods
                    .filter { method ->
                        method.name == "addIgnoredSlots" &&
                            method.parameterTypes.contentEquals(
                                arrayOf(List::class.java),
                            ) &&
                            method.returnType == Void.TYPE
                    }
                    .singleOrNull()
                    ?.apply { isAccessible = true }
            } ?: return "add-ignored-slots-contract-missing"
        val setIgnoredSlots =
            readOrNull {
                containerClass.declaredMethods
                    .filter { method ->
                        method.name == "setIgnoredSlots" &&
                            method.parameterTypes.contentEquals(
                                arrayOf(List::class.java),
                            ) &&
                            method.returnType == Void.TYPE
                    }
                    .singleOrNull()
                    ?.apply { isAccessible = true }
            } ?: return "set-ignored-slots-contract-missing"
        val batteryContainerClass =
            readOrNull {
                Class.forName(BATTERY_CONTAINER, false, classLoader)
            } ?: return "battery-container-class-missing"
        val hideField =
            readOrNull {
                generateSequence(batteryContainerClass) { clazz -> clazz.superclass }
                    .mapNotNull { clazz ->
                        clazz.declaredFields.firstOrNull { candidate ->
                            candidate.name == "mIsHideBattery" &&
                                candidate.type == java.lang.Boolean.TYPE
                        }
                    }
                    .firstOrNull()
                    ?.apply { isAccessible = true }
            } ?: return "battery-hide-field-missing"
        val batterySetIsHide =
            readOrNull {
                batteryContainerClass.declaredMethods
                    .firstOrNull { method ->
                        method.name == "setIsHideBattery" &&
                            method.parameterTypes.contentEquals(
                                arrayOf(Boolean::class.javaObjectType),
                            ) &&
                            method.returnType == Void.TYPE
                    }
                    ?.apply { isAccessible = true }
            } ?: return "battery-hide-method-contract-missing"

        val onMeasure =
            readOrNull {
                containerClass.declaredMethods
                    .firstOrNull { method ->
                        method.name == "onMeasure" &&
                            method.parameterTypes.contentEquals(
                                arrayOf(
                                    Int::class.javaPrimitiveType,
                                    Int::class.javaPrimitiveType,
                                ),
                            ) &&
                            method.returnType == Void.TYPE
                    }
                    ?.apply { isAccessible = true }
            } ?: return "on-measure-contract-missing"
        val onLayout =
            readOrNull {
                containerClass.declaredMethods
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
            } ?: return "on-layout-contract-missing"

        val getIslandShowing =
            readOrNull {
                generateSequence(containerClass) { clazz -> clazz.superclass }
                    .mapNotNull { clazz ->
                        clazz.declaredMethods.firstOrNull { method ->
                            method.name == "getIslandShowing" &&
                                method.parameterCount == 0 &&
                                method.returnType == Boolean::class.javaPrimitiveType
                        }
                    }
                    .firstOrNull()
                    ?.apply { isAccessible = true }
            } ?: return "island-showing-contract-missing"

        eventSink = guardedDiagnostic(onEvent)
        failNativeSink = onFailNative
        ignoredSlotsField = field
        addIgnoredSlotsMethod = addIgnoredSlots
        setIgnoredSlotsMethod = setIgnoredSlots
        batteryHideField = hideField

        val first =
            runCatching {
                module
                    .hook(onMeasure)
                    .setId(MEASURE_HOOK_ID)
                    .intercept(layoutHooker(refreshMasksAfter = false))
            }.getOrElse { error ->
                clearInstallState()
                rethrowFatal(error)
                return "measure-hook-" + (error.message ?: error.javaClass.simpleName)
            }
        val second =
            runCatching {
                module
                    .hook(onLayout)
                    .setId(LAYOUT_HOOK_ID)
                    .intercept(layoutHooker(refreshMasksAfter = true))
            }.getOrElse { error ->
                val failure = rollbackInstall(
                    "layout-hook-" + (error.message ?: error.javaClass.simpleName),
                    listOf(first),
                )
                rethrowFatal(error)
                return failure
            }
        val third =
            runCatching {
                module
                    .hook(batterySetIsHide)
                    .setId(BATTERY_HIDE_HOOK_ID)
                    .intercept(batteryHideStateHooker())
            }.getOrElse { error ->
                val failure = rollbackInstall(
                    "battery-hide-hook-" + (error.message ?: error.javaClass.simpleName),
                    listOf(first, second),
                )
                rethrowFatal(error)
                return failure
            }

        val fourth =
            runCatching {
                module
                    .hook(getIslandShowing)
                    .setId(ISLAND_SHOWING_HOOK_ID)
                    .intercept(islandShowingHooker())
            }.getOrElse { error ->
                val failure = rollbackInstall(
                    "island-showing-hook-" + (error.message ?: error.javaClass.simpleName),
                    listOf(first, second, third),
                )
                rethrowFatal(error)
                return failure
            }

        measureHook = first
        layoutHook = second
        batteryHideHook = third
        islandShowingHook = fourth
        return null
    }

    @Synchronized
    fun activate(host: Any): Result {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return Result.Failure("main-thread-required")
        }
        if (installedHookCount != HOOK_COUNT) {
            return Result.Failure("hooks-not-ready")
        }
        val hostView =
            host as? ViewGroup
                ?: return Result.Failure("host-not-view-group")
        if (hostView.javaClass.name != HOME_HOST) {
            return Result.Failure("home-host-mismatch")
        }
        val statusIcons =
            NativeParticipantAccess.groupFor(host)
                ?: return Result.Failure("status-icon-group-missing")
        if (statusIcons.javaClass.name != STATUS_ICON_CONTAINER) {
            return Result.Failure("status-icon-group-type-mismatch")
        }
        val batteryContainer =
            hostView.directChild(BATTERY_CONTAINER) as? ViewGroup
                ?: return Result.Failure("battery-container-missing")
        val battery =
            batteryContainer.directChild(BATTERY_VIEW)
                ?: return Result.Failure("battery-view-missing")
        val batteryCarrier =
            SysUiCarrierMetrics.resolveView(battery)
                ?: return Result.Failure("battery-core-carrier-missing")
        val field =
            ignoredSlotsField
                ?: return Result.Failure("ignored-slots-field-unavailable")
        val hideField =
            batteryHideField
                ?: return Result.Failure("battery-hide-field-unavailable")
        SysUiCarrierMetrics.resolveWidthPx(batteryCarrier)
            ?: return Result.Failure("battery-core-width-unavailable")

        readOrNull { field.get(statusIcons) as? MutableList<*> }
            ?: return Result.Failure("ignored-slots-list-unavailable")

        val existing = current
        if (existing?.matches(hostView, statusIcons, batteryContainer, battery, batteryCarrier) == true) {
            if (!existing.syncEndReservation()) {
                return Result.Failure("home-reuse-sync-failed-native-restored")
            }
            val masked = existing.refreshClipMasks()
            if (
                current !== existing
            ) {
                return Result.Failure("home-reuse-aborted-after-fail-native")
            }
            batteryContainer.requestLayout()
            return Result.Active(representedSlots.size, masked, true)
        }

        existing?.stop("host-replaced")
        val session =
            Session(
                host = hostView,
                statusIcons = statusIcons,
                batteryContainer = batteryContainer,
                battery = battery,
                batteryCarrier = batteryCarrier,
                ignoredSlotsField = field,
                addIgnoredSlotsMethod = null,
                setIgnoredSlotsMethod = null,
                ignoreScope = IgnoreScope.CALL,
                batteryHideField = hideField,
                surfaceName = "home",
                eventPrefix = "homePresentation",
                retainReservationOnWidthLoss = false,
                onEvent = { event -> eventSink?.invoke(event) },
                onFailNative = ::onSessionFailure,
            )
        current = session
        val masked = session.start()
        if (
            current !== session
        ) {
            return Result.Failure("home-activation-aborted-after-fail-native")
        }
        batteryContainer.requestLayout()
        eventSink?.invoke(
            "homePresentation active carrier=MiuiStatusBatteryContainer.overlay " +
                "representedSlots=" + representedSlots.joinToString(",") +
                " maskedViews=" + masked +
                " slotExclusion=scoped-native-measure-layout " +
                    "carrierReservation=status-icons-end-padding " +
                    "carrierAuthority=battery_icon_container visualMask=clipBounds",
        )
        return Result.Active(representedSlots.size, masked, false)
    }

    @Synchronized
    fun activateKeyguard(
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
        deferNativeLayoutOwnershipUntilCommit: Boolean = false,
        onEvent: (String) -> Unit,
        onFailNative: (String) -> Unit,
        onReady: (Result.Active) -> Unit,
    ): Result =
        activateKeyguardFamily(
            surface = KeyguardFamilySurface.KEYGUARD,
            resolved = resolved,
            preMaskBeforeLayout = false,
            deferNativeLayoutOwnershipUntilCommit =
                deferNativeLayoutOwnershipUntilCommit,
            onEvent = onEvent,
            onFailNative = onFailNative,
            onReady = onReady,
        )

    @Synchronized
    fun deactivateKeyguard(source: String): Result =
        deactivateKeyguardFamily(
            surface = KeyguardFamilySurface.KEYGUARD,
            source = source,
        )

    @Synchronized
    fun commitKeyguardDeferredLayoutOwnership(): Result {
        val session =
            keyguardFamilyCurrent
                ?: return Result.Inactive(0)
        if (keyguardFamilySurface != KeyguardFamilySurface.KEYGUARD) {
            return Result.Failure("keyguard-family-surface-mismatch")
        }
        val masked =
            session.commitDeferredNativeLayoutOwnership()
                ?: return Result.Failure(
                    "keyguard-deferred-layout-commit-unavailable",
                )
        if (
            keyguardFamilyCurrent !== session ||
                keyguardFamilySurface != KeyguardFamilySurface.KEYGUARD
        ) {
            return Result.Failure(
                "keyguard-deferred-layout-commit-aborted-after-fail-native",
            )
        }
        return if (session.isLayoutCutoverReady()) {
            Result.Active(
                representedSlots = representedSlots.size,
                maskedViews = masked,
                reused = true,
            )
        } else {
            Result.Prepared(
                representedSlots = representedSlots.size,
                reused = true,
            )
        }
    }

    @Synchronized
    fun activateAod(
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
        preMaskBeforeLayout: Boolean = false,
        onEvent: (String) -> Unit,
        onFailNative: (String) -> Unit,
        onReady: (Result.Active) -> Unit,
    ): Result =
        activateKeyguardFamily(
            surface = KeyguardFamilySurface.AOD,
            resolved = resolved,
            preMaskBeforeLayout = preMaskBeforeLayout,
            deferNativeLayoutOwnershipUntilCommit = false,
            onEvent = onEvent,
            onFailNative = onFailNative,
            onReady = onReady,
        )

    @Synchronized
    fun deactivateAod(source: String): Result =
        deactivateKeyguardFamily(
            surface = KeyguardFamilySurface.AOD,
            source = source,
        )

    private fun activateKeyguardFamily(
        surface: KeyguardFamilySurface,
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
        preMaskBeforeLayout: Boolean,
        deferNativeLayoutOwnershipUntilCommit: Boolean,
        onEvent: (String) -> Unit,
        onFailNative: (String) -> Unit,
        onReady: (Result.Active) -> Unit,
    ): Result {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return Result.Failure("main-thread-required")
        }
        if (installedHookCount != HOOK_COUNT) {
            return Result.Failure("hooks-not-ready")
        }

        val field =
            ignoredSlotsField
                ?: return Result.Failure("ignored-slots-field-unavailable")
        val addMethod =
            addIgnoredSlotsMethod
                ?: return Result.Failure("add-ignored-slots-method-unavailable")
        val setMethod =
            setIgnoredSlotsMethod
                ?: return Result.Failure("set-ignored-slots-method-unavailable")
        val hideField =
            batteryHideField
                ?: return Result.Failure("battery-hide-field-unavailable")
        SysUiCarrierMetrics.resolveWidthPx(resolved.batteryCarrier)
            ?: return Result.Failure(surface.surfaceName + "-battery-core-width-unavailable")

        readOrNull { field.get(resolved.statusIcons) as? MutableList<*> }
            ?: return Result.Failure(
                surface.surfaceName + "-ignored-slots-list-unavailable",
            )

        keyguardFamilyEventSink = guardedDiagnostic(onEvent)
        keyguardFamilyFailNativeSink = onFailNative
        keyguardFamilyReadySink = onReady

        val existing = keyguardFamilyCurrent
        if (
            existing?.matches(
                host = resolved.host,
                statusIcons = resolved.statusIcons,
                batteryContainer = resolved.systemIcons,
                battery = resolved.battery,
                batteryCarrier = resolved.batteryCarrier,
            ) == true
        ) {
            keyguardFamilySurface = surface
            existing.retargetPresentation(
                surfaceName = surface.surfaceName,
                eventPrefix = surface.eventPrefix,
            )
            val masked =
                existing.start(
                    deferVisualMaskUntilLayout = true,
                    preMaskBeforeLayout = preMaskBeforeLayout,
                    deferNativeLayoutOwnershipUntilCommit =
                        deferNativeLayoutOwnershipUntilCommit,
                    onLayoutReady = { maskedViews ->
                        onKeyguardFamilyLayoutReady(
                            session = existing,
                            surface = surface,
                            maskedViews = maskedViews,
                            reused = true,
                        )
                    },
                )
            if (
                keyguardFamilyCurrent !== existing ||
                    keyguardFamilySurface != surface
            ) {
                return Result.Failure(
                    surface.surfaceName + "-reuse-aborted-after-fail-native",
                )
            }
            return if (existing.isLayoutCutoverReady()) {
                Result.Active(representedSlots.size, masked, true)
            } else {
                Result.Prepared(representedSlots.size, true)
            }
        }

        existing?.stop(surface.surfaceName + "-host-replaced")
        val session =
            Session(
                host = resolved.host,
                statusIcons = resolved.statusIcons,
                batteryContainer = resolved.systemIcons,
                battery = resolved.battery,
                batteryCarrier = resolved.batteryCarrier,
                ignoredSlotsField = field,
                addIgnoredSlotsMethod = addMethod,
                setIgnoredSlotsMethod = setMethod,
                ignoreScope = IgnoreScope.SESSION,
                batteryHideField = hideField,
                surfaceName = surface.surfaceName,
                eventPrefix = surface.eventPrefix,
                retainReservationOnWidthLoss = false,
                onEvent = { event -> keyguardFamilyEventSink?.invoke(event) },
                onFailNative = ::onKeyguardFamilySessionFailure,
            )
        keyguardFamilyCurrent = session
        keyguardFamilySurface = surface
        val masked =
            session.start(
                deferVisualMaskUntilLayout = true,
                preMaskBeforeLayout = preMaskBeforeLayout,
                deferNativeLayoutOwnershipUntilCommit =
                    deferNativeLayoutOwnershipUntilCommit,
                onLayoutReady = { maskedViews ->
                    onKeyguardFamilyLayoutReady(
                        session = session,
                        surface = surface,
                        maskedViews = maskedViews,
                        reused = false,
                    )
                },
            )
        if (
            keyguardFamilyCurrent !== session ||
                keyguardFamilySurface != surface
        ) {
            return Result.Failure(
                surface.surfaceName + "-activation-aborted-after-fail-native",
            )
        }
        return if (session.isLayoutCutoverReady()) {
            Result.Active(representedSlots.size, masked, false)
        } else {
            Result.Prepared(representedSlots.size, false)
        }
    }

    private fun deactivateKeyguardFamily(
        surface: KeyguardFamilySurface,
        source: String,
    ): Result {
        if (keyguardFamilySurface != surface) {
            return Result.Inactive(0)
        }
        val session = keyguardFamilyCurrent ?: return Result.Inactive(0)
        keyguardFamilyCurrent = null
        keyguardFamilySurface = null
        return try {
            val restored = session.stop(source)
            keyguardFamilyEventSink?.invoke(
                surface.eventPrefix + " inactive source=" + source +
                    " restoredViews=" + restored,
            )
            Result.Inactive(restored)
        } finally {
            keyguardFamilyEventSink = null
            keyguardFamilyFailNativeSink = null
            keyguardFamilyReadySink = null
        }
    }

    @Synchronized
    fun ownsBatteryContainer(candidate: ViewGroup): Boolean =
        current?.ownsBatteryContainer(candidate) == true

    @Synchronized
    fun ownsKeyguardBatteryContainer(candidate: ViewGroup): Boolean =
        keyguardFamilySurface == KeyguardFamilySurface.KEYGUARD &&
            keyguardFamilyCurrent?.ownsBatteryContainer(candidate) == true

    @Synchronized
    fun activateCc(
        host: ViewGroup,
        statusIcons: ViewGroup,
        batteryContainer: ViewGroup,
        battery: View,
        batteryCarrier: View,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onFailNative: (String) -> Unit,
        onReady: (Result.Active) -> Unit,
    ): Result {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return Result.Failure("main-thread-required")
        }
        if (installedHookCount != HOOK_COUNT) {
            return Result.Failure("hooks-not-ready", retryAfterLayout = true)
        }
        if (host !== batteryContainer || host.javaClass.name != BATTERY_CONTAINER) {
            return Result.Failure("fake-status-bar-area-mismatch")
        }
        if (statusIcons.javaClass.name != STATUS_ICON_CONTAINER) {
            return Result.Failure("status-icon-group-type-mismatch")
        }
        if (battery.javaClass.name != BATTERY_VIEW) {
            return Result.Failure("battery-view-type-mismatch")
        }

        val field =
            ignoredSlotsField
                ?: return Result.Failure("ignored-slots-field-unavailable")
        val addMethod =
            addIgnoredSlotsMethod
                ?: return Result.Failure("add-ignored-slots-method-unavailable")
        val setMethod =
            setIgnoredSlotsMethod
                ?: return Result.Failure("set-ignored-slots-method-unavailable")
        val hideField =
            batteryHideField
                ?: return Result.Failure("battery-hide-field-unavailable")
        SysUiCarrierMetrics.resolveWidthPx(batteryCarrier)
            ?: return Result.Failure(
                "battery-core-width-unavailable",
                retryAfterLayout = true,
            )

        readOrNull { field.get(statusIcons) as? MutableList<*> }
            ?: return Result.Failure(
                "ignored-slots-list-unavailable",
                retryAfterLayout = true,
            )

        controlCenterEventSink = guardedDiagnostic(onEvent)
        controlCenterFailNativeSink = onFailNative
        controlCenterReadySink = onReady

        val existing = controlCenterCurrent
        val sameHost =
            existing?.matches(
                host = host,
                statusIcons = statusIcons,
                batteryContainer = batteryContainer,
                battery = battery,
                batteryCarrier = batteryCarrier,
            ) == true
        if (!sameHost && (host.width <= 0 || host.layoutParams?.width != host.width)) {
            // The fake carrier must finish its native layout before we lease its width.
            return Result.Failure("fake-carrier-native-layout-pending", retryAfterLayout = true)
        }
        if (sameHost) {
            val masked =
                existing.start(
                    deferVisualMaskUntilLayout = true,
                    onLayoutReady = { maskedViews ->
                        onCcLayoutReady(
                            session = existing,
                            maskedViews = maskedViews,
                            reused = true,
                        )
                    },
                )
            if (
                controlCenterCurrent !== existing
            ) {
                return Result.Failure(
                    "control-center-reuse-aborted-after-fail-native",
                )
            }
            return if (existing.isLayoutCutoverReady()) {
                Result.Active(
                    representedSlots = representedSlots.size,
                    maskedViews = masked,
                    reused = true,
                )
            } else {
                Result.Prepared(
                    representedSlots = representedSlots.size,
                    reused = true,
                )
            }
        }

        existing?.stop("host-replaced")
        val session =
            Session(
                host = host,
                statusIcons = statusIcons,
                batteryContainer = batteryContainer,
                battery = battery,
                batteryCarrier = batteryCarrier,
                ignoredSlotsField = field,
                addIgnoredSlotsMethod = addMethod,
                setIgnoredSlotsMethod = setMethod,
                ignoreScope = IgnoreScope.SESSION,
                batteryHideField = hideField,
                surfaceName = "control-center-fake",
                eventPrefix = "controlCenterPresentation",
                retainReservationOnWidthLoss = true,
                onEvent = { event -> controlCenterEventSink?.invoke(event) },
                onFailNative = ::onCcSessionFailure,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
            )
        controlCenterCurrent = session
        val masked =
            session.start(
                deferVisualMaskUntilLayout = true,
                onLayoutReady = { maskedViews ->
                    onCcLayoutReady(
                        session = session,
                        maskedViews = maskedViews,
                        reused = false,
                    )
                },
            )
        if (
            controlCenterCurrent !== session
        ) {
            return Result.Failure(
                "control-center-activation-aborted-after-fail-native",
            )
        }
        return if (session.isLayoutCutoverReady()) {
            Result.Active(
                representedSlots = representedSlots.size,
                maskedViews = masked,
                reused = false,
            )
        } else {
            Result.Prepared(
                representedSlots = representedSlots.size,
                reused = false,
            )
        }
    }

    @Synchronized
    fun adoptCcLayoutAfterHotReload(): Result {
        val session =
            controlCenterCurrent
                ?: return Result.Inactive(0)
        val masked =
            session.adoptTransferredCompactLayout()
                ?: return Result.Failure(
                    "transferred-compact-layout-adoption-failed",
                )
        if (
            controlCenterCurrent !== session
        ) {
            return Result.Failure(
                "transferred-compact-layout-aborted-after-fail-native",
            )
        }
        return Result.Active(
            representedSlots = representedSlots.size,
            maskedViews = masked,
            reused = true,
        )
    }

    @Synchronized
    fun deactivateCc(source: String): Result {
        val session =
            controlCenterCurrent
                ?: return Result.Inactive(0)
        controlCenterCurrent = null
        return try {
            val restored = session.stop(source)
            controlCenterEventSink?.invoke(
                "controlCenterPresentation inactive source=" + source +
                    " restoredViews=" + restored,
            )
            Result.Inactive(restored)
        } finally {
            controlCenterEventSink = null
            controlCenterFailNativeSink = null
            controlCenterReadySink = null
        }
    }

    @Synchronized
    fun onCcVisibilityChanged(visible: Boolean): Boolean =
        controlCenterCurrent?.onCcVisibilityChanged(visible) ?: true

    @Synchronized
    fun updateCcTransitionReservation(
        requestedSlotWidthPx: Int,
    ): Boolean =
        controlCenterCurrent
            ?.updateTransitionReservation(requestedSlotWidthPx)
            ?: false

    @Synchronized
    fun clearCcTransitionReservation(source: String): Boolean =
        controlCenterCurrent
            ?.clearTransitionReservation(source)
            ?: true

    @Synchronized
    fun failCcPresentation(reason: String): Boolean {
        if (controlCenterCurrent == null) return false
        onCcSessionFailure(reason)
        return true
    }

    @Synchronized
    fun deactivate(source: String): Result {
        val session = current ?: return Result.Inactive(0)
        current = null
        steadyPeerMirrorActive = false
        steadyPeerMirrorHiddenSlots = emptySet()
        controlCenterCurrent?.updateSteadyPeerMirror(active = false, hiddenSlots = emptySet())
        val restored = session.stop(source)
        eventSink?.invoke(
            "homePresentation inactive source=" + source +
                " restoredViews=" + restored,
        )
        return Result.Inactive(restored)
    }

    @Synchronized
    fun releaseGenerationForHotReload(
        requestLayout: Boolean = true,
    ): Int {
        val failures = ArrayList<Throwable>(1)
        fun stop(session: Session?): Int =
            session?.let {
                try {
                    it.stop(
                        source = "hotReload-oldGeneration",
                        requestLayout = requestLayout,
                    )
                } catch (error: Throwable) {
                    rethrowFatal(error)
                    failures.add(error)
                    0
                }
            } ?: 0

        val cc = controlCenterCurrent
        controlCenterCurrent = null
        val controlCenterRestored = stop(cc)

        val home = current
        current = null
        val homeRestored = stop(home)

        val keyguard = keyguardFamilyCurrent
        keyguardFamilyCurrent = null
        keyguardFamilySurface = null
        val keyguardFamilyRestored = stop(keyguard)

        controlCenterEventSink = null
        controlCenterFailNativeSink = null
        controlCenterReadySink = null
        eventSink = null
        failNativeSink = null
        keyguardFamilyEventSink = null
        keyguardFamilyFailNativeSink = null
        keyguardFamilyReadySink = null
        controlCenterSourceScene = SourceScene.UNKNOWN
        steadyPeerMirrorActive = false
        steadyPeerMirrorHiddenSlots = emptySet()

        if (failures.isNotEmpty()) {
            val error = IllegalStateException("hotReload-release-incomplete", failures.first())
            failures.drop(1).forEach(error::addSuppressed)
            throw error
        }
        return homeRestored + keyguardFamilyRestored + controlCenterRestored
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        val failures = ArrayList<Throwable>(1)
        fun release(action: () -> Unit) {
            try {
                action()
            } catch (error: Throwable) {
                rethrowFatal(error)
                failures.add(error)
            }
        }
        fun unhook(handle: HookHandle?): HookHandle? {
            if (handle == null) return null
            return try {
                handle.unhook()
                null
            } catch (error: Throwable) {
                rethrowFatal(error)
                failures.add(error)
                handle
            }
        }
        release { deactivateCc(source) }
        release { deactivateAod(source) }
        release { deactivateKeyguard(source) }
        release { deactivate(source) }
        val failedMeasure = unhook(measureHook)
        val failedLayout = unhook(layoutHook)
        val failedBatteryHide = unhook(batteryHideHook)
        val failedIsland = unhook(islandShowingHook)
        clearInstallState()
        // Unremoved Hooks still belong to this source: do not reinstall duplicates.
        measureHook = failedMeasure
        layoutHook = failedLayout
        batteryHideHook = failedBatteryHide
        islandShowingHook = failedIsland
        if (failures.isNotEmpty()) {
            val error = IllegalStateException("presentation-reset-incomplete", failures.first())
            failures.drop(1).forEach(error::addSuppressed)
            throw error
        }
    }

    @Synchronized
    fun cleanupLegacyParticipant(host: Any): LegacyCleanupResult {
        val group =
            NativeParticipantAccess.groupFor(host)
                ?: return LegacyCleanupResult.Failure("status-icon-group-missing")
        val legacyView = NativeParticipantAccess.findSlotView(group, LEGACY_SLOT)
        val handles =
            when (val resolution = NativeParticipantAccess.resolve(host)) {
                is NativeParticipantAccess.ResolveResult.Ready -> resolution.handles
                is NativeParticipantAccess.ResolveResult.Failure -> {
                    return if (legacyView == null) {
                        LegacyCleanupResult.NotPresent
                    } else {
                        LegacyCleanupResult.Failure(resolution.reason)
                    }
                }
            }
        val holder = NativeParticipantAccess.iconHolder(handles, LEGACY_SLOT)
        if (legacyView == null && holder == null) {
            return LegacyCleanupResult.NotPresent
        }
        val removal =
            NativeParticipantAccess.removal(handles.controller.javaClass)
                ?: return LegacyCleanupResult.Failure("legacy-removal-contract-missing")
        val removed =
            runCatching {
                NativeParticipantAccess.invokeRemoval(handles, removal, LEGACY_SLOT)
                NativeParticipantAccess.clearBindableEntries(handles, LEGACY_SLOT)
                NativeParticipantAccess.findSlotView(group, LEGACY_SLOT) == null &&
                    NativeParticipantAccess.iconHolder(handles, LEGACY_SLOT) == null
            }.getOrElse { error ->
                rethrowFatal(error)
                false
            }
        return if (removed) {
            LegacyCleanupResult.Removed
        } else {
            LegacyCleanupResult.Failure("legacy-removal-verification-failed")
        }
    }

    // A post-native owner failure must not replace the framework method's result.
    private inline fun afterNativeCallback(
        session: Session?,
        source: String,
        action: () -> Unit,
    ) {
        if (session == null) return
        try {
            action()
        } catch (error: Throwable) {
            rethrowFatal(error)
            session.reportCallbackFailure(source, error)
            try {
                synchronized(this) {
                    when {
                        current === session -> onSessionFailure(source)
                        controlCenterCurrent === session -> onCcSessionFailure(source)
                        keyguardFamilyCurrent === session -> onKeyguardFamilySessionFailure(source)
                    }
                }
            } catch (cleanup: Throwable) {
                rethrowFatal(cleanup)
                session.reportCallbackFailure(source, error, cleanup)
            }
        }
    }

    private fun layoutHooker(refreshMasksAfter: Boolean): Hooker =
        Hooker { chain ->
            val target = readOrNull { chain.thisObject as? ViewGroup }
                ?: return@Hooker chain.proceed()
            val session =
                synchronized(this) {
                    controlCenterCurrent?.takeIf { candidate -> candidate.owns(target) }
                        ?: current?.takeIf { candidate -> candidate.owns(target) }
                        ?: keyguardFamilyCurrent?.takeIf { candidate -> candidate.owns(target) }
                } ?: return@Hooker chain.proceed()

            emitDiagnostic {
                session.traceFakeCarrierWidth(
                    if (refreshMasksAfter) "layout-before" else "measure-before",
                )
            }
            val result = session.withRepresentedSlotsIgnored { chain.proceed() }
            emitDiagnostic {
                session.traceFakeCarrierWidth(
                    if (refreshMasksAfter) "layout-after" else "measure-after",
                )
            }
            afterNativeCallback(
                session,
                if (refreshMasksAfter) "layout-post-native" else "measure-post-native",
            ) {
                if (refreshMasksAfter) {
                    syncPeerMirrorAfterLayout(session)
                }
                if (
                    refreshMasksAfter &&
                    session.validateNativeLayoutBeforeVisualMask()
                ) {
                    val masked = session.refreshClipMasks()
                    session.onNativeLayoutCompleted(masked)
                }
            }
            result
        }

    private fun islandShowingHooker(): Hooker =
        Hooker { chain ->
            val nativeResult = chain.proceed()
            val nativeShowing =
                nativeResult as? Boolean
                    ?: return@Hooker nativeResult
            val target =
                readOrNull { chain.thisObject as? ViewGroup }
                    ?: return@Hooker nativeResult
            val session =
                synchronized(this) {
                    controlCenterCurrent
                        ?.takeIf { candidate -> candidate.owns(target) }
                } ?: return@Hooker nativeResult
            val exposed =
                SteadyPeerMirrorPolicy.exposeFakeIslandShowing(
                    nativeIslandShowing = nativeShowing,
                    steadyMirrorActive = session.isSteadyPeerMirrorActive(),
                )
            if (nativeShowing && !exposed) {
                session.reportSteadyPeerMirrorIslandSuppression()
            }
            exposed
        }

    private fun syncPeerMirrorAfterLayout(session: Session): Boolean {
        val role =
            synchronized(this) {
                when {
                    current === session -> HOME_SURFACE
                    controlCenterCurrent === session -> CONTROL_CENTER_FAKE_SURFACE
                    else -> null
                }
            } ?: return false

        if (role == HOME_SURFACE) {
            val homeMirrorAllowed =
                synchronized(this) {
                    controlCenterSourceScene == SourceScene.HOME
                }
            if (!homeMirrorAllowed) {
                return false
            }

            val snapshot = session.captureSteadyPeerMirror()
            var changed = false
            val fake =
                synchronized(this) {
                    changed =
                        steadyPeerMirrorActive != snapshot.active ||
                            steadyPeerMirrorHiddenSlots != snapshot.hiddenSlots
                    steadyPeerMirrorActive = snapshot.active
                    steadyPeerMirrorHiddenSlots = snapshot.hiddenSlots
                    if (changed) {
                        emitDiagnostic {
                            eventSink?.invoke(
                                "steadyPeerMirror source=home active=" + snapshot.active +
                                    " hiddenSlots=[" +
                                    snapshot.hiddenSlots.sorted().joinToString(",") +
                                    "] authority=home-native-island-state",
                            )
                        }
                    }
                    controlCenterCurrent
                }
            fake?.updateSteadyPeerMirror(
                active = snapshot.active,
                hiddenSlots = snapshot.hiddenSlots,
            )
            return changed
        }

        val snapshot =
            synchronized(this) {
                if (controlCenterSourceScene == SourceScene.HOME) {
                    SteadyPeerMirrorSnapshot(
                        active = steadyPeerMirrorActive,
                        hiddenSlots = steadyPeerMirrorHiddenSlots,
                    )
                } else {
                    SteadyPeerMirrorSnapshot(false, emptySet())
                }
            }
        session.updateSteadyPeerMirror(
            active = snapshot.active,
            hiddenSlots = snapshot.hiddenSlots,
        )
        return false
    }
    private fun batteryHideStateHooker(): Hooker =
        Hooker { chain ->
            val target = readOrNull { chain.thisObject as? ViewGroup }
                ?: return@Hooker chain.proceed()
            val result = chain.proceed()
            val session =
                synchronized(this) {
                    controlCenterCurrent
                        ?.takeIf { candidate -> candidate.ownsBatteryContainer(target) }
                        ?: current?.takeIf { candidate -> candidate.ownsBatteryContainer(target) }
                        ?: keyguardFamilyCurrent
                            ?.takeIf { candidate -> candidate.ownsBatteryContainer(target) }
                }
            afterNativeCallback(session, "battery-hide-post-native") {
                session?.syncEndReservation()
            }
            result
        }

    // Ordinary restore failures must not block other owners' native fallback.
    private fun finishFailure(vararg actions: () -> Unit) {
        val failures = ArrayList<Throwable>(1)
        actions.forEach { action ->
            try {
                action()
            } catch (error: Throwable) {
                rethrowFatal(error)
                failures.add(error)
            }
        }
        failures.firstOrNull()?.let { error ->
            failures.drop(1).forEach(error::addSuppressed)
            throw error
        }
    }

    @Synchronized
    private fun onSessionFailure(reason: String) {
        val session = current ?: return
        current = null
        steadyPeerMirrorActive = false
        steadyPeerMirrorHiddenSlots = emptySet()
        finishFailure(
            { controlCenterCurrent?.updateSteadyPeerMirror(active = false, hiddenSlots = emptySet()) },
            { session.stop("fail-native:" + reason) },
            { eventSink?.invoke("homePresentation failNative reason=" + reason) },
            { failNativeSink?.invoke(reason) },
        )
    }

    @Synchronized
    private fun onKeyguardFamilySessionFailure(reason: String) {
        val session = keyguardFamilyCurrent ?: return
        val surface = keyguardFamilySurface ?: return
        keyguardFamilyCurrent = null
        keyguardFamilySurface = null
        try {
            finishFailure(
                { session.stop("fail-native:" + reason) },
                {
                    keyguardFamilyEventSink?.invoke(
                        surface.eventPrefix + " failNative reason=" + reason,
                    )
                },
                { keyguardFamilyFailNativeSink?.invoke(reason) },
            )
        } finally {
            keyguardFamilyEventSink = null
            keyguardFamilyFailNativeSink = null
            keyguardFamilyReadySink = null
        }
    }

    @Synchronized
    private fun onKeyguardFamilyLayoutReady(
        session: Session,
        surface: KeyguardFamilySurface,
        maskedViews: Int,
        reused: Boolean,
    ) {
        if (
            keyguardFamilyCurrent !== session ||
            keyguardFamilySurface != surface
        ) {
            return
        }
        val active =
            Result.Active(
                representedSlots = representedSlots.size,
                maskedViews = maskedViews,
                reused = reused,
            )
        keyguardFamilyEventSink?.invoke(
            surface.eventPrefix +
                " active carrier=MiuiStatusBatteryContainer.overlay " +
                "representedSlots=" + representedSlots.joinToString(",") +
                " maskedViews=" + maskedViews +
                " slotExclusion=session-native-ignored-slots " +
                "carrierReservation=status-icons-end-padding " +
                "carrierAuthority=battery_icon_container visualMask=clipBounds " +
                "motion=keyguard-system-icons-inherited cutover=" +
                if (surface == KeyguardFamilySurface.AOD) {
                    "stable-aod-layout-ready"
                } else {
                    "compact-layout-ready"
                },
        )
        keyguardFamilyReadySink?.invoke(active)
    }

    @Synchronized
    private fun onCcLayoutReady(
        session: Session,
        maskedViews: Int,
        reused: Boolean,
    ) {
        if (controlCenterCurrent !== session) {
            return
        }
        val active =
            Result.Active(
                representedSlots = representedSlots.size,
                maskedViews = maskedViews,
                reused = reused,
            )
        controlCenterEventSink?.invoke(
            "controlCenterPresentation active carrier=QS_FAKE.system_icon_area " +
                "representedSlots=" + representedSlots.joinToString(",") +
                " maskedViews=" + maskedViews +
                " slotExclusion=session-native-ignored-slots " +
                "carrierReservation=qs-fake-capacity-lease+status-icons-end-padding " +
                "carrierAuthority=battery_icon_container visualMask=clipBounds " +
                "cutover=compact-layout-ready nativeLayoutReservationOwner=single",
        )
        controlCenterReadySink?.invoke(active)
    }

    @Synchronized
    private fun onCcSessionFailure(reason: String) {
        val session = controlCenterCurrent ?: return
        controlCenterCurrent = null
        try {
            finishFailure(
                { session.stop("fail-native:" + reason) },
                { controlCenterEventSink?.invoke("controlCenterPresentation failNative reason=" + reason) },
                { controlCenterFailNativeSink?.invoke(reason) },
            )
        } finally {
            controlCenterEventSink = null
            controlCenterFailNativeSink = null
            controlCenterReadySink = null
        }
    }

    private fun rollbackInstall(
        reason: String,
        handles: List<HookHandle>,
    ): String {
        val remaining = handles.filter { handle ->
            try {
                handle.unhook()
                false
            } catch (error: Throwable) {
                rethrowFatal(error)
                true
            }
        }
        clearInstallState()
        measureHook = remaining.firstOrNull { it.id == MEASURE_HOOK_ID }
        layoutHook = remaining.firstOrNull { it.id == LAYOUT_HOOK_ID }
        batteryHideHook = remaining.firstOrNull { it.id == BATTERY_HIDE_HOOK_ID }
        islandShowingHook = remaining.firstOrNull { it.id == ISLAND_SHOWING_HOOK_ID }
        return if (remaining.isEmpty()) reason else reason + "-hook-cleanup-failed"
    }

    private fun clearInstallState() {
        measureHook = null
        layoutHook = null
        batteryHideHook = null
        islandShowingHook = null
        ignoredSlotsField = null
        addIgnoredSlotsMethod = null
        setIgnoredSlotsMethod = null
        batteryHideField = null
        controlCenterCurrent = null
        keyguardFamilyCurrent = null
        keyguardFamilySurface = null
        controlCenterEventSink = null
        controlCenterFailNativeSink = null
        controlCenterReadySink = null
        eventSink = null
        failNativeSink = null
        keyguardFamilyEventSink = null
        keyguardFamilyFailNativeSink = null
        keyguardFamilyReadySink = null
        controlCenterSourceScene = SourceScene.UNKNOWN
        steadyPeerMirrorActive = false
        steadyPeerMirrorHiddenSlots = emptySet()
    }

    private class Session(
        host: ViewGroup,
        statusIcons: ViewGroup,
        batteryContainer: ViewGroup,
        battery: View,
        batteryCarrier: View,
        private val ignoredSlotsField: Field,
        private val addIgnoredSlotsMethod: java.lang.reflect.Method?,
        private val setIgnoredSlotsMethod: java.lang.reflect.Method?,
        private val ignoreScope: IgnoreScope,
        private val batteryHideField: Field,
        private var surfaceName: String,
        private var eventPrefix: String,
        private val retainReservationOnWidthLoss: Boolean,
        private val onEvent: (String) -> Unit,
        private val onFailNative: (String) -> Unit,
        private val isDetailedDiagnosticsEnabled: () -> Boolean = { true },
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val statusIcons = WeakReference(statusIcons)
        private val batteryContainer = WeakReference(batteryContainer)
        private val battery = WeakReference(battery)
        private val batteryCarrier = WeakReference(batteryCarrier)
        private enum class Lifecycle {
            CREATED,
            RUNNING,
            STOPPED,
        }

        private var lifecycle = Lifecycle.CREATED
        private val active: Boolean
            get() = lifecycle != Lifecycle.STOPPED
        private val started: Boolean
            get() = lifecycle != Lifecycle.CREATED
        private var deferVisualMaskUntilLayout = false
        private var compactLayoutReady = false
        private var layoutReadyCallback: ((Int) -> Unit)? = null
        private var lastReservationDelta: Int? = null
        private var steadyPeerMirrorActive = false
        private var steadyPeerMirrorHiddenSlots: Set<String> = emptySet()
        private var mirrorIslandSuppressionLogged = false
        private var nativePadding: PaddingState? = null
        private var appliedPadding: PaddingState? = null
        private var nativeFakeCarrierLayoutWidthPx: Int? = null
        private var fakeCarrierParentWidth: Int? = null
        private var appliedFakeCarrierWidthPx: Int? = null
        private var fakeCarrierCapacityDeltaPx: Int? = null
        private enum class LeasePhase {
            PREARM,
            VISIBLE,
            SUPPRESSED,
        }

        private var capacityLeaseAwaitingLayout = false
        private var firstLeaseWidthMismatch: String? = null
        private var leasePhase = LeasePhase.PREARM
        private var pendingNativeCarrierWidth: Int? = null
        private var batteryWidthUnavailable = false
        private var persistentIgnoredSlotsApplied = false
        private var aodPeersReleased = false
        private var aodBatteryReleased = false
        private var nativeLayoutOwnershipDeferred = false
        private var ownedPersistentIgnoredSlots: List<String> = emptyList()
        private var transitionRequestedSlotWidthPx: Int? = null
        private val fakePeerMetrics =
            if (
                surfaceName == CONTROL_CENTER_FAKE_SURFACE &&
                statusIcons.javaClass.name ==
                    "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
            ) {
                readOrNull {
                    val cls = statusIcons.javaClass
                    cls.getDeclaredField("measureViews").apply { isAccessible = true } to
                        cls.getDeclaredField("iconSpacing").apply { isAccessible = true }
                }
            } else {
                null
            }
        private val clipStates = mutableListOf<ClipState>()
        private val mirroredPeerClipStates = mutableListOf<ClipState>()
        private val batteryLayoutListener =
            View.OnLayoutChangeListener {
                    _,
                    left,
                    _,
                    right,
                    _,
                    oldLeft,
                    _,
                    oldRight,
                    _,
                ->
                if (right - left != oldRight - oldLeft) {
                    syncEndReservation()
                }
            }

        private val carrierLayoutListener =
            View.OnLayoutChangeListener {
                    _,
                    left,
                    _,
                    right,
                    _,
                    oldLeft,
                    _,
                    oldRight,
                    _,
                ->
                if (right - left != oldRight - oldLeft) {
                    syncEndReservation()
                }
            }

        fun matches(
            host: ViewGroup,
            statusIcons: ViewGroup,
            batteryContainer: ViewGroup,
            battery: View,
            batteryCarrier: View,
        ): Boolean =
            active &&
                this.host.get() === host &&
                this.statusIcons.get() === statusIcons &&
                this.batteryContainer.get() === batteryContainer &&
                this.battery.get() === battery &&
                this.batteryCarrier.get() === batteryCarrier

        fun owns(candidate: ViewGroup): Boolean =
            active && statusIcons.get() === candidate

        fun ownsBatteryContainer(candidate: ViewGroup): Boolean =
            active && batteryContainer.get() === candidate

        fun reportCallbackFailure(source: String, error: Throwable, cleanup: Throwable? = null) {
            emitDiagnostic {
                onEvent(
                    eventPrefix + " callback source=" + source +
                        " state=" +
                        (if (cleanup == null) "callback-failed" else "cleanup-incomplete") +
                        " errorType=" + error.javaClass.name +
                        (cleanup?.let { " cleanupErrorType=" + it.javaClass.name } ?: ""),
                )
            }
        }

        fun retargetPresentation(
            surfaceName: String,
            eventPrefix: String,
        ) {
            if ((aodPeersReleased || aodBatteryReleased) && this.surfaceName != surfaceName) {
                onAodIconTransition(toLockScreen = false)
            }
            this.surfaceName = surfaceName
            this.eventPrefix = eventPrefix
        }

        fun hasPresentationClaim(): Boolean =
            active &&
                (
                    persistentIgnoredSlotsApplied ||
                        aodBatteryReleased ||
                        clipStates.isNotEmpty() ||
                        appliedPadding != null
                )

        fun isNativeCarrierPresentationVisible(): Boolean {
            val carrier = batteryContainer.get() ?: return false
            return active &&
                carrier.isAttachedToWindow &&
                carrier.visibility == View.VISIBLE &&
                carrier.alpha > 0f &&
                carrier.isShown
        }

        fun ownedRepresentedSlots(): Set<String> {
            if (!active || !compactLayoutReady) return emptySet()
            return clipStates
                .mapNotNull { state ->
                    state.view.get()
                        ?.let(NativeParticipantAccess::slotOf)
                        ?.takeIf(representedSlots::contains)
                }
                .toSet()
        }

        fun start(
            deferVisualMaskUntilLayout: Boolean = false,
            preMaskBeforeLayout: Boolean = false,
            deferNativeLayoutOwnershipUntilCommit: Boolean = false,
            onLayoutReady: ((Int) -> Unit)? = null,
        ): Int {
            this.deferVisualMaskUntilLayout = deferVisualMaskUntilLayout
            this.layoutReadyCallback = onLayoutReady
            if (started) {
                if (
                    nativeLayoutOwnershipDeferred &&
                    !deferNativeLayoutOwnershipUntilCommit
                ) {
                    val group =
                        statusIcons.get()
                            ?: run {
                                onFailNative("status-icon-group-released")
                                return 0
                            }
                    nativeLayoutOwnershipDeferred = false
                    compactLayoutReady = false
                    if (!applyPersistentIgnoredSlots(group)) return 0
                    if (!syncEndReservation()) return 0
                    onEvent(
                        eventPrefix +
                            " deferredLayoutRetarget state=resumed" +
                            " nativeLayoutOwnership=active" +
                            " next=native-status-icons-onLayout",
                    )
                }
                if (
                    deferNativeLayoutOwnershipUntilCommit &&
                    !persistentIgnoredSlotsApplied &&
                    appliedPadding == null
                ) {
                    nativeLayoutOwnershipDeferred = true
                    compactLayoutReady = false
                    return refreshClipMasks()
                }
                syncEndReservation()
                if (isLayoutCutoverReady()) {
                    layoutReadyCallback = null
                    return refreshClipMasks()
                }
                return if (
                    deferVisualMaskUntilLayout && preMaskBeforeLayout
                ) {
                    refreshClipMasks()
                } else {
                    0
                }
            }

            lifecycle = Lifecycle.RUNNING
            host.get()?.addOnAttachStateChangeListener(this)
            val group =
                statusIcons.get()
                    ?: run {
                        onFailNative("status-icon-group-released")
                        return 0
                    }
            nativePadding = PaddingState.from(group)
            battery.get()?.addOnLayoutChangeListener(batteryLayoutListener)
            batteryCarrier.get()?.addOnLayoutChangeListener(carrierLayoutListener)
            if (deferNativeLayoutOwnershipUntilCommit) {
                nativeLayoutOwnershipDeferred = true
                compactLayoutReady = false
                val masked = refreshClipMasks()
                onEvent(
                    eventPrefix +
                        " visualHandoff maskedViews=" + masked +
                        " nativeLayoutOwnership=deferred",
                )
                return masked
            }
            if (!applyPersistentIgnoredSlots(group)) return 0
            if (!syncEndReservation()) return 0

            if (
                VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                    deferVisualMaskUntilLayout = deferVisualMaskUntilLayout,
                    laidOut = group.isLaidOut,
                    layoutRequested = group.isLayoutRequested,
                    capacityLeaseAwaitingLayout =
                        capacityLeaseAwaitingLayout,
                    width = group.width,
                    height = group.height,
                )
            ) {
                val masked = refreshClipMasks()
                compactLayoutReady = true
                layoutReadyCallback = null
                onEvent(
                    eventPrefix + " layoutReady source=existing-native-status-icons-layout" +
                        " maskedViews=" + masked,
                )
                return masked
            }

            if (deferVisualMaskUntilLayout) {
                compactLayoutReady = false
                val preMasked =
                    deferVisualMaskUntilLayout && preMaskBeforeLayout
                val masked =
                    if (preMasked) {
                        refreshClipMasks()
                    } else {
                        0
                    }
                onEvent(
                    eventPrefix + " preLayoutVisualMask active=" + preMasked +
                        " maskedViews=" + masked +
                        " fallbackVisual=" +
                        if (preMasked) {
                            "outgoing-guiyuan-or-masked-native-until-layout"
                        } else {
                            "native-until-native-layout"
                        },
                )
                return masked
            }

            compactLayoutReady = true
            return refreshClipMasks()
        }

        fun commitDeferredNativeLayoutOwnership(): Int? {
            if (
                !active ||
                !started ||
                !nativeLayoutOwnershipDeferred
            ) {
                return null
            }
            val group =
                statusIcons.get()
                    ?: run {
                        onFailNative("status-icon-group-released")
                        return null
                    }
            nativeLayoutOwnershipDeferred = false
            if (!applyPersistentIgnoredSlots(group)) return null
            if (!syncEndReservation()) return null

            val masked = refreshClipMasks()
            if (
                VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                    deferVisualMaskUntilLayout = deferVisualMaskUntilLayout,
                    laidOut = group.isLaidOut,
                    layoutRequested = group.isLayoutRequested,
                    capacityLeaseAwaitingLayout =
                        capacityLeaseAwaitingLayout,
                    width = group.width,
                    height = group.height,
                )
            ) {
                compactLayoutReady = true
                layoutReadyCallback = null
                onEvent(
                    eventPrefix +
                        " deferredLayoutCommit state=ready" +
                        " source=existing-native-status-icons-layout" +
                        " maskedViews=" + masked,
                )
                return masked
            }

            compactLayoutReady = false
            onEvent(
                eventPrefix +
                    " deferredLayoutCommit state=pending" +
                    " maskedViews=" + masked +
                    " next=native-status-icons-onLayout",
            )
            return masked
        }

        fun releaseAodBatteryMask() {
            if (!active || !started || aodBatteryReleased) return
            val nativeBattery = battery.get()
                ?: run {
                    onFailNative("aod-battery-view-released")
                    return
                }
            val clip = clipStates.firstOrNull { it.view.get() === nativeBattery }
                ?: run {
                    onFailNative("aod-battery-mask-missing")
                    return
                }
            if (!restoreClipState(clip)) {
                onFailNative("aod-battery-mask-restore-failed")
                return
            }
            clipStates.remove(clip)
            aodBatteryReleased = true
            onEvent(eventPrefix + " battery=native owner=original-aod-mode-exit")
        }

        fun onAodIconTransition(toLockScreen: Boolean) {
            if (!active || !started) return
            if (toLockScreen) {
                if (aodPeersReleased) return
                aodPeersReleased = true
                if (!restoreAodPeerClips()) {
                    onFailNative("aod-peer-clip-restore-incomplete")
                    return
                }
                // Restore native peer layout with its visibility, not at session cleanup.
                if (!restoreEndReservation()) {
                    onFailNative("aod-peer-reservation-restore-failed")
                    return
                }
                if (!restorePersistentIgnoredSlots(requestLayout = true)) {
                    onFailNative("aod-peer-restore-failed")
                    return
                }
                onEvent(eventPrefix + " peers=original-icons owner=native-keyguard-animation")
            } else {
                if (!aodPeersReleased && !aodBatteryReleased) return
                val reclaimPeers = aodPeersReleased
                val group = statusIcons.get()
                    ?: run {
                        onFailNative("aod-peer-status-group-released")
                        return
                    }
                aodPeersReleased = false
                aodBatteryReleased = false
                if (reclaimPeers && !syncEndReservation()) return
                refreshClipMasks()
                if (reclaimPeers && !applyPersistentIgnoredSlots(group)) return
                onEvent(eventPrefix + " peers=guiyuan-aod owner=representation")
            }
        }

        private fun restoreAodPeerClips(): Boolean {
            val batteryView = battery.get() ?: return false
            val iterator = clipStates.iterator()
            while (iterator.hasNext()) {
                val state = iterator.next()
                val view = state.view.get()
                if (view !== batteryView) {
                    // A changed native clip is no longer ours; keep a failed restore receipt.
                    if (
                        view != null &&
                        view.clipBounds == state.appliedClip &&
                        !restoreClipState(state)
                    ) {
                        return false
                    }
                    iterator.remove()
                }
            }
            return true
        }

        fun stop(
            source: String,
            requestLayout: Boolean = true,
        ): Int {
            if (
                !active &&
                clipStates.isEmpty() &&
                mirroredPeerClipStates.isEmpty() &&
                appliedPadding == null &&
                appliedFakeCarrierWidthPx == null
            ) {
                return 0
            }
            lifecycle = Lifecycle.STOPPED
            aodPeersReleased = false
            aodBatteryReleased = false
            layoutReadyCallback = null
            compactLayoutReady = false
            nativeLayoutOwnershipDeferred = false
            leasePhase = LeasePhase.PREARM
            pendingNativeCarrierWidth = null
            deferVisualMaskUntilLayout = false

            val failures = ArrayList<Throwable>(1)
            fun release(action: () -> Unit) {
                try {
                    action()
                } catch (error: Throwable) {
                    rethrowFatal(error)
                    failures.add(error)
                }
            }
            release { host.get()?.removeOnAttachStateChangeListener(this) }
            release { battery.get()?.removeOnLayoutChangeListener(batteryLayoutListener) }
            release { batteryCarrier.get()?.removeOnLayoutChangeListener(carrierLayoutListener) }

            var reservationRestored = false
            var ignoredSlotsRestored = false
            var restored = 0
            release { reservationRestored = restoreEndReservation() }
            release { ignoredSlotsRestored = restorePersistentIgnoredSlots(requestLayout) }
            release { restored += restoreClipMasks(clipStates, failures) }
            release { restored += restoreClipMasks(mirroredPeerClipStates, failures) }

            val explicitLayoutRequest =
                requestLayout && ignoreScope == IgnoreScope.CALL
            if (explicitLayoutRequest) {
                release { batteryContainer.get()?.requestLayout() }
            }
            release {
                onEvent(
                    eventPrefix + " cleanup source=" + source +
                        " restoredClipBounds=" + restored +
                        " restoredEndReservation=" + reservationRestored +
                        " restoredIgnoredSlots=" + ignoredSlotsRestored +
                        " requestLayout=" + explicitLayoutRequest,
                )
            }
            failures.firstOrNull()?.let { error ->
                failures.drop(1).forEach(error::addSuppressed)
                throw error
            }
            return restored
        }

        fun <T> withRepresentedSlotsIgnored(block: () -> T): T {
            if (!active || ignoreScope == IgnoreScope.SESSION) {
                return block()
            }
            fun failAndProceed(reason: String): T {
                try {
                    onFailNative(reason)
                } catch (error: Throwable) {
                    rethrowFatal(error)
                    emitDiagnostic {
                        onEvent(eventPrefix + " failNative callbackFailure=" + error.javaClass.name)
                    }
                }
                // Failed cleanup must not replace the original SystemUI call.
                return block()
            }

            val container = statusIcons.get()
            if (container == null) {
                return failAndProceed("status-icon-group-released")
            }
            @Suppress("UNCHECKED_CAST")
            val list =
                readOrNull {
                    ignoredSlotsField.get(container) as? MutableList<String>
                }
            if (list == null) {
                return failAndProceed("ignored-slots-list-unavailable")
            }

            val owned =
                try {
                    OwnedEntries.add(list, representedSlots)
                } catch (error: Throwable) {
                    rethrowFatal(error)
                    try {
                        onFailNative(
                            "ignored-slots-add-" +
                                (error.message ?: error.javaClass.simpleName),
                        )
                    } catch (failure: Throwable) {
                        rethrowFatal(failure)
                        if (failure !== error) error.addSuppressed(failure)
                    }
                    return block()
                }

            var nativeFailure: Throwable? = null
            return try {
                block()
            } catch (error: Throwable) {
                nativeFailure = error
                throw error
            } finally {
                try {
                    OwnedEntries.restore(list, owned)
                } catch (error: Throwable) {
                    rethrowFatal(error)
                    try {
                        onFailNative(
                            "ignored-slots-restore-" +
                                (error.message ?: error.javaClass.simpleName),
                        )
                    } catch (failure: Throwable) {
                        rethrowFatal(failure)
                        if (failure !== error) error.addSuppressed(failure)
                    }
                    // A failed cleanup must not replace the native method's exception.
                    if (nativeFailure !== error) nativeFailure?.addSuppressed(error)
                }
            }
        }

        private fun applyPersistentIgnoredSlots(group: ViewGroup): Boolean {
            if (
                ignoreScope != IgnoreScope.SESSION ||
                persistentIgnoredSlotsApplied
            ) {
                return true
            }
            val addMethod =
                addIgnoredSlotsMethod
                    ?: run {
                        onFailNative("add-ignored-slots-method-unavailable")
                        return false
                    }
            @Suppress("UNCHECKED_CAST")
            val live =
                readOrNull {
                    ignoredSlotsField.get(group) as? MutableList<String>
                }
                    ?: run {
                        onFailNative("ignored-slots-list-unavailable")
                        return false
                    }
            val before = live.toList()
            val owned =
                OwnedEntries.delta(
                    existing = before,
                    requested = representedSlots,
                )
            val applied =
                runCatching {
                    if (owned.isNotEmpty()) {
                        addMethod.invoke(group, ArrayList(owned))
                    }
                    @Suppress("UNCHECKED_CAST")
                    val after =
                        ignoredSlotsField.get(group) as? MutableList<String>
                            ?: error("ignored-slots-list-unavailable-after-add")
                    check(representedSlots.all(after::contains)) {
                        "ignored-slots-native-api-did-not-retain-represented-slots"
                    }
                }
            if (applied.isFailure) {
                applied.exceptionOrNull()?.let(::rethrowFatal)
                val rollback =
                    setIgnoredSlotsMethod?.let { setMethod ->
                        runCatching {
                            @Suppress("UNCHECKED_CAST")
                            val current =
                                ignoredSlotsField.get(group) as? MutableList<String>
                                    ?: error("ignored-slots-list-unavailable-during-rollback")
                            // Native callbacks may add unrelated slots before the failed add returns.
                            val target = OwnedEntries.restoreTarget(current, owned)
                            if (target != current) {
                                setMethod.invoke(group, ArrayList(target))
                            }
                            @Suppress("UNCHECKED_CAST")
                            val restored =
                                ignoredSlotsField.get(group) as? MutableList<String>
                                    ?: error("ignored-slots-list-unavailable-after-rollback")
                            restored == target
                        }.onFailure(::rethrowFatal).getOrDefault(false)
                    } ?: false
                onFailNative(
                    "ignored-slots-session-add-" +
                        (
                            applied.exceptionOrNull()?.message
                                ?: applied.exceptionOrNull()?.javaClass?.simpleName
                                ?: "unknown"
                        ) +
                        "-rollback=" + rollback,
                )
                return false
            }
            ownedPersistentIgnoredSlots = owned
            persistentIgnoredSlotsApplied = true
            onEvent(
                eventPrefix + " ignoredSlots active lifetime=presentation-session" +
                    " owned=" + owned.joinToString(",") +
                    " represented=" + representedSlots.joinToString(",") +
                    " nativeApi=addIgnoredSlots",
            )
            return true
        }

        private fun restorePersistentIgnoredSlots(
            requestLayout: Boolean,
        ): Boolean {
            if (
                ignoreScope != IgnoreScope.SESSION ||
                !persistentIgnoredSlotsApplied
            ) {
                return true
            }
            val owned = ownedPersistentIgnoredSlots
            if (owned.isEmpty()) {
                persistentIgnoredSlotsApplied = false
                ownedPersistentIgnoredSlots = emptyList()
                return true
            }
            val group = statusIcons.get() ?: return false
            @Suppress("UNCHECKED_CAST")
            val live =
                readOrNull {
                    ignoredSlotsField.get(group) as? MutableList<String>
                } ?: return false
            val target =
                OwnedEntries.restoreTarget(
                    live = live,
                    ownedEntries = owned,
                )
            val useNativeSetter = requestLayout
            val setMethod =
                if (useNativeSetter) {
                    setIgnoredSlotsMethod ?: return false
                } else {
                    null
                }
            val restored =
                runCatching {
                    if (target != live) {
                        if (useNativeSetter) {
                            setMethod!!.invoke(group, ArrayList(target))
                        } else {
                            live.clear()
                            live.addAll(target)
                        }
                    }
                    @Suppress("UNCHECKED_CAST")
                    val after =
                        ignoredSlotsField.get(group) as? MutableList<String>
                            ?: return@runCatching false
                    after == target
                }.getOrElse { error ->
                    rethrowFatal(error)
                    false
                }
            if (restored) {
                persistentIgnoredSlotsApplied = false
                ownedPersistentIgnoredSlots = emptyList()
                if (!useNativeSetter) {
                    onEvent(
                        eventPrefix +
                            " ignoredSlots restore=handoff-no-layout owned=" +
                            owned.joinToString(",") +
                            " nativeApi=owned-list-delta",
                    )
                }
            } else {
                onEvent(
                    eventPrefix +
                        " ignoredSlots restore=failed owned=" + owned.joinToString(",") +
                        " requestLayout=" + requestLayout,
                )
            }
            return restored
        }

        fun onCcVisibilityChanged(visible: Boolean): Boolean {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return true
            if (visible) {
                if (leasePhase == LeasePhase.VISIBLE) {
                    return true
                }
                leasePhase = LeasePhase.PREARM

                // The first visible edge is the ownership handoff from hidden/prearm
                // to the visible QS_FAKE cycle. Reconcile while hidden ownership is
                // still in force so a native hidden relayout can be adopted instead
                // of being misclassified as a visible writer conflict.
                if (!syncEndReservation()) {
                    return false
                }
                leasePhase = LeasePhase.VISIBLE
                return true
            }

            if (leasePhase == LeasePhase.SUPPRESSED) return true

            // Close the transition reservation while native layout writes are
            // still allowed. Only then suppress/release the capacity lease.
            // This keeps abrupt visible=false edges from retaining expanded
            // peer padding if HyperOS skips a fraction=0 sample.
            if (!clearTransitionReservation("visible-cycle-hidden")) {
                return false
            }
            leasePhase = LeasePhase.SUPPRESSED
            return releaseCapacityLeaseAtHidden()
        }

        fun updateTransitionReservation(
            requestedSlotWidthPx: Int,
        ): Boolean {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return false
            val normalized = requestedSlotWidthPx.coerceAtLeast(0)
            if (transitionRequestedSlotWidthPx == normalized) return true
            transitionRequestedSlotWidthPx = normalized
            return syncEndReservation()
        }

        fun clearTransitionReservation(source: String): Boolean {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return true
            if (transitionRequestedSlotWidthPx == null) return true
            transitionRequestedSlotWidthPx = null
            val restored = syncEndReservation()
            onEvent(
                eventPrefix +
                    " transitionReservation cleared source=" + source +
                    " restoredCompact=" + restored,
            )
            return restored
        }

        private fun measuredNativePeerWidth(group: ViewGroup): Int? {
            val (viewsField, spacingField) = fakePeerMetrics ?: return null
            val views = readOrNull { viewsField.get(group) as? List<*> } ?: return null
            val spacing = readOrNull { spacingField.getInt(group) }
                ?: return null
            if (spacing < 0 || views.size > group.childCount) return null

            // Match the native onMeasure inputs, including its ignored-slot filter.
            var width = group.paddingStart.toLong()
            views.forEachIndexed { index, item ->
                val view = item as? View ?: return null
                if (view.parent !== group || view.measuredWidth < 0) return null
                val measured =
                    view.measuredWidth.toLong() + view.paddingStart + view.paddingEnd
                if (measured < 0L) return null
                if (measured > 0L) {
                    width += measured
                    if (index > 0) width += spacing
                }
            }
            return width.takeIf {
                it >= 0L && it <= Int.MAX_VALUE.toLong()
            }?.toInt()
        }

        fun syncEndReservation(): Boolean {
            if (!active || aodPeersReleased) return true
            if (nativeLayoutOwnershipDeferred) {
                return true
            }
            if (
                surfaceName == CONTROL_CENTER_FAKE_SURFACE &&
                leasePhase == LeasePhase.SUPPRESSED
            ) {
                return true
            }
            val group = statusIcons.get() ?: run { onFailNative("status-icon-group-released"); return false }
            val container = batteryContainer.get() ?: run { onFailNative("battery-container-released"); return false }
            val batteryView = battery.get() ?: run { onFailNative("battery-view-released"); return false }
            val hostView = host.get() ?: run { onFailNative(surfaceName + "-host-released"); return false }
            val baseline = nativePadding ?: PaddingState.from(group).also { nativePadding = it }
            val live = PaddingState.from(group)
            val previousApplied = appliedPadding
            if (live != baseline && live != previousApplied) {
                onFailNative("status-icon-padding-writer-conflict")
                return false
            }
            val nativeHide =
                readOrNull { batteryHideField.getBoolean(container) }
                    ?: run { onFailNative("battery-hide-state-unavailable"); return false }
            val actualBatteryWidthPx =
                (if (batteryView.measuredWidth > 0) batteryView.measuredWidth else batteryView.width)
                    .takeIf { width -> width > 0 }
                    ?: run {
                        if (
                            retainReservationOnWidthLoss &&
                            compactLayoutReady
                        ) {
                            if (!batteryWidthUnavailable) {
                                batteryWidthUnavailable = true
                                onEvent(
                                    eventPrefix +
                                        " endReservation deferred reason=battery-live-width-unavailable",
                                )
                            }
                            return true
                        }
                        onFailNative("battery-live-width-unavailable")
                        return false
                    }
            if (batteryWidthUnavailable) {
                batteryWidthUnavailable = false
                onEvent(
                    eventPrefix +
                        " endReservation resumed reason=battery-live-width-restored",
                )
            }
            val carrier =
                batteryCarrier.get()
                    ?: run { onFailNative("battery-core-carrier-released"); return false }
            val stableCarrierWidthPx =
                SysUiCarrierMetrics.resolveWidthPx(carrier)
                    ?: run { onFailNative("battery-core-width-unavailable"); return false }
            if (actualBatteryWidthPx < stableCarrierWidthPx) {
                onFailNative("battery-presentation-narrower-than-core")
                return false
            }
            val resolved =
                SteadyLayoutResolver.resolve(
                    hostWidthPx = hostView.width,
                    hostHeightPx = hostView.height,
                    baseCarrierWidthPx = stableCarrierWidthPx,
                    isRtl = hostView.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                ) ?: run { onFailNative(surfaceName + "-layout-unavailable"); return false }
            val compactSlotWidthPx =
                CompactReservationPolicy.resolveCenteredVisualWidth(
                    baseSlotWidthPx = resolved.visualWidth,
                    userScale = VisualPrefsOwner.current().combinedScale,
                )
            val requestedSlotWidthPx =
                EndReservationPolicy.resolveRequestedSlotWidth(
                    compactSlotWidthPx = compactSlotWidthPx,
                    transitionRequestedSlotWidthPx = transitionRequestedSlotWidthPx,
                )
            val requestedReservationDelta =
                EndReservationPolicy.resolvePaddingEndDelta(
                    nativeHide = nativeHide,
                    actualBatteryWidthPx = actualBatteryWidthPx,
                    requestedSlotWidthPx = requestedSlotWidthPx,
                )
            val capacityDeltaPx =
                ensureFakeCarrierCapacityLease(hostView)
                    ?: return false
            if (surfaceName == CONTROL_CENTER_FAKE_SURFACE) {
                val capacityBound =
                    EndReservationPolicy.resolveCapacityBoundedReservationDelta(
                        nativeHide = nativeHide,
                        compactSlotWidthPx = compactSlotWidthPx,
                        requestedReservationDeltaPx = requestedReservationDelta,
                        capacityDeltaPx = capacityDeltaPx,
                    )
                if (requestedReservationDelta > capacityBound) {
                    // A fake carrier's width lease does not bound the icon row's
                    // end padding when the native measured peers still fit.
                    val contentWidth =
                        measuredNativePeerWidth(group)
                            ?: run {
                                onFailNative("fake-native-peer-measure-unavailable")
                                return false
                            }
                    if (
                        !EndReservationPolicy.fitsFakeNativePeerRow(
                            rowWidthPx = group.width,
                            contentWidthPx = contentWidth,
                            targetPaddingEndPx = baseline.end + requestedReservationDelta,
                        )
                    ) {
                        onFailNative("fake-native-peer-row-overflow")
                        return false
                    }
                }
            }
            val reservationDelta = requestedReservationDelta
            val capacityReservationDeltaPx =
                EndReservationPolicy.resolveFakeCarrierCapacityRequirement(
                    nativeHide = nativeHide,
                    compactSlotWidthPx = compactSlotWidthPx,
                    reservationDeltaPx = reservationDelta,
                )
            val target =
                PaddingState(
                    baseline.start,
                    baseline.top,
                    baseline.end + reservationDelta,
                    baseline.bottom,
                )
            if (live != target) {
                group.setPaddingRelative(target.start, target.top, target.end, target.bottom)
            }
            if (PaddingState.from(group) != target) {
                onFailNative("status-icon-end-reservation-apply-failed")
                return false
            }
            appliedPadding = if (target == baseline) null else target
            if (lastReservationDelta != reservationDelta) {
                lastReservationDelta = reservationDelta
                val shouldReport =
                    surfaceName != CONTROL_CENTER_FAKE_SURFACE ||
                        (
                            isDetailedDiagnosticsEnabled() &&
                                transitionRequestedSlotWidthPx == null
                        )
                if (shouldReport) {
                    onEvent(
                        eventPrefix + " endReservation nativeHide=" + nativeHide +
                            " stableCarrierWidth=" + stableCarrierWidthPx +
                            " actualBatteryWidth=" + actualBatteryWidthPx +
                            " compactSlotWidth=" + compactSlotWidthPx +
                            " visualScale=" + VisualPrefsOwner.current().combinedScale +
                            " requestedSlotWidth=" + requestedSlotWidthPx +
                            " transitionRequestedSlotWidth=" +
                            (transitionRequestedSlotWidthPx ?: -1) +
                            " requestedPaddingEndDelta=" + requestedReservationDelta +
                            " paddingEndDelta=" + reservationDelta +
                            " capacityClamped=" + (reservationDelta != requestedReservationDelta) +
                            " basePaddingEnd=" + baseline.end +
                            " appliedPaddingEnd=" + target.end +
                            " fakeCarrierWidth=" + (appliedFakeCarrierWidthPx ?: -1) +
                            " fakeCarrierCapacityDelta=" + capacityDeltaPx +
                            " fakeCarrierCapacityRequired=" + capacityReservationDeltaPx +
                            " carrierAuthority=battery_icon_container " +
                            "owner=qs-fake-capacity-lease+statusIcons-paddingEnd",
                    )
                }
            }
            return true
        }

        private fun restoreEndReservation(): Boolean {
            var paddingRestored = appliedPadding == null
            val applied = appliedPadding
            val paddingFailure = runCatching {
                if (applied != null) {
                    val group = statusIcons.get()
                    val baseline = nativePadding
                    if (group == null || baseline == null) {
                        paddingRestored = false
                    } else {
                        val live = PaddingState.from(group)
                        if (live != applied) {
                            paddingRestored = false
                            onEvent(
                                eventPrefix + " endReservation restore=skipped reason=writer-changed " +
                                    "livePaddingEnd=" + live.end +
                                    " appliedPaddingEnd=" + applied.end,
                            )
                        } else {
                            group.setPaddingRelative(
                                baseline.start,
                                baseline.top,
                                baseline.end,
                                baseline.bottom,
                            )
                            paddingRestored = PaddingState.from(group) == baseline
                        }
                    }
                }
            }.exceptionOrNull()
            paddingFailure?.let(::rethrowFatal)
            appliedPadding = null
            val carrier = runCatching { restoreFakeCarrierCapacityLease() }
            carrier.exceptionOrNull()?.let(::rethrowFatal)
            if (paddingFailure != null) {
                carrier.exceptionOrNull()?.let(paddingFailure::addSuppressed)
                throw paddingFailure
            }
            val carrierRestored = carrier.getOrThrow()
            return paddingRestored && carrierRestored
        }

        private fun ensureFakeCarrierCapacityLease(hostView: ViewGroup): Int? {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return 0

            val parent =
                hostView.parent as? ViewGroup
                    ?: run {
                        onFailNative("fake-carrier-parent-unavailable")
                        return null
                    }
            if (parent.childCount != 1 || parent.getChildAt(0) !== hostView) {
                onFailNative("fake-carrier-exclusive-parent-contract-unavailable")
                return null
            }
            val params =
                hostView.layoutParams
                    ?: run {
                        onFailNative("fake-carrier-layout-params-unavailable")
                        return null
                    }
            val margins = params as? ViewGroup.MarginLayoutParams
            if (
                margins != null &&
                (
                    margins.marginStart != 0 ||
                        margins.marginEnd != 0 ||
                        margins.leftMargin != 0 ||
                        margins.rightMargin != 0
                )
            ) {
                onFailNative("fake-carrier-horizontal-margin-contract-unsupported")
                return null
            }

            val parentContentWidthPx =
                (
                    parent.width -
                        parent.paddingLeft -
                        parent.paddingRight
                ).takeIf { width -> width > 0 }
                    ?: run {
                        onFailNative("fake-carrier-parent-width-unavailable")
                        return null
                    }
            if (!isFakeCarrierEndAnchored(hostView, parent)) {
                onFailNative("fake-carrier-end-anchor-unverified")
                return null
            }

            val existingAppliedWidthPx = appliedFakeCarrierWidthPx
            if (existingAppliedWidthPx != null) {
                when (
                    FakeCarrierCapacityLeasePolicy.resolveExistingLeaseAction(
                        visibleCycleActive = leasePhase == LeasePhase.VISIBLE,
                        liveWidthPx = params.width,
                        appliedWidthPx = existingAppliedWidthPx,
                        currentParentContentWidthPx = parentContentWidthPx,
                        leasedParentContentWidthPx = fakeCarrierParentWidth,
                    )
                ) {
                    FakeCarrierCapacityLeasePolicy.ExistingLeaseAction.REUSE ->
                        return fakeCarrierCapacityDeltaPx

                    FakeCarrierCapacityLeasePolicy.ExistingLeaseAction.ADOPT_HIDDEN_NATIVE -> {
                        val adoptedNativeWidthPx = params.width
                        val previousNativeWidthPx = nativeFakeCarrierLayoutWidthPx
                        clearCapacityLeaseSnapshot()
                        pendingNativeCarrierWidth = adoptedNativeWidthPx
                        onEvent(
                            eventPrefix +
                                " fakeCarrierCapacity lease=adopt-native-hidden-prearm" +
                                " liveWidth=" + adoptedNativeWidthPx +
                                " appliedWidth=" + existingAppliedWidthPx +
                                " previousNativeWidth=" + (previousNativeWidthPx ?: -1) +
                                " owner=control-center-fake-session",
                        )
                    }

                    FakeCarrierCapacityLeasePolicy.ExistingLeaseAction.FAIL_WRITER_CONFLICT -> {
                        onFailNative("fake-carrier-width-writer-conflict")
                        return null
                    }
                }
            }

            val pendingNativeBaselineWidthPx =
                pendingNativeCarrierWidth
            val baselineWidthPx =
                when {
                    pendingNativeBaselineWidthPx != null &&
                        params.width == pendingNativeBaselineWidthPx ->
                        pendingNativeBaselineWidthPx

                    hostView.width > 0 &&
                        params.width == hostView.width ->
                        hostView.width

                    else -> {
                        onFailNative("fake-carrier-width-contract-unavailable")
                        return null
                    }
                }
            if (baselineWidthPx <= 0) {
                onFailNative("fake-carrier-width-unavailable")
                return null
            }
            val capacityDeltaPx =
                EndReservationPolicy.resolveFakeCarrierCapacityDelta(
                    nativeCarrierWidthPx = baselineWidthPx,
                    parentContentWidthPx = parentContentWidthPx,
                ) ?: run {
                    onFailNative("fake-carrier-capacity-unavailable")
                    return null
                }

            nativeFakeCarrierLayoutWidthPx = baselineWidthPx
            fakeCarrierParentWidth = parentContentWidthPx
            fakeCarrierCapacityDeltaPx = capacityDeltaPx
            pendingNativeCarrierWidth = null

            if (params.width != parentContentWidthPx) {
                params.width = parentContentWidthPx
                hostView.layoutParams = params
                capacityLeaseAwaitingLayout = true
            }
            if (hostView.layoutParams?.width != parentContentWidthPx) {
                clearCapacityLeaseSnapshot()
                onFailNative("fake-carrier-capacity-lease-apply-failed")
                return null
            }
            appliedFakeCarrierWidthPx = parentContentWidthPx
            onEvent(
                eventPrefix +
                    " fakeCarrierCapacity lease=active" +
                    " nativeWidth=" + baselineWidthPx +
                    " leasedWidth=" + parentContentWidthPx +
                    " capacityDelta=" + capacityDeltaPx +
                    " owner=control-center-fake-session",
            )
            return capacityDeltaPx
        }

        private fun isFakeCarrierEndAnchored(
            hostView: View,
            parent: ViewGroup,
        ): Boolean {
            val contentLeft = parent.paddingLeft
            val contentRight = parent.width - parent.paddingRight
            if (contentRight <= contentLeft) return false
            return if (hostView.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                hostView.left == contentLeft
            } else {
                hostView.right == contentRight
            }
        }

        private fun releaseCapacityLeaseAtHidden(): Boolean {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return true
            val appliedWidthPx = appliedFakeCarrierWidthPx
            if (appliedWidthPx == null) {
                clearCapacityLeaseSnapshot()
                return true
            }
            val hostView = host.get()
            val baselineLayoutWidthPx = nativeFakeCarrierLayoutWidthPx
            val params = hostView?.layoutParams
            if (hostView == null || baselineLayoutWidthPx == null || params == null) {
                clearCapacityLeaseSnapshot()
                return false
            }

            val liveWidthPx = params.width
            var nextNativeBaselineWidthPx = liveWidthPx
            val restored =
                when (liveWidthPx) {
                    appliedWidthPx -> {
                        params.width = baselineLayoutWidthPx
                        hostView.layoutParams = params
                        nextNativeBaselineWidthPx = baselineLayoutWidthPx
                        hostView.layoutParams?.width == baselineLayoutWidthPx
                    }

                    baselineLayoutWidthPx -> true

                    else -> {
                        // The native hidden boundary may already have replaced
                        // the fake carrier geometry. Ownership ends here; do
                        // not race that writer. The next visible cycle will
                        // establish a fresh baseline from the live native View.
                        onEvent(
                            eventPrefix +
                                " fakeCarrierCapacity release=adopt-native-hidden-width" +
                                " liveWidth=" + liveWidthPx +
                                " appliedWidth=" + appliedWidthPx +
                                " baselineWidth=" + baselineLayoutWidthPx,
                        )
                        true
                    }
                }
            clearCapacityLeaseSnapshot()
            pendingNativeCarrierWidth =
                nextNativeBaselineWidthPx.takeIf { width -> width > 0 }
            onEvent(
                eventPrefix +
                    " fakeCarrierCapacity lease=hidden-released" +
                    " restored=" + restored +
                    " nextNativeBaselineWidth=" +
                    (pendingNativeCarrierWidth ?: -1) +
                    " owner=control-center-fake-visible-cycle",
            )
            return restored
        }

        private fun restoreFakeCarrierCapacityLease(): Boolean {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return true

            val appliedWidthPx = appliedFakeCarrierWidthPx
            if (appliedWidthPx == null) {
                clearCapacityLeaseSnapshot()
                return true
            }
            val hostView = host.get()
            val baselineLayoutWidthPx = nativeFakeCarrierLayoutWidthPx
            if (hostView == null || baselineLayoutWidthPx == null) {
                clearCapacityLeaseSnapshot()
                return false
            }
            val params = hostView.layoutParams
            if (params == null) {
                clearCapacityLeaseSnapshot()
                return false
            }
            if (params.width != appliedWidthPx) {
                try {
                    onEvent(
                        eventPrefix +
                            " fakeCarrierCapacity restore=skipped reason=writer-changed" +
                            " liveWidth=" + params.width +
                            " appliedWidth=" + appliedWidthPx,
                    )
                } finally {
                    clearCapacityLeaseSnapshot()
                }
                return false
            }

            return try {
                params.width = baselineLayoutWidthPx
                hostView.layoutParams = params
                hostView.layoutParams?.width == baselineLayoutWidthPx
            } finally {
                clearCapacityLeaseSnapshot()
                pendingNativeCarrierWidth = null
            }
        }

        private fun clearCapacityLeaseSnapshot() {
            firstLeaseWidthMismatch = null
            nativeFakeCarrierLayoutWidthPx = null
            fakeCarrierParentWidth = null
            appliedFakeCarrierWidthPx = null
            fakeCarrierCapacityDeltaPx = null
            capacityLeaseAwaitingLayout = false
        }

        fun isLayoutCutoverReady(): Boolean =
            active &&
                started &&
                (!deferVisualMaskUntilLayout || compactLayoutReady)

        fun adoptTransferredCompactLayout(): Int? {
            if (
                !active ||
                !started ||
                !deferVisualMaskUntilLayout
            ) {
                return null
            }
            if (!syncEndReservation()) {
                return null
            }
            if (capacityLeaseAwaitingLayout) {
                return null
            }
            val masked = refreshClipMasks()
            if (compactLayoutReady) {
                return masked
            }
            compactLayoutReady = true
            val callback = layoutReadyCallback
            layoutReadyCallback = null
            onEvent(
                eventPrefix + " layoutReady source=hot-reload-transfer" +
                    " maskedViews=" + masked,
            )
            callback?.invoke(masked)
            return masked
        }

        fun traceFakeCarrierWidth(boundary: String) {
            if (
                surfaceName != CONTROL_CENTER_FAKE_SURFACE ||
                !capacityLeaseAwaitingLayout ||
                firstLeaseWidthMismatch != null
            ) return
            val hostView = host.get() ?: return
            val appliedWidth = appliedFakeCarrierWidthPx ?: return
            val liveWidth = hostView.layoutParams?.width ?: return
            if (liveWidth == appliedWidth) return

            firstLeaseWidthMismatch = boundary
            emitDiagnostic {
                val parent = hostView.parent as? ViewGroup
                onEvent(
                    eventPrefix + " fakeCarrierCapacity boundary=width-mismatch" +
                        " firstObserved=" + boundary +
                        " liveWidth=" + liveWidth +
                        " appliedWidth=" + appliedWidth +
                        " nativeWidth=" + nativeFakeCarrierLayoutWidthPx +
                        " viewWidth=" + hostView.width +
                        " measuredWidth=" + hostView.measuredWidth +
                        " layoutRequested=" + hostView.isLayoutRequested +
                        " parentContentWidth=" +
                        (parent?.let { it.width - it.paddingLeft - it.paddingRight } ?: "unavailable") +
                        " endAnchored=" +
                        (parent?.let { isFakeCarrierEndAnchored(hostView, it) } ?: "unavailable") +
                        " phase=" + leasePhase.name,
                )
            }
        }

        fun validateNativeLayoutBeforeVisualMask(): Boolean {
            if (!active || !started) return false
            if (surfaceName == CONTROL_CENTER_FAKE_SURFACE && appliedFakeCarrierWidthPx == null) {
                return false
            }
            if (!capacityLeaseAwaitingLayout) return true

            val hostView =
                host.get()
                    ?: run {
                        onFailNative("fake-carrier-post-lease-host-released")
                        return false
                    }
            val parent =
                hostView.parent as? ViewGroup
                    ?: run {
                        onFailNative("fake-carrier-post-lease-parent-unavailable")
                        return false
                    }
            val expectedWidthPx =
                appliedFakeCarrierWidthPx
                    ?: run {
                        onFailNative("fake-carrier-post-lease-state-invalid")
                        return false
                    }
            val liveWidthPx = hostView.layoutParams?.width
            val endAnchored = isFakeCarrierEndAnchored(hostView, parent)
            if (liveWidthPx != expectedWidthPx || !endAnchored) {
                val parentWidthPx =
                    parent.width - parent.paddingLeft - parent.paddingRight
                if (
                    leasePhase == LeasePhase.PREARM &&
                    endAnchored &&
                    liveWidthPx != null &&
                    FakeCarrierCapacityLeasePolicy.resolveExistingLeaseAction(
                        visibleCycleActive = false,
                        liveWidthPx = liveWidthPx,
                        appliedWidthPx = expectedWidthPx,
                        currentParentContentWidthPx = parentWidthPx,
                        leasedParentContentWidthPx = fakeCarrierParentWidth,
                    ) == FakeCarrierCapacityLeasePolicy.ExistingLeaseAction.ADOPT_HIDDEN_NATIVE
                ) {
                    // Native changed its hidden carrier; the next visible edge must lease afresh.
                    clearCapacityLeaseSnapshot()
                    pendingNativeCarrierWidth = liveWidthPx
                    onEvent(
                        eventPrefix + " fakeCarrierCapacity lease=yield-hidden-prearm" +
                            " liveWidth=" + liveWidthPx +
                            " next=visible-cycle",
                    )
                    return false
                }
                onFailNative("fake-carrier-post-lease-layout-invalid")
                return false
            }
            return true
        }

        fun onNativeLayoutCompleted(maskedViews: Int) {
            if (
                !active ||
                !started ||
                !deferVisualMaskUntilLayout ||
                compactLayoutReady ||
                nativeLayoutOwnershipDeferred
            ) {
                return
            }
            capacityLeaseAwaitingLayout = false
            compactLayoutReady = true
            val callback = layoutReadyCallback
            layoutReadyCallback = null
            onEvent(
                eventPrefix + " layoutReady source=native-status-icons-onLayout" +
                    " maskedViews=" + maskedViews,
            )
            callback?.invoke(maskedViews)
        }

        fun captureSteadyPeerMirror(): SteadyPeerMirrorSnapshot {
            if (!active || surfaceName != HOME_SURFACE) {
                return SteadyPeerMirrorSnapshot(false, emptySet())
            }
            val islandShowing = SysUiIslandSource.currentShowing() == true
            if (!islandShowing) {
                return SteadyPeerMirrorSnapshot(false, emptySet())
            }
            val group = statusIcons.get()
                ?: return SteadyPeerMirrorSnapshot(false, emptySet())
            val hidden =
                buildSet {
                    for (index in 0 until group.childCount) {
                        val child = group.getChildAt(index)
                        val slot = NativeParticipantAccess.slotOf(child) ?: continue
                        if (slot in representedSlots) continue
                        val state =
                            NativeStatusObserver
                                .readIslandVisibilityState(group, child)
                                ?: continue
                        if (
                            SteadyPeerMirrorPolicy.isIslandHidden(
                                visibleState = state.visibleState,
                                inIslandState = state.inIslandState,
                            )
                        ) {
                            add(slot)
                        }
                    }
                }
            return SteadyPeerMirrorSnapshot(true, hidden)
        }

        fun updateSteadyPeerMirror(
            active: Boolean,
            hiddenSlots: Set<String>,
        ) {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return
            val normalized = if (active) hiddenSlots.toSet() else emptySet()
            steadyPeerMirrorActive = active
            steadyPeerMirrorHiddenSlots = normalized
            if (!active) {
                mirrorIslandSuppressionLogged = false
            }
            if (compactLayoutReady) {
                refreshMirroredPeerClipMasks()
            }
        }

        fun isSteadyPeerMirrorActive(): Boolean =
            active &&
                surfaceName == CONTROL_CENTER_FAKE_SURFACE &&
                steadyPeerMirrorActive

        fun reportSteadyPeerMirrorIslandSuppression() {
            if (mirrorIslandSuppressionLogged) return
            mirrorIslandSuppressionLogged = true
            onEvent(
                eventPrefix +
                    " steadyPeerMirror islandSuppressed" +
                    " authority=home-native-island-state",
            )
        }

        private fun refreshMirroredPeerClipMasks(): Int {
            if (!active || surfaceName != CONTROL_CENTER_FAKE_SURFACE || !compactLayoutReady) {
                return 0
            }
            val group = statusIcons.get() ?: return 0
            val targets = linkedSetOf<View>()
            if (steadyPeerMirrorActive) {
                for (index in 0 until group.childCount) {
                    val child = group.getChildAt(index)
                    val slot = NativeParticipantAccess.slotOf(child) ?: continue
                    if (slot !in representedSlots && slot in steadyPeerMirrorHiddenSlots) {
                        targets += child
                    }
                }
            }

            syncClipMasks(mirroredPeerClipStates, targets)
            return mirroredPeerClipStates.count { state -> state.view.get() != null }
        }
        fun refreshClipMasks(): Int {
            if (!active) {
                return 0
            }
            val group = statusIcons.get()
                ?: run {
                    onFailNative("status-icon-group-released")
                    return 0
                }
            val batteryView = battery.get()
                ?: run {
                    onFailNative("battery-view-released")
                    return 0
                }

            val targets = linkedSetOf<View>()
            if (!aodBatteryReleased) targets += batteryView
            if (!aodPeersReleased) {
                for (index in 0 until group.childCount) {
                    val child = group.getChildAt(index)
                    if (NativeParticipantAccess.slotOf(child) in representedSlots) {
                        targets += child
                    }
                }
            }

            syncClipMasks(clipStates, targets)
            refreshMirroredPeerClipMasks()
            return clipStates.count { state -> state.view.get() != null }
        }

        private fun syncClipMasks(
            states: MutableList<ClipState>,
            targets: Set<View>,
        ) {
            val iterator = states.iterator()
            while (iterator.hasNext()) {
                val state = iterator.next()
                val view = state.view.get()
                if (view == null || view !in targets) {
                    if (view != null && view.clipBounds == state.appliedClip) {
                        val restored = runCatching { restoreClipState(state) }
                            .onFailure(::rethrowFatal)
                            .getOrDefault(false)
                        if (!restored) {
                            onFailNative("native-clip-release-unverified")
                            return
                        }
                    }
                    iterator.remove()
                }
            }

            for (view in targets) {
                if (states.none { state -> state.view.get() === view }) {
                    val nativeClip = view.clipBounds?.let(::Rect)
                    val applied = Rect(0, 0, 0, 0)
                    // Own the rollback receipt before a setter can partially write.
                    states += ClipState(WeakReference(view), nativeClip, applied)
                    val appliedOk = runCatching {
                        view.clipBounds = applied
                        view.clipBounds == applied
                    }.onFailure(::rethrowFatal).getOrDefault(false)
                    if (!appliedOk) {
                        onFailNative("native-clip-apply-unverified")
                        return
                    }
                }
            }
        }

        override fun onViewAttachedToWindow(view: View) = Unit

        override fun onViewDetachedFromWindow(view: View) {
            if (active) {
                onFailNative(surfaceName + "-host-detached")
            }
        }

        private fun restoreClipMasks(
            states: MutableList<ClipState>,
            failures: MutableList<Throwable>,
        ): Int {
            val snapshot = states.toList()
            states.clear()
            var restored = 0
            snapshot.forEach { state ->
                try {
                    val view = state.view.get()
                    if (view != null && view.clipBounds == state.appliedClip) {
                        if (restoreClipState(state)) {
                            restored += 1
                        } else if (view.clipBounds == state.appliedClip) {
                            states += state
                            failures += IllegalStateException("native-clip-restore-incomplete")
                        }
                    }
                } catch (error: Throwable) {
                    rethrowFatal(error)
                    states += state
                    failures.add(error)
                }
            }
            return restored
        }
        private fun restoreClipState(state: ClipState): Boolean {
            val view = state.view.get() ?: return false
            if (view.clipBounds != state.appliedClip) {
                return false
            }
            view.clipBounds = state.nativeClip?.let(::Rect)
            return view.clipBounds == state.nativeClip
        }
    }

    private data class SteadyPeerMirrorSnapshot(
        val active: Boolean,
        val hiddenSlots: Set<String>,
    )

    private data class ClipState(
        val view: WeakReference<View>,
        val nativeClip: Rect?,
        val appliedClip: Rect,
    )

    private data class PaddingState(
        val start: Int,
        val top: Int,
        val end: Int,
        val bottom: Int,
    ) {
        companion object {
            fun from(view: View): PaddingState =
                PaddingState(view.paddingStart, view.paddingTop, view.paddingEnd, view.paddingBottom)
        }
    }

    private enum class IgnoreScope {
        CALL,
        SESSION,
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) {
                return child
            }
        }
        return null
    }

    internal sealed interface Result {
        data class Active(
            val representedSlots: Int,
            val maskedViews: Int,
            val reused: Boolean,
        ) : Result
        data class Prepared(
            val representedSlots: Int,
            val reused: Boolean,
        ) : Result
        data class Inactive(val restoredViews: Int) : Result
        data class Failure(
            val reason: String,
            val retryAfterLayout: Boolean = false,
        ) : Result
    }


    internal sealed interface LegacyCleanupResult {
        data object NotPresent : LegacyCleanupResult
        data object Removed : LegacyCleanupResult
        data class Failure(val reason: String) : LegacyCleanupResult
    }
}
