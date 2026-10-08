package com.chaners.guiyuan.xposed

import android.graphics.drawable.ClipDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.util.ArrayList
import java.util.WeakHashMap

internal object SysUiTintSource {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val UPDATE_TINT_METHOD_NAME = "updateLightDarkTint"
    const val ON_DARK_CHANGED_INTERNAL_METHOD_NAME = "onDarkChangedInternal"
    const val HOOK_COUNT = 2

    private const val UPDATE_HOOK_ID = "combinedstatus.tint.battery.update"
    private const val INTERNAL_HOOK_ID = "combinedstatus.tint.battery.internal"
    private val lastStates = WeakHashMap<View, TintState>()
    private val firstEventLogged = WeakHashMap<View, Unit>()
    private val batteryIconStructureLogged = WeakHashMap<View, Unit>()
    private val lastSemanticBatteryTints = WeakHashMap<View, List<Int>>()
    private val batteryIconClipFields = HashMap<Class<*>, List<Field>>()
    private val colorFilterColorGetters = HashMap<Class<*>, java.lang.reflect.Method>()
    private val colorFilterWithoutColorGetter = HashSet<Class<*>>()

    @Volatile
    private var batteryPercentViewField: Field? = null

    @Volatile
    private var batteryIconViewField: Field? = null

