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
                selectDataSubscriptionId(presentation, defaultDataSubscriptionId, dualSignalEnabled)

            val singleDetectedSubId =
                detectSoleActiveSubscription(presentation, dualSignalEnabled)

            val selectedMobile =
                selectMobileSignal(
                    snapshot, nativeNoSimVisible, singleDetectedSubId,
                    preferredDataSubscriptionId, dualSignalEnabled,
                )

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
                resolveMobileLevel(
                    selectedSignal, airplaneMode, mobileRecoveryPending, nativeNoSimVisible,
                )

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
            // A drawable event does not establish that the native activity View
            // is visible. Only the native visibility collector can authorize it.
            // Unknown remains hidden until the first verified Home SIM event.
            val nativeMobileActivityId =
                snapshot.mobileActivityResIds[activitySubscriptionId]
                    ?.takeIf { activitySubscriptionId in snapshot.mobileActivityShownIds }

            return RenderModel(
                batteryPercent = batteryPercent,
                charging = battery.charging,
                centerIndicator = centerIndicator,
                chargingIconResId = battery.chargingIconResId,
                networkActivityResId =
                    resolveNetworkActivityResource(
                        centerIndicator = centerIndicator,
                        wifiActivityResId = snapshot.wifiActivityResId,
                        nativeMobileActivityId = nativeMobileActivityId,
                        keepMobileActivityInEmpty =
                            !airplaneMode && !mobileRecoveryPending &&
                                !nativeNoSimVisible && !mobileUnavailableMark,
                    ),
                mobileLevel = mobileLevel,
                mobileUnavailableMark = mobileUnavailableMark,
                effectiveDataSubscriptionId = selectedSubscriptionId,
                dualMobileLevels = dualLevels,
                batterySemanticState = batterySemanticState,
                batterySystemSemanticColor = resolveBatterySystemColor(battery, batterySemanticState),
            )
        }

        private fun selectDataSubscriptionId(
            presentation: PresentationStore.Snapshot,
            defaultDataSubscriptionId: Int,
            dualSignalEnabled: Boolean,
        ): Int =
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

        private fun detectSoleActiveSubscription(
            presentation: PresentationStore.Snapshot,
            dualSignalEnabled: Boolean,
        ): Int? =
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

        private fun selectMobileSignal(
            snapshot: StatusStateStore.Snapshot,
            nativeNoSimVisible: Boolean,
            singleDetectedSubId: Int?,
            preferredDataSubscriptionId: Int,
            dualSignalEnabled: Boolean,
        ): Pair<Int, StatusStateStore.MobileState>? =
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

        private fun resolveMobileLevel(
            selectedSignal: SignalStrength?,
            airplaneMode: Boolean,
            mobileRecoveryPending: Boolean,
            nativeNoSimVisible: Boolean,
        ): Int? =
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

        private fun resolveNetworkActivityResource(
            centerIndicator: CenterIndicator,
            wifiActivityResId: Int?,
            nativeMobileActivityId: Int?,
            keepMobileActivityInEmpty: Boolean,
        ): Int? =
        when (centerIndicator) {
            is CenterIndicator.Wifi -> wifiActivityResId
            is CenterIndicator.MobileType -> nativeMobileActivityId
            CenterIndicator.Empty ->
                if (keepMobileActivityInEmpty) {
                    // Native visibility already accounts for Wi-Fi availability.
                    // Do not gate a verified mobile activity on Guiyuan's
                    // independently observed Wi-Fi state during a handoff.
                    nativeMobileActivityId
                } else null
            else -> null
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
