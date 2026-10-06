package com.chaners.guiyuan.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.util.WeakHashMap

internal object SceneSource {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val UPDATE_STATE_METHOD_NAME = "updateState"
    const val STATUS_BAR_STATE_FIELD_NAME = "mStatusBarState"
    const val HOOK_COUNT = 1

    private const val HOOK_ID = "combinedstatus.scene.battery.updateState"

    private val states = WeakHashMap<View, SceneUpdate>()

    @Volatile
    private var statusBarStateField: Field? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onSceneState: (SceneUpdate) -> Unit,
        onEvent: ((String) -> Unit)?,
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

        statusBarStateField = stateField

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
                        )
                        result
                    },
                )

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
            when (current.javaClass.name) {
                KEYGUARD_HOST_CLASS_NAME -> return SourceScene.KEYGUARD
                HOME_HOST_CLASS_NAME -> return SourceScene.HOME
            }
            current = current.parent as? View
        }
        return SourceScene.UNKNOWN
    }

    internal fun classifySteadySourceAncestors(
        classNames: List<String>,
    ): SourceScene =
        when {
            KEYGUARD_HOST_CLASS_NAME in classNames -> SourceScene.KEYGUARD
            HOME_HOST_CLASS_NAME in classNames -> SourceScene.HOME
            else -> SourceScene.UNKNOWN
        }

    @Synchronized
    fun currentState(sourceView: View): SceneUpdate? {
        states[sourceView]?.let { return it }

        val field = statusBarStateField ?: return null
        val rawState =
            runCatching { field.getInt(sourceView) }
                .getOrNull()
                ?: return null
        return SceneUpdate(
            sourceView = sourceView,
            surface = classifyRawState(rawState),
            rawState = rawState,
        ).also { states[sourceView] = it }
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
    ) {
        val update =
            SceneUpdate(
                sourceView = sourceView,
                surface = classifyRawState(rawState),
                rawState = rawState,
            )
        val changed =
            synchronized(this) {
                states.put(sourceView, update) != update
            }
        if (!changed) {
            return
        }

        onSceneState(update)
        onEvent?.invoke(
            "sceneState source=" + source +
                " raw=" + rawState +
                " batteryState=" + update.surface.name +
                " authority=battery-status-state-readonly" +
                " homeVisibilityAuthority=host+panel-coordinator" +
                " geometryWrites=0",
        )
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

    private const val HOME_HOST_CLASS_NAME =
        "com.android.systemui.statusbar.phone.MiuiNotificationStatusContainer"
    private const val KEYGUARD_HOST_CLASS_NAME =
        "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView"

    private const val STATUS_BAR_STATE_SHADE = 0
    private const val STATUS_BAR_STATE_KEYGUARD = 1
    private const val STATUS_BAR_STATE_SHADE_LOCKED = 2
}
