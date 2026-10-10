package com.chaners.guiyuan.xposed

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.RUNTIME_REMOTE_PREFS_NAME
import com.chaners.guiyuan.system.DiagProtocol
import com.chaners.guiyuan.xposed.prefs.DiagPrefsOwner
import com.chaners.guiyuan.xposed.prefs.FeaturePrefsOwner
import com.chaners.guiyuan.xposed.prefs.VisualPrefsOwner
import com.chaners.guiyuan.xposed.network.ConnectivityPolicy
import com.chaners.guiyuan.xposed.network.NetworkSuppressionPolicy
import com.chaners.guiyuan.xposed.network.NativeNetworkSuppressor
import com.chaners.guiyuan.xposed.network.SysUiDefaultDataSubSource
import com.chaners.guiyuan.xposed.network.SysUiNetworkRuntime
import com.chaners.guiyuan.xposed.network.SysUiNetworkSource
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.util.concurrent.atomic.AtomicLong

class GyModule : XposedModule() {
    private data class BoundaryHandoff(
        val precommit: Boolean,
        var layoutReady: Boolean = false,
        var visualReady: Boolean = false,
    )

    private sealed interface AodWindow {
        val boundaryPending: Boolean

        data class Running(
            override val boundaryPending: Boolean,
            val homeOwnedAtStart: Boolean,
        ) : AodWindow

        data class Waiting(
            val toLockScreen: Boolean,
        ) : AodWindow {
            override val boundaryPending = true
        }
    }

    // Home native-AOD fallback is one phase; candidate and active never overlap.
    private enum class HomeAodFallback {
        NONE,
        CANDIDATE,
        ACTIVE,
    }

    private var islandSourceInstalled = false
    @Volatile
    private var ccSourceInstalled = false
    private var controlCenterSceneVisible = false
    private var controlCenterSceneEligible = false
    private var controlCenterSourceScene = SourceScene.UNKNOWN
    private var steadyStatusSourceScene = SourceScene.UNKNOWN
    private var stableFamilyScene =
        ScenePolicy.StableKeyguardAodScene.UNKNOWN
    // Non-null only while HyperOS is inside, or still finishing, a full-AOD handoff.
    private var aodWindow: AodWindow? = null
    // Non-null only while Keyguard is taking over the native AOD boundary.
    private var boundaryHandoff: BoundaryHandoff? = null
    private var homeAodFallback = HomeAodFallback.NONE
    // Origin evidence and target prearm have different lifetimes; neither is the fallback phase.
    private var homeAodOriginPending = false
    private var homeAodTargetPrearmPending = false
    private var ccExpansion = 0f
    // Guiyuan presentation readiness is separate from the latest native readiness callback.
    private var keyguardRuntimeReady = false
    private var aodRendererAttached = false
    private var keyguardReadyObserved = false
    // CC can keep this lease while a Keyguard readiness edge is being handed off.
    private var keyguardCcLeaseActive = false
    private var lastBatteryNumberProbeSummary: String? = null
    private var runtimeSessionId = newRuntimeSessionId()
    private val diagnosticSequence = AtomicLong(0L)
    private val renderTraceSequence = AtomicLong(0L)

    @Volatile
    private var detailedDiagnosticsEnabled = BuildConfig.DEVELOPMENT_PROBES

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        bindRuntimeDiagnostics()
        bindFeatureCfg()
        bindVisualCfg()
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

        val compatibility = SysUiCompatibilityProbe.inspect(param.classLoader)
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
        installNetworkSuppression(
            classLoader = param.classLoader,
            source = "coldStart",
        )

