package com.chaners.guiyuan.xposed

import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import java.lang.reflect.Field

// Passive native font/view/field lookups; Hook receipts belong to SysUiBatterySource.
internal object SysUiBatteryEvidence {
    fun readNumberFontContract(host: View): Pair<Typeface, String>? {
        val root = host as? ViewGroup ?: return null
        val container = root.directChild(SysUiBatterySource.BATTERY_CONTAINER_CLASS_NAME) as? ViewGroup
            ?: return null
        val battery = container.directChild(SysUiBatterySource.BATTERY_METER_VIEW_CLASS_NAME)
            ?: return null
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
        val hollow = battery.javaClass.getField("mHollowBatteryIconView").get(battery)
            ?: return null
        val hollowPaint = hollow.javaClass.getField("textPaint").get(hollow) as? Paint
            ?: return null
        val hollowFace = loader.loadClass("com.android.systemui.statusbar.util.MiuiBatteryUtils")
            .getField("FONT_WGHT").get(null) as? Typeface ?: return null
        if (hollowPaint.typeface !== hollowFace) return null
        val path = config.getField("FONT_MIPRO_PATH").get(null) as? String
            ?: return null
        return hollowFace to path
    }

    fun nativeBatteryMeter(host: Any): View? {
        val root = host as? ViewGroup ?: return null
        val container = root.directChild(SysUiBatterySource.BATTERY_CONTAINER_CLASS_NAME) as? ViewGroup
            ?: return null
        return container.directChild(SysUiBatterySource.BATTERY_METER_VIEW_CLASS_NAME)
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.javaClass.name == className) return child
        }
        return null
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
