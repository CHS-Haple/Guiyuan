package com.chaners.guiyuan.xposed

import android.graphics.Paint
import android.util.Log
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import com.chaners.guiyuan.settings.FontWeightAxis
import android.view.ViewGroup
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicBoolean

internal object SysUiBatterySource {
    const val BATTERY_ICON_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterIconView"
    const val BATTERY_METER_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    const val BATTERY_LEVEL_METHOD_NAME = "onBatteryLevelChanged"
    const val CHARGE_STATE_METHOD_NAME = "onChargeStateChanged"
    const val POWER_SAVE_METHOD_NAME = "onPowerSaveChanged"
    const val PERFORMANCE_METHOD_NAME = "onPerformanceModeChanged"
    const val MIUI_OPTIMIZATION_METHOD_NAME = "setMiuiOptimizationEnabled"
    const val UPDATE_CHARGE_AND_TEXT_METHOD_NAME = "updateChargeAndText"
    const val HOOK_COUNT = 6

    private const val LEVEL_HOOK_ID = "combinedstatus.battery.level"
    private const val CHARGE_HOOK_ID = "combinedstatus.battery.charge"
    private const val POWER_SAVE_HOOK_ID = "combinedstatus.battery.power-save"
    private const val PERFORMANCE_HOOK_ID = "combinedstatus.battery.performance"
    private const val MIUI_OPTIMIZATION_HOOK_ID = "combinedstatus.battery.miui-optimization"
    private const val CHARGING_GLYPH_HOOK_ID = "combinedstatus.battery.charging-glyph"

    @Volatile
    private var hooksReady = false
    @Volatile
    private var failedInstallHandles: List<HookHandle> = emptyList()
    @Volatile
    private var lastState: StatusStateStore.BatteryState? = null

    @Volatile

    private var lastChargingIconResId: Int? = null

    internal fun rethrowFatal(error: Throwable) {
        val cause = (error as? InvocationTargetException)?.targetException ?: error
        if (cause is VirtualMachineError || cause is ThreadDeath) throw cause
    }

    private inline fun <T> readOrNull(block: () -> T): T? =
        try {
            block()
        } catch (error: Throwable) {
            rethrowFatal(error)
            null
        }

    internal data class VerifiedFont(
        val host: View,
        val face: Typeface,
        val axis: FontWeightAxis,
    )

    @Volatile
    private var verifiedFont: VerifiedFont? = null

    fun currentNumberFont(): VerifiedFont? =
        verifiedFont?.takeIf { it.host === SysUiHostRegistry.current() }

    fun setVerifiedNumberFont(host: View, face: Typeface?, axis: FontWeightAxis?) {
        verifiedFont =
            if (face != null && axis != null) VerifiedFont(host, face, axis) else null
    }

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onBatteryState: (StatusStateStore.BatteryState) -> Unit,
        onChargingIconResource: (Int) -> Unit,
        onRuntimeFailure: (String, Throwable) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        check(!hooksReady && failedInstallHandles.isEmpty()) { "battery-hooks-already-installed" }

        val iconClass =
            Class.forName(BATTERY_ICON_VIEW_CLASS_NAME, false, classLoader)
        val meterClass =
            Class.forName(BATTERY_METER_VIEW_CLASS_NAME, false, classLoader)
        val levelField = iconClass.requiredField("mLevel")
        val chargingField = iconClass.requiredField("mCharging")
        val progressStatusMethod =
            iconClass.getDeclaredMethod("getProgressStatus")
                .apply { isAccessible = true }
        val chargingColorField = iconClass.requiredField("mBatteryChargingColor")
        val powerSaveColorField = iconClass.requiredField("mBatteryPowerSaveColor")
        val superPowerSaveColorField =
            iconClass.optionalField(
                "mBatterySuperPowerSaveColor",
                "mBatterySuperSaveColor",
            )
        val performanceColorField =
            iconClass.requiredField("mBatteryPerformanceModeColor")
        val lowColorField = iconClass.requiredField("mBatteryLowColor")
        val miuiOptimizationField =
            iconClass.requiredField("mMiuiOptimizationEnabled")
        val chargingIconMethod =
            meterClass.getDeclaredMethod("getHollowChargingIconId")
                .apply { isAccessible = true }

