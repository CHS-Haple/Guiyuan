package com.chaners.guiyuan.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.util.WeakHashMap

internal object SysUiSceneSource {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val UPDATE_STATE_METHOD_NAME = "updateState"
    const val STATUS_BAR_STATE_FIELD_NAME = "mStatusBarState"
    const val HOOK_COUNT = 1

    private const val HOOK_ID = "combinedstatus.scene.battery.updateState"

    // Values must not retain the weak View keys.
    private val states = WeakHashMap<View, Int>()

    @Volatile
    private var statusBarStateField: Field? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onSceneState: (SceneUpdate) -> Unit,
        onEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
    ): List<HookHandle> {
        val batteryClass =
            Class.forName(BATTERY_VIEW_CLASS_NAME, false, classLoader)
        val stateField =
            batteryClass.getDeclaredField(STATUS_BAR_STATE_FIELD_NAME)
                .apply { isAccessible = true }
        val updateStateMethod =
            batteryClass.getDeclaredMethod(
                UPDATE_STATE_METHOD_NAME,
                Int::class.javaPrimitiveType,
            ).apply { isAccessible = true }

        val handle =
            module
                .hook(updateStateMethod)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        val sourceView = chain.thisObject as? View
                            ?: return@Hooker result
                        val rawState = (chain.getArg(0) as? Number)?.toInt()
                            ?: return@Hooker result
                        publish(
                            sourceView = sourceView,
                            rawState = rawState,
                            source = "updateState",
                            onSceneState = onSceneState,
                            onEvent = onEvent,
                            isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                        )
                        result
                    },
                )
        statusBarStateField = stateField

        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID

    fun steadySourceScene(update: SceneUpdate): SourceScene {
        val structural = steadySourceScene(update.sourceView)
        return when (structural) {
            SourceScene.HOME ->
                if (update.surface == Surface.UNLOCKED_STATUS_BAR) {
                    SourceScene.HOME
                } else {
                    SourceScene.UNKNOWN
                }

            SourceScene.KEYGUARD ->
                if (
                    update.surface == Surface.KEYGUARD ||
                    update.surface == Surface.SHADE_LOCKED
                ) {
                    SourceScene.KEYGUARD
                } else {
                    SourceScene.UNKNOWN
                }

            SourceScene.UNKNOWN -> SourceScene.UNKNOWN
        }
    }

    fun steadySourceScene(sourceView: View): SourceScene {
        var current: View? = sourceView
        while (current != null) {
            val scene = classifyHost(current.javaClass.name)
            if (scene != SourceScene.UNKNOWN) return scene
            current = current.parent as? View
        }
        return SourceScene.UNKNOWN
    }

    internal fun classifyHost(className: String): SourceScene =
        when (className) {
            StatusBarHostCapture.HOST_CLASS_NAME -> SourceScene.HOME
            KEYGUARD_HOST_CLASS_NAME -> SourceScene.KEYGUARD
            else -> SourceScene.UNKNOWN
        }

    @Synchronized
    fun currentSurface(sourceView: View): Surface? {
        val rawState =
            states[sourceView]
                ?: run {
                    val field = statusBarStateField ?: return null
                    runCatching { field.getInt(sourceView) }
                        .getOrNull()
                        ?.also { states[sourceView] = it }
                        ?: return null
                }
        return classifyRawState(rawState)
    }

    @Synchronized
    fun resetRuntimeState() {
        states.clear()
        statusBarStateField = null
    }

    internal fun classifyRawState(rawState: Int): Surface =
        when (rawState) {
            STATUS_BAR_STATE_SHADE -> Surface.UNLOCKED_STATUS_BAR
            STATUS_BAR_STATE_KEYGUARD -> Surface.KEYGUARD
            STATUS_BAR_STATE_SHADE_LOCKED -> Surface.SHADE_LOCKED
            else -> Surface.UNKNOWN
        }

    private fun publish(
        sourceView: View,
        rawState: Int,
        source: String,
        onSceneState: (SceneUpdate) -> Unit,
        onEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
    ) {
        synchronized(this) {
            if (states[sourceView] == rawState) {
                return
            }
            states[sourceView] = rawState
        }
        val update =
            SceneUpdate(
                sourceView = sourceView,
                surface = classifyRawState(rawState),
                rawState = rawState,
            )
        onSceneState(update)
        if (onEvent != null && isDetailedDiagnosticsEnabled()) {
            onEvent(
                "sceneState source=" + source +
                    " raw=" + rawState +
                    " batteryState=" + update.surface.name +
                    " authority=battery-status-state-readonly" +
                    " homeVisibilityAuthority=host+panel-coordinator",
            )
        }
    }

    internal enum class Surface {
        UNLOCKED_STATUS_BAR,
        KEYGUARD,
        SHADE_LOCKED,
        UNKNOWN,
    }

    internal data class SceneUpdate(
        val sourceView: View,
        val surface: Surface,
        val rawState: Int,
    )

    private const val KEYGUARD_HOST_CLASS_NAME =
        "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView"

    private const val STATUS_BAR_STATE_SHADE = 0
    private const val STATUS_BAR_STATE_KEYGUARD = 1
    private const val STATUS_BAR_STATE_SHADE_LOCKED = 2
}
