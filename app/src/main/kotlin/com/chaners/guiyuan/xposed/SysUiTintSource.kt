package com.chaners.guiyuan.xposed

import android.view.View
import android.widget.TextView
import com.chaners.guiyuan.xposed.SysUiFatal.rethrow as rethrowFatal
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
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

    @Volatile
    private var hooksReady = false
    @Volatile
    private var failedInstallHandles: List<HookHandle> = emptyList()
    @Volatile
    private var batteryPercentViewField: Field? = null

    @Volatile
    private var lastSourceView: WeakReference<View>? = null

    // One callback generation retains exactly the fields, sinks and diagnostic
    // gate captured at install; runtime receipts remain owned by SysUiTintSource.
    private class TintHookCallbacks(
        private val percentField: Field,
        private val iconField: Field,
        private val onTintState: (TintUpdate) -> Unit,
        private val onRuntimeFailure: (Throwable) -> Unit,
        private val onEvent: ((String) -> Unit)?,
        private val isDetailedDiagnosticsEnabled: () -> Boolean,
    ) {
        private val warned = AtomicBoolean(false)
        fun hooker(source: String, reportNativeArguments: Boolean): Hooker =
            Hooker { chain ->
                val result = chain.proceed()
                val sourceView =
                    if (SysUiTintSource.hooksReady) {
                        optionalNativeRead { chain.thisObject as? View }
                    } else {
                        null
                    }
                if (sourceView != null) {
                    val state =
                        SysUiTintSource.dispatchAppliedState(
                            sourceView = sourceView,
                            percentField = percentField,
                            onTintState = onTintState,
                            onRuntimeFailure = onRuntimeFailure,
                        )
                    SysUiPresentationRuntime.emitPassiveDiagnostic(warned, "Guiyuan[Tint]") {
                        if (onEvent != null && isDetailedDiagnosticsEnabled()) {
                            SysUiTintDiagnosticProbe.probeBatteryIconAuthority(
                                sourceView = sourceView,
                                iconField = iconField,
                                source = source,
                                onEvent = onEvent,
                            )
                            if (reportNativeArguments && state != null) {
                                SysUiTintDiagnosticProbe.probeFirstNativeArguments(
                                    sourceView = sourceView,
                                    state = state,
                                    onEvent = onEvent,
                                    getArg = { chain.getArg(it) },
                                )
                            }
                        }
                    }
                }
                result
            }
    }

    /** Read-only native members; the source owner still installs, rolls back and retires hooks. */
    private class TintNativeMembers(
        val percentField: Field,
        val iconField: Field,
        val updateMethod: Method,
        val internalMethod: Method,
    ) {
        companion object {
            fun resolve(classLoader: ClassLoader): TintNativeMembers {
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

                return TintNativeMembers(percentField, iconField, updateMethod, internalMethod)
            }
        }
    }

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

        val native = TintNativeMembers.resolve(classLoader)
        batteryPercentViewField = native.percentField
        val callbacks = TintHookCallbacks(
            percentField = native.percentField,
            iconField = native.iconField,
            onTintState = onTintState,
            onRuntimeFailure = onRuntimeFailure,
            onEvent = onEvent,
            isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
        )

        val updateHandle =
            runCatching {
                module
                    .hook(native.updateMethod)
                    .setId(UPDATE_HOOK_ID)
                    .intercept(
                        callbacks.hooker(UPDATE_TINT_METHOD_NAME, reportNativeArguments = true),
                    )
            }.getOrElse { error ->
                clearSourceState()
                throw error
            }
        val internalHandle =
            runCatching {
                module
                    .hook(native.internalMethod)
                    .setId(INTERNAL_HOOK_ID)
                    .intercept(
                        callbacks.hooker(
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
