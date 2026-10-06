package com.chaners.guiyuan.xposed

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.settings.FeatureSettings
import com.chaners.guiyuan.settings.RUNTIME_REMOTE_PREFS_NAME
import com.chaners.guiyuan.system.DiagProtocol
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.util.concurrent.atomic.AtomicLong

class GyModule : XposedModule() {
    private var islandMotionSourceInstalled = false
    private var panelTransitionSourceInstalled = false
    private var ccVisible = false
    private var ccEligible = false
    private var ccSourceScene = SourceScene.UNKNOWN
    private var steadyStatusSourceScene = SourceScene.UNKNOWN
    private var lastStableKeyguardAodScene =
        ScenePolicy.StableKeyguardAodScene.UNKNOWN
    private var keyguardAodFullTargetPending = false
    private var aodTargetToLockscreen: Boolean? = null
    private var keyguardAodFullTransitionActive = false
    private var keyguardHandoffActive = false
    private var keyguardPrecommitActive = false
    private var keyguardCompactReady = false
    private var keyguardVisualBoundaryReached = false
    private var homeOwnedAtAodStart = false
    private var homeNativeAodFallbackCandidate = false
    private var homeNativeAodFallbackActive = false
    private var homeAodTransitionOriginPending = false
    private var homeAodTargetPrearmPending = false
    private var ccFraction = 0f
    private var keyguardRuntimeReady = false
    private var aodRendererAttached = false
    private var keyguardPresentationReadyObserved = false
    private var keyguardControlCenterLeaseActive = false
    private var lastBatteryProbeSummary: String? = null
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

        val compatibility = CompatibilityProbe.inspect(param.classLoader)
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

        installHomePresentation(
            classLoader = param.classLoader,
            source = "coldStart",
        )
        installNetworkSuppression(
            classLoader = param.classLoader,
            source = "coldStart",
        )