        runCatching {
            SysUiHostHook.install(
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

        if (SysUiHostHook.isReady) {
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
            installCcSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            installIslandSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
        }
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        val prepared =
            SysUiHotReload.prepare(
                param = param,
                generationHandoff =
                    Runnable {
                        teardownOldGeneration(
                            continuousHandoff = true,
                        )
                    },
            )
        if (prepared is SysUiHotReload.PrepareResult.Unavailable) {
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
            return false
        }

        prepared as SysUiHotReload.PrepareResult.Ready
        val hookCount =
            1 +
                SysUiBatteryRuntime.installedHookCount +
                SysUiNetworkRuntime.installedHookCount +
                SysUiPresentationRuntime.installedHookCount +
                SysUiPresentationOwner.installedHookCount +
                NativeNetworkSuppressor.installedHookCount +
                if (islandSourceInstalled) {
                    SysUiIslandSource.HOOK_COUNT
                } else {
                    0
                } +
                if (ccSourceInstalled) {
                    SysUiCcSource.HOOK_COUNT
                } else {
                    SysUiCcSource.failedInstallHookCount
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
        val takeoverResult =
            runCatching {
                SysUiHotReload.takeOverHooks(
                    param = param,
                    onCaptured = ::onStatusHostCaptured,
                )
            }
        val takeover = takeoverResult.getOrNull()

        if (takeover == null) {
            bindRuntimeDiagnostics()
            bindFeatureCfg()
            bindVisualCfg()
            logDiagnostic(
                level = Log.ERROR,
                event = "hotReload.complete",
                component = "hotReload",
                state = "error",
                "reason" to (takeoverResult.exceptionOrNull()?.message ?: "status-host-hook-missing"),
                "restartScope" to true,
            )
            return
        }

        runCatching {
            val removed = takeover.removedHooks

            SysUiBatteryRuntime.resetRuntimeState()
            SysUiNetworkRuntime.resetRuntimeState()
            islandSourceInstalled = false
            ccSourceInstalled = false
            controlCenterSceneVisible = false
            controlCenterSceneEligible = false
            controlCenterSourceScene = SourceScene.UNKNOWN
            steadyStatusSourceScene = SourceScene.UNKNOWN
            stableFamilyScene =
                ScenePolicy.StableKeyguardAodScene.UNKNOWN
            aodWindow = null
            clearBoundaryHandoff()
            homeAodFallback = HomeAodFallback.NONE
            homeAodOriginPending = false
            homeAodTargetPrearmPending = false
            ccExpansion = 0f
            keyguardRuntimeReady = false
            aodRendererAttached = false
            keyguardReadyObserved = false
            keyguardCcLeaseActive = false
            SysUiPresentationRuntime.resetRuntimeState()
            SysUiKeyguardHostResolver.resetRuntimeState()
            SysUiPresentationOwner.resetRuntimeState("hotReload")
            NativeNetworkSuppressor.resetRuntimeState("hotReload")
            bindRuntimeDiagnostics()
            bindFeatureCfg()
            bindVisualCfg()
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
            installHomePresentationOwner(
                classLoader = classLoader,
                source = "hotReload",
            )
            installNetworkSuppression(
                classLoader = classLoader,
                source = "hotReload",
            )
            installCcSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            installIslandSource(
                classLoader = classLoader,
                source = "hotReload",
            )

            val restored = SysUiHotReload.restoreTransfer(param)
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

            val capture = SysUiHostRegistry.restore(restored.host)
            if (capture == null) {
                logDiagnostic(
                    level = Log.WARN,
                    event = "hotReload.complete",
                    component = "hotReload",
                    state = "unavailable",
                    "reason" to "status-host-replaced-before-restore",
                    "restartScope" to true,
                )
                return@runCatching
            }
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
                Handler(Looper.getMainLooper()).post {
                    val reason =
                        when {
                            !hostView.isAttachedToWindow -> "status-host-detached"
                            SysUiHostRegistry.current() !== hostView -> "status-host-replaced"
                            else -> null
                        }
                    if (reason == null) {
                        restoreHotReloadRuntimeOnMain(
                            capture = capture,
                            restored = restored,
                            removedHooks = removed,
                        )
                    } else {
                        logDiagnostic(
                            level = Log.WARN,
                            event = "hotReload.complete",
                            component = "hotReload",
                            state = "unavailable",
                            "reason" to reason,
                            "restartScope" to true,
                        )
                    }
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
        capture: SysUiHostRegistry.Capture,
        restored: HotReloadTransfer.Restored,
        removedHooks: Int,
    ) {
        runCatching {
            var restoredSnapshot =
                StatusStateStore.restoreHotReloadState(restored.state)
            val bindings =
                SysUiNetworkSource.restoreHotReloadBindings(restored.bindings)
            SysUiNetworkSource.seedRestoredWifiState(
                onEvent =
                    if (BuildConfig.RUNTIME_DIAGNOSTICS && detailedDiagnosticsEnabled) {
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
                    SysUiPresentationOwner.cleanupLegacyParticipant(capture.host)
            ) {
                SysUiPresentationOwner.LegacyCleanupResult.NotPresent -> Unit
                SysUiPresentationOwner.LegacyCleanupResult.Removed -> {
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
                is SysUiPresentationOwner.LegacyCleanupResult.Failure -> {
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
            )

            SysUiCcSource.restoreHomeEligibility(
                restored.controlCenterHomeEligible,
            )
            val controlCenterFakeRestore =
                restored.controlCenterFakeHost?.let { fakeHost ->
                    restoreCcFakeAfterReload(
                        host = fakeHost,
                        transferredCompactReady = restored.controlCenterCompactReady,
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
                "batteryPercent" to restoredSnapshot.battery?.percent,
                "batteryCharging" to restoredSnapshot.battery?.charging,
                "wifiState" to
                    when (restoredSnapshot.wifi) {
                        StatusStateStore.WifiState.Unknown -> "unknown"
                        StatusStateStore.WifiState.Hidden -> "hidden"
                        is StatusStateStore.WifiState.Visible -> "visible"
                    },
                "wifiSignal" to
                    (restoredSnapshot.wifi as? StatusStateStore.WifiState.Visible)
                        ?.signal
                        ?.logToken,
                "mobileSubs" to restoredSnapshot.mobile.keys.joinToString(","),
                "airplane" to restoredSnapshot.airplaneMode,
                "mobileRecoveryPending" to restoredSnapshot.mobileRecoveryPending,
                "homePresentation" to "native-carrier-lifecycle",
                "controlCenterHomeEligible" to
                    (restored.controlCenterHomeEligible ?: "unknown"),
                "controlCenterFakePrearm" to controlCenterFakeRestore,
                "tintTransfer" to if (transferredTint != null) "restored" else "native-fallback",
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
        val failure =
            SysUiPresentationOwner.install(
                module = this,
                classLoader = classLoader,
                onEvent = { event ->
                    if (detailedDiagnosticsEnabled) {
                        log(Log.INFO, TAG, event)
                    }
                },
                onFailNative = ::onHomeRuntimeFailure,
            )
        if (failure == null) {
            logDiagnostic(
                level = Log.INFO,
                event = "hook.install",
                component = "homePresentation",
                state = "ready",
                "source" to source,
                "hooks" to SysUiPresentationOwner.installedHookCount,
                "carrier" to "MiuiNotificationStatusContainer.overlay",
            )
        } else {
            logDiagnostic(
                level = Log.WARN,
                event = "hook.install",
                component = "homePresentation",
                state = "unavailable",
                "source" to source,
                "reason" to failure,
                "fallback" to "native-systemui",
            )
        }
    }

    private fun installNetworkSuppression(
        classLoader: ClassLoader,
        source: String,
    ) {
        val failure =
            NativeNetworkSuppressor.install(
                module = this,
                classLoader = classLoader,
                onEvent = { event ->
                    if (detailedDiagnosticsEnabled) {
                        log(Log.INFO, TAG, event)
                    }
                },
                isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                onObservationAttached = { observationSource ->
                    logDiagnostic(
                        level = Log.INFO,
                        event = "source.attach",
                        component = "statusIconObservation",
                        state = "ready",
                        "source" to observationSource,
                        "trigger" to "home-dark-icon-manager-registration",
                        "mode" to "observation-only",
                    )
                },
                onStatusPresentationChanged = ::onStatusPresentationChanged,
            )
        if (failure == null) {
            logDiagnostic(
                level = Log.INFO,
                event = "hook.install",
                component = "nativeNetworkSuppression",
                state = "ready",
                "source" to source,
                "hooks" to NativeNetworkSuppressor.installedHookCount,
            )
        } else {
            logDiagnostic(
                level = Log.WARN,
                event = "hook.install",
                component = "nativeNetworkSuppression",
                state = "unavailable",
                "source" to source,
                "reason" to failure,
            )
        }
    }

    private fun installNetworkStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SysUiNetworkRuntime.attach(
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
                onMobileSignal = { subscriptionId, signal ->
                    val trace = beginRenderTrace("mobile")
                    val changed =
                        StatusStateStore.updateMobileSignal(
                            subscriptionId = subscriptionId,
                            signal = signal,
                        )
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
                    NativeNetworkSuppressor.preMaskMobileSignal(image)
                },
                onPresentationChanged = {
                    refreshMobilePresentation(beginRenderTrace("networkPresentation"))
                },
                onEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onNetworkPipelineEvent else null,
                isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
            )
        }.onSuccess { result ->
            refreshStatusIconObservation("network-source:" + source)
            val hookCount = SysUiNetworkRuntime.installedHookCount
            val fullyReady =
                result.wifiReady &&
                    result.mobileReady &&
                    hookCount == SysUiNetworkSource.HOOK_COUNT
            val (level, state) =
                when {
                    fullyReady -> Log.INFO to "ready"
                    hookCount > 0 -> Log.WARN to "partial"
                    else -> Log.ERROR to "error"
                }
            logDiagnostic(
                level = level,
                event = "source.install",
                component = "network",
                state = state,
                "hooks" to hookCount,
                "expectedHooks" to SysUiNetworkSource.HOOK_COUNT,
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
            }
        }.onFailure { error ->
            SysUiNetworkRuntime.resetRuntimeState()
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

    private fun installIslandSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SysUiIslandSource.install(
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
            islandSourceInstalled =
                handles.size == SysUiIslandSource.HOOK_COUNT
            logDiagnostic(
                level = if (islandSourceInstalled) Log.INFO else Log.WARN,
                event = "source.install",
                component = "islandMotion",
                state = if (islandSourceInstalled) "ready" else "partial",
                "hooks" to handles.size,
                "expectedHooks" to SysUiIslandSource.HOOK_COUNT,
                "source" to source,
            )
        }.onFailure { error ->
            islandSourceInstalled = false
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


    private fun installCcSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SysUiCcSource.install(
                module = this,
                classLoader = classLoader,
                onUpdate = { update ->
                    if (ccSourceInstalled) onCcUpdate(update)
                },
                onFakePresentationAttached = { host ->
                    if (ccSourceInstalled) onCcFakeAttached(host)
                },
                onRuntimeFailure = ::onCcRuntimeFailure,
                onEvent = { event ->
                    if (ccSourceInstalled) onCcEvent(event)
                },
                isProbeEnabled = {
                    BuildConfig.DEVELOPMENT_PROBES || detailedDiagnosticsEnabled
                },
            )
        }.onSuccess { handles ->
            val expectedHooks =
                SysUiCcSource.HOOK_COUNT
            ccSourceInstalled = handles.size == expectedHooks
            // Home yields Control Center only after the projected native
            // carrier is structurally ready.
            HomeRenderSession.onControlCenterAuthorityChanged(true)
            logDiagnostic(
                level = if (ccSourceInstalled) Log.INFO else Log.WARN,
                event = "source.install",
                component = "panelTransition",
                state = if (ccSourceInstalled) "ready" else "partial",
                "hooks" to handles.size,
                "expectedHooks" to expectedHooks,
                "notificationHomeLifecycle" to "system-icons-carrier",
                "source" to source,
            )
        }.onFailure { error ->
            ccSourceInstalled = false
            HomeRenderSession.onControlCenterAuthorityChanged(true)
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "panelTransition",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "hooks" to SysUiCcSource.failedInstallHookCount,
                "source" to source,
            )
            log(Log.ERROR, TAG, "Panel transition source installation failed", error)
        }
    }

    private fun onCcUpdate(
        update: SysUiCcSource.Update,
    ) {
        val effectiveSourceScene = handleCcUpdate(update)
        val transitionUpdate =
            if (effectiveSourceScene != null) {
                update.copy(sourceScene = effectiveSourceScene)
            } else {
                update
            }
        CcTransitionOwner.onSourceUpdate(transitionUpdate)

        if (
            detailedDiagnosticsEnabled &&
            update.fraction != null &&
            lastBatteryNumberProbeSummary == null
        ) {
            val batteryNumberProbe =
                CcTransitionOwner.latestBatteryNumberProbeDiagnostic()
            if (batteryNumberProbe != null) {
                lastBatteryNumberProbeSummary = batteryNumberProbe
                logDiagnostic(
                    level = Log.INFO,
                    event = "target.probe",
                    component = "batteryNumberTarget",
                    state = "ready",
                    "summary" to batteryNumberProbe,
                )
            }
        }

    }

    private fun handleCcUpdate(
        update: SysUiCcSource.Update,
    ): SourceScene? {
        update.fraction?.let(::onCcExpansion)

        val visible = update.visible ?: return null
        if (!visible) {
            controlCenterSceneVisible = false
            // Restore Home first. QS_FAKE compact presentation remains prearmed
            // for the lifetime of the native fake root; only the Combined
            // overlay visibility changes with Control Center visibility.
            HomeRenderSession.onControlCenterAuthorityChanged(true)
            CcRenderSession.setRequestedVisible(false)
            return null
        }

        controlCenterSceneVisible = true
        if (!CcRenderSession.beginVisibleCycle()) {
            HomeRenderSession.onControlCenterAuthorityChanged(true)
            logDiagnostic(
                level = Log.WARN,
                event = "projection.visibleCycle",
                component = "controlCenterProjection",
                state = "native",
                "reason" to "visible-cycle-rearm-failed",
                "fallback" to "native-control-center-until-next-native-event",
            )
            return update.sourceScene
        }
        val reportedSourceScene =
            update.sourceScene
                ?: SourceScene.UNKNOWN
        val incomingBoundaryReady =
            incomingKeyguardReadyForCc()
        val effectiveSourceScene =
            ScenePolicy.resolveControlCenterSourceScene(
                reportedSourceScene = reportedSourceScene,
                steadySourceScene = steadyStatusSourceScene,
                lastStableFamilyScene = stableFamilyScene,
                incomingKeyguardPresentationReady = incomingBoundaryReady,
            )
        updateCcSourceEligibility(
            sourceScene = effectiveSourceScene,
            authority =
                when {
                    effectiveSourceScene == reportedSourceScene ->
                        "hyperos-realSystemIcons"
                    incomingBoundaryReady &&
                        effectiveSourceScene == SourceScene.KEYGUARD ->
                        "incoming-keyguard-presentation"
                    else -> "steady-source-view-override"
                },
        )
        val carrier = update.presentationHost
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

        val prepareFailure = prepareCcFake(carrier, "visible-fallback")
        if (prepareFailure == null) {
            val ready =
                CcRenderSession.setRequestedVisible(true)
            if (!ready) {
                HomeRenderSession.onControlCenterAuthorityChanged(true)
            }
        } else {
            HomeRenderSession.onControlCenterAuthorityChanged(true)
            logDiagnostic(
                level = Log.WARN,
                event = "projection.attach",
                component = "controlCenterProjection",
                state = "unavailable",
                "reason" to prepareFailure,
                "fallback" to "home-visible",
            )
        }
        return effectiveSourceScene
    }

    private fun updateCcSourceEligibility(
        sourceScene: SourceScene,
        authority: String,
    ) {
        if (
            keyguardCcLeaseActive &&
            sourceScene != SourceScene.KEYGUARD
        ) {
            releaseKeyguardCcLease(
                source = "source-scene:" + sourceScene.name + ":" + authority,
                reconcileReadiness = true,
            )
        }
        controlCenterSourceScene = sourceScene
        SysUiPresentationOwner.updateCcSourceScene(sourceScene)
        acquireKeyguardCcLease(
            source = "source-scene:" + authority,
        )
        val settings = FeaturePrefsOwner.current()
        val incomingBoundaryReady =
            incomingKeyguardReadyForCc()
        val keyguardPresentationReady =
            keyguardRuntimeReady || incomingBoundaryReady
        val keyguardEligible =
            settings.enabled &&
                settings.keyguard &&
                keyguardPresentationReady
        val nextEligible =
            ScenePolicy.controlCenterProjectionEligible(
                featureEnabled = settings.enabled,
                sourceScene = sourceScene,
                keyguardEnabled = keyguardEligible,
            )
        if (nextEligible == controlCenterSceneEligible) {
            return
        }

        controlCenterSceneEligible = nextEligible
        CcRenderSession.setSceneEligible(nextEligible)
        CcTransitionOwner.setSceneEligible(nextEligible)
        logDiagnostic(
            level = Log.INFO,
            event = "scene.eligibility",
            component = "controlCenterProjection",
            state = if (nextEligible) "eligible" else "native",
            "sourceScene" to sourceScene.name,
            "authority" to authority,
            "keyguardEnabled" to settings.keyguard,
            "keyguardRuntimeReady" to keyguardRuntimeReady,
            "incomingBoundaryReady" to incomingBoundaryReady,
            "keyguardPresentationReady" to keyguardPresentationReady,
            "controlCenterVisible" to controlCenterSceneVisible,
            "fallback" to if (nextEligible) "combined-qs-fake" else "native-qs-fake",
        )
    }

    private fun onCcExpansion(rawFraction: Float) {
        val fraction = rawFraction.coerceIn(0f, 1f)
        val previous = ccExpansion
        ccExpansion = fraction

        if (fraction > 0f) {
            if (
                controlCenterSourceScene != SourceScene.KEYGUARD &&
                incomingKeyguardReadyForCc()
            ) {
                updateCcSourceEligibility(
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
            keyguardCcLeaseActive &&
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
                incomingKeyguardReadyForCc()
        if (
            keyguardCcLeaseActive ||
            !ScenePolicy.shouldAcquireKeyguardCcLease(
                sourceScene = controlCenterSourceScene,
                keyguardPresentationReady = keyguardPresentationReady,
                nativeFraction = ccExpansion,
            )
        ) {
            return
        }

        keyguardCcLeaseActive = true
        logDiagnostic(
            level = Log.INFO,
            event = "presentation.lease",
            component = "keyguardControlCenter",
            state = "acquired",
            "source" to source,
            "sourceScene" to controlCenterSourceScene.name,
            "nativeFraction" to ccExpansion,
            "cleanupBoundary" to "native-fraction-zero-or-authoritative-source-change",
        )
    }

    private fun shouldRetainKeyguardCcLease(): Boolean {
        val resolved =
            SysUiKeyguardHostResolver.current()
                as? SysUiKeyguardHostResolver.ResolveResult.Ready
                ?: return false
        val settings = FeaturePrefsOwner.current()
        val aodBlocked =
            SysUiKeyguardAodSource
                .currentState(resolved.host.battery)
                ?.blocksProjection
                ?: true
        val incomingBoundaryReady =
            incomingKeyguardReadyForCc()
        return ScenePolicy.shouldRetainKeyguardCcLease(
            leaseActive = keyguardCcLeaseActive,
            sourceScene = controlCenterSourceScene,
            featureEnabled = settings.enabled,
            keyguardEnabled = settings.keyguard,
            hostAttached = resolved.host.systemIcons.isAttachedToWindow,
            aodBlocked = aodBlocked,
            incomingBoundaryPresentationReady = incomingBoundaryReady,
            nativeFraction = ccExpansion,
        )
    }

    private fun releaseKeyguardCcLease(
        source: String,
        reconcileReadiness: Boolean,
    ) {
        if (!keyguardCcLeaseActive) return
        keyguardCcLeaseActive = false
        logDiagnostic(
            level = Log.INFO,
            event = "presentation.lease",
            component = "keyguardControlCenter",
            state = "released",
            "source" to source,
            "sourceScene" to controlCenterSourceScene.name,
            "nativeFraction" to ccExpansion,
            "observedReady" to keyguardReadyObserved,
            "reconcileReadiness" to reconcileReadiness,
        )
        if (
            reconcileReadiness &&
            !keyguardReadyObserved &&
            !incomingKeyguardReadyForCc()
        ) {
            onKeyguardReadinessLost(
                source = "lease-release:" + source,
            )
        }
    }

    private fun incomingKeyguardReadyForCc(): Boolean {
        val settings = FeaturePrefsOwner.current()
        if (
            !settings.enabled ||
            !settings.keyguard ||
            settings.aod
        ) {
            return false
        }
        val resolved =
            SysUiKeyguardHostResolver.current()
                as? SysUiKeyguardHostResolver.ResolveResult.Ready
                ?: return false
        val handoff = boundaryHandoff
        return ScenePolicy.incomingKeyguardPresentationReady(
            visualHandoffActive = handoff != null,
            layoutPrecommitActive = handoff?.precommit == true,
            compactLayoutReady = handoff?.layoutReady == true,
            visualBoundaryReached = handoff?.visualReady == true,
            hostAttached = resolved.host.systemIcons.isAttachedToWindow,
        )
    }

    private fun refreshCcSourceEligibility(authority: String) {
        updateCcSourceEligibility(
            sourceScene = controlCenterSourceScene,
            authority = authority,
        )
    }

    private fun reconcileCcForKeyguard(authority: String) {
        if (
            !ScenePolicy.shouldReconcileCcForKeyguard(
                ccVisible = controlCenterSceneVisible,
                nativeFraction = ccExpansion,
                leaseActive = keyguardCcLeaseActive,
            )
        ) {
            return
        }
        refreshCcSourceEligibility(authority)
    }

    private fun restoreCcFakeAfterReload(
        host: ViewGroup,
        transferredCompactReady: Boolean,
    ): String {
        val failure =
            CcRenderSession.restoreAfterHotReload(
                host = host,
                onEvent = ::onCcEvent,
                isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                onProjectionReadinessChanged = ::onCcProjectionReadyChanged,
                transferredCompactReady = transferredCompactReady,
            )
        if (failure != null) {
            logDiagnostic(
                level = Log.WARN,
                event = "projection.restore",
                component = "controlCenterProjection",
                state = "fallback",
                "source" to "hot-reload-transfer",
                "reason" to failure.reason,
                "next" to "first-native-layout-prearm",
            )
            onCcFakeAttached(host)
            return "fallback-first-native-layout:" + failure.reason
        }

        val compactReady =
            CcRenderSession
                .nativeReadyForHotReload()
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
        )
        return if (compactReady) {
            "restored-laid-out-compact-ready"
        } else {
            "restored-laid-out-native-layout-pending"
        }
    }

    private fun onCcFakeAttached(host: ViewGroup) {
        when (
            val result =
                CcRenderSession.prearmAfterNextNativeLayout(
                    host = host,
                    onEvent = ::onCcEvent,
                    isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                    onProjectionReadinessChanged = ::onCcProjectionReadyChanged,
                )
        ) {
            is CcRenderSession.PrearmResult.Scheduled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "projection.prearm",
                    component = "controlCenterProjection",
                    state = "scheduled",
                    "source" to "fake-root-attached",
                    "boundary" to "first-native-layout",
                    "reused" to result.reused,
                    "requestedVisible" to controlCenterSceneVisible,
                )
            }

            is CcRenderSession.PrearmResult.Failure -> {
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

    private fun prepareCcFake(
        host: ViewGroup,
        source: String,
    ): String? {
        val failure =
            CcRenderSession.attach(
                host = host,
                onEvent = ::onCcEvent,
                isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                onProjectionReadinessChanged = ::onCcProjectionReadyChanged,
            )
        if (failure != null) {
            logDiagnostic(
                level = Log.WARN,
                event = "projection.prepare",
                component = "controlCenterProjection",
                state = "unavailable",
                "source" to source,
                "reason" to failure.reason,
                "fallback" to "native-qs-fake",
            )
        }
        return failure?.reason
    }

    private fun onCcProjectionReadyChanged(ready: Boolean) {
        CcTransitionOwner.onProjectionReadinessChanged(ready)
        if (!controlCenterSceneVisible) {
            return
        }
        // Projected owner is already visible when ready=true. On the reverse
        // edge Home is restored before the projected owner is removed.
        HomeRenderSession.onControlCenterAuthorityChanged(!ready)
    }

    private fun onCcEvent(event: String) {
        if (detailedDiagnosticsEnabled) {
            log(Log.INFO, TAG, event)
        }
    }

    private fun onCcRuntimeFailure(error: Throwable) {
        val cleanupFailures = mutableListOf<String>()
        fun safely(step: String, block: () -> Unit) {
            try {
                block()
            } catch (cleanupError: Throwable) {
                if (
                    cleanupError is VirtualMachineError ||
                    cleanupError is ThreadDeath
                ) {
                    throw cleanupError
                }
                cleanupFailures += step + ":" + cleanupError.javaClass.simpleName
            }
        }

        if (keyguardCcLeaseActive) {
            safely("lease") {
                releaseKeyguardCcLease(
                    source = "panel-runtime-failure",
                    reconcileReadiness = false,
                )
            }
        }
        controlCenterSceneEligible = false
        controlCenterSourceScene = SourceScene.UNKNOWN
        safely("source-scene") {
            SysUiPresentationOwner.updateCcSourceScene(SourceScene.UNKNOWN)
        }
        safely("transition-eligibility") {
            CcTransitionOwner.setSceneEligible(false)
        }
        safely("transition-detach") {
            CcTransitionOwner.detach("panel-runtime-failure")
        }
        safely("render-eligibility") {
            CcRenderSession.setSceneEligible(false)
        }
        safely("home-authority") {
            HomeRenderSession.onControlCenterAuthorityChanged(true)
        }
        safely("error-report") {
            logDiagnostic(
                level = Log.ERROR,
                event = "runtime.callback",
                component = "panelTransition",
                state =
                    if (cleanupFailures.isEmpty()) "fallback-requested" else "cleanup-incomplete",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "fallback" to
                    if (cleanupFailures.isEmpty()) "native-control-center" else "unconfirmed",
                "cleanupFailures" to
                    cleanupFailures.takeIf { it.isNotEmpty() }?.joinToString(","),
            )
            log(Log.ERROR, TAG, "Panel transition runtime callback failed", error)
        }
    }

    private fun installBatteryStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SysUiBatteryRuntime.attach(
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
        }.onSuccess { hooks ->
            val ready = hooks == SysUiBatterySource.HOOK_COUNT
            logDiagnostic(
                level = if (ready) Log.INFO else Log.WARN,
                event = "source.install",
                component = "batteryState",
                state = if (ready) "ready" else "partial",
                "hooks" to hooks,
                "expectedHooks" to SysUiBatterySource.HOOK_COUNT,
                "source" to source,
                "authority" to
                    "MiuiBatteryMeterIconView.getProgressStatus() via " +
                    "BatteryController callbacks",
            )
        }.onFailure { error ->
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "batteryState",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "hooks" to SysUiBatterySource.failedInstallHookCount,
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
            SysUiPresentationRuntime.attach(
                module = this,
                classLoader = classLoader,
                onTintState = ::onTintStateUpdate,
                onSceneState = ::onSceneStateUpdate,
                onKeyguardAodState = ::onKeyguardAodStateUpdate,
                onKeyguardFullAodTransitionStarted = ::onFullAodStarted,
                onKeyguardFullAodTransitionCommitted = ::onFullAodCommitted,
                onKeyguardStatusIconTransition = ::onKeyguardIconTransition,
                onMobileTypeChanged = { drawable ->
                    refreshMobilePresentation(
                        trace = beginRenderTrace("mobileType"),
                        pendingMobileTypeDrawable = drawable,
                    )
                },
                onTintEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onTintSourceEvent else null,
                isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
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
                "tintFailure" to result.tintFailure,
                "sceneHooks" to result.sceneHooks,
                "sceneFailure" to result.sceneFailure,
                "mobileTypeHooks" to result.mobileTypeHooks,
                "mobileTypeFailure" to result.mobileTypeFailure,
                "keyguardAodHooks" to result.keyguardAodHooks,
                "keyguardAodReady" to result.keyguardAodReady,
                "keyguardFullAodHooks" to result.keyguardFullAodHooks,
                "keyguardFullAodReady" to result.keyguardFullAodReady,
                "keyguardStatusIconHooks" to result.keyguardStatusIconHooks,
                "keyguardStatusIconReady" to result.keyguardStatusIconReady,
                "source" to source,
            )
            if (result.keyguardAodReady) {
                SysUiKeyguardHostResolver.current()?.let { resolution ->
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
                "tintCleanupRemainingHooks" to SysUiTintSource.failedInstallHookCount,
                "source" to source,
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
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.resolve",
                    component = "mobilePresentation",
                    state = "ready",
                    "mode" to presentation.mode.name,
                    "boundRoots" to presentation.boundRoots,
                    "activeBoundRoots" to presentation.activeBoundRoots,
                    "visibleRoots" to presentation.visibleRoots,
                    "activeSubAuthority" to presentation.activeSubscriptionAuthority,
                    "activeSubIds" to presentation.activeSubscriptionIds.joinToString(","),
                    "presentationRootSubId" to presentation.presentationRootSubscriptionId,
                    "effectiveDataSubId" to presentation.effectiveDataSubscriptionId,
                    "networkTypeSubId" to presentation.networkTypeSubscriptionId,
                    "networkType" to presentation.networkType?.label,
                    "enhanced" to presentation.networkType?.enhanced,
                    "networkTypeSource" to presentation.networkType?.source?.name,
                    "nativeMobileReplacementReady" to presentation.nativeMobileReplacementReady,
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
        CcRenderSession.onState(snapshot)
    }

    private fun onPresentationStateChanged(trace: RuntimeRenderTrace? = null) {
        HomeRenderSession.onPresentationStateChanged(trace)
        KeyguardRenderSession.onPresentationStateChanged()
        CcRenderSession.onPresentationStateChanged()
        refreshStatusIconObservation("presentation")
    }

    private fun refreshStatusIconObservation(source: String) {
        NativeNetworkSuppressor.refreshObservation(source)
    }

    private fun onStatusPresentationChanged(
        state: PresentationStore.StatusIconPresentation,
    ) {
        val trace = beginRenderTrace("statusIcons")
        val changed =
            PresentationStore.updateStatusIcons(state)

        HomeRenderSession.onStatusIconTintUpdate(
            state.appliedTint,
        )
        KeyguardRenderSession.onPresentationStateChanged()
        CcRenderSession.onPresentationStateChanged()

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
                                ?.toString(16)
                                ?.padStart(8, '0')
                                ?: "none"
                        ),
                    "noSimVisible" to state.noSimVisible,
                    "noSimPackage" to state.noSimIcon?.packageName,
                    "noSimResId" to state.noSimIcon?.resourceId,
                )
            }
        }
    }

    private fun onTintStateUpdate(update: SysUiTintSource.TintUpdate) {
        KeyguardRenderSession.onTintUpdate(update)
        val liveStatusIconTint =
            NativeNetworkSuppressor.currentAppliedStatusIconTint()
        val resolvedState =
            TintAuthority.resolveBatteryEvent(
                batteryState = update.state,
                liveStatusIconTint = liveStatusIconTint,
            )
        val resolvedUpdate = update.copy(state = resolvedState)
        HomeRenderSession.onTintUpdate(resolvedUpdate)
        CcRenderSession.onTintUpdate(resolvedUpdate)

    }

    private fun onFullAodStarted() {
        val settings = FeaturePrefsOwner.current()
        val homeOwnedAtStart =
            SysUiPresentationOwner
                .homeSlots()
                .isNotEmpty()
        val homeCarrierVisibleAtStart =
            SysUiPresentationOwner
                .homeCarrierVisible()

        if (
            ScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguard,
                aodEnabled = settings.aod,
                homePresentationOwned = homeOwnedAtStart,
                homeOriginConfirmed =
                    homeCarrierVisibleAtStart &&
                        steadyStatusSourceScene == SourceScene.HOME,
            )
        ) {
            homeAodFallback = HomeAodFallback.CANDIDATE
        }
        homeAodOriginPending =
            settings.enabled &&
                steadyStatusSourceScene == SourceScene.HOME &&
                stableFamilyScene ==
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN &&
                homeOwnedAtStart &&
                (settings.keyguard || settings.aod)
        val boundaryPending =
            SysUiPresentationRuntime.keyguardStatusIconReady &&
                stableFamilyScene !=
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN &&
                settings.keyguard != settings.aod
        aodWindow =
            AodWindow.Running(
                boundaryPending = boundaryPending,
                homeOwnedAtStart = homeOwnedAtStart,
            )

        logDiagnostic(
            level = Log.INFO,
            event = "aod.targetWindow",
            component = "keyguardAod",
            state = if (boundaryPending) "pending" else "observation-only",
            "source" to "animateFullAod:before",
            "visualBoundaryAuthority" to
                if (SysUiPresentationRuntime.keyguardStatusIconReady) {
                    "native-animateIconContainer"
                } else {
                    "status-icons-alpha-fallback"
                },
            "homeOriginLatched" to homeAodOriginPending,
            "homeNativeAodFallbackCandidate" to (homeAodFallback == HomeAodFallback.CANDIDATE),
            "homeNativeAodFallbackActive" to (homeAodFallback == HomeAodFallback.ACTIVE),
            "homePresentationOwnedAtStart" to homeOwnedAtStart,
            "homeCarrierVisibleAtStart" to homeCarrierVisibleAtStart,
        )
    }

    private fun onFullAodCommitted() {
        val running = aodWindow as? AodWindow.Running
        val boundaryPending = running?.boundaryPending == true
        val homeOwnedAtStart = running?.homeOwnedAtStart == true
        val settings = FeaturePrefsOwner.current()
        val resolution = SysUiKeyguardHostResolver.current()
        val target =
            (resolution as? SysUiKeyguardHostResolver.ResolveResult.Ready)
                ?.host
                ?.let { resolved ->
                    SysUiKeyguardHostResolver.nativeToLockScreenTarget(resolved)
                }

        aodWindow =
            if (boundaryPending && target != null) {
                AodWindow.Waiting(target)
            } else {
                null
            }
        if (target != false) {
            homeAodOriginPending = false
            homeAodTargetPrearmPending = false
            homeAodFallback = HomeAodFallback.NONE
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
            "visualBoundaryPending" to (aodWindow?.boundaryPending == true),
            "homeOriginLatched" to homeAodOriginPending,
            "homeNativeAodFallbackCandidate" to (homeAodFallback == HomeAodFallback.CANDIDATE),
            "homeNativeAodFallbackActive" to (homeAodFallback == HomeAodFallback.ACTIVE),
        )

        val releaseTransientHomeKeyguard =
            ScenePolicy.shouldReleaseTransientHomeKeyguardForDisabledAod(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguard,
                aodEnabled = settings.aod,
                homeNativeAodFallbackCandidate =
                    (homeAodFallback == HomeAodFallback.CANDIDATE),
                homePresentationOwnedAtFullAodStart = homeOwnedAtStart,
                nativeToLockScreenTarget = target,
            )
        if (releaseTransientHomeKeyguard) {
            homeAodFallback = HomeAodFallback.ACTIVE
            homeAodOriginPending = false
            homeAodTargetPrearmPending = false
            clearBoundaryHandoff()
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
            )
        }

        (resolution as? SysUiKeyguardHostResolver.ResolveResult.Ready)?.let { ready ->
            if (!releaseTransientHomeKeyguard) {
                armBoundaryHandoff(
                    resolution = ready,
                    nativeToLockScreenTarget = target,
                    source = "animateFullAod:after",
                    visualBoundaryReached = false,
                )
            }
            val prearmed =
                armHomeAodPrearm(
                    resolution = ready,
                    nativeToLockScreenTarget = target,
                    source = "animateFullAod:after",
                )
            if (
                homeAodOriginPending &&
                target == false &&
                !prearmed
            ) {
                onKeyguardHostResolution(
                    resolution = ready,
                    source = "home-aod-origin:animateFullAod:after",
                )
            }
        }
    }

    private fun onKeyguardIconTransition() {
        val resolution =
            SysUiKeyguardHostResolver.current()
                as? SysUiKeyguardHostResolver.ResolveResult.Ready
                ?: run {
                    // Missing host invalidates the start snapshot as well as the boundary.
                    aodWindow =
                        when (val state = aodWindow) {
                            is AodWindow.Running ->
                                state.copy(
                                    boundaryPending = false,
                                    homeOwnedAtStart = false,
                                )
                            is AodWindow.Waiting -> null
                            null -> null
                        }
                    homeAodFallback = HomeAodFallback.NONE
                    homeAodOriginPending = false
                    homeAodTargetPrearmPending = false
                    return
                }
        val aodState =
            SysUiKeyguardAodSource.currentState(resolution.host.battery)
        val target =
            SysUiKeyguardHostResolver.nativeToLockScreenTarget(resolution.host)

        val prearmed =
            armHomeAodPrearm(
                resolution = resolution,
                nativeToLockScreenTarget = target,
                source = "animateIconContainer",
            )
        if (
            homeAodOriginPending &&
            target == false &&
            !prearmed
        ) {
            onKeyguardHostResolution(
                resolution = resolution,
                source = "home-aod-origin:animateIconContainer",
            )
        }

        if (aodWindow?.boundaryPending != true) {
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
                SysUiKeyguardHostResolver.statusIconsPresentationAlpha(
                    resolution.host,
                ),
            "authority" to "native-status-icon-animation",
        )
        if (!eligible) return

        val visualOnlyIncomingKeyguard =
            armBoundaryHandoff(
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
        clearAodBoundary()
    }

    private fun clearAodBoundary() {
        // The visual boundary can finish before animateFullAod itself does.
        aodWindow =
            when (val state = aodWindow) {
                is AodWindow.Running -> state.copy(boundaryPending = false)
                is AodWindow.Waiting -> null
                null -> null
            }
    }

    private fun armBoundaryHandoff(
        resolution: SysUiKeyguardHostResolver.ResolveResult.Ready,
        nativeToLockScreenTarget: Boolean?,
        source: String,
        visualBoundaryReached: Boolean,
    ): Boolean {
        if (boundaryHandoff != null) {
            if (visualBoundaryReached) {
                onBoundaryVisualReady(source)
            }
            return true
        }

        val settings = FeaturePrefsOwner.current()
        val eligible =
            ScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguard,
                aodEnabled = settings.aod,
                lastStableFamilyScene = stableFamilyScene,
                nativeToLockScreenTarget = nativeToLockScreenTarget,
                homeNativeAodFallbackActive = (homeAodFallback == HomeAodFallback.ACTIVE),
            )
        if (!eligible) return false

        beginBoundaryHandoff(
            resolution = resolution,
            source = source,
            visualBoundaryReached = visualBoundaryReached,
        )
        return boundaryHandoff != null
    }

    private fun beginBoundaryHandoff(
        resolution: SysUiKeyguardHostResolver.ResolveResult.Ready,
        source: String,
        visualBoundaryReached: Boolean,
    ) {
        val settings = FeaturePrefsOwner.current()
        val statusIconsAlphaAtArm =
            SysUiKeyguardHostResolver.statusIconsPresentationAlpha(
                resolution.host,
            )
        val precommit =
            ScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguard,
                aodEnabled = settings.aod,
                lastStableFamilyScene = stableFamilyScene,
                nativeToLockScreenTarget =
                    SysUiKeyguardHostResolver.nativeToLockScreenTarget(
                        resolution.host,
                    ),
                statusIconsPresentationAlpha = statusIconsAlphaAtArm,
                homeNativeAodFallbackActive = (homeAodFallback == HomeAodFallback.ACTIVE),
            )
        boundaryHandoff =
            BoundaryHandoff(
                precommit = precommit,
                visualReady = visualBoundaryReached,
            )

        val attached =
            attachKeyguardRenderer(
                resolved = resolution.host,
                source = source + ":visual-only",
            )
        if (!attached) {
            clearBoundaryHandoff()
            return
        }
        logDiagnostic(
            level = Log.INFO,
            event = "aod.visualHandoff",
            component = "keyguardPresentation",
            state = "armed",
            "source" to source,
            "nativeLayoutOwnership" to
                if (precommit) {
                    "precommit-before-reveal"
                } else {
                    "deferred-until-stable"
                },
            "nativeVisualMask" to "clipBounds",
            "renderer" to "keyguard-combined",
            "statusIconsAlphaAtArm" to statusIconsAlphaAtArm,
            "nativeLifecycleAuthority" to "status-icons-presentation-alpha",
        )
    }

    private fun precommitBoundaryLayout(source: String) {
        val handoff = boundaryHandoff ?: return
        if (!handoff.precommit) return
        when (
            val result =
                SysUiPresentationOwner.commitKeyguardDeferredLayoutOwnership()
        ) {
            is SysUiPresentationOwner.Result.Active -> {
                onBoundaryPrelayoutReady(
                    result = result,
                    source = source + ":precommit-ready",
                )
            }

            is SysUiPresentationOwner.Result.Prepared -> {
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
                )
            }

            is SysUiPresentationOwner.Result.Failure -> {
                onKeyguardRuntimeFailure(result.reason)
                deactivateKeyguardRuntime("boundary-prelayout-commit-failed")
            }

            is SysUiPresentationOwner.Result.Inactive -> {
                deactivateKeyguardRuntime("boundary-prelayout-session-missing")
            }
        }
    }

    private fun onBoundaryPrelayoutReady(
        result: SysUiPresentationOwner.Result.Active,
        source: String,
    ) {
        val handoff = boundaryHandoff
        if (handoff == null || !handoff.precommit) {
            completeKeyguardCutover(
                result = result,
                source = source,
            )
            return
        }
        handoff.layoutReady = true
        keyguardRuntimeReady = false
        KeyguardRenderSession.setNativeHandoffActive(!handoff.visualReady)
        logDiagnostic(
            level = Log.INFO,
            event = "presentation.cutover",
            component = "keyguardPresentation",
            state =
                if (handoff.visualReady) {
                    "visual-handoff-prelayout-ready"
                } else {
                    "prelayout-ready-hidden"
                },
            "source" to source,
            "representedSlots" to result.representedSlots,
            "maskedViews" to result.maskedViews,
            "nativeVisualBoundaryReached" to handoff.visualReady,
        )
        if (handoff.visualReady) {
            reconcileCcForKeyguard(
                "keyguard-boundary-layout-ready",
            )
        }
    }

    private fun onBoundaryVisualReady(source: String) {
        val handoff = boundaryHandoff ?: return
        handoff.visualReady = true
        if (!handoff.precommit) return
        if (handoff.layoutReady) {
            KeyguardRenderSession.setNativeHandoffActive(false)
            logDiagnostic(
                level = Log.INFO,
                event = "aod.visualHandoff",
                component = "keyguardPresentation",
                state = "revealed",
                "source" to source,
                "layoutAuthority" to "precommitted-before-native-animation",
            )
            reconcileCcForKeyguard(
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
            )
        }
    }

    private fun clearBoundaryHandoff() {
        boundaryHandoff = null
    }

    private fun completeBoundaryHandoff(source: String): Boolean {
        val handoff = boundaryHandoff ?: return false
        if (handoff.precommit) {
            clearBoundaryHandoff()
            onKeyguardReadyChanged(
                ready = true,
                source = source + ":precommitted-layout",
            )
            return true
        }
        boundaryHandoff = null
        return when (
            val result =
                SysUiPresentationOwner.commitKeyguardDeferredLayoutOwnership()
        ) {
            is SysUiPresentationOwner.Result.Active -> {
                completeKeyguardCutover(
                    result = result,
                    source = source + ":compact-ready",
                )
                true
            }

            is SysUiPresentationOwner.Result.Prepared -> {
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
                )
                true
            }

            is SysUiPresentationOwner.Result.Failure -> {
                onKeyguardRuntimeFailure(result.reason)
                deactivateKeyguardRuntime("boundary-layout-commit-failed")
                true
            }

            is SysUiPresentationOwner.Result.Inactive -> {
                deactivateKeyguardRuntime("boundary-layout-session-missing")
                true
            }
        }
    }

    private fun armHomeAodPrearm(
        resolution: SysUiKeyguardHostResolver.ResolveResult.Ready,
        nativeToLockScreenTarget: Boolean?,
        source: String,
    ): Boolean {
        val settings = FeaturePrefsOwner.current()
        val homeOwned =
            SysUiPresentationOwner
                .homeSlots()
                .isNotEmpty()

        if (nativeToLockScreenTarget == true) {
            homeAodOriginPending = false
            homeAodTargetPrearmPending = false
            return false
        }

        if (homeAodTargetPrearmPending) {
            if (!settings.enabled || !settings.aod) {
                homeAodTargetPrearmPending = false
                return false
            }
            return true
        }

        val currentOriginEligible =
            ScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = settings.enabled,
                aodEnabled = settings.aod,
                steadySourceScene = steadyStatusSourceScene,
                lastStableFamilyScene = stableFamilyScene,
                homePresentationOwned = homeOwned,
                nativeToLockScreenTarget = nativeToLockScreenTarget,
            )
        val eligible =
            settings.enabled &&
                settings.aod &&
                nativeToLockScreenTarget == false &&
                (homeAodOriginPending || currentOriginEligible)
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
                if (homeAodOriginPending) {
                    "latched-home"
                } else {
                    "home"
                },
            "homePresentationOwnedAtArm" to homeOwned,
        )
        onKeyguardHostResolution(
            resolution = resolution,
            source = "home-aod-target-prearm:" + source,
        )
        return true
    }

    private fun onKeyguardAodStateUpdate(
        update: SysUiKeyguardAodSource.AodUpdate,
    ) {
        val waiting = aodWindow as? AodWindow.Waiting
        if (
            waiting != null &&
            !update.isAodAnimate &&
            ScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = waiting.toLockScreen,
                toAod = update.toAod,
                isAodAnimate = update.isAodAnimate,
            )
        ) {
            aodWindow = null
        }

        val settings = FeaturePrefsOwner.current()
        val activateHomeNativeAodFallback =
            ScenePolicy.shouldConsumeHomeNativeAodFallbackOnAodState(
                candidateActive = (homeAodFallback == HomeAodFallback.CANDIDATE),
                featureEnabled = settings.enabled,
                keyguardEnabled = settings.keyguard,
                aodEnabled = settings.aod,
                toAod = update.toAod,
                isAodAnimate = update.isAodAnimate,
            )
        if (activateHomeNativeAodFallback) {
            homeAodFallback = HomeAodFallback.ACTIVE
            clearBoundaryHandoff()
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
            )
        }

        val stableAod =
            SysUiKeyguardAodSource.isStableAod(
                toAod = update.toAod,
                isAodAnimate = update.isAodAnimate,
            )
        if (aodWindow !is AodWindow.Running && stableAod) {
            homeAodFallback = HomeAodFallback.NONE
            homeAodOriginPending = false
            homeAodTargetPrearmPending = false
            if (boundaryHandoff != null) {
                clearBoundaryHandoff()
                deactivateKeyguardRuntime("boundary-handoff-returned-to-aod")
            }
        }

        refreshStableFamilyFromAod(update)
        if (
            !update.isAodAnimate &&
            !update.toAod &&
            steadyStatusSourceScene == SourceScene.KEYGUARD
        ) {
            homeAodFallback = HomeAodFallback.NONE
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
                completeBoundaryHandoff(
                    source = "aod:" + update.source,
                )
        if (!boundaryHandoffHandled && boundaryHandoff == null) {
            SysUiKeyguardHostResolver.current()?.let { resolution ->
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
                    when ((aodWindow as? AodWindow.Waiting)?.toLockScreen) {
                        true -> "keyguard"
                        false -> "aod"
                        null -> "none"
                    },
                "homeOriginLatched" to homeAodOriginPending,
                "homeNativeAodFallbackCandidate" to (homeAodFallback == HomeAodFallback.CANDIDATE),
                "homeNativeAodFallbackActive" to (homeAodFallback == HomeAodFallback.ACTIVE),
            )
        }
    }

    private fun onSceneStateUpdate(update: SysUiSceneSource.SceneUpdate) {
        val sourceScene = SysUiSceneSource.steadySourceScene(update)
        if (
            steadyStatusSourceScene == SourceScene.HOME &&
            sourceScene == SourceScene.KEYGUARD &&
            stableFamilyScene == ScenePolicy.StableKeyguardAodScene.UNKNOWN &&
            aodWindow == null &&
            homeAodFallback == HomeAodFallback.NONE
        ) {
            val settings = FeaturePrefsOwner.current()
            if (
                ScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
                    featureEnabled = settings.enabled,
                    keyguardEnabled = settings.keyguard,
                    aodEnabled = settings.aod,
                    homePresentationOwned =
                        SysUiPresentationOwner.homeSlots().isNotEmpty(),
                    homeOriginConfirmed = true,
                )
            ) {
                // Keyguard ancestry can hide Home before the native AOD target arrives.
                homeAodFallback = HomeAodFallback.CANDIDATE
            }
        }
        if (sourceScene != SourceScene.UNKNOWN) {
            steadyStatusSourceScene = sourceScene
        }
        refreshStableFamilyFromScene(update, sourceScene)
        if (sourceScene == SourceScene.KEYGUARD) {
            SysUiKeyguardHostResolver.observe(update)?.let { resolution ->
                onKeyguardHostResolution(
                    resolution = resolution,
                    source = "scene-state",
                )
            }
        }
        if (sourceScene == SourceScene.HOME) {
            if (aodWindow !is AodWindow.Running) {
                homeAodFallback = HomeAodFallback.NONE
            }
            // UNLOCKED_STATUS_BAR + Home ancestry is the authoritative unlock
            // boundary. A Keyguard Control Center lease must never outlive it:
            // otherwise a fast first pull-down can consume stale KEYGUARD
            // source state and temporarily fall back to native QS icons.
            if (keyguardCcLeaseActive) {
                releaseKeyguardCcLease(
                    source = "authoritative-home",
                    reconcileReadiness = false,
                )
            }
            updateCcSourceEligibility(
                sourceScene = SourceScene.HOME,
                authority = "steady-source-view",
            )

            if (aodRendererAttached) {
                val retainAodHandoff =
                    SysUiPresentationOwner.aodClaimed()
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
                    )
                }
            }
            // Clear the old Keyguard renderer only after Control Center source
            // ownership has already moved to Home. Any readiness callback caused
            // by teardown therefore refreshes HOME eligibility, never stale
            // KEYGUARD eligibility.
            deactivateKeyguardRuntime("home-source-active")
        } else if (sourceScene != SourceScene.UNKNOWN) {
            updateCcSourceEligibility(
                sourceScene = sourceScene,
                authority = "steady-source-view",
            )
        }

        if (BuildConfig.RUNTIME_DIAGNOSTICS && detailedDiagnosticsEnabled) {
            SysUiKeyguardHostProbe.capture(update)?.let(::onKeyguardHostProbe)
        }

        SysUiTintSource.currentState(update.sourceView)?.let { state ->
            onTintStateUpdate(
                SysUiTintSource.TintUpdate(
                    sourceView = update.sourceView,
                    state = state,
                ),
            )
        }
        if (
            update.surface ==
                SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR
        ) {
            refreshStatusIconObservation("scene-unlocked")
        }
    }

    private fun refreshStableFamilyFromAod(
        update: SysUiKeyguardAodSource.AodUpdate,
    ) {
        if (update.isAodAnimate) return
        val next =
            when {
                SysUiKeyguardAodSource.isStableAod(
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
            updateStableFamily(next, "aod:" + update.source)
        }
    }

    private fun refreshStableFamilyFromScene(
        update: SysUiSceneSource.SceneUpdate,
        sourceScene: SourceScene,
    ) {
        when (sourceScene) {
            SourceScene.HOME -> {
                val aodState = SysUiKeyguardAodSource.currentState(update.sourceView)
                if (
                    aodState != null &&
                    !aodState.isAodAnimate &&
                    !aodState.toAod
                ) {
                    updateStableFamily(
                        ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                        "scene-home-stable",
                    )
                }
            }

            SourceScene.KEYGUARD -> {
                val aodState = SysUiKeyguardAodSource.currentState(update.sourceView)
                if (
                    aodState != null &&
                    !aodState.isAodAnimate &&
                    !SysUiKeyguardAodSource.isStableAod(
                        toAod = aodState.toAod,
                        isAodAnimate = aodState.isAodAnimate,
                    )
                ) {
                    updateStableFamily(
                        ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                        "scene-keyguard",
                    )
                }
            }

            SourceScene.UNKNOWN -> Unit
        }
    }

    private fun updateStableFamily(
        next: ScenePolicy.StableKeyguardAodScene,
        source: String,
    ) {
        if (next == stableFamilyScene) return
        val previous = stableFamilyScene
        stableFamilyScene = next
        logDiagnostic(
            level = Log.INFO,
            event = "scene.stableFamily",
            component = "keyguardAod",
            state = next.name.lowercase(),
            "source" to source,
            "previous" to previous.name,
        )
    }

    private fun resolveKeyguardAodProjection(
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
        fullAodVisualBoundary: Boolean = false,
    ): ScenePolicy.KeyguardAodProjection? {
        val settings = FeaturePrefsOwner.current()
        val aodState =
            SysUiKeyguardAodSource.currentState(resolved.battery)
                ?: return null
        return ScenePolicy.resolveKeyguardAodProjection(
            featureEnabled = settings.enabled,
            keyguardEnabled = settings.keyguard,
            aodEnabled = settings.aod,
            toAod = aodState.toAod,
            isAodAnimate = aodState.isAodAnimate,
            steadySourceScene = steadyStatusSourceScene,
            lastStableFamilyScene = stableFamilyScene,
            homePresentationOwned =
                SysUiPresentationOwner
                    .homeSlots()
                    .isNotEmpty(),
            keyguardStatusIconsAlpha =
                SysUiKeyguardHostResolver
                    .statusIconsPresentationAlpha(resolved),
            nativeToLockScreenTarget =
                SysUiKeyguardHostResolver
                    .nativeToLockScreenTarget(resolved),
            fullAodTargetSourceReady =
                SysUiPresentationRuntime.keyguardFullAodReady,
            fullAodTargetPending = aodWindow?.boundaryPending == true,
            fullAodVisualBoundary = fullAodVisualBoundary,
            homeAodTransitionOrigin = homeAodOriginPending,
            homeAodTargetPrearm = homeAodTargetPrearmPending,
            homeNativeAodFallbackActive = (homeAodFallback == HomeAodFallback.ACTIVE),
        )
    }

    private fun onKeyguardHostResolution(
        resolution: SysUiKeyguardHostResolver.ResolveResult,
        source: String,
        fullAodVisualBoundary: Boolean = false,
    ) {
        val settings = FeaturePrefsOwner.current()
        when (resolution) {
            is SysUiKeyguardHostResolver.ResolveResult.Ready -> {
                if (!settings.enabled) {
                    deactivateAodRuntime("feature-ineligible")
                    deactivateKeyguardRuntime("feature-ineligible")
                    return
                }
                if (!SysUiPresentationRuntime.keyguardAodReady) {
                    deactivateAodRuntime("aod-authority-unavailable")
                    deactivateKeyguardRuntime("aod-authority-unavailable")
                    if (settings.keyguard || settings.aod) {
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

            is SysUiKeyguardHostResolver.ResolveResult.Inactive -> {
                deactivateAodRuntime(
                    "scene-inactive:" + resolution.surface.name,
                )
                deactivateKeyguardRuntime(
                    "scene-inactive:" + resolution.surface.name,
                )
            }

            is SysUiKeyguardHostResolver.ResolveResult.Failure -> {
                if (settings.enabled && (settings.keyguard || settings.aod)) {
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
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
        source: String,
    ): Boolean {
        val failure =
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
        return if (failure == null) {
            aodRendererAttached = false
            logDiagnostic(
                level = Log.INFO,
                event = "renderer.attach",
                component = "keyguardRenderer",
                state = "ready",
                "source" to source,
                "rawState" to resolved.rawState,
                "aodOwned" to false,
            )
            true
        } else {
            deactivateKeyguardRuntime("renderer-attach-failed")
            logDiagnostic(
                level = Log.WARN,
                event = "renderer.attach",
                component = "keyguardRenderer",
                state = "unavailable",
                "source" to source,
                "reason" to failure,
                "fallback" to "native-keyguard",
            )
            false
        }
    }

    private fun attachAodRenderer(
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
        source: String,
    ): Boolean {
        val failure =
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
                    onAodReadyChanged(
                        ready = ready,
                        source = source,
                    )
                },
            )
        return if (failure == null) {
            aodRendererAttached = true
            logDiagnostic(
                level = Log.INFO,
                event = "renderer.attach",
                component = "aodRenderer",
                state = "ready",
                "source" to source,
                "rawState" to resolved.rawState,
                "aodOwned" to true,
            )
            true
        } else {
            deactivateAodRuntime("renderer-attach-failed")
            logDiagnostic(
                level = Log.WARN,
                event = "renderer.attach",
                component = "aodRenderer",
                state = "unavailable",
                "source" to source,
                "reason" to failure,
                "fallback" to "native-aod",
            )
            false
        }
    }

    private fun onKeyguardReadyChanged(
        ready: Boolean,
        source: String,
    ) {
        keyguardReadyObserved = ready
        if (!ready) {
            if (incomingKeyguardReadyForCc()) {
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.readiness",
                    component = "keyguardPresentation",
                    state = "retained",
                    "source" to source,
                    "reason" to "incoming-boundary-presentation-ready",
                    "nativeFraction" to ccExpansion,
                    "leaseActive" to keyguardCcLeaseActive,
                        )
                return
            }
            if (shouldRetainKeyguardCcLease()) {
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.readiness",
                    component = "keyguardPresentation",
                    state = "retained",
                    "source" to source,
                    "nativeFraction" to ccExpansion,
                    "leaseActive" to keyguardCcLeaseActive,
                    "cleanupDeferredUntil" to "native-control-center-handoff-end",
                        )
                return
            }
            onKeyguardReadinessLost(source)
            return
        }

        val settings = FeaturePrefsOwner.current()
        if (!settings.enabled || !settings.keyguard) {
            deactivateKeyguardRuntime("feature-ineligible")
            return
        }

        val resolved = SysUiKeyguardHostResolver.current()
        if (resolved !is SysUiKeyguardHostResolver.ResolveResult.Ready) {
            deactivateKeyguardRuntime("resolver-not-ready")
            return
        }
        if (
            resolveKeyguardAodProjection(
                resolved = resolved.host,
                fullAodVisualBoundary = boundaryHandoff != null,
            ) != ScenePolicy.KeyguardAodProjection.KEYGUARD
        ) {
            deactivateKeyguardRuntime("projection-ineligible")
            return
        }

        val visualOnlyBoundary = boundaryHandoff != null
        when (
            val result =
                SysUiPresentationOwner.activateKeyguard(
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
                            boundaryHandoff?.precommit == true
                        ) {
                            onBoundaryPrelayoutReady(
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
            is SysUiPresentationOwner.Result.Active -> {
                if (visualOnlyBoundary && boundaryHandoff?.precommit == true) {
                    onBoundaryPrelayoutReady(
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

            is SysUiPresentationOwner.Result.Prepared -> {
                if (visualOnlyBoundary && boundaryHandoff?.precommit == true) {
                    keyguardRuntimeReady = false
                    KeyguardRenderSession.setNativeHandoffActive(true)
                    precommitBoundaryLayout(source)
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
                )
                if (!visualOnlyBoundary) {
                    refreshCcSourceEligibility(
                        "keyguard-compact-layout-pending",
                    )
                }
            }

            is SysUiPresentationOwner.Result.Failure -> {
                keyguardRuntimeReady = false
                KeyguardRenderSession.setNativeHandoffActive(true)
                SysUiPresentationOwner.deactivateKeyguard("activation-failed")
                logDiagnostic(
                    level = Log.WARN,
                    event = "presentation.cutover",
                    component = "keyguardPresentation",
                    state = "native",
                    "source" to source,
                    "reason" to result.reason,
                    "fallback" to "native-keyguard",
                )
                refreshCcSourceEligibility("keyguard-activation-failed")
            }

            is SysUiPresentationOwner.Result.Inactive -> Unit
        }
    }

    private fun onKeyguardReadinessLost(source: String) {
        clearBoundaryHandoff()
        keyguardRuntimeReady = false
        KeyguardRenderSession.setNativeHandoffActive(true)
        SysUiPresentationOwner.deactivateKeyguard("readiness-lost:" + source)
        reconcileCcForKeyguard("keyguard-readiness-lost")
    }

    private fun completeKeyguardCutover(
        result: SysUiPresentationOwner.Result.Active,
        source: String,
    ) {
        val settings = FeaturePrefsOwner.current()
        val resolved = SysUiKeyguardHostResolver.current()
        if (
            !settings.enabled ||
            !settings.keyguard ||
            resolved !is SysUiKeyguardHostResolver.ResolveResult.Ready ||
            resolveKeyguardAodProjection(resolved.host) !=
                ScenePolicy.KeyguardAodProjection.KEYGUARD
        ) {
            deactivateKeyguardRuntime("cutover-projection-ineligible")
            return
        }
        keyguardRuntimeReady = true
        keyguardReadyObserved = true
        if (aodWindow == null && homeAodFallback == HomeAodFallback.CANDIDATE) {
            // A real Keyguard cutover supersedes the preceding Home-origin hint.
            homeAodFallback = HomeAodFallback.NONE
        }
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
        refreshCcSourceEligibility("keyguard-ready")
    }

    private fun onKeyguardRuntimeFailure(reason: String) {
        clearBoundaryHandoff()
        keyguardCcLeaseActive = false
        keyguardReadyObserved = false
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
        reconcileCcForKeyguard("keyguard-fail-native")
    }

    private fun deactivateKeyguardRuntime(source: String) {
        val wasReady = keyguardRuntimeReady
        clearBoundaryHandoff()
        keyguardCcLeaseActive = false
        keyguardReadyObserved = false
        keyguardRuntimeReady = false
        KeyguardRenderSession.setNativeHandoffActive(true)
        SysUiPresentationOwner.deactivateKeyguard(source)
        KeyguardRenderSession.detach()
        if (wasReady) {
            reconcileCcForKeyguard("keyguard-deactivate:" + source)
        }
    }

    private fun onAodReadyChanged(
        ready: Boolean,
        source: String,
    ) {
        if (!ready) {
            onAodReadinessLost(source)
            return
        }

        val settings = FeaturePrefsOwner.current()
        if (!settings.enabled || !settings.aod) {
            deactivateAodRuntime("feature-ineligible")
            return
        }

        val resolved = SysUiKeyguardHostResolver.current()
        if (resolved !is SysUiKeyguardHostResolver.ResolveResult.Ready) {
            deactivateAodRuntime("resolver-not-ready")
            return
        }
        val aodState =
            SysUiKeyguardAodSource.currentState(resolved.host.battery)
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
                        SysUiPresentationOwner
                            .homeSlots()
                            .isNotEmpty()
                )

        when (
            val result =
                SysUiPresentationOwner.activateAod(
                    resolved = resolved.host,
                    preMaskBeforeLayout = homeTransitionPrearm,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onFailNative = ::onAodRuntimeFailure,
                    onReady = { active ->
                        completeAodCutover(
                            result = active,
                            source = "native-layout",
                        )
                    },
                )
        ) {
            is SysUiPresentationOwner.Result.Active -> {
                completeAodCutover(
                    result = result,
                    source = source,
                )
            }

            is SysUiPresentationOwner.Result.Prepared -> {
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

            is SysUiPresentationOwner.Result.Failure -> {
                KeyguardRenderSession.setAodNativeHandoffActive(true)
                SysUiPresentationOwner.deactivateAod("activation-failed")
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

            is SysUiPresentationOwner.Result.Inactive -> Unit
        }
    }

    private fun onAodReadinessLost(source: String) {
        KeyguardRenderSession.setAodNativeHandoffActive(true)
        SysUiPresentationOwner.deactivateAod("readiness-lost:" + source)
    }

    private fun completeAodCutover(
        result: SysUiPresentationOwner.Result.Active,
        source: String,
    ) {
        val settings = FeaturePrefsOwner.current()
        val resolved = SysUiKeyguardHostResolver.current()
        if (
            !settings.enabled ||
            !settings.aod ||
            resolved !is SysUiKeyguardHostResolver.ResolveResult.Ready ||
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

    private fun onAodRuntimeFailure(reason: String) {
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
        SysUiPresentationOwner.deactivateAod(source)
        KeyguardRenderSession.detachAod()
    }

    private fun onKeyguardHostProbe(snapshot: SysUiKeyguardHostProbe.Snapshot) {
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
            "rendering" to
                if (FeaturePrefsOwner.current().keyguard) {
                    "candidate"
                } else {
                    "disabled"
                },
            "suppression" to if (keyguardRuntimeReady) "active" else "native",
            "healthSnapshot" to false,
        )
    }

    private fun teardownOldGeneration(
        continuousHandoff: Boolean = false,
    ) {
        ccSourceInstalled = false
        controlCenterSceneVisible = false
        controlCenterSceneEligible = false
        controlCenterSourceScene = SourceScene.UNKNOWN
        steadyStatusSourceScene = SourceScene.UNKNOWN
        stableFamilyScene =
            ScenePolicy.StableKeyguardAodScene.UNKNOWN
        aodWindow = null
        homeAodTargetPrearmPending = false
        ccExpansion = 0f
        keyguardRuntimeReady = false
        aodRendererAttached = false
        keyguardReadyObserved = false
        keyguardCcLeaseActive = false
        CcTransitionOwner.detach("hotReload-oldGeneration")
        CcRenderSession.detach(
            source = "hotReload-oldGeneration",
            releaseNativePresentation = !continuousHandoff,
        )
        HomeRenderSession.detach()
        KeyguardRenderSession.detach()
        KeyguardRenderSession.detachAod()
        val restoredPresentationViews =
            SysUiPresentationOwner.releaseGenerationForHotReload(
                requestLayout =
                    !continuousHandoff,
            )
        NativeNetworkSuppressor.deactivate("hotReload-oldGeneration")
        StatusBarStableSession.detach()
        SysUiCoreRuntime.detach()
        SysUiPresentationRuntime.resetRuntimeState()
        SysUiKeyguardHostResolver.resetRuntimeState()
        PresentationStore.reset()
        SysUiIslandSource.resetRuntimeState()
        SysUiCcSource.resetRuntimeState()

        logDiagnostic(
            level = Log.INFO,
            event = "runtime.teardown",
            component = "runtimeSession",
            state = "released",
            "source" to "hotReload.oldGeneration",
            "homePresentationRestoredViews" to restoredPresentationViews,
            "continuousHandoff" to continuousHandoff,
            "intermediateRequestLayout" to
                !continuousHandoff,
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
                SysUiCoreRuntime.attach(
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
            "subscriptionId" to SysUiDefaultDataSubSource.currentSubscriptionId(),
        )
        logDiagnostic(
            level = if (coreRuntime?.connectivityReady == true) Log.INFO else Log.WARN,
            event = "source.attach",
            component = "connectivity",
            state = if (coreRuntime?.connectivityReady == true) "ready" else "unavailable",
            "source" to source,
        )
        refreshMobilePresentation()

        val stableFailure =
            StatusBarStableSession.attach(
                host = host,
                onEvent = { event ->
                    if (detailedDiagnosticsEnabled) {
                        log(Log.INFO, TAG, event)
                    }
                },
            )
        if (stableFailure == null) {
            logDiagnostic(
                level = Log.INFO,
                event = "session.attach",
                component = "stableStatus",
                state = "ready",
                "source" to source,
            )
            // Hot Reload may not be followed by another battery callback.
            if (StatusStateStore.snapshot().battery?.charging == true) {
                val iconId = SysUiBatterySource.readHostChargingIconId(host)
                if (iconId != null) {
                    StatusStateStore.updateBatteryChargingIcon(iconId)?.let { snapshot ->
                        onCombinedStateChanged(snapshot)
                    }
                }
                logDiagnostic(
                    level = Log.INFO,
                    event = "glyph.seed",
                    component = "batteryState",
                    state = if (iconId != null) "ready" else "unavailable",
                    "source" to source,
                    "chargingIconId" to (iconId ?: "unavailable"),
                    "storedIconId" to
                        (StatusStateStore.snapshot().battery?.chargingIconResId ?: "unavailable"),
                )
            }
        } else {
            logDiagnostic(
                level = Log.WARN,
                event = "session.attach",
                component = "stableStatus",
                state = "unavailable",
                "reason" to stableFailure,
                "source" to source,
            )
        }

        when (
            val observation =
                NativeNetworkSuppressor.attachObserver(
                    host = host,
                    source = source,
                )
        ) {
            is NativeNetworkSuppressor.Result.Active -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "source.attach",
                    component = "statusIconObservation",
                    state = "ready",
                    "source" to source,
                    "mode" to "observation-only",
                )
            }
            is NativeNetworkSuppressor.Result.Pending -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "source.attach",
                    component = "statusIconObservation",
                    state = "pending",
                    "source" to source,
                    "reason" to observation.reason,
                    "trigger" to "home-dark-icon-manager-registration",
                )
            }
            is NativeNetworkSuppressor.Result.Failure -> {
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
            is NativeNetworkSuppressor.Result.Inactive -> Unit
        }

        val rendererInitialTintState =
            initialTintState?.let { transferred ->
                TintAuthority.rebaseTransferred(
                    transferred = transferred,
                    liveStatusIconTint =
                        NativeNetworkSuppressor
                            .currentAppliedStatusIconTint(),
                )
            }

        val renderFailure =
            HomeRenderSession.attach(
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
        if (renderFailure == null) {
            logDiagnostic(
                level = Log.INFO,
                event = "renderer.attach",
                component = "renderer",
                state = "ready",
                "source" to source,
            )
        } else {
            logDiagnostic(
                level = Log.WARN,
                event = "renderer.attach",
                component = "renderer",
                state = "unavailable",
                "reason" to renderFailure,
                "source" to source,
            )
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
        if (!FeaturePrefsOwner.current().enabled) {
            HomeRenderSession.setNativeHandoffActive(true)
            SysUiPresentationOwner.deactivate("feature-disabled:" + source)
            return
        }
        if (!ready) {
            HomeRenderSession.setNativeHandoffActive(true)
            SysUiPresentationOwner.deactivate("readiness-lost:" + source)
            return
        }

        when (val result = SysUiPresentationOwner.activate(host)) {
            is SysUiPresentationOwner.Result.Active -> {
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
            is SysUiPresentationOwner.Result.Prepared -> {
                HomeRenderSession.setNativeHandoffActive(true)
                SysUiPresentationOwner.deactivate("unexpected-prepared")
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
            is SysUiPresentationOwner.Result.Failure -> {
                HomeRenderSession.setNativeHandoffActive(true)
                SysUiPresentationOwner.deactivate("activation-failed")
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
            is SysUiPresentationOwner.Result.Inactive -> Unit
        }
    }

    private fun onHomeRuntimeFailure(reason: String) {
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

        SysUiNetworkSource.bindingTopologyLines().forEach { line ->
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
                            "source" to source,
                    "root" to subtree.rootClassName,
                    "children" to subtree.rootChildCount,
                    "nodes" to subtree.entries.size,
                    "truncated" to subtree.truncated,
                )
            }
        }
    }

    private fun onStatusHostCaptured(capture: SysUiHostRegistry.Capture) {
        logDiagnostic(
            level = Log.INFO,
            event = "host.capture",
            component = "statusHost",
            "identity" to capture.identity,
            "replacement" to capture.replacement,
        )
        attachHostRuntime(
            host = capture.host,
            source = if (capture.replacement) "hostReplacement" else "hostCapture",
        )

    }

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
                prefs = getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME),
                forceDetailed = BuildConfig.DEVELOPMENT_PROBES,
                onChanged = ::setDetailedDiagnosticsEnabled,
            )
        }.onSuccess { result ->
            logDiagnostic(
                level = Log.INFO,
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "ready",
                "level" to if (result.detailed) "detailed" else "general",
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

    private fun bindFeatureCfg() {
        runCatching {
            FeaturePrefsOwner.bind(
                source = getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME),
                onChanged = ::onFeatureCfgChanged,
            )
        }.onSuccess { cfg ->
            logDiagnostic(
                level = Log.INFO,
                event = "runtimePreferences.bind",
                component = "featureSettings",
                state = "ready",
                "combinedStatusEnabled" to cfg.enabled,
                "keyguardEnabled" to cfg.keyguard,
                "aodEnabled" to cfg.aod,
                "transport" to "remote-preferences",
            )
        }.onFailure { error ->
            FeaturePrefsOwner.unbind()
            onFeatureCfgChanged(
                FeaturePrefsOwner.current(),
                null,
            )
            logDiagnostic(
                level = Log.WARN,
                event = "runtimePreferences.bind",
                component = "featureSettings",
                state = "unavailable",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "fallback" to "native-systemui",
            )
        }
    }

    private fun onFeatureCfgChanged(
        cfg: FeatureCfg,
        transportNs: Long?,
    ) {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            val dispatch =
                Runnable {
                    onFeatureCfgChanged(
                        cfg = cfg,
                        transportNs = transportNs,
                    )
                }
            val hostView = SysUiHostRegistry.current() as? android.view.View
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
                "combinedStatusEnabled" to cfg.enabled,
                "keyguardEnabled" to cfg.keyguard,
                "aodEnabled" to cfg.aod,
                "fallback" to "leave-current-native-ownership-unchanged",
            )
            return
        }

        if (
            !cfg.enabled ||
            !cfg.keyguard ||
            cfg.aod
        ) {
            homeAodFallback = HomeAodFallback.NONE
        }
        HomeRenderSession.onFeatureCfgChanged(cfg)
        KeyguardRenderSession.onFeatureCfgChanged(cfg)
        CcRenderSession.onFeatureCfgChanged(cfg)

        if (!cfg.enabled) {
            releaseFeatureOwnership("feature-disabled")
            deactivateAodRuntime("feature-disabled")
            deactivateKeyguardRuntime("feature-disabled")
        } else {
            if (!cfg.keyguard) {
                deactivateKeyguardRuntime("keyguard-feature-disabled")
            }
            if (!cfg.aod) {
                deactivateAodRuntime("aod-feature-disabled")
            }
            SysUiKeyguardHostResolver.current()?.let { resolution ->
                onKeyguardHostResolution(
                    resolution = resolution,
                    source = "feature-settings",
                )
            }
        }
        refreshCcSourceEligibility("feature-settings")

        logDiagnostic(
            level = Log.INFO,
            event = "featureSettings.changed",
            component = "combinedStatus",
            state = if (cfg.enabled) "enabled" else "disabled",
            "combinedStatusEnabled" to cfg.enabled,
            "keyguardEnabled" to cfg.keyguard,
            "aodEnabled" to cfg.aod,
            "preferenceTransportMs" to
                (
                    transportNs
                        ?.let { nanos -> nanos / 1_000_000.0 }
                        ?: "initial-bind"
                ),
            "fallback" to if (cfg.enabled) "combined-status" else "native-systemui",
        )
    }

    private fun releaseFeatureOwnership(source: String) {
        aodWindow = null
        clearBoundaryHandoff()
        homeAodFallback = HomeAodFallback.NONE
        homeAodOriginPending = false
        homeAodTargetPrearmPending = false
        controlCenterSceneEligible = false
        keyguardCcLeaseActive = false
        CcRenderSession.setSceneEligible(false)
        CcTransitionOwner.setSceneEligible(false)
        SysUiPresentationOwner.deactivateCc(source)
        SysUiPresentationOwner.deactivateAod(source)
        SysUiPresentationOwner.deactivateKeyguard(source)
        SysUiPresentationOwner.deactivate(source)
        NativeNetworkSuppressor.deactivate(source)
        HomeRenderSession.setNativeHandoffActive(true)
    }

    private fun bindVisualCfg() {
        runCatching {
            VisualPrefsOwner.bind(
                source = getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME),
                onChanged = ::onVisualCfgChanged,
            )
        }.onSuccess { settings ->
            logDiagnostic(
                level = Log.INFO,
                event = "runtimePreferences.bind",
                component = "visualSettings",
                state = "ready",
                "layout" to settings.layout.persistedValue,
                "mobileFollowsBattery" to settings.mobileFollowsBatteryColor,
                "networkFollowsBattery" to settings.centerFollowsBatteryColor,
                "batteryNumber" to settings.showTopReadout,
                "chargingIcon" to settings.showTopChargingIcon,
                "batteryNumberFollowsBattery" to settings.topTextFollowsBatteryColor,
                "chargingIconFollowsBattery" to
                    settings.topChargingIconFollowsBatteryColor,
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

    private fun onVisualCfgChanged(
        visual: VisualCfg,
    ) {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            val dispatch =
                Runnable {
                    onVisualCfgChanged(visual)
                }
            val hostView = SysUiHostRegistry.current() as? android.view.View
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

        if (visual != VisualPrefsOwner.current()) {
            return
        }

        HomeRenderSession.onVisualCfgChanged(visual)
        KeyguardRenderSession.onVisualCfgChanged(visual)
        CcRenderSession.onVisualCfgChanged(visual)
        SysUiPresentationOwner.onVisualCfgChanged()
        if (detailedDiagnosticsEnabled) {
            logDiagnostic(
                level = Log.INFO,
                event = "visualSettings.changed",
                component = "renderer",
                "layout" to visual.layout.persistedValue,
                "combinedScale" to visual.combinedScale,
                "wifiSizeScale" to visual.wifiScale,
                "mobileTypeSizeScale" to visual.mobileTypeScale,
                "mobileFollowsBattery" to visual.mobileFollowsBatteryColor,
                "networkFollowsBattery" to visual.centerFollowsBatteryColor,
                "batteryNumber" to visual.showTopReadout,
                "chargingIcon" to visual.showTopChargingIcon,
                "batteryNumberFollowsBattery" to visual.topTextFollowsBatteryColor,
                "chargingIconFollowsBattery" to
                    visual.topChargingIconFollowsBatteryColor,
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
            if (detailedDiagnosticsEnabled) {
                SysUiTintSource.resetDiagnosticProbes()
            }
            logDiagnostic(
                level = Log.INFO,
                event = "diagnostics.level",
                component = "diagnostics",
                "level" to if (detailedDiagnosticsEnabled) "detailed" else "general",
            )
        }
    }

    private fun rotateDiagnosticSession() {
        runtimeSessionId = newRuntimeSessionId()
        diagnosticSequence.set(0L)
        renderTraceSequence.set(0L)
        lastBatteryNumberProbeSummary = null
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
        writeDiagnostic(level, event, component, state, fields)
    }

    private fun logDiagnostic(
        level: Int,
        event: String,
        component: String,
        vararg fields: Pair<String, Any?>,
    ) {
        writeDiagnostic(level, event, component, null, fields)
    }

    private fun writeDiagnostic(
        level: Int,
        event: String,
        component: String,
        state: String?,
        fields: Array<out Pair<String, Any?>>,
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
