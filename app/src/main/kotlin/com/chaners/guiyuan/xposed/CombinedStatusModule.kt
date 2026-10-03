package com.chaners.guiyuan.xposed

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.settings.CombinedStatusFeatureSettings
import com.chaners.guiyuan.settings.RUNTIME_REMOTE_PREFS_NAME
import com.chaners.guiyuan.system.RuntimeDiagnosticsProtocol
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.util.concurrent.atomic.AtomicLong

class CombinedStatusModule : XposedModule() {
    private var islandMotionSourceInstalled = false
    private var panelTransitionSourceInstalled = false
    private var controlCenterSceneVisible = false
    private var controlCenterSceneEligible = false
    private var controlCenterSourceScene = CombinedStatusSourceScene.UNKNOWN
    private var steadyStatusSourceScene = CombinedStatusSourceScene.UNKNOWN
    private var lastStableKeyguardAodScene =
        CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN
    private var keyguardAodFullTargetPending = false
    private var keyguardAodPendingTargetToLockScreen: Boolean? = null
    private var keyguardAodFullTransitionActive = false
    private var keyguardBoundaryVisualHandoffActive = false
    private var keyguardBoundaryLayoutPrecommitActive = false
    private var keyguardBoundaryCompactLayoutReady = false
    private var keyguardBoundaryVisualBoundaryReached = false
    private var homePresentationOwnedAtFullAodStart = false
    private var homeNativeAodFallbackCandidate = false
    private var homeNativeAodFallbackActive = false
    private var homeAodTransitionOriginPending = false
    private var homeAodTargetPrearmPending = false
    private var controlCenterExpansionFraction = 0f
    private var keyguardRuntimeReady = false
    private var aodRendererAttached = false
    private var keyguardPresentationReadyObserved = false
    private var keyguardControlCenterLeaseActive = false
    private var controlCenterGeometryProbeBucket = -1
    private var lastBatteryNumberProbeDiagnosticSummary: String? = null
    private var runtimeSessionId = newRuntimeSessionId()
    private val diagnosticSequence = AtomicLong(0L)
    private val renderTraceSequence = AtomicLong(0L)

