package com.chaners.guiyuan.xposed

import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import java.lang.reflect.Field

// Passive native font/view/state lookups; Hook receipts belong to SysUiBatterySource.
internal object SysUiBatteryEvidence {
    // Installation-scoped reflection contract; delivery and fallback receipts remain in the source.
    class Reader(
        classLoader: ClassLoader,
        private val fallbackChargingIconId: () -> Int?,
    ) {
        val iconClass =
            Class.forName(SysUiBatterySource.BATTERY_ICON_VIEW_CLASS_NAME, false, classLoader)
        val meterClass =
            Class.forName(SysUiBatterySource.BATTERY_METER_VIEW_CLASS_NAME, false, classLoader)
        private val levelField = iconClass.batteryRequiredField("mLevel")
        private val chargingField = iconClass.batteryRequiredField("mCharging")
        private val meterIconField = meterClass.batteryRequiredField("mBatteryIconView")
        private val progressStatusMethod =
            iconClass.getDeclaredMethod("getProgressStatus")
                .apply { isAccessible = true }
        private val chargingColorField = iconClass.batteryRequiredField("mBatteryChargingColor")
        private val powerSaveColorField = iconClass.batteryRequiredField("mBatteryPowerSaveColor")
        private val superPowerSaveColorField =
            iconClass.batteryOptionalField(
                "mBatterySuperPowerSaveColor",
                "mBatterySuperSaveColor",
            )
        private val performanceColorField =
            iconClass.batteryRequiredField("mBatteryPerformanceModeColor")
        private val lowColorField = iconClass.batteryRequiredField("mBatteryLowColor")
        private val miuiOptimizationField =
            iconClass.batteryRequiredField("mMiuiOptimizationEnabled")
        private val chargingIconMethod =
            meterClass.getDeclaredMethod("getHollowChargingIconId")
                .apply { isAccessible = true }

        private fun readChargingIconId(iconView: View): Int? {
            var parent = iconView.parent
            while (parent is View) {
                if (meterClass.isInstance(parent)) {
                    return readMeterChargingIconId(parent)
                }
                parent = parent.parent
            }
            return null
        }

        fun readState(iconView: View): StatusStateStore.BatteryState? {
            val level =
                optionalNativeRead { levelField.getInt(iconView) }
                    ?.coerceIn(0, 100)
                    ?: return null
            val charging =
                optionalNativeRead { chargingField.getBoolean(iconView) }
                    ?: return null
            val nativeStatusName =
                optionalNativeRead {
                    (progressStatusMethod.invoke(iconView) as? Enum<*>)?.name
                }
            val semanticState =
                BatterySemanticState.fromNativeProgressStatus(
                    nativeStatusName,
                )
            val miuiOptimizationEnabled =
                optionalNativeRead { miuiOptimizationField.getBoolean(iconView) }
                    ?: false
            val systemSemanticColor =
                if (miuiOptimizationEnabled) {
                    semanticState?.let { state ->
                        semanticColor(iconView, state)
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
                        optionalNativeRead { lowColorField.getInt(iconView) }
                    } else null,
                chargingIconResId =
                    if (charging) {
                        readChargingIconId(iconView) ?: fallbackChargingIconId()
                    } else {
                        null
                    },
            )
        }

        fun iconView(targetView: View?): View? =
            when {
                targetView == null -> null
                iconClass.isInstance(targetView) -> targetView
                meterClass.isInstance(targetView) ->
                    optionalNativeRead { meterIconField.get(targetView) as? View }
                else -> null
            }

        fun readMeterChargingIconId(meterView: View): Int? =
            optionalNativeRead { chargingIconMethod.invoke(meterView) as? Int }
                ?.takeIf { it != 0 }

        private fun semanticColor(icon: Any, state: BatterySemanticState): Int? {
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
            return optionalNativeRead { field.getInt(icon) }
                ?.takeIf { color -> color ushr 24 != 0 }
        }

    }

    fun readNumberFontContract(host: View): Pair<Typeface, String?>? {
        val battery = nativeBatteryMeter(host) ?: return null
        val hollow = battery.javaClass.getField("mHollowBatteryIconView").get(battery)
            ?: return null
        val paint = hollow.javaClass.getField("textPaint").get(hollow) as? Paint
            ?: return null
        val face = paint.typeface ?: return null
        // The live face is useful even when the OEM file identity cannot be proved.
        return face to optionalNativeRead { readNumberFontPath(battery, face) }
    }

    private fun readNumberFontPath(battery: View, face: Typeface): String? {
        val loader = battery.javaClass.classLoader ?: return null
        val config = loader.loadClass("com.miui.utils.configs.MiuiConfigs")
        val percentFace = config.getField("sMiproTypefaceWght500").get(null) as? Typeface
            ?: return null
        val markFace = config.getField("sMiproTypefaceWght600").get(null) as? Typeface
            ?: return null
        val percent = battery.javaClass.getField("mBatteryPercentView").get(battery) as? TextView
            ?: return null
        val mark = battery.javaClass.getField("mBatteryPercentMarkView").get(battery) as? TextView
            ?: return null
        if (percent.typeface !== percentFace || mark.typeface !== markFace) {
            return null
        }
        val hollowFace = loader.loadClass("com.android.systemui.statusbar.util.MiuiBatteryUtils")
            .getField("FONT_WGHT").get(null) as? Typeface ?: return null
        if (face !== hollowFace) return null
        return config.getField("FONT_MIPRO_PATH").get(null) as? String
    }

    fun nativeBatteryMeter(host: Any): View? {
        val root = host as? ViewGroup ?: return null
        val container = root.directNativeChild(SysUiBatterySource.BATTERY_CONTAINER_CLASS_NAME) as? ViewGroup
            ?: return null
        return container.directNativeChild(SysUiBatterySource.BATTERY_METER_VIEW_CLASS_NAME)
    }

}

internal fun Class<*>.batteryRequiredField(name: String): Field =
    getDeclaredField(name).apply { isAccessible = true }

internal fun Class<*>.batteryOptionalField(vararg names: String): Field? =
    names.firstNotNullOfOrNull { name ->
        optionalNativeRead {
            getDeclaredField(name).apply { isAccessible = true }
        }
    }
