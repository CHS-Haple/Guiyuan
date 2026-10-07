package com.chaners.guiyuan.xposed

import android.view.View
import java.lang.reflect.Method

internal object BindableContractProbe {
    private const val BINDABLE_ICON =
        "com.android.systemui.statusbar.pipeline.icons.shared.model.BindableIcon"
    private const val MODERN_VIEW_CREATOR =
        "com.android.systemui.statusbar.pipeline.icons.shared.model.ModernStatusBarViewCreator"
    private const val BINDABLE_REGISTRY =
        "com.android.systemui.statusbar.pipeline.icons.shared.BindableIconsRegistryImpl"
    private const val BINDABLE_HOLDER =
        "com.android.systemui.statusbar.phone.StatusBarIconHolder\$BindableIconHolder"
    private const val MODERN_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView"
    private const val SINGLE_BINDABLE_VIEW =
        "com.android.systemui.statusbar.pipeline.shared.ui.view.SingleBindableStatusBarIconView"
    private const val MAX_RUNTIME_ENTRIES = 16
    private const val MAX_SLOT_ORDER_ENTRIES = 40

    fun inspect(host: Any): Snapshot {
        val resolution = NativeParticipantAccess.resolve(host)
        val handles =
            when (resolution) {
                is NativeParticipantAccess.ResolveResult.Ready ->
                    resolution.handles

                is NativeParticipantAccess.ResolveResult.Failure ->
                    return Snapshot.unavailable(resolution.reason)
            }

        val classLoader = handles.classLoader
        val bindableIconClass = classOrNull(BINDABLE_ICON, classLoader)
        val creatorClass = classOrNull(MODERN_VIEW_CREATOR, classLoader)
        val registryClass = classOrNull(BINDABLE_REGISTRY, classLoader)
        val holderClass = classOrNull(BINDABLE_HOLDER, classLoader)
        val modernViewClass = classOrNull(MODERN_STATUS_BAR_VIEW, classLoader)
        val singleBindableViewClass = classOrNull(SINGLE_BINDABLE_VIEW, classLoader)

        val bindableInterfaceReady =
            bindableIconClass != null &&
                bindableIconClass.hasMethod(
                    name = "getSlot",
                    parameterTypes = emptyList(),
                    returnType = String::class.java.name,
                ) &&
                bindableIconClass.hasMethod(
                    name = "getShouldBindIcon",
                    parameterTypes = emptyList(),
                    returnType = "boolean",
                ) &&
                bindableIconClass.hasMethod(
                    name = "getInitializer",
                    parameterTypes = emptyList(),
                    returnType = MODERN_VIEW_CREATOR,
                )

        val creatorReady =
            creatorClass?.hasMethod(
                name = "createAndBind",
                parameterTypes = listOf("android.content.Context"),
                returnType = MODERN_STATUS_BAR_VIEW,
            ) == true

        val registryConstructors =
            registryClass
                ?.declaredConstructors
                ?.map { constructor ->
                    constructor.parameterTypes.joinToString(
                        prefix = "(",
                        postfix = ")",
                    ) { type -> type.name }
                }
                ?.sorted()
                .orEmpty()

        val holderConstructors =
            holderClass
                ?.declaredConstructors
                ?.map { constructor ->
                    constructor.parameterTypes.joinToString(
                        prefix = "(",
                        postfix = ")",
                    ) { type -> type.name }
                }
                ?.sorted()
                .orEmpty()

        val managerBindableMap =
            readField(handles.manager, "mBindableIcons") as? Map<*, *>
        val managerEntries =
            managerBindableMap
                ?.entries
                ?.take(MAX_RUNTIME_ENTRIES)
                ?.map { entry ->
                    val key = entry.key?.toString() ?: "null"
                    val valueClass = entry.value?.javaClass?.name ?: "null"
                    key + ":" + valueClass
                }
                ?.sorted()
                .orEmpty()

        val statusBarIconList =
            readField(handles.controller, "mStatusBarIconList")
        val viewOnlySlotsCollection =
            statusBarIconList?.let {
                readField(it, "mViewOnlySlots")
            } as? Collection<*>
        val viewOnlySlots =
            viewOnlySlotsCollection
                ?.take(MAX_RUNTIME_ENTRIES)
                ?.mapNotNull { value -> value?.toString() }
                ?.sorted()
                .orEmpty()

        val runtimeBindableViews =
            if (modernViewClass == null) {
                emptyList()
            } else {
                buildList {
                    for (index in 0 until handles.group.childCount) {
                        val child = handles.group.getChildAt(index)
                        if (!modernViewClass.isInstance(child)) {
                            continue
                        }
                        val layoutParams = child.layoutParams
                        add(
                            "index=" + index +
                                ",class=" + child.javaClass.name +
                                ",slot=" + (slotOf(child) ?: "unknown") +
                                ",bounds=" +
                                child.left + "," + child.top + "-" +
                                child.right + "," + child.bottom +
                                ",size=" + child.width + "x" + child.height +
                                ",measured=" +
                                child.measuredWidth + "x" + child.measuredHeight +
                                ",layout=" +
                                (layoutParams?.width ?: Int.MIN_VALUE) + "x" +
                                (layoutParams?.height ?: Int.MIN_VALUE) +
                                ",visibility=" + visibilityName(child.visibility),
                        )
                        if (size >= MAX_RUNTIME_ENTRIES) {
                            break
                        }
                    }
                }
            }

        val runtimeSlotOrder =
            buildList {
                for (index in 0 until handles.group.childCount) {
                    val child = handles.group.getChildAt(index)
                    val slot = slotOf(child) ?: continue
                    val layoutParams = child.layoutParams
                    add(
                        "index=" + index +
                            ",slot=" + slot +
                            ",class=" + child.javaClass.name +
                            ",visibility=" + visibilityName(child.visibility) +
                            ",iconVisible=" +
                            (NativeParticipantAccess.iconVisible(child)
                                ?.toString() ?: "unknown") +
                            ",bounds=" +
                            child.left + "," + child.top + "-" +
                            child.right + "," + child.bottom +
                            ",size=" + child.width + "x" + child.height +
                            ",measured=" +
                            child.measuredWidth + "x" + child.measuredHeight +
                            ",layout=" +
                            (layoutParams?.width ?: Int.MIN_VALUE) + "x" +
                            (layoutParams?.height ?: Int.MIN_VALUE),
                    )
                    if (size >= MAX_SLOT_ORDER_ENTRIES) {
                        break
                    }
                }
            }

        val dynamicRegistrationObserved =
            managerEntries.any { entry -> entry.startsWith("combined_status_") } ||
                viewOnlySlots.any { slot -> slot.contains("combined_status_") }

        val staticContractReady =
            bindableInterfaceReady &&
                creatorReady &&
                modernViewClass != null &&
                singleBindableViewClass != null &&
                holderClass != null &&
                registryClass != null

        return Snapshot(
            available = true,
            reason = null,
            bindableInterfaceReady = bindableInterfaceReady,
            creatorReady = creatorReady,
            registryClass = registryClass?.name,
            registryConstructors = registryConstructors,
            holderClass = holderClass?.name,
            holderConstructors = holderConstructors,
            modernViewClass = modernViewClass?.name,
            singleBindableViewClass = singleBindableViewClass?.name,
            managerBindableMapReady = managerBindableMap != null,
            managerBindableCount = managerBindableMap?.size ?: -1,
            managerBindableEntries = managerEntries,
            viewOnlySlotsReady = viewOnlySlotsCollection != null,
            viewOnlySlots = viewOnlySlots,
            runtimeBindableViews = runtimeBindableViews,
            runtimeSlotOrder = runtimeSlotOrder,
            groupClipChildren = handles.group.clipChildren,
            groupClipToPadding = handles.group.clipToPadding,
            groupHeight = handles.group.height,
            staticContractReady = staticContractReady,
            dynamicRegistrationObserved = dynamicRegistrationObserved,
        )
    }

