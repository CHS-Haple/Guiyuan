package com.chaners.guiyuan.xposed.network

import com.chaners.guiyuan.xposed.StatusStateStore
import io.github.libxposed.api.XposedModule

internal object SysUiNetworkRuntime {
    private var current: SysUiNetworkSource.InstallResult? = null

    val installedHookCount: Int
        @Synchronized get() = current?.handles?.size ?: 0

    val wifiReady: Boolean
        @Synchronized get() = current?.wifiReady == true

    val mobileReady: Boolean
        @Synchronized get() = current?.mobileReady == true

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onWifiState: (StatusStateStore.WifiState) -> Unit,
        onWifiActivity: (Int?) -> Unit,
        onRuntimeFailure: (String, Throwable) -> Unit,
        onMobileSignal: (subscriptionId: Int, signal: SignalStrength) -> Unit,
        onMobileActivityResource: (subscriptionId: Int, resourceId: Int?, visible: Boolean) -> Unit,
        onMobileActivityVisibility: (subscriptionId: Int, visible: Boolean) -> Unit,
        onPresentationChanged: (() -> Unit)?,
        onEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
    ): SysUiNetworkSource.InstallResult =
        SysUiNetworkSource.install(
            module = module,
            classLoader = classLoader,
            onWifiState = onWifiState,
            onWifiActivity = onWifiActivity,
            onRuntimeFailure = onRuntimeFailure,
            onMobileSignal = onMobileSignal,
            onMobileActivityResource = onMobileActivityResource,
            onMobileActivityVisibility = onMobileActivityVisibility,
            onPresentationChanged = onPresentationChanged,
            onEvent = onEvent,
            isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
        ).also { current = it }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        SysUiNetworkSource.resetEventState()
    }
}
