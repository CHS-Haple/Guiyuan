package com.chaners.guiyuan.xposed

import android.os.Handler
import android.os.Looper
import android.view.View
import com.chaners.guiyuan.xposed.network.SysUiNetworkSource
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
        val removedHooks: Int,
        val classLoader: ClassLoader,
    )

    fun prepare(
        param: HotReloadingParam,
    ): PrepareResult {
        if (!SysUiHostHook.isReady) {
            return PrepareResult.Unavailable("status-host-hook-not-ready")
        }

        val host =
            SysUiHostRegistry.current()
                ?: return PrepareResult.Unavailable("status-host-not-captured")
        // A captured host can survive detachment; it must not own the next generation.
        if ((host as? View)?.isAttachedToWindow != true) {
            return PrepareResult.Unavailable("status-host-detached")
        }
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

        // V10 retires native CC layout before the new module can adopt it.
        val controlCenterCompactReady = false

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

    fun retireOnMain(cleanup: () -> Unit): Boolean {
        val main = Looper.getMainLooper()
        if (Looper.myLooper() === main) {
            cleanup()
            return true
        }
        val handler = Handler(main)
        return MainThreadRetire(handler::post, handler::removeCallbacks).run(cleanup)
    }

    fun releaseOldHooks(param: HotReloadedParam): Int =
        param.oldHookHandles.count { handle ->
            try {
                handle.unhook()
                false
            } catch (error: Throwable) {
                if (error is VirtualMachineError || error is ThreadDeath) throw error
                true
            }
        }

    fun takeOverHooks(
        param: HotReloadedParam,
        onCaptured: (SysUiHostRegistry.Capture) -> Unit,
    ): HookTakeover? {
        val oldHandles = param.oldHookHandles
        val hostHandle = SysUiHostHook.findOwnedHandle(oldHandles)
            ?: run {
                val failed = releaseOldHooks(param)
                if (failed > 0) {
                    error("status-host-hook-missing-cleanup-failed:$failed")
                }
                return null
            }
        val classLoader = hostHandle.executable.declaringClass.classLoader
            ?: return null

        var removed = 0
        oldHandles.forEach { handle ->
            if (handle !== hostHandle) {
                try {
                    handle.unhook()
                } catch (error: Throwable) {
                    if (error is VirtualMachineError || error is ThreadDeath) throw error
                    throw IllegalStateException("stale-hook-unhook-failed", error)
                }
                removed += 1
            }
        }

        SysUiHostHook.replace(
            handle = hostHandle,
            onCaptured = onCaptured,
        )

        return HookTakeover(
            removedHooks = removed,
            classLoader = classLoader,
        )
    }

    fun restoreTransfer(param: HotReloadedParam): HotReloadTransfer.Restored? =
        HotReloadTransfer.restore(param.savedInstanceState)
}
