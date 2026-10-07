package com.chaners.guiyuan.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field

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
        if (installedHookCount == HOOK_COUNT) {
            eventSink = onEvent
            failNativeSink = onFailNative
            return null
        }
        if (installedHookCount != 0) {
            return "partial-hook-state"
        }

        val containerClass =
            runCatching {
                Class.forName(STATUS_ICON_CONTAINER, false, classLoader)
            }.getOrElse {
                return "status-icon-container-class-missing"
            }
        val field =
            generateSequence(containerClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { candidate ->
                        candidate.name == "ignoredSlots" &&
                            java.util.List::class.java.isAssignableFrom(candidate.type)
                    }
                }
                .firstOrNull()
                ?.apply { isAccessible = true }
                ?: return "ignored-slots-field-missing"
        val addIgnoredSlots =
            containerClass.declaredMethods
                .filter { method ->
                    method.name == "addIgnoredSlots" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(java.util.List::class.java),
                        ) &&
                        method.returnType == Void.TYPE
                }
                .singleOrNull()
                ?.apply { isAccessible = true }
                ?: return "add-ignored-slots-contract-missing"
        val setIgnoredSlots =
            containerClass.declaredMethods
                .filter { method ->
                    method.name == "setIgnoredSlots" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(java.util.List::class.java),
                        ) &&
                        method.returnType == Void.TYPE
                }
                .singleOrNull()
                ?.apply { isAccessible = true }
                ?: return "set-ignored-slots-contract-missing"
        val batteryContainerClass =
            runCatching {
                Class.forName(BATTERY_CONTAINER, false, classLoader)
            }.getOrElse {
                return "battery-container-class-missing"
            }
        val hideField =
            generateSequence(batteryContainerClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { candidate ->
                        candidate.name == "mIsHideBattery" &&
                            candidate.type == java.lang.Boolean.TYPE
                    }
                }
                .firstOrNull()
                ?.apply { isAccessible = true }
                ?: return "battery-hide-field-missing"
        val batterySetIsHide =
            batteryContainerClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "setIsHideBattery" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(java.lang.Boolean::class.java),
                        ) &&
                        method.returnType == Void.TYPE
                }
                ?.apply { isAccessible = true }
                ?: return "battery-hide-method-contract-missing"

        val onMeasure =
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
                ?: return "on-measure-contract-missing"
        val onLayout =
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
                ?: return "on-layout-contract-missing"

        val getIslandShowing =
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
                ?: return "island-showing-contract-missing"

        eventSink = onEvent
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
                return "measure-hook-" + (error.message ?: error.javaClass.simpleName)
            }
        val second =
            runCatching {
                module
                    .hook(onLayout)
                    .setId(LAYOUT_HOOK_ID)
                    .intercept(layoutHooker(refreshMasksAfter = true))
            }.getOrElse { error ->
                runCatching { first.unhook() }
                clearInstallState()
                return "layout-hook-" + (error.message ?: error.javaClass.simpleName)
            }
        val third =
            runCatching {
                module
                    .hook(batterySetIsHide)
                    .setId(BATTERY_HIDE_HOOK_ID)
                    .intercept(batteryHideStateHooker())
            }.getOrElse { error ->
                runCatching { first.unhook() }
                runCatching { second.unhook() }
                clearInstallState()
                return "battery-hide-hook-" + (error.message ?: error.javaClass.simpleName)
            }

        val fourth =
            runCatching {
                module
                    .hook(getIslandShowing)
                    .setId(ISLAND_SHOWING_HOOK_ID)
                    .intercept(islandShowingHooker())
            }.getOrElse { error ->
                runCatching { first.unhook() }
                runCatching { second.unhook() }
                runCatching { third.unhook() }
                clearInstallState()
                return "island-showing-hook-" +
                    (error.message ?: error.javaClass.simpleName)
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
        val baseSlotWidthPx =
            SysUiCarrierMetrics.resolveWidthPx(batteryCarrier)
                ?: return Result.Failure("battery-core-width-unavailable")

        @Suppress("UNCHECKED_CAST")
        val list =
            runCatching { field.get(statusIcons) as? MutableList<String> }.getOrNull()
                ?: return Result.Failure("ignored-slots-list-unavailable")
        list.size

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

        @Suppress("UNCHECKED_CAST")
        val list =
            runCatching { field.get(resolved.statusIcons) as? MutableList<String> }.getOrNull()
                ?: return Result.Failure(
                    surface.surfaceName + "-ignored-slots-list-unavailable",
                )
        list.size

        keyguardFamilyEventSink = onEvent
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
        val restored = session.stop(source)
        keyguardFamilyEventSink?.invoke(
            surface.eventPrefix + " inactive source=" + source +
                " restoredViews=" + restored,
        )
        keyguardFamilyEventSink = null
        keyguardFamilyFailNativeSink = null
        keyguardFamilyReadySink = null
        return Result.Inactive(restored)
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
            return Result.Failure("hooks-not-ready")
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
            ?: return Result.Failure("battery-core-width-unavailable")

        @Suppress("UNCHECKED_CAST")
        val list =
            runCatching { field.get(statusIcons) as? MutableList<String> }.getOrNull()
                ?: return Result.Failure("ignored-slots-list-unavailable")
        list.size

        controlCenterEventSink = onEvent
        controlCenterFailNativeSink = onFailNative
        controlCenterReadySink = onReady

        val existing = controlCenterCurrent
        if (
            existing?.matches(
                host = host,
                statusIcons = statusIcons,
                batteryContainer = batteryContainer,
                battery = battery,
                batteryCarrier = batteryCarrier,
            ) == true
        ) {
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
        val restored = session.stop(source)
        controlCenterEventSink?.invoke(
            "controlCenterPresentation inactive source=" + source +
                " restoredViews=" + restored,
        )
        controlCenterEventSink = null
        controlCenterFailNativeSink = null
        controlCenterReadySink = null
        return Result.Inactive(restored)
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
        val controlCenterRestored =
            controlCenterCurrent?.let { session ->
                controlCenterCurrent = null
                session.stop(
                    source = "hotReload-oldGeneration",
                    requestLayout = requestLayout,
                )
            } ?: 0
        val homeRestored =
            current?.let { session ->
                current = null
                session.stop(
                    source = "hotReload-oldGeneration",
                    requestLayout = requestLayout,
                )
            } ?: 0
        val keyguardFamilyRestored =
            keyguardFamilyCurrent?.let { session ->
                keyguardFamilyCurrent = null
                keyguardFamilySurface = null
                session.stop(
                    source = "hotReload-oldGeneration",
                    requestLayout = requestLayout,
                )
            } ?: 0
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
        return homeRestored + keyguardFamilyRestored + controlCenterRestored
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivateCc(source)
        deactivateAod(source)
        deactivateKeyguard(source)
        deactivate(source)
        runCatching { measureHook?.unhook() }
        runCatching { layoutHook?.unhook() }
        runCatching { batteryHideHook?.unhook() }
        runCatching { islandShowingHook?.unhook() }
        clearInstallState()
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
            }.getOrDefault(false)
        return if (removed) {
            LegacyCleanupResult.Removed
        } else {
            LegacyCleanupResult.Failure("legacy-removal-verification-failed")
        }
    }

    private fun layoutHooker(refreshMasksAfter: Boolean): Hooker =
        Hooker { chain ->
            val target = chain.thisObject as? ViewGroup
                ?: return@Hooker chain.proceed()
            val session =
                synchronized(this) {
                    controlCenterCurrent?.takeIf { candidate -> candidate.owns(target) }
                        ?: current?.takeIf { candidate -> candidate.owns(target) }
                        ?: keyguardFamilyCurrent?.takeIf { candidate -> candidate.owns(target) }
                } ?: return@Hooker chain.proceed()

            val result = session.withRepresentedSlotsIgnored { chain.proceed() }
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
            result
        }

    private fun islandShowingHooker(): Hooker =
        Hooker { chain ->
            val nativeResult = chain.proceed()
            val nativeShowing =
                nativeResult as? Boolean
                    ?: return@Hooker nativeResult
            val target =
                chain.thisObject as? ViewGroup
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
                        eventSink?.invoke(
                            "steadyPeerMirror source=home active=" + snapshot.active +
                                " hiddenSlots=[" +
                                snapshot.hiddenSlots.sorted().joinToString(",") +
                                "] authority=home-native-island-state",
                        )
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
            val target = chain.thisObject as? ViewGroup
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
            session?.syncEndReservation()
            result
        }

    @Synchronized
    private fun onSessionFailure(reason: String) {
        val session = current ?: return
        current = null
        steadyPeerMirrorActive = false
        steadyPeerMirrorHiddenSlots = emptySet()
        controlCenterCurrent?.updateSteadyPeerMirror(active = false, hiddenSlots = emptySet())
        session.stop("fail-native:" + reason)
        eventSink?.invoke(
            "homePresentation failNative reason=" + reason,
        )
        failNativeSink?.invoke(reason)
    }

    @Synchronized
    private fun onKeyguardFamilySessionFailure(reason: String) {
        val session = keyguardFamilyCurrent ?: return
        val surface = keyguardFamilySurface ?: return
        keyguardFamilyCurrent = null
        keyguardFamilySurface = null
        session.stop("fail-native:" + reason)
        keyguardFamilyEventSink?.invoke(
            surface.eventPrefix + " failNative reason=" + reason,
        )
        keyguardFamilyFailNativeSink?.invoke(reason)
        keyguardFamilyEventSink = null
        keyguardFamilyFailNativeSink = null
        keyguardFamilyReadySink = null
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
        session.stop("fail-native:" + reason)
        controlCenterEventSink?.invoke(
            "controlCenterPresentation failNative reason=" + reason,
        )
        controlCenterFailNativeSink?.invoke(reason)
        controlCenterEventSink = null
        controlCenterFailNativeSink = null
        controlCenterReadySink = null
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
        private var leasePhase = LeasePhase.PREARM
        private var pendingNativeCarrierWidth: Int? = null
        private var batteryWidthUnavailable = false
        private var persistentIgnoredSlotsApplied = false
        private var nativeLayoutOwnershipDeferred = false
        private var ownedPersistentIgnoredSlots: List<String> = emptyList()
        private var transitionRequestedSlotWidthPx: Int? = null
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

        fun retargetPresentation(
            surfaceName: String,
            eventPrefix: String,
        ) {
            this.surfaceName = surfaceName
            this.eventPrefix = eventPrefix
        }

        fun hasPresentationClaim(): Boolean =
            active &&
                (
                    persistentIgnoredSlotsApplied ||
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
            layoutReadyCallback = null
            compactLayoutReady = false
            nativeLayoutOwnershipDeferred = false
            leasePhase = LeasePhase.PREARM
            pendingNativeCarrierWidth = null
            deferVisualMaskUntilLayout = false
            host.get()?.removeOnAttachStateChangeListener(this)
            battery.get()?.removeOnLayoutChangeListener(batteryLayoutListener)
            batteryCarrier.get()?.removeOnLayoutChangeListener(carrierLayoutListener)
            val reservationRestored = restoreEndReservation()
            val ignoredSlotsRestored =
                restorePersistentIgnoredSlots(
                    requestLayout = requestLayout,
                )
            val restored = restoreClipMasks() + restoreMirroredPeerClipMasks()
            val explicitLayoutRequest =
                requestLayout && ignoreScope == IgnoreScope.CALL
            if (explicitLayoutRequest) {
                batteryContainer.get()?.requestLayout()
            }
            onEvent(
                eventPrefix + " cleanup source=" + source +
                    " restoredClipBounds=" + restored +
                    " restoredEndReservation=" + reservationRestored +
                    " restoredIgnoredSlots=" + ignoredSlotsRestored +
                    " requestLayout=" + explicitLayoutRequest,
            )
            return restored
        }

        fun <T> withRepresentedSlotsIgnored(block: () -> T): T {
            if (!active || ignoreScope == IgnoreScope.SESSION) {
                return block()
            }
            val container = statusIcons.get()
            if (container == null) {
                onFailNative("status-icon-group-released")
                return block()
            }
            @Suppress("UNCHECKED_CAST")
            val list =
                runCatching {
                    ignoredSlotsField.get(container) as? MutableList<String>
                }.getOrNull()
            if (list == null) {
                onFailNative("ignored-slots-list-unavailable")
                return block()
            }

            val owned =
                try {
                    OwnedEntries.add(list, representedSlots)
                } catch (error: Throwable) {
                    onFailNative(
                        "ignored-slots-add-" +
                            (error.message ?: error.javaClass.simpleName),
                    )
                    return block()
                }

            return try {
                block()
            } finally {
                OwnedEntries.restore(list, owned)
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
                runCatching {
                    ignoredSlotsField.get(group) as? MutableList<String>
                }.getOrNull()
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
                val rollback =
                    setIgnoredSlotsMethod?.let { setMethod ->
                        runCatching {
                            setMethod.invoke(group, ArrayList(before))
                        }.isSuccess
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
                runCatching {
                    ignoredSlotsField.get(group) as? MutableList<String>
                }.getOrNull() ?: return false
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
                }.getOrDefault(false)
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

        fun syncEndReservation(): Boolean {
            if (!active) return true
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
                runCatching { batteryHideField.getBoolean(container) }.getOrNull()
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
                HomeLayoutResolver.resolve(
                    hostWidthPx = hostView.width,
                    hostHeightPx = hostView.height,
                    baseCarrierWidthPx = stableCarrierWidthPx,
                    isRtl = hostView.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                ) ?: run { onFailNative(surfaceName + "-layout-unavailable"); return false }
            val compactSlotWidthPx =
                CompactReservationPolicy.resolveCenteredVisualWidth(
                    baseSlotWidthPx = resolved.requestedSlotWidthPx.toInt(),
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
            val reservationDelta =
                if (surfaceName == CONTROL_CENTER_FAKE_SURFACE) {
                    EndReservationPolicy.resolveCapacityBoundedReservationDelta(
                        nativeHide = nativeHide,
                        compactSlotWidthPx = compactSlotWidthPx,
                        requestedReservationDeltaPx = requestedReservationDelta,
                        capacityDeltaPx = capacityDeltaPx,
                    )
                } else {
                    requestedReservationDelta
                }
            val capacityReservationDeltaPx =
                EndReservationPolicy.resolveFakeCarrierCapacityRequirement(
                    nativeHide = nativeHide,
                    compactSlotWidthPx = compactSlotWidthPx,
                    reservationDeltaPx = reservationDelta,
                )
            if (
                surfaceName == CONTROL_CENTER_FAKE_SURFACE &&
                capacityReservationDeltaPx > capacityDeltaPx
            ) {
                onFailNative("fake-carrier-capacity-insufficient")
                return false
            }
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
            if (applied != null) {
                val group = statusIcons.get()
                val baseline = nativePadding
                if (group == null || baseline == null) {
                    paddingRestored = false
                    appliedPadding = null
                } else {
                    val live = PaddingState.from(group)
                    if (live != applied) {
                        onEvent(
                            eventPrefix + " endReservation restore=skipped reason=writer-changed " +
                                "livePaddingEnd=" + live.end +
                                " appliedPaddingEnd=" + applied.end,
                        )
                        paddingRestored = false
                    } else {
                        group.setPaddingRelative(
                            baseline.start,
                            baseline.top,
                            baseline.end,
                            baseline.bottom,
                        )
                        paddingRestored = PaddingState.from(group) == baseline
                    }
                    appliedPadding = null
                }
            }
            val carrierRestored = restoreFakeCarrierCapacityLease()
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
                onEvent(
                    eventPrefix +
                        " fakeCarrierCapacity restore=skipped reason=writer-changed" +
                        " liveWidth=" + params.width +
                        " appliedWidth=" + appliedWidthPx,
                )
                clearCapacityLeaseSnapshot()
                return false
            }

            params.width = baselineLayoutWidthPx
            hostView.layoutParams = params
            val restored = hostView.layoutParams?.width == baselineLayoutWidthPx
            clearCapacityLeaseSnapshot()
            pendingNativeCarrierWidth = null
            return restored
        }

        private fun clearCapacityLeaseSnapshot() {
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

        fun validateNativeLayoutBeforeVisualMask(): Boolean {
            if (!active || !started) return false
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
            if (
                hostView.layoutParams?.width != expectedWidthPx ||
                !isFakeCarrierEndAnchored(hostView, parent)
            ) {
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
                            NativeNetworkSuppressor
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
            val changed =
                steadyPeerMirrorActive != active ||
                    steadyPeerMirrorHiddenSlots != normalized
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

            val iterator = mirroredPeerClipStates.iterator()
            while (iterator.hasNext()) {
                val state = iterator.next()
                val view = state.view.get()
                if (view == null || view !in targets) {
                    if (view != null) {
                        restoreClipState(state)
                    }
                    iterator.remove()
                }
            }

            targets.forEach { view ->
                if (mirroredPeerClipStates.none { state -> state.view.get() === view }) {
                    val nativeClip = view.clipBounds?.let(::Rect)
                    val applied = Rect(0, 0, 0, 0)
                    view.clipBounds = applied
                    mirroredPeerClipStates += ClipState(WeakReference(view), nativeClip, applied)
                }
            }
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
            targets += batteryView
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                if (NativeParticipantAccess.slotOf(child) in representedSlots) {
                    targets += child
                }
            }

            val iterator = clipStates.iterator()
            while (iterator.hasNext()) {
                val state = iterator.next()
                val view = state.view.get()
                if (view == null || view !in targets) {
                    if (view != null) {
                        restoreClipState(state)
                    }
                    iterator.remove()
                }
            }

            targets.forEach { view ->
                if (clipStates.none { state -> state.view.get() === view }) {
                    val nativeClip = view.clipBounds?.let(::Rect)
                    val applied = Rect(0, 0, 0, 0)
                    view.clipBounds = applied
                    clipStates += ClipState(WeakReference(view), nativeClip, applied)
                }
            }
            refreshMirroredPeerClipMasks()
            return clipStates.count { state -> state.view.get() != null }
        }

        override fun onViewAttachedToWindow(view: View) = Unit

        override fun onViewDetachedFromWindow(view: View) {
            if (active) {
                onFailNative(surfaceName + "-host-detached")
            }
        }

        private fun restoreClipMasks(): Int {
            val states = clipStates.toList()
            clipStates.clear()
            var restored = 0
            states.forEach { state ->
                if (restoreClipState(state)) {
                    restored += 1
                }
            }
            return restored
        }

        private fun restoreMirroredPeerClipMasks(): Int {
            val states = mirroredPeerClipStates.toList()
            mirroredPeerClipStates.clear()
            var restored = 0
            states.forEach { state ->
                if (restoreClipState(state)) {
                    restored += 1
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
        data class Failure(val reason: String) : Result
    }


    internal sealed interface LegacyCleanupResult {
        data object NotPresent : LegacyCleanupResult
        data object Removed : LegacyCleanupResult
        data class Failure(val reason: String) : LegacyCleanupResult
    }
}