    private fun classOrNull(
        name: String,
        classLoader: ClassLoader,
    ): Class<*>? =
        runCatching {
            Class.forName(name, false, classLoader)
        }.getOrNull()

    private fun readField(
        target: Any,
        name: String,
    ): Any? {
        val field =
            generateSequence(target.javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields
                        .firstOrNull { candidate -> candidate.name == name }
                }
                .firstOrNull()
                ?: return null

        return runCatching {
            field.isAccessible = true
            field.get(target)
        }.getOrNull()
    }

    private fun slotOf(view: View): String? {
        val accessor =
            generateSequence<Class<*>>(view.javaClass) { clazz -> clazz.superclass }
                .flatMap { clazz -> clazz.declaredMethods.asSequence() }
                .firstOrNull { method ->
                    method.name == "getSlot" &&
                        method.parameterCount == 0 &&
                        method.returnType == String::class.java
                }
                ?: return null

        return runCatching {
            accessor.isAccessible = true
            accessor.invoke(view) as? String
        }.getOrNull()
    }

    private fun visibilityName(visibility: Int): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }

    private fun Class<*>.hasMethod(
        name: String,
        parameterTypes: List<String>,
        returnType: String,
    ): Boolean =
        allMethods().any { method ->
            method.name == name &&
                method.parameterTypes.map { type -> type.name } == parameterTypes &&
                method.returnType.name == returnType
        }

    private fun Class<*>.allMethods(): Sequence<Method> =
        generateSequence(this) { clazz -> clazz.superclass }
            .flatMap { clazz -> clazz.declaredMethods.asSequence() }

    internal data class Snapshot(
        val available: Boolean,
        val reason: String?,
        val bindableInterfaceReady: Boolean,
        val creatorReady: Boolean,
        val registryClass: String?,
        val registryConstructors: List<String>,
        val holderClass: String?,
        val holderConstructors: List<String>,
        val modernViewClass: String?,
        val singleBindableViewClass: String?,
        val managerBindableMapReady: Boolean,
        val managerBindableCount: Int,
        val managerBindableEntries: List<String>,
        val viewOnlySlotsReady: Boolean,
        val viewOnlySlots: List<String>,
        val runtimeBindableViews: List<String>,
        val runtimeSlotOrder: List<String>,
        val groupClipChildren: Boolean,
        val groupClipToPadding: Boolean,
        val groupHeight: Int,
        val staticContractReady: Boolean,
        val dynamicRegistrationObserved: Boolean,
    ) {
        companion object {
            fun unavailable(reason: String): Snapshot =
                Snapshot(
                    available = false,
                    reason = reason,
                    bindableInterfaceReady = false,
                    creatorReady = false,
                    registryClass = null,
                    registryConstructors = emptyList(),
                    holderClass = null,
                    holderConstructors = emptyList(),
                    modernViewClass = null,
                    singleBindableViewClass = null,
                    managerBindableMapReady = false,
                    managerBindableCount = -1,
                    managerBindableEntries = emptyList(),
                    viewOnlySlotsReady = false,
                    viewOnlySlots = emptyList(),
                    runtimeBindableViews = emptyList(),
                    runtimeSlotOrder = emptyList(),
                    groupClipChildren = true,
                    groupClipToPadding = true,
                    groupHeight = -1,
                    staticContractReady = false,
                    dynamicRegistrationObserved = false,
                )
        }
    }
}
