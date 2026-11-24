package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedModule

internal object SysUiBatteryRuntime {
    private var hooks = 0

    val installedHookCount: Int
        @Synchronized get() = hooks + SysUiBatterySource.failedInstallHookCount

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        sinks: SysUiBatterySource.Sinks,
    ): Int {
        hooks =
            SysUiBatterySource.install(
                module = module,
                classLoader = classLoader,
                sinks = sinks,
            ).size
        return hooks
    }

    @Synchronized
    fun resetRuntimeState() {
        hooks = 0
        SysUiBatterySource.resetRuntimeState()
    }
}
