package com.chaners.guiyuan.xposed.network

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.chaners.guiyuan.xposed.StatusStateStore
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.WeakHashMap

internal object SysUiNetworkSource {
    const val WIFI_BINDER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.binder.MiuiWifiViewBinder"
    const val WIFI_BIND_METHOD_NAME = "bind"
    const val WIFI_ICON_EMITTER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.binder.MiuiWifiViewBinder\$bind\$1\$1\$2\$1"
    const val WIFI_ICON_EMIT_METHOD_NAME = "emit"
    const val WIFI_LOCATION_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.LocationBasedWifiViewModel"
    private const val WIFI_ICON_VISIBLE_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.model.WifiIcon\$Visible"
    private const val ICON_RESOURCE_CLASS_NAME =
        "com.android.systemui.common.shared.model.Icon\$Resource"
    private const val WIFI_ICON_HIDDEN_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.model.WifiIcon\$Hidden"
    const val HOME_WIFI_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.HomeWifiViewModel"

    const val MOBILE_BINDER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder"
    const val MOBILE_BIND_METHOD_NAME = "bind"
    const val MOBILE_LOCATION_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.LocationBasedMobileViewModel"
    const val HOME_MOBILE_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.HomeMobileIconViewModel"
    const val MOBILE_ICON_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconViewModel"
    const val MOBILE_VIEW_LOGGER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.MobileViewLogger"
    const val MOBILE_SIGNAL_EMITTER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder\$bind\$1\$1\$4\$2"
    const val MOBILE_SIGNAL_EMIT_METHOD_NAME = "emit"

    const val HOOK_COUNT = 4

    private const val WIFI_BIND_HOOK_ID = "combinedstatus.network.wifi.bind"
    private const val WIFI_ICON_HOOK_ID = "combinedstatus.network.wifi.icon"
    private const val WIFI_ICON_COLLECTOR_CLASS_ID = 1
    private const val MAX_PARENT_CHAIN_DEPTH = 8
    private const val MOBILE_BIND_HOOK_ID = "combinedstatus.network.mobile.bind"
    private const val MOBILE_SIGNAL_HOOK_ID = "combinedstatus.network.mobile.signal"

    private val hookIds = setOf(
        WIFI_BIND_HOOK_ID,
        WIFI_ICON_HOOK_ID,
        MOBILE_BIND_HOOK_ID,
        MOBILE_SIGNAL_HOOK_ID,
    )

    private val wifiRoots = WeakHashMap<ViewGroup, Any?>()
    private val mobileRoots = WeakHashMap<ViewGroup, Int>()
    private val lastWifiEvents = WeakHashMap<ImageView, String>()
    private val lastWifiTaggedResources = WeakHashMap<ImageView, Int?>()
    private val lastMobileEvents = WeakHashMap<ImageView, String>()

    @Volatile
    private var wifiSeedContract: WifiSeedContract? = null

    internal data class InstallFailure(
        val component: String,
        val stage: String,
        val errorType: String,
        val reason: String,
    )

    internal data class MobilePresentationBinding(
        val root: ViewGroup,
        val subscriptionId: Int,
    )

    internal data class BindingRestoreResult(
        val wifiRoots: Int,
        val mobileRoots: Int,
    )

    internal data class InstallResult(
        val handles: List<HookHandle>,
        val wifiReady: Boolean,
        val mobileReady: Boolean,
        val failures: List<InstallFailure>,
    )

    private data class BranchInstallResult(
        val handles: List<HookHandle>,
        val ready: Boolean,
        val failure: InstallFailure?,
    )

    private data class WifiSeedContract(
        val wifiIconGetter: Method?,
        val wifiVisibleIconField: Field,
        val iconResourceIdAccessor: IconResourceIdAccessor,
    )

    private data class IconResourceIdAccessor(
        val method: Method? = null,
        val field: Field? = null,
        val description: String,
    ) {
        fun read(target: Any): Int? =
            when {
                method != null ->
                    (method.invoke(target) as? Number)
                        ?.toInt()
                        ?.takeIf { it != 0 }
                field != null ->
                    (field.get(target) as? Number)
                        ?.toInt()
                        ?.takeIf { it != 0 }
                else -> null
            }
    }

    private data class WifiSemanticValue(
        val state: StatusStateStore.WifiState?,
        val resourceId: Int?,
        val resourceName: String?,
        val valueType: String,
    )