        fun readChargingIconId(iconView: View): Int? {
            var parent = iconView.parent
            while (parent is View) {
                if (meterClass.isInstance(parent)) {
                    return readOrNull { chargingIconMethod.invoke(parent) as? Int }
                        ?.takeIf { it != 0 }
                }
                parent = parent.parent
            }
            return null
        }

        fun readState(iconView: View): StatusStateStore.BatteryState? {
            val level =
                readOrNull { levelField.getInt(iconView) }
                    ?.coerceIn(0, 100)
                    ?: return null
            val charging =
                readOrNull { chargingField.getBoolean(iconView) }
                    ?: return null
            val nativeStatusName =
                readOrNull {
                    (progressStatusMethod.invoke(iconView) as? Enum<*>)?.name
                }
            val semanticState =
                BatterySemanticState.fromNativeProgressStatus(
                    nativeStatusName,
                )
            val miuiOptimizationEnabled =
                readOrNull { miuiOptimizationField.getBoolean(iconView) }
                    ?: false
            val systemSemanticColor =
                if (miuiOptimizationEnabled) {
                    semanticState?.let { state ->
                        semanticColor(
                            icon = iconView,
                            state = state,
                            chargingColorField = chargingColorField,
                            powerSaveColorField = powerSaveColorField,
                            superPowerSaveColorField = superPowerSaveColorField,
                            performanceColorField = performanceColorField,
                            lowColorField = lowColorField,
                        )
                    }
                } else {
                    null
                }

            return StatusStateStore.BatteryState(
                percent = level,
                charging = charging,
                semanticState = semanticState,
                systemSemanticColor = systemSemanticColor,
                lowSystemColor =
                    if (miuiOptimizationEnabled) {
                        readOrNull { lowColorField.getInt(iconView) }
                    } else null,
                chargingIconResId =
                    if (charging) {
                        readChargingIconId(iconView) ?: lastChargingIconResId
                    } else {
                        null
                    },
            )
        }

        val warned = AtomicBoolean(false)
        fun fallback(source: String, error: Throwable) {
            rethrowFatal(error)
            try {
                onRuntimeFailure(source, error)
            } catch (failure: Throwable) {
                rethrowFatal(failure)
            }
        }

        fun report(message: () -> String) {
            val sink = onEvent ?: return
            try {
                sink(message())
            } catch (error: Throwable) {
                rethrowFatal(error)
                if (warned.compareAndSet(false, true)) {
                    readOrNull { Log.w("Guiyuan[Battery]", "Diagnostic failed", error) }
                }
            }
        }

        fun publish(
            iconView: View,
            sourceMethod: String,
        ) {
            val state = readState(iconView) ?: return
            val changed =
                synchronized(this) {
                    if (!state.charging) {
                        lastChargingIconResId = null
                    } else if (state.chargingIconResId != null) {
                        lastChargingIconResId = state.chargingIconResId
                    }
                    if (lastState == state) {
                        false
                    } else {
                        lastState = state
                        true
                    }
                }
            if (!changed) return
            try {
                onBatteryState(state)
            } catch (error: Throwable) {
                fallback("state", error)
                synchronized(this) {
                    if (lastState == state) lastState = null
                }
                return
            }
            report {
                "batteryState source=MiuiBatteryMeterIconView." + sourceMethod +
                    " percent=" + state.percent +
                    " charging=" + state.charging +
                    " semantic=" + (state.semanticState?.name ?: "unavailable") +
                    " systemColor=" +
                    (state.systemSemanticColor?.let(::colorHex) ?: "status-icon") +
                    " chargingIconId=" + (state.chargingIconResId ?: "unavailable") +
                    " semanticAuthority=MiuiBatteryMeterIconView.getProgressStatus()"
            }
        }

