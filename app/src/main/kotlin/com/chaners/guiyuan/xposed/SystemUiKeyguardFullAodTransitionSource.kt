package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SystemUiKeyguardFullAodTransitionSource {
    const val HOOK_COUNT = 1

    private const val CONTROLLER_CLASS =
        "com.android.systemui.statusbar.phone.KeyguardStatusBarViewControllerInject"
    private const val ANIMATE_FULL_AOD_METHOD = "animateFullAod"
    private const val HOOK_ID =
        "combinedstatus.keyguardAod.animateFullAod"

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onTransitionStarted: () -> Unit,
        onTransitionCommitted: () -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val controllerClass = Class.forName(CONTROLLER_CLASS, false, classLoader)
        val candidates =
            controllerClass.declaredMethods.filter { method ->
                method.name == ANIMATE_FULL_AOD_METHOD &&
                    matchesAnimateFullAodSignature(
                        parameterTypes = method.parameterTypes,
                        returnType = method.returnType,
                    )
            }
        val method =
            candidates.singleOrNull()
                ?.apply { isAccessible = true }
                ?: error(
                    "full-aod-animation-method-contract-" +
                        if (candidates.isEmpty()) "missing" else "ambiguous",
                )

        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val rawArg0 = chain.getArg(0) as? Boolean
                        val rawArg1 = chain.getArg(1) as? Boolean

                        // Open the native transition scope before HyperOS runs.
                        // animateIconContainer may be invoked inside proceed(),
                        // so waiting until animateFullAod returns would miss the
                        // actual status-icon visual lifecycle event.
                        onTransitionStarted()
                        val result = chain.proceed()

                        // Direction is read from mToLockScreen only after native
                        // code returns. Raw arguments remain diagnostics only.
                        onTransitionCommitted()
                        onEvent?.invoke(
                            "keyguardFullAod source=animateFullAod" +
                                " arg0=" + (rawArg0 ?: "unavailable") +
                                " arg1=" + (rawArg1 ?: "unavailable") +
                                " eventDriven=true readOnly=true nativeGeometryWrites=0",
                        )
                        result
                    },
                )
        return listOf(handle)
    }

    internal fun matchesAnimateFullAodSignature(
        parameterTypes: Array<Class<*>>,
        returnType: Class<*>,
    ): Boolean =
        parameterTypes.size == 2 &&
            isBooleanType(parameterTypes[0]) &&
            isBooleanType(parameterTypes[1]) &&
            returnType == Void.TYPE

    private fun isBooleanType(type: Class<*>): Boolean =
        type == Boolean::class.javaPrimitiveType ||
            type == Boolean::class.javaObjectType
}
