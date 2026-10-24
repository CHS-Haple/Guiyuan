package com.chaners.guiyuan.xposed

import android.util.Log
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.util.concurrent.atomic.AtomicBoolean

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
        val warned = AtomicBoolean(false)
        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        try {
                            val showing = chain.getArg(0) as? Boolean ?: false
                            synchronized(this) {
                                islandShowing = showing
                            }
                            if (onEvent != null && isProbeEnabled()) {
                                if (synchronized(this) { lastDiagnosticShowing != showing }) {
                                    val secondary = chain.getArg(1) as? Boolean ?: false
                                    val animate = chain.getArg(2) as? Boolean ?: false
                                    onEvent(
                                        "islandOwner event showing=" + showing +
                                            " secondary=" + secondary +
                                            " animate=" + animate,
                                    )
                                    synchronized(this) { lastDiagnosticShowing = showing }
                                }
                            }
                        } catch (error: Throwable) {
                            if (error is VirtualMachineError || error is ThreadDeath) throw error
                            if (warned.compareAndSet(false, true)) {
                                runCatching {
                                    Log.w("Guiyuan[Island]", "Observation failed", error)
                                }
                            }
                        }
                        result
                    },
                )
        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID

    // Observation only; SystemUI still owns island motion.
    @Synchronized
    fun currentShowing(): Boolean? = islandShowing

    fun resetRuntimeState() {
        synchronized(this) {
            islandShowing = null
            lastDiagnosticShowing = null
        }
    }
}
