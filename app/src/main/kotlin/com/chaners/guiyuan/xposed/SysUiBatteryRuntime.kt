package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedModule

internal object SysUiBatteryRuntime {
    private var hooks = 0

    val installedHookCount: Int
        @Synchronized get() = hooks

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onBatteryState: (StatusStateStore.BatteryState) -> Unit,
        onChargingIconResource: (Int) -> Boolean,
        onEvent: ((String) -> Unit)?,
    ): Int {
        hooks =
            SysUiBatterySource.install(
                module = module,
                classLoader = classLoader,
                onBatteryState = onBatteryState,
                onChargingIconResource = onChargingIconResource,
                onEvent = onEvent,
            ).size
        return hooks
    }

    @Synchronized
    fun resetRuntimeState() {
        hooks = 0
        SysUiBatterySource.resetRuntimeState()
    }
}
