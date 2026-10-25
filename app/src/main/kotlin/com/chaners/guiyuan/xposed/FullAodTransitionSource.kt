package com.chaners.guiyuan.xposed

import android.util.Log
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.util.concurrent.atomic.AtomicBoolean

internal object FullAodTransitionSource {
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
        onRuntimeFailure: (String, Throwable) -> Unit,
        onEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
    ): List<HookHandle> {
        val controllerClass = Class.forName(CONTROLLER_CLASS, false, classLoader)
        val candidates =
            controllerClass.declaredMethods.filter { method ->
                method.name == ANIMATE_FULL_AOD_METHOD &&
                    matchesSignature(
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

        val warned = AtomicBoolean(false)
        fun fallback(stage: String, error: Throwable) {
            if (error is VirtualMachineError || error is ThreadDeath) throw error
            try {
                onRuntimeFailure(stage, error)
            } catch (failure: Throwable) {
                if (failure is VirtualMachineError || failure is ThreadDeath) throw failure
            }
        }
        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        // Open the native transition scope before HyperOS runs.
                        // animateIconContainer may be invoked inside proceed(),
                        // so waiting until animateFullAod returns would miss the
                        // actual status-icon visual lifecycle event.
                        var startFailure: Throwable? = null
                        try {
                            onTransitionStarted()
                        } catch (error: Throwable) {
                            if (error is VirtualMachineError || error is ThreadDeath) throw error
                            startFailure = error
                        }
                        val result =
                            try {
                                chain.proceed()
                            } catch (error: Throwable) {
                                if (error !is VirtualMachineError && error !is ThreadDeath) {
                                    fallback("full-aod-native", error)
                                }
                                throw error
                            }

                        // Do not commit a start that did not complete.
                        if (startFailure != null) {
                            fallback("full-aod-start", startFailure)
                        } else {
                            try {
                                // Native mToLockScreen is readable after proceed.
                                onTransitionCommitted()
                            } catch (error: Throwable) {
                                fallback("full-aod-commit", error)
                            }
                        }
                        try {
                            if (onEvent != null && isDetailedDiagnosticsEnabled()) {
                                val rawArg0 = chain.getArg(0) as? Boolean
                                val rawArg1 = chain.getArg(1) as? Boolean
                                onEvent(
                                    "keyguardFullAod source=animateFullAod" +
                                        " arg0=" + (rawArg0 ?: "unavailable") +
                                        " arg1=" + (rawArg1 ?: "unavailable"),
                                )
                            }
                        } catch (error: Throwable) {
                            if (error is VirtualMachineError || error is ThreadDeath) throw error
                            if (warned.compareAndSet(false, true)) {
                                try {
                                    Log.w("Guiyuan[FullAod]", "Diagnostic failed", error)
                                } catch (failure: Throwable) {
                                    if (failure is VirtualMachineError || failure is ThreadDeath) throw failure
                                }
                            }
                        }
                        result
                    },
                )
        return listOf(handle)
    }

    internal fun matchesSignature(
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
