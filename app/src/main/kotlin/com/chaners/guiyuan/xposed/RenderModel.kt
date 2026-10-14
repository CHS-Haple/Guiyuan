package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.xposed.network.ConnectivityPolicy
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SignalStrength

internal data class DualMobileLevels(val sim1: Int, val sim2: Int)

internal data class RenderModel(
    val batteryPercent: Int,
    val charging: Boolean,
    val centerIndicator: CenterIndicator,
    val chargingIconResId: Int? = null,
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
        ): RenderModel? {
            val battery = snapshot.battery ?: return null

            val nativeNoSimVisible = presentation.statusIcons.noSimVisible
            val noSimIcon =
                presentation.statusIcons.noSimIcon
                    ?.takeIf { nativeNoSimVisible }

            val preferredDataSubscriptionId =
                if (dualSignalEnabled) {
                    // An unknown data SIM cannot borrow the native presentation root.
                    defaultDataSubscriptionId
                } else {
                    presentation.mobilePresentation
                        ?.effectiveDataSubscriptionId
                        ?.takeIf { subscriptionId -> subscriptionId >= 0 }
                        ?: defaultDataSubscriptionId
                }

            val singleDetectedSubId =
                presentation.mobilePresentation
                    ?.takeIf { native ->
                        dualSignalEnabled &&
                            native.mode == NativePresentationResolver.Mode.SINGLE &&
                            native.activeSubscriptionAuthority == "subscription-manager" &&
                            native.activeSubscriptionIds.size == 1
                    }
                    ?.activeSubscriptionIds
                    ?.single()

            val selectedMobile =
                if (nativeNoSimVisible) {
                    null
                } else if (singleDetectedSubId != null) {
                    snapshot.mobile[singleDetectedSubId]
                        ?.takeIf { it.signal !is SignalStrength.Unknown }
                        ?.let { singleDetectedSubId to it }
                } else {
                    val preferred =
                        snapshot.mobile[preferredDataSubscriptionId]
                            ?.takeIf { it.signal !is SignalStrength.Unknown }
                            ?.let { preferredDataSubscriptionId to it }
                    preferred ?: if (dualSignalEnabled) {
                        null
                    } else {
                        snapshot.mobile.entries
                            .firstOrNull { it.value.signal !is SignalStrength.Unknown }
                            ?.let { it.key to it.value }
                    }
                }

            val selectedSubscriptionId =
                if (nativeNoSimVisible) {
                    -1
                } else {
                    selectedMobile?.first
                        ?: singleDetectedSubId
                        ?: preferredDataSubscriptionId.takeIf { it >= 0 }
                        ?: snapshot.mobile.keys.firstOrNull()
                            ?.takeUnless { dualSignalEnabled }
                        ?: -1
                }

            val airplaneMode = snapshot.airplaneMode == true
            val mobileRecoveryPending = snapshot.mobileRecoveryPending
            val selectedSignal = selectedMobile?.second?.signal

            val mobileLevel =
                if (airplaneMode || mobileRecoveryPending || nativeNoSimVisible) {
                    null
                } else {
                    when (selectedSignal) {
                        null -> null
                        SignalStrength.Unknown -> null
                        SignalStrength.Unavailable -> null
                        is SignalStrength.Level -> selectedSignal.value.coerceIn(0, 4)
                    }
                }

            val centerIndicator =
                ConnectivityPolicy.resolve(
                    wifi = snapshot.wifi,
                    airplaneMode = airplaneMode,
                    connectivity = presentation.connectivity,
                    mobileType =
                        if (mobileRecoveryPending || nativeNoSimVisible) {
                            null
                        } else {
                            presentation.mobilePresentation?.let { native ->
                                native.networkType?.takeUnless {
                                    dualSignalEnabled &&
                                        native.activeSubscriptionIds.size > 1 &&
                                        native.networkTypeSubscriptionId != selectedSubscriptionId
                                }
                            }
                        },
                    noSimIcon = noSimIcon,
                )
                    ?: if (mobileRecoveryPending || nativeNoSimVisible) {
                        CenterIndicator.Empty
                    } else {
                        return null
                    }

            val mobileUnavailableMark =
                when {
                    airplaneMode -> true
                    nativeNoSimVisible -> true
                    mobileRecoveryPending -> false
                    selectedSignal is SignalStrength.Unavailable -> true
                    else -> false
                }

            // Use platform slot identity; subscription IDs and data-SIM preference
            // are not the SIM1/SIM2 row order.
            val dualLevels =
                if (dualSignalEnabled && snapshot.airplaneMode == false &&
                    !mobileRecoveryPending && !nativeNoSimVisible
                ) {
                    val native = presentation.mobilePresentation
                    val slots = native?.slotSubscriptionIds
                    val sim1Id = slots?.get(0)
                    val sim2Id = slots?.get(1)
                    if (native != null &&
                        native.activeSubscriptionAuthority == "subscription-manager" &&
                        (native.mode == NativePresentationResolver.Mode.DUAL_AGGREGATED ||
                            native.mode == NativePresentationResolver.Mode.DUAL_SEPARATE) &&
                        native.activeSubscriptionIds.size == 2 &&
                        slots?.size == 2 &&
                        sim1Id != null && sim2Id != null && sim1Id != sim2Id &&
                        sim1Id in native.activeSubscriptionIds &&
                        sim2Id in native.activeSubscriptionIds
                    ) {
                        val first =
                            (snapshot.mobile[sim1Id]?.signal as? SignalStrength.Level)?.value
                        val second =
                            (snapshot.mobile[sim2Id]?.signal as? SignalStrength.Level)?.value
                        if (first != null && second != null) {
                            DualMobileLevels(
                                sim1 = first.coerceIn(0, 4),
                                sim2 = second.coerceIn(0, 4),
                            )
                        } else null
                    } else null
                } else null

            val batteryPercent = battery.percent.coerceIn(0, 100)
            val batterySemanticState =
                battery.semanticState
                    ?: BatterySemanticState.NORMAL

            return RenderModel(
                batteryPercent = batteryPercent,
                charging = battery.charging,
                centerIndicator = centerIndicator,
                chargingIconResId = battery.chargingIconResId,
                mobileLevel = mobileLevel,
                mobileUnavailableMark = mobileUnavailableMark,
                effectiveDataSubscriptionId = selectedSubscriptionId,
                dualMobileLevels = dualLevels,
                batterySemanticState = batterySemanticState,
                batterySystemSemanticColor = battery.systemSemanticColor,
            )
        }
    }
}
