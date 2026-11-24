package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_DEFAULT
import com.chaners.guiyuan.xposed.network.CenterIndicator

// Null is an observed unknown level; absent or unavailable signals do not form dual rows.
internal data class DualMobileLevels(val sim1: Int?, val sim2: Int?)

internal data class RenderModel(
    val batteryPercent: Int,
    val charging: Boolean,
    val centerIndicator: CenterIndicator,
    val chargingIconResId: Int? = null,
    val networkActivityResId: Int? = null,
    val mobileLevel: Int?,
    val mobileUnavailableMark: Boolean = false,
    val effectiveDataSubscriptionId: Int,
    val batterySemanticState: BatterySemanticState =
        BatterySemanticState.NORMAL,
    val batterySystemSemanticColor: Int? = null,
    val dualMobileLevels: DualMobileLevels? = null,
) {
    companion object {
        fun from(
            snapshot: StatusStateStore.Snapshot,
            presentation: PresentationStore.Snapshot,
            defaultDataSubscriptionId: Int,
            dualSignalEnabled: Boolean = false,
            lowBatteryThreshold: Int = LOW_BATTERY_THRESHOLD_DEFAULT,
        ): RenderModel? {
            val battery = snapshot.battery ?: return null

            val mobile = RenderMobileSelection.selectMobilePresentation(
                snapshot, presentation, defaultDataSubscriptionId, dualSignalEnabled,
            )
            val airplaneMode = snapshot.airplaneMode == true
            val mobileRecoveryPending = snapshot.mobileRecoveryPending
            val nativeNoSimVisible = presentation.statusIcons.noSimVisible
            val nativeMobileWithheld = mobileRecoveryPending || nativeNoSimVisible
            val centerIndicator = RenderMobileSelection.resolveCenterIndicator(
                snapshot, presentation, mobile.subscriptionId, dualSignalEnabled,
            ) ?: return null

            val dualLevels =
                RenderMobileSelection.resolveDualLevels(
                    snapshot = snapshot,
                    presentation = presentation,
                    dualSignalEnabled = dualSignalEnabled,
                    mobileRecoveryPending = mobileRecoveryPending,
                    nativeNoSimVisible = nativeNoSimVisible,
                )

            val batteryPercent = battery.percent.coerceIn(0, 100)
            val batterySemanticState = BatterySemanticState.atThreshold(
                battery.semanticState,
                batteryPercent,
                lowBatteryThreshold,
            )

            return RenderModel(
                batteryPercent = batteryPercent,
                charging = battery.charging,
                centerIndicator = centerIndicator,
                chargingIconResId = battery.chargingIconResId,
                networkActivityResId = RenderMobileSelection.resolveNetworkActivityResource(
                    snapshot = snapshot,
                    presentation = presentation,
                    selectedSubscriptionId = mobile.subscriptionId,
                    centerIndicator = centerIndicator,
                    keepMobileActivityInEmpty =
                        !airplaneMode && !nativeMobileWithheld && !mobile.unavailableMark,
                ),
                mobileLevel = mobile.level,
                mobileUnavailableMark = mobile.unavailableMark,
                effectiveDataSubscriptionId = mobile.subscriptionId,
                dualMobileLevels = dualLevels,
                batterySemanticState = batterySemanticState,
                batterySystemSemanticColor = resolveBatterySystemColor(battery, batterySemanticState),
            )
        }


        private fun resolveBatterySystemColor(
            battery: StatusStateStore.BatteryState,
            batterySemanticState: BatterySemanticState,
        ): Int? =
        when (batterySemanticState) {
            BatterySemanticState.NORMAL -> null
            BatterySemanticState.LOW ->
                battery.lowSystemColor
                    ?: battery.systemSemanticColor.takeIf {
                        battery.semanticState == BatterySemanticState.LOW
                    }
            else -> battery.systemSemanticColor
        }

    }
}
