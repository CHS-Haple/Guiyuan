package com.chaners.guiyuan.xposed

import android.graphics.drawable.ClipDrawable
import android.view.View
import android.widget.ImageView
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import java.lang.reflect.Field
import java.util.WeakHashMap

// Passive native battery-icon reflection, performed only during requested detailed diagnostics.
// The existing SysUiTintSource lock serializes all diagnostic caches and their retirement.
internal object SysUiTintDiagnosticProbe {
    private val firstEventLogged = WeakHashMap<View, Unit>()
    private val batteryIconStructureLogged = WeakHashMap<View, Unit>()
    private val batteryIconClipFields = HashMap<Class<*>, List<Field>>()
    private val colorFilterColorGetters = HashMap<Class<*>, java.lang.reflect.Method>()
    private val colorFilterWithoutColorGetter = HashSet<Class<*>>()

    fun probeBatteryIconAuthority(
        sourceView: View,
        iconField: Field,
        source: String,
        onEvent: (String) -> Unit,
    ) {
        val iconView =
            optionalNativeRead {
                iconField.get(sourceView) as? View
            } ?: return

        if (synchronized(SysUiTintSource) { batteryIconStructureLogged.containsKey(iconView) }) {
            return
        }

        val semanticTints = mutableSetOf<Int>()
        val clipFieldsText =
            collectClipDrawableFields(iconView).joinToString(",") { field ->
                val clip = optionalNativeRead { field.get(iconView) as? ClipDrawable }
                val filter = clip?.colorFilter
                val color = readColorFilterColor(filter)
                if (color != null && isChromaticTint(color)) semanticTints += color
                field.name + ":" +
                    (filter?.javaClass?.name ?: "no-filter") + ":" +
                    (color?.let(::colorHex) ?: "color-unavailable")
            }.ifEmpty { "none" }
        val semanticTintsText =
            semanticTints.sorted().joinToString(",") { colorHex(it) }.ifEmpty { "none" }
        val imageTint =
            (iconView as? ImageView)
                ?.imageTintList
                ?.defaultColor
        onEvent(
            "batteryIconProbe source=" + source +
                " iconClass=" + iconView.javaClass.name +
                " imageView=" + (iconView is ImageView) +
                " clipFields=" + clipFieldsText +
                " imageTint=" + (imageTint?.let(::colorHex) ?: "none") +
                " semanticTints=" + semanticTintsText,
        )
        synchronized(SysUiTintSource) { batteryIconStructureLogged[iconView] = Unit }
    }

    inline fun probeFirstNativeArguments(
        sourceView: View,
        state: TintState,
        onEvent: (String) -> Unit,
        getArg: (Int) -> Any?,
    ) {
        if (synchronized(SysUiTintSource) { firstEventLogged.containsKey(sourceView) }) return
        val darkIntensity =
            (getArg(1) as? Number)?.toFloat()
                ?: Float.NaN
        val lightColor =
            (getArg(3) as? Number)?.toInt() ?: 0
        val darkColor =
            (getArg(4) as? Number)?.toInt() ?: 0
        val useTint =
            getArg(5) as? Boolean ?: false
        onEvent(
            "tintSource receiver=" +
                sourceView.javaClass.simpleName +
                " batteryApplied=" + colorHex(state.appliedTint) +
                " authority=battery-anchor-fallback" +
                " intensity=" + darkIntensity +
                " light=" + colorHex(lightColor) +
                " dark=" + colorHex(darkColor) +
                " useTint=" + useTint,
        )
        synchronized(SysUiTintSource) { firstEventLogged[sourceView] = Unit }
    }

    private fun readColorFilterColor(filter: android.graphics.ColorFilter?): Int? {
        if (filter == null) {
            return null
        }
        val filterClass = filter.javaClass
        val getter =
            synchronized(SysUiTintSource) {
                colorFilterColorGetters[filterClass]
                    ?: if (filterClass in colorFilterWithoutColorGetter) {
                        null
                    } else {
                        filterClass.methods
                            .firstOrNull { method ->
                                method.name == "getColor" &&
                                    method.parameterCount == 0 &&
                                    (
                                        method.returnType == Int::class.javaPrimitiveType ||
                                            method.returnType == Int::class.javaObjectType
                                    )
                            }
                            ?.also { method ->
                                colorFilterColorGetters[filterClass] = method
                            }
                            ?: run {
                                colorFilterWithoutColorGetter += filterClass
                                null
                            }
                    }
            }
                ?: return null
        return optionalNativeRead {
            (getter.invoke(filter) as? Number)?.toInt()
        }
    }


    private fun collectClipDrawableFields(iconView: View): List<Field> {
        val iconClass = iconView.javaClass
        synchronized(SysUiTintSource) {
            batteryIconClipFields[iconClass]?.let { return it }
        }

        val fields = mutableListOf<Field>()
        var current: Class<*>? = iconClass
        while (
            current != null &&
            View::class.java.isAssignableFrom(current)
        ) {
            for (field in current.declaredFields) {
                if (!ClipDrawable::class.java.isAssignableFrom(field.type)) continue
                field.isAccessible = true
                fields += field
            }
            current = current.superclass
        }
        val resolved =
            fields.distinctBy { field ->
                field.declaringClass.name + "#" + field.name
            }
        synchronized(SysUiTintSource) {
            batteryIconClipFields[iconClass] = resolved
        }
        return resolved
    }

    private fun isChromaticTint(color: Int): Boolean {
        val red = color ushr 16 and 0xff
        val green = color ushr 8 and 0xff
        val blue = color and 0xff
        return red != green || green != blue
    }

    fun resetTransient() = synchronized(SysUiTintSource) {
        firstEventLogged.clear()
        batteryIconStructureLogged.clear()
    }

    fun clearAll() = synchronized(SysUiTintSource) {
        firstEventLogged.clear()
        batteryIconStructureLogged.clear()
        batteryIconClipFields.clear()
        colorFilterColorGetters.clear()
        colorFilterWithoutColorGetter.clear()
    }
}
