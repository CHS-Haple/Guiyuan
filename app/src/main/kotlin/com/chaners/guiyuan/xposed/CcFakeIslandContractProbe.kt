package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import java.lang.reflect.Field

/**
 * Bounded, passive CC fake-island contract observation.
 * No HookHandle, View cache, lifecycle owner or active listener lives here.
 */
internal object CcFakeIslandContractProbe {
    private val candidateFieldNames =
        listOf(
            "slot",
            "visibleState",
            "inIslandState",
            "beforeInIslandState",
            "islandChanged",
            "supportAnim",
            "forceAppear",
            "layoutTranslationX",
        )

    fun describe(root: ViewGroup): String {
        val views = ArrayList<View>()
        collect(root, 0, views)
        val contractViews = views.mapNotNull(::describeView).distinct().take(8)
        return "controlCenterFakeIslandContract root=" + root.javaClass.name +
            " candidates=" + contractViews.size +
            " entries=[" + contractViews.joinToString("|") +
            "]"
    }

    private fun collect(view: View, depth: Int, views: MutableList<View>) {
        if (views.size >= 64 || depth > 6) return
        views += view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collect(view.getChildAt(index), depth + 1, views)
                if (views.size >= 64) return
            }
        }
    }

    private fun hierarchy(type: Class<*>): List<Class<*>> {
        val result = ArrayList<Class<*>>()
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            result += current
            current = current.superclass
        }
        return result
    }

    private fun field(type: Class<*>, name: String): Field? =
        hierarchy(type).firstNotNullOfOrNull { owner ->
            optionalNativeRead {
                owner.getDeclaredField(name).apply { isAccessible = true }
            }
        }

    private fun describeView(view: View): String? {
        val resolvedFields = candidateFieldNames.mapNotNull { name ->
            field(view.javaClass, name)?.let { resolved -> name to resolved }
        }
        val methodNames =
            hierarchy(view.javaClass)
                .flatMap { owner -> owner.declaredMethods.asList() }
                .map { method -> method.name }
                .filter { name ->
                    name.contains("island", ignoreCase = true) ||
                        name.contains("forceAppear", ignoreCase = true) ||
                        name.contains("visibleState", ignoreCase = true)
                }
                .distinct()
                .sorted()
        if (resolvedFields.none { (name, _) -> name == "inIslandState" } &&
            methodNames.none { name -> name.contains("island", ignoreCase = true) }
        ) {
            return null
        }
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val values = resolvedFields.joinToString(",") { (name, resolved) ->
            val value = optionalNativeRead { resolved.get(view) }?.toString() ?: "unavailable"
            name + "=" + value
        }
        return view.javaClass.name +
            "(x=" + location[0] +
            ",y=" + location[1] +
            ",w=" + view.width +
            ",h=" + view.height +
            ",fields={" + values +
            "},methods=[" + methodNames.take(16).joinToString(",") +
            "])"
    }
}
