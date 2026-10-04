package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SystemUiKeyguardStatusIconTransitionSource {
    const val HOOK_COUNT = 1

    private const val KEYGUARD_VIEW_CLASS =
        "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView"
    private const val ANIMATE_ICON_CONTAINER_METHOD = "animateIconContainer"
    private const val HOOK_ID =
        "combinedstatus.keyguardAod.animateIconContainer"

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onTransition: () -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val viewClass = Class.forName(KEYGUARD_VIEW_CLASS, false, classLoader)
        val candidates =
            viewClass.declaredMethods.filter { method ->
                method.name == ANIMATE_ICON_CONTAINER_METHOD &&
                    matchesAnimateIconContainerSignature(
                        parameterTypes = method.parameterTypes,
                        returnType = method.returnType,
                    )
            }
        val method =
            candidates.singleOrNull()
                ?.apply { isAccessible = true }
                ?: error(
                    "keyguard-status-icon-animation-contract-" +
                        if (candidates.isEmpty()) "missing" else "ambiguous",
                )

        return listOf(
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val rawArg0 = chain.getArg(0) as? Boolean
                        val result = chain.proceed()
                        onTransition()
                        onEvent?.invoke(
                            "keyguardStatusIconTransition source=animateIconContainer" +
                                " arg0=" + (rawArg0 ?: "unavailable") +
                                " eventDriven=true readOnly=true nativeGeometryWrites=0",
                        )
                        result
                    },
                ),
        )
    }

    internal fun matchesAnimateIconContainerSignature(
        parameterTypes: Array<Class<*>>,
        returnType: Class<*>,
    ): Boolean =
        parameterTypes.size == 1 &&
            (parameterTypes[0] == Boolean::class.javaPrimitiveType ||
                parameterTypes[0] == Boolean::class.javaObjectType) &&
            returnType == Void.TYPE
}
