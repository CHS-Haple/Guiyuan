package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object StatusBarHostCapture {
    const val HOST_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiNotificationStatusContainer"
    const val HOST_READY_METHOD_NAME = "onFinishInflate"
    const val HOOK_ID = "combinedstatus.statusHost.onFinishInflate"

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onCaptured: (SysUiHostRegistry.Capture) -> Unit,
    ): HookHandle {
        val hostClass = Class.forName(HOST_CLASS_NAME, false, classLoader)
        val hostReadyMethod = hostClass.getDeclaredMethod(HOST_READY_METHOD_NAME)

        return module
            .hook(hostReadyMethod)
            .setId(HOOK_ID)
            .intercept(hooker(onCaptured))
    }

    fun replace(
        handle: HookHandle,
        onCaptured: (SysUiHostRegistry.Capture) -> Unit,
    ): HookHandle = handle.replaceHook(hooker(onCaptured))

    fun matches(handle: HookHandle): Boolean {
        if (handle.id == HOOK_ID) {
            return true
        }

        val executable = handle.executable
        return executable.declaringClass.name == HOST_CLASS_NAME &&
            executable.name == HOST_READY_METHOD_NAME &&
            executable.parameterCount == 0
    }

    private fun hooker(
        onCaptured: (SysUiHostRegistry.Capture) -> Unit,
    ): Hooker = Hooker { chain ->
        val result = chain.proceed()

        chain.thisObject?.let { host ->
            SysUiHostRegistry.capture(host)?.let(onCaptured)
        }

        result
    }
}
