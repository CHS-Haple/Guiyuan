package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

// Slot querying and icon removal share the native slot contract but not participant acquisition.
internal object NativeStatusSlots {
    private const val REFLECTED_STRING_TYPE = "java.lang.String"
    // Status-icon children are queried from native onLayout hot paths. Resolve the
    // reflective slot getter once per concrete SystemUI view class instead of
    // walking the method hierarchy for every child on every layout.
    private val slotAccessorCache = ConcurrentHashMap<Class<*>, Method>()
    private val slotAccessorMissing = ConcurrentHashMap.newKeySet<Class<*>>()

    fun iconHolder(
        handles: NativeParticipantAccess.Handles,
        slot: String,
        tag: Int = 0,
    ): Any? {
        val iconList =
            handles.controller.readField("mStatusBarIconList")
                ?: return null
        val getter =
            optionalNativeRead {
                iconList.javaClass
                    .allMethods()
                    .firstOrNull { method ->
                        method.name == "getIconHolder" &&
                            method.parameterTypes.map { type -> type.name } ==
                                listOf("int", REFLECTED_STRING_TYPE)
                    }?.apply { isAccessible = true }
            } ?: return null
        return optionalNativeRead { getter.invoke(iconList, tag, slot) }
    }

    @Suppress("UNCHECKED_CAST")
    fun clearBindableEntries(
        handles: NativeParticipantAccess.Handles,
        slot: String,
        expectedHolder: Any? = null,
    ): Int {
        val managers =
            handles.controller.readField("mIconGroups") as? Iterable<*>
                ?: return 0
        var removed = 0
        for (manager in managers) {
            if (manager == null) continue
            val map =
                manager.readField("mBindableIcons") as? MutableMap<Any?, Any?>
                    ?: continue
            val current = map[slot]
            if (current != null && (expectedHolder == null || current === expectedHolder)) {
                map.remove(slot)
                removed += 1
            }
        }
        return removed
    }

    fun removal(controllerClass: Class<*>): Removal? =
        optionalNativeRead {
            val candidates =
                controllerClass
                    .allMethods()
                    .mapNotNull { method ->
                        classifyRemoval(method)?.let { mode ->
                            Removal(method = method, mode = mode)
                        }
                    }
                    .toList()

            candidates.minByOrNull { candidate ->
                when (candidate.mode) {
                    RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG -> 0
                    RemovalMode.REMOVE_ALL_SLOT -> 1
                    RemovalMode.REMOVE_TAGGED -> 2
                    RemovalMode.REMOVE_SLOT -> 3
                }
            }
        }

    fun classifyRemoval(method: Method): RemovalMode? {
        val params = method.parameterTypes.map { type -> type.name }
        return when {
            method.name == "removeAllIconsForSlot" &&
                params == listOf(REFLECTED_STRING_TYPE, "boolean") ->
                RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG

            method.name == "removeAllIconsForSlot" &&
                params == listOf(REFLECTED_STRING_TYPE) ->
                RemovalMode.REMOVE_ALL_SLOT

            method.name == "removeIcon" &&
                params == listOf(REFLECTED_STRING_TYPE, "int") ->
                RemovalMode.REMOVE_TAGGED

            method.name == "removeIcon" &&
                params == listOf(REFLECTED_STRING_TYPE) ->
                RemovalMode.REMOVE_SLOT

            else -> null
        }
    }

    fun invokeRemoval(
        handles: NativeParticipantAccess.Handles,
        removal: Removal,
        slot: String,
    ) {
        removal.method.isAccessible = true
        when (removal.mode) {
            RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG ->
                removal.method.invoke(
                    handles.controller,
                    slot,
                    false,
                )

            RemovalMode.REMOVE_ALL_SLOT ->
                removal.method.invoke(
                    handles.controller,
                    slot,
                )

            RemovalMode.REMOVE_TAGGED ->
                removal.method.invoke(
                    handles.controller,
                    slot,
                    0,
                )

            RemovalMode.REMOVE_SLOT ->
                removal.method.invoke(
                    handles.controller,
                    slot,
                )
        }
    }

    fun findSlotView(
        group: ViewGroup,
        slot: String,
    ): View? {
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            if (slotOf(child) == slot) {
                return child
            }
        }
        return null
    }

    fun slotOf(view: View): String? {
        val viewClass = view.javaClass
        val getSlot =
            slotAccessorCache[viewClass]
                ?: run {
                    if (viewClass in slotAccessorMissing) return null
                    val resolved =
                        optionalNativeRead {
                            viewClass
                                .allMethods()
                                .firstOrNull { method ->
                                    method.name == "getSlot" &&
                                        method.parameterCount == 0 &&
                                        method.returnType == String::class.java
                                }?.apply { isAccessible = true }
                        }
                    if (resolved == null) {
                        slotAccessorMissing += viewClass
                        return null
                    }
                    slotAccessorCache.putIfAbsent(viewClass, resolved) ?: resolved
                }

        return optionalNativeRead { getSlot.invoke(view) as? String }
    }

    fun resourceEntryName(view: View): String? {
        if (view.id == View.NO_ID) return null
        return optionalNativeRead { view.resources.getResourceEntryName(view.id) }
    }

    fun findResourceView(root: View, entryName: String): View? {
        if (resourceEntryName(root) == entryName) return root
        val group = root as? ViewGroup ?: return null
        for (index in 0 until group.childCount) {
            findResourceView(group.getChildAt(index), entryName)?.let { return it }
        }
        return null
    }

    internal data class Removal(
        val method: Method,
        val mode: RemovalMode,
    )

    internal enum class RemovalMode {
        REMOVE_ALL_SLOT_PIPELINE_FLAG,
        REMOVE_ALL_SLOT,
        REMOVE_TAGGED,
        REMOVE_SLOT,
    }
}

internal fun Class<*>.allMethods(): Sequence<Method> =
    generateSequence(this) { clazz -> clazz.superclass }
        .flatMap { clazz -> clazz.declaredMethods.asSequence() }

internal fun Any.readField(name: String): Any? =
    optionalNativeRead {
        generateSequence(javaClass) { clazz -> clazz.superclass }
            .mapNotNull { clazz -> clazz.declaredFields.firstOrNull { it.name == name } }
            .firstOrNull()
            ?.apply { isAccessible = true }
            ?.get(this)
    }

