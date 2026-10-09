package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

internal object NativeParticipantAccess {
    const val PHONE_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"
    const val ICON_HOLDER =
        "com.android.systemui.statusbar.phone.StatusBarIconHolder"
    const val STATUS_BAR_ICON_VIEW =
        "com.android.systemui.statusbar.StatusBarIconView"
    const val STATUS_ICON_DISPLAYABLE =
        "com.android.systemui.statusbar.StatusIconDisplayable"
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
                statusBarView = statusBarView,
                manager = manager,
                group = group,
                controller = controller.value,
                controllerSource = controller.source,
                classLoader = classLoader,
                holderClass = classOrNull(ICON_HOLDER, classLoader),
                iconViewClass = classOrNull(STATUS_BAR_ICON_VIEW, classLoader),
                displayableClass = classOrNull(STATUS_ICON_DISPLAYABLE, classLoader),
            ),
        )
    }

    fun resourceSetter(controllerClass: Class<*>): ResourceSetter? =
        controllerClass
            .allMethods()
            .filter { method -> method.name == "setIcon" }
            .mapNotNull { method ->
                classifyResourceSetIcon(method)?.let { mode ->
                    ResourceSetter(method = method, mode = mode)
                }
            }
            .firstOrNull()

    fun setIconHolderAvailable(controllerClass: Class<*>): Boolean =
        controllerClass.hasMethod(
            name = "setIcon",
            parameterTypes =
                listOf(
                    "java.lang.String",
                    ICON_HOLDER,
                ),
        )

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

    fun invokeSetIconHolder(
        handles: Handles,
        slot: String,
        holder: Any,
    ) {
        val holderClass =
            handles.holderClass
                ?: error("status-icon-holder-class-missing")
        check(holderClass.isInstance(holder)) {
            "status-icon-holder-type-mismatch"
        }
        val setter =
            handles.controller.javaClass.findMethod(
                name = "setIcon",
                parameterTypes = listOf("java.lang.String", ICON_HOLDER),
            ) ?: error("set-icon-holder-method-missing")
        setter.isAccessible = true
        setter.invoke(handles.controller, slot, holder)
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

    fun visibilityMethod(controllerClass: Class<*>): Method? =
        controllerClass.findMethod(
            name = "setIconVisibility",
            parameterTypes =
                listOf(
                    "java.lang.String",
                    "boolean",
                ),
        )

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

    fun classifyResourceSetIcon(method: Method): ResourceSetIconMode? {
        if (method.name != "setIcon" || method.parameterCount != 3) {
            return null
        }
        val params = method.parameterTypes
        val intType = Int::class.javaPrimitiveType
        val firstIsText = CharSequence::class.java.isAssignableFrom(params[0])
        val thirdIsText = CharSequence::class.java.isAssignableFrom(params[2])

        return when {
            firstIsText &&
                params[1] == String::class.java &&
                params[2] == intType ->
                ResourceSetIconMode.CONTENT_SLOT_RES

            params[0] == String::class.java &&
                params[1] == intType &&
                thirdIsText ->
                ResourceSetIconMode.SLOT_RES_CONTENT

            else -> null
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

    fun invokeCreate(
        handles: Handles,
        setter: ResourceSetter,
        slot: String,
        resourceId: Int,
        contentDescription: CharSequence,
    ) {
        setter.method.isAccessible = true
        when (setter.mode) {
            ResourceSetIconMode.CONTENT_SLOT_RES ->
                setter.method.invoke(
                    handles.controller,
                    contentDescription,
                    slot,
                    resourceId,
                )

            ResourceSetIconMode.SLOT_RES_CONTENT ->
                setter.method.invoke(
                    handles.controller,
                    slot,
                    resourceId,
                    contentDescription,
                )
        }
    }

    fun invokeVisibility(
        handles: Handles,
        method: Method,
        slot: String,
        visible: Boolean,
    ) {
        method.isAccessible = true
        method.invoke(handles.controller, slot, visible)
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

    fun iconVisible(view: View): Boolean? {
        val accessor =
            view.javaClass
                .allMethods()
                .firstOrNull { method ->
                    method.name == "isIconVisible" &&
                        method.parameterCount == 0 &&
                        (
                            method.returnType == Boolean::class.javaPrimitiveType ||
                                method.returnType == Boolean::class.java
                        )
                }

        val viaAccessor =
            accessor?.let { method ->
                runCatching {
                    method.isAccessible = true
                    method.invoke(view) as? Boolean
                }.getOrNull()
            }
        if (viaAccessor != null) {
            return viaAccessor
        }

        val icon = view.readField("mIcon") ?: return null
        return icon.readField("visible") as? Boolean
    }

    fun methodSignatures(
        clazz: Class<*>,
        names: Set<String>,
    ): List<String> =
        clazz
            .allMethods()
            .filter { method -> method.name in names }
            .distinctBy(::methodSignature)
            .map(::methodSignature)
            .sorted()
            .toList()

    fun holderFactories(holderClass: Class<*>?): List<Method> {
        val clazz = holderClass ?: return emptyList()
        return clazz
            .declaredMethods
            .filter { method ->
                java.lang.reflect.Modifier.isStatic(method.modifiers) &&
                    clazz.isAssignableFrom(method.returnType)
            }
            .sortedBy(::methodSignature)
            .toList()
    }

    fun hasMethodSignature(
        clazz: Class<*>?,
        name: String,
        parameterTypes: List<String>,
    ): Boolean =
        clazz?.hasMethod(name, parameterTypes) == true

    fun methodSignature(method: Method): String =
        method.name +
            "(" +
            method.parameterTypes.joinToString(",") { type -> type.name } +
            "):" +
            method.returnType.name

    fun resourceEntryName(view: View): String? {
        if (view.id == View.NO_ID) {
            return null
        }
        return runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrNull()
    }

    private fun Class<*>.findMethod(
        name: String,
        parameterTypes: List<String>,
    ): Method? =
        allMethods().firstOrNull { method ->
            method.name == name &&
                method.parameterTypes.map { type -> type.name } == parameterTypes
        }

    private fun Class<*>.hasMethod(
        name: String,
        parameterTypes: List<String>,
    ): Boolean = findMethod(name, parameterTypes) != null

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
    ): ControllerRef? {
        val observedController =
            NativeParticipantRuntime.controllerFor(manager)
        if (observedController != null) {
            return ControllerRef(
                value = observedController,
                source = "add-icon-group-observer",
            )
        }

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
                        return ControllerRef(
                            value = controller,
                            source = "dependency",
                        )
                    }
                }
            }
        }

        val legacyController = manager.readField("mController")
        if (legacyController != null) {
            return ControllerRef(
                value = legacyController,
                source = "manager-field",
            )
        }

        return null
    }

    private fun classOrNull(
        name: String,
        classLoader: ClassLoader,
    ): Class<*>? =
        runCatching {
            Class.forName(name, false, classLoader)
        }.getOrNull()

    internal data class Handles(
        val statusBarView: View,
        val manager: Any,
        val group: ViewGroup,
        val controller: Any,
        val controllerSource: String,
        val classLoader: ClassLoader,
        val holderClass: Class<*>?,
        val iconViewClass: Class<*>?,
        val displayableClass: Class<*>?,
    )

    internal sealed interface ResolveResult {
        data class Ready(
            val handles: Handles,
        ) : ResolveResult

        data class Failure(
            val reason: String,
        ) : ResolveResult
    }

    private data class ControllerRef(
        val value: Any,
        val source: String,
    )

    internal data class ResourceSetter(
        val method: Method,
        val mode: ResourceSetIconMode,
    )

    internal data class Removal(
        val method: Method,
        val mode: RemovalMode,
    )

    internal enum class ResourceSetIconMode {
        CONTENT_SLOT_RES,
        SLOT_RES_CONTENT,
    }

    internal enum class RemovalMode {
        REMOVE_ALL_SLOT_PIPELINE_FLAG,
        REMOVE_ALL_SLOT,
        REMOVE_TAGGED,
        REMOVE_SLOT,
    }
}