        runCatching {
            HostRuntime.install(
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

        if (HostRuntime.isReady) {
            installBatteryStateSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            installNetworkStateSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            installPresentationSources(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            installCcTransitionSource(
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
            HotReloadRuntime.prepare(
                param = param,
                generationHandoff =
                    Runnable {
                        teardownOldGenerationForHotReload(
                            continuousHandoff = true,
                        )
                    },
            )
        if (prepared is HotReloadRuntime.PrepareResult.Unavailable) {
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

        prepared as HotReloadRuntime.PrepareResult.Ready
        val hookCount =
            1 +
                BatteryRuntime.installedHookCount +
                NetworkRuntime.installedHookCount +
                PresentationRuntime.installedHookCount +
                HomePresentation.installedHookCount +
                NativeNetworkSuppression.installedHookCount +
                if (islandMotionSourceInstalled) {
                    IslandMotionSource.HOOK_COUNT
                } else {
                    0
                } +
                if (panelTransitionSourceInstalled) {
                    PanelTransitionSource.expectedHookCount(
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
            "controlCenterCompactReady" to prepared.ccCompactReady,
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
            "controlCenterCompactReady" to prepared.ccCompactReady,
        )
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        rotateDiagnosticSession()
        val takeover =
            HotReloadRuntime.takeOverHooks(
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

            BatteryRuntime.resetRuntimeState()
            NetworkRuntime.resetRuntimeState()
            islandMotionSourceInstalled = false
            panelTransitionSourceInstalled = false
            ccVisible = false
            ccEligible = false
            ccSourceScene = SourceScene.UNKNOWN
            steadyStatusSourceScene = SourceScene.UNKNOWN
            lastStableKeyguardAodScene =
                ScenePolicy.StableKeyguardAodScene.UNKNOWN
            keyguardAodFullTargetPending = false
            aodTargetToLockscreen = null
            keyguardAodFullTransitionActive = false
            resetKeyguardHandoff()
            homeOwnedAtAodStart = false
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = false
            homeAodTransitionOriginPending = false
            homeAodTargetPrearmPending = false
            ccFraction = 0f
            keyguardRuntimeReady = false
            aodRendererAttached = false
            keyguardPresentationReadyObserved = false
            keyguardControlCenterLeaseActive = false
            PresentationRuntime.resetRuntimeState()
            KeyguardHostResolver.resetRuntimeState()
            HomePresentation.resetRuntimeState("hotReload")
            NativeNetworkSuppression.resetRuntimeState("hotReload")
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
            installPresentationSources(
                classLoader = classLoader,
                source = "hotReload",
            )
            installHomePresentation(
                classLoader = classLoader,
                source = "hotReload",
            )
            installNetworkSuppression(
                classLoader = classLoader,
                source = "hotReload",
            )
            installCcTransitionSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            installIslandMotionSource(
                classLoader = classLoader,
                source = "hotReload",
            )

            val restored = HotReloadRuntime.restoreTransfer(param)
            if (restored == null) {
                StatusStateStore.restoreHotReloadState(null)
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

            val capture = HostRegistry.restoreStatusHost(restored.host)
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
        capture: HostRegistry.Capture,
        restored: HotReloadTransfer.Restored,
        removedHooks: Int,
    ) {
        runCatching {
            var restoredSnapshot =
                StatusStateStore.restoreHotReloadState(restored.state)
            val bindings =
                NetworkStateSource.restoreHotReloadBindings(restored.bindings)
            NetworkStateSource.seedRestoredWifiState(
                onEvent =
                    if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                        ::onNetworkPipelineEvent
                    } else {
                        null
                    },
            )?.let { wifi ->
                StatusStateStore.updateWifi(wifi)?.let { snapshot ->
                    restoredSnapshot = snapshot
                }
            }

            when (
                val legacy =
                    HomePresentation.cleanupLegacyParticipant(capture.host)
            ) {
                HomePresentation.LegacyCleanupResult.NotPresent -> Unit
                HomePresentation.LegacyCleanupResult.Removed -> {
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
                is HomePresentation.LegacyCleanupResult.Failure -> {
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

            PanelTransitionSource.restoreCcHomeEligibility(
                restored.ccHomeEligible,
            )
            val controlCenterFakeRestore =
                restored.controlCenterFakeHost?.let { fakeHost ->
                    restoreCcAfterReload(
                        host = fakeHost,
                        transferredCompactReady = restored.ccCompactReady,
                    )
                } ?: "late-fallback"
            val transferredTint =
                restored.appliedTint?.let { appliedTint ->
                    TintState(
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
                    (restored.ccHomeEligible ?: "unknown"),
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

    // Runtime owners are installed once per SystemUI generation. Each owner
    // keeps its own cleanup boundary so Hot Reload cannot create two writers.
    private fun installHomePresentation(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                HomePresentation.install(
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
            HomePresentation.InstallResult.Installed,
            HomePresentation.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "homePresentation",
                    state = "ready",
                    "source" to source,
                    "hooks" to HomePresentation.installedHookCount,
                    "carrier" to "MiuiNotificationStatusContainer.overlay",
                    "nativeGeometryWrites" to 0,
                )
            }
            is HomePresentation.InstallResult.Failure -> {
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
                NativeParticipantUi.install(
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
                            is StatusSlotReservation.Result.Ready -> {
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

                            is StatusSlotReservation.Result.Failure -> {
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
            NativeParticipantUi.InstallResult.Installed,
            NativeParticipantUi.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeCombinedParticipant",
                    state = "ready",
                    "source" to source,
                    "hooks" to NativeParticipantUi.installedHookCount,
                    "visible" to false,
                    "nativeGeometryWrites" to 0,
                )
            }

            is NativeParticipantUi.InstallResult.Failure -> {
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

    private fun installNetworkSuppression(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                NativeNetworkSuppression.install(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onObservationAttached = { observationSource ->
                        logDiagnostic(
                            level = Log.INFO,
                            event = "source.attach",
                            component = "statusIconObservation",
                            state = "ready",
                            "source" to observationSource,
                            "trigger" to "home-dark-icon-manager-registration",
                            "mode" to "observation-only",
                            "suppressionWriters" to 0,
                        )
                    },
                    onStatusPresentationChanged = ::onStatusIconPresentationChanged,
                )
        ) {
            NativeNetworkSuppression.InstallResult.Installed,
            NativeNetworkSuppression.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeNetworkSuppression",
                    state = "ready",
                    "source" to source,
                    "hooks" to NativeNetworkSuppression.installedHookCount,
                    "nativeGeometryWrites" to 0,
                )
            }

            is NativeNetworkSuppression.InstallResult.Failure -> {
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
                NativeBatterySuppression.install(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onNativeLayoutHideChanged = { hidden ->
                        NativeParticipantUi
                            .onNativeBatteryLayoutHideChanged(hidden)
                    },
                )
        ) {
            NativeBatterySuppression.InstallResult.Installed,
            NativeBatterySuppression.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeBatterySuppression",
                    state = "ready",
                    "source" to source,
                    "hooks" to NativeBatterySuppression.installedHookCount,
                    "contract" to
                        "MiuiStatusBatteryContainer.setIsHideBattery(Boolean):native-layout-authority+visual-mask",
                    "nativeGeometryWrites" to 0,
                )
            }

            is NativeBatterySuppression.InstallResult.Failure -> {
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

    private fun installParticipantObserver(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                NativeParticipantRuntime.installControllerObserver(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                )
        ) {
            NativeParticipantRuntime.InstallResult.Installed,
            NativeParticipantRuntime.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeParticipantControllerObserver",
                    state = "ready",
                    "source" to source,
                    "hooks" to NativeParticipantRuntime.installedHookCount,
                    "nativeGeometryWrites" to 0,
                )
            }

            is NativeParticipantRuntime.InstallResult.Failure -> {
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
            NetworkRuntime.attach(
                module = this,
                classLoader = classLoader,
                onWifiState = { state ->
                    val trace = beginRenderTrace("wifi")
                    val changed = StatusStateStore.updateWifi(state)
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
                    val changed = StatusStateStore.updateMobile(update)
                    val stateTrace =
                        if (changed != null) {
                            markStateCommitted(trace)
                        } else {
                            trace
                        }
                    refreshMobilePresentation(stateTrace)
                    if (changed != null) {
                        onCombinedStateChanged(
                            snapshot = StatusStateStore.snapshot(),
                            trace = stateTrace,
                        )
                    }
                },
                onMobileSignalWillApply = { image ->
                    NativeNetworkSuppression.preMaskMobileSignal(image)
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
                    NetworkRuntime.installedHookCount == NetworkStateSource.HOOK_COUNT
            val state =
                when {
                    fullyReady -> "ready"
                    NetworkRuntime.installedHookCount > 0 -> "partial"
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
                "hooks" to NetworkRuntime.installedHookCount,
                "expectedHooks" to NetworkStateSource.HOOK_COUNT,
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
                    " hooks=" + NetworkRuntime.installedHookCount +
                    "/" + NetworkStateSource.HOOK_COUNT +
                    " wifi=" + result.wifiReady +
                    " mobile=" + result.mobileReady +
                    " source=" + source +
                    " rebindRequired=" + (source == "hotReload"),
            )
        }.onFailure { error ->
            NetworkRuntime.resetRuntimeState()
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
            IslandMotionSource.install(
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
                handles.size == IslandMotionSource.HOOK_COUNT
            logDiagnostic(
                level = if (islandMotionSourceInstalled) Log.INFO else Log.WARN,
                event = "source.install",
                component = "islandMotion",
                state = if (islandMotionSourceInstalled) "ready" else "partial",
                "hooks" to handles.size,
                "expectedHooks" to IslandMotionSource.HOOK_COUNT,
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


    // Control Center motion stays HyperOS-owned. Guiyuan only projects into
    // the verified QS_FAKE carrier and yields native on ambiguous geometry.
    private fun installCcTransitionSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            PanelTransitionSource.install(
                module = this,
                classLoader = classLoader,
                onUpdate = ::onPanelTransitionUpdate,
                onFakePresentationAttached = ::onCcAttached,
                onRuntimeFailure = ::onPanelTransitionRuntimeFailure,
                onEvent = ::onPanelTransitionEvent,
                isProbeEnabled = {
                    BuildConfig.DEVELOPMENT_PROBES || detailedDiagnosticsEnabled
                },
                includeControlCenterDiagnostics = BuildConfig.RUNTIME_DIAGNOSTICS,
            )
        }.onSuccess { handles ->
            val expectedHooks =
                PanelTransitionSource.expectedHookCount(
                    BuildConfig.RUNTIME_DIAGNOSTICS,
                )
            panelTransitionSourceInstalled = handles.size == expectedHooks
            // Home yields Control Center only after the projected native
            // carrier is structurally ready.
            HomeRenderSession.onControlCenterAuthorityChanged(true)
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
            HomeRenderSession.onControlCenterAuthorityChanged(true)
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
        update: PanelTransitionSource.Update,
    ) {
        val effectiveSourceScene = handleCcPanelUpdate(update)
        val transitionUpdate =
            if (effectiveSourceScene != null) {
                update.copy(ccSourceScene = effectiveSourceScene)
            } else {
                update
            }
        CcTransition.onPanelUpdate(transitionUpdate)

        if (
            detailedDiagnosticsEnabled &&
            update.fraction != null &&
            lastBatteryProbeSummary == null
        ) {
            val batteryNumberProbe =
                CcTransition.latestBatteryNumberProbeDiagnostic()
            if (batteryNumberProbe != null) {
                lastBatteryProbeSummary = batteryNumberProbe
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

    }

    private fun handleCcPanelUpdate(
        update: PanelTransitionSource.Update,
    ): SourceScene? {
        update.fraction?.let(::onCcFraction)

        val visible = update.visible ?: return null
        if (!visible) {
            ccVisible = false
            // Restore Home first. QS_FAKE compact presentation remains prearmed
            // for the lifetime of the native fake root; only the Combined
            // overlay visibility changes with Control Center visibility.
            HomeRenderSession.onControlCenterAuthorityChanged(true)
            CcSession.setRequestedVisible(false)
            return null
        }

        ccVisible = true
        if (!CcSession.beginVisibleCycle()) {
            HomeRenderSession.onControlCenterAuthorityChanged(true)
            logDiagnostic(
                level = Log.WARN,
                event = "projection.visibleCycle",
                component = "controlCenterProjection",
                state = "native",
                "reason" to "visible-cycle-rearm-failed",
                "fallback" to "native-control-center-until-next-native-event",
            )
            return update.ccSourceScene
        }
        val panelSourceScene =
            update.ccSourceScene
                ?: SourceScene.UNKNOWN
        val incomingBoundaryReady =
            incomingKeyguardReady()
        val effectiveSourceScene =
            ScenePolicy.resolveCcSourceScene(
                panelSourceScene = panelSourceScene,
                steadySourceScene = steadyStatusSourceScene,
                lastStableFamilyScene = lastStableKeyguardAodScene,
                incomingKeyguardPresentationReady = incomingBoundaryReady,
            )
        updateCcSource(
            sourceScene = effectiveSourceScene,
            authority =
                when {
                    effectiveSourceScene == panelSourceScene ->
                        "hyperos-realSystemIcons"
                    incomingBoundaryReady &&
                        effectiveSourceScene == SourceScene.KEYGUARD ->
                        "incoming-keyguard-presentation"
                    else -> "steady-source-view-override"
                },
        )
        val carrier = update.ccHost
        if (carrier == null) {
            HomeRenderSession.onControlCenterAuthorityChanged(true)
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

        when (prepareCc(carrier, "visible-fallback")) {
            CcSession.AttachResult.Ready -> {
                val ready =
                    CcSession.setRequestedVisible(true)
                if (!ready) {
                    HomeRenderSession.onControlCenterAuthorityChanged(true)
                }
            }

            is CcSession.AttachResult.Failure -> {
                HomeRenderSession.onControlCenterAuthorityChanged(true)
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

    private fun updateCcSource(
        sourceScene: SourceScene,
        authority: String,
    ) {
        if (
            keyguardControlCenterLeaseActive &&
            sourceScene != SourceScene.KEYGUARD
        ) {
            releaseKeyguardCcLease(
                source = "source-scene:" + sourceScene.name + ":" + authority,
                reconcileReadiness = true,
            )
        }
        ccSourceScene = sourceScene
        HomePresentation.updateCcSourceScene(sourceScene)
        acquireKeyguardCcLease(
            source = "source-scene:" + authority,
        )
        val settings = FeaturePrefsOwner.currentSettings()
        val incomingBoundaryReady =
            incomingKeyguardReady()
        val keyguardPresentationReady =
            keyguardRuntimeReady || incomingBoundaryReady
        val keyguardEligible =
            settings.enabled &&
                settings.keyguardEnabled &&
                keyguardPresentationReady
        val nextEligible =
            ScenePolicy.controlCenterProjectionEligible(
                featureEnabled = settings.enabled,
                sourceScene = sourceScene,
                keyguardEnabled = keyguardEligible,
            )
        if (nextEligible == ccEligible) {
            return
        }

        ccEligible = nextEligible
        CcSession.setSceneEligible(nextEligible)
        CcTransition.setSceneEligible(nextEligible)
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
            "controlCenterVisible" to ccVisible,
            "fallback" to if (nextEligible) "combined-qs-fake" else "native-qs-fake",
            "nativeGeometryWrites" to 0,
        )
    }

    private fun onCcFraction(rawFraction: Float) {
        val fraction = rawFraction.coerceIn(0f, 1f)
        val previous = ccFraction
        ccFraction = fraction

        if (fraction > 0f) {
            if (
                ccSourceScene != SourceScene.KEYGUARD &&
                incomingKeyguardReady()
            ) {
                updateCcSource(
                    sourceScene = SourceScene.KEYGUARD,
                    authority = "incoming-keyguard-fraction",
                )
            }
            acquireKeyguardCcLease(
                source = "native-fraction",
            )
            return
        }

        if (
            keyguardControlCenterLeaseActive &&
            previous > 0f
        ) {
            releaseKeyguardCcLease(
                source = "native-fraction-zero",
                reconcileReadiness = true,
            )
        }
    }

    private fun acquireKeyguardCcLease(source: String) {
        val keyguardPresentationReady =
            keyguardRuntimeReady ||
                incomingKeyguardReady()
        if (
            keyguardControlCenterLeaseActive ||
            !ScenePolicy.shouldAcquireKeyguardCcLease(
                sourceScene = ccSourceScene,
                keyguardPresentationReady = keyguardPresentationReady,
                nativeFraction = ccFraction,
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
            "sourceScene" to ccSourceScene.name,
            "nativeFraction" to ccFraction,
            "cleanupBoundary" to "native-fraction-zero-or-authoritative-source-change",
            "timingDelay" to false,
            "nativeGeometryWrites" to 0,
        )
    }

    private fun shouldKeepKeyguardCcLease(): Boolean {
        val resolved =
            KeyguardHostResolver.current()
                as? KeyguardHostResolver.ResolveResult.Ready
                ?: return false
        val settings = FeaturePrefsOwner.currentSettings()
        val aodBlocked =
            KeyguardAodSource
                .currentState(resolved.host.battery)
                ?.blocksProjection
                ?: true
        val incomingBoundaryReady =
            incomingKeyguardReady()
        return ScenePolicy.shouldKeepKeyguardCcLease(
            leaseActive = keyguardControlCenterLeaseActive,
            sourceScene = ccSourceScene,
            featureEnabled = settings.enabled,
            keyguardEnabled = settings.keyguardEnabled,
            hostAttached = resolved.host.systemIcons.isAttachedToWindow,
            aodBlocked = aodBlocked,
            incomingBoundaryPresentationReady = incomingBoundaryReady,
            nativeFraction = ccFraction,
        )
    }

    private fun releaseKeyguardCcLease(
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
            "sourceScene" to ccSourceScene.name,
            "nativeFraction" to ccFraction,
            "observedReady" to keyguardPresentationReadyObserved,
            "reconcileReadiness" to reconcileReadiness,
            "timingDelay" to false,
            "nativeGeometryWrites" to 0,
        )
        if (
            reconcileReadiness &&
            !keyguardPresentationReadyObserved &&
            !incomingKeyguardReady()
        ) {
            applyKeyguardNotReady(
                source = "lease-release:" + source,
            )
        }
    }

    private fun incomingKeyguardReady(): Boolean {
        val settings = FeaturePrefsOwner.currentSettings()
        if (
            !settings.enabled ||
            !settings.keyguardEnabled ||
            settings.aodEnabled
        ) {
            return false
        }
        val resolved =
            KeyguardHostResolver.current()
                as? KeyguardHostResolver.ResolveResult.Ready
                ?: return false
        return ScenePolicy.incomingKeyguardPresentationReady(
            visualHandoffActive = keyguardHandoffActive,
            layoutPrecommitActive = keyguardPrecommitActive,
            compactLayoutReady = keyguardCompactReady,
            visualBoundaryReached = keyguardVisualBoundaryReached,
            hostAttached = resolved.host.systemIcons.isAttachedToWindow,
        )
    }

    private fun refreshCcSource(authority: String) {
        updateCcSource(
            sourceScene = ccSourceScene,
            authority = authority,
        )
    }

    private fun reconcileKeyguardCc(authority: String) {
        if (
            !ScenePolicy.shouldReconcileKeyguardCc(
                controlCenterVisible = ccVisible,
                nativeFraction = ccFraction,
                leaseActive = keyguardControlCenterLeaseActive,
            )
        ) {
            return
        }
        refreshCcSource(authority)
    }

    private fun restoreCcAfterReload(
        host: ViewGroup,
        transferredCompactReady: Boolean,
    ): String {
        return when (
            val result =
                CcSession.restoreLaidOutHost(
                    host = host,
                    onEvent = ::onPanelTransitionEvent,
                    isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                    onProjectionReadinessChanged = ::onCcProjectionReady,
                    transferredCompactReady = transferredCompactReady,
                )
        ) {
            CcSession.AttachResult.Ready -> {
                val compactReady =
                    CcSession
                        .isNativeReadyForReload()
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

            is CcSession.AttachResult.Failure -> {
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
                onCcAttached(host)
                "fallback-first-native-layout:" + result.reason
            }
        }
    }

    private fun onCcAttached(host: ViewGroup) {
        when (
            val result =
                CcSession.prearmAfterNextNativeLayout(
                    host = host,
                    onEvent = ::onPanelTransitionEvent,
                    isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                    onProjectionReadinessChanged = ::onCcProjectionReady,
                )
        ) {
            is CcSession.PrearmResult.Scheduled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "projection.prearm",
                    component = "controlCenterProjection",
                    state = "scheduled",
                    "source" to "fake-root-attached",
                    "boundary" to "first-native-layout",
                    "reused" to result.reused,
                    "requestedVisible" to ccVisible,
                    "nativeGeometryWrites" to 0,
                )
            }

            is CcSession.PrearmResult.Failure -> {
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

    private fun prepareCc(
        host: ViewGroup,
        source: String,
    ): CcSession.AttachResult {
        val result =
            CcSession.attach(
                host = host,
                onEvent = ::onPanelTransitionEvent,
                isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                onProjectionReadinessChanged = ::onCcProjectionReady,
            )
        if (result is CcSession.AttachResult.Failure) {
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

    private fun onCcProjectionReady(ready: Boolean) {
        CcTransition.onProjectionReadinessChanged(ready)
        if (!ccVisible) {
            return
        }
        // Projected owner is already visible when ready=true. On the reverse
        // edge Home is restored before the projected owner is removed.
        HomeRenderSession.onControlCenterAuthorityChanged(!ready)
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
                releaseKeyguardCcLease(
                    source = "panel-runtime-failure",
                    reconcileReadiness = false,
                )
            }
        }
        ccEligible = false
        ccSourceScene = SourceScene.UNKNOWN
        safely {
            HomePresentation.updateCcSourceScene(SourceScene.UNKNOWN)
        }
        safely {
            CcTransition.setSceneEligible(false)
        }
        safely {
            CcTransition.detach("panel-runtime-failure")
        }
        safely {
            CcSession.setSceneEligible(false)
        }
        safely {
            HomeRenderSession.onControlCenterAuthorityChanged(true)
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

    // Native state sources only report facts. They never decide which scene
    // owns presentation; that decision stays in the scene/presentation layer.
    private fun installBatteryStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            BatteryRuntime.attach(
                module = this,
                classLoader = classLoader,
                onBatteryState = { state ->
                    val trace = beginRenderTrace("battery")
                    StatusStateStore.updateBattery(state)?.let { snapshot ->
                        onCombinedStateChanged(
                            snapshot = snapshot,
                            trace = markStateCommitted(trace),
                        )
                    }
                },
                onChargingIconResource = { resourceId ->
                    val trace = beginRenderTrace("battery-charging-glyph")
                    StatusStateStore.updateBatteryChargingIcon(resourceId)
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
                "expectedHooks" to BatterySource.HOOK_COUNT,
                "source" to source,
                "authority" to
                    "MiuiBatteryMeterIconView.getProgressStatus() via " +
                    "BatteryController callbacks",
                "eventDriven" to true,
            )
        }.onFailure { error ->
            BatteryRuntime.resetRuntimeState()
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

    private fun installPresentationSources(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            PresentationRuntime.attach(
                module = this,
                classLoader = classLoader,
                onTintState = ::onTintStateUpdate,
                onSceneState = ::onSceneStateUpdate,
                onKeyguardAodState = ::onKeyguardAodStateUpdate,
                onAodTransitionStart = ::onAodTransitionStart,
                onAodTransitionCommit = ::onAodTransitionCommit,
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
                KeyguardHostResolver.current()?.let { resolution ->
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
                state = StatusStateStore.snapshot(),
                pendingMobileTypeDrawable = pendingMobileTypeDrawable,
            )
        val changed =
            PresentationStore.updateMobilePresentation(presentation)
        val recoveryCompleted =
            StatusStateStore.completeMobileRecoveryIfReady(
                preferredSubscriptionId = presentation.effectiveDataSubscriptionId ?: -1,
                mobileTypeReady = presentation.networkType != null,
                mobileDataEnabled =
                    PresentationStore
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
        snapshot: StatusStateStore.Snapshot,
        trace: RuntimeRenderTrace? = null,
    ) {
        HomeRenderSession.onState(snapshot, trace)
        KeyguardRenderSession.onState(snapshot)
        CcSession.onState(snapshot)
    }

    private fun onPresentationStateChanged(trace: RuntimeRenderTrace? = null) {
        HomeRenderSession.onPresentationStateChanged(trace)
        KeyguardRenderSession.onPresentationStateChanged()
        CcSession.onPresentationStateChanged()
        refreshStatusIconObservation("presentation")
    }

    private fun refreshStatusIconObservation(source: String) {
        NativeNetworkSuppression.refreshObservation(source)
    }

    private fun onStatusIconPresentationChanged(
        state: PresentationStore.StatusIconPresentation,
    ) {
        val trace = beginRenderTrace("statusIcons")
        val changed =
            PresentationStore.updateStatusIcons(state)

        HomeRenderSession.onStatusIconTintUpdate(
            state.appliedTint,
        )
        KeyguardRenderSession.onPresentationStateChanged()
        CcSession.onPresentationStateChanged()

        if (changed != null) {
            val presentationTrace = markPresentationCommitted(trace)
            HomeRenderSession.onPresentationStateChanged(
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
                                ?.function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }(16)
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

    private fun onTintStateUpdate(update: TintSource.TintUpdate) {
        KeyguardRenderSession.onTintUpdate(update)
        val liveStatusIconTint =
            NativeNetworkSuppression.currentAppliedStatusIconTint()
        val resolvedState =
            TintAuthority.resolveBatteryEvent(
                batteryState = update.state,
                liveStatusIconTint = liveStatusIconTint,
            )
        val resolvedUpdate = update.copy(state = resolvedState)
        HomeRenderSession.onTintUpdate(resolvedUpdate)
        CcSession.onTintUpdate(resolvedUpdate)
        if (detailedDiagnosticsEnabled) {
            log(
                Log.INFO,
                TAG,
                "tintCommit source=batteryDarkReceiver" +
                    " applied=#" +
                    resolvedState.appliedTint.toUInt().function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }(16).padStart(8, '0') +
                    " statusIcon=#" +
                    (
                        resolvedState.statusIconTint
                            ?.toUInt()
                            ?.function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }(16)
                            ?.padStart(8, '0')
                            ?: "none"
                    ) +
                    " liveStatusIcon=#" +
                    (
                        liveStatusIconTint
                            ?.toUInt()
                            ?.function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }(16)
                            ?.padStart(8, '0')
                            ?: "none"
                    ) +
                    " authority=live-systemui-status-icons",
            )
        }
    }

    // Keyguard and AOD share one family renderer. The handoff state below
    // keeps one visual writer while native callbacks change scene authority.
    private fun onAodTransitionStart() {
        val settings = FeaturePrefsOwner.currentSettings()
        val homeOwnedAtStart =
            HomePresentation
                .homeOwnedSlots()
                .isNotEmpty()
        val homeCarrierVisibleAtStart =
            HomePresentation
                .isHomeCarrierVisible()

        keyguardAodFullTransitionActive = true
        aodTargetToLockscreen = null
        homeOwnedAtAodStart = homeOwnedAtStart
        if (
            ScenePolicy.shouldArmHomeAodFallback(
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
                steadyStatusSourceScene == SourceScene.HOME &&
                lastStableKeyguardAodScene ==
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN &&
                homeOwnedAtStart &&
                (settings.keyguardEnabled || settings.aodEnabled)
        keyguardAodFullTargetPending =
            PresentationRuntime.keyguardStatusIconReady &&
                lastStableKeyguardAodScene !=
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN &&
                settings.keyguardEnabled != settings.aodEnabled

        logDiagnostic(
            level = Log.INFO,
            event = "aod.targetWindow",
            component = "keyguardAod",
            state = if (keyguardAodFullTargetPending) "pending" else "observation-only",
            "source" to "animateFullAod:before",
            "visualBoundaryAuthority" to
                if (PresentationRuntime.keyguardStatusIconReady) {
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

    private fun onAodTransitionCommit() {
        keyguardAodFullTransitionActive = false
        val settings = FeaturePrefsOwner.currentSettings()
        val resolution = KeyguardHostResolver.current()
        val target =
            (resolution as? KeyguardHostResolver.ResolveResult.Ready)
                ?.host
                ?.let { resolved ->
                    KeyguardHostResolver.nativeToLockScreenTarget(resolved)
                }

        if (keyguardAodFullTargetPending) {
            aodTargetToLockscreen = target
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
            ScenePolicy.shouldReleaseHomeForAodOff(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguardEnabled,
                aodEnabled = settings.aodEnabled,
                homeNativeAodFallbackCandidate =
                    homeNativeAodFallbackCandidate,
                homeOwnedAtAodStart =
                    homeOwnedAtAodStart,
                nativeToLockScreenTarget = target,
            )
        if (releaseTransientHomeKeyguard) {
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = true
            homeAodTransitionOriginPending = false
            homeAodTargetPrearmPending = false
            resetKeyguardHandoff()
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

        (resolution as? KeyguardHostResolver.ResolveResult.Ready)?.let { ready ->
            if (!releaseTransientHomeKeyguard) {
                armKeyguardHandoff(
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
        homeOwnedAtAodStart = false
    }

    private fun onKeyguardStatusIconTransition() {
        val resolution =
            KeyguardHostResolver.current()
                as? KeyguardHostResolver.ResolveResult.Ready
                ?: run {
                    keyguardAodFullTargetPending = false
                    aodTargetToLockscreen = null
                    homeOwnedAtAodStart = false
                    homeNativeAodFallbackCandidate = false
                    homeNativeAodFallbackActive = false
                    homeAodTransitionOriginPending = false
                    homeAodTargetPrearmPending = false
                    return
                }
        val aodState =
            KeyguardAodSource.currentState(resolution.host.battery)
        val target =
            KeyguardHostResolver.nativeToLockScreenTarget(resolution.host)

        if (keyguardAodFullTargetPending && target != null) {
            aodTargetToLockscreen = target
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
                KeyguardHostResolver.statusIconsPresentationAlpha(
                    resolution.host,
                ),
            "authority" to "native-status-icon-animation",
            "eventDriven" to true,
            "readOnly" to true,
            "nativeGeometryWrites" to 0,
        )
        if (!eligible) return

        val visualOnlyIncomingKeyguard =
            armKeyguardHandoff(
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
        aodTargetToLockscreen = null
    }

    private fun armKeyguardHandoff(
        resolution: KeyguardHostResolver.ResolveResult.Ready,
        nativeToLockScreenTarget: Boolean?,
        source: String,
        visualBoundaryReached: Boolean,
    ): Boolean {
        if (keyguardHandoffActive) {
            if (visualBoundaryReached) {
                onKeyguardVisualBoundary(source)
            }
            return true
        }

        val settings = FeaturePrefsOwner.currentSettings()
        val eligible =
            ScenePolicy.shouldUseKeyguardHandoff(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguardEnabled,
                aodEnabled = settings.aodEnabled,
                lastStableFamilyScene = lastStableKeyguardAodScene,
                nativeToLockScreenTarget = nativeToLockScreenTarget,
                homeNativeAodFallbackActive = homeNativeAodFallbackActive,
            )
        if (!eligible) return false

        beginKeyguardHandoff(
            resolution = resolution,
            source = source,
            visualBoundaryReached = visualBoundaryReached,
        )
        return keyguardHandoffActive
    }

    private fun beginKeyguardHandoff(
        resolution: KeyguardHostResolver.ResolveResult.Ready,
        source: String,
        visualBoundaryReached: Boolean,
    ) {
        val settings = FeaturePrefsOwner.currentSettings()
        keyguardHandoffActive = true
        val statusIconsAlphaAtArm =
            KeyguardHostResolver.statusIconsPresentationAlpha(
                resolution.host,
            )
        keyguardPrecommitActive =
            ScenePolicy.shouldPrecommitKeyguardLayout(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguardEnabled,
                aodEnabled = settings.aodEnabled,
                lastStableFamilyScene = lastStableKeyguardAodScene,
                nativeToLockScreenTarget =
                    KeyguardHostResolver.nativeToLockScreenTarget(
                        resolution.host,
                    ),
                statusIconsPresentationAlpha = statusIconsAlphaAtArm,
                homeNativeAodFallbackActive = homeNativeAodFallbackActive,
            )
        keyguardCompactReady = false
        keyguardVisualBoundaryReached = visualBoundaryReached

        val attached =
            attachKeyguardRenderer(
                resolved = resolution.host,
                source = source + ":visual-only",
            )
        if (!attached) {
            resetKeyguardHandoff()
            return
        }
        logDiagnostic(
            level = Log.INFO,
            event = "aod.visualHandoff",
            component = "keyguardPresentation",
            state = "armed",
            "source" to source,
            "nativeLayoutOwnership" to
                if (keyguardPrecommitActive) {
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

    private fun precommitKeyguardLayout(source: String) {
        if (
            !keyguardHandoffActive ||
            !keyguardPrecommitActive
        ) {
            return
        }
        when (
            val result =
                HomePresentation.commitKeyguardLayout()
        ) {
            is HomePresentation.StateResult.Active -> {
                onKeyguardPrelayoutReady(
                    result = result,
                    source = source + ":precommit-ready",
                )
            }

            is HomePresentation.StateResult.Prepared -> {
                keyguardRuntimeReady = false
                KeyguardRenderSession.setNativeHandoffActive(true)
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

            is HomePresentation.StateResult.Failure -> {
                onKeyguardRuntimeFailure(result.reason)
                deactivateKeyguardRuntime("boundary-prelayout-commit-failed")
            }

            is HomePresentation.StateResult.Inactive -> {
                deactivateKeyguardRuntime("boundary-prelayout-session-missing")
            }
        }
    }

    private fun onKeyguardPrelayoutReady(
        result: HomePresentation.StateResult.Active,
        source: String,
    ) {
        if (
            !keyguardHandoffActive ||
            !keyguardPrecommitActive
        ) {
            completeKeyguardCutover(
                result = result,
                source = source,
            )
            return
        }
        keyguardCompactReady = true
        keyguardRuntimeReady = false
        KeyguardRenderSession.setNativeHandoffActive(
            !keyguardVisualBoundaryReached,
        )
        logDiagnostic(
            level = Log.INFO,
            event = "presentation.cutover",
            component = "keyguardPresentation",
            state =
                if (keyguardVisualBoundaryReached) {
                    "visual-handoff-prelayout-ready"
                } else {
                    "prelayout-ready-hidden"
                },
            "source" to source,
            "representedSlots" to result.representedSlots,
            "maskedViews" to result.maskedViews,
            "nativeVisualBoundaryReached" to keyguardVisualBoundaryReached,
            "nativeGeometryWrites" to 0,
        )
        if (keyguardVisualBoundaryReached) {
            reconcileKeyguardCc(
                "keyguard-boundary-layout-ready",
            )
        }
    }

    private fun onKeyguardVisualBoundary(source: String) {
        keyguardVisualBoundaryReached = true
        if (!keyguardPrecommitActive) return
        if (keyguardCompactReady) {
            KeyguardRenderSession.setNativeHandoffActive(false)
            logDiagnostic(
                level = Log.INFO,
                event = "aod.visualHandoff",
                component = "keyguardPresentation",
                state = "revealed",
                "source" to source,
                "layoutAuthority" to "precommitted-before-native-animation",
                "nativeGeometryWrites" to 0,
            )
            reconcileKeyguardCc(
                "keyguard-boundary-visual-ready",
            )
        } else {
            KeyguardRenderSession.setNativeHandoffActive(true)
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

    private fun resetKeyguardHandoff() {
        keyguardHandoffActive = false
        keyguardPrecommitActive = false
        keyguardCompactReady = false
        keyguardVisualBoundaryReached = false
    }

    private fun completeKeyguardHandoff(source: String): Boolean {
        if (!keyguardHandoffActive) return false
        if (keyguardPrecommitActive) {
            resetKeyguardHandoff()
            onKeyguardReadyChanged(
                ready = true,
                source = source + ":precommitted-layout",
            )
            return true
        }
        keyguardHandoffActive = false
        return when (
            val result =
                HomePresentation.commitKeyguardLayout()
        ) {
            is HomePresentation.StateResult.Active -> {
                completeKeyguardCutover(
                    result = result,
                    source = source + ":compact-ready",
                )
                true
            }

            is HomePresentation.StateResult.Prepared -> {
                keyguardRuntimeReady = false
                KeyguardRenderSession.setNativeHandoffActive(false)
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

            is HomePresentation.StateResult.Failure -> {
                onKeyguardRuntimeFailure(result.reason)
                deactivateKeyguardRuntime("boundary-layout-commit-failed")
                true
            }

            is HomePresentation.StateResult.Inactive -> {
                deactivateKeyguardRuntime("boundary-layout-session-missing")
                true
            }
        }
    }

    private fun armHomeAodTargetPrearmIfEligible(
        resolution: KeyguardHostResolver.ResolveResult.Ready,
        nativeToLockScreenTarget: Boolean?,
        source: String,
    ): Boolean {
        val settings = FeaturePrefsOwner.currentSettings()
        val homeOwned =
            HomePresentation
                .homeOwnedSlots()
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
            ScenePolicy.shouldArmHomeAodTargetPrearm(
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
        update: KeyguardAodSource.AodUpdate,
    ) {
        if (
            !update.isAodAnimate &&
            !keyguardAodFullTransitionActive &&
            ScenePolicy.aodTargetReachedStableState(
                pendingTargetToLockScreen = aodTargetToLockscreen,
                toAod = update.toAod,
                isAodAnimate = update.isAodAnimate,
            )
        ) {
            keyguardAodFullTargetPending = false
            aodTargetToLockscreen = null
        }

        val settings = FeaturePrefsOwner.currentSettings()
        val activateHomeNativeAodFallback =
            ScenePolicy.shouldConsumeHomeAodFallback(
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
            resetKeyguardHandoff()
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
            KeyguardAodSource.isStableAod(
                toAod = update.toAod,
                isAodAnimate = update.isAodAnimate,
            )
        if (!keyguardAodFullTransitionActive && stableAod) {
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = false
            homeAodTransitionOriginPending = false
            homeAodTargetPrearmPending = false
            if (keyguardHandoffActive) {
                resetKeyguardHandoff()
                deactivateKeyguardRuntime("boundary-handoff-returned-to-aod")
            }
        }

        refreshStableSceneFromAod(update)
        if (
            !update.isAodAnimate &&
            !update.toAod &&
            steadyStatusSourceScene == SourceScene.KEYGUARD
        ) {
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = false
        }
        if (update.blocksProjection) {
            releaseKeyguardCcLease(
                source = "aod:" + update.source,
                reconcileReadiness = false,
            )
        }
        val boundaryHandoffHandled =
            !update.isAodAnimate &&
                !update.toAod &&
                completeKeyguardHandoff(
                    source = "aod:" + update.source,
                )
        if (!boundaryHandoffHandled && !keyguardHandoffActive) {
            KeyguardHostResolver.current()?.let { resolution ->
                onKeyguardHostResolution(
                    resolution = resolution,
                    source = "aod:" + update.source,
                )
            }
        }
        KeyguardRenderSession.onAodState(update)
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
                    when (aodTargetToLockscreen) {
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

    private fun onSceneStateUpdate(update: SceneSource.SceneUpdate) {
        val sourceScene = SceneSource.steadySourceScene(update)
        if (sourceScene != SourceScene.UNKNOWN) {
            steadyStatusSourceScene = sourceScene
        }
        refreshStableSceneFromStatusBar(update, sourceScene)
        if (sourceScene == SourceScene.KEYGUARD) {
            KeyguardHostResolver.observe(update)?.let { resolution ->
                onKeyguardHostResolution(
                    resolution = resolution,
                    source = "scene-state",
                )
            }
        }
        if (sourceScene == SourceScene.HOME) {
            if (!keyguardAodFullTransitionActive) {
                homeNativeAodFallbackActive = false
            }
            // UNLOCKED_STATUS_BAR + Home ancestry is the authoritative unlock
            // boundary. A Keyguard Control Center lease must never outlive it:
            // otherwise a fast first pull-down can consume stale KEYGUARD
            // source state and temporarily fall back to native QS icons.
            if (keyguardControlCenterLeaseActive) {
                releaseKeyguardCcLease(
                    source = "authoritative-home",
                    reconcileReadiness = false,
                )
            }
            updateCcSource(
                sourceScene = SourceScene.HOME,
                authority = "steady-source-view",
            )

            if (aodRendererAttached) {
                val retainAodHandoff =
                    HomePresentation.currentAodPresentationClaimed()
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
        } else if (sourceScene != SourceScene.UNKNOWN) {
            updateCcSource(
                sourceScene = sourceScene,
                authority = "steady-source-view",
            )
        }

        if (BuildConfig.RUNTIME_DIAGNOSTICS) {
            KeyguardHostProbe.capture(update)?.let(::onKeyguardHostProbe)
        }

        TintSource.currentState(update.sourceView)?.let { state ->
            onTintStateUpdate(
                TintSource.TintUpdate(
                    sourceView = update.sourceView,
                    state = state,
                ),
            )
        }
        if (
            update.surface ==
                SceneSource.Surface.UNLOCKED_STATUS_BAR
        ) {
            refreshStatusIconObservation("scene-unlocked")
        }
    }

    private fun refreshStableSceneFromAod(
        update: KeyguardAodSource.AodUpdate,
    ) {
        if (update.isAodAnimate) return
        val next =
            when {
                KeyguardAodSource.isStableAod(
                    toAod = update.toAod,
                    isAodAnimate = update.isAodAnimate,
                ) ->
                    ScenePolicy.StableKeyguardAodScene.AOD

                !update.toAod &&
                    steadyStatusSourceScene == SourceScene.KEYGUARD ->
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD

                steadyStatusSourceScene == SourceScene.HOME ->
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN

                else -> null
            }
        if (next != null) {
            updateStableKeyguardAodScene(next, "aod:" + update.source)
        }
    }

    private fun refreshStableSceneFromStatusBar(
        update: SceneSource.SceneUpdate,
        sourceScene: SourceScene,
    ) {
        when (sourceScene) {
            SourceScene.HOME -> {
                val aodState = KeyguardAodSource.currentState(update.sourceView)
                if (
                    aodState != null &&
                    !aodState.isAodAnimate &&
                    !aodState.toAod
                ) {
                    updateStableKeyguardAodScene(
                        ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                        "scene-home-stable",
                    )
                }
            }

            SourceScene.KEYGUARD -> {
                val aodState = KeyguardAodSource.currentState(update.sourceView)
                if (
                    aodState != null &&
                    !aodState.isAodAnimate &&
                    !KeyguardAodSource.isStableAod(
                        toAod = aodState.toAod,
                        isAodAnimate = aodState.isAodAnimate,
                    )
                ) {
                    updateStableKeyguardAodScene(
                        ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                        "scene-keyguard",
                    )
                }
            }

            SourceScene.UNKNOWN -> Unit
        }
    }

    private fun updateStableKeyguardAodScene(
        next: ScenePolicy.StableKeyguardAodScene,
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

    private fun resolveKeyguardAodProjection(
        resolved: KeyguardHostResolver.ResolvedHost,
        fullAodVisualBoundary: Boolean = false,
    ): ScenePolicy.KeyguardAodProjection? {
        val settings = FeaturePrefsOwner.currentSettings()
        val aodState =
            KeyguardAodSource.currentState(resolved.battery)
                ?: return null
        return ScenePolicy.resolveKeyguardAodProjection(
            featureEnabled = settings.enabled,
            keyguardEnabled = settings.keyguardEnabled,
            aodEnabled = settings.aodEnabled,
            toAod = aodState.toAod,
            isAodAnimate = aodState.isAodAnimate,
            steadySourceScene = steadyStatusSourceScene,
            lastStableFamilyScene = lastStableKeyguardAodScene,
            homePresentationOwned =
                HomePresentation
                    .homeOwnedSlots()
                    .isNotEmpty(),
            keyguardStatusIconsAlpha =
                KeyguardHostResolver
                    .statusIconsPresentationAlpha(resolved),
            nativeToLockScreenTarget =
                KeyguardHostResolver
                    .nativeToLockScreenTarget(resolved),
            fullAodTargetSourceReady =
                PresentationRuntime.keyguardFullAodReady,
            fullAodTargetPending = keyguardAodFullTargetPending,
            fullAodVisualBoundary = fullAodVisualBoundary,
            homeAodTransitionOrigin = homeAodTransitionOriginPending,
            homeAodTargetPrearm = homeAodTargetPrearmPending,
            homeNativeAodFallbackActive = homeNativeAodFallbackActive,
        )
    }

    private fun onKeyguardHostResolution(
        resolution: KeyguardHostResolver.ResolveResult,
        source: String,
        fullAodVisualBoundary: Boolean = false,
    ) {
        val settings = FeaturePrefsOwner.currentSettings()
        when (resolution) {
            is KeyguardHostResolver.ResolveResult.Ready -> {
                if (!settings.enabled) {
                    deactivateAodRuntime("feature-ineligible")
                    deactivateKeyguardRuntime("feature-ineligible")
                    return
                }
                if (!PresentationRuntime.keyguardAodReady) {
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
                    resolveKeyguardAodProjection(
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
                    ScenePolicy.KeyguardAodProjection.AOD -> {
                        if (
                            attachAodRenderer(
                                resolved = resolution.host,
                                source = source,
                            )
                        ) {
                            deactivateKeyguardRuntime("aod-family-handoff")
                        }
                    }

                    ScenePolicy.KeyguardAodProjection.KEYGUARD -> {
                        if (
                            attachKeyguardRenderer(
                                resolved = resolution.host,
                                source = source,
                            )
                        ) {
                            deactivateAodRuntime("keyguard-family-handoff")
                        }
                    }

                    ScenePolicy.KeyguardAodProjection.NATIVE -> {
                        deactivateAodRuntime("keyguard-aod-native")
                        deactivateKeyguardRuntime("keyguard-aod-native")
                    }
                }
            }

            is KeyguardHostResolver.ResolveResult.Inactive -> {
                deactivateAodRuntime(
                    "scene-inactive:" + resolution.surface.name,
                )
                deactivateKeyguardRuntime(
                    "scene-inactive:" + resolution.surface.name,
                )
            }

            is KeyguardHostResolver.ResolveResult.Failure -> {
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
        resolved: KeyguardHostResolver.ResolvedHost,
        source: String,
    ): Boolean {
        return when (
            val result =
                KeyguardRenderSession.attach(
                    resolved = resolved,
                    sceneEligible = true,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                    onPresentationReadinessChanged = { ready ->
                        onKeyguardReadyChanged(
                            ready = ready,
                            source = source,
                        )
                    },
                )
        ) {
            KeyguardRenderSession.AttachResult.Ready -> {
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

            is KeyguardRenderSession.AttachResult.Failure -> {
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
        resolved: KeyguardHostResolver.ResolvedHost,
        source: String,
    ): Boolean {
        return when (
            val result =
                KeyguardRenderSession.attachAod(
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
            KeyguardRenderSession.AttachResult.Ready -> {
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

            is KeyguardRenderSession.AttachResult.Failure -> {
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

    private fun onKeyguardReadyChanged(
        ready: Boolean,
        source: String,
    ) {
        keyguardPresentationReadyObserved = ready
        if (!ready) {
            if (incomingKeyguardReady()) {
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.readiness",
                    component = "keyguardPresentation",
                    state = "retained",
                    "source" to source,
                    "reason" to "incoming-boundary-presentation-ready",
                    "nativeFraction" to ccFraction,
                    "leaseActive" to keyguardControlCenterLeaseActive,
                    "timingDelay" to false,
                    "nativeGeometryWrites" to 0,
                )
                return
            }
            if (shouldKeepKeyguardCcLease()) {
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.readiness",
                    component = "keyguardPresentation",
                    state = "retained",
                    "source" to source,
                    "nativeFraction" to ccFraction,
                    "leaseActive" to keyguardControlCenterLeaseActive,
                    "cleanupDeferredUntil" to "native-control-center-handoff-end",
                    "timingDelay" to false,
                    "nativeGeometryWrites" to 0,
                )
                return
            }
            applyKeyguardNotReady(source)
            return
        }

        val settings = FeaturePrefsOwner.currentSettings()
        if (!settings.enabled || !settings.keyguardEnabled) {
            deactivateKeyguardRuntime("feature-ineligible")
            return
        }

        val resolved = KeyguardHostResolver.current()
        if (resolved !is KeyguardHostResolver.ResolveResult.Ready) {
            deactivateKeyguardRuntime("resolver-not-ready")
            return
        }
        if (
            resolveKeyguardAodProjection(
                resolved = resolved.host,
                fullAodVisualBoundary = keyguardHandoffActive,
            ) != ScenePolicy.KeyguardAodProjection.KEYGUARD
        ) {
            deactivateKeyguardRuntime("projection-ineligible")
            return
        }

        val visualOnlyBoundary = keyguardHandoffActive
        when (
            val result =
                HomePresentation.activateKeyguard(
                    resolved = resolved.host,
                    deferNativeLayoutOwnershipUntilCommit = visualOnlyBoundary,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onFailNative = ::onKeyguardRuntimeFailure,
                    onReady = { active ->
                        if (
                            keyguardHandoffActive &&
                            keyguardPrecommitActive
                        ) {
                            onKeyguardPrelayoutReady(
                                result = active,
                                source = "native-layout",
                            )
                        } else {
                            completeKeyguardCutover(
                                result = active,
                                source = "native-layout",
                            )
                        }
                    },
                )
        ) {
            is HomePresentation.StateResult.Active -> {
                if (visualOnlyBoundary && keyguardPrecommitActive) {
                    onKeyguardPrelayoutReady(
                        result = result,
                        source = source,
                    )
                } else {
                    completeKeyguardCutover(
                        result = result,
                        source = source,
                    )
                }
            }

            is HomePresentation.StateResult.Prepared -> {
                if (visualOnlyBoundary && keyguardPrecommitActive) {
                    keyguardRuntimeReady = false
                    KeyguardRenderSession.setNativeHandoffActive(true)
                    precommitKeyguardLayout(source)
                    return
                }
                keyguardRuntimeReady = false
                KeyguardRenderSession.setNativeHandoffActive(
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
                    refreshCcSource(
                        "keyguard-compact-layout-pending",
                    )
                }
            }

            is HomePresentation.StateResult.Failure -> {
                keyguardRuntimeReady = false
                KeyguardRenderSession.setNativeHandoffActive(true)
                HomePresentation.deactivateKeyguard("activation-failed")
                logDiagnostic(
                    level = Log.WARN,
                    event = "presentation.cutover",
                    component = "keyguardPresentation",
                    state = "native",
                    "source" to source,
                    "reason" to result.reason,
                    "fallback" to "native-keyguard",
                )
                refreshCcSource("keyguard-activation-failed")
            }

            is HomePresentation.StateResult.Inactive -> Unit
        }
    }

    private fun applyKeyguardNotReady(source: String) {
        resetKeyguardHandoff()
        keyguardRuntimeReady = false
        KeyguardRenderSession.setNativeHandoffActive(true)
        HomePresentation.deactivateKeyguard("readiness-lost:" + source)
        reconcileKeyguardCc("keyguard-readiness-lost")
    }

    private fun completeKeyguardCutover(
        result: HomePresentation.StateResult.Active,
        source: String,
    ) {
        val settings = FeaturePrefsOwner.currentSettings()
        val resolved = KeyguardHostResolver.current()
        if (
            !settings.enabled ||
            !settings.keyguardEnabled ||
            resolved !is KeyguardHostResolver.ResolveResult.Ready ||
            resolveKeyguardAodProjection(resolved.host) !=
                ScenePolicy.KeyguardAodProjection.KEYGUARD
        ) {
            deactivateKeyguardRuntime("cutover-projection-ineligible")
            return
        }
        keyguardRuntimeReady = true
        keyguardPresentationReadyObserved = true
        KeyguardRenderSession.setNativeHandoffActive(false)
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
        refreshCcSource("keyguard-ready")
    }

    private fun onKeyguardRuntimeFailure(reason: String) {
        resetKeyguardHandoff()
        keyguardControlCenterLeaseActive = false
        keyguardPresentationReadyObserved = false
        keyguardRuntimeReady = false
        KeyguardRenderSession.setNativeHandoffActive(true)
        logDiagnostic(
            level = Log.WARN,
            event = "presentation.failNative",
            component = "keyguardPresentation",
            state = "native",
            "reason" to reason,
            "fallback" to "native-keyguard",
        )
        reconcileKeyguardCc("keyguard-fail-native")
    }

    private fun deactivateKeyguardRuntime(source: String) {
        val wasReady = keyguardRuntimeReady
        resetKeyguardHandoff()
        keyguardControlCenterLeaseActive = false
        keyguardPresentationReadyObserved = false
        keyguardRuntimeReady = false
        KeyguardRenderSession.setNativeHandoffActive(true)
        HomePresentation.deactivateKeyguard(source)
        KeyguardRenderSession.detach()
        if (wasReady) {
            reconcileKeyguardCc("keyguard-deactivate:" + source)
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

        val settings = FeaturePrefsOwner.currentSettings()
        if (!settings.enabled || !settings.aodEnabled) {
            deactivateAodRuntime("feature-ineligible")
            return
        }

        val resolved = KeyguardHostResolver.current()
        if (resolved !is KeyguardHostResolver.ResolveResult.Ready) {
            deactivateAodRuntime("resolver-not-ready")
            return
        }
        val aodState =
            KeyguardAodSource.currentState(resolved.host.battery)
                ?: run {
                    deactivateAodRuntime("aod-state-unavailable")
                    return
                }
        if (
            resolveKeyguardAodProjection(resolved.host) !=
            ScenePolicy.KeyguardAodProjection.AOD
        ) {
            deactivateAodRuntime("projection-ineligible")
            return
        }
        val homeTransitionPrearm =
            homeAodTargetPrearmPending ||
                (
                    aodState.isAodAnimate &&
                        steadyStatusSourceScene == SourceScene.HOME &&
                        HomePresentation
                            .homeOwnedSlots()
                            .isNotEmpty()
                )

        when (
            val result =
                HomePresentation.activateAod(
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
            is HomePresentation.StateResult.Active -> {
                completeAodPresentationCutover(
                    result = result,
                    source = source,
                )
            }

            is HomePresentation.StateResult.Prepared -> {
                KeyguardRenderSession.setAodNativeHandoffActive(true)
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

            is HomePresentation.StateResult.Failure -> {
                KeyguardRenderSession.setAodNativeHandoffActive(true)
                HomePresentation.deactivateAod("activation-failed")
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

            is HomePresentation.StateResult.Inactive -> Unit
        }
    }

    private fun applyAodPresentationReadinessLost(source: String) {
        KeyguardRenderSession.setAodNativeHandoffActive(true)
        HomePresentation.deactivateAod("readiness-lost:" + source)
    }

    private fun completeAodPresentationCutover(
        result: HomePresentation.StateResult.Active,
        source: String,
    ) {
        val settings = FeaturePrefsOwner.currentSettings()
        val resolved = KeyguardHostResolver.current()
        if (
            !settings.enabled ||
            !settings.aodEnabled ||
            resolved !is KeyguardHostResolver.ResolveResult.Ready ||
            resolveKeyguardAodProjection(resolved.host) !=
                ScenePolicy.KeyguardAodProjection.AOD
        ) {
            deactivateAodRuntime("cutover-projection-ineligible")
            return
        }

        KeyguardRenderSession.setAodNativeHandoffActive(false)
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
        KeyguardRenderSession.setAodNativeHandoffActive(true)
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
        KeyguardRenderSession.setAodNativeHandoffActive(true)
        HomePresentation.deactivateAod(source)
        KeyguardRenderSession.detachAod()
    }

    private fun onKeyguardHostProbe(snapshot: KeyguardHostProbe.Snapshot) {
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
                if (FeaturePrefsOwner.currentSettings().keyguardEnabled) {
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

    // Hot Reload transfers only verified state. Anything tied to the old
    // class loader is released before the new generation can take ownership.
    private fun teardownOldGenerationForHotReload(
        continuousHandoff: Boolean = false,
    ) {
        ccVisible = false
        ccEligible = false
        ccSourceScene = SourceScene.UNKNOWN
        steadyStatusSourceScene = SourceScene.UNKNOWN
        lastStableKeyguardAodScene =
            ScenePolicy.StableKeyguardAodScene.UNKNOWN
        keyguardAodFullTargetPending = false
        keyguardAodFullTransitionActive = false
        homeAodTargetPrearmPending = false
        ccFraction = 0f
        keyguardRuntimeReady = false
        aodRendererAttached = false
        keyguardPresentationReadyObserved = false
        keyguardControlCenterLeaseActive = false
        CcTransition.detach("hotReload-oldGeneration")
        CcSession.detach(
            source = "hotReload-oldGeneration",
            releaseNativePresentation = !continuousHandoff,
        )
        HomeRenderSession.detach()
        KeyguardRenderSession.detach()
        KeyguardRenderSession.detachAod()
        val restoredPresentationViews =
            HomePresentation.releaseGenerationForHotReload(
                requestLayout =
                    HomePresentation.HotReloadHandoffPolicy
                        .shouldRequestLayoutOnRelease(continuousHandoff),
            )
        NativeNetworkSuppression.deactivate("hotReload-oldGeneration")
        StatusBarSession.detach()
        CoreRuntime.detach()
        PresentationRuntime.resetRuntimeState()
        KeyguardHostResolver.resetRuntimeState()
        PresentationStore.reset()
        IslandMotionSource.resetRuntimeState()
        PanelTransitionSource.resetRuntimeState()

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
                HomePresentation.HotReloadHandoffPolicy
                    .shouldRequestLayoutOnRelease(continuousHandoff),
            "stableStatusDetached" to true,
            "airplaneObserverDetached" to true,
            "defaultDataSubscriptionObserverDetached" to true,
            "mainThread" to true,
        )
        unbindRuntimeDiagnostics()
        FeaturePrefsOwner.unbind()
        VisualPrefsOwner.unbind()
    }

    private fun attachHostRuntime(
        host: Any,
        source: String,
        initialNativeHandoffActive: Boolean = false,
        initialTintState: TintState? = null,
        allowLiveTintSeed: Boolean = true,
    ) {
        val hostContext = (host as? android.view.View)?.context
        val coreRuntime =
            hostContext?.let { context ->
                CoreRuntime.attach(
                    context = context,
                    onAirplaneMode = { enabled ->
                        val trace = beginRenderTrace("airplaneObserver")
                        StatusStateStore.updateAirplaneMode(enabled)?.let { snapshot ->
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
                            PresentationStore.updateConnectivity(state)
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
            "subscriptionId" to DataSubSource.currentSubscriptionId(),
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
            val stableSession = StatusBarSession.attach(
                host = host,
                onEvent = { event ->
                    if (detailedDiagnosticsEnabled) {
                        log(Log.INFO, TAG, event)
                    }
                },
            )
        ) {
            StatusBarSession.AttachResult.Ready -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "session.attach",
                    component = "stableStatus",
                    state = "ready",
                    "source" to source,
                )
            }

            is StatusBarSession.AttachResult.Failure -> {
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
                NativeNetworkSuppression.attachObserver(
                    host = host,
                    source = source,
                )
        ) {
            is NativeNetworkSuppression.StateResult.Active -> {
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
            is NativeNetworkSuppression.StateResult.Pending -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "source.attach",
                    component = "statusIconObservation",
                    state = "pending",
                    "source" to source,
                    "reason" to observation.reason,
                    "trigger" to "home-dark-icon-manager-registration",
                    "suppressionWriters" to 0,
                )
            }
            is NativeNetworkSuppression.StateResult.Failure -> {
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
            is NativeNetworkSuppression.StateResult.Inactive -> Unit
        }

        val rendererInitialTintState =
            initialTintState?.let { transferred ->
                TintAuthority.rebaseTransferred(
                    transferred = transferred,
                    liveStatusIconTint =
                        NativeNetworkSuppression
                            .currentAppliedStatusIconTint(),
                )
            }

        when (
            val renderSession = HomeRenderSession.attach(
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
                    onHomeReadyChanged(host, ready, source)
                },
            )
        ) {
            HomeRenderSession.AttachResult.Ready -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "renderer.attach",
                    component = "renderer",
                    state = "ready",
                    "source" to source,
                )
            }

            is HomeRenderSession.AttachResult.Failure -> {
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

    private fun onHomeReadyChanged(
        host: Any,
        ready: Boolean,
        source: String,
    ) {
        if (!FeaturePrefsOwner.currentSettings().enabled) {
            HomeRenderSession.setNativeHandoffActive(true)
            HomePresentation.deactivate("feature-disabled:" + source)
            return
        }
        if (!ready) {
            HomeRenderSession.setNativeHandoffActive(true)
            HomePresentation.deactivate("readiness-lost:" + source)
            return
        }

        when (val result = HomePresentation.activate(host)) {
            is HomePresentation.StateResult.Active -> {
                HomeRenderSession.setNativeHandoffActive(false)
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
            is HomePresentation.StateResult.Prepared -> {
                HomeRenderSession.setNativeHandoffActive(true)
                HomePresentation.deactivate("unexpected-prepared")
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
            is HomePresentation.StateResult.Failure -> {
                HomeRenderSession.setNativeHandoffActive(true)
                HomePresentation.deactivate("activation-failed")
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
            is HomePresentation.StateResult.Inactive -> Unit
        }
    }

    private fun onHomePresentationRuntimeFailure(reason: String) {
        HomeRenderSession.setNativeHandoffActive(true)
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
                NativeParticipantRuntime.schedule(
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
            NativeParticipantRuntime.ScheduleResult.Scheduled -> {
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

            is NativeParticipantRuntime.ScheduleResult.Failure -> {
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
        val nativeParticipant = ParticipantContractProbe.inspect(host)
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
                BindableContractProbe.inspect(host)
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
                BindableGeometryProbe.inspect(host)
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
                NativeParticipantUi.attachHidden(
                    host = host,
                    onHandoffStateChanged = { active ->
                        val presentation =
                            PresentationStore.snapshot()
                        val state = StatusStateStore.snapshot()
                        val wifi = state.wifi

                        if (
                            active &&
                            !FeaturePrefsOwner.currentSettings().enabled
                        ) {
                            val batterySuppression =
                                NativeBatterySuppression.deactivate(
                                    "feature-disabled-native-handoff",
                                )
                            val networkSuppression =
                                NativeNetworkSuppression.deactivate(
                                    "feature-disabled-native-handoff",
                                )
                            HomeRenderSession.setNativeHandoffActive(false)
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
                                NativeBatterySuppression.activate(
                                    host = host,
                                    source = "native-handoff:" + source,
                                )
                            if (
                                batterySuppression is
                                    NativeBatterySuppression.StateResult.Failure
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
                                    NativeNetworkSuppression.activate(
                                        host = host,
                                        suppressWifi =
                                            NetworkRuntime.wifiReady &&
                                                ConnectivityPolicy
                                                    .wifiReplacementReady(
                                                        wifi = wifi,
                                                        connectivity = presentation.connectivity,
                                                    ),
                                        suppressMobile =
                                            NetworkSuppressionPolicy.suppressMobile(
                                                airplaneMode = state.airplaneMode,
                                                presentation = presentation.mobilePresentation,
                                                wasSuppressed = false,
                                            ),
                                    )
                                if (
                                    networkSuppression is
                                        NativeNetworkSuppression.StateResult.Failure
                                ) {
                                    val batteryRollback =
                                        NativeBatterySuppression.deactivate(
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
                                                    NativeBatterySuppression.StateResult.Inactive &&
                                                batteryRollback.changed
                                            ) {
                                                1
                                            } else {
                                                0
                                            },
                                    )
                                    false
                                } else {
                                    HomeRenderSession.setNativeHandoffActive(true)
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
                                                    NativeBatterySuppression.StateResult.Active &&
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
                                NativeBatterySuppression.deactivate(
                                    "native-handoff-fallback",
                                )
                            if (
                                batterySuppression is
                                    NativeBatterySuppression.StateResult.Failure
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
                                    NativeNetworkSuppression.deactivate(
                                        "native-handoff-fallback",
                                    )
                                HomeRenderSession.setNativeHandoffActive(false)
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
                                                NativeBatterySuppression.StateResult.Inactive &&
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
            is NativeParticipantUi.AttachResult.Ready -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "participant.attach",
                    component = "nativeCombinedParticipant",
                    state = "ready",
                    "source" to source,
                    "slot" to NativeParticipantUi.SLOT,
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

            is NativeParticipantUi.AttachResult.Failure -> {
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

        NetworkStateSource.bindingTopologyLines().forEach { line ->
            log(Log.INFO, TAG, line)
        }



        NativeStatusInventory.schedule(host) { snapshot ->
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

    private fun onStatusHostCaptured(capture: HostRegistry.Capture) {
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

    // Preferences are observed, never polled. Diagnostic switches may change
    // observation detail but must not change functional ownership or hooks.
    private fun bindRuntimeDiagnostics() {
        if (!BuildConfig.RUNTIME_DIAGNOSTICS) {
            DiagPrefsOwner.unbind()
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
            DiagPrefsOwner.bind(
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
            DiagPrefsOwner.unbind()
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
            FeaturePrefsOwner.bind(
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
            FeaturePrefsOwner.unbind()
            onRuntimeFeatureSettingsChanged(
                FeaturePrefsOwner.currentSettings(),
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
        settings: FeatureSettings,
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
            val hostView = HostRegistry.currentStatusHost() as? android.view.View
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

        NativeParticipantUi.onFeatureSettingsChanged(settings)
        if (
            !settings.enabled ||
            !settings.keyguardEnabled ||
            settings.aodEnabled
        ) {
            homeNativeAodFallbackCandidate = false
            homeNativeAodFallbackActive = false
        }
        HomeRenderSession.onFeatureSettingsChanged(settings)
        KeyguardRenderSession.onFeatureSettingsChanged(settings)
        CcSession.onFeatureSettingsChanged(settings)

        if (!settings.enabled) {
            releaseFeatureOwnership("feature-disabled")
            deactivateAodRuntime("feature-disabled")
            deactivateKeyguardRuntime("feature-disabled")
        } else {
            if (!settings.keyguardEnabled) {
                deactivateKeyguardRuntime("keyguard-feature-disabled")
            }
            if (!settings.aodEnabled) {
                deactivateAodRuntime("aod-feature-disabled")
            }
            KeyguardHostResolver.current()?.let { resolution ->
                onKeyguardHostResolution(
                    resolution = resolution,
                    source = "feature-settings",
                )
            }
        }
        refreshCcSource("feature-settings")

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

    private fun releaseFeatureOwnership(source: String) {
        keyguardAodFullTargetPending = false
        aodTargetToLockscreen = null
        keyguardAodFullTransitionActive = false
        resetKeyguardHandoff()
        homeOwnedAtAodStart = false
        homeNativeAodFallbackCandidate = false
        homeNativeAodFallbackActive = false
        homeAodTransitionOriginPending = false
        homeAodTargetPrearmPending = false
        ccEligible = false
        keyguardControlCenterLeaseActive = false
        CcSession.setSceneEligible(false)
        CcTransition.setSceneEligible(false)
        HomePresentation.deactivateCc(source)
        HomePresentation.deactivateAod(source)
        HomePresentation.deactivateKeyguard(source)
        HomePresentation.deactivate(source)
        NativeBatterySuppression.deactivate(source)
        NativeNetworkSuppression.deactivate(source)
        HomeRenderSession.setNativeHandoffActive(true)
    }

    private fun bindRuntimeVisualSettings() {
        runCatching {
            VisualPrefsOwner.bind(
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
                "mobileFollowsBattery" to settings.mobileFollowsBattery,
                "networkFollowsBattery" to settings.centerFollowsBattery,
                "batteryNumber" to settings.topReadoutEnabled,
                "chargingIcon" to settings.chargingIconEnabled,
                "batteryNumberFollowsBattery" to settings.topTextFollowsBattery,
                "chargingIconFollowsBattery" to
                    settings.chargingIconFollowsBattery,
                "transport" to "remote-preferences",
            )
        }.onFailure { error ->
            VisualPrefsOwner.unbind()
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
        settings: com.chaners.guiyuan.settings.VisualSettings,
    ) {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            val dispatch =
                Runnable {
                    onRuntimeVisualSettingsChanged(settings)
                }
            val hostView = HostRegistry.currentStatusHost() as? android.view.View
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

        if (settings != VisualPrefsOwner.currentSettings()) {
            return
        }

        HomeRenderSession.onVisualSettingsChanged(settings)
        KeyguardRenderSession.onVisualSettingsChanged(settings)
        CcSession.onVisualSettingsChanged(settings)
        HomePresentation.onVisualSettingsChanged()
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
                "mobileFollowsBattery" to settings.mobileFollowsBattery,
                "networkFollowsBattery" to settings.centerFollowsBattery,
                "batteryNumber" to settings.topReadoutEnabled,
                "chargingIcon" to settings.chargingIconEnabled,
                "batteryNumberFollowsBattery" to settings.topTextFollowsBattery,
                "chargingIconFollowsBattery" to
                    settings.chargingIconFollowsBattery,
                "eventDriven" to true,
                "mainThread" to true,
            )
        }
    }

    private fun logCurrentDiagnosticsHealth() {
        val state =
            when {
                !BuildConfig.RUNTIME_DIAGNOSTICS -> "disabled"
                DiagPrefsOwner.isBound -> "ready"
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
        DiagPrefsOwner.unbind()
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
        lastBatteryProbeSummary = null
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

    private fun onRenderLatencySample(sample: RenderLatencySample) {
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
            SystemClock.elapsedRealtime().function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }(36)

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
                        put(key, value.function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }())
                    }
                }
                put("sessionId", runtimeSessionId)
                put("uptimeMs", SystemClock.elapsedRealtime().function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }())
                put("sequence", diagnosticSequence.incrementAndGet().function function function function function function function toString() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }() { [native code] }())
            }
        log(
            level,
            TAG,
            DiagProtocol.format(
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
