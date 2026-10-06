package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedModule

internal object SysUiHostHook {
    @Volatile
    private var ready = false

    // Hook readiness is separate from whether a live host exists.
    val isReady: Boolean
        get() = ready

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onCaptured: (SysUiHostRegistry.Capture) -> Unit,
    ): HookHandle =
        StatusBarHostCapture.install(
            module = module,
            classLoader = classLoader,
            onCaptured = onCaptured,
        ).also { ready = true }

    // Hot Reload reuses this hook instead of creating a second capture path.
    fun findOwnedHandle(handles: List<HookHandle>): HookHandle? =
        handles.firstOrNull(StatusBarHostCapture::matches)

    fun replace(
        handle: HookHandle,
        onCaptured: (SysUiHostRegistry.Capture) -> Unit,
    ): HookHandle =
        StatusBarHostCapture.replace(
            handle = handle,
            onCaptured = onCaptured,
        ).also { ready = true }

    fun markUnavailable() {
        ready = false
    }
}
