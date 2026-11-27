package com.chaners.guiyuan.xposed.network

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.chaners.guiyuan.xposed.NativeStatusSlots
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

// Resolves native tint witnesses; observation sessions and delivery remain with NativeStatusObserver.
internal object NativeStatusTintResolver {
    private const val DARK_ICON_DISPATCHER_CLASS =
        "com.android.systemui.plugins.DarkIconDispatcher"
    private const val DARK_ICON_DISPATCHER_FIELD = "mDarkIconDispatcher"
    private const val TINT_AREAS_FIELD = "mTintAreas"
    private const val ICON_TINT_FIELD = "mIconTint"
    private const val MANAGER_COLOR_FIELD = "mColor"
    private val representedTintSlots =
        setOf("combined_status", "wifi", "mobile", "stacked_mobile", NO_SIM_SLOT, AIRPLANE_SLOT)

    // Cache reflective lookups, not the live SystemUI tint values.
    private val tintFieldNames =
        setOf(DARK_ICON_DISPATCHER_FIELD, TINT_AREAS_FIELD, ICON_TINT_FIELD, MANAGER_COLOR_FIELD)
    private val tintFieldsByClass =
        ConcurrentHashMap<Class<*>, List<Field>>()
    private val tintMethodByManagerClass =
        ConcurrentHashMap<Class<*>, Method>()

    // Clear only at the original runtime-reset boundary.
    fun resetCaches() {
        tintFieldsByClass.clear()
        tintMethodByManagerClass.clear()
    }

    fun resolveLocationAwareManagerTint(
        manager: Any?,
        anchorView: View?,
    ): Int? {
        manager ?: return null
        anchorView ?: return null

        val dispatcher =
            readObjectField(manager, DARK_ICON_DISPATCHER_FIELD)
                ?: return null
        val iconTint =
            readIntField(dispatcher, ICON_TINT_FIELD)
                ?.takeIf(StatusIconTintPolicy::isVisibleTint)
                ?: return null
        val tintAreas =
            readObjectField(dispatcher, TINT_AREAS_FIELD)
                as? Collection<*>
                ?: return null

        return optionalNativeRead {
            val managerClass = manager.javaClass
            val getTint =
                tintMethodByManagerClass[managerClass]
                    ?: Class.forName(
                        DARK_ICON_DISPATCHER_CLASS,
                        false,
                        managerClass.classLoader,
                    ).methods.firstOrNull { method ->
                        method.name == "getTint" &&
                            method.parameterCount == 3 &&
                            View::class.java.isAssignableFrom(
                                method.parameterTypes[1],
                            ) &&
                            method.parameterTypes[2] ==
                                Int::class.javaPrimitiveType
                    }?.also { tintMethodByManagerClass[managerClass] = it }
            getTint?.let { method ->
                (method.invoke(null, tintAreas, anchorView, iconTint) as? Number)
                    ?.toInt()
                    ?.takeIf(StatusIconTintPolicy::isVisibleTint)
            }
        }
    }

    fun resolveManagerFallbackTint(manager: Any?): Int? {
        manager ?: return null

        readIntField(manager, MANAGER_COLOR_FIELD)
            ?.takeIf(StatusIconTintPolicy::isVisibleTint)
            ?.let { return it }

        val dispatcher =
            readObjectField(manager, DARK_ICON_DISPATCHER_FIELD)
                ?: return null
        return readIntField(dispatcher, ICON_TINT_FIELD)
            ?.takeIf(StatusIconTintPolicy::isVisibleTint)
    }

    fun resolveTintAnchorView(group: ViewGroup): View? {
        for (index in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(index)
            if (isTintAuthorityCandidate(child)) {
                return child
            }
        }
        return null
    }

    /** Read only applied native image tint; no cached View or observer callback is retained. */
    fun findAppliedTint(view: View): Int? {
        if (view is ImageView) {
            view.imageTintList
                ?.defaultColor
                ?.takeIf { color -> (color ushr 24) != 0 }
                ?.let { return it }
        }
        val group = view as? ViewGroup ?: return null
        for (index in 0 until group.childCount) {
            findAppliedTint(group.getChildAt(index))?.let { return it }
        }
        return null
    }

    fun isTintAuthorityCandidate(view: View): Boolean =
        isTintAuthorityCandidate(
            slot = NativeStatusSlots.slotOf(view),
            visible = view.visibility == View.VISIBLE,
            width = view.width,
            height = view.height,
        )

    internal fun isTintAuthorityCandidate(
        slot: String?,
        visible: Boolean,
        width: Int,
        height: Int,
    ): Boolean =
        slot !in representedTintSlots &&
            visible &&
            width > 0 &&
            height > 0

    private fun tintFields(clazz: Class<*>): List<Field> =
        tintFieldsByClass.computeIfAbsent(clazz) {
            optionalNativeRead {
                generateSequence(clazz) { it.superclass }
                    .flatMap { it.declaredFields.asSequence() }
                    .filter { it.name in tintFieldNames }
                    .toList()
            } ?: emptyList()
        }

    private fun readObjectField(
        target: Any,
        name: String,
    ): Any? {
        val field =
            tintFields(target.javaClass)
                .firstOrNull { it.name == name }
                ?: return null
        return optionalNativeRead {
            field.isAccessible = true
            field.get(target)
        }
    }

    private fun readIntField(
        target: Any,
        name: String,
    ): Int? {
        val field =
            tintFields(target.javaClass)
                .firstOrNull {
                    it.name == name &&
                        (it.type == Int::class.javaPrimitiveType ||
                            it.type == Int::class.javaObjectType)
                }
                ?: return null
        return optionalNativeRead {
            field.isAccessible = true
            (field.get(target) as? Number)?.toInt()
        }
    }

}
