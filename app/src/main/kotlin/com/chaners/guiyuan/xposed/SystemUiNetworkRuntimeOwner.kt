package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedModule

internal object SystemUiNetworkRuntimeOwner {
    private var current: SysUiNetworkSource.InstallResult? = null

    val installedHookCount: Int
        @Synchronized get() = current?.handles?.size ?: 0

    val wifiReady: Boolean
        @Synchronized get() = current?.wifiReady == true

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onWifiState: (StatusStateStore.WifiState) -> Unit,
        onMobileIcon: (StatusStateStore.MobileIconUpdate) -> Unit,
        onMobileSignalWillApply: ((android.widget.ImageView) -> Unit)?,
        onPresentationChanged: (() -> Unit)?,
        onEvent: ((String) -> Unit)?,
    ): SysUiNetworkSource.InstallResult =
        SysUiNetworkSource.install(
            module = module,
            classLoader = classLoader,
            onWifiState = onWifiState,
            onMobileIcon = onMobileIcon,
            onMobileSignalWillApply = onMobileSignalWillApply,
            onPresentationChanged = onPresentationChanged,
            onEvent = onEvent,
        ).also { current = it }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        SysUiNetworkSource.resetEventState()
    }
}