    private class InstallStageException(
        val stage: String,
        cause: Throwable,
    ) : RuntimeException(cause)

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onWifiState: (StatusStateStore.WifiState) -> Unit,
        onMobileIcon: (StatusStateStore.MobileIconUpdate) -> Unit,
        onMobileSignalWillApply: ((ImageView) -> Unit)?,
        onPresentationChanged: (() -> Unit)?,
        onEvent: ((String) -> Unit)?,
    ): InstallResult {
        val wifi =
            installWifi(
                module = module,
                classLoader = classLoader,
                onWifiState = onWifiState,
                onEvent = onEvent,
            )
        val mobile =
            installMobile(
                module = module,
                classLoader = classLoader,
                onMobileIcon = onMobileIcon,
                onMobileSignalWillApply = onMobileSignalWillApply,
                onPresentationChanged = onPresentationChanged,
                onEvent = onEvent,
            )

        return InstallResult(
            handles = wifi.handles + mobile.handles,
            wifiReady = wifi.ready,
            mobileReady = mobile.ready,
            failures = listOfNotNull(wifi.failure, mobile.failure),
        )
    }

    private fun installWifi(
        module: XposedModule,
        classLoader: ClassLoader,
        onWifiState: (StatusStateStore.WifiState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): BranchInstallResult {
        val created = mutableListOf<HookHandle>()

        return try {
            val wifiBinderClass =
                atStage("wifi.resolve.binderClass") {
                    Class.forName(WIFI_BINDER_CLASS_NAME, false, classLoader)
                }
            val wifiLocationVmClass =
                atStage("wifi.resolve.locationViewModelClass") {
                    Class.forName(WIFI_LOCATION_VIEW_MODEL_CLASS_NAME, false, classLoader)
                }
            val wifiBindMethod =
                atStage("wifi.resolve.bindMethod") {
                    wifiBinderClass.getDeclaredMethod(
                        WIFI_BIND_METHOD_NAME,
                        ViewGroup::class.java,
                        wifiLocationVmClass,
                    )
                }
            val wifiVisibleClass =
                atStage("wifi.resolve.visibleClass") {
                    Class.forName(WIFI_ICON_VISIBLE_CLASS_NAME, false, classLoader)
                }
            val iconResourceClass =
                atStage("wifi.resolve.iconResourceClass") {
                    Class.forName(ICON_RESOURCE_CLASS_NAME, false, classLoader)
                }
            val wifiVisibleIconField =
                atStage("wifi.resolve.visibleIconField") {
                    wifiVisibleClass
                        .getDeclaredField("icon")
                        .apply { isAccessible = true }
                }
            val iconResourceIdAccessor =
                atStage("wifi.resolve.iconResourceIdAccessor") {
                    resolveIconResourceIdAccessor(iconResourceClass)
                }
            val wifiIconEmitterClass =
                atStage("wifi.resolve.iconEmitterClass") {
                    Class.forName(WIFI_ICON_EMITTER_CLASS_NAME, false, classLoader)
                }
            val wifiIconEmitMethod =
                atStage("wifi.resolve.iconEmitMethod") {
                    resolveEmitterMethod(
                        emitterClass = wifiIconEmitterClass,
                        methodName = WIFI_ICON_EMIT_METHOD_NAME,
                    )
                }
            val wifiIconImageField =
                atStage("wifi.resolve.iconViewField") {
                    wifiIconEmitterClass
                        .getDeclaredField("\$iconView")
                        .apply { isAccessible = true }
                }
            val wifiIconClassIdField =
                atStage("wifi.resolve.classIdField") {
                    wifiIconEmitterClass
                        .getDeclaredField("\$r8\$classId")
                        .apply { isAccessible = true }
                }
            val wifiIconGetter =
                wifiLocationVmClass.methods
                    .firstOrNull { method ->
                        method.name == "getWifiIcon" &&
                            method.parameterCount == 0
                    }
                    ?.apply { isAccessible = true }
            val seedContract =
                WifiSeedContract(
                    wifiIconGetter = wifiIconGetter,
                    wifiVisibleIconField = wifiVisibleIconField,
                    iconResourceIdAccessor = iconResourceIdAccessor,
                )
            wifiSeedContract = seedContract

            created +=
                atStage("wifi.hook.bind") {
                    module
                        .hook(wifiBindMethod)
                        .setId(WIFI_BIND_HOOK_ID)
                        .intercept(
                            wifiBindHooker(
                                seedContract = seedContract,
                                onWifiState = onWifiState,
                                onEvent = onEvent,
                            ),
                        )
                }
            created +=
                atStage("wifi.hook.iconEmit") {
                    module
                        .hook(wifiIconEmitMethod)
                        .setId(WIFI_ICON_HOOK_ID)
                        .intercept(
                            wifiIconHooker(
                                wifiImageField = wifiIconImageField,
                                wifiClassIdField = wifiIconClassIdField,
                                wifiVisibleIconField = wifiVisibleIconField,
                                iconResourceIdAccessor = iconResourceIdAccessor,
                                onWifiState = onWifiState,
                                onEvent = onEvent,
                            ),
                        )
                }

            BranchInstallResult(
                handles = created.toList(),
                ready = true,
                failure = null,
            )
        } catch (error: InstallStageException) {
            created.forEach { handle -> runCatching { handle.unhook() } }
            wifiSeedContract = null
            BranchInstallResult(
                handles = emptyList(),
                ready = false,
                failure = installFailure("wifi", error),
            )
        }
    }

    private fun installMobile(
        module: XposedModule,
        classLoader: ClassLoader,
        onMobileIcon: (StatusStateStore.MobileIconUpdate) -> Unit,
        onMobileSignalWillApply: ((ImageView) -> Unit)?,
        onPresentationChanged: (() -> Unit)?,
        onEvent: ((String) -> Unit)?,
    ): BranchInstallResult {
        val created = mutableListOf<HookHandle>()

        return try {
            val mobileBinderClass =
                atStage("mobile.resolve.binderClass") {
                    Class.forName(MOBILE_BINDER_CLASS_NAME, false, classLoader)
                }
            val mobileLocationVmClass =
                atStage("mobile.resolve.locationViewModelClass") {
                    Class.forName(MOBILE_LOCATION_VIEW_MODEL_CLASS_NAME, false, classLoader)
                }
            val mobileIconVmClass =
                atStage("mobile.resolve.iconViewModelClass") {
                    Class.forName(MOBILE_ICON_VIEW_MODEL_CLASS_NAME, false, classLoader)
                }
            val mobileLoggerClass =
                atStage("mobile.resolve.loggerClass") {
                    Class.forName(MOBILE_VIEW_LOGGER_CLASS_NAME, false, classLoader)
                }
            val mobileBindMethod =
                atStage("mobile.resolve.bindMethod") {
                    mobileBinderClass.getDeclaredMethod(
                        MOBILE_BIND_METHOD_NAME,
                        ViewGroup::class.java,
                        mobileLocationVmClass,
                        mobileIconVmClass,
                        mobileLoggerClass,
                    )
                }
            val mobileSignalEmitterClass =
                atStage("mobile.resolve.signalEmitterClass") {
                    Class.forName(MOBILE_SIGNAL_EMITTER_CLASS_NAME, false, classLoader)
                }
            val mobileSignalEmitMethod =
                atStage("mobile.resolve.signalEmitMethod") {
                    resolveEmitterMethod(
                        emitterClass = mobileSignalEmitterClass,
                        methodName = MOBILE_SIGNAL_EMIT_METHOD_NAME,
                    )
                }
            val mobileImageField =
                atStage("mobile.resolve.imageField") {
                    mobileSignalEmitterClass
                        .getDeclaredField("\$mobile")
                        .apply { isAccessible = true }
                }
            val mobileClassIdField =
                atStage("mobile.resolve.classIdField") {
                    mobileSignalEmitterClass
                        .getDeclaredField("\$r8\$classId")
                        .apply { isAccessible = true }
                }
            val subscriptionIdMethod =
                atStage("mobile.resolve.subscriptionIdMethod") {
                    mobileLocationVmClass
                        .getDeclaredMethod("getSubscriptionId")
                        .apply { isAccessible = true }
                }

            created +=
                atStage("mobile.hook.bind") {
                    module
                        .hook(mobileBindMethod)
                        .setId(MOBILE_BIND_HOOK_ID)
                        .intercept(
                            mobileBindHooker(
                                subscriptionIdMethod = subscriptionIdMethod,
                                onPresentationChanged = onPresentationChanged,
                                onEvent = onEvent,
                            ),
                        )
                }
            created +=
                atStage("mobile.hook.signalEmit") {
                    module
                        .hook(mobileSignalEmitMethod)
                        .setId(MOBILE_SIGNAL_HOOK_ID)
                        .intercept(
                            mobileSignalHooker(
                                mobileImageField = mobileImageField,
                                mobileClassIdField = mobileClassIdField,
                                onMobileIcon = onMobileIcon,
                                onMobileSignalWillApply = onMobileSignalWillApply,
                                onEvent = onEvent,
                            ),
                        )
                }

            BranchInstallResult(
                handles = created.toList(),
                ready = true,
                failure = null,
            )
        } catch (error: InstallStageException) {
            created.forEach { handle -> runCatching { handle.unhook() } }
            BranchInstallResult(
                handles = emptyList(),
                ready = false,
                failure = installFailure("mobile", error),
            )
        }
    }

    private fun resolveEmitterMethod(
        emitterClass: Class<*>,
        methodName: String,
    ): Method {
        val candidates =
            emitterClass.declaredMethods.filter { method ->
                method.name == methodName &&
                    method.parameterCount == 2 &&
                    method.parameterTypes.firstOrNull() == Any::class.java
            }

        val method =
            when (candidates.size) {
                1 -> candidates.single()
                0 -> throw NoSuchMethodException(
                    emitterClass.name + "#" + methodName + "(Object, <continuation>)",
                )
                else -> throw NoSuchMethodException(
                    emitterClass.name + "#" + methodName +
                        " is ambiguous candidates=" +
                        candidates.joinToString(",") { candidate -> candidate.toGenericString() },
                )
            }

        return method.apply { isAccessible = true }
    }

    @Synchronized
    fun resetEventState() {
        lastWifiEvents.clear()
        lastWifiTaggedResources.clear()
        lastMobileEvents.clear()
        wifiSeedContract = null
    }

    @Synchronized
    fun exportHotReloadBindings(): Array<Any?> {
        val wifi = ArrayList<Any>(wifiRoots.size)
        wifiRoots.forEach { (root, viewModel) ->
            wifi.add(arrayOf(root, viewModel))
        }

        val mobile = ArrayList<Any>(mobileRoots.size)
        mobileRoots.forEach { (root, subscriptionId) ->
            mobile.add(arrayOf(root, subscriptionId))
        }

        return arrayOf(wifi, mobile)
    }

    @Synchronized
    fun restoreHotReloadBindings(raw: Any?): BindingRestoreResult {
        wifiRoots.clear()
        mobileRoots.clear()
        lastWifiEvents.clear()
        lastWifiTaggedResources.clear()
        lastMobileEvents.clear()

        val payload = raw as? Array<*>
            ?: return BindingRestoreResult(wifiRoots = 0, mobileRoots = 0)

        val wifi = payload.getOrNull(0) as? List<*>
        wifi.orEmpty().forEach { value ->
            val pair = value as? Array<*>
            val root =
                (pair?.getOrNull(0) as? ViewGroup)
                    ?: (value as? ViewGroup)
                    ?: return@forEach
            val viewModel =
                pair
                    ?.getOrNull(1)
                    ?.takeIf { candidate ->
                        candidate.javaClass.name == HOME_WIFI_VIEW_MODEL_CLASS_NAME
                    }
            if (root.isAttachedToWindow) {
                wifiRoots[root] = viewModel
            }
        }

        val mobile = payload.getOrNull(1) as? List<*>
        mobile.orEmpty().forEach { value ->
            val pair = value as? Array<*> ?: return@forEach
            val root = pair.getOrNull(0) as? ViewGroup ?: return@forEach
            val subscriptionId = (pair.getOrNull(1) as? Number)?.toInt() ?: return@forEach
            if (root.isAttachedToWindow) {
                mobileRoots[root] = subscriptionId
            }
        }

        return BindingRestoreResult(
            wifiRoots = wifiRoots.size,
            mobileRoots = mobileRoots.size,
        )
    }

    @Synchronized
    fun hotReloadBindingCounts(): Pair<Int, Int> =
        wifiRoots.size to mobileRoots.size

    @Synchronized
    fun mobilePresentationBindings(): List<MobilePresentationBinding> =
        mobileRoots.mapNotNull { (root, subscriptionId) ->
            if (root.isAttachedToWindow) {
                MobilePresentationBinding(root, subscriptionId)
            } else {
                null
            }
        }

    @Synchronized
    fun bindingTopologyLines(): List<String> =
        buildList {
            wifiRoots.keys.forEach { root ->
                add(
                    "nativeSlot binding=wifi root=" + root.javaClass.simpleName +
                        " rootId=" + resourceId(root) +
                        " parentChain=" + parentChain(root) +
                        " layout=" + layoutToken(root) +
                        "",
                )
            }
            mobileRoots.forEach { (root, subscriptionId) ->
                add(
                    "nativeSlot binding=mobile subId=" + subscriptionId +
                        " root=" + root.javaClass.simpleName +
                        " rootId=" + resourceId(root) +
                        " parentChain=" + parentChain(root) +
                        " layout=" + layoutToken(root) +
                        "",
                )
            }
        }

    private inline fun <T> atStage(
        stage: String,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (error: Throwable) {
            throw InstallStageException(stage, error)
        }

    private fun installFailure(
        component: String,
        error: InstallStageException,
    ): InstallFailure {
        val cause = error.cause ?: error
        return InstallFailure(
            component = component,
            stage = error.stage,
            errorType = cause.javaClass.name,
            reason = cause.message ?: cause.javaClass.simpleName,
        )
    }

    fun matches(handle: HookHandle): Boolean = handle.id in hookIds

    private fun wifiBindHooker(
        seedContract: WifiSeedContract,
        onWifiState: (StatusStateStore.WifiState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): Hooker = Hooker { chain ->
        val root = chain.getArg(0) as? ViewGroup
        val viewModel = chain.getArg(1)

        if (
            root != null &&
            viewModel?.javaClass?.name == HOME_WIFI_VIEW_MODEL_CLASS_NAME
        ) {
            val previous = synchronized(this) {
                wifiRoots.put(root, viewModel)
            }
            val bindingChanged = previous !== viewModel
            if (bindingChanged) {
                onEvent?.invoke(
                    "networkPipeline wifi bound " +
                        "stage=beforeProceed " +
                        "root=" + root.javaClass.simpleName +
                        " rootId=" + resourceId(root) +
                        " vm=" + viewModel.javaClass.simpleName +
                        " parentChain=" + parentChain(root) +
                        " layout=" + layoutToken(root) +
                        "",
                )
                readWifiSeed(
                    root = root,
                    viewModel = viewModel,
                    contract = seedContract,
                    source = "bind",
                    onEvent = onEvent,
                )?.let(onWifiState)
            }
        }

        chain.proceed()
    }

    @Synchronized
    fun seedRestoredWifiState(
        onEvent: ((String) -> Unit)?,
    ): StatusStateStore.WifiState? {
        val contract = wifiSeedContract ?: return null
        val binding =
            wifiRoots.entries.firstOrNull { (root, viewModel) ->
                root.isAttachedToWindow &&
                    viewModel?.javaClass?.name == HOME_WIFI_VIEW_MODEL_CLASS_NAME
            } ?: run {
                onEvent?.invoke(
                    "networkPipeline wifi seed source=hotReloadRestore " +
                        "state=unavailable reason=view-model-not-transferred",
                )
                return null
            }

        val viewModel = binding.value ?: return null
        return readWifiSeed(
            root = binding.key,
            viewModel = viewModel,
            contract = contract,
            source = "hotReloadRestore",
            onEvent = onEvent,
        )
    }

    private fun readWifiSeed(
        root: View,
        viewModel: Any,
        contract: WifiSeedContract,
        source: String,
        onEvent: ((String) -> Unit)?,
    ): StatusStateStore.WifiState? {
        val getter =
            contract.wifiIconGetter
                ?: run {
                    onEvent?.invoke(
                        "networkPipeline wifi seed source=" + source +
                            " state=unavailable reason=getWifiIcon-missing",
                    )
                    return null
                }

        val flow =
            runCatching {
                getter.invoke(viewModel)
            }.getOrElse { error ->
                onEvent?.invoke(
                    "networkPipeline wifi seed source=" + source +
                        " state=unavailable reason=wifiIcon-read-" +
                        error.javaClass.simpleName,
                )
                return null
            } ?: run {
                onEvent?.invoke(
                    "networkPipeline wifi seed source=" + source +
                        " state=unavailable reason=wifiIcon-null",
                )
                return null
            }

        val valueGetter =
            flow.javaClass.methods
                .firstOrNull { method ->
                    method.name == "getValue" &&
                        method.parameterCount == 0
                }
                ?.apply { isAccessible = true }
                ?: run {
                    onEvent?.invoke(
                        "networkPipeline wifi seed source=" + source +
                            " state=unavailable reason=stateFlow-value-missing " +
                            "flow=" + flow.javaClass.name,
                    )
                    return null
                }

        val value =
            runCatching {
                valueGetter.invoke(flow)
            }.getOrElse { error ->
                onEvent?.invoke(
                    "networkPipeline wifi seed source=" + source +
                        " state=unavailable reason=stateFlow-value-" +
                        error.javaClass.simpleName,
                )
                return null
            }

        val semantic =
            decodeWifiSemantic(
                value = value,
                sourceView = root,
                wifiVisibleIconField = contract.wifiVisibleIconField,
                iconResourceIdAccessor = contract.iconResourceIdAccessor,
            )

        onEvent?.invoke(
            "networkPipeline wifi seed source=" + source +
                " state=" + if (semantic.state != null) "ready" else "unavailable" +
                " getter=" + getter.name +
                " flow=" + flow.javaClass.simpleName +
                " valueType=" + semantic.valueType +
                " modelResId=" + (semantic.resourceId ?: 0) +
                " modelResource=" + (semantic.resourceName ?: "n/a") +
                " resourceAccessor=" + contract.iconResourceIdAccessor.description,
        )
        return semantic.state
    }

    private fun resolveIconResourceIdAccessor(
        iconResourceClass: Class<*>,
    ): IconResourceIdAccessor {
        listOf("getResId", "getRes").forEach { name ->
            val method =
                iconResourceClass.methods
                    .firstOrNull { candidate ->
                        candidate.name == name &&
                            candidate.parameterCount == 0 &&
                            (
                                candidate.returnType == Int::class.javaPrimitiveType ||
                                    Number::class.java.isAssignableFrom(candidate.returnType)
                            )
                    }
            if (method != null) {
                method.isAccessible = true
                return IconResourceIdAccessor(
                    method = method,
                    description = "method:" + method.name,
                )
            }
        }

        listOf("resId", "res").forEach { name ->
            val field =
                runCatching {
                    iconResourceClass.getDeclaredField(name)
                }.getOrNull()
            if (
                field != null &&
                (
                    field.type == Int::class.javaPrimitiveType ||
                        Number::class.java.isAssignableFrom(field.type)
                )
            ) {
                field.isAccessible = true
                return IconResourceIdAccessor(
                    field = field,
                    description = "field:" + field.name,
                )
            }
        }

        throw NoSuchFieldException(
            "No supported resource-id accessor in " + iconResourceClass.name +
                " methods=" +
                iconResourceClass.methods
                    .filter { it.parameterCount == 0 }
                    .joinToString(",") { it.name + ":" + it.returnType.simpleName } +
                " fields=" +
                iconResourceClass.declaredFields
                    .joinToString(",") { it.name + ":" + it.type.simpleName },
        )
    }

    private fun decodeWifiSemantic(
        value: Any?,
        sourceView: View,
        wifiVisibleIconField: Field,
        iconResourceIdAccessor: IconResourceIdAccessor,
    ): WifiSemanticValue {
        val valueType = value?.javaClass?.name
        if (valueType == WIFI_ICON_HIDDEN_CLASS_NAME) {
            return WifiSemanticValue(
                state = StatusStateStore.WifiState.Hidden,
                resourceId = null,
                resourceName = null,
                valueType = value?.javaClass?.simpleName ?: "null",
            )
        }

        if (valueType != WIFI_ICON_VISIBLE_CLASS_NAME) {
            return WifiSemanticValue(
                state = null,
                resourceId = null,
                resourceName = null,
                valueType = value?.javaClass?.simpleName ?: "null",
            )
        }

        val modelResId =
            runCatching {
                val icon = wifiVisibleIconField.get(value)
                if (icon?.javaClass?.name == ICON_RESOURCE_CLASS_NAME) {
                    iconResourceIdAccessor.read(icon)
                } else {
                    null
                }
            }.getOrNull()
        val modelResourceName =
            modelResId?.let { id -> resourceName(sourceView, id) }

        return WifiSemanticValue(
            state =
                StatusStateStore.WifiState.Visible(
                    iconResId = modelResId,
                    signal = SysUiSignalParser.wifi(modelResourceName),
                    internetValidated =
                        SysUiSignalParser.wifiInternetValidated(
                            modelResourceName,
                        ),
                ),
            resourceId = modelResId,
            resourceName = modelResourceName,
            valueType = value.javaClass.simpleName,
        )
    }

    private fun wifiIconHooker(
        wifiImageField: Field,
        wifiClassIdField: Field,
        wifiVisibleIconField: Field,
        iconResourceIdAccessor: IconResourceIdAccessor,
        onWifiState: (StatusStateStore.WifiState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): Hooker = Hooker { chain ->
        val value = chain.getArg(0)
        val emitter = chain.thisObject
        val classId =
            runCatching {
                wifiClassIdField.getInt(emitter)
            }.getOrDefault(-1)

        if (classId != WIFI_ICON_COLLECTOR_CLASS_ID) {
            return@Hooker chain.proceed()
        }

        val image =
            runCatching {
                wifiImageField.get(emitter) as? ImageView
            }.getOrNull()
        val bound = image != null && findWifiBinding(image)
        val semantic =
            if (bound && image != null) {
                decodeWifiSemantic(
                    value = value,
                    sourceView = image,
                    wifiVisibleIconField = wifiVisibleIconField,
                    iconResourceIdAccessor = iconResourceIdAccessor,
                )
            } else {
                null
            }

        val result = chain.proceed()

        if (image != null && bound && semantic != null) {
            val taggedResId =
                (image.tag as? Number)
                    ?.toInt()
                    ?.takeIf { it != 0 }
            val taggedResource =
                taggedResId?.let { id -> resourceName(image, id) }
            val previousTaggedResId =
                synchronized(this) {
                    lastWifiTaggedResources.put(image, taggedResId)
                }
            val hotspotAppliedFallback =
                shouldUseAppliedHotspotFallback(
                    semanticState = semantic.state,
                    taggedResId = taggedResId,
                    previousTaggedResId = previousTaggedResId,
                    taggedResource = taggedResource,
                )
            val effective =
                if (hotspotAppliedFallback) {
                    WifiSemanticValue(
                        state =
                            StatusStateStore.WifiState.Visible(
                                iconResId = taggedResId,
                                signal = SysUiSignalParser.wifi(taggedResource),
                                internetValidated =
                                    SysUiSignalParser.wifiInternetValidated(
                                        taggedResource,
                                    ),
                            ),
                        resourceId = taggedResId,
                        resourceName = taggedResource,
                        valueType = semantic.valueType + "+AppliedHotspot",
                    )
                } else {
                    semantic
                }
            val eventKey =
                effective.valueType + ":" +
                    (effective.resourceId?.toString() ?: "none")
            val changed =
                synchronized(this) {
                    lastWifiEvents.put(image, eventKey) != eventKey
                }

            if (changed) {
                effective.state?.let(onWifiState)
                onEvent?.invoke(
                    "networkPipeline wifi iconEvent " +
                        "phase=semanticBeforeProceed/viewAfterProceed " +
                        "viewId=" + resourceId(image) +
                        " classId=" + classId +
                        " valueType=" + semantic.valueType +
                        " effectiveType=" + effective.valueType +
                        " modelResId=" + (semantic.resourceId ?: 0) +
                        " modelResource=" + (semantic.resourceName ?: "n/a") +
                        " taggedResId=" + (taggedResId ?: 0) +
                        " taggedResource=" + (taggedResource ?: "n/a") +
                        " appliedFallback=" +
                        if (hotspotAppliedFallback) {
                            "hotspot-tag-change"
                        } else {
                            "none"
                        } +
                        " visibility=" + visibilityName(image.visibility) +
                        " presentation=" + wifiPresentationToken(image),
                )
            }
        }

        result
    }

    internal fun shouldUseAppliedHotspotFallback(
        semanticState: StatusStateStore.WifiState?,
        taggedResId: Int?,
        previousTaggedResId: Int?,
        taggedResource: String?,
    ): Boolean =
        semanticState == StatusStateStore.WifiState.Hidden &&
            taggedResId != null &&
            taggedResId != previousTaggedResId &&
            SysUiSignalParser.isHotspotWifiResource(taggedResource)

    private fun mobileBindHooker(
        subscriptionIdMethod: Method,
        onPresentationChanged: (() -> Unit)?,
        onEvent: ((String) -> Unit)?,
    ): Hooker = Hooker { chain ->
        val root = chain.getArg(0) as? ViewGroup
        val locationViewModel = chain.getArg(1)
        val iconViewModel = chain.getArg(2)
        var bindingLog: String? = null

        if (
            root != null &&
            locationViewModel?.javaClass?.name == HOME_MOBILE_VIEW_MODEL_CLASS_NAME
        ) {
            val subscriptionId = runCatching {
                (subscriptionIdMethod.invoke(locationViewModel) as Number).toInt()
            }.getOrDefault(-1)

            val previous = synchronized(this) {
                mobileRoots.put(root, subscriptionId)
            }
            if (previous == null || previous != subscriptionId) {
                bindingLog =
                    "networkPipeline mobile bound " +
                        "stage=beforeProceed " +
                        "root=" + root.javaClass.simpleName +
                        " rootId=" + resourceId(root) +
                        " locationVm=" + locationViewModel.javaClass.simpleName +
                        " subId=" + subscriptionId +
                        " iconVm=" + (iconViewModel?.javaClass?.simpleName ?: "none") +
                        " parentChain=" + parentChain(root) +
                        " layout=" + layoutToken(root) +
                        ""
            }
        }

        val result = chain.proceed()
        bindingLog?.let {
            onEvent?.invoke(it)
            onPresentationChanged?.invoke()
        }
        result
    }

    private fun mobileSignalHooker(
        mobileImageField: Field,
        mobileClassIdField: Field,
        onMobileIcon: (StatusStateStore.MobileIconUpdate) -> Unit,
        onMobileSignalWillApply: ((ImageView) -> Unit)?,
        onEvent: ((String) -> Unit)?,
    ): Hooker = Hooker { chain ->
        val emitter = chain.thisObject
        val image = runCatching {
            mobileImageField.get(emitter) as? ImageView
        }.getOrNull()
        val classId = runCatching {
            mobileClassIdField.getInt(emitter)
        }.getOrDefault(-1)
        val value = chain.getArg(0)

        if (image != null && classId == 0) {
            runCatching {
                onMobileSignalWillApply?.invoke(image)
            }.onFailure { error ->
                onEvent?.invoke(
                    "networkPipeline mobile preMask failed " +
                        "error=" + error.javaClass.simpleName,
                )
            }
        }

        var eventLog: String? = null
        if (image != null) {
            val subscriptionId = findMobileSubscription(image)
            if (subscriptionId != null) {
                val valueText = when (value) {
                    is Number -> value.toLong().toString()
                    null -> "null"
                    else -> value.javaClass.simpleName
                }
                val eventKey = classId.toString() + ":" + valueText
                val changed = synchronized(this) {
                    lastMobileEvents.put(image, eventKey) != eventKey
                }

                if (changed) {
                    val resourceId = (value as? Number)
                        ?.toInt()
                        ?.takeIf { it != 0 }
                    val resourceName = resourceId?.let { id -> resourceName(image, id) }
                    val kind = when (classId) {
                        0 -> StatusStateStore.MobileIconKind.SIGNAL
                        1 -> StatusStateStore.MobileIconKind.VOLTE
                        2 -> StatusStateStore.MobileIconKind.VOWIFI
                        else -> null
                    }
                    if (kind != null) {
                        onMobileIcon(
                            StatusStateStore.MobileIconUpdate(
                                subscriptionId = subscriptionId,
                                kind = kind,
                                resourceId = resourceId,
                                signal = if (
                                    kind == StatusStateStore.MobileIconKind.SIGNAL
                                ) {
                                    SysUiSignalParser.mobile(resourceName)
                                } else {
                                    null
                                },
                            ),
                        )
                    }

                    eventLog =
                        "networkPipeline mobile iconEvent " +
                            "phase=beforeProceed " +
                            "subId=" + subscriptionId +
                            " viewId=" + resourceId(image) +
                            " classId=" + classId +
                            " value=" + valueText +
                            " resource=" + (resourceName ?: "n/a")
                }
            }
        }

        val result = chain.proceed()
        eventLog?.let { onEvent?.invoke(it) }
        result
    }

    @Synchronized
    private fun findWifiBinding(view: View): Boolean {
        var current: View? = view
        while (current != null) {
            val group = current as? ViewGroup
            if (group != null && wifiRoots.containsKey(group)) {
                return true
            }
            current = current.parent as? View
        }
        return false
    }

    @Synchronized
    private fun findMobileSubscription(view: View): Int? {
        var current: View? = view
        while (current != null) {
            val group = current as? ViewGroup
            if (group != null && mobileRoots.containsKey(group)) {
                return mobileRoots[group]
            }
            current = current.parent as? View
        }
        return null
    }

    private fun parentChain(view: View): String =
        buildList {
            var current: View? = view
            repeat(MAX_PARENT_CHAIN_DEPTH) {
                val value = current ?: return@repeat
                add(
                    value.javaClass.simpleName +
                        "[" + resourceId(value) + "]",
                )
                current = value.parent as? View
            }
        }.joinToString(">")

    private fun layoutToken(view: View): String {
        val params = view.layoutParams
        return if (params == null) {
            "none"
        } else {
            params.width.toString() + "x" + params.height +
                ":measured=" + view.measuredWidth + "x" + view.measuredHeight
        }
    }

    private fun wifiPresentationToken(image: ImageView): String {
        val drawable = image.drawable
        val bounds = drawable?.bounds
        val tint = image.imageTintList
        return buildString {
            append("drawable=")
            append(drawable?.javaClass?.simpleName ?: "none")
            append(" intrinsic=")
            append(drawable?.intrinsicWidth ?: 0)
            append('x')
            append(drawable?.intrinsicHeight ?: 0)
            append(" bounds=")
            if (bounds == null) {
                append("none")
            } else {
                append(bounds.left)
                append(',')
                append(bounds.top)
                append(',')
                append(bounds.right)
                append(',')
                append(bounds.bottom)
            }
            append(" drawableAlpha=")
            append(drawable?.alpha ?: -1)
            append(" imageAlpha=")
            append(image.imageAlpha)
            append(" tint=")
            append(colorStateListToken(tint, image.drawableState))
            append(" tintMode=")
            append(image.imageTintMode?.name ?: "none")
            append(" colorFilter=")
            append(image.colorFilter?.javaClass?.simpleName ?: "none")
            append(" scaleType=")
            append(image.scaleType?.name ?: "none")
            append(" matrix=")
            append(image.imageMatrix?.toShortString() ?: "none")
            append(" padding=")
            append(image.paddingLeft)
            append(',')
            append(image.paddingTop)
            append(',')
            append(image.paddingRight)
            append(',')
            append(image.paddingBottom)
            append(" measured=")
            append(image.measuredWidth)
            append('x')
            append(image.measuredHeight)
        }
    }

    private fun colorStateListToken(
        tint: ColorStateList?,
        state: IntArray,
    ): String {
        if (tint == null) return "none"
        val resolved = tint.getColorForState(state, tint.defaultColor)
        return "0x" + resolved.toUInt().toString(16).padStart(8, '0') +
            "/default=0x" + tint.defaultColor.toUInt().toString(16).padStart(8, '0')
    }

    private fun visibilityName(visibility: Int): String = when (visibility) {
        View.VISIBLE -> "VISIBLE"
        View.INVISIBLE -> "INVISIBLE"
        View.GONE -> "GONE"
        else -> visibility.toString()
    }

    private fun resourceId(view: View): String {
        if (view.id == View.NO_ID) {
            return "none"
        }

        return runCatching {
            view.resources.getResourceName(view.id)
        }.getOrElse {
            view.id.toString()
        }
    }

    private fun resourceName(
        view: View,
        resId: Int,
    ): String {
        if (resId == 0) {
            return "none"
        }

        return runCatching {
            view.resources.getResourceName(resId)
        }.getOrElse {
            resId.toString()
        }
    }
}
