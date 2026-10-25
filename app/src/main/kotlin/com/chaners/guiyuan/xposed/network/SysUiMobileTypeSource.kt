package com.chaners.guiyuan.xposed.network

import android.graphics.drawable.Drawable
import android.util.Log
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.util.concurrent.atomic.AtomicBoolean

internal object SysUiMobileTypeSource {
    const val CLASS_NAME = "com.miui.systemui.statusbar.views.MobileTypeDrawable"
    const val METHOD_NAME = "measure"
    const val HOOK_COUNT = 1
    private const val HOOK_ID = "combinedstatus.mobileType.measure"

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onChanged: (Drawable) -> Unit,
    ): List<HookHandle> {
        val clazz = Class.forName(CLASS_NAME, false, classLoader)
        val method =
            clazz.getDeclaredMethod(METHOD_NAME)
                .apply { isAccessible = true }

        val warned = AtomicBoolean(false)
        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        try {
                            (chain.thisObject as? Drawable)?.let(onChanged)
                        } catch (error: Throwable) {
                            if (error is VirtualMachineError || error is ThreadDeath) throw error
                            if (warned.compareAndSet(false, true)) {
                                runCatching {
                                    Log.w("Guiyuan[MobileType]", "Observation failed", error)
                                }
                            }
                        }
                        chain.proceed()
                    },
                )
        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID
}
