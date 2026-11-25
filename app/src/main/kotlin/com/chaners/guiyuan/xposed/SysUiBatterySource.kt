package com.chaners.guiyuan.xposed

import android.graphics.Typeface
import android.view.View
import com.chaners.guiyuan.settings.FontWeightAxis
import android.view.ViewGroup
import com.chaners.guiyuan.xposed.SysUiFatal.rethrow as rethrowFatal
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

internal object SysUiBatterySource {
    const val BATTERY_ICON_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterIconView"
    const val BATTERY_METER_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    internal const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    const val BATTERY_LEVEL_METHOD_NAME = "onBatteryLevelChanged"
    const val CHARGE_STATE_METHOD_NAME = "onChargeStateChanged"
    const val POWER_SAVE_METHOD_NAME = "onPowerSaveChanged"
    const val PERFORMANCE_METHOD_NAME = "onPerformanceModeChanged"
    const val MIUI_OPTIMIZATION_METHOD_NAME = "setMiuiOptimizationEnabled"
    const val UPDATE_CHARGE_AND_TEXT_METHOD_NAME = "updateChargeAndText"
    const val HOOK_COUNT = 6

    private const val LEVEL_HOOK_ID = "combinedstatus.battery.level"
    private const val CHARGE_HOOK_ID = "combinedstatus.battery.charge"
    private const val POWER_SAVE_HOOK_ID = "combinedstatus.battery.power-save"
    private const val PERFORMANCE_HOOK_ID = "combinedstatus.battery.performance"
    private const val MIUI_OPTIMIZATION_HOOK_ID = "combinedstatus.battery.miui-optimization"
    private const val CHARGING_GLYPH_HOOK_ID = "combinedstatus.battery.charging-glyph"

    @Volatile
    private var hooksReady = false
    @Volatile
    private var failedInstallHandles: List<HookHandle> = emptyList()
    private class Delivery(val state: StatusStateStore.BatteryState)
    @Volatile
    private var lastDelivery: Delivery? = null
    @Volatile
    private var nativeStateReader: ((View) -> StatusStateStore.BatteryState?)? = null
    @Volatile
    private var lastChargingIconResId: Int? = null

    internal data class VerifiedFont(
        val host: View,
        val face: Typeface,
        val axis: FontWeightAxis,
    )

    @Volatile
    private var verifiedFont: VerifiedFont? = null

    fun currentNumberFont(): VerifiedFont? =
        verifiedFont?.takeIf { it.host === SysUiHostRegistry.current() }

    fun setVerifiedNumberFont(host: View, face: Typeface?, axis: FontWeightAxis?) {
        verifiedFont =
            if (face != null && axis != null) VerifiedFont(host, face, axis) else null
    }

    // All delivery and diagnostic callbacks belong to one installing generation.
    internal class Sinks(
        val onBatteryState: (StatusStateStore.BatteryState) -> Unit,
        val onChargingIconResource: (Int) -> Unit,
        val onRuntimeFailure: (String, Throwable) -> Unit,
        val onEvent: ((String) -> Unit)?,
        val isDetailedDiagnosticsEnabled: () -> Boolean,
    )

    // Resolve every exact-ROM method before attempting the first Hook, so a
    // reflection mismatch cannot leave a partly installed battery generation.
    private class BatteryMethods(
        val levelMethod: Method,
        val chargeMethod: Method,
        val powerSaveMethod: Method,
        val performanceMethod: Method,
        val miuiOptimizationMethod: Method,
        val updateChargeAndTextMethod: Method,
    )

