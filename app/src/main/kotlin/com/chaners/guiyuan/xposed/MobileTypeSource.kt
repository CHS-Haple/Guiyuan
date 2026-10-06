package com.chaners.guiyuan.xposed

import android.graphics.drawable.Drawable
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object MobileTypeSource {
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

        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        (chain.thisObject as? Drawable)?.let(onChanged)
                        chain.proceed()
                    },
                )
        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID
}