        fun publishChargingGlyph(
            meterView: View,
            sourceMethod: String,
        ) {
            val iconId =
                readOrNull { chargingIconMethod.invoke(meterView) as? Int }
                    ?.takeIf { it != 0 }
                    ?: return

            val changed =
                synchronized(this) {
                    val changed = lastChargingIconResId != iconId
                    lastChargingIconResId = iconId
                    changed
                }
            // Repeated samples still reconcile the state store after Hot Reload.
            try {
                onChargingIconResource(iconId)
            } catch (error: Throwable) {
                fallback("charging-glyph", error)
                return
            }
            if (changed) {
                report {
                    "batteryChargingGlyph source=MiuiBatteryMeterView." + sourceMethod +
                        " resourceId=" + iconId +
                        " authority=MiuiBatteryMeterView.getHollowChargingIconId()"
                }
            }
        }

        fun hook(
            method: Method,
            hookId: String,
        ): HookHandle =
            module
                .hook(method)
                .setId(hookId)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        if (!hooksReady) return@Hooker result
                        val iconView = readOrNull { chain.thisObject as? View }
                            ?: return@Hooker result
                        try {
                            publish(
                                iconView = iconView,
                                sourceMethod = method.name,
                            )
                        } catch (error: Throwable) {
                            fallback("state", error)
                        }
                        result
                    },
                )

        val levelMethod =
            iconClass.getDeclaredMethod(
                BATTERY_LEVEL_METHOD_NAME,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val chargeMethod =
            iconClass.getDeclaredMethod(
                CHARGE_STATE_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val powerSaveMethod =
            iconClass.getDeclaredMethod(
                POWER_SAVE_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val performanceMethod =
            iconClass.getDeclaredMethod(
                PERFORMANCE_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val miuiOptimizationMethod =
            iconClass.getDeclaredMethod(
                MIUI_OPTIMIZATION_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val updateChargeAndTextMethod =
            meterClass.getDeclaredMethod(UPDATE_CHARGE_AND_TEXT_METHOD_NAME)
                .apply { isAccessible = true }
        val handles = ArrayList<HookHandle>(HOOK_COUNT)
        try {
            handles +=
                module
                    .hook(updateChargeAndTextMethod)
                    .setId(CHARGING_GLYPH_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val result = chain.proceed()
                            if (!hooksReady) return@Hooker result
                            val meterView = readOrNull { chain.thisObject as? View }
                                ?: return@Hooker result
                            try {
                                publishChargingGlyph(
                                    meterView = meterView,
                                    sourceMethod = updateChargeAndTextMethod.name,
                                )
                            } catch (error: Throwable) {
                                fallback("charging-glyph", error)
                            }
                            result
                        },
                    )
            handles += hook(levelMethod, LEVEL_HOOK_ID)
            handles += hook(chargeMethod, CHARGE_HOOK_ID)
            handles += hook(powerSaveMethod, POWER_SAVE_HOOK_ID)
            handles += hook(performanceMethod, PERFORMANCE_HOOK_ID)
            handles += hook(miuiOptimizationMethod, MIUI_OPTIMIZATION_HOOK_ID)
            hooksReady = true
            return handles
        } catch (error: Throwable) {
            hooksReady = false
            val remaining = handles.asReversed().filter { handle ->
                try {
                    handle.unhook()
                    false
                } catch (failure: Throwable) {
                    if (failure is VirtualMachineError || failure is ThreadDeath) throw failure
                    true
                }
            }
            failedInstallHandles = remaining
            if (error is VirtualMachineError || error is ThreadDeath) throw error
            if (remaining.isNotEmpty()) {
                throw IllegalStateException("battery-source-hook-cleanup-failed", error)
            }
            throw error
        }
    }

    val failedInstallHookCount: Int
        get() = failedInstallHandles.size

    // Verified against the exact SystemUI fingerprint: both native number
    // routes must use the known file-backed faces, not mipro family fallbacks.
    fun readHostNumberFont(host: View): Pair<Typeface, String>? = readOrNull {
        val root = host as? ViewGroup ?: return@readOrNull null
        val container = root.directChild(BATTERY_CONTAINER_CLASS_NAME) as? ViewGroup
            ?: return@readOrNull null
        val battery = container.directChild(BATTERY_METER_VIEW_CLASS_NAME)
            ?: return@readOrNull null
        val loader = battery.javaClass.classLoader ?: return@readOrNull null
        val config = loader.loadClass("com.miui.utils.configs.MiuiConfigs")
        val percentFace = config.getField("sMiproTypefaceWght500").get(null) as? Typeface
            ?: return@readOrNull null
        val markFace = config.getField("sMiproTypefaceWght600").get(null) as? Typeface
            ?: return@readOrNull null
        val percent = battery.javaClass.getField("mBatteryPercentView").get(battery) as? TextView
            ?: return@readOrNull null
        val mark = battery.javaClass.getField("mBatteryPercentMarkView").get(battery) as? TextView
            ?: return@readOrNull null
        if (percent.typeface !== percentFace || mark.typeface !== markFace) {
            return@readOrNull null
        }
        val hollow = battery.javaClass.getField("mHollowBatteryIconView").get(battery)
            ?: return@readOrNull null
        val hollowPaint = hollow.javaClass.getField("textPaint").get(hollow) as? Paint
            ?: return@readOrNull null
        val hollowFace = loader.loadClass("com.android.systemui.statusbar.util.MiuiBatteryUtils")
            .getField("FONT_WGHT").get(null) as? Typeface ?: return@readOrNull null
        if (hollowPaint.typeface !== hollowFace) return@readOrNull null
        val path = config.getField("FONT_MIPRO_PATH").get(null) as? String
            ?: return@readOrNull null
        hollowFace to path
    }

    fun readHostChargingIconId(host: Any): Int? {
        val root = host as? ViewGroup ?: return null
        val container = root.directChild(BATTERY_CONTAINER_CLASS_NAME) as? ViewGroup
            ?: return null
        val battery = container.directChild(BATTERY_METER_VIEW_CLASS_NAME)
            ?: return null
        return readOrNull {
            battery.javaClass.getDeclaredMethod("getHollowChargingIconId").apply {
                isAccessible = true
            }.invoke(battery) as? Int
        }?.takeIf { it != 0 }
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.javaClass.name == className) return child
        }
        return null
    }

    private fun semanticColor(
        icon: Any,
        state: BatterySemanticState,
        chargingColorField: Field,
        powerSaveColorField: Field,
        superPowerSaveColorField: Field?,
        performanceColorField: Field,
        lowColorField: Field,
    ): Int? {
        val field =
            when (state) {
                BatterySemanticState.NORMAL -> return null
                BatterySemanticState.CHARGING -> chargingColorField
                BatterySemanticState.POWER_SAVE -> powerSaveColorField
                BatterySemanticState.SUPER_POWER_SAVE ->
                    superPowerSaveColorField ?: powerSaveColorField
                BatterySemanticState.PERFORMANCE -> performanceColorField
                BatterySemanticState.LOW -> lowColorField
            }
        return readOrNull { field.getInt(icon) }
            ?.takeIf { color -> color ushr 24 != 0 }
    }

    private fun Class<*>.requiredField(name: String): Field =
        getDeclaredField(name).apply { isAccessible = true }

    private fun Class<*>.optionalField(vararg names: String): Field? =
        names.firstNotNullOfOrNull { name ->
            readOrNull {
                getDeclaredField(name).apply { isAccessible = true }
            }
        }

    private fun colorHex(color: Int): String =
        "#" + color.toUInt().toString(16).padStart(8, '0')

    fun matches(handle: HookHandle): Boolean =
        handle.id == LEVEL_HOOK_ID ||
            handle.id == CHARGE_HOOK_ID ||
            handle.id == POWER_SAVE_HOOK_ID ||
            handle.id == PERFORMANCE_HOOK_ID ||
            handle.id == MIUI_OPTIMIZATION_HOOK_ID ||
            handle.id == CHARGING_GLYPH_HOOK_ID

    @Synchronized
    fun resetRuntimeState() {
        failedInstallHandles = emptyList()
        hooksReady = false
        lastState = null
        lastChargingIconResId = null
        verifiedFont = null
    }
}
