package com.chaners.guiyuan.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.util.WeakHashMap

internal object SystemUiKeyguardAodStateSource {
    const val HOOK_COUNT = 2

    private const val BATTERY_VIEW_CLASS =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val SET_AOD_ANIMATE_METHOD = "setIsAodAnimate"
    private const val TOGGLE_AOD_METHOD = "toggleAodMode"
    private const val TO_AOD_FIELD = "mToAod"
    private const val IS_AOD_ANIMATE_FIELD = "mIsAodAnimate"
    private const val ANIM_TO_AOD_FIELD = "mAnimToAod"

    private const val SET_AOD_ANIMATE_HOOK_ID =
        "combinedstatus.keyguardAod.setIsAodAnimate"
    private const val TOGGLE_AOD_HOOK_ID =
        "combinedstatus.keyguardAod.toggleAodMode"

    private val states = WeakHashMap<View, AodUpdate>()

    @Volatile private var toAodField: Field? = null
    @Volatile private var isAodAnimateField: Field? = null
    @Volatile private var animToAodField: Field? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onAodState: (AodUpdate) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val batteryClass = Class.forName(BATTERY_VIEW_CLASS, false, classLoader)
        val resolvedToAod =
            resolveBooleanField(batteryClass, TO_AOD_FIELD)
                ?: error("aod-to-field-contract-missing")
        val resolvedIsAnimate =
            resolveBooleanField(batteryClass, IS_AOD_ANIMATE_FIELD)
                ?: error("aod-animate-field-contract-missing")
        val resolvedAnimToAod =
            resolveBooleanField(batteryClass, ANIM_TO_AOD_FIELD)

        val setAnimateCandidates =
            batteryClass.declaredMethods.filter { method ->
                method.name == SET_AOD_ANIMATE_METHOD &&
                    method.parameterCount == 1 &&
                    isBooleanType(method.parameterTypes[0]) &&
                    method.returnType == Void.TYPE
            }
        val setAnimateMethod =
            setAnimateCandidates.singleOrNull()
                ?.apply { isAccessible = true }
                ?: error(
                    "aod-set-animate-method-contract-" +
                        if (setAnimateCandidates.isEmpty()) "missing" else "ambiguous",
                )

        val toggleCandidates =
            batteryClass.declaredMethods.filter { method ->
                method.name == TOGGLE_AOD_METHOD &&
                    matchesToggleAodSignature(
                        parameterTypes = method.parameterTypes,
                        returnType = method.returnType,
                    )
            }
        val toggleMethod =
            toggleCandidates.singleOrNull()
                ?.apply { isAccessible = true }
                ?: error(
                    "aod-toggle-method-contract-" +
                        if (toggleCandidates.isEmpty()) "missing" else "ambiguous",
                )

        toAodField = resolvedToAod
        isAodAnimateField = resolvedIsAnimate
        animToAodField = resolvedAnimToAod

        val handles = ArrayList<HookHandle>(HOOK_COUNT)
        try {
            handles +=
                module.hook(setAnimateMethod)
                    .setId(SET_AOD_ANIMATE_HOOK_ID)
                    .intercept(aodHooker(SET_AOD_ANIMATE_METHOD, onAodState, onEvent))
            handles +=
                module.hook(toggleMethod)
                    .setId(TOGGLE_AOD_HOOK_ID)
                    .intercept(aodHooker(TOGGLE_AOD_METHOD, onAodState, onEvent))
            return handles
        } catch (error: Throwable) {
            handles.asReversed().forEach { handle -> runCatching { handle.unhook() } }
            resetRuntimeState()
            throw error
        }
    }

    private fun aodHooker(
        source: String,
        onAodState: (AodUpdate) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): Hooker =
        Hooker { chain ->
            val result = chain.proceed()
            val sourceView = chain.thisObject as? View
            if (sourceView != null) {
                publish(sourceView, source, onAodState, onEvent)
            }
            result
        }

    @Synchronized
    fun currentState(sourceView: View): AodUpdate? {
        states[sourceView]?.let { return it }
        return readState(sourceView, "seed")?.also { update ->
            states[sourceView] = update
        }
    }

    @Synchronized
    fun resetRuntimeState() {
        states.clear()
        toAodField = null
        isAodAnimateField = null
        animToAodField = null
    }

    internal fun blocksKeyguardProjection(
        toAod: Boolean,
        isAodAnimate: Boolean,
        animToAod: Boolean?,
    ): Boolean =
        toAod || isAodAnimate

    internal fun isStableAod(
        toAod: Boolean,
        isAodAnimate: Boolean,
    ): Boolean =
        toAod && !isAodAnimate

    private fun publish(
        sourceView: View,
        source: String,
        onAodState: (AodUpdate) -> Unit,
        onEvent: ((String) -> Unit)?,
    ) {
        val update = readState(sourceView, source) ?: return
        synchronized(this) {
            states[sourceView] = update
        }
        onAodState(update)
        onEvent?.invoke(
            "keyguardAod source=" + source +
                " toAod=" + update.toAod +
                " isAodAnimate=" + update.isAodAnimate +
                " animToAod=" + (update.animToAod ?: "unavailable") +
                " blocked=" + update.blocksProjection +
                " readOnly=true nativeGeometryWrites=0",
        )
    }

    private fun readState(
        sourceView: View,
        source: String,
    ): AodUpdate? {
        val toAod = readBoolean(sourceView, toAodField ?: return null) ?: return null
        val isAodAnimate =
            readBoolean(sourceView, isAodAnimateField ?: return null) ?: return null
        val animToAod = animToAodField?.let { field -> readBoolean(sourceView, field) }
        return AodUpdate(
            sourceView = sourceView,
            toAod = toAod,
            isAodAnimate = isAodAnimate,
            animToAod = animToAod,
            blocksProjection =
                blocksKeyguardProjection(
                    toAod = toAod,
                    isAodAnimate = isAodAnimate,
                    animToAod = animToAod,
                ),
            source = source,
        )
    }

    private fun resolveBooleanField(type: Class<*>, name: String): Field? =
        generateSequence(type) { current -> current.superclass }
            .mapNotNull { current ->
                current.declaredFields.firstOrNull { field ->
                    field.name == name && isBooleanType(field.type)
                }
            }
            .firstOrNull()
            ?.apply { isAccessible = true }

    private fun readBoolean(owner: Any, field: Field): Boolean? =
        runCatching { field.get(owner) as? Boolean }.getOrNull()

    internal fun matchesToggleAodSignature(
        parameterTypes: Array<Class<*>>,
        returnType: Class<*>,
    ): Boolean =
        parameterTypes.size == 1 &&
            isBooleanType(parameterTypes[0]) &&
            returnType == Void.TYPE

    private fun isBooleanType(type: Class<*>): Boolean =
        type == Boolean::class.javaPrimitiveType ||
            type == Boolean::class.javaObjectType

    internal data class AodUpdate(
        val sourceView: View,
        val toAod: Boolean,
        val isAodAnimate: Boolean,
        val animToAod: Boolean?,
        val blocksProjection: Boolean,
        val source: String,
    )
}