    @Volatile
    private var lastSourceView: WeakReference<View>? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onTintState: (TintUpdate) -> Unit,
        onEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
    ): List<HookHandle> {
        val batteryClass =
            Class.forName(BATTERY_VIEW_CLASS_NAME, false, classLoader)
        val percentField =
            batteryClass.getDeclaredField("mBatteryPercentView")
                .apply { isAccessible = true }
        val iconField =
            batteryClass.getDeclaredField("mBatteryIconView")
                .apply { isAccessible = true }
        val updateMethod =
            batteryClass.getDeclaredMethod(
                UPDATE_TINT_METHOD_NAME,
                ArrayList::class.java,
                Float::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val internalMethod =
            batteryClass
                .getDeclaredMethod(ON_DARK_CHANGED_INTERNAL_METHOD_NAME)
                .apply { isAccessible = true }

        batteryPercentViewField = percentField
        batteryIconViewField = iconField

        val updateHandle =
            module
                .hook(updateMethod)
                .setId(UPDATE_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        val sourceView = chain.thisObject as? View
                            ?: return@Hooker result
                        val state =
                            dispatchAppliedState(
                                sourceView = sourceView,
                                percentField = percentField,
                                onTintState = onTintState,
                            )
                        if (onEvent != null && isDetailedDiagnosticsEnabled()) {
                            probeBatteryIconAuthority(
                                sourceView = sourceView,
                                iconField = iconField,
                                source = UPDATE_TINT_METHOD_NAME,
                                onEvent = onEvent,
                            )
                            if (
                                state != null &&
                                synchronized(this) {
                                    firstEventLogged.put(sourceView, Unit) == null
                                }
                            ) {
                                val darkIntensity =
                                    (chain.getArg(1) as? Number)?.toFloat()
                                        ?: Float.NaN
                                val lightColor =
                                    (chain.getArg(3) as? Number)?.toInt() ?: 0
                                val darkColor =
                                    (chain.getArg(4) as? Number)?.toInt() ?: 0
                                val useTint =
                                    chain.getArg(5) as? Boolean ?: false
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
                            }
                        }

                        result
                    },
                )

        val internalHandle =
            module
                .hook(internalMethod)
                .setId(INTERNAL_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        val sourceView = chain.thisObject as? View
                            ?: return@Hooker result
                        dispatchAppliedState(
                            sourceView = sourceView,
                            percentField = percentField,
                            onTintState = onTintState,
                        )
                        if (onEvent != null && isDetailedDiagnosticsEnabled()) {
                            probeBatteryIconAuthority(
                                sourceView = sourceView,
                                iconField = iconField,
                                source = ON_DARK_CHANGED_INTERNAL_METHOD_NAME,
                                onEvent = onEvent,
                            )
                        }
                        result
                    },
                )

        return listOf(updateHandle, internalHandle)
    }

    private fun dispatchAppliedState(
        sourceView: View,
        percentField: Field,
        onTintState: (TintUpdate) -> Unit,
    ): TintState? {
        val state =
            readAppliedState(
                sourceView = sourceView,
                percentField = percentField,
            ) ?: return null

        val changed =
            synchronized(this) {
                val previous = lastStates[sourceView]
                lastStates[sourceView] = state
                lastSourceView = WeakReference(sourceView)
                previous != state
            }
        if (changed) {
            onTintState(TintUpdate(sourceView, state))
        }
        return state
    }

    private fun probeBatteryIconAuthority(
        sourceView: View,
        iconField: Field,
        source: String,
        onEvent: (String) -> Unit,
    ) {
        val iconView =
            runCatching {
                iconField.get(sourceView) as? View
            }.getOrNull() ?: return

        val clipFields = collectClipDrawableFields(iconView)
        val clipStates: List<BatteryClipTintState> =
            clipFields.map { field ->
                val clip =
                    runCatching {
                        field.get(iconView) as? ClipDrawable
                    }.getOrNull()
                val filter = clip?.colorFilter
                BatteryClipTintState(
                    fieldName = field.name,
                    filterClass = filter?.javaClass?.name,
                    color = readColorFilterColor(filter),
                )
            }
        val semanticTints =
            clipStates
                .mapNotNull { state -> state.color }
                .filter(::isChromaticTint)
                .distinct()
                .sorted()

        val imageTint =
            (iconView as? ImageView)
                ?.imageTintList
                ?.defaultColor
        val structureFirst =
            synchronized(this) {
                batteryIconStructureLogged.put(sourceView, Unit) == null
            }
        val semanticChanged =
            synchronized(this) {
                val previous = lastSemanticBatteryTints[sourceView]
                lastSemanticBatteryTints[sourceView] = semanticTints
                semanticTints.isNotEmpty() && previous != semanticTints
            }

        if (!structureFirst && !semanticChanged) {
            return
        }

        onEvent(
            "batteryIconProbe source=" + source +
                " iconClass=" + iconView.javaClass.name +
                " imageView=" + (iconView is ImageView) +
                " clipFields=" +
                (
                    if (clipStates.isEmpty()) {
                        "none"
                    } else {
                        clipStates.joinToString(",") { state ->
                            state.fieldName + ":" +
                                (state.filterClass ?: "no-filter") + ":" +
                                (state.color?.let(::colorHex) ?: "color-unavailable")
                        }
                    }
                ) +
                " imageTint=" + (imageTint?.let(::colorHex) ?: "none") +
                " semanticTints=" +
                (
                    if (semanticTints.isEmpty()) {
                        "none"
                    } else {
                        semanticTints.joinToString(",") { colorHex(it) }
                    }
                ),
        )
    }

    private fun readColorFilterColor(filter: android.graphics.ColorFilter?): Int? {
        if (filter == null) {
            return null
        }
        val filterClass = filter.javaClass
        val getter =
            synchronized(this) {
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
        return runCatching {
            (getter.invoke(filter) as? Number)?.toInt()
        }.getOrNull()
    }


    private fun collectClipDrawableFields(iconView: View): List<Field> {
        val iconClass = iconView.javaClass
        synchronized(this) {
            batteryIconClipFields[iconClass]?.let { return it }
        }

        val fields = mutableListOf<Field>()
        var current: Class<*>? = iconClass
        while (
            current != null &&
            View::class.java.isAssignableFrom(current)
        ) {
            current.declaredFields
                .filter { field ->
                    ClipDrawable::class.java.isAssignableFrom(field.type)
                }
                .forEach { field ->
                    field.isAccessible = true
                    fields += field
                }
            current = current.superclass
        }
        val resolved =
            fields.distinctBy { field ->
                field.declaringClass.name + "#" + field.name
            }
        synchronized(this) {
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

    fun matches(handle: HookHandle): Boolean =
        handle.id == UPDATE_HOOK_ID || handle.id == INTERNAL_HOOK_ID

    @Synchronized
    fun resetRuntimeState() {
        lastStates.clear()
        firstEventLogged.clear()
        batteryIconStructureLogged.clear()
        lastSemanticBatteryTints.clear()
        batteryIconClipFields.clear()
        colorFilterColorGetters.clear()
        colorFilterWithoutColorGetter.clear()
        batteryPercentViewField = null
        batteryIconViewField = null
        lastSourceView = null
    }

    @Synchronized
    fun currentSourceView(): View? = lastSourceView?.get()

    @Synchronized
    fun currentState(sourceView: View): TintState? {
        val cached =
            lastStates[sourceView]
                ?.takeIf { it.isVisible }
        val field = batteryPercentViewField
        val refreshed =
            field?.let { percentField ->
                readAppliedState(
                    sourceView = sourceView,
                    percentField = percentField,
                )
            }
        if (
            refreshed != null &&
            refreshed.isVisible
        ) {
            lastStates[sourceView] = refreshed
            return refreshed
        }
        return cached
    }

    private fun readAppliedState(
        sourceView: View,
        percentField: Field,
    ): TintState? {
        val percentView =
            runCatching {
                percentField.get(sourceView) as? TextView
            }.getOrNull() ?: return null
        return TintState(
            appliedTint = percentView.currentTextColor,
        )
    }

    private fun colorHex(color: Int): String =
        "#" + color.toUInt().toString(16).padStart(8, '0')

    private data class BatteryClipTintState(
        val fieldName: String,
        val filterClass: String?,
        val color: Int?,
    )

    internal data class TintUpdate(
        val sourceView: View,
        val state: TintState,
    )
}
