package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

internal object NativeParticipantAccess {
    const val PHONE_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"
    private const val DEPENDENCY =
        "com.android.systemui.Dependency"
    private val STATUS_BAR_ICON_CONTROLLER_CANDIDATES =
        listOf(
            "com.android.systemui.statusbar.phone.ui.StatusBarIconController",
            "com.android.systemui.statusbar.phone.StatusBarIconController",
        )

    // Status-icon children are queried from native onLayout hot paths. Resolve the
    // reflective slot getter once per concrete SystemUI view class instead of
    // walking the method hierarchy for every child on every layout.
    private val slotAccessorCache = ConcurrentHashMap<Class<*>, Method>()
    private val slotAccessorMissing = ConcurrentHashMap.newKeySet<Class<*>>()

    fun managerFor(host: Any): Any? {
        val hostView = host as? View ?: return null
        val statusBarView =
            generateSequence(hostView) { view -> view.parent as? View }
                .firstOrNull { view -> view.javaClass.name == PHONE_STATUS_BAR_VIEW }
                ?: return null
        return statusBarView.readField("mDarkIconManager")
    }

    fun groupFor(host: Any): ViewGroup? =
        managerFor(host)?.readField("mGroup") as? ViewGroup

    fun resolve(host: Any): ResolveResult {
        val hostView = host as? View
            ?: return ResolveResult.Failure("host-not-view")

        val statusBarView =
            generateSequence(hostView) { view -> view.parent as? View }
                .firstOrNull { view -> view.javaClass.name == PHONE_STATUS_BAR_VIEW }
                ?: return ResolveResult.Failure("phone-status-bar-view-missing")

        val classLoader = statusBarView.javaClass.classLoader
            ?: return ResolveResult.Failure("systemui-classloader-missing")
        val manager = statusBarView.readField("mDarkIconManager")
            ?: return ResolveResult.Failure("dark-icon-manager-missing")
        val group = manager.readField("mGroup") as? ViewGroup
            ?: return ResolveResult.Failure("status-icon-group-missing")
        val controller =
            resolveStatusBarIconController(
                classLoader = classLoader,
                manager = manager,
            ) ?: return ResolveResult.Failure("status-icon-controller-missing")

        return ResolveResult.Ready(
            Handles(
                manager = manager,
                group = group,
                controller = controller,
            ),
        )
    }

    fun iconHolder(
        handles: Handles,
        slot: String,
        tag: Int = 0,
    ): Any? {
        val iconList =
            handles.controller.readField("mStatusBarIconList")
                ?: return null
        val getter =
            iconList.javaClass
                .allMethods()
                .firstOrNull { method ->
                    method.name == "getIconHolder" &&
                        method.parameterTypes.map { type -> type.name } ==
                            listOf("int", "java.lang.String")
                }
                ?: return null
        return runCatching {
            getter.isAccessible = true
            getter.invoke(iconList, tag, slot)
        }.getOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    fun clearBindableEntries(
        handles: Handles,
        slot: String,
        expectedHolder: Any? = null,
    ): Int {
        val managers =
            handles.controller.readField("mIconGroups") as? Iterable<*>
                ?: return 0
        var removed = 0
        managers.forEach { manager ->
            manager ?: return@forEach
            val map =
                manager.readField("mBindableIcons") as? MutableMap<Any?, Any?>
                    ?: return@forEach
            val current = map[slot]
            if (current != null && (expectedHolder == null || current === expectedHolder)) {
                map.remove(slot)
                removed += 1
            }
        }
        return removed
    }

    fun removal(controllerClass: Class<*>): Removal? {
        val candidates =
            controllerClass
                .allMethods()
                .mapNotNull { method ->
                    classifyRemoval(method)?.let { mode ->
                        Removal(method = method, mode = mode)
                    }
                }
                .toList()

        return candidates.minByOrNull { candidate ->
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
                params == listOf("java.lang.String", "boolean") ->
                RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG

            method.name == "removeAllIconsForSlot" &&
                params == listOf("java.lang.String") ->
                RemovalMode.REMOVE_ALL_SLOT

            method.name == "removeIcon" &&
                params == listOf("java.lang.String", "int") ->
                RemovalMode.REMOVE_TAGGED

            method.name == "removeIcon" &&
                params == listOf("java.lang.String") ->
                RemovalMode.REMOVE_SLOT

            else -> null
        }
    }

    fun invokeRemoval(
        handles: Handles,
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
                    if (viewClass in slotAccessorMissing) {
                        return null
                    }
                    val resolved =
                        viewClass
                            .allMethods()
                            .firstOrNull { method ->
                                method.name == "getSlot" &&
                                    method.parameterCount == 0 &&
                                    method.returnType == String::class.java
                            }
                    if (resolved == null) {
                        slotAccessorMissing += viewClass
                        return null
                    }
                    resolved.isAccessible = true
                    slotAccessorCache.putIfAbsent(viewClass, resolved) ?: resolved
                }

        return runCatching {
            getSlot.invoke(view) as? String
        }.getOrNull()
    }

    fun resourceEntryName(view: View): String? {
        if (view.id == View.NO_ID) {
            return null
        }
        return runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrNull()
    }

    private fun Class<*>.allMethods(): Sequence<Method> =
        generateSequence(this) { clazz -> clazz.superclass }
            .flatMap { clazz -> clazz.declaredMethods.asSequence() }

    private fun Any.readField(name: String): Any? {
        val field =
            generateSequence(javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz -> clazz.declaredFields.firstOrNull { it.name == name } }
                .firstOrNull()
                ?: return null
        return runCatching {
            field.isAccessible = true
            field.get(this)
        }.getOrNull()
    }

    private fun resolveStatusBarIconController(
        classLoader: ClassLoader,
        manager: Any,
    ): Any? {
        val dependencyClass = classOrNull(DEPENDENCY, classLoader)
        if (dependencyClass != null) {
            val getMethod =
                dependencyClass
                    .allMethods()
                    .firstOrNull { method ->
                        method.name == "get" &&
                            java.lang.reflect.Modifier.isStatic(method.modifiers) &&
                            method.parameterTypes.contentEquals(
                                arrayOf<Class<*>>(Class::class.java),
                            )
                    }

            if (getMethod != null) {
                STATUS_BAR_ICON_CONTROLLER_CANDIDATES.forEach { className ->
                    val controllerType =
                        classOrNull(className, classLoader)
                            ?: return@forEach
                    val controller =
                        runCatching {
                            getMethod.isAccessible = true
                            getMethod.invoke(null, controllerType)
                        }.getOrNull()
                    if (controller != null) {
                        return controller
                    }
                }
            }
        }

        return manager.readField("mController")
    }

    private fun classOrNull(
        name: String,
        classLoader: ClassLoader,
    ): Class<*>? =
        runCatching {
            Class.forName(name, false, classLoader)
        }.getOrNull()

    internal data class Handles(
        val manager: Any,
        val group: ViewGroup,
        val controller: Any,
    )

    internal sealed interface ResolveResult {
        data class Ready(
            val handles: Handles,
        ) : ResolveResult

        data class Failure(
            val reason: String,
        ) : ResolveResult
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