    private fun resolveBatteryMethods(iconClass: Class<*>, meterClass: Class<*>): BatteryMethods {
        val levelMethod =
            meterClass.getDeclaredMethod(
                BATTERY_LEVEL_METHOD_NAME,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val chargeMethod =
            meterClass.getDeclaredMethod(
                CHARGE_STATE_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val powerSaveMethod =
            meterClass.getDeclaredMethod(
                POWER_SAVE_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val performanceMethod =
            meterClass.getDeclaredMethod(
                PERFORMANCE_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val miuiOptimizationMethod =
            iconClass.getDeclaredMethod(
                MIUI_OPTIMIZATION_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val updateChargeAndTextMethod =
            meterClass.getDeclaredMethod(UPDATE_CHARGE_AND_TEXT_METHOD_NAME)
                .apply { isAccessible = true }
        return BatteryMethods(
            levelMethod = levelMethod,
            chargeMethod = chargeMethod,
            powerSaveMethod = powerSaveMethod,
            performanceMethod = performanceMethod,
            miuiOptimizationMethod = miuiOptimizationMethod,
            updateChargeAndTextMethod = updateChargeAndTextMethod,
        )
    }

    @Suppress("TooGenericExceptionCaught")
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        sinks: Sinks,
    ): List<HookHandle> {
        check(!hooksReady && failedInstallHandles.isEmpty()) { "battery-hooks-already-installed" }

        val reader = SysUiBatteryEvidence.Reader(classLoader) { lastChargingIconResId }
        val iconClass = reader.iconClass
        val meterClass = reader.meterClass

        val warned = AtomicBoolean(false)
        fun fallback(source: String, error: Throwable) {
            rethrowFatal(error)
            SysUiPresentationRuntime.emitPassiveDiagnostic {
                sinks.onRuntimeFailure(source, error)
            }
        }

        fun report(message: () -> String) {
            val sink = sinks.onEvent ?: return
            SysUiPresentationRuntime.emitPassiveDiagnostic(warned, "Guiyuan[Battery]") {
                if (sinks.isDetailedDiagnosticsEnabled()) sink(message())
            }
        }

        fun publish(
            iconView: View,
            sourceMethod: String,
            levelCallbackCharging: Boolean? = null,
        ) {
            val state = reader.readState(iconView) ?: return
            if (levelCallbackCharging != null && levelCallbackCharging != state.charging) {
                report {
                    "batteryLevelParameterMismatch source=" + sourceMethod +
                        " nativeChargingArgument=" + levelCallbackCharging +
                        " observedIconCharging=" + state.charging +
                        " iconIdentity=" + System.identityHashCode(iconView)
                }
            }
            val delivery =
                synchronized(this) {
                    updateChargingIconReceiptLocked(state)
                    if (lastDelivery?.state == state) {
                        null
                    } else {
                        Delivery(state).also { lastDelivery = it }
                    }
                } ?: return
            try {
                sinks.onBatteryState(state)
            } catch (error: Throwable) {
                fallback("state", error)
                synchronized(this) {
                    if (lastDelivery === delivery) lastDelivery = null
                }
                return
            }
            report {
                "batteryState source=" + sourceMethod +
                    " percent=" + state.percent +
                    " charging=" + state.charging +
                    " iconIdentity=" + System.identityHashCode(iconView) +
                    " semantic=" + (state.semanticState?.name ?: "unavailable") +
                    " systemColor=" +
                    (state.systemSemanticColor?.let(::colorHex) ?: "status-icon") +
                    " chargingIconId=" + (state.chargingIconResId ?: "unavailable") +
                    " semanticAuthority=MiuiBatteryMeterIconView.getProgressStatus()"
            }
        }

        fun publishChargingGlyph(
            meterView: View,
            sourceMethod: String,
        ) {
            val iconId =
                reader.readMeterChargingIconId(meterView) ?: return

            val changed =
                synchronized(this) {
                    val changed = lastChargingIconResId != iconId
                    lastChargingIconResId = iconId
                    changed
                }
            // Repeated samples still reconcile the state store after Hot Reload.
            try {
                sinks.onChargingIconResource(iconId)
            } catch (error: Throwable) {
                fallback("charging-glyph", error)
                return
            }
            if (changed) {
                report {
                    "batteryChargingGlyph source=MiuiBatteryMeterView." + sourceMethod +
                        " resourceId=" + iconId +
                        " authority=MiuiBatteryMeterView.getHollowChargingIconId()"
                }
            }
        }

        fun hook(
            method: Method,
            hookId: String,
        ): HookHandle =
            module
                .hook(method)
                .setId(hookId)
                .intercept(
                    Hooker { chain ->
                        // Capture the callback argument before native/other Hook work.
                        val levelChargingAtEntry =
                            if (method.name == BATTERY_LEVEL_METHOD_NAME) {
                                optionalNativeRead { chain.args.getOrNull(2) as? Boolean }
                            } else null
                        val result = chain.proceed()
                        val targetView =
                            if (hooksReady) optionalNativeRead { chain.thisObject as? View } else null
                        val iconView = reader.iconView(targetView)
                        if (iconView != null) {
                            try {
                                publish(
                                    iconView = iconView,
                                    sourceMethod = method.declaringClass.simpleName + "." + method.name,
                                    levelCallbackCharging = levelChargingAtEntry,
                                )
                            } catch (error: Throwable) {
                                fallback("state", error)
                            }
                        }
                        result
                    },
                )

        val methods = resolveBatteryMethods(iconClass, meterClass)
        val handles = ArrayList<HookHandle>(HOOK_COUNT)
        try {
            handles +=
                module
                    .hook(methods.updateChargeAndTextMethod)
                    .setId(CHARGING_GLYPH_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val result = chain.proceed()
                            if (hooksReady) {
                                val meterView = optionalNativeRead { chain.thisObject as? View }
                                if (meterView != null) {
                                    try {
                                        publishChargingGlyph(
                                            meterView = meterView,
                                            sourceMethod = methods.updateChargeAndTextMethod.name,
                                        )
                                    } catch (error: Throwable) {
                                        fallback("charging-glyph", error)
                                    }
                                }
                            }
                            result
                        },
                    )
            handles += hook(methods.levelMethod, LEVEL_HOOK_ID)
            handles += hook(methods.chargeMethod, CHARGE_HOOK_ID)
            handles += hook(methods.powerSaveMethod, POWER_SAVE_HOOK_ID)
            handles += hook(methods.performanceMethod, PERFORMANCE_HOOK_ID)
            handles += hook(methods.miuiOptimizationMethod, MIUI_OPTIMIZATION_HOOK_ID)
            nativeStateReader = reader::readState
            hooksReady = true
            return handles
        } catch (error: Throwable) {
            hooksReady = false
            nativeStateReader = null
            val remaining = handles.asReversed().filter { it.unhookFailed() }
            failedInstallHandles = remaining
            rethrowFatal(error)
            if (remaining.isNotEmpty()) {
                throw IllegalStateException("battery-source-hook-cleanup-failed", error)
            }
            throw error
        }
    }

    val failedInstallHookCount: Int
        get() = failedInstallHandles.size

    // Verified against the exact SystemUI fingerprint: both native number
    // routes must use the known file-backed faces, not mipro family fallbacks.
    fun readHostNumberFont(host: View): Pair<Typeface, String>? =
        optionalNativeRead { SysUiBatteryEvidence.readNumberFontContract(host) }

    // Reconcile the transferred snapshot with the same authority and delivery receipt as hooks.
    fun reconcileHostBatteryState(
        host: Any,
        commit: (StatusStateStore.BatteryState) -> Unit,
    ): StatusStateStore.BatteryState? {
        if (!hooksReady) return null
        val readState = nativeStateReader ?: return null
        val priorDelivery = lastDelivery
        val meter = SysUiBatteryEvidence.nativeBatteryMeter(host) as? ViewGroup ?: return null
        val pending = ArrayDeque<View>()
        pending.add(meter)
        var icon: View? = null
        var visited = 0
        while (pending.isNotEmpty()) {
            // A changed or ambiguous native subtree cannot be a charging-state authority.
            if (++visited > 64) return null
            val view = pending.removeFirst()
            if (view.javaClass.name == BATTERY_ICON_VIEW_CLASS_NAME) {
                if (icon != null) return null
                icon = view
            }
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    pending.add(view.getChildAt(index))
                }
            }
        }
        val state = icon?.let(readState) ?: return null
        commit(state)
        synchronized(this) {
            // A reentrant hook delivery takes precedence over the initial native sample.
            if (lastDelivery === priorDelivery) {
                lastDelivery = Delivery(state)
                updateChargingIconReceiptLocked(state)
            }
        }
        return state
    }

    fun readHostChargingIconId(host: Any): Int? {
        val battery = SysUiBatteryEvidence.nativeBatteryMeter(host) ?: return null
        return optionalNativeRead {
            battery.javaClass.getDeclaredMethod("getHollowChargingIconId").apply {
                isAccessible = true
            }.invoke(battery) as? Int
        }?.takeIf { it != 0 }
    }

    // Both deliveries update this receipt only while holding the source monitor.
    private fun updateChargingIconReceiptLocked(state: StatusStateStore.BatteryState) {
        if (!state.charging) {
            lastChargingIconResId = null
        } else if (state.chargingIconResId != null) {
            lastChargingIconResId = state.chargingIconResId
        }
    }

    fun matches(handle: HookHandle): Boolean =
        handle.id == LEVEL_HOOK_ID ||
            handle.id == CHARGE_HOOK_ID ||
            handle.id == POWER_SAVE_HOOK_ID ||
            handle.id == PERFORMANCE_HOOK_ID ||
            handle.id == MIUI_OPTIMIZATION_HOOK_ID ||
            handle.id == CHARGING_GLYPH_HOOK_ID

    @Synchronized
    fun resetRuntimeState() {
        failedInstallHandles = emptyList()
        hooksReady = false
        lastDelivery = null
        nativeStateReader = null
        lastChargingIconResId = null
        verifiedFont = null
    }
}
