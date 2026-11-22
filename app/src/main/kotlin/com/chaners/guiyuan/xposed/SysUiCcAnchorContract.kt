package com.chaners.guiyuan.xposed

import android.view.ViewGroup
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import java.lang.reflect.Field
import java.lang.reflect.Method

// One verified native Header/combined-root reflection contract, resolved per install.
// Does not register hooks, cache Views globally or write CC scene/presentation state.
internal class SysUiCcAnchorContract private constructor(
    private val callbackClass: Class<*>,
    private val callbacksField: Field,
    private val callbackOuterField: Field,
    private val addBatteryIslandField: Field,
    private val realSystemIconsField: Field,
    private val headerControllerField: Field,
    private val lazyGetMethod: Method,
    private val fakeStatusBarField: Field,
    private val statusBarField: Field,
) {
    fun resolveHeader(delegate: Any): Any? {
        val callbacks =
            optionalNativeRead { callbacksField.get(delegate) as? Iterable<*> }
                ?: return null
        val callback =
            callbacks.firstOrNull { candidate ->
                candidate != null && callbackClass.isInstance(candidate)
            } ?: return null
        return optionalNativeRead { callbackOuterField.get(callback) }
    }

    fun realSystemIcons(header: Any): ViewGroup? =
        optionalNativeRead { realSystemIconsField.get(header) as? ViewGroup }

    fun fakePresentationRoot(header: Any): ViewGroup? =
        fakeStatusBar(header)

    fun transitionEndpoints(
        header: Any,
    ): SysUiCcSource.TransitionEndpoints? {
        // Use the same native combined-header witness for both endpoints.
        val combined = combinedHeader(header) ?: return null
        val fakeRoot =
            optionalNativeRead { fakeStatusBarField.get(combined) as? ViewGroup }
                ?: return null
        val finalRoot =
            optionalNativeRead { statusBarField.get(combined) as? ViewGroup }
                ?: return null
        return SysUiCcSource.TransitionEndpoints(
            fakeRoot = fakeRoot,
            finalRoot = finalRoot,
        )
    }

    fun batteryIslandActive(header: Any): Boolean? =
        readBoolean(addBatteryIslandField, header)

    fun headerFromCallback(callback: Any?): Any? =
        callback
            ?.let { candidate ->
                optionalNativeRead { callbackOuterField.get(candidate) }
            }

    private fun combinedHeader(header: Any): Any? {
        val lazy =
            optionalNativeRead { headerControllerField.get(header) }
                ?: return null
        return optionalNativeRead { lazyGetMethod.invoke(lazy) }
    }

    private fun fakeStatusBar(header: Any): ViewGroup? {
        val combinedHeader = combinedHeader(header) ?: return null
        return optionalNativeRead {
            fakeStatusBarField.get(combinedHeader) as? ViewGroup
        }
    }

    private fun readBoolean(field: Field, target: Any): Boolean? =
        optionalNativeRead { field.getBoolean(target) }

    companion object {
        const val CONTROL_CENTER_HEADER_CALLBACK_CLASS =
            "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController\$controlCenterCallback\$1"
        private const val CONTROL_CENTER_HEADER_CLASS =
            "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController"
        private const val COMBINED_HEADER_CLASS =
            "com.android.systemui.controlcenter.shade.CombinedHeaderController"
        private const val DAGGER_LAZY_CLASS = "dagger.Lazy"

        fun resolve(
            classLoader: ClassLoader,
            delegateClass: Class<*>,
        ): SysUiCcAnchorContract? =
            optionalNativeRead {
                val callbackClass =
                    Class.forName(
                        CONTROL_CENTER_HEADER_CALLBACK_CLASS,
                        false,
                        classLoader,
                    )
                val headerClass =
                    Class.forName(
                        CONTROL_CENTER_HEADER_CLASS,
                        false,
                        classLoader,
                    )
                val combinedHeaderClass =
                    Class.forName(
                        COMBINED_HEADER_CLASS,
                        false,
                        classLoader,
                    )
                val lazyClass =
                    Class.forName(
                        DAGGER_LAZY_CLASS,
                        false,
                        classLoader,
                    )
                SysUiCcAnchorContract(
                    callbackClass = callbackClass,
                    callbacksField =
                        delegateClass.getDeclaredField("callbacks").accessible(),
                    callbackOuterField =
                        callbackClass.getDeclaredField("this\$0").accessible(),
                    addBatteryIslandField =
                        headerClass.getDeclaredField("isAddBatteryIsland").accessible(),
                    realSystemIconsField =
                        headerClass.getDeclaredField("realSystemIcons").accessible(),
                    headerControllerField =
                        headerClass.getDeclaredField("headerController").accessible(),
                    lazyGetMethod =
                        lazyClass.getDeclaredMethod("get").apply {
                            isAccessible = true
                        },
                    fakeStatusBarField =
                        combinedHeaderClass
                            .getDeclaredField("controlCenterFakeStatusBar")
                            .accessible(),
                    statusBarField =
                        combinedHeaderClass
                            .getDeclaredField("controlCenterStatusBar")
                            .accessible(),
                )
            }
    }
}


private fun Field.accessible(): Field =
    apply { isAccessible = true }
