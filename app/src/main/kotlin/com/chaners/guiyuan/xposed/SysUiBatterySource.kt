package com.chaners.guiyuan.xposed

import android.view.View
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object SysUiBatterySource {
    const val BATTERY_ICON_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterIconView"
    const val BATTERY_METER_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
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
    private var lastState: StatusStateStore.BatteryState? = null

    @Volatile
    private var lastChargingIconResId: Int? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onBatteryState: (StatusStateStore.BatteryState) -> Unit,
        onChargingIconResource: (Int) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
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
                    return runCatching { chargingIconMethod.invoke(parent) as? Int }
                        .getOrNull()
                        ?.takeIf { it != 0 }
                }
                parent = parent.parent
            }
            return null
        }

        fun readState(iconView: View): StatusStateStore.BatteryState? {
            val level =
                runCatching { levelField.getInt(iconView) }
                    .getOrNull()
                    ?.coerceIn(0, 100)
                    ?: return null
            val charging =
                runCatching { chargingField.getBoolean(iconView) }
                    .getOrNull()
                    ?: return null
            val nativeStatusName =
                runCatching {
                    (progressStatusMethod.invoke(iconView) as? Enum<*>)?.name
                }.getOrNull()
            val semanticState =
                BatterySemanticState.fromNativeProgressStatus(
                    nativeStatusName,
                )
            val miuiOptimizationEnabled =
                runCatching { miuiOptimizationField.getBoolean(iconView) }
                    .getOrDefault(false)
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
                chargingIconResId =
                    if (charging) {
                        lastChargingIconResId ?: readChargingIconId(iconView)
                    } else {
                        null
                    },
            )
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
                    } else if (lastChargingIconResId == null) {
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
            onBatteryState(state)
            onEvent?.invoke(
                "batteryState source=MiuiBatteryMeterIconView." + sourceMethod +
                    " percent=" + state.percent +
                    " charging=" + state.charging +
                    " semantic=" + (state.semanticState?.name ?: "unavailable") +
                    " systemColor=" +
                    (state.systemSemanticColor?.let(::colorHex) ?: "status-icon") +
                    " chargingIconId=" + (state.chargingIconResId ?: "unavailable") +
                    " semanticAuthority=MiuiBatteryMeterIconView.getProgressStatus()",
            )
        }

        fun publishChargingGlyph(
            meterView: View,
            sourceMethod: String,
        ) {
            val nativeId =
                runCatching {
                    chargingIconMethod.invoke(meterView) as? Int
                }.getOrNull()
            val iconId =
                synchronized(this) {
                    val charging = lastState?.charging
                    val nextId =
                        resolveChargingIconId(
                            charging = charging,
                            nativeId = nativeId,
                            lastId = lastChargingIconResId,
                        )
                    if (lastChargingIconResId == nextId) {
                        return@synchronized null
                    }
                    lastChargingIconResId = nextId
                    nextId.takeIf { charging == true }
                } ?: return

            onChargingIconResource(iconId)
            onEvent?.invoke(
                "batteryChargingGlyph source=MiuiBatteryMeterView." + sourceMethod +
                    " charging=true" +
                    " resourceId=" + iconId +
                    " authority=MiuiBatteryMeterView.getHollowChargingIconId()",
            )
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
                        val iconView = chain.thisObject as? View
                            ?: return@Hooker result
                        publish(
                            iconView = iconView,
                            sourceMethod = method.name,
                        )
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
        val chargingGlyphHook =
            module
                .hook(updateChargeAndTextMethod)
                .setId(CHARGING_GLYPH_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        val meterView = chain.thisObject as? View
                            ?: return@Hooker result
                        publishChargingGlyph(
                            meterView = meterView,
                            sourceMethod = updateChargeAndTextMethod.name,
                        )
                        result
                    },
                )

        return listOf(
            hook(levelMethod, LEVEL_HOOK_ID),
            hook(chargeMethod, CHARGE_HOOK_ID),
            hook(powerSaveMethod, POWER_SAVE_HOOK_ID),
            hook(performanceMethod, PERFORMANCE_HOOK_ID),
            hook(miuiOptimizationMethod, MIUI_OPTIMIZATION_HOOK_ID),
            chargingGlyphHook,
        )
    }

    internal fun resolveChargingIconId(
        charging: Boolean?,
        nativeId: Int?,
        lastId: Int?,
    ): Int? {
        if (charging == false) return null

        // A missing glyph sample is not a charging-state transition.
        return nativeId?.takeIf { it != 0 } ?: lastId?.takeIf { it != 0 }
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
        return runCatching { field.getInt(icon) }
            .getOrNull()
            ?.takeIf { color -> color ushr 24 != 0 }
    }

    private fun Class<*>.requiredField(name: String): Field =
        getDeclaredField(name).apply { isAccessible = true }

    private fun Class<*>.optionalField(vararg names: String): Field? =
        names.firstNotNullOfOrNull { name ->
            runCatching {
                getDeclaredField(name).apply { isAccessible = true }
            }.getOrNull()
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
        lastState = null
        lastChargingIconResId = null
    }
}
