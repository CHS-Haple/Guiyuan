package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_DEFAULT
import com.chaners.guiyuan.xposed.network.ConnectivityPolicy
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SignalStrength

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

            val nativeNoSimVisible = presentation.statusIcons.noSimVisible
            val noSimIcon =
                presentation.statusIcons.noSimIcon
                    ?.takeIf { nativeNoSimVisible }

            val preferredDataSubscriptionId =
                if (dualSignalEnabled) {
                    val native = presentation.mobilePresentation
                    // A removed data SIM can outlive its platform subscription event.
                    defaultDataSubscriptionId.takeIf { subId ->
                        subId >= 0 &&
                            (native?.activeSubsAuthoritative != true ||
                                subId in native.activeSubscriptionIds)
                    } ?: -1
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
                            native.activeSubsAuthoritative &&
                            native.activeSubscriptionIds.size == 1 &&
                            // A bound native root need not be the only active platform SIM.
                            native.platformActiveSubscriptionIds?.singleOrNull() ==
                                native.activeSubscriptionIds[0]
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
            // The native aggregate root may belong to a different SIM.
            val dualTypeMismatch =
                dualSignalEnabled &&
                    (presentation.mobilePresentation?.platformActiveSubscriptionIds?.size ?: 0) > 1 &&
                    presentation.mobilePresentation?.networkTypeSubscriptionId !=
                        selectedSubscriptionId

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
                        if (mobileRecoveryPending || nativeNoSimVisible || dualTypeMismatch) {
                            null
                        } else {
                            presentation.mobilePresentation?.networkType
                        },
                    noSimIcon = noSimIcon,
                )
                    ?: if (
                        mobileRecoveryPending || nativeNoSimVisible ||
                        (dualTypeMismatch && !presentation.connectivity.known)
                    ) {
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

            val dualLevels =
                resolveDualLevels(
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

            val activitySubscriptionId =
                resolveActivitySubscriptionId(presentation, selectedSubscriptionId)

            return RenderModel(
                batteryPercent = batteryPercent,
                charging = battery.charging,
                centerIndicator = centerIndicator,
                chargingIconResId = battery.chargingIconResId,
                networkActivityResId = when (centerIndicator) {
                    is CenterIndicator.Wifi -> snapshot.wifiActivityResId
                    is CenterIndicator.MobileType ->
                        snapshot.mobileActivityResIds[activitySubscriptionId]
                    else -> null
                },
                mobileLevel = mobileLevel,
                mobileUnavailableMark = mobileUnavailableMark,
                effectiveDataSubscriptionId = selectedSubscriptionId,
                dualMobileLevels = dualLevels,
                batterySemanticState = batterySemanticState,
                batterySystemSemanticColor = when (batterySemanticState) {
                    BatterySemanticState.NORMAL -> null
                    BatterySemanticState.LOW ->
                        battery.lowSystemColor
                            ?: battery.systemSemanticColor.takeIf {
                                battery.semanticState == BatterySemanticState.LOW
                            }
                    else -> battery.systemSemanticColor
                },
            )
        }
        // Slot mapping belongs to the platform; data-SIM preference cannot set row order.
        private fun resolveDualLevels(
            snapshot: StatusStateStore.Snapshot,
            presentation: PresentationStore.Snapshot,
            dualSignalEnabled: Boolean,
            mobileRecoveryPending: Boolean,
            nativeNoSimVisible: Boolean,
        ): DualMobileLevels? {
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
                    native.activeSubsAuthoritative &&
                    (native.mode == NativePresentationResolver.Mode.DUAL_AGGREGATED ||
                        native.mode == NativePresentationResolver.Mode.DUAL_SEPARATE) &&
                    native.activeSubscriptionIds.size == 2 &&
                    native.platformActiveSubscriptionIds ==
                        native.activeSubscriptionIds.toSet() &&
                    slots?.size == 2 &&
                    sim1Id != null && sim2Id != null && sim1Id != sim2Id &&
                    sim1Id in native.activeSubscriptionIds &&
                    sim2Id in native.activeSubscriptionIds
                ) {
                    val first = snapshot.mobile[sim1Id]?.signal
                    val second = snapshot.mobile[sim2Id]?.signal
                    if (
                        first != null && second != null &&
                        first !is SignalStrength.Unavailable &&
                        second !is SignalStrength.Unavailable
                    ) {
                        // An unknown level is dim, not a confirmed zero-bar reading.
                        DualMobileLevels(
                            sim1 = (first as? SignalStrength.Level)?.value?.coerceIn(0, 4),
                            sim2 = (second as? SignalStrength.Level)?.value?.coerceIn(0, 4),
                        )
                    } else null
                } else null
            } else null
            return dualLevels
        }

        // One aggregated root is authoritative for its activity drawable, not data-SIM ID.
        private fun resolveActivitySubscriptionId(
            presentation: PresentationStore.Snapshot,
            selectedSubscriptionId: Int,
        ): Int {
        // In the native aggregate there is exactly one displayed mobile root.
        // Its activity drawable is the presentation truth, even when its
        // subscription ID differs from the default-data SIM (e.g. a native
        // upstream Flow proxy projects SIM2 activity onto SIM1's visible root).
        // Leave signal/data-SIM identity and separate-row selection unchanged.
        val activitySubscriptionId =
            presentation.mobilePresentation?.takeIf { native ->
                native.mode == NativePresentationResolver.Mode.DUAL_AGGREGATED &&
                    native.nativeMobileReplacementReady &&
                    native.activeSubsAuthoritative &&
                    selectedSubscriptionId in native.activeSubscriptionIds &&
                    native.presentationRootSubscriptionId in native.activeSubscriptionIds
            }?.presentationRootSubscriptionId ?: selectedSubscriptionId
            return activitySubscriptionId
        }

    }
}
