package com.chaners.guiyuan.xposed

import android.util.Log
import android.view.View
import android.widget.TextView
import com.chaners.guiyuan.xposed.SysUiFatal.rethrow as rethrowFatal
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.util.ArrayList
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal object SysUiTintSource {

    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val UPDATE_TINT_METHOD_NAME = "updateLightDarkTint"
    const val ON_DARK_CHANGED_INTERNAL_METHOD_NAME = "onDarkChangedInternal"
    const val HOOK_COUNT = 2

    private const val UPDATE_HOOK_ID = "combinedstatus.tint.battery.update"
    private const val INTERNAL_HOOK_ID = "combinedstatus.tint.battery.internal"
    private val lastStates = WeakHashMap<View, TintState>()
    // Native reads and successful owner delivery are independent facts.
    private class Delivery(val state: TintState)
    private val deliveredStates = WeakHashMap<View, Delivery>()
    private val firstEventLogged = WeakHashMap<View, Unit>()

    @Volatile
    private var hooksReady = false
    @Volatile
    private var failedInstallHandles: List<HookHandle> = emptyList()
    @Volatile
    private var batteryPercentViewField: Field? = null

    @Volatile
    private var lastSourceView: WeakReference<View>? = null

    @Suppress("TooGenericExceptionCaught")
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onTintState: (TintUpdate) -> Unit,
        onRuntimeFailure: (Throwable) -> Unit,
        onEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
    ): List<HookHandle> {
        check(!hooksReady && failedInstallHandles.isEmpty()) { "tint-hooks-already-installed" }

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
        val warned = AtomicBoolean(false)
        fun diagnose(block: () -> Unit) {
            try {
                block()
            } catch (error: Throwable) {
                rethrowFatal(error)
                if (warned.compareAndSet(false, true)) {
                    SysUiPresentationRuntime.emitPassiveDiagnostic {
                        Log.w("Guiyuan[Tint]", "Diagnostic failed", error)
                    }
                }
            }
        }

        // Both native tint callbacks have one post-native state delivery owner;
        // only updateLightDarkTint carries first-event diagnostic arguments.
        fun tintHooker(source: String, reportNativeArguments: Boolean): Hooker =
            Hooker { chain ->
                val result = chain.proceed()
                val sourceView =
                    if (hooksReady) {
                        optionalNativeRead { chain.thisObject as? View }
                    } else {
                        null
                    }
                if (sourceView != null) {
                    val state =
                        dispatchAppliedState(
                            sourceView = sourceView,
                            percentField = percentField,
                            onTintState = onTintState,
                            onRuntimeFailure = onRuntimeFailure,
                        )
                    diagnose {
                        if (onEvent != null && isDetailedDiagnosticsEnabled()) {
                            SysUiTintDiagnosticProbe.probeBatteryIconAuthority(
                                sourceView = sourceView,
                                iconField = iconField,
                                source = source,
                                onEvent = onEvent,
                            )
                            if (
                                reportNativeArguments &&
                                state != null &&
                                synchronized(this) {
                                    !firstEventLogged.containsKey(sourceView)
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
                                synchronized(this) { firstEventLogged[sourceView] = Unit }
                            }
                        }
                    }
                }
                result
            }

        val updateHandle =
            runCatching {
                module
                    .hook(updateMethod)
                    .setId(UPDATE_HOOK_ID)
                    .intercept(
                        tintHooker(UPDATE_TINT_METHOD_NAME, reportNativeArguments = true),
                    )
            }.getOrElse { error ->
                clearSourceState()
                throw error
            }
        val internalHandle =
            runCatching {
                module
                    .hook(internalMethod)
                    .setId(INTERNAL_HOOK_ID)
                    .intercept(
                        tintHooker(
                            ON_DARK_CHANGED_INTERNAL_METHOD_NAME,
                            reportNativeArguments = false,
                        ),
                    )
            }.getOrElse { error ->
                val cleanupFailed =
                    try {
                        updateHandle.unhook()
                        false
                    } catch (failure: Throwable) {
                        rethrowFatal(failure)
                        true
                    }
                if (cleanupFailed) failedInstallHandles = listOf(updateHandle)
                clearSourceState()
                rethrowFatal(error)
                if (cleanupFailed) {
                    throw IllegalStateException("tint-source-hook-cleanup-failed", error)
                }
                throw error
            }
        hooksReady = true
        return listOf(updateHandle, internalHandle)
    }

    val failedInstallHookCount: Int
        get() = failedInstallHandles.size

    @Suppress("TooGenericExceptionCaught")
    private fun dispatchAppliedState(
        sourceView: View,
        percentField: Field,
        onTintState: (TintUpdate) -> Unit,
        onRuntimeFailure: (Throwable) -> Unit,
    ): TintState? {
        val state =
            readAppliedState(
                sourceView = sourceView,
                percentField = percentField,
            ) ?: return null

        val delivery =
            synchronized(this) {
                lastStates[sourceView] = state
                if (lastSourceView?.get() !== sourceView) {
                    lastSourceView = WeakReference(sourceView)
                }
                if (deliveredStates[sourceView]?.state == state) {
                    null
                } else {
                    Delivery(state).also { deliveredStates[sourceView] = it }
                }
            }
        if (delivery == null) return state
        try {
            onTintState(TintUpdate(sourceView, state))
        } catch (error: Throwable) {
            rethrowFatal(error)
            try {
                onRuntimeFailure(error)
            } catch (failure: Throwable) {
                rethrowFatal(failure)
                if (failure !== error) error.addSuppressed(failure)
            } finally {
                // Keep the failed delivery marked during native ownership cleanup.
                synchronized(this) {
                    if (deliveredStates[sourceView] === delivery) {
                        deliveredStates.remove(sourceView)
                    }
                }
            }
            return null
        }
        return state
    }

    fun matches(handle: HookHandle): Boolean =
        handle.id == UPDATE_HOOK_ID || handle.id == INTERNAL_HOOK_ID

    @Synchronized
    fun resetDiagnosticProbes() {
        firstEventLogged.clear()
        SysUiTintDiagnosticProbe.resetTransient()
    }

    @Synchronized
    fun resetRuntimeState() {
        failedInstallHandles = emptyList()
        clearSourceState()
    }

    @Synchronized
    private fun clearSourceState() {
        hooksReady = false
        lastStates.clear()
        deliveredStates.clear()
        firstEventLogged.clear()
        SysUiTintDiagnosticProbe.clearAll()
        batteryPercentViewField = null
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
            optionalNativeRead {
                percentField.get(sourceView) as? TextView
            } ?: return null
        return TintState(
            appliedTint = percentView.currentTextColor,
        )
    }

    internal data class TintUpdate(
        val sourceView: View,
        val state: TintState,
    )
}
