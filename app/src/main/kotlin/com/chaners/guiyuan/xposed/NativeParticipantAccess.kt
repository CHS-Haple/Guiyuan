package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import android.view.View
import android.view.ViewGroup

internal object NativeParticipantAccess {
    const val PHONE_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"
    private const val DEPENDENCY =
        "com.android.systemui.Dependency"
    private val STATUS_BAR_ICON_CONTROLLER_CANDIDATES =
        listOf(
            "com.android.systemui.statusbar.phone.ui.StatusBarIconController",
            "com.android.systemui.statusbar.phone.StatusBarIconController",
        )

    private fun phoneStatusBarViewFor(host: View): View? =
        generateSequence(host) { view -> view.parent as? View }
            .firstOrNull { view -> view.javaClass.name == PHONE_STATUS_BAR_VIEW }

    fun managerFor(host: Any): Any? {
        val hostView = host as? View ?: return null
        val statusBarView = phoneStatusBarViewFor(hostView) ?: return null
        return statusBarView.readField("mDarkIconManager")
    }

    fun groupFor(host: Any): ViewGroup? =
        managerFor(host)?.readField("mGroup") as? ViewGroup

    fun resolve(host: Any): ResolveResult {
        val hostView = host as? View
            ?: return ResolveResult.Failure("host-not-view")

        val statusBarView =
            phoneStatusBarViewFor(hostView)
                ?: return ResolveResult.Failure("phone-status-bar-view-missing")

        val classLoader = statusBarView.javaClass.classLoader
            ?: return ResolveResult.Failure("systemui-classloader-missing")
        val manager = statusBarView.readField("mDarkIconManager")
            ?: return ResolveResult.Failure("dark-icon-manager-missing")
        val group = manager.readField("mGroup") as? ViewGroup
            ?: return ResolveResult.Failure("status-icon-group-missing")
        val controller =
            resolveStatusBarIconController(
                classLoader = classLoader,
                manager = manager,
            ) ?: return ResolveResult.Failure("status-icon-controller-missing")

        return ResolveResult.Ready(
            Handles(
                manager = manager,
                group = group,
                controller = controller,
            ),
        )
    }

    private fun resolveStatusBarIconController(
        classLoader: ClassLoader,
        manager: Any,
    ): Any? =
        dependencyStatusBarIconController(classLoader)
            ?: manager.readField("mController")

    private fun dependencyStatusBarIconController(classLoader: ClassLoader): Any? {
        val dependencyClass = classOrNull(DEPENDENCY, classLoader) ?: return null
        val getMethod =
            optionalNativeRead {
                dependencyClass
                    .allMethods()
                    .firstOrNull { method ->
                        method.name == "get" &&
                            java.lang.reflect.Modifier.isStatic(method.modifiers) &&
                            method.parameterTypes.contentEquals(
                                arrayOf<Class<*>>(Class::class.java),
                            )
                    }?.apply { isAccessible = true }
            } ?: return null

        for (className in STATUS_BAR_ICON_CONTROLLER_CANDIDATES) {
            val controllerType = classOrNull(className, classLoader) ?: continue
            val controller = optionalNativeRead { getMethod.invoke(null, controllerType) }
            if (controller != null) return controller
        }
        return null
    }

    private fun classOrNull(
        name: String,
        classLoader: ClassLoader,
    ): Class<*>? =
        optionalNativeRead { Class.forName(name, false, classLoader) }

    internal data class Handles(
        val manager: Any,
        val group: ViewGroup,
        val controller: Any,
    )

    internal sealed interface ResolveResult {
        data class Ready(
            val handles: Handles,
        ) : ResolveResult

        data class Failure(
            val reason: String,
        ) : ResolveResult
    }


}
