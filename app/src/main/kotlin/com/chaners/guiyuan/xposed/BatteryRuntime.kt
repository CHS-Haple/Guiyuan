package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedModule

internal object BatteryRuntime {
    private var current: AttachResult? = null

    val installedHookCount: Int
        @Synchronized get() = current?.hooks ?: 0

    internal data class AttachResult(
        val hooks: Int,
    ) {
        val ready: Boolean
            get() = hooks == BatterySource.HOOK_COUNT
    }

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onBatteryState: (StatusStateStore.BatteryState) -> Unit,
        onChargingIconResource: (Int?) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): AttachResult =
        AttachResult(
            hooks =
                BatterySource.install(
                    module = module,
                    classLoader = classLoader,
                    onBatteryState = onBatteryState,
                    onChargingIconResource = onChargingIconResource,
                    onEvent = onEvent,
                ).size,
        ).also { current = it }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        BatterySource.resetRuntimeState()
    }
}
