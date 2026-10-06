package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedModule

internal object HostRuntime {
    @Volatile
    private var ready = false

    val isReady: Boolean
        get() = ready

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onCaptured: (HostRegistry.Capture) -> Unit,
    ): HookHandle =
        StatusBarHostCapture.install(
            module = module,
            classLoader = classLoader,
            onCaptured = onCaptured,
        ).also { ready = true }

    fun findOwnedHandle(handles: List<HookHandle>): HookHandle? =
        handles.firstOrNull(StatusBarHostCapture::matches)

    fun replace(
        handle: HookHandle,
        onCaptured: (HostRegistry.Capture) -> Unit,
    ): HookHandle =
        StatusBarHostCapture.replace(
            handle = handle,
            onCaptured = onCaptured,
        ).also { ready = true }

    fun markUnavailable() {
        ready = false
    }
}
