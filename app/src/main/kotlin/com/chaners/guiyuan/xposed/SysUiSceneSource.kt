package com.chaners.guiyuan.xposed

import android.view.View
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal object SysUiSceneSource {

    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val UPDATE_STATE_METHOD_NAME = "updateState"
    const val STATUS_BAR_STATE_FIELD_NAME = "mStatusBarState"
    const val HOOK_COUNT = 1

    private const val HOOK_ID = "combinedstatus.scene.battery.updateState"

    // Values must not retain the weak View keys.
    private val states = WeakHashMap<View, Int>()
    // A native state read is not proof that its owner callback completed.
    private class Delivery(val state: Int)
    private val deliveredStates = WeakHashMap<View, Delivery>()

    @Volatile
    private var statusBarStateField: Field? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onSceneState: (SceneUpdate) -> Unit,
        onRuntimeFailure: (Throwable) -> Unit,
        diagnostics: HookDiagnostics,
    ): List<HookHandle> {
        val onEvent = diagnostics.onEvent
        val isDetailedDiagnosticsEnabled = diagnostics.isDetailedDiagnosticsEnabled
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

        val warned = AtomicBoolean(false)
        val onDiagnostic: ((SceneUpdate) -> Unit)? = onEvent?.let { sink ->
            { update ->
                SysUiPresentationRuntime.emitPassiveDiagnostic(warned, "Guiyuan[Scene]") {
                    if (isDetailedDiagnosticsEnabled()) {
                        sink(
                            "sceneState source=updateState" +
                                " raw=" + update.rawState +
                                " batteryState=" + update.surface.name +
                                " authority=battery-status-state-readonly" +
                                " homeVisibilityAuthority=host+panel-coordinator",
                        )
                    }
                }
            }
        }
        val handle =
            module
                .hook(updateStateMethod)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        val sourceView = optionalNativeRead { chain.thisObject as? View }
                        if (sourceView != null) {
                            val rawState = optionalNativeRead { (chain.getArg(0) as? Number)?.toInt() }
                            if (rawState != null) {
                                publish(
                                    sourceView = sourceView,
                                    rawState = rawState,
                                    onSceneState = onSceneState,
                                    onRuntimeFailure = onRuntimeFailure,
                                    onDiagnostic = onDiagnostic,
                                )
                            }
                        }
                        result
                    },
                )
        statusBarStateField = stateField

        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID

    fun steadySourceScene(update: SceneUpdate): SourceScene =
        SceneClassificationPolicy.steadySourceScene(steadySourceScene(update.sourceView), update.surface)

    // A new generation must re-read native state even if updateState never fires.
    // Do not turn the read into a delivered hook callback or infer scene from ancestry alone.
    fun currentSteadySourceScene(sourceView: View): SourceScene =
        if (sourceView.isAttachedToWindow) {
            SceneClassificationPolicy.steadySourceScene(steadySourceScene(sourceView), currentSurface(sourceView))
        } else {
            SourceScene.UNKNOWN
        }

    fun steadySourceScene(sourceView: View): SourceScene {
        var current: View? = sourceView
        while (current != null) {
            val scene = SceneClassificationPolicy.classifyHost(current.javaClass.name)
            if (scene != SourceScene.UNKNOWN) return scene
            current = current.parent as? View
        }
        return SourceScene.UNKNOWN
    }

    @Synchronized
    fun currentSurface(sourceView: View): Surface? {
        val rawState =
            states[sourceView]
                ?: run {
                    val field = statusBarStateField ?: return null
                    optionalNativeRead { field.getInt(sourceView) }
                        ?.also { states[sourceView] = it }
                        ?: return null
                }
        return SceneClassificationPolicy.classifyRawState(rawState)
    }

    @Synchronized
    fun resetRuntimeState() {
        states.clear()
        deliveredStates.clear()
        statusBarStateField = null
    }

    @Suppress("TooGenericExceptionCaught")
    private fun publish(
        sourceView: View,
        rawState: Int,
        onSceneState: (SceneUpdate) -> Unit,
        onRuntimeFailure: (Throwable) -> Unit,
        onDiagnostic: ((SceneUpdate) -> Unit)?,
    ) {
        val delivery =
            synchronized(this) {
                states[sourceView] = rawState
                if (deliveredStates[sourceView]?.state == rawState) {
                    null
                } else {
                    Delivery(rawState).also { deliveredStates[sourceView] = it }
                }
            } ?: return
        val update =
            SceneUpdate(
                sourceView = sourceView,
                surface = SceneClassificationPolicy.classifyRawState(rawState),
                rawState = rawState,
            )
        try {
            onSceneState(update)
        } catch (error: Throwable) {
            SysUiPresentationRuntime.throwIfFatal(error)
            try {
                onRuntimeFailure(error)
            } catch (failure: Throwable) {
                SysUiPresentationRuntime.throwIfFatal(failure)
                if (failure !== error) error.addSuppressed(failure)
            } finally {
                // Keep the failed delivery marked during owner cleanup to avoid reentrant retries.
                synchronized(this) {
                    if (deliveredStates[sourceView] === delivery) {
                        deliveredStates.remove(sourceView)
                    }
                }
            }
            return
        }
        onDiagnostic?.invoke(update)
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
}
