package com.chaners.guiyuan.xposed.network

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
        sinks: SysUiNetworkSource.Sinks,
        diagnostics: SysUiNetworkSource.Diagnostics,
    ): SysUiNetworkSource.InstallResult =
        SysUiNetworkSource.install(
            module = module,
            classLoader = classLoader,
            sinks = sinks,
            diagnostics = diagnostics,
        ).also { current = it }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        SysUiNetworkSource.resetEventState()
    }
}
