package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam

internal object SysUiHotReload {
    internal sealed interface PrepareResult {
        data class Ready(
            val host: Any,
            val wifiRoots: Int,
            val mobileRoots: Int,
            val tintTransferred: Boolean,
            val controlCenterCompactReady: Boolean,
        ) : PrepareResult

        data class Unavailable(
            val reason: String,
            val wifiRoots: Int = 0,
            val mobileRoots: Int = 0,
        ) : PrepareResult
    }

    internal data class HookTakeover(
        val hostHandle: HookHandle,
        val removedHooks: Int,
        val classLoader: ClassLoader,
    )

    fun prepare(
        param: HotReloadingParam,
        generationHandoff: Runnable,
    ): PrepareResult {
        if (!SysUiHostHook.isReady) {
            return PrepareResult.Unavailable("status-host-hook-not-ready")
        }

        val host =
            SysUiHostRegistry.current()
                ?: return PrepareResult.Unavailable("status-host-not-captured")
        val snapshot = StatusStateStore.snapshot()
        val stableTint = HomeRenderSession.currentTintState()
        val bindingCounts = SysUiNetworkSource.hotReloadBindingCounts()
        val bindingStateReady =
            (snapshot.wifi is StatusStateStore.WifiState.Unknown || bindingCounts.first > 0) &&
                (snapshot.mobile.isEmpty() || bindingCounts.second > 0)
        if (!bindingStateReady) {
            return PrepareResult.Unavailable(
                reason = "network-bindings-not-ready",
                wifiRoots = bindingCounts.first,
                mobileRoots = bindingCounts.second,
            )
        }

        val controlCenterCompactReady =
            CcRenderSession
                .nativeReadyForHotReload()

        val transfer =
            HotReloadTransfer.capture(
                host = host,
                state = StatusStateStore.exportHotReloadState(),
                bindings = SysUiNetworkSource.exportHotReloadBindings(),
                // Legacy transfer slot remains null for compatibility. Notification
                // Shade now follows the native system_icons carrier lifecycle.
                notificationShadeHomeEligible = null,
                controlCenterHomeEligible =
                    SysUiCcSource.currentHomeEligibility(),
                appliedTint = stableTint?.appliedTint,
                statusIconTint = stableTint?.statusIconTint,
                controlCenterFakeHost =
                    CcRenderSession.hotReloadHost(),
                controlCenterCompactReady = controlCenterCompactReady,
                generationHandoff = generationHandoff,
            ) ?: return PrepareResult.Unavailable(
                reason = "state-transfer-capture-failed",
                wifiRoots = bindingCounts.first,
                mobileRoots = bindingCounts.second,
            )

        runCatching {
            param.setSavedInstanceState(transfer)
        }.getOrElse { error ->
            return PrepareResult.Unavailable(
                reason = error.message ?: error.javaClass.simpleName,
                wifiRoots = bindingCounts.first,
                mobileRoots = bindingCounts.second,
            )
        }

        return PrepareResult.Ready(
            host = host,
            wifiRoots = bindingCounts.first,
            mobileRoots = bindingCounts.second,
            tintTransferred = stableTint != null,
            controlCenterCompactReady = controlCenterCompactReady,
        )
    }

    fun takeOverHooks(
        param: HotReloadedParam,
        onCaptured: (SysUiHostRegistry.Capture) -> Unit,
    ): HookTakeover? {
        val oldHandles = param.oldHookHandles
        val hostHandle = SysUiHostHook.findOwnedHandle(oldHandles)
            ?: run {
                oldHandles.forEach { handle -> runCatching { handle.unhook() } }
                return null
            }

        SysUiHostHook.replace(
            handle = hostHandle,
            onCaptured = onCaptured,
        )

        var removed = 0
        oldHandles.forEach { handle ->
            if (handle !== hostHandle) {
                runCatching { handle.unhook() }
                removed += 1
            }
        }

        val classLoader = hostHandle.executable.declaringClass.classLoader ?: return null
        return HookTakeover(
            hostHandle = hostHandle,
            removedHooks = removed,
            classLoader = classLoader,
        )
    }

    fun restoreTransfer(param: HotReloadedParam): HotReloadTransfer.Restored? =
        HotReloadTransfer.restore(param.savedInstanceState)
}