    @Volatile
    private var detailedDiagnosticsEnabled = BuildConfig.DEVELOPMENT_PROBES

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        bindRuntimeDiagnostics()
        bindRuntimeFeatureSettings()
        bindRuntimeVisualSettings()
        log(
            Log.INFO,
            TAG,
            "Module loaded in " + param.processName +
                " build=" + BuildConfig.BUILD_ID +
                " channel=" + BuildConfig.BUILD_CHANNEL +
                " diagnostics=" + if (detailedDiagnosticsEnabled) "detailed" else "general" +
                " with Xposed API " + apiVersion,
        )
        logDiagnostic(
            level = Log.INFO,
            event = "module.loaded",
            component = "module",
            state = "ready",
            "process" to param.processName,
            "build" to BuildConfig.BUILD_ID,
            "channel" to BuildConfig.BUILD_CHANNEL,
            "api" to apiVersion,
        )
        logCurrentDiagnosticsHealth()
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName != SYSTEM_UI_PACKAGE) {
            return
        }

        val compatibility = SystemUiCompatibilityProbe.inspect(param.classLoader)
        log(Log.INFO, TAG, compatibility.summary)
        val statusHostAvailable = compatibility.isAvailable("statusHost")
        logDiagnostic(
            level = if (statusHostAvailable) Log.INFO else Log.WARN,
            event = "compatibility.probe",
            component = "compatibility",
            state = if (statusHostAvailable) "ready" else "unavailable",
            "statusHost" to if (statusHostAvailable) "available" else "missing",
        )

        if (!statusHostAvailable) {
            return
        }

        installHomePresentationOwner(
            classLoader = param.classLoader,
            source = "coldStart",
        )
        installNativeNetworkSuppression(
            classLoader = param.classLoader,
            source = "coldStart",
        )

        runCatching {
            SystemUiHostRuntimeOwner.install(
                module = this,
                classLoader = param.classLoader,
                onCaptured = ::onStatusHostCaptured,
            )
        }.onSuccess {
            logDiagnostic(
                level = Log.INFO,
                event = "hook.install",
                component = "statusHostHook",
                state = "ready",
            )
        }.onFailure { error ->
            logDiagnostic(
                level = Log.ERROR,
                event = "hook.install",
                component = "statusHostHook",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
            )
            log(Log.ERROR, TAG, "Status host hook installation failed", error)
        }

        if (SystemUiHostRuntimeOwner.isReady) {
            installBatteryStateSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            installNetworkStateSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            installPresentationRuntimeSources(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            installPanelTransitionSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            installIslandMotionSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
        }
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        val prepared =
            SystemUiHotReloadRuntimeOwner.prepare(
                param = param,
                generationHandoff =
                    Runnable {
                        teardownOldGenerationForHotReload(
                            continuousHandoff = true,
                        )
                    },
            )
        if (prepared is SystemUiHotReloadRuntimeOwner.PrepareResult.Unavailable) {
            logDiagnostic(
                level = Log.WARN,
                event = "hotReload.prepare",
                component = "hotReload",
                state = "unavailable",
                "reason" to prepared.reason,
                "wifiRoots" to prepared.wifiRoots,
                "mobileRoots" to prepared.mobileRoots,
                "restartScope" to true,
            )
            log(Log.WARN, TAG, "Hot reload declined reason=" + prepared.reason)
            return false
        }

        prepared as SystemUiHotReloadRuntimeOwner.PrepareResult.Ready
        val hookCount =
            1 +
                SystemUiBatteryRuntimeOwner.installedHookCount +
                SystemUiNetworkRuntimeOwner.installedHookCount +
                SystemUiPresentationRuntimeOwner.installedHookCount +
                SystemUiHomePresentationOwner.installedHookCount +
                SystemUiNativeNetworkSuppressionOwner.installedHookCount +
                if (islandMotionSourceInstalled) {
                    SystemUiIslandMotionSource.HOOK_COUNT
                } else {
                    0
                } +
                if (panelTransitionSourceInstalled) {
                    SystemUiPanelTransitionSource.expectedHookCount(
                        BuildConfig.RUNTIME_DIAGNOSTICS,
                    )
                } else {
                    0
                }
        logDiagnostic(
            level = Log.INFO,
            event = "hotReload.prepare",
            component = "hotReload",
            state = "preparing",
            "hooks" to hookCount,
            "build" to BuildConfig.BUILD_ID,
            "transfer" to "classloader-neutral",
            "hostIdentity" to System.identityHashCode(prepared.host),
            "wifiRoots" to prepared.wifiRoots,
            "mobileRoots" to prepared.mobileRoots,
            "tintTransfer" to if (prepared.tintTransferred) "ready" else "native-fallback",
            "controlCenterCompactReady" to prepared.controlCenterCompactReady,
        )
        log(
            Log.INFO,
            TAG,
            "Hot reload preparing build=" + BuildConfig.BUILD_ID +
                " hooks=" + hookCount +
                " transfer=classloader-neutral",
        )

        logDiagnostic(
            level = Log.INFO,
            event = "hotReload.cleanup",
            component = "hotReload",
            state = "deferred-to-new-generation",
            "uiMutation" to "single-main-thread-handoff",
            "homePresentation" to "retained-until-generation-handoff",
            "controlCenterCompactReady" to prepared.controlCenterCompactReady,
        )
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        rotateDiagnosticSession()
        val takeover =
            SystemUiHotReloadRuntimeOwner.takeOverHooks(
                param = param,
                onCaptured = ::onStatusHostCaptured,
            )

        if (takeover == null) {
            bindRuntimeDiagnostics()
            bindRuntimeFeatureSettings()
            bindRuntimeVisualSettings()
            logDiagnostic(
                level = Log.ERROR,
                event = "hotReload.complete",
                component = "hotReload",
                state = "error",
                "reason" to "status-host-hook-missing",
                "restartScope" to true,
            )
            log(
                Log.ERROR,
                TAG,
                "Hot reload incomplete reason=status-host-hook-missing restartScope=true",
            )
            return
        }

        runCatching {
            val removed = takeover.removedHooks

            SystemUiBatteryRuntimeOwner.resetRuntimeState()
            SystemUiNetworkRuntimeOwner.resetRuntimeState()
            islandMotionSourceInstalled = false
            panelTransitionSourceInstalled = false
            controlCenterSceneVisible = false
            controlCenterSceneEligible = false
            controlCenterSourceScene = CombinedStatusSourceScene.UNKNOWN
            steadyStatusSourceScene = CombinedStatusSourceScene.UNKNOWN
            lastStableKeyguardAodScene =
                CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN
            keyguardAodFullTargetPending = false
            keyguardAodPendingTargetToLockScreen = null
            keyguardAodFullTransitionActive = false
            resetKeyguardBoundaryHandoffState()
            homePresentationOwnedAtFullAodStart = false
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = false
            homeAodTransitionOriginPending = false
            homeAodTargetPrearmPending = false
            controlCenterExpansionFraction = 0f
            keyguardRuntimeReady = false
            aodRendererAttached = false
            keyguardPresentationReadyObserved = false
            keyguardControlCenterLeaseActive = false
            controlCenterGeometryProbeBucket = -1
            SystemUiPresentationRuntimeOwner.resetRuntimeState()
            SystemUiKeyguardHostResolver.resetRuntimeState()
            SystemUiHomePresentationOwner.resetRuntimeState("hotReload")
            SystemUiNativeNetworkSuppressionOwner.resetRuntimeState("hotReload")
            bindRuntimeDiagnostics()
            bindRuntimeFeatureSettings()
            bindRuntimeVisualSettings()
            logDiagnostic(
                level = Log.INFO,
                event = "module.reloaded",
                component = "module",
                state = "ready",
                "build" to BuildConfig.BUILD_ID,
                "channel" to BuildConfig.BUILD_CHANNEL,
            )
            logDiagnostic(
                level = Log.INFO,
                event = "compatibility.revalidated",
                component = "compatibility",
                state = "ready",
                "statusHost" to "available",
                "source" to "hotReloadHook",
            )
            logDiagnostic(
                level = Log.INFO,
                event = "hook.replace",
                component = "statusHostHook",
                state = "ready",
                "source" to "hotReload",
            )
            logCurrentDiagnosticsHealth()

            val classLoader = takeover.classLoader
            installBatteryStateSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            installNetworkStateSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            installPresentationRuntimeSources(
                classLoader = classLoader,
                source = "hotReload",
            )
            installHomePresentationOwner(
                classLoader = classLoader,
                source = "hotReload",
            )
            installNativeNetworkSuppression(
                classLoader = classLoader,
                source = "hotReload",
            )
            installPanelTransitionSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            installIslandMotionSource(
                classLoader = classLoader,
                source = "hotReload",
            )

            val restored = SystemUiHotReloadRuntimeOwner.restoreTransfer(param)
            if (restored == null) {
                CombinedStatusStateStore.restoreHotReloadState(null)
                logDiagnostic(
                    level = Log.WARN,
                    event = "hotReload.restore",
                    component = "hotReload",
                    state = "unavailable",
                    "reason" to "saved-state-missing-or-unsupported-generation",
                    "restartScope" to true,
                )
                logDiagnostic(
                    level = Log.WARN,
                    event = "hotReload.complete",
                    component = "hotReload",
                    state = "partial",
                    "build" to BuildConfig.BUILD_ID,
                    "statusHostHook" to "replaced",
                    "staleHooks" to removed,
                    "restartScope" to true,
                )
                return@runCatching
            }

            val capture = SystemUiHostRegistry.restoreStatusHost(restored.host)
            logDiagnostic(
                level = Log.INFO,
                event = "host.restore",
                component = "statusHost",
                state = "ready",
                "identity" to capture.identity,
                "replacement" to capture.replacement,
                "source" to "hotReloadTransfer",
            )

            val hostView = capture.host as? android.view.View
                ?: error("restored-host-not-view")
            val restoreScheduled =
                hostView.post {
                    restoreHotReloadRuntimeOnMain(
                        capture = capture,
                        restored = restored,
                        removedHooks = removed,
                    )
                }
            if (!restoreScheduled) {
                logDiagnostic(
                    level = Log.ERROR,
                    event = "hotReload.complete",
                    component = "hotReload",
                    state = "error",
                    "reason" to "main-thread-restore-scheduling-failed",
                    "restartScope" to true,
                )
                return@runCatching
            }

            logDiagnostic(
                level = Log.INFO,
                event = "hotReload.restore",
                component = "hotReload",
                state = "scheduled",
                "hostIdentity" to capture.identity,
                "uiMutation" to "main-thread-only",
                "restartScope" to false,
            )
        }.onFailure { error ->
            logDiagnostic(
                level = Log.ERROR,
                event = "hotReload.complete",
                component = "hotReload",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "restartScope" to true,
            )
            log(Log.ERROR, TAG, "Hot reload failed restartScope=true", error)
        }
    }

    private fun restoreHotReloadRuntimeOnMain(
        capture: SystemUiHostRegistry.Capture,
        restored: CombinedStatusHotReloadTransfer.Restored,
        removedHooks: Int,
    ) {
        runCatching {
            var restoredSnapshot =
                CombinedStatusStateStore.restoreHotReloadState(restored.state)
            val bindings =
                SystemUiNetworkStateSource.restoreHotReloadBindings(restored.bindings)
            SystemUiNetworkStateSource.seedRestoredWifiState(
                onEvent =
                    if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                        ::onNetworkPipelineEvent
                    } else {
                        null
                    },
            )?.let { wifi ->
                CombinedStatusStateStore.updateWifi(wifi)?.let { snapshot ->
                    restoredSnapshot = snapshot
                }
            }

            when (
                val legacy =
                    SystemUiHomePresentationOwner.cleanupLegacyParticipant(capture.host)
            ) {
                SystemUiHomePresentationOwner.LegacyCleanupResult.NotPresent -> Unit
                SystemUiHomePresentationOwner.LegacyCleanupResult.Removed -> {
                    logDiagnostic(
                        level = Log.WARN,
                        event = "hotReload.migration",
                        component = "homePresentation",
                        state = "restart-required",
                        "reason" to "legacy-participant-removed-native-mask-state-untrusted",
                        "restartScope" to true,
                    )
                    return@runCatching
                }
                is SystemUiHomePresentationOwner.LegacyCleanupResult.Failure -> {
                    logDiagnostic(
                        level = Log.ERROR,
                        event = "hotReload.migration",
                        component = "homePresentation",
                        state = "restart-required",
                        "reason" to legacy.reason,
                        "restartScope" to true,
                    )
                    return@runCatching
                }
            }

            val generationHandoff =
                restored.generationHandoff?.let { handoff ->
                    runCatching {
                        handoff.run()
                        "continuous"
                    }.getOrElse { error ->
                        throw IllegalStateException(
                            "old-generation-handoff-failed",
                            error,
                        )
                    }
                } ?: "legacy-pre-cleaned"

            logDiagnostic(
                level = Log.INFO,
                event = "hotReload.generationHandoff",
                component = "hotReload",
                state = "ready",
                "mode" to generationHandoff,
                "layoutCommit" to "single-main-thread-turn",
                "intermediateRequestLayout" to false,
            )

            SystemUiPanelTransitionSource.restoreControlCenterHomeEligibility(
                restored.controlCenterHomeEligible,
            )
            val controlCenterFakeRestore =
                restored.controlCenterFakeHost?.let { fakeHost ->
                    restoreControlCenterFakePresentationAfterHotReload(
                        host = fakeHost,
                        transferredCompactReady = restored.controlCenterCompactReady,
                    )
                } ?: "late-fallback"
            val transferredTint =
                restored.appliedTint?.let { appliedTint ->
                    CombinedStatusTintState(
                        appliedTint = appliedTint,
                        statusIconTint = restored.statusIconTint,
                    )
                }
            attachHostRuntime(
                host = capture.host,
                source = "hotReloadRestore",
                initialNativeHandoffActive = true,
                initialTintState = transferredTint,
                allowLiveTintSeed = false,
            )

            logDiagnostic(
                level = Log.INFO,
                event = "hotReload.restore",
                component = "hotReload",
                state = "ready",
                "hostIdentity" to capture.identity,
                "wifiRoots" to bindings.wifiRoots,
                "mobileRoots" to bindings.mobileRoots,
                "state" to restoredSnapshot.logLine,
                "homePresentation" to "native-carrier-lifecycle",
                "controlCenterHomeEligible" to
                    (restored.controlCenterHomeEligible ?: "unknown"),
                "controlCenterFakePrearm" to controlCenterFakeRestore,
                "tintTransfer" to if (transferredTint != null) "restored" else "native-fallback",
                "mainThread" to true,
            )
            logDiagnostic(
                level = Log.INFO,
                event = "hotReload.complete",
                component = "hotReload",
                state = "ready",
                "build" to BuildConfig.BUILD_ID,
                "statusHostHook" to "replaced",
                "staleHooks" to removedHooks,
                "controlCenterFakePrearm" to controlCenterFakeRestore,
                "restartScope" to false,
            )
        }.onFailure { error ->
            logDiagnostic(
                level = Log.ERROR,
                event = "hotReload.complete",
                component = "hotReload",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "restartScope" to true,
            )
            log(Log.ERROR, TAG, "Hot reload main-thread restore failed", error)
        }
    }

    private fun installHomePresentationOwner(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                SystemUiHomePresentationOwner.install(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onFailNative = ::onHomePresentationRuntimeFailure,
                )
        ) {
            SystemUiHomePresentationOwner.InstallResult.Installed,
            SystemUiHomePresentationOwner.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "homePresentation",
                    state = "ready",
                    "source" to source,
                    "hooks" to SystemUiHomePresentationOwner.installedHookCount,
                    "carrier" to "MiuiNotificationStatusContainer.overlay",
                    "nativeGeometryWrites" to 0,
                )
            }
            is SystemUiHomePresentationOwner.InstallResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "hook.install",
                    component = "homePresentation",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "fallback" to "native-systemui",
                )
            }
        }
    }

    private fun installNativeCombinedParticipant(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                SystemUiNativeCombinedParticipantOwner.install(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    isTransitionProbeEnabled = { detailedDiagnosticsEnabled },
                    onSlotOrderResult = { slotOrder ->
                        when (slotOrder) {
                            is NativeStatusBarSlotReservation.Result.Ready -> {
                                logDiagnostic(
                                    level = Log.INFO,
                                    event = "slot.reserve",
                                    component = "nativeSlotOrder",
                                    state = "ready",
                                    "source" to source,
                                    "mode" to "controller-pre-init",
                                    "created" to slotOrder.created,
                                    "nativeIndex" to slotOrder.nativeIndex,
                                    "fromIndex" to slotOrder.fromIndex,
                                    "toIndex" to slotOrder.toIndex,
                                    "slotCount" to slotOrder.slotCount,
                                    "viewOnlySynced" to slotOrder.viewOnlySynced,
                                    "originalOrderPreserved" to
                                        slotOrder.originalOrderPreserved,
                                    "visible" to false,
                                    "nativeGeometryWrites" to 0,
                                )
                            }

                            is NativeStatusBarSlotReservation.Result.Failure -> {
                                logDiagnostic(
                                    level = Log.WARN,
                                    event = "slot.reserve",
                                    component = "nativeSlotOrder",
                                    state = "unavailable",
                                    "source" to source,
                                    "mode" to "controller-pre-init",
                                    "reason" to slotOrder.reason,
                                    "visible" to false,
                                    "nativeGeometryWrites" to 0,
                                )
                            }
                        }
                    },
                )
        ) {
            SystemUiNativeCombinedParticipantOwner.InstallResult.Installed,
            SystemUiNativeCombinedParticipantOwner.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeCombinedParticipant",
                    state = "ready",
                    "source" to source,
                    "hooks" to SystemUiNativeCombinedParticipantOwner.installedHookCount,
                    "visible" to false,
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeCombinedParticipantOwner.InstallResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "hook.install",
                    component = "nativeCombinedParticipant",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun installNativeNetworkSuppression(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                SystemUiNativeNetworkSuppressionOwner.install(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onStatusPresentationChanged = ::onStatusIconPresentationChanged,
                )
        ) {
            SystemUiNativeNetworkSuppressionOwner.InstallResult.Installed,
            SystemUiNativeNetworkSuppressionOwner.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeNetworkSuppression",
                    state = "ready",
                    "source" to source,
                    "hooks" to SystemUiNativeNetworkSuppressionOwner.installedHookCount,
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeNetworkSuppressionOwner.InstallResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "hook.install",
                    component = "nativeNetworkSuppression",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun installNativeBatterySuppression(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                SystemUiNativeBatterySuppressionOwner.install(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onNativeLayoutHideChanged = { hidden ->
                        SystemUiNativeCombinedParticipantOwner
                            .onNativeBatteryLayoutHideChanged(hidden)
                    },
                )
        ) {
            SystemUiNativeBatterySuppressionOwner.InstallResult.Installed,
            SystemUiNativeBatterySuppressionOwner.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeBatterySuppression",
                    state = "ready",
                    "source" to source,
                    "hooks" to SystemUiNativeBatterySuppressionOwner.installedHookCount,
                    "contract" to
                        "MiuiStatusBatteryContainer.setIsHideBattery(Boolean):native-layout-authority+visual-mask",
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeBatterySuppressionOwner.InstallResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "hook.install",
                    component = "nativeBatterySuppression",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun installNativeParticipantControllerObserver(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                SystemUiNativeParticipantRuntimeOwner.installControllerObserver(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                )
        ) {
            SystemUiNativeParticipantRuntimeOwner.InstallResult.Installed,
            SystemUiNativeParticipantRuntimeOwner.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeParticipantControllerObserver",
                    state = "ready",
                    "source" to source,
                    "hooks" to SystemUiNativeParticipantRuntimeOwner.installedHookCount,
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeParticipantRuntimeOwner.InstallResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "hook.install",
                    component = "nativeParticipantControllerObserver",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun installNetworkStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiNetworkRuntimeOwner.attach(
                module = this,
                classLoader = classLoader,
                onWifiState = { state ->
                    val trace = beginRenderTrace("wifi")
                    val changed = CombinedStatusStateStore.updateWifi(state)
                    if (changed != null) {
                        val stateTrace = markStateCommitted(trace)
                        refreshStatusIconObservation("wifi-semantic")
                        onCombinedStateChanged(
                            snapshot = changed,
                            trace = stateTrace,
                        )
                    }
                },
                onMobileIcon = { update ->
                    val trace = beginRenderTrace("mobile")
                    val changed = CombinedStatusStateStore.updateMobile(update)
                    val stateTrace =
                        if (changed != null) {
                            markStateCommitted(trace)
                        } else {
                            trace
                        }
                    refreshMobilePresentation(stateTrace)
                    if (changed != null) {
                        onCombinedStateChanged(
                            snapshot = CombinedStatusStateStore.snapshot(),
                            trace = stateTrace,
                        )
                    }
                },
                onMobileSignalWillApply = { image ->
                    SystemUiNativeNetworkSuppressionOwner.preMaskMobileSignal(image)
                },
                onPresentationChanged = {
                    refreshMobilePresentation(beginRenderTrace("networkPresentation"))
                },
                onEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onNetworkPipelineEvent else null,
            )
        }.onSuccess { result ->
            refreshStatusIconObservation("network-source:" + source)
            val fullyReady =
                result.wifiReady &&
                    result.mobileReady &&
                    SystemUiNetworkRuntimeOwner.installedHookCount == SystemUiNetworkStateSource.HOOK_COUNT
            val state =
                when {
                    fullyReady -> "ready"
                    SystemUiNetworkRuntimeOwner.installedHookCount > 0 -> "partial"
                    else -> "error"
                }
            logDiagnostic(
                level =
                    when (state) {
                        "ready" -> Log.INFO
                        "partial" -> Log.WARN
                        else -> Log.ERROR
                    },
                event = "source.install",
                component = "network",
                state = state,
                "hooks" to SystemUiNetworkRuntimeOwner.installedHookCount,
                "expectedHooks" to SystemUiNetworkStateSource.HOOK_COUNT,
                "wifi" to if (result.wifiReady) "ready" else "error",
                "mobile" to if (result.mobileReady) "ready" else "error",
                "source" to source,
            )
            result.failures.forEach { failure ->
                logDiagnostic(
                    level = Log.ERROR,
                    event = "source.install.branch",
                    component = "network." + failure.component,
                    state = "error",
                    "stage" to failure.stage,
                    "errorType" to failure.errorType,
                    "reason" to failure.reason,
                    "source" to source,
                )
                log(
                    Log.ERROR,
                    TAG,
                    "Network branch installation failed component=" + failure.component +
                        " stage=" + failure.stage +
                        " errorType=" + failure.errorType +
                        " reason=" + failure.reason +
                        " source=" + source,
                )
            }
            log(
                if (fullyReady) Log.INFO else Log.WARN,
                TAG,
                "networkSource state=" + state +
                    " hooks=" + SystemUiNetworkRuntimeOwner.installedHookCount +
                    "/" + SystemUiNetworkStateSource.HOOK_COUNT +
                    " wifi=" + result.wifiReady +
                    " mobile=" + result.mobileReady +
                    " source=" + source +
                    " rebindRequired=" + (source == "hotReload"),
            )
        }.onFailure { error ->
            SystemUiNetworkRuntimeOwner.resetRuntimeState()
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "network",
                state = "error",
                "errorType" to error.javaClass.name,
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
            )
            log(Log.ERROR, TAG, "Network state source installation failed", error)
        }
    }

    private fun installIslandMotionSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiIslandMotionSource.install(
                module = this,
                classLoader = classLoader,
                onEvent =
                    if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                        ::onIslandMotionEvent
                    } else {
                        null
                    },
                isProbeEnabled = {
                    BuildConfig.DEVELOPMENT_PROBES || detailedDiagnosticsEnabled
                },
            )
        }.onSuccess { handles ->
            islandMotionSourceInstalled =
                handles.size == SystemUiIslandMotionSource.HOOK_COUNT
            logDiagnostic(
                level = if (islandMotionSourceInstalled) Log.INFO else Log.WARN,
                event = "source.install",
                component = "islandMotion",
                state = if (islandMotionSourceInstalled) "ready" else "partial",
                "hooks" to handles.size,
                "expectedHooks" to SystemUiIslandMotionSource.HOOK_COUNT,
                "source" to source,
                "nativeGeometryWrites" to 0,
            )
            log(
                Log.INFO,
                TAG,
                "islandMotionSource hooks=ready count=" + handles.size +
                    " source=" + source +
                    " authority=island-status diagnostics=" +
                    BuildConfig.RUNTIME_DIAGNOSTICS +
                    " nativeGeometryWrites=0",
            )
        }.onFailure { error ->
            islandMotionSourceInstalled = false
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "islandMotion",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
            )
            log(Log.ERROR, TAG, "Island motion source installation failed", error)
        }
    }

    private fun onIslandMotionEvent(event: String) {
        if (detailedDiagnosticsEnabled) {
            log(Log.INFO, TAG, event)
        }
    }


    private fun installPanelTransitionSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiPanelTransitionSource.install(
                module = this,
                classLoader = classLoader,
                onUpdate = ::onPanelTransitionUpdate,
                onFakePresentationAttached = ::onControlCenterFakePresentationAttached,
                onRuntimeFailure = ::onPanelTransitionRuntimeFailure,
                onEvent = ::onPanelTransitionEvent,
                isProbeEnabled = {
                    BuildConfig.DEVELOPMENT_PROBES || detailedDiagnosticsEnabled
                },
                includeControlCenterDiagnostics = BuildConfig.RUNTIME_DIAGNOSTICS,
            )
        }.onSuccess { handles ->
            val expectedHooks =
                SystemUiPanelTransitionSource.expectedHookCount(
                    BuildConfig.RUNTIME_DIAGNOSTICS,
                )
            panelTransitionSourceInstalled = handles.size == expectedHooks
            // Home yields Control Center only after the projected native
            // carrier is structurally ready.
            CombinedStatusHomeRenderSession.onControlCenterAuthorityChanged(true)
            logDiagnostic(
                level = if (panelTransitionSourceInstalled) Log.INFO else Log.WARN,
                event = "source.install",
                component = "panelTransition",
                state = if (panelTransitionSourceInstalled) "ready" else "partial",
                "hooks" to handles.size,
                "expectedHooks" to expectedHooks,
                "notificationRuntimeHook" to false,
                "notificationHomeLifecycle" to "system-icons-carrier",
                "controlCenterVisibilityRuntimeHook" to true,
                "controlCenterExpansionRuntimeHook" to true,
                "controlCenterAppearanceRuntimeHook" to true,
                "source" to source,
                "nativeGeometryWrites" to 0,
            )
        }.onFailure { error ->
            panelTransitionSourceInstalled = false
            controlCenterGeometryProbeBucket = -1
            CombinedStatusHomeRenderSession.onControlCenterAuthorityChanged(true)
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "panelTransition",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
            )
            log(Log.ERROR, TAG, "Panel transition source installation failed", error)
        }
    }

    private fun onPanelTransitionUpdate(
        update: SystemUiPanelTransitionSource.Update,
    ) {
        val effectiveSourceScene = handleControlCenterPanelUpdate(update)
        val transitionUpdate =
            if (effectiveSourceScene != null) {
                update.copy(controlCenterSourceScene = effectiveSourceScene)
            } else {
                update
            }
        CombinedStatusControlCenterTransitionOwner.onPanelUpdate(transitionUpdate)

        if (detailedDiagnosticsEnabled) {
            val batteryNumberProbe =
                CombinedStatusControlCenterTransitionOwner.latestBatteryNumberProbeDiagnostic()
            if (
                batteryNumberProbe != null &&
                batteryNumberProbe != lastBatteryNumberProbeDiagnosticSummary
            ) {
                lastBatteryNumberProbeDiagnosticSummary = batteryNumberProbe
                logDiagnostic(
                    level = Log.INFO,
                    event = "target.probe",
                    component = "batteryNumberTarget",
                    state = "ready",
                    "summary" to batteryNumberProbe,
                    "readOnly" to true,
                    "nativeGeometryWrites" to 0,
                )
            }
        }

        if (!detailedDiagnosticsEnabled) {
            return
        }

        val bucket =
            SystemUiPanelTransitionSource.diagnosticBucket(update.fraction)
                ?: return
        if (update.visible == false) {
            controlCenterGeometryProbeBucket = -1
            return
        }
        if (bucket == controlCenterGeometryProbeBucket) {
            return
        }
        controlCenterGeometryProbeBucket = bucket
        val geometry =
            SystemUiNativeNetworkSuppressionOwner.currentTransitionTargetGeometry()
        val state =
            SystemUiNativeNetworkSuppressionOwner.currentTransitionStateSnapshot()
        val projection =
            CombinedStatusControlCenterRenderSession.currentProjectionGeometryDiagnostic()
        val transitionOwner =
            CombinedStatusControlCenterTransitionOwner.currentDiagnostic()
        log(
            Log.INFO,
            TAG,
            "controlCenterTransitionGeometry " +
                "fraction=" + update.fraction +
                " bucket=" + bucket + "/8 " +
                (geometry?.summary ?: "geometry=unavailable") +
                " " + (state?.summary ?: "state=unavailable") +
                " " + projection +
                " " + transitionOwner +
                " readOnly=true nativeGeometryWrites=0",
        )
    }

    private fun handleControlCenterPanelUpdate(
        update: SystemUiPanelTransitionSource.Update,
    ): CombinedStatusSourceScene? {
        update.fraction?.let(::onControlCenterExpansionFraction)

        val visible = update.visible ?: return null
        if (!visible) {
            controlCenterSceneVisible = false
            // Restore Home first. QS_FAKE compact presentation remains prearmed
            // for the lifetime of the native fake root; only the Combined
            // overlay visibility changes with Control Center visibility.
            CombinedStatusHomeRenderSession.onControlCenterAuthorityChanged(true)
            CombinedStatusControlCenterRenderSession.setRequestedVisible(false)
            return null
        }

        controlCenterSceneVisible = true
        CombinedStatusControlCenterRenderSession.beginVisibleCycle()
        val panelSourceScene =
            update.controlCenterSourceScene
                ?: CombinedStatusSourceScene.UNKNOWN
        val incomingBoundaryReady =
            incomingKeyguardPresentationReadyForControlCenter()
        val effectiveSourceScene =
            CombinedStatusScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = panelSourceScene,
                steadySourceScene = steadyStatusSourceScene,
                lastStableFamilyScene = lastStableKeyguardAodScene,
                incomingKeyguardPresentationReady = incomingBoundaryReady,
            )
        updateControlCenterSourceSceneEligibility(
            sourceScene = effectiveSourceScene,
            authority =
                when {
                    effectiveSourceScene == panelSourceScene ->
                        "hyperos-realSystemIcons"
                    incomingBoundaryReady &&
                        effectiveSourceScene == CombinedStatusSourceScene.KEYGUARD ->
                        "incoming-keyguard-presentation"
                    else -> "steady-source-view-override"
                },
        )
        val carrier = update.controlCenterPresentationHost
        if (carrier == null) {
            CombinedStatusHomeRenderSession.onControlCenterAuthorityChanged(true)
            logDiagnostic(
                level = Log.WARN,
                event = "projection.attach",
                component = "controlCenterProjection",
                state = "unavailable",
                "reason" to "fake-presentation-root-unresolved",
                "fallback" to "native-control-center",
            )
            return effectiveSourceScene
        }

        when (prepareControlCenterFakePresentation(carrier, "visible-fallback")) {
            CombinedStatusControlCenterRenderSession.AttachResult.Ready -> {
                val ready =
                    CombinedStatusControlCenterRenderSession.setRequestedVisible(true)
                if (!ready) {
                    CombinedStatusHomeRenderSession.onControlCenterAuthorityChanged(true)
                }
            }

            is CombinedStatusControlCenterRenderSession.AttachResult.Failure -> {
                CombinedStatusHomeRenderSession.onControlCenterAuthorityChanged(true)
                logDiagnostic(
                    level = Log.WARN,
                    event = "projection.attach",
                    component = "controlCenterProjection",
                    state = "unavailable",
                    "reason" to "fake-presentation-prepare-failed",
                    "fallback" to "home-visible",
                )
            }
        }
        return effectiveSourceScene
    }

    private fun updateControlCenterSourceSceneEligibility(
        sourceScene: CombinedStatusSourceScene,
        authority: String,
    ) {
        if (
            keyguardControlCenterLeaseActive &&
            sourceScene != CombinedStatusSourceScene.KEYGUARD
        ) {
            releaseKeyguardControlCenterLease(
                source = "source-scene:" + sourceScene.name + ":" + authority,
                reconcileReadiness = true,
            )
        }
        controlCenterSourceScene = sourceScene
        acquireKeyguardControlCenterLeaseIfEligible(
            source = "source-scene:" + authority,
        )
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val incomingBoundaryReady =
            incomingKeyguardPresentationReadyForControlCenter()
        val keyguardPresentationReady =
            keyguardRuntimeReady || incomingBoundaryReady
        val keyguardEligible =
            settings.enabled &&
                settings.keyguardEnabled &&
                keyguardPresentationReady
        val nextEligible =
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = settings.enabled,
                sourceScene = sourceScene,
                keyguardEnabled = keyguardEligible,
            )
        if (nextEligible == controlCenterSceneEligible) {
            return
        }

        controlCenterSceneEligible = nextEligible
        CombinedStatusControlCenterRenderSession.setSceneEligible(nextEligible)
        CombinedStatusControlCenterTransitionOwner.setSceneEligible(nextEligible)
        logDiagnostic(
            level = Log.INFO,
            event = "scene.eligibility",
            component = "controlCenterProjection",
            state = if (nextEligible) "eligible" else "native",
            "sourceScene" to sourceScene.name,
            "authority" to authority,
            "keyguardEnabled" to settings.keyguardEnabled,
            "keyguardRuntimeReady" to keyguardRuntimeReady,
            "incomingBoundaryReady" to incomingBoundaryReady,
            "keyguardPresentationReady" to keyguardPresentationReady,
            "controlCenterVisible" to controlCenterSceneVisible,
            "fallback" to if (nextEligible) "combined-qs-fake" else "native-qs-fake",
            "nativeGeometryWrites" to 0,
        )
    }

    private fun onControlCenterExpansionFraction(rawFraction: Float) {
        val fraction = rawFraction.coerceIn(0f, 1f)
        val previous = controlCenterExpansionFraction
        controlCenterExpansionFraction = fraction

        if (fraction > 0f) {
            if (
                controlCenterSourceScene != CombinedStatusSourceScene.KEYGUARD &&
                incomingKeyguardPresentationReadyForControlCenter()
            ) {
                updateControlCenterSourceSceneEligibility(
                    sourceScene = CombinedStatusSourceScene.KEYGUARD,
                    authority = "incoming-keyguard-fraction",
                )
            }
            acquireKeyguardControlCenterLeaseIfEligible(
                source = "native-fraction",
            )
            return
        }

        if (
            keyguardControlCenterLeaseActive &&
            previous > 0f
        ) {
            releaseKeyguardControlCenterLease(
                source = "native-fraction-zero",
                reconcileReadiness = true,
            )
        }
    }

    private fun acquireKeyguardControlCenterLeaseIfEligible(source: String) {
        val keyguardPresentationReady =
            keyguardRuntimeReady ||
                incomingKeyguardPresentationReadyForControlCenter()
        if (
            keyguardControlCenterLeaseActive ||
            !CombinedStatusScenePolicy.shouldAcquireKeyguardControlCenterLease(
                sourceScene = controlCenterSourceScene,
                keyguardPresentationReady = keyguardPresentationReady,
                nativeFraction = controlCenterExpansionFraction,
            )
        ) {
            return
        }

        keyguardControlCenterLeaseActive = true
        logDiagnostic(
            level = Log.INFO,
            event = "presentation.lease",
            component = "keyguardControlCenter",
            state = "acquired",
            "source" to source,
            "sourceScene" to controlCenterSourceScene.name,
            "nativeFraction" to controlCenterExpansionFraction,
            "cleanupBoundary" to "native-fraction-zero-or-authoritative-source-change",
            "timingDelay" to false,
            "nativeGeometryWrites" to 0,
        )
    }

    private fun shouldRetainKeyguardControlCenterLease(): Boolean {
        val resolved =
            SystemUiKeyguardHostResolver.current()
                as? SystemUiKeyguardHostResolver.ResolveResult.Ready
                ?: return false
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val aodBlocked =
            SystemUiKeyguardAodStateSource
                .currentState(resolved.host.battery)
                ?.blocksProjection
                ?: true
        val incomingBoundaryReady =
            incomingKeyguardPresentationReadyForControlCenter()
        return CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
            leaseActive = keyguardControlCenterLeaseActive,
            sourceScene = controlCenterSourceScene,
            featureEnabled = settings.enabled,
            keyguardEnabled = settings.keyguardEnabled,
            hostAttached = resolved.host.systemIcons.isAttachedToWindow,
            aodBlocked = aodBlocked,
            incomingBoundaryPresentationReady = incomingBoundaryReady,
            nativeFraction = controlCenterExpansionFraction,
        )
    }

    private fun releaseKeyguardControlCenterLease(
        source: String,
        reconcileReadiness: Boolean,
    ) {
        if (!keyguardControlCenterLeaseActive) return
        keyguardControlCenterLeaseActive = false
        logDiagnostic(
            level = Log.INFO,
            event = "presentation.lease",
            component = "keyguardControlCenter",
            state = "released",
            "source" to source,
            "sourceScene" to controlCenterSourceScene.name,
            "nativeFraction" to controlCenterExpansionFraction,
            "observedReady" to keyguardPresentationReadyObserved,
            "reconcileReadiness" to reconcileReadiness,
            "timingDelay" to false,
            "nativeGeometryWrites" to 0,
        )
        if (
            reconcileReadiness &&
            !keyguardPresentationReadyObserved &&
            !incomingKeyguardPresentationReadyForControlCenter()
        ) {
            applyKeyguardPresentationReadinessLost(
                source = "lease-release:" + source,
            )
        }
    }

    private fun incomingKeyguardPresentationReadyForControlCenter(): Boolean {
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        if (
            !settings.enabled ||
            !settings.keyguardEnabled ||
            settings.aodEnabled
        ) {
            return false
        }
        val resolved =
            SystemUiKeyguardHostResolver.current()
                as? SystemUiKeyguardHostResolver.ResolveResult.Ready
                ?: return false
        return CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
            visualHandoffActive = keyguardBoundaryVisualHandoffActive,
            layoutPrecommitActive = keyguardBoundaryLayoutPrecommitActive,
            compactLayoutReady = keyguardBoundaryCompactLayoutReady,
            visualBoundaryReached = keyguardBoundaryVisualBoundaryReached,
            hostAttached = resolved.host.systemIcons.isAttachedToWindow,
        )
    }

    private fun refreshControlCenterSourceSceneEligibility(authority: String) {
        updateControlCenterSourceSceneEligibility(
            sourceScene = controlCenterSourceScene,
            authority = authority,
        )
    }

    private fun reconcileControlCenterForKeyguardLifecycle(authority: String) {
        if (
            !CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = controlCenterSceneVisible,
                nativeFraction = controlCenterExpansionFraction,
                leaseActive = keyguardControlCenterLeaseActive,
            )
        ) {
            return
        }
        refreshControlCenterSourceSceneEligibility(authority)
    }

    private fun restoreControlCenterFakePresentationAfterHotReload(
        host: ViewGroup,
        transferredCompactReady: Boolean,
    ): String {
        return when (
            val result =
                CombinedStatusControlCenterRenderSession.restoreLaidOutHostAfterHotReload(
                    host = host,
                    onEvent = ::onPanelTransitionEvent,
                    isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                    onProjectionReadinessChanged = ::onControlCenterProjectionReadinessChanged,
                    transferredCompactReady = transferredCompactReady,
                )
        ) {
            CombinedStatusControlCenterRenderSession.AttachResult.Ready -> {
                val compactReady =
                    CombinedStatusControlCenterRenderSession
                        .currentNativePresentationReadyForHotReload()
                logDiagnostic(
                    level = Log.INFO,
                    event = "projection.restore",
                    component = "controlCenterProjection",
                    state = if (compactReady) "ready" else "prepared",
                    "source" to "hot-reload-transfer",
                    "boundary" to "outside-native-layout",
                    "transferredCompactReady" to transferredCompactReady,
                    "next" to
                        if (compactReady) {
                            "native-status-icons-layout-refresh"
                        } else {
                            "native-status-icons-layout"
                        },
                    "nativeGeometryWrites" to 0,
                )
                if (compactReady) {
                    "restored-laid-out-compact-ready"
                } else {
                    "restored-laid-out-native-layout-pending"
                }
            }

            is CombinedStatusControlCenterRenderSession.AttachResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "projection.restore",
                    component = "controlCenterProjection",
                    state = "fallback",
                    "source" to "hot-reload-transfer",
                    "reason" to result.reason,
                    "next" to "first-native-layout-prearm",
                    "nativeGeometryWrites" to 0,
                )
                onControlCenterFakePresentationAttached(host)
                "fallback-first-native-layout:" + result.reason
            }
        }
    }

    private fun onControlCenterFakePresentationAttached(host: ViewGroup) {
        when (
            val result =
                CombinedStatusControlCenterRenderSession.prearmAfterNextNativeLayout(
                    host = host,
                    onEvent = ::onPanelTransitionEvent,
                    isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                    onProjectionReadinessChanged = ::onControlCenterProjectionReadinessChanged,
                )
        ) {
            is CombinedStatusControlCenterRenderSession.PrearmResult.Scheduled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "projection.prearm",
                    component = "controlCenterProjection",
                    state = "scheduled",
                    "source" to "fake-root-attached",
                    "boundary" to "first-native-layout",
                    "reused" to result.reused,
                    "requestedVisible" to controlCenterSceneVisible,
                    "nativeGeometryWrites" to 0,
                )
            }

            is CombinedStatusControlCenterRenderSession.PrearmResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "projection.prearm",
                    component = "controlCenterProjection",
                    state = "unavailable",
                    "source" to "fake-root-attached",
                    "reason" to result.reason,
                    "fallback" to "native-qs-fake",
                )
            }
        }
    }

    private fun prepareControlCenterFakePresentation(
        host: ViewGroup,
        source: String,
    ): CombinedStatusControlCenterRenderSession.AttachResult {
        val result =
            CombinedStatusControlCenterRenderSession.attach(
                host = host,
                onEvent = ::onPanelTransitionEvent,
                isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                onProjectionReadinessChanged = ::onControlCenterProjectionReadinessChanged,
            )
        if (result is CombinedStatusControlCenterRenderSession.AttachResult.Failure) {
            logDiagnostic(
                level = Log.WARN,
                event = "projection.prepare",
                component = "controlCenterProjection",
                state = "unavailable",
                "source" to source,
                "reason" to result.reason,
                "fallback" to "native-qs-fake",
            )
        }
        return result
    }

    private fun onControlCenterProjectionReadinessChanged(ready: Boolean) {
        CombinedStatusControlCenterTransitionOwner.onProjectionReadinessChanged(ready)
        if (!controlCenterSceneVisible) {
            return
        }
        // Projected owner is already visible when ready=true. On the reverse
        // edge Home is restored before the projected owner is removed.
        CombinedStatusHomeRenderSession.onControlCenterAuthorityChanged(!ready)
    }

    private fun onPanelTransitionEvent(event: String) {
        if (detailedDiagnosticsEnabled) {
            log(Log.INFO, TAG, event)
        }
    }

    private fun onPanelTransitionRuntimeFailure(error: Throwable) {
        fun safely(block: () -> Unit) {
            try {
                block()
            } catch (cleanupError: Throwable) {
                if (
                    cleanupError is VirtualMachineError ||
                    cleanupError is ThreadDeath
                ) {
                    throw cleanupError
                }
            }
        }

        if (keyguardControlCenterLeaseActive) {
            safely {
                releaseKeyguardControlCenterLease(
                    source = "panel-runtime-failure",
                    reconcileReadiness = false,
                )
            }
        }
        controlCenterSceneEligible = false
        controlCenterSourceScene = CombinedStatusSourceScene.UNKNOWN
        safely {
            CombinedStatusControlCenterTransitionOwner.setSceneEligible(false)
        }
        safely {
            CombinedStatusControlCenterTransitionOwner.detach("panel-runtime-failure")
        }
        safely {
            CombinedStatusControlCenterRenderSession.setSceneEligible(false)
        }
        safely {
            CombinedStatusHomeRenderSession.onControlCenterAuthorityChanged(true)
        }
        safely {
            logDiagnostic(
                level = Log.ERROR,
                event = "runtime.callback",
                component = "panelTransition",
                state = "fail-native",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "fallback" to "native-control-center",
            )
            log(Log.ERROR, TAG, "Panel transition runtime callback failed", error)
        }
    }

    private fun installBatteryStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiBatteryRuntimeOwner.attach(
                module = this,
                classLoader = classLoader,
                onBatteryState = { state ->
                    val trace = beginRenderTrace("battery")
                    CombinedStatusStateStore.updateBattery(state)?.let { snapshot ->
                        onCombinedStateChanged(
                            snapshot = snapshot,
                            trace = markStateCommitted(trace),
                        )
                    }
                },
                onChargingIconResource = { resourceId ->
                    val trace = beginRenderTrace("battery-charging-glyph")
                    CombinedStatusStateStore.updateBatteryChargingIcon(resourceId)
                        ?.let { snapshot ->
                            onCombinedStateChanged(
                                snapshot = snapshot,
                                trace = markStateCommitted(trace),
                            )
                        }
                },
                onEvent =
                    if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                        { event ->
                            if (detailedDiagnosticsEnabled) {
                                log(Log.INFO, TAG, event)
                            }
                        }
                    } else {
                        null
                    },
            )
        }.onSuccess { result ->
            logDiagnostic(
                level = if (result.ready) Log.INFO else Log.WARN,
                event = "source.install",
                component = "batteryState",
                state = if (result.ready) "ready" else "partial",
                "hooks" to result.hooks,
                "expectedHooks" to SystemUiBatteryStateSource.HOOK_COUNT,
                "source" to source,
                "authority" to
                    "MiuiBatteryMeterIconView.getProgressStatus() via " +
                    "BatteryController callbacks",
                "eventDriven" to true,
            )
        }.onFailure { error ->
            SystemUiBatteryRuntimeOwner.resetRuntimeState()
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "batteryState",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
            )
            log(Log.ERROR, TAG, "Battery state source installation failed", error)
        }
    }

    private fun installPresentationRuntimeSources(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiPresentationRuntimeOwner.attach(
                module = this,
                classLoader = classLoader,
                onTintState = ::onTintStateUpdate,
                onSceneState = ::onSceneStateUpdate,
                onKeyguardAodState = ::onKeyguardAodStateUpdate,
                onKeyguardFullAodTransitionStarted = ::onKeyguardFullAodTransitionStarted,
                onKeyguardFullAodTransitionCommitted = ::onKeyguardFullAodTransitionCommitted,
                onKeyguardStatusIconTransition = ::onKeyguardStatusIconTransition,
                onMobileTypeChanged = { drawable ->
                    refreshMobilePresentation(
                        trace = beginRenderTrace("mobileType"),
                        pendingMobileTypeDrawable = drawable,
                    )
                },
                onTintEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onTintSourceEvent else null,
                onSceneEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onSceneSourceEvent else null,
                onKeyguardAodEvent =
                    if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                        { event ->
                            if (detailedDiagnosticsEnabled) {
                                log(Log.INFO, TAG, event)
                            }
                        }
                    } else {
                        null
                    },
            )
        }.onSuccess { result ->
            logDiagnostic(
                level =
                    if (result.tintReady && result.sceneReady && result.mobileTypeReady) {
                        Log.INFO
                    } else {
                        Log.WARN
                    },
                event = "source.install",
                component = "presentationRuntime",
                state =
                    if (result.tintReady && result.sceneReady && result.mobileTypeReady) {
                        "ready"
                    } else {
                        "partial"
                    },
                "tintHooks" to result.tintHooks,
                "sceneHooks" to result.sceneHooks,
                "mobileTypeHooks" to result.mobileTypeHooks,
                "keyguardAodHooks" to result.keyguardAodHooks,
                "keyguardAodReady" to result.keyguardAodReady,
                "keyguardFullAodHooks" to result.keyguardFullAodHooks,
                "keyguardFullAodReady" to result.keyguardFullAodReady,
                "keyguardStatusIconHooks" to result.keyguardStatusIconHooks,
                "keyguardStatusIconReady" to result.keyguardStatusIconReady,
                "source" to source,
                "nativeGeometryWrites" to 0,
            )
            if (result.keyguardAodReady) {
                SystemUiKeyguardHostResolver.current()?.let { resolution ->
                    onKeyguardHostResolution(
                        resolution = resolution,
                        source = "aod-authority-ready",
                    )
                }
            }
        }.onFailure { error ->
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "presentationRuntime",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
                "nativeGeometryWrites" to 0,
            )
            log(Log.ERROR, TAG, "Presentation runtime source installation failed", error)
        }
    }

    private fun refreshMobilePresentation(
        trace: RuntimeRenderTrace? = null,
        pendingMobileTypeDrawable: Drawable? = null,
    ) {
        val presentation =
            NativePresentationResolver.resolve(
                state = CombinedStatusStateStore.snapshot(),
                pendingMobileTypeDrawable = pendingMobileTypeDrawable,
            )
        val changed =
            CombinedStatusPresentationStateStore.updateMobilePresentation(presentation)
        val recoveryCompleted =
            CombinedStatusStateStore.completeMobileRecoveryIfReady(
                preferredSubscriptionId = presentation.effectiveDataSubscriptionId ?: -1,
                mobileTypeReady = presentation.networkType != null,
                mobileDataEnabled =
                    CombinedStatusPresentationStateStore
                        .snapshot()
                        .connectivity
                        .mobileDataEnabled,
            )
        if (changed != null || recoveryCompleted != null) {
            val presentationTrace = markPresentationCommitted(trace)
            if (detailedDiagnosticsEnabled && changed != null) {
                log(Log.INFO, TAG, presentation.logLine)
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.resolve",
                    component = "mobilePresentation",
                    state = "ready",
                    "mode" to presentation.mode.name,
                    "boundRoots" to presentation.boundRoots,
                    "visibleRoots" to presentation.visibleRoots,
                    "activeSubIds" to presentation.activeSubscriptionIds.joinToString(","),
                    "presentationRootSubId" to presentation.presentationRootSubscriptionId,
                    "effectiveDataSubId" to presentation.effectiveDataSubscriptionId,
                    "networkTypeSubId" to presentation.networkTypeSubscriptionId,
                    "networkType" to presentation.networkType?.label,
                    "enhanced" to presentation.networkType?.enhanced,
                    "geometryWrites" to 0,
                )
            }
            if (detailedDiagnosticsEnabled && recoveryCompleted != null) {
                logDiagnostic(
                    level = Log.INFO,
                    event = "mobile.recovery",
                    component = "network",
                    state = "ready",
                    "effectiveDataSubId" to presentation.effectiveDataSubscriptionId,
                    "networkType" to presentation.networkType?.label,
                    "eventDriven" to true,
                )
            }
            onPresentationStateChanged(
                presentationTrace,
            )
        }
    }

    private fun onSceneSourceEvent(event: String) {
        if (detailedDiagnosticsEnabled) {
            log(Log.INFO, TAG, event)
        }
    }

    private fun onTintSourceEvent(event: String) {
        if (detailedDiagnosticsEnabled) {
            log(Log.INFO, TAG, event)
        }
    }

    private fun onNetworkPipelineEvent(event: String) {
        if (detailedDiagnosticsEnabled) {
            log(Log.INFO, TAG, event)
        }
    }

    private fun onCombinedStateChanged(
        snapshot: CombinedStatusStateStore.Snapshot,
        trace: RuntimeRenderTrace? = null,
    ) {
        CombinedStatusHomeRenderSession.onState(snapshot, trace)
        CombinedStatusKeyguardRenderSession.onState(snapshot)
        CombinedStatusControlCenterRenderSession.onState(snapshot)
    }

    private fun onPresentationStateChanged(trace: RuntimeRenderTrace? = null) {
        CombinedStatusHomeRenderSession.onPresentationStateChanged(trace)
        CombinedStatusKeyguardRenderSession.onPresentationStateChanged()
        CombinedStatusControlCenterRenderSession.onPresentationStateChanged()
        refreshStatusIconObservation("presentation")
    }

    private fun refreshStatusIconObservation(source: String) {
        SystemUiNativeNetworkSuppressionOwner.refreshObservation(source)
    }

    private fun onStatusIconPresentationChanged(
        state: CombinedStatusPresentationStateStore.StatusIconPresentation,
    ) {
        val trace = beginRenderTrace("statusIcons")
        val changed =
            CombinedStatusPresentationStateStore.updateStatusIcons(state)

        CombinedStatusHomeRenderSession.onStatusIconTintUpdate(
            state.appliedTint,
        )
        CombinedStatusKeyguardRenderSession.onPresentationStateChanged()
        CombinedStatusControlCenterRenderSession.onPresentationStateChanged()

        if (changed != null) {
            val presentationTrace = markPresentationCommitted(trace)
            CombinedStatusHomeRenderSession.onPresentationStateChanged(
                presentationTrace,
            )
            if (detailedDiagnosticsEnabled) {
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.resolve",
                    component = "statusIcons",
                    state = "ready",
                    "tint" to
                        (
                            state.appliedTint
                                ?.toUInt()
                                ?.toString(16)
                                ?.padStart(8, '0')
                                ?: "none"
                        ),
                    "noSimVisible" to state.noSimVisible,
                    "noSimPackage" to state.noSimIcon?.packageName,
                    "noSimResId" to state.noSimIcon?.resourceId,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun onTintStateUpdate(update: SystemUiTintStateSource.TintUpdate) {
        CombinedStatusKeyguardRenderSession.onTintUpdate(update)
        val liveStatusIconTint =
            SystemUiNativeNetworkSuppressionOwner.currentAppliedStatusIconTint()
        val resolvedState =
            CombinedStatusTintAuthority.resolveBatteryEvent(
                batteryState = update.state,
                liveStatusIconTint = liveStatusIconTint,
            )
        val resolvedUpdate = update.copy(state = resolvedState)
        CombinedStatusHomeRenderSession.onTintUpdate(resolvedUpdate)
        CombinedStatusControlCenterRenderSession.onTintUpdate(resolvedUpdate)
        if (detailedDiagnosticsEnabled) {
            log(
                Log.INFO,
                TAG,
                "tintCommit source=batteryDarkReceiver" +
                    " applied=#" +
                    resolvedState.appliedTint.toUInt().toString(16).padStart(8, '0') +
                    " statusIcon=#" +
                    (
                        resolvedState.statusIconTint
                            ?.toUInt()
                            ?.toString(16)
                            ?.padStart(8, '0')
                            ?: "none"
                    ) +
                    " liveStatusIcon=#" +
                    (
                        liveStatusIconTint
                            ?.toUInt()
                            ?.toString(16)
                            ?.padStart(8, '0')
                            ?: "none"
                    ) +
                    " authority=live-systemui-status-icons",
            )
        }
    }

    private fun onKeyguardFullAodTransitionStarted() {
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val homeOwnedAtStart =
            SystemUiHomePresentationOwner
                .currentHomeRepresentedSlotOwnership()
                .isNotEmpty()
        val homeCarrierVisibleAtStart =
            SystemUiHomePresentationOwner
                .currentHomeCarrierPresentationVisible()

        keyguardAodFullTransitionActive = true
        keyguardAodPendingTargetToLockScreen = null
        homePresentationOwnedAtFullAodStart = homeOwnedAtStart
        if (
            CombinedStatusScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguardEnabled,
                aodEnabled = settings.aodEnabled,
                homePresentationOwned = homeOwnedAtStart,
                homeCarrierPresentationVisible = homeCarrierVisibleAtStart,
            )
        ) {
            homeNativeAodFallbackCandidate = true
        }
        homeAodTransitionOriginPending =
            settings.enabled &&
                steadyStatusSourceScene == CombinedStatusSourceScene.HOME &&
                lastStableKeyguardAodScene ==
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN &&
                homeOwnedAtStart &&
                (settings.keyguardEnabled || settings.aodEnabled)
        keyguardAodFullTargetPending =
            SystemUiPresentationRuntimeOwner.keyguardStatusIconReady &&
                lastStableKeyguardAodScene !=
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN &&
                settings.keyguardEnabled != settings.aodEnabled

        logDiagnostic(
            level = Log.INFO,
            event = "aod.targetWindow",
            component = "keyguardAod",
            state = if (keyguardAodFullTargetPending) "pending" else "observation-only",
            "source" to "animateFullAod:before",
            "visualBoundaryAuthority" to
                if (SystemUiPresentationRuntimeOwner.keyguardStatusIconReady) {
                    "native-animateIconContainer"
                } else {
                    "status-icons-alpha-fallback"
                },
            "homeOriginLatched" to homeAodTransitionOriginPending,
            "homeNativeAodFallbackCandidate" to homeNativeAodFallbackCandidate,
            "homeNativeAodFallbackActive" to homeNativeAodFallbackActive,
            "homePresentationOwnedAtStart" to homeOwnedAtStart,
            "homeCarrierVisibleAtStart" to homeCarrierVisibleAtStart,
            "eventDriven" to true,
            "readOnly" to true,
            "nativeGeometryWrites" to 0,
        )
    }

    private fun onKeyguardFullAodTransitionCommitted() {
        keyguardAodFullTransitionActive = false
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val resolution = SystemUiKeyguardHostResolver.current()
        val target =
            (resolution as? SystemUiKeyguardHostResolver.ResolveResult.Ready)
                ?.host
                ?.let { resolved ->
                    SystemUiKeyguardHostResolver.nativeToLockScreenTarget(resolved)
                }

        if (keyguardAodFullTargetPending) {
            keyguardAodPendingTargetToLockScreen = target
            if (target == null) {
                keyguardAodFullTargetPending = false
            }
        }
        if (target == true || target == null) {
            homeAodTransitionOriginPending = false
            homeAodTargetPrearmPending = false
        }
        if (target == true) {
            homeNativeAodFallbackActive = false
        } else if (target == null) {
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = false
        }

        logDiagnostic(
            level = if (target != null) Log.INFO else Log.WARN,
            event = "aod.target",
            component = "keyguardAod",
            state =
                when (target) {
                    true -> "keyguard"
                    false -> "aod"
                    null -> "unavailable"
                },
            "source" to "animateFullAod:after",
            "authority" to "native-mToLockScreen",
            "visualBoundaryPending" to keyguardAodFullTargetPending,
            "homeOriginLatched" to homeAodTransitionOriginPending,
            "homeNativeAodFallbackCandidate" to homeNativeAodFallbackCandidate,
            "homeNativeAodFallbackActive" to homeNativeAodFallbackActive,
            "eventDriven" to true,
            "readOnly" to true,
            "nativeGeometryWrites" to 0,
        )

        val releaseTransientHomeKeyguard =
            CombinedStatusScenePolicy.shouldReleaseTransientHomeKeyguardForDisabledAod(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguardEnabled,
                aodEnabled = settings.aodEnabled,
                homeNativeAodFallbackCandidate =
                    homeNativeAodFallbackCandidate,
                homePresentationOwnedAtFullAodStart =
                    homePresentationOwnedAtFullAodStart,
                nativeToLockScreenTarget = target,
            )
        if (releaseTransientHomeKeyguard) {
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = true
            homeAodTransitionOriginPending = false
            homeAodTargetPrearmPending = false
            resetKeyguardBoundaryHandoffState()
            deactivateKeyguardRuntime("home-aod-disabled-target")
            logDiagnostic(
                level = Log.INFO,
                event = "aod.homeTransientKeyguard",
                component = "keyguardPresentation",
                state = "released",
                "source" to "animateFullAod:after",
                "target" to "native-aod",
                "authority" to
                    "home-full-aod-candidate+home-owner+native-mToLockScreen",
                "nativeGeometryWrites" to 0,
            )
        }

        (resolution as? SystemUiKeyguardHostResolver.ResolveResult.Ready)?.let { ready ->
            if (!releaseTransientHomeKeyguard) {
                armKeyguardBoundaryVisualHandoffIfEligible(
                    resolution = ready,
                    nativeToLockScreenTarget = target,
                    source = "animateFullAod:after",
                    visualBoundaryReached = false,
                )
            }
            val prearmed =
                armHomeAodTargetPrearmIfEligible(
                    resolution = ready,
                    nativeToLockScreenTarget = target,
                    source = "animateFullAod:after",
                )
            if (
                homeAodTransitionOriginPending &&
                target == false &&
                !prearmed
            ) {
                onKeyguardHostResolution(
                    resolution = ready,
                    source = "home-aod-origin:animateFullAod:after",
                )
            }
        }
        homePresentationOwnedAtFullAodStart = false
    }

    private fun onKeyguardStatusIconTransition() {
        val resolution =
            SystemUiKeyguardHostResolver.current()
                as? SystemUiKeyguardHostResolver.ResolveResult.Ready
                ?: run {
                    keyguardAodFullTargetPending = false
                    keyguardAodPendingTargetToLockScreen = null
                    homePresentationOwnedAtFullAodStart = false
                    homeNativeAodFallbackCandidate = false
                    homeNativeAodFallbackActive = false
                    homeAodTransitionOriginPending = false
                    homeAodTargetPrearmPending = false
                    return
                }
        val aodState =
            SystemUiKeyguardAodStateSource.currentState(resolution.host.battery)
        val target =
            SystemUiKeyguardHostResolver.nativeToLockScreenTarget(resolution.host)

        if (keyguardAodFullTargetPending && target != null) {
            keyguardAodPendingTargetToLockScreen = target
        }

        val prearmed =
            armHomeAodTargetPrearmIfEligible(
                resolution = resolution,
                nativeToLockScreenTarget = target,
                source = "animateIconContainer",
            )
        if (
            homeAodTransitionOriginPending &&
            target == false &&
            !prearmed
        ) {
            onKeyguardHostResolution(
                resolution = resolution,
                source = "home-aod-origin:animateIconContainer",
            )
        }

        if (!keyguardAodFullTargetPending) {
            return
        }

        val eligible = target != null
        logDiagnostic(
            level = if (eligible) Log.INFO else Log.WARN,
            event = "aod.visualBoundary",
            component = "keyguardAod",
            state = if (eligible) "ready" else "ignored",
            "source" to "animateIconContainer",
            "target" to
                when (target) {
                    true -> "keyguard"
                    false -> "aod"
                    null -> "unavailable"
                },
            "isAodAnimate" to aodState?.isAodAnimate,
            "statusIconsAlpha" to
                SystemUiKeyguardHostResolver.statusIconsPresentationAlpha(
                    resolution.host,
                ),
            "authority" to "native-status-icon-animation",
            "eventDriven" to true,
            "readOnly" to true,
            "nativeGeometryWrites" to 0,
        )
        if (!eligible) return

        val visualOnlyIncomingKeyguard =
            armKeyguardBoundaryVisualHandoffIfEligible(
                resolution = resolution,
                nativeToLockScreenTarget = target,
                source = "status-icon-animation",
                visualBoundaryReached = true,
            )
        if (!visualOnlyIncomingKeyguard) {
            onKeyguardHostResolution(
                resolution = resolution,
                source = "status-icon-animation",
                fullAodVisualBoundary = true,
            )
        }
        keyguardAodFullTargetPending = false
        keyguardAodPendingTargetToLockScreen = null
    }

    private fun armKeyguardBoundaryVisualHandoffIfEligible(
        resolution: SystemUiKeyguardHostResolver.ResolveResult.Ready,
        nativeToLockScreenTarget: Boolean?,
        source: String,
        visualBoundaryReached: Boolean,
    ): Boolean {
        if (keyguardBoundaryVisualHandoffActive) {
            if (visualBoundaryReached) {
                onKeyguardBoundaryVisualBoundaryReached(source)
            }
            return true
        }

        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val eligible =
            CombinedStatusScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguardEnabled,
                aodEnabled = settings.aodEnabled,
                lastStableFamilyScene = lastStableKeyguardAodScene,
                nativeToLockScreenTarget = nativeToLockScreenTarget,
                homeNativeAodFallbackActive = homeNativeAodFallbackActive,
            )
        if (!eligible) return false

        beginKeyguardBoundaryVisualHandoff(
            resolution = resolution,
            source = source,
            visualBoundaryReached = visualBoundaryReached,
        )
        return keyguardBoundaryVisualHandoffActive
    }

    private fun beginKeyguardBoundaryVisualHandoff(
        resolution: SystemUiKeyguardHostResolver.ResolveResult.Ready,
        source: String,
        visualBoundaryReached: Boolean,
    ) {
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        keyguardBoundaryVisualHandoffActive = true
        val statusIconsAlphaAtArm =
            SystemUiKeyguardHostResolver.statusIconsPresentationAlpha(
                resolution.host,
            )
        keyguardBoundaryLayoutPrecommitActive =
            CombinedStatusScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguardEnabled,
                aodEnabled = settings.aodEnabled,
                lastStableFamilyScene = lastStableKeyguardAodScene,
                nativeToLockScreenTarget =
                    SystemUiKeyguardHostResolver.nativeToLockScreenTarget(
                        resolution.host,
                    ),
                statusIconsPresentationAlpha = statusIconsAlphaAtArm,
                homeNativeAodFallbackActive = homeNativeAodFallbackActive,
            )
        keyguardBoundaryCompactLayoutReady = false
        keyguardBoundaryVisualBoundaryReached = visualBoundaryReached

        val attached =
            attachKeyguardRenderer(
                resolved = resolution.host,
                source = source + ":visual-only",
            )
        if (!attached) {
            resetKeyguardBoundaryHandoffState()
            return
        }
        logDiagnostic(
            level = Log.INFO,
            event = "aod.visualHandoff",
            component = "keyguardPresentation",
            state = "armed",
            "source" to source,
            "nativeLayoutOwnership" to
                if (keyguardBoundaryLayoutPrecommitActive) {
                    "precommit-before-reveal"
                } else {
                    "deferred-until-stable"
                },
            "nativeVisualMask" to "clipBounds",
            "renderer" to "keyguard-combined",
            "statusIconsAlphaAtArm" to statusIconsAlphaAtArm,
            "nativeLifecycleAuthority" to "status-icons-presentation-alpha",
            "nativeGeometryWrites" to 0,
        )
    }

    private fun precommitKeyguardBoundaryLayout(source: String) {
        if (
            !keyguardBoundaryVisualHandoffActive ||
            !keyguardBoundaryLayoutPrecommitActive
        ) {
            return
        }
        when (
            val result =
                SystemUiHomePresentationOwner.commitKeyguardDeferredLayoutOwnership()
        ) {
            is SystemUiHomePresentationOwner.StateResult.Active -> {
                onKeyguardBoundaryPrelayoutReady(
                    result = result,
                    source = source + ":precommit-ready",
                )
            }

            is SystemUiHomePresentationOwner.StateResult.Prepared -> {
                keyguardRuntimeReady = false
                CombinedStatusKeyguardRenderSession.setNativeHandoffActive(true)
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.cutover",
                    component = "keyguardPresentation",
                    state = "prelayout-pending",
                    "source" to source,
                    "representedSlots" to result.representedSlots,
                    "reused" to result.reused,
                    "next" to "native-status-icons-layout-before-reveal",
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiHomePresentationOwner.StateResult.Failure -> {
                onKeyguardPresentationRuntimeFailure(result.reason)
                deactivateKeyguardRuntime("boundary-prelayout-commit-failed")
            }

            is SystemUiHomePresentationOwner.StateResult.Inactive -> {
                deactivateKeyguardRuntime("boundary-prelayout-session-missing")
            }
        }
    }

    private fun onKeyguardBoundaryPrelayoutReady(
        result: SystemUiHomePresentationOwner.StateResult.Active,
        source: String,
    ) {
        if (
            !keyguardBoundaryVisualHandoffActive ||
            !keyguardBoundaryLayoutPrecommitActive
        ) {
            completeKeyguardPresentationCutover(
                result = result,
                source = source,
            )
            return
        }
        keyguardBoundaryCompactLayoutReady = true
        keyguardRuntimeReady = false
        CombinedStatusKeyguardRenderSession.setNativeHandoffActive(
            !keyguardBoundaryVisualBoundaryReached,
        )
        logDiagnostic(
            level = Log.INFO,
            event = "presentation.cutover",
            component = "keyguardPresentation",
            state =
                if (keyguardBoundaryVisualBoundaryReached) {
                    "visual-handoff-prelayout-ready"
                } else {
                    "prelayout-ready-hidden"
                },
            "source" to source,
            "representedSlots" to result.representedSlots,
            "maskedViews" to result.maskedViews,
            "nativeVisualBoundaryReached" to keyguardBoundaryVisualBoundaryReached,
            "nativeGeometryWrites" to 0,
        )
        if (keyguardBoundaryVisualBoundaryReached) {
            reconcileControlCenterForKeyguardLifecycle(
                "keyguard-boundary-layout-ready",
            )
        }
    }

    private fun onKeyguardBoundaryVisualBoundaryReached(source: String) {
        keyguardBoundaryVisualBoundaryReached = true
        if (!keyguardBoundaryLayoutPrecommitActive) return
        if (keyguardBoundaryCompactLayoutReady) {
            CombinedStatusKeyguardRenderSession.setNativeHandoffActive(false)
            logDiagnostic(
                level = Log.INFO,
                event = "aod.visualHandoff",
                component = "keyguardPresentation",
                state = "revealed",
                "source" to source,
                "layoutAuthority" to "precommitted-before-native-animation",
                "nativeGeometryWrites" to 0,
            )
            reconcileControlCenterForKeyguardLifecycle(
                "keyguard-boundary-visual-ready",
            )
        } else {
            CombinedStatusKeyguardRenderSession.setNativeHandoffActive(true)
            logDiagnostic(
                level = Log.WARN,
                event = "aod.visualHandoff",
                component = "keyguardPresentation",
                state = "waiting-prelayout",
                "source" to source,
                "fallback" to "native-until-compact-layout-ready",
                "nativeGeometryWrites" to 0,
            )
        }
    }

    private fun resetKeyguardBoundaryHandoffState() {
        keyguardBoundaryVisualHandoffActive = false
        keyguardBoundaryLayoutPrecommitActive = false
        keyguardBoundaryCompactLayoutReady = false
        keyguardBoundaryVisualBoundaryReached = false
    }

    private fun completeKeyguardBoundaryVisualHandoff(source: String): Boolean {
        if (!keyguardBoundaryVisualHandoffActive) return false
        if (keyguardBoundaryLayoutPrecommitActive) {
            resetKeyguardBoundaryHandoffState()
            onKeyguardPresentationReadinessChanged(
                ready = true,
                source = source + ":precommitted-layout",
            )
            return true
        }
        keyguardBoundaryVisualHandoffActive = false
        return when (
            val result =
                SystemUiHomePresentationOwner.commitKeyguardDeferredLayoutOwnership()
        ) {
            is SystemUiHomePresentationOwner.StateResult.Active -> {
                completeKeyguardPresentationCutover(
                    result = result,
                    source = source + ":compact-ready",
                )
                true
            }

            is SystemUiHomePresentationOwner.StateResult.Prepared -> {
                keyguardRuntimeReady = false
                CombinedStatusKeyguardRenderSession.setNativeHandoffActive(false)
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.cutover",
                    component = "keyguardPresentation",
                    state = "visual-handoff-layout-pending",
                    "source" to source,
                    "representedSlots" to result.representedSlots,
                    "reused" to result.reused,
                    "nativeVisuals" to "masked",
                    "next" to "native-status-icons-layout",
                    "nativeGeometryWrites" to 0,
                )
                true
            }

            is SystemUiHomePresentationOwner.StateResult.Failure -> {
                onKeyguardPresentationRuntimeFailure(result.reason)
                deactivateKeyguardRuntime("boundary-layout-commit-failed")
                true
            }

            is SystemUiHomePresentationOwner.StateResult.Inactive -> {
                deactivateKeyguardRuntime("boundary-layout-session-missing")
                true
            }
        }
    }

    private fun armHomeAodTargetPrearmIfEligible(
        resolution: SystemUiKeyguardHostResolver.ResolveResult.Ready,
        nativeToLockScreenTarget: Boolean?,
        source: String,
    ): Boolean {
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val homeOwned =
            SystemUiHomePresentationOwner
                .currentHomeRepresentedSlotOwnership()
                .isNotEmpty()

        if (nativeToLockScreenTarget == true) {
            homeAodTransitionOriginPending = false
            homeAodTargetPrearmPending = false
            return false
        }

        if (homeAodTargetPrearmPending) {
            if (!settings.enabled || !settings.aodEnabled) {
                homeAodTargetPrearmPending = false
                return false
            }
            return true
        }

        val currentOriginEligible =
            CombinedStatusScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = settings.enabled,
                aodEnabled = settings.aodEnabled,
                steadySourceScene = steadyStatusSourceScene,
                lastStableFamilyScene = lastStableKeyguardAodScene,
                homePresentationOwned = homeOwned,
                nativeToLockScreenTarget = nativeToLockScreenTarget,
            )
        val eligible =
            settings.enabled &&
                settings.aodEnabled &&
                nativeToLockScreenTarget == false &&
                (homeAodTransitionOriginPending || currentOriginEligible)
        if (!eligible) return false

        homeAodTargetPrearmPending = true
        logDiagnostic(
            level = Log.INFO,
            event = "aod.homePrearm",
            component = "keyguardAod",
            state = "armed",
            "source" to source,
            "target" to "aod",
            "origin" to
                if (homeAodTransitionOriginPending) {
                    "latched-home"
                } else {
                    "home"
                },
            "homePresentationOwnedAtArm" to homeOwned,
            "nativeGeometryWrites" to 0,
        )
        onKeyguardHostResolution(
            resolution = resolution,
            source = "home-aod-target-prearm:" + source,
        )
        return true
    }

    private fun onKeyguardAodStateUpdate(
        update: SystemUiKeyguardAodStateSource.AodUpdate,
    ) {
        if (
            !update.isAodAnimate &&
            !keyguardAodFullTransitionActive &&
            CombinedStatusScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = keyguardAodPendingTargetToLockScreen,
                toAod = update.toAod,
                isAodAnimate = update.isAodAnimate,
            )
        ) {
            keyguardAodFullTargetPending = false
            keyguardAodPendingTargetToLockScreen = null
        }

        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val activateHomeNativeAodFallback =
            CombinedStatusScenePolicy.shouldConsumeHomeNativeAodFallbackOnAodState(
                candidateActive = homeNativeAodFallbackCandidate,
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguardEnabled,
                aodEnabled = settings.aodEnabled,
                toAod = update.toAod,
                isAodAnimate = update.isAodAnimate,
            )
        if (activateHomeNativeAodFallback) {
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = true
            resetKeyguardBoundaryHandoffState()
            deactivateKeyguardRuntime("home-aod-disabled-native-transition")
            logDiagnostic(
                level = Log.INFO,
                event = "aod.homeTransientKeyguard",
                component = "keyguardPresentation",
                state = "released",
                "source" to "aod:" + update.source,
                "target" to "native-aod",
                "authority" to
                    "home-full-aod-candidate+native-toAod-animation",
                "nativeGeometryWrites" to 0,
            )
        }

        val stableAod =
            SystemUiKeyguardAodStateSource.isStableAod(
                toAod = update.toAod,
                isAodAnimate = update.isAodAnimate,
            )
        if (!keyguardAodFullTransitionActive && stableAod) {
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = false
            homeAodTransitionOriginPending = false
            homeAodTargetPrearmPending = false
            if (keyguardBoundaryVisualHandoffActive) {
                resetKeyguardBoundaryHandoffState()
                deactivateKeyguardRuntime("boundary-handoff-returned-to-aod")
            }
        }

        refreshStableKeyguardAodSceneFromAodState(update)
        if (
            !update.isAodAnimate &&
            !update.toAod &&
            steadyStatusSourceScene == CombinedStatusSourceScene.KEYGUARD
        ) {
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = false
        }
        if (update.blocksProjection) {
            releaseKeyguardControlCenterLease(
                source = "aod:" + update.source,
                reconcileReadiness = false,
            )
        }
        val boundaryHandoffHandled =
            !update.isAodAnimate &&
                !update.toAod &&
                completeKeyguardBoundaryVisualHandoff(
                    source = "aod:" + update.source,
                )
        if (!boundaryHandoffHandled && !keyguardBoundaryVisualHandoffActive) {
            SystemUiKeyguardHostResolver.current()?.let { resolution ->
                onKeyguardHostResolution(
                    resolution = resolution,
                    source = "aod:" + update.source,
                )
            }
        }
        CombinedStatusKeyguardRenderSession.onAodState(update)
        if (detailedDiagnosticsEnabled) {
            logDiagnostic(
                level = Log.INFO,
                event = "aod.state",
                component = "keyguardAod",
                state =
                    when {
                        stableAod -> "aod-stable"
                        update.blocksProjection -> "native-transition"
                        else -> "keyguard-eligible"
                    },
                "source" to update.source,
                "toAod" to update.toAod,
                "isAodAnimate" to update.isAodAnimate,
                "animToAod" to update.animToAod,
                "blocksProjection" to update.blocksProjection,
                "pendingTarget" to
                    when (keyguardAodPendingTargetToLockScreen) {
                        true -> "keyguard"
                        false -> "aod"
                        null -> "none"
                    },
                "homeOriginLatched" to homeAodTransitionOriginPending,
                "homeNativeAodFallbackCandidate" to homeNativeAodFallbackCandidate,
                "homeNativeAodFallbackActive" to homeNativeAodFallbackActive,
                "nativeGeometryWrites" to 0,
            )
        }
    }

    private fun onSceneStateUpdate(update: SystemUiSceneStateSource.SceneUpdate) {
        val sourceScene = SystemUiSceneStateSource.steadySourceScene(update)
        if (sourceScene != CombinedStatusSourceScene.UNKNOWN) {
            steadyStatusSourceScene = sourceScene
        }
        refreshStableKeyguardAodSceneFromSceneState(update, sourceScene)
        if (sourceScene == CombinedStatusSourceScene.KEYGUARD) {
            SystemUiKeyguardHostResolver.observe(update)?.let { resolution ->
                onKeyguardHostResolution(
                    resolution = resolution,
                    source = "scene-state",
                )
            }
        }
        if (sourceScene == CombinedStatusSourceScene.HOME) {
            if (!keyguardAodFullTransitionActive) {
                homeNativeAodFallbackActive = false
            }
            // UNLOCKED_STATUS_BAR + Home ancestry is the authoritative unlock
            // boundary. A Keyguard Control Center lease must never outlive it:
            // otherwise a fast first pull-down can consume stale KEYGUARD
            // source state and temporarily fall back to native QS icons.
            if (keyguardControlCenterLeaseActive) {
                releaseKeyguardControlCenterLease(
                    source = "authoritative-home",
                    reconcileReadiness = false,
                )
            }
            updateControlCenterSourceSceneEligibility(
                sourceScene = CombinedStatusSourceScene.HOME,
                authority = "steady-source-view",
            )

            if (aodRendererAttached) {
                val retainAodHandoff =
                    SystemUiHomePresentationOwner.currentAodPresentationClaimed()
                if (!retainAodHandoff) {
                    deactivateAodRuntime("home-source-active")
                } else {
                    logDiagnostic(
                        level = Log.INFO,
                        event = "scene.defer",
                        component = "aodPresentation",
                        state = "retained",
                        "source" to "steady-source-view",
                        "observedScene" to sourceScene.name,
                        "reason" to "aod-to-home-continuous-handoff",
                        "nativeGeometryWrites" to 0,
                    )
                }
            }
            // Clear the old Keyguard renderer only after Control Center source
            // ownership has already moved to Home. Any readiness callback caused
            // by teardown therefore refreshes HOME eligibility, never stale
            // KEYGUARD eligibility.
            deactivateKeyguardRuntime("home-source-active")
        } else if (sourceScene != CombinedStatusSourceScene.UNKNOWN) {
            updateControlCenterSourceSceneEligibility(
                sourceScene = sourceScene,
                authority = "steady-source-view",
            )
        }

        if (BuildConfig.RUNTIME_DIAGNOSTICS) {
            SystemUiKeyguardHostProbe.capture(update)?.let(::onKeyguardHostProbe)
        }

        SystemUiTintStateSource.currentState(update.sourceView)?.let { state ->
            onTintStateUpdate(
                SystemUiTintStateSource.TintUpdate(
                    sourceView = update.sourceView,
                    state = state,
                ),
            )
        }
        if (
            update.surface ==
                SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR
        ) {
            refreshStatusIconObservation("scene-unlocked")
        }
    }

    private fun refreshStableKeyguardAodSceneFromAodState(
        update: SystemUiKeyguardAodStateSource.AodUpdate,
    ) {
        if (update.isAodAnimate) return
        val next =
            when {
                SystemUiKeyguardAodStateSource.isStableAod(
                    toAod = update.toAod,
                    isAodAnimate = update.isAodAnimate,
                ) ->
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD

                !update.toAod &&
                    steadyStatusSourceScene == CombinedStatusSourceScene.KEYGUARD ->
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD

                steadyStatusSourceScene == CombinedStatusSourceScene.HOME ->
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN

                else -> null
            }
        if (next != null) {
            updateStableKeyguardAodScene(next, "aod:" + update.source)
        }
    }

    private fun refreshStableKeyguardAodSceneFromSceneState(
        update: SystemUiSceneStateSource.SceneUpdate,
        sourceScene: CombinedStatusSourceScene,
    ) {
        when (sourceScene) {
            CombinedStatusSourceScene.HOME -> {
                val aodState = SystemUiKeyguardAodStateSource.currentState(update.sourceView)
                if (
                    aodState != null &&
                    !aodState.isAodAnimate &&
                    !aodState.toAod
                ) {
                    updateStableKeyguardAodScene(
                        CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
                        "scene-home-stable",
                    )
                }
            }

            CombinedStatusSourceScene.KEYGUARD -> {
                val aodState = SystemUiKeyguardAodStateSource.currentState(update.sourceView)
                if (
                    aodState != null &&
                    !aodState.isAodAnimate &&
                    !SystemUiKeyguardAodStateSource.isStableAod(
                        toAod = aodState.toAod,
                        isAodAnimate = aodState.isAodAnimate,
                    )
                ) {
                    updateStableKeyguardAodScene(
                        CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
                        "scene-keyguard",
                    )
                }
            }

            CombinedStatusSourceScene.UNKNOWN -> Unit
        }
    }

    private fun updateStableKeyguardAodScene(
        next: CombinedStatusScenePolicy.StableKeyguardAodScene,
        source: String,
    ) {
        if (next == lastStableKeyguardAodScene) return
        val previous = lastStableKeyguardAodScene
        lastStableKeyguardAodScene = next
        logDiagnostic(
            level = Log.INFO,
            event = "scene.stableFamily",
            component = "keyguardAod",
            state = next.name.lowercase(),
            "source" to source,
            "previous" to previous.name,
            "nativeGeometryWrites" to 0,
        )
    }

    private fun resolveCurrentKeyguardAodProjection(
        resolved: SystemUiKeyguardHostResolver.ResolvedHost,
        fullAodVisualBoundary: Boolean = false,
    ): CombinedStatusScenePolicy.KeyguardAodProjection? {
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val aodState =
            SystemUiKeyguardAodStateSource.currentState(resolved.battery)
                ?: return null
        return CombinedStatusScenePolicy.resolveKeyguardAodProjection(
            featureEnabled = settings.enabled,
            keyguardEnabled = settings.keyguardEnabled,
            aodEnabled = settings.aodEnabled,
            toAod = aodState.toAod,
            isAodAnimate = aodState.isAodAnimate,
            steadySourceScene = steadyStatusSourceScene,
            lastStableFamilyScene = lastStableKeyguardAodScene,
            homePresentationOwned =
                SystemUiHomePresentationOwner
                    .currentHomeRepresentedSlotOwnership()
                    .isNotEmpty(),
            keyguardStatusIconsAlpha =
                SystemUiKeyguardHostResolver
                    .statusIconsPresentationAlpha(resolved),
            nativeToLockScreenTarget =
                SystemUiKeyguardHostResolver
                    .nativeToLockScreenTarget(resolved),
            fullAodTargetSourceReady =
                SystemUiPresentationRuntimeOwner.keyguardFullAodReady,
            fullAodTargetPending = keyguardAodFullTargetPending,
            fullAodVisualBoundary = fullAodVisualBoundary,
            homeAodTransitionOrigin = homeAodTransitionOriginPending,
            homeAodTargetPrearm = homeAodTargetPrearmPending,
            homeNativeAodFallbackActive = homeNativeAodFallbackActive,
        )
    }

    private fun onKeyguardHostResolution(
        resolution: SystemUiKeyguardHostResolver.ResolveResult,
        source: String,
        fullAodVisualBoundary: Boolean = false,
    ) {
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        when (resolution) {
            is SystemUiKeyguardHostResolver.ResolveResult.Ready -> {
                if (!settings.enabled) {
                    deactivateAodRuntime("feature-ineligible")
                    deactivateKeyguardRuntime("feature-ineligible")
                    return
                }
                if (!SystemUiPresentationRuntimeOwner.keyguardAodReady) {
                    deactivateAodRuntime("aod-authority-unavailable")
                    deactivateKeyguardRuntime("aod-authority-unavailable")
                    if (settings.keyguardEnabled || settings.aodEnabled) {
                        logDiagnostic(
                            level = Log.WARN,
                            event = "aod.authority",
                            component = "keyguardAod",
                            state = "unavailable",
                            "source" to source,
                            "fallback" to "native-keyguard-aod",
                        )
                    }
                    return
                }

                val projection =
                    resolveCurrentKeyguardAodProjection(
                        resolved = resolution.host,
                        fullAodVisualBoundary = fullAodVisualBoundary,
                    )
                        ?: run {
                            deactivateAodRuntime("aod-state-unavailable")
                            deactivateKeyguardRuntime("aod-state-unavailable")
                            logDiagnostic(
                                level = Log.WARN,
                                event = "aod.state",
                                component = "keyguardAod",
                                state = "unavailable",
                                "source" to source,
                                "fallback" to "native-keyguard-aod",
                            )
                            return
                        }
                when (projection) {
                    CombinedStatusScenePolicy.KeyguardAodProjection.AOD -> {
                        if (
                            attachAodRenderer(
                                resolved = resolution.host,
                                source = source,
                            )
                        ) {
                            deactivateKeyguardRuntime("aod-family-handoff")
                        }
                    }

                    CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD -> {
                        if (
                            attachKeyguardRenderer(
                                resolved = resolution.host,
                                source = source,
                            )
                        ) {
                            deactivateAodRuntime("keyguard-family-handoff")
                        }
                    }

                    CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE -> {
                        deactivateAodRuntime("keyguard-aod-native")
                        deactivateKeyguardRuntime("keyguard-aod-native")
                    }
                }
            }

            is SystemUiKeyguardHostResolver.ResolveResult.Inactive -> {
                deactivateAodRuntime(
                    "scene-inactive:" + resolution.surface.name,
                )
                deactivateKeyguardRuntime(
                    "scene-inactive:" + resolution.surface.name,
                )
            }

            is SystemUiKeyguardHostResolver.ResolveResult.Failure -> {
                if (settings.enabled && (settings.keyguardEnabled || settings.aodEnabled)) {
                    deactivateAodRuntime("resolver-failed")
                    deactivateKeyguardRuntime("resolver-failed")
                    logDiagnostic(
                        level = Log.WARN,
                        event = "host.resolve",
                        component = "keyguardAodRenderer",
                        state = "unavailable",
                        "source" to source,
                        "reason" to resolution.reason,
                        "fallback" to "native-keyguard-aod",
                    )
                }
            }
        }
    }

    private fun attachKeyguardRenderer(
        resolved: SystemUiKeyguardHostResolver.ResolvedHost,
        source: String,
    ): Boolean {
        return when (
            val result =
                CombinedStatusKeyguardRenderSession.attach(
                    resolved = resolved,
                    sceneEligible = true,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                    onPresentationReadinessChanged = { ready ->
                        onKeyguardPresentationReadinessChanged(
                            ready = ready,
                            source = source,
                        )
                    },
                )
        ) {
            CombinedStatusKeyguardRenderSession.AttachResult.Ready -> {
                aodRendererAttached = false
                logDiagnostic(
                    level = Log.INFO,
                    event = "renderer.attach",
                    component = "keyguardRenderer",
                    state = "ready",
                    "source" to source,
                    "rawState" to resolved.rawState,
                    "aodOwned" to false,
                    "nativeGeometryWrites" to 0,
                )
                true
            }

            is CombinedStatusKeyguardRenderSession.AttachResult.Failure -> {
                deactivateKeyguardRuntime("renderer-attach-failed")
                logDiagnostic(
                    level = Log.WARN,
                    event = "renderer.attach",
                    component = "keyguardRenderer",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "fallback" to "native-keyguard",
                )
                false
            }
        }
    }

    private fun attachAodRenderer(
        resolved: SystemUiKeyguardHostResolver.ResolvedHost,
        source: String,
    ): Boolean {
        return when (
            val result =
                CombinedStatusKeyguardRenderSession.attachAod(
                    resolved = resolved,
                    sceneEligible = true,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                    onPresentationReadinessChanged = { ready ->
                        onAodPresentationReadinessChanged(
                            ready = ready,
                            source = source,
                        )
                    },
                )
        ) {
            CombinedStatusKeyguardRenderSession.AttachResult.Ready -> {
                aodRendererAttached = true
                logDiagnostic(
                    level = Log.INFO,
                    event = "renderer.attach",
                    component = "aodRenderer",
                    state = "ready",
                    "source" to source,
                    "rawState" to resolved.rawState,
                    "aodOwned" to true,
                    "nativeGeometryWrites" to 0,
                )
                true
            }

            is CombinedStatusKeyguardRenderSession.AttachResult.Failure -> {
                deactivateAodRuntime("renderer-attach-failed")
                logDiagnostic(
                    level = Log.WARN,
                    event = "renderer.attach",
                    component = "aodRenderer",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "fallback" to "native-aod",
                )
                false
            }
        }
    }

    private fun onKeyguardPresentationReadinessChanged(
        ready: Boolean,
        source: String,
    ) {
        keyguardPresentationReadyObserved = ready
        if (!ready) {
            if (incomingKeyguardPresentationReadyForControlCenter()) {
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.readiness",
                    component = "keyguardPresentation",
                    state = "retained",
                    "source" to source,
                    "reason" to "incoming-boundary-presentation-ready",
                    "nativeFraction" to controlCenterExpansionFraction,
                    "leaseActive" to keyguardControlCenterLeaseActive,
                    "timingDelay" to false,
                    "nativeGeometryWrites" to 0,
                )
                return
            }
            if (shouldRetainKeyguardControlCenterLease()) {
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.readiness",
                    component = "keyguardPresentation",
                    state = "retained",
                    "source" to source,
                    "nativeFraction" to controlCenterExpansionFraction,
                    "leaseActive" to keyguardControlCenterLeaseActive,
                    "cleanupDeferredUntil" to "native-control-center-handoff-end",
                    "timingDelay" to false,
                    "nativeGeometryWrites" to 0,
                )
                return
            }
            applyKeyguardPresentationReadinessLost(source)
            return
        }

        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        if (!settings.enabled || !settings.keyguardEnabled) {
            deactivateKeyguardRuntime("feature-ineligible")
            return
        }

        val resolved = SystemUiKeyguardHostResolver.current()
        if (resolved !is SystemUiKeyguardHostResolver.ResolveResult.Ready) {
            deactivateKeyguardRuntime("resolver-not-ready")
            return
        }
        if (
            resolveCurrentKeyguardAodProjection(
                resolved = resolved.host,
                fullAodVisualBoundary = keyguardBoundaryVisualHandoffActive,
            ) != CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD
        ) {
            deactivateKeyguardRuntime("projection-ineligible")
            return
        }

        val visualOnlyBoundary = keyguardBoundaryVisualHandoffActive
        when (
            val result =
                SystemUiHomePresentationOwner.activateKeyguard(
                    resolved = resolved.host,
                    deferNativeLayoutOwnershipUntilCommit = visualOnlyBoundary,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onFailNative = ::onKeyguardPresentationRuntimeFailure,
                    onReady = { active ->
                        if (
                            keyguardBoundaryVisualHandoffActive &&
                            keyguardBoundaryLayoutPrecommitActive
                        ) {
                            onKeyguardBoundaryPrelayoutReady(
                                result = active,
                                source = "native-layout",
                            )
                        } else {
                            completeKeyguardPresentationCutover(
                                result = active,
                                source = "native-layout",
                            )
                        }
                    },
                )
        ) {
            is SystemUiHomePresentationOwner.StateResult.Active -> {
                if (visualOnlyBoundary && keyguardBoundaryLayoutPrecommitActive) {
                    onKeyguardBoundaryPrelayoutReady(
                        result = result,
                        source = source,
                    )
                } else {
                    completeKeyguardPresentationCutover(
                        result = result,
                        source = source,
                    )
                }
            }

            is SystemUiHomePresentationOwner.StateResult.Prepared -> {
                if (visualOnlyBoundary && keyguardBoundaryLayoutPrecommitActive) {
                    keyguardRuntimeReady = false
                    CombinedStatusKeyguardRenderSession.setNativeHandoffActive(true)
                    precommitKeyguardBoundaryLayout(source)
                    return
                }
                keyguardRuntimeReady = false
                CombinedStatusKeyguardRenderSession.setNativeHandoffActive(
                    !visualOnlyBoundary,
                )
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.cutover",
                    component = "keyguardPresentation",
                    state =
                        if (visualOnlyBoundary) {
                            "visual-handoff"
                        } else {
                            "prepared"
                        },
                    "source" to source,
                    "representedSlots" to result.representedSlots,
                    "reused" to result.reused,
                    "next" to
                        if (visualOnlyBoundary) {
                            "stable-keyguard-layout-commit"
                        } else {
                            "native-status-icons-layout"
                        },
                    "fallback" to
                        if (visualOnlyBoundary) {
                            "combined-visual-native-layout-deferred"
                        } else {
                            "native-keyguard-until-compact-layout"
                        },
                    "nativeGeometryWrites" to 0,
                )
                if (!visualOnlyBoundary) {
                    refreshControlCenterSourceSceneEligibility(
                        "keyguard-compact-layout-pending",
                    )
                }
            }

            is SystemUiHomePresentationOwner.StateResult.Failure -> {
                keyguardRuntimeReady = false
                CombinedStatusKeyguardRenderSession.setNativeHandoffActive(true)
                SystemUiHomePresentationOwner.deactivateKeyguard("activation-failed")
                logDiagnostic(
                    level = Log.WARN,
                    event = "presentation.cutover",
                    component = "keyguardPresentation",
                    state = "native",
                    "source" to source,
                    "reason" to result.reason,
                    "fallback" to "native-keyguard",
                )
                refreshControlCenterSourceSceneEligibility("keyguard-activation-failed")
            }

            is SystemUiHomePresentationOwner.StateResult.Inactive -> Unit
        }
    }

    private fun applyKeyguardPresentationReadinessLost(source: String) {
        resetKeyguardBoundaryHandoffState()
        keyguardRuntimeReady = false
        CombinedStatusKeyguardRenderSession.setNativeHandoffActive(true)
        SystemUiHomePresentationOwner.deactivateKeyguard("readiness-lost:" + source)
        reconcileControlCenterForKeyguardLifecycle("keyguard-readiness-lost")
    }

    private fun completeKeyguardPresentationCutover(
        result: SystemUiHomePresentationOwner.StateResult.Active,
        source: String,
    ) {
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val resolved = SystemUiKeyguardHostResolver.current()
        if (
            !settings.enabled ||
            !settings.keyguardEnabled ||
            resolved !is SystemUiKeyguardHostResolver.ResolveResult.Ready ||
            resolveCurrentKeyguardAodProjection(resolved.host) !=
                CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD
        ) {
            deactivateKeyguardRuntime("cutover-projection-ineligible")
            return
        }
        keyguardRuntimeReady = true
        keyguardPresentationReadyObserved = true
        CombinedStatusKeyguardRenderSession.setNativeHandoffActive(false)
        logDiagnostic(
            level = Log.INFO,
            event = "presentation.cutover",
            component = "keyguardPresentation",
            state = "combined",
            "source" to source,
            "representedSlots" to result.representedSlots,
            "maskedViews" to result.maskedViews,
            "motion" to "inherited-from-keyguard-system-icons",
            "aodOwned" to false,
        )
        refreshControlCenterSourceSceneEligibility("keyguard-ready")
    }

    private fun onKeyguardPresentationRuntimeFailure(reason: String) {
        resetKeyguardBoundaryHandoffState()
        keyguardControlCenterLeaseActive = false
        keyguardPresentationReadyObserved = false
        keyguardRuntimeReady = false
        CombinedStatusKeyguardRenderSession.setNativeHandoffActive(true)
        logDiagnostic(
            level = Log.WARN,
            event = "presentation.failNative",
            component = "keyguardPresentation",
            state = "native",
            "reason" to reason,
            "fallback" to "native-keyguard",
        )
        reconcileControlCenterForKeyguardLifecycle("keyguard-fail-native")
    }

    private fun deactivateKeyguardRuntime(source: String) {
        val wasReady = keyguardRuntimeReady
        resetKeyguardBoundaryHandoffState()
        keyguardControlCenterLeaseActive = false
        keyguardPresentationReadyObserved = false
        keyguardRuntimeReady = false
        CombinedStatusKeyguardRenderSession.setNativeHandoffActive(true)
        SystemUiHomePresentationOwner.deactivateKeyguard(source)
        CombinedStatusKeyguardRenderSession.detach()
        if (wasReady) {
            reconcileControlCenterForKeyguardLifecycle("keyguard-deactivate:" + source)
        }
    }

    private fun onAodPresentationReadinessChanged(
        ready: Boolean,
        source: String,
    ) {
        if (!ready) {
            applyAodPresentationReadinessLost(source)
            return
        }

        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        if (!settings.enabled || !settings.aodEnabled) {
            deactivateAodRuntime("feature-ineligible")
            return
        }

        val resolved = SystemUiKeyguardHostResolver.current()
        if (resolved !is SystemUiKeyguardHostResolver.ResolveResult.Ready) {
            deactivateAodRuntime("resolver-not-ready")
            return
        }
        val aodState =
            SystemUiKeyguardAodStateSource.currentState(resolved.host.battery)
                ?: run {
                    deactivateAodRuntime("aod-state-unavailable")
                    return
                }
        if (
            resolveCurrentKeyguardAodProjection(resolved.host) !=
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD
        ) {
            deactivateAodRuntime("projection-ineligible")
            return
        }
        val homeTransitionPrearm =
            homeAodTargetPrearmPending ||
                (
                    aodState.isAodAnimate &&
                        steadyStatusSourceScene == CombinedStatusSourceScene.HOME &&
                        SystemUiHomePresentationOwner
                            .currentHomeRepresentedSlotOwnership()
                            .isNotEmpty()
                )

        when (
            val result =
                SystemUiHomePresentationOwner.activateAod(
                    resolved = resolved.host,
                    preMaskBeforeLayout = homeTransitionPrearm,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onFailNative = ::onAodPresentationRuntimeFailure,
                    onReady = { active ->
                        completeAodPresentationCutover(
                            result = active,
                            source = "native-layout",
                        )
                    },
                )
        ) {
            is SystemUiHomePresentationOwner.StateResult.Active -> {
                completeAodPresentationCutover(
                    result = result,
                    source = source,
                )
            }

            is SystemUiHomePresentationOwner.StateResult.Prepared -> {
                CombinedStatusKeyguardRenderSession.setAodNativeHandoffActive(true)
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.cutover",
                    component = "aodPresentation",
                    state = "prepared",
                    "source" to source,
                    "representedSlots" to result.representedSlots,
                    "reused" to result.reused,
                    "next" to "native-status-icons-layout",
                    "preMasked" to homeTransitionPrearm,
                    "fallback" to
                        if (homeTransitionPrearm) {
                            "outgoing-guiyuan-or-masked-native-until-compact-layout"
                        } else {
                            "native-aod-until-compact-layout"
                        },
                )
            }

            is SystemUiHomePresentationOwner.StateResult.Failure -> {
                CombinedStatusKeyguardRenderSession.setAodNativeHandoffActive(true)
                SystemUiHomePresentationOwner.deactivateAod("activation-failed")
                logDiagnostic(
                    level = Log.WARN,
                    event = "presentation.cutover",
                    component = "aodPresentation",
                    state = "native",
                    "source" to source,
                    "reason" to result.reason,
                    "fallback" to "native-aod",
                )
            }

            is SystemUiHomePresentationOwner.StateResult.Inactive -> Unit
        }
    }

    private fun applyAodPresentationReadinessLost(source: String) {
        CombinedStatusKeyguardRenderSession.setAodNativeHandoffActive(true)
        SystemUiHomePresentationOwner.deactivateAod("readiness-lost:" + source)
    }

    private fun completeAodPresentationCutover(
        result: SystemUiHomePresentationOwner.StateResult.Active,
        source: String,
    ) {
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val resolved = SystemUiKeyguardHostResolver.current()
        if (
            !settings.enabled ||
            !settings.aodEnabled ||
            resolved !is SystemUiKeyguardHostResolver.ResolveResult.Ready ||
            resolveCurrentKeyguardAodProjection(resolved.host) !=
                CombinedStatusScenePolicy.KeyguardAodProjection.AOD
        ) {
            deactivateAodRuntime("cutover-projection-ineligible")
            return
        }

        CombinedStatusKeyguardRenderSession.setAodNativeHandoffActive(false)
        logDiagnostic(
            level = Log.INFO,
            event = "presentation.cutover",
            component = "aodPresentation",
            state = "combined",
            "source" to source,
            "representedSlots" to result.representedSlots,
            "maskedViews" to result.maskedViews,
            "motion" to "native-aod-host-inherited",
            "aodOwned" to true,
        )
    }

    private fun onAodPresentationRuntimeFailure(reason: String) {
        CombinedStatusKeyguardRenderSession.setAodNativeHandoffActive(true)
        logDiagnostic(
            level = Log.WARN,
            event = "presentation.failNative",
            component = "aodPresentation",
            state = "native",
            "reason" to reason,
            "fallback" to "native-aod",
        )
    }

    private fun deactivateAodRuntime(source: String) {
        homeAodTargetPrearmPending = false
        aodRendererAttached = false
        CombinedStatusKeyguardRenderSession.setAodNativeHandoffActive(true)
        SystemUiHomePresentationOwner.deactivateAod(source)
        CombinedStatusKeyguardRenderSession.detachAod()
    }

    private fun onKeyguardHostProbe(snapshot: SystemUiKeyguardHostProbe.Snapshot) {
        logDiagnostic(
            level = Log.INFO,
            event = "keyguard.hostProbe",
            component = "keyguardProbe",
            state = if (snapshot.complete) "ready" else "partial",
            "source" to "MiuiBatteryMeterView.updateState",
            "rawState" to snapshot.rawState,
            "host" to snapshot.host.summary,
            "systemIcons" to snapshot.systemIcons?.summary,
            "statusIcons" to snapshot.statusIcons?.summary,
            "battery" to snapshot.battery?.summary,
            "batteryCarrier" to snapshot.batteryCarrier?.summary,
            "batteryCarrierWidthPx" to snapshot.batteryCarrierWidthPx,
            "batteryMatchesSceneSource" to snapshot.batteryMatchesSceneSource,
            "selectedRealSystemIcons" to snapshot.selectedRealSystemIcons?.summary,
            "selectedAsRealSystemIcons" to snapshot.selectedAsRealSystemIcons,
            "complete" to snapshot.complete,
            "retryPolicy" to "later-keyguard-scene-event-until-positive-ready",
            "hookDelta" to 0,
            "rendering" to
                if (RuntimeFeaturePreferencesOwner.currentSettings().keyguardEnabled) {
                    "candidate"
                } else {
                    "disabled"
                },
            "suppression" to if (keyguardRuntimeReady) "active" else "native",
            "aodProbe" to false,
            "nativeGeometryWrites" to 0,
            "healthSnapshot" to false,
        )
    }

    private fun teardownOldGenerationForHotReload(
        continuousHandoff: Boolean = false,
    ) {
        controlCenterSceneVisible = false
        controlCenterSceneEligible = false
        controlCenterSourceScene = CombinedStatusSourceScene.UNKNOWN
        steadyStatusSourceScene = CombinedStatusSourceScene.UNKNOWN
        lastStableKeyguardAodScene =
            CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN
        keyguardAodFullTargetPending = false
        keyguardAodFullTransitionActive = false
        homeAodTargetPrearmPending = false
        controlCenterExpansionFraction = 0f
        keyguardRuntimeReady = false
        aodRendererAttached = false
        keyguardPresentationReadyObserved = false
        keyguardControlCenterLeaseActive = false
        CombinedStatusControlCenterTransitionOwner.detach("hotReload-oldGeneration")
        CombinedStatusControlCenterRenderSession.detach(
            source = "hotReload-oldGeneration",
            releaseNativePresentation = !continuousHandoff,
        )
        CombinedStatusHomeRenderSession.detach()
        CombinedStatusKeyguardRenderSession.detach()
        CombinedStatusKeyguardRenderSession.detachAod()
        val restoredPresentationViews =
            SystemUiHomePresentationOwner.releaseGenerationForHotReload(
                requestLayout =
                    SystemUiHomePresentationOwner.HotReloadHandoffPolicy
                        .shouldRequestLayoutOnRelease(continuousHandoff),
            )
        SystemUiNativeNetworkSuppressionOwner.deactivate("hotReload-oldGeneration")
        StatusBarStableSession.detach()
        SystemUiCoreRuntimeOwner.detach()
        SystemUiPresentationRuntimeOwner.resetRuntimeState()
        SystemUiKeyguardHostResolver.resetRuntimeState()
        CombinedStatusPresentationStateStore.reset()
        SystemUiIslandMotionSource.resetRuntimeState()
        SystemUiPanelTransitionSource.resetRuntimeState()

        logDiagnostic(
            level = Log.INFO,
            event = "runtime.teardown",
            component = "runtimeSession",
            state = "ready",
            "source" to "hotReload.oldGeneration",
            "rendererDetached" to true,
            "homePresentationRestoredViews" to restoredPresentationViews,
            "continuousHandoff" to continuousHandoff,
            "intermediateRequestLayout" to
                SystemUiHomePresentationOwner.HotReloadHandoffPolicy
                    .shouldRequestLayoutOnRelease(continuousHandoff),
            "stableStatusDetached" to true,
            "airplaneObserverDetached" to true,
            "defaultDataSubscriptionObserverDetached" to true,
            "mainThread" to true,
        )
        unbindRuntimeDiagnostics()
        RuntimeFeaturePreferencesOwner.unbind()
        RuntimeVisualPreferencesOwner.unbind()
    }

    private fun attachHostRuntime(
        host: Any,
        source: String,
        initialNativeHandoffActive: Boolean = false,
        initialTintState: CombinedStatusTintState? = null,
        allowLiveTintSeed: Boolean = true,
    ) {
        val hostContext = (host as? android.view.View)?.context
        val coreRuntime =
            hostContext?.let { context ->
                SystemUiCoreRuntimeOwner.attach(
                    context = context,
                    onAirplaneMode = { enabled ->
                        val trace = beginRenderTrace("airplaneObserver")
                        CombinedStatusStateStore.updateAirplaneMode(enabled)?.let { snapshot ->
                            onCombinedStateChanged(
                                snapshot = snapshot,
                                trace = markStateCommitted(trace),
                            )
                        }
                        refreshStatusIconObservation("airplane")
                    },
                    onDefaultDataSubscriptionChanged = {
                        refreshMobilePresentation(
                            beginRenderTrace("defaultDataSubscription"),
                        )
                    },
                    onConnectivityState = { state ->
                        val trace = beginRenderTrace("connectivity")
                        val changed =
                            CombinedStatusPresentationStateStore.updateConnectivity(state)
                        val presentationTrace =
                            if (changed != null) {
                                markPresentationCommitted(trace)
                            } else {
                                trace
                            }
                        refreshMobilePresentation(presentationTrace)
                        changed?.let {
                            onPresentationStateChanged(
                                presentationTrace,
                            )
                        }
                    },
                    onEvent =
                        if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                            ::onNetworkPipelineEvent
                        } else {
                            null
                        },
                )
            }

        logDiagnostic(
            level = if (coreRuntime?.airplaneReady == true) Log.INFO else Log.WARN,
            event = "source.attach",
            component = "airplane",
            state = if (coreRuntime?.airplaneReady == true) "ready" else "unavailable",
            "source" to source,
            "observer" to "settings-global-content-observer",
        )
        logDiagnostic(
            level =
                if (coreRuntime?.defaultDataSubscriptionReady == true) Log.INFO else Log.WARN,
            event = "source.attach",
            component = "defaultDataSubscription",
            state =
                if (coreRuntime?.defaultDataSubscriptionReady == true) "ready" else "unavailable",
            "source" to source,
            "observer" to "default-data-subscription-broadcast",
            "subscriptionId" to SystemUiDefaultDataSubscriptionSource.currentSubscriptionId(),
            "eventDriven" to true,
        )
        logDiagnostic(
            level = if (coreRuntime?.connectivityReady == true) Log.INFO else Log.WARN,
            event = "source.attach",
            component = "connectivity",
            state = if (coreRuntime?.connectivityReady == true) "ready" else "unavailable",
            "source" to source,
        )
        refreshMobilePresentation()

        when (
            val stableSession = StatusBarStableSession.attach(
                host = host,
                onEvent = { event ->
                    if (detailedDiagnosticsEnabled) {
                        log(Log.INFO, TAG, event)
                    }
                },
            )
        ) {
            StatusBarStableSession.AttachResult.Ready -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "session.attach",
                    component = "stableStatus",
                    state = "ready",
                    "source" to source,
                )
            }

            is StatusBarStableSession.AttachResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "session.attach",
                    component = "stableStatus",
                    state = "unavailable",
                    "reason" to stableSession.reason,
                    "source" to source,
                )
            }
        }

        when (
            val observation =
                SystemUiNativeNetworkSuppressionOwner.attachObserver(host)
        ) {
            is SystemUiNativeNetworkSuppressionOwner.StateResult.Active -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "source.attach",
                    component = "statusIconObservation",
                    state = "ready",
                    "source" to source,
                    "mode" to "observation-only",
                    "suppressionWriters" to 0,
                )
            }
            is SystemUiNativeNetworkSuppressionOwner.StateResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "source.attach",
                    component = "statusIconObservation",
                    state = "unavailable",
                    "source" to source,
                    "reason" to observation.reason,
                    "fallback" to "native-systemui",
                )
            }
            is SystemUiNativeNetworkSuppressionOwner.StateResult.Inactive -> Unit
        }

        val rendererInitialTintState =
            initialTintState?.let { transferred ->
                CombinedStatusTintAuthority.rebaseTransferred(
                    transferred = transferred,
                    liveStatusIconTint =
                        SystemUiNativeNetworkSuppressionOwner
                            .currentAppliedStatusIconTint(),
                )
            }

        when (
            val renderSession = CombinedStatusHomeRenderSession.attach(
                host = host,
                onEvent = { event ->
                    if (detailedDiagnosticsEnabled) {
                        log(Log.INFO, TAG, event)
                    }
                },
                onLatencySample = ::onRenderLatencySample,
                isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                initialNativeHandoffActive = true,
                initialTintState = rendererInitialTintState,
                allowLiveTintSeed = allowLiveTintSeed,
                onPresentationReadinessChanged = { ready ->
                    onHomePresentationReadinessChanged(host, ready, source)
                },
            )
        ) {
            CombinedStatusHomeRenderSession.AttachResult.Ready -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "renderer.attach",
                    component = "renderer",
                    state = "ready",
                    "source" to source,
                )
            }

            is CombinedStatusHomeRenderSession.AttachResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "renderer.attach",
                    component = "renderer",
                    state = "unavailable",
                    "reason" to renderSession.reason,
                    "source" to source,
                )
            }
        }

        scheduleNativeSlotProbe(host = host, source = source)

        logDiagnostic(
            level = Log.INFO,
            event = "runtime.attach",
            component = "runtimeSession",
            state = "ready",
            "source" to source,
        )
    }

    private fun onHomePresentationReadinessChanged(
        host: Any,
        ready: Boolean,
        source: String,
    ) {
        if (!RuntimeFeaturePreferencesOwner.currentSettings().enabled) {
            CombinedStatusHomeRenderSession.setNativeHandoffActive(true)
            SystemUiHomePresentationOwner.deactivate("feature-disabled:" + source)
            return
        }
        if (!ready) {
            CombinedStatusHomeRenderSession.setNativeHandoffActive(true)
            SystemUiHomePresentationOwner.deactivate("readiness-lost:" + source)
            return
        }

        when (val result = SystemUiHomePresentationOwner.activate(host)) {
            is SystemUiHomePresentationOwner.StateResult.Active -> {
                CombinedStatusHomeRenderSession.setNativeHandoffActive(false)
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.cutover",
                    component = "homePresentation",
                    state = "combined",
                    "source" to source,
                    "representedSlots" to result.representedSlots,
                    "maskedViews" to result.maskedViews,
                    "islandMotion" to "inherited-from-system_icon_area",
                )
            }
            is SystemUiHomePresentationOwner.StateResult.Prepared -> {
                CombinedStatusHomeRenderSession.setNativeHandoffActive(true)
                SystemUiHomePresentationOwner.deactivate("unexpected-prepared")
                logDiagnostic(
                    level = Log.WARN,
                    event = "presentation.cutover",
                    component = "homePresentation",
                    state = "native",
                    "source" to source,
                    "reason" to "unexpected-prepared-state",
                    "fallback" to "native-systemui",
                )
            }
            is SystemUiHomePresentationOwner.StateResult.Failure -> {
                CombinedStatusHomeRenderSession.setNativeHandoffActive(true)
                SystemUiHomePresentationOwner.deactivate("activation-failed")
                logDiagnostic(
                    level = Log.WARN,
                    event = "presentation.cutover",
                    component = "homePresentation",
                    state = "native",
                    "source" to source,
                    "reason" to result.reason,
                    "fallback" to "native-systemui",
                )
            }
            is SystemUiHomePresentationOwner.StateResult.Inactive -> Unit
        }
    }

    private fun onHomePresentationRuntimeFailure(reason: String) {
        CombinedStatusHomeRenderSession.setNativeHandoffActive(true)
        logDiagnostic(
            level = Log.WARN,
            event = "presentation.failNative",
            component = "homePresentation",
            state = "native",
            "reason" to reason,
            "fallback" to "native-systemui",
        )
    }

    private fun scheduleNativeParticipantRuntime(
        host: Any,
        source: String,
    ) {
        when (
            val result =
                SystemUiNativeParticipantRuntimeOwner.schedule(
                    host = host,
                    onReady = { readyHost ->
                        attachNativeCombinedParticipant(
                            host = readyHost,
                            source = source,
                        )
                        if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                            runNativeParticipantDiagnostics(
                                host = readyHost,
                                source = source,
                            )
                        }
                    },
                    onFailure = { reason ->
                        logDiagnostic(
                            level = Log.WARN,
                            event = "participant.lifecycle",
                            component = "nativeParticipant",
                            state = "unavailable",
                            "source" to source,
                            "reason" to reason,
                            "nativeGeometryWrites" to 0,
                        )
                    },
                )
        ) {
            SystemUiNativeParticipantRuntimeOwner.ScheduleResult.Scheduled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "participant.lifecycle",
                    component = "nativeParticipant",
                    state = "pending",
                    "source" to source,
                    "trigger" to "native-dark-icon-manager-registered",
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeParticipantRuntimeOwner.ScheduleResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "participant.lifecycle",
                    component = "nativeParticipant",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun runNativeParticipantDiagnostics(
        host: Any,
        source: String,
    ) {
        val nativeParticipant = NativeParticipantContractProbe.inspect(host)
        log(Log.INFO, TAG, nativeParticipant.logLine)
        logDiagnostic(
            level =
                if (nativeParticipant.registrationContractReady) {
                    Log.INFO
                } else {
                    Log.WARN
                },
            event = "contract.probe",
            component = "nativeParticipant",
            state =
                if (nativeParticipant.registrationContractReady) {
                    "ready"
                } else {
                    "observed"
                },
            "available" to nativeParticipant.available,
            "reason" to nativeParticipant.reason,
            "manager" to nativeParticipant.managerClass,
            "group" to nativeParticipant.groupClass,
            "groupRes" to nativeParticipant.groupResource,
            "controller" to nativeParticipant.controllerClass,
            "controllerSource" to nativeParticipant.controllerSource,
            "controllerMatches" to nativeParticipant.controllerMatches,
            "managerMatches" to nativeParticipant.managerMatches,
            "groupMatches" to nativeParticipant.groupMatches,
            "setIconHolder" to nativeParticipant.setIconHolder,
            "resourceSetIconMode" to nativeParticipant.resourceSetIconMode,
            "setIconVisibility" to nativeParticipant.setIconVisibility,
            "removalReady" to nativeParticipant.removalReady,
            "removalMode" to nativeParticipant.removalMode,
            "addIconGroup" to nativeParticipant.addIconGroup,
            "removeIconGroup" to nativeParticipant.removeIconGroup,
            "addHolder" to nativeParticipant.addHolder,
            "holderFactoryReady" to nativeParticipant.holderFactoryReady,
            "statusIconDisplayable" to nativeParticipant.iconViewDisplayable,
            "slotAccessor" to nativeParticipant.iconViewSlotAccessor,
            "systemManagedCreationReady" to nativeParticipant.systemManagedCreationReady,
            "registrationReady" to nativeParticipant.registrationContractReady,
            "setIconSignatures" to
                nativeParticipant.setIconSignatures.joinToString("|"),
            "removeSignatures" to
                nativeParticipant.removeSignatures.joinToString("|"),
            "holderFactories" to
                nativeParticipant.holderFactorySignatures.joinToString("|"),
            "nativeGeometryWrites" to 0,
        )

        if (nativeParticipant.registrationContractReady) {
            val bindableParticipant =
                NativeBindableParticipantContractProbe.inspect(host)
            log(Log.INFO, TAG, bindableParticipant.logLine)
            val bindableProbeReady =
                bindableParticipant.staticContractReady &&
                    bindableParticipant.managerBindableMapReady &&
                    bindableParticipant.viewOnlySlotsReady
            logDiagnostic(
                level = if (bindableProbeReady) Log.INFO else Log.WARN,
                event = "contract.probe",
                component = "nativeBindableParticipant",
                state = if (bindableProbeReady) "ready" else "observed",
                "source" to source,
                "available" to bindableParticipant.available,
                "reason" to bindableParticipant.reason,
                "interfaceReady" to bindableParticipant.bindableInterfaceReady,
                "creatorReady" to bindableParticipant.creatorReady,
                "registry" to bindableParticipant.registryClass,
                "registryConstructors" to
                    bindableParticipant.registryConstructors.joinToString("|"),
                "holder" to bindableParticipant.holderClass,
                "holderConstructors" to
                    bindableParticipant.holderConstructors.joinToString("|"),
                "modernView" to bindableParticipant.modernViewClass,
                "singleView" to bindableParticipant.singleBindableViewClass,
                "managerMapReady" to bindableParticipant.managerBindableMapReady,
                "managerMapCount" to bindableParticipant.managerBindableCount,
                "managerEntries" to
                    bindableParticipant.managerBindableEntries.joinToString("|"),
                "viewOnlySlotsReady" to bindableParticipant.viewOnlySlotsReady,
                "viewOnlySlots" to
                    bindableParticipant.viewOnlySlots.joinToString("|"),
                "runtimeViews" to
                    bindableParticipant.runtimeBindableViews.joinToString("|"),
                "slotOrder" to
                    bindableParticipant.runtimeSlotOrder.joinToString("|"),
                "groupClipChildren" to bindableParticipant.groupClipChildren,
                "groupClipToPadding" to bindableParticipant.groupClipToPadding,
                "groupHeight" to bindableParticipant.groupHeight,
                "staticContractReady" to bindableParticipant.staticContractReady,
                "dynamicRegistrationObserved" to
                    bindableParticipant.dynamicRegistrationObserved,
                "nativeGeometryWrites" to 0,
            )

            val visualGeometry =
                NativeBindableVisualGeometryProbe.inspect(host)
            log(Log.INFO, TAG, visualGeometry.logLine)
            logDiagnostic(
                level = if (visualGeometry.ready) Log.INFO else Log.WARN,
                event = "contract.probe",
                component = "nativeBindableVisualGeometry",
                state = if (visualGeometry.ready) "ready" else "observed",
                "source" to source,
                "available" to visualGeometry.available,
                "reason" to visualGeometry.reason,
                "reference" to visualGeometry.referenceClass,
                "referenceBounds" to visualGeometry.referenceBounds,
                "referenceLayoutWidth" to visualGeometry.referenceLayoutWidth,
                "referenceLayoutHeight" to visualGeometry.referenceLayoutHeight,
                "groupHeight" to visualGeometry.groupHeight,
                "groupClipChildren" to visualGeometry.groupClipChildren,
                "groupClipToPadding" to visualGeometry.groupClipToPadding,
                "visualWidth" to visualGeometry.visualWidth,
                "visualHeight" to visualGeometry.visualHeight,
                "shellMeasuredWidth" to visualGeometry.shellMeasuredWidth,
                "shellMeasuredHeight" to visualGeometry.shellMeasuredHeight,
                "shellClipChildren" to visualGeometry.shellClipChildren,
                "renderMeasuredWidth" to visualGeometry.renderMeasuredWidth,
                "renderMeasuredHeight" to visualGeometry.renderMeasuredHeight,
                "renderBounds" to visualGeometry.renderBounds,
                "projectedTop" to visualGeometry.projectedTop,
                "projectedBottom" to visualGeometry.projectedBottom,
                "projectedFitsGroup" to visualGeometry.projectedFitsGroup,
                "nativeGeometryWrites" to 0,
            )

        }
    }

    private fun attachNativeCombinedParticipant(
        host: Any,
        source: String,
    ) {
        when (
            val nativeCombined =
                SystemUiNativeCombinedParticipantOwner.attachHidden(
                    host = host,
                    onHandoffStateChanged = { active ->
                        val presentation =
                            CombinedStatusPresentationStateStore.snapshot()
                        val state = CombinedStatusStateStore.snapshot()
                        val wifi = state.wifi

                        if (
                            active &&
                            !RuntimeFeaturePreferencesOwner.currentSettings().enabled
                        ) {
                            val batterySuppression =
                                SystemUiNativeBatterySuppressionOwner.deactivate(
                                    "feature-disabled-native-handoff",
                                )
                            val networkSuppression =
                                SystemUiNativeNetworkSuppressionOwner.deactivate(
                                    "feature-disabled-native-handoff",
                                )
                            CombinedStatusHomeRenderSession.setNativeHandoffActive(false)
                            logDiagnostic(
                                level = Log.INFO,
                                event = "visibility.handoff",
                                component = "nativeCombinedParticipant",
                                state = "blocked",
                                "source" to source,
                                "reason" to "master-switch-disabled",
                                "nativeActive" to false,
                                "overlayActive" to false,
                                "networkSuppression" to networkSuppression.summary,
                                "batterySuppression" to batterySuppression.summary,
                                "nativeGeometryWrites" to 0,
                            )
                            false
                        } else if (active) {
                            val batterySuppression =
                                SystemUiNativeBatterySuppressionOwner.activate(
                                    host = host,
                                    source = "native-handoff:" + source,
                                )
                            if (
                                batterySuppression is
                                    SystemUiNativeBatterySuppressionOwner.StateResult.Failure
                            ) {
                                logDiagnostic(
                                    level = Log.WARN,
                                    event = "visibility.handoff",
                                    component = "nativeCombinedParticipant",
                                    state = "fallback",
                                    "source" to source,
                                    "nativeActive" to false,
                                    "overlayActive" to true,
                                    "networkSuppression" to "not-attempted",
                                    "batterySuppression" to batterySuppression.summary,
                                    "nativeGeometryWrites" to 0,
                                )
                                false
                            } else {
                                val networkSuppression =
                                    SystemUiNativeNetworkSuppressionOwner.activate(
                                        host = host,
                                        suppressWifi =
                                            SystemUiNetworkRuntimeOwner.wifiReady &&
                                                CombinedStatusConnectivityPolicy
                                                    .wifiReplacementReady(
                                                        wifi = wifi,
                                                        connectivity = presentation.connectivity,
                                                    ),
                                        suppressMobile =
                                            NativeNetworkSuppressionPolicy.suppressMobile(
                                                airplaneMode = state.airplaneMode,
                                                presentation = presentation.mobilePresentation,
                                                wasSuppressed = false,
                                            ),
                                    )
                                if (
                                    networkSuppression is
                                        SystemUiNativeNetworkSuppressionOwner.StateResult.Failure
                                ) {
                                    val batteryRollback =
                                        SystemUiNativeBatterySuppressionOwner.deactivate(
                                            "native-handoff-rollback",
                                        )
                                    logDiagnostic(
                                        level = Log.WARN,
                                        event = "visibility.handoff",
                                        component = "nativeCombinedParticipant",
                                        state = "fallback",
                                        "source" to source,
                                        "nativeActive" to false,
                                        "overlayActive" to true,
                                        "networkSuppression" to networkSuppression.summary,
                                        "batterySuppression" to batteryRollback.summary,
                                        "nativeGeometryWrites" to
                                            if (
                                                batteryRollback is
                                                    SystemUiNativeBatterySuppressionOwner.StateResult.Inactive &&
                                                batteryRollback.changed
                                            ) {
                                                1
                                            } else {
                                                0
                                            },
                                    )
                                    false
                                } else {
                                    CombinedStatusHomeRenderSession.setNativeHandoffActive(true)
                                    logDiagnostic(
                                        level = Log.INFO,
                                        event = "visibility.handoff",
                                        component = "nativeCombinedParticipant",
                                        state = "active",
                                        "source" to source,
                                        "nativeActive" to true,
                                        "overlayActive" to false,
                                        "networkSuppression" to networkSuppression.summary,
                                        "batterySuppression" to batterySuppression.summary,
                                        "nativeGeometryWrites" to
                                            if (
                                                batterySuppression is
                                                    SystemUiNativeBatterySuppressionOwner.StateResult.Active &&
                                                batterySuppression.changed
                                            ) {
                                                1
                                            } else {
                                                0
                                            },
                                    )
                                    true
                                }
                            }
                        } else {
                            val batterySuppression =
                                SystemUiNativeBatterySuppressionOwner.deactivate(
                                    "native-handoff-fallback",
                                )
                            if (
                                batterySuppression is
                                    SystemUiNativeBatterySuppressionOwner.StateResult.Failure
                            ) {
                                logDiagnostic(
                                    level = Log.WARN,
                                    event = "visibility.handoff",
                                    component = "nativeCombinedParticipant",
                                    state = "active",
                                    "source" to source,
                                    "nativeActive" to true,
                                    "overlayActive" to false,
                                    "networkSuppression" to "kept-active",
                                    "batterySuppression" to batterySuppression.summary,
                                    "nativeGeometryWrites" to 0,
                                )
                                false
                            } else {
                                val networkSuppression =
                                    SystemUiNativeNetworkSuppressionOwner.deactivate(
                                        "native-handoff-fallback",
                                    )
                                CombinedStatusHomeRenderSession.setNativeHandoffActive(false)
                                logDiagnostic(
                                    level = Log.INFO,
                                    event = "visibility.handoff",
                                    component = "nativeCombinedParticipant",
                                    state = "fallback",
                                    "source" to source,
                                    "nativeActive" to false,
                                    "overlayActive" to true,
                                    "networkSuppression" to networkSuppression.summary,
                                    "batterySuppression" to batterySuppression.summary,
                                    "nativeGeometryWrites" to
                                        if (
                                            batterySuppression is
                                                SystemUiNativeBatterySuppressionOwner.StateResult.Inactive &&
                                            batterySuppression.changed
                                        ) {
                                            1
                                        } else {
                                            0
                                        },
                                )
                                true
                            }
                        }
                    },
                )
        ) {
            is SystemUiNativeCombinedParticipantOwner.AttachResult.Ready -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "participant.attach",
                    component = "nativeCombinedParticipant",
                    state = "ready",
                    "source" to source,
                    "slot" to SystemUiNativeCombinedParticipantOwner.SLOT,
                    "visible" to false,
                    "registryRestored" to nativeCombined.registryRestored,
                    "root" to nativeCombined.rootClass,
                    "rootVisibility" to nativeCombined.rootVisibility,
                    "iconVisible" to nativeCombined.iconVisible,
                    "layoutWidth" to nativeCombined.layoutWidth,
                    "layoutHeight" to nativeCombined.layoutHeight,
                    "renderWidth" to nativeCombined.renderWidth,
                    "renderHeight" to nativeCombined.renderHeight,
                    "renderTop" to nativeCombined.renderTop,
                    "renderBottom" to nativeCombined.renderBottom,
                    "managerEntry" to nativeCombined.managerEntry,
                    "modelReady" to nativeCombined.modelReady,
                    "tintReady" to nativeCombined.tintReady,
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeCombinedParticipantOwner.AttachResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "participant.attach",
                    component = "nativeCombinedParticipant",
                    state = "unavailable",
                    "source" to source,
                    "reason" to nativeCombined.reason,
                    "visible" to false,
                    "nativeGeometryWrites" to 0,
                )
            }
        }

    }

    private fun scheduleNativeSlotProbe(
        host: Any,
        source: String,
    ) {
        if (
            !BuildConfig.DEVELOPMENT_PROBES &&
            !(BuildConfig.RUNTIME_DIAGNOSTICS && detailedDiagnosticsEnabled)
        ) {
            return
        }

        SystemUiNetworkStateSource.bindingTopologyLines().forEach { line ->
            log(Log.INFO, TAG, line)
        }



        SystemUiNativeStatusInventory.schedule(host) { snapshot ->
            log(Log.INFO, TAG, snapshot.summary)
            log(Log.INFO, TAG, snapshot.hostLine)
            snapshot.entries.forEach { entry ->
                log(Log.INFO, TAG, entry.logLine)
            }
            snapshot.statusIconSubtree?.let { subtree ->
                log(Log.INFO, TAG, subtree.summary)
                subtree.entries.forEach { entry ->
                    log(Log.INFO, TAG, entry.logLine)
                }
                logDiagnostic(
                    level = Log.INFO,
                    event = "slot.probe",
                    component = "nativeSlot",
                    state = "observed",
                    "source" to source,
                    "root" to subtree.rootClassName,
                    "children" to subtree.rootChildCount,
                    "nodes" to subtree.entries.size,
                    "truncated" to subtree.truncated,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun onStatusHostCaptured(capture: SystemUiHostRegistry.Capture) {
        logDiagnostic(
            level = Log.INFO,
            event = "host.capture",
            component = "statusHost",
            state = "ready",
            "identity" to capture.identity,
            "replacement" to capture.replacement,
        )
        log(
            Log.INFO,
            TAG,
            "statusHost captured id=" + capture.identity +
                " replacement=" + capture.replacement,
        )
        attachHostRuntime(
            host = capture.host,
            source = if (capture.replacement) "hostReplacement" else "hostCapture",
        )

    }

    private fun bindRuntimeDiagnostics() {
        if (!BuildConfig.RUNTIME_DIAGNOSTICS) {
            RuntimeDiagnosticsPreferencesOwner.unbind()
            detailedDiagnosticsEnabled = BuildConfig.DEVELOPMENT_PROBES
            logDiagnostic(
                level = Log.INFO,
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "disabled",
                "channel" to BuildConfig.BUILD_CHANNEL,
            )
            return
        }

        runCatching {
            RuntimeDiagnosticsPreferencesOwner.bind(
                preferences = getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME),
                forceDetailed = BuildConfig.DEVELOPMENT_PROBES,
                onDetailedChanged = ::setDetailedDiagnosticsEnabled,
            )
        }.onSuccess { result ->
            logDiagnostic(
                level = Log.INFO,
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "ready",
                "level" to if (result.detailedEnabled) "detailed" else "general",
                "transport" to "remote-preferences",
            )
        }.onFailure { error ->
            RuntimeDiagnosticsPreferencesOwner.unbind()
            detailedDiagnosticsEnabled = BuildConfig.DEVELOPMENT_PROBES
            logDiagnostic(
                level = Log.WARN,
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "unavailable",
                "reason" to (error.message ?: error.javaClass.simpleName),
            )
            log(Log.WARN, TAG, "Runtime diagnostics preference unavailable", error)
        }
    }

    private fun bindRuntimeFeatureSettings() {
        runCatching {
            RuntimeFeaturePreferencesOwner.bind(
                preferences = getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME),
                onChanged = ::onRuntimeFeatureSettingsChanged,
            )
        }.onSuccess { settings ->
            logDiagnostic(
                level = Log.INFO,
                event = "runtimePreferences.bind",
                component = "featureSettings",
                state = "ready",
                "combinedStatusEnabled" to settings.enabled,
                "keyguardEnabled" to settings.keyguardEnabled,
                "aodEnabled" to settings.aodEnabled,
                "transport" to "remote-preferences",
            )
        }.onFailure { error ->
            RuntimeFeaturePreferencesOwner.unbind()
            onRuntimeFeatureSettingsChanged(
                RuntimeFeaturePreferencesOwner.currentSettings(),
                null,
            )
            logDiagnostic(
                level = Log.WARN,
                event = "runtimePreferences.bind",
                component = "featureSettings",
                state = "unavailable",
                "combinedStatusEnabled" to false,
                "keyguardEnabled" to false,
                "aodEnabled" to false,
                "reason" to (error.message ?: error.javaClass.simpleName),
                "fallback" to "native-systemui",
            )
        }
    }

    private fun onRuntimeFeatureSettingsChanged(
        settings: CombinedStatusFeatureSettings,
        preferenceTransportLatencyNanos: Long?,
    ) {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            val dispatch =
                Runnable {
                    onRuntimeFeatureSettingsChanged(
                        settings = settings,
                        preferenceTransportLatencyNanos = preferenceTransportLatencyNanos,
                    )
                }
            val hostView = SystemUiHostRegistry.currentStatusHost() as? android.view.View
            val scheduled =
                (hostView?.post(dispatch) == true) ||
                    Handler(Looper.getMainLooper()).post(dispatch)
            if (scheduled) {
                return
            }
            logDiagnostic(
                level = Log.ERROR,
                event = "featureSettings.dispatch",
                component = "combinedStatus",
                state = "error",
                "reason" to "main-thread-dispatch-failed",
                "combinedStatusEnabled" to settings.enabled,
                "keyguardEnabled" to settings.keyguardEnabled,
                "aodEnabled" to settings.aodEnabled,
                "fallback" to "leave-current-native-ownership-unchanged",
            )
            return
        }

        SystemUiNativeCombinedParticipantOwner.onFeatureSettingsChanged(settings)
        if (
            !settings.enabled ||
            !settings.keyguardEnabled ||
            settings.aodEnabled
        ) {
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = false
        }
        CombinedStatusHomeRenderSession.onFeatureSettingsChanged(settings)
        CombinedStatusKeyguardRenderSession.onFeatureSettingsChanged(settings)
        CombinedStatusControlCenterRenderSession.onFeatureSettingsChanged(settings)

        if (!settings.enabled) {
            releaseFeaturePresentationOwnership("feature-disabled")
            deactivateAodRuntime("feature-disabled")
            deactivateKeyguardRuntime("feature-disabled")
        } else {
            if (!settings.keyguardEnabled) {
                deactivateKeyguardRuntime("keyguard-feature-disabled")
            }
            if (!settings.aodEnabled) {
                deactivateAodRuntime("aod-feature-disabled")
            }
            SystemUiKeyguardHostResolver.current()?.let { resolution ->
                onKeyguardHostResolution(
                    resolution = resolution,
                    source = "feature-settings",
                )
            }
        }
        refreshControlCenterSourceSceneEligibility("feature-settings")

        logDiagnostic(
            level = Log.INFO,
            event = "featureSettings.changed",
            component = "combinedStatus",
            state = if (settings.enabled) "enabled" else "disabled",
            "combinedStatusEnabled" to settings.enabled,
            "keyguardEnabled" to settings.keyguardEnabled,
            "aodEnabled" to settings.aodEnabled,
            "preferenceTransportMs" to
                (
                    preferenceTransportLatencyNanos
                        ?.let { nanos -> nanos / 1_000_000.0 }
                        ?: "initial-bind"
                ),
            "eventDriven" to true,
            "mainThread" to true,
            "fallback" to if (settings.enabled) "combined-status" else "native-systemui",
        )
    }

    private fun releaseFeaturePresentationOwnership(source: String) {
        keyguardAodFullTargetPending = false
        keyguardAodPendingTargetToLockScreen = null
        keyguardAodFullTransitionActive = false
        resetKeyguardBoundaryHandoffState()
        homePresentationOwnedAtFullAodStart = false
        homeNativeAodFallbackCandidate = false
        homeNativeAodFallbackActive = false
        homeAodTransitionOriginPending = false
        homeAodTargetPrearmPending = false
        controlCenterSceneEligible = false
        keyguardControlCenterLeaseActive = false
        CombinedStatusControlCenterRenderSession.setSceneEligible(false)
        CombinedStatusControlCenterTransitionOwner.setSceneEligible(false)
        SystemUiHomePresentationOwner.deactivateControlCenter(source)
        SystemUiHomePresentationOwner.deactivateAod(source)
        SystemUiHomePresentationOwner.deactivateKeyguard(source)
        SystemUiHomePresentationOwner.deactivate(source)
        SystemUiNativeBatterySuppressionOwner.deactivate(source)
        SystemUiNativeNetworkSuppressionOwner.deactivate(source)
        CombinedStatusHomeRenderSession.setNativeHandoffActive(true)
    }

    private fun bindRuntimeVisualSettings() {
        runCatching {
            RuntimeVisualPreferencesOwner.bind(
                preferences = getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME),
                onChanged = ::onRuntimeVisualSettingsChanged,
            )
        }.onSuccess { settings ->
            logDiagnostic(
                level = Log.INFO,
                event = "runtimePreferences.bind",
                component = "visualSettings",
                state = "ready",
                "layout" to settings.contentLayout.persistedValue,
                "mobileFollowsBattery" to settings.mobileFollowsBatteryColor,
                "networkFollowsBattery" to settings.centerFollowsBatteryColor,
                "batteryNumber" to settings.batteryTopReadoutEnabled,
                "chargingIcon" to settings.batteryTopChargingIconEnabled,
                "batteryNumberFollowsBattery" to settings.batteryTopTextFollowsBatteryColor,
                "chargingIconFollowsBattery" to
                    settings.batteryTopChargingIconFollowsBatteryColor,
                "transport" to "remote-preferences",
            )
        }.onFailure { error ->
            RuntimeVisualPreferencesOwner.unbind()
            logDiagnostic(
                level = Log.WARN,
                event = "runtimePreferences.bind",
                component = "visualSettings",
                state = "unavailable",
                "reason" to (error.message ?: error.javaClass.simpleName),
            )
        }
    }

    private fun onRuntimeVisualSettingsChanged(
        settings: com.chaners.guiyuan.settings.CombinedStatusVisualSettings,
    ) {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            val dispatch =
                Runnable {
                    onRuntimeVisualSettingsChanged(settings)
                }
            val hostView = SystemUiHostRegistry.currentStatusHost() as? android.view.View
            val scheduled =
                (hostView?.post(dispatch) == true) ||
                    Handler(Looper.getMainLooper()).post(dispatch)
            if (scheduled) {
                return
            }
            logDiagnostic(
                level = Log.ERROR,
                event = "visualSettings.dispatch",
                component = "renderer",
                state = "error",
                "reason" to "main-thread-dispatch-failed",
                "fallback" to "retain-last-visual-settings",
            )
            return
        }

        if (settings != RuntimeVisualPreferencesOwner.currentSettings()) {
            return
        }

        CombinedStatusHomeRenderSession.onVisualSettingsChanged(settings)
        CombinedStatusKeyguardRenderSession.onVisualSettingsChanged(settings)
        CombinedStatusControlCenterRenderSession.onVisualSettingsChanged(settings)
        SystemUiHomePresentationOwner.onVisualSettingsChanged()
        if (detailedDiagnosticsEnabled) {
            logDiagnostic(
                level = Log.INFO,
                event = "visualSettings.changed",
                component = "renderer",
                state = "ready",
                "layout" to settings.contentLayout.persistedValue,
                "combinedScale" to settings.combinedScale,
                "wifiSizeScale" to settings.wifiSizeScale,
                "mobileTypeSizeScale" to settings.mobileTypeSizeScale,
                "mobileFollowsBattery" to settings.mobileFollowsBatteryColor,
                "networkFollowsBattery" to settings.centerFollowsBatteryColor,
                "batteryNumber" to settings.batteryTopReadoutEnabled,
                "chargingIcon" to settings.batteryTopChargingIconEnabled,
                "batteryNumberFollowsBattery" to settings.batteryTopTextFollowsBatteryColor,
                "chargingIconFollowsBattery" to
                    settings.batteryTopChargingIconFollowsBatteryColor,
                "eventDriven" to true,
                "mainThread" to true,
            )
        }
    }

    private fun logCurrentDiagnosticsHealth() {
        val state =
            when {
                !BuildConfig.RUNTIME_DIAGNOSTICS -> "disabled"
                RuntimeDiagnosticsPreferencesOwner.isBound -> "ready"
                else -> "unavailable"
            }
        logDiagnostic(
            level = if (state == "unavailable") Log.WARN else Log.INFO,
            event = "diagnostics.snapshot",
            component = "diagnostics",
            state = state,
            "level" to if (detailedDiagnosticsEnabled) "detailed" else "general",
            "channel" to BuildConfig.BUILD_CHANNEL,
        )
    }

    private fun unbindRuntimeDiagnostics() {
        RuntimeDiagnosticsPreferencesOwner.unbind()
    }

    private fun setDetailedDiagnosticsEnabled(enabled: Boolean) {
        val previous = detailedDiagnosticsEnabled
        detailedDiagnosticsEnabled = enabled
        if (previous != detailedDiagnosticsEnabled) {
            logDiagnostic(
                level = Log.INFO,
                event = "diagnostics.level",
                component = "diagnostics",
                state = "ready",
                "level" to if (detailedDiagnosticsEnabled) "detailed" else "general",
            )
        }
    }

    private fun rotateDiagnosticSession() {
        runtimeSessionId = newRuntimeSessionId()
        diagnosticSequence.set(0L)
        renderTraceSequence.set(0L)
        lastBatteryNumberProbeDiagnosticSummary = null
    }

    private fun beginRenderTrace(source: String): RuntimeRenderTrace? {
        if (!detailedDiagnosticsEnabled) {
            return null
        }

        return RuntimeRenderTrace(
            id = renderTraceSequence.incrementAndGet(),
            source = source,
            sourceNanos = SystemClock.elapsedRealtimeNanos(),
        )
    }

    private fun markStateCommitted(trace: RuntimeRenderTrace?): RuntimeRenderTrace? =
        trace?.withStateCommitted(SystemClock.elapsedRealtimeNanos())

    private fun markPresentationCommitted(trace: RuntimeRenderTrace?): RuntimeRenderTrace? =
        trace?.withPresentationCommitted(SystemClock.elapsedRealtimeNanos())

    private fun onRenderLatencySample(sample: RuntimeRenderLatencySample) {
        if (!detailedDiagnosticsEnabled) {
            return
        }

        logDiagnostic(
            level = Log.INFO,
            event = "pipeline.latency",
            component = "renderLatency",
            state = "observed",
            "traceId" to sample.traceId,
            "source" to sample.source,
            "sourceToStateUs" to sample.sourceToStateUs,
            "sourceToPresentationUs" to sample.sourceToPresentationUs,
            "stateToPresentationUs" to sample.stateToPresentationUs,
            "stateToModelUs" to sample.stateToModelUs,
            "presentationToModelUs" to sample.presentationToModelUs,
            "modelToDrawUs" to sample.modelToDrawUs,
            "sourceToDrawUs" to sample.sourceToDrawUs,
            "commitMainThread" to sample.committedOnMainThread,
            "sampling" to "latest-visible-change-only",
            "healthSnapshot" to false,
        )
    }

    private fun newRuntimeSessionId(): String =
        BuildConfig.BUILD_ID + "-" +
            Process.myPid() + "-" +
            SystemClock.elapsedRealtime().toString(36)

    private fun logDiagnostic(
        level: Int,
        event: String,
        component: String,
        state: String,
        vararg fields: Pair<String, Any?>,
    ) {
        val values =
            buildMap {
                fields.forEach { (key, value) ->
                    if (value != null) {
                        put(key, value.toString())
                    }
                }
                put("sessionId", runtimeSessionId)
                put("uptimeMs", SystemClock.elapsedRealtime().toString())
                put("sequence", diagnosticSequence.incrementAndGet().toString())
            }
        log(
            level,
            TAG,
            RuntimeDiagnosticsProtocol.format(
                event = event,
                component = component,
                state = state,
                fields = values,
            ),
        )
    }

    private companion object {
        const val TAG = "CombinedStatus"
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
