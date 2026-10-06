package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SysUiIslandSource {
    const val HOOK_COUNT = 1

    private const val LISTENER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector\$islandListener\$1"
    private const val STATUS_METHOD_NAME = "onIslandStatusChanged"
    private const val HOOK_ID = "combinedstatus.island.home.status"
    @Volatile
    private var islandShowing: Boolean? = null
    private var lastDiagnosticShowing: Boolean? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { true },
    ): List<HookHandle> {
        val listenerClass = Class.forName(LISTENER_CLASS_NAME, false, classLoader)
        val method =
            listenerClass.getDeclaredMethod(
                STATUS_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val showing = chain.getArg(0) as? Boolean ?: false
                        val secondary = chain.getArg(1) as? Boolean ?: false
                        val animate = chain.getArg(2) as? Boolean ?: false
                        val result = chain.proceed()
                        synchronized(this) {
                            islandShowing = showing
                        }
                        if (onEvent == null || !isProbeEnabled()) {
                            return@Hooker result
                        }
                        val shouldReport =
                            synchronized(this) {
                                val changed = lastDiagnosticShowing != showing
                                if (changed) {
                                    lastDiagnosticShowing = showing
                                }
                                changed
                            }
                        if (shouldReport) {
                            onEvent(
                                "islandOwner event showing=" + showing +
                                    " secondary=" + secondary +
                                    " animate=" + animate +
                                    " geometryWrites=0",
                            )
                        }
                        result
                    },
                )
        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID

    // Observation only; SystemUI still owns island motion.
    @Synchronized
    fun currentIslandShowing(): Boolean? = islandShowing

    fun resetRuntimeState() {
        synchronized(this) {
            islandShowing = null
            lastDiagnosticShowing = null
        }
    }

}
