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

            val mobile = selectMobilePresentation(
                snapshot, presentation, defaultDataSubscriptionId, dualSignalEnabled,
            )
            val airplaneMode = snapshot.airplaneMode == true
            val mobileRecoveryPending = snapshot.mobileRecoveryPending
            val nativeNoSimVisible = presentation.statusIcons.noSimVisible
            val nativeMobileWithheld = mobileRecoveryPending || nativeNoSimVisible
            val centerIndicator = resolveCenterIndicator(
                snapshot, presentation, mobile.subscriptionId, dualSignalEnabled,
            ) ?: return null

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

            return RenderModel(
                batteryPercent = batteryPercent,
                charging = battery.charging,
                centerIndicator = centerIndicator,
                chargingIconResId = battery.chargingIconResId,
                networkActivityResId = resolveNetworkActivityResource(
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


        private data class MobileSelection(
            val subscriptionId: Int,
            val level: Int?,
            val unavailableMark: Boolean,
        )

        private fun selectMobilePresentation(
            snapshot: StatusStateStore.Snapshot,
            presentation: PresentationStore.Snapshot,
            defaultDataSubscriptionId: Int,
            dualSignalEnabled: Boolean,
        ): MobileSelection {
            val nativeNoSimVisible = presentation.statusIcons.noSimVisible
            val preferred = selectDataSubscriptionId(
                presentation, defaultDataSubscriptionId, dualSignalEnabled,
            )
            val sole = detectSoleActiveSubscription(presentation, dualSignalEnabled)
            val selected = selectMobileSignal(
                snapshot, nativeNoSimVisible, sole, preferred, dualSignalEnabled,
            )
            val subscriptionId = if (nativeNoSimVisible) {
                -1
            } else {
                selected?.first
                    ?: sole
                    ?: preferred.takeIf { it >= 0 }
                    ?: snapshot.mobile.keys.firstOrNull()?.takeUnless { dualSignalEnabled }
                    ?: -1
            }
            val signal = selected?.second?.signal
            val airplaneMode = snapshot.airplaneMode == true
            val recoveryPending = snapshot.mobileRecoveryPending
            return MobileSelection(
                subscriptionId = subscriptionId,
                level = resolveMobileLevel(
                    signal, airplaneMode, recoveryPending, nativeNoSimVisible,
                ),
                unavailableMark =
                    airplaneMode || nativeNoSimVisible ||
                        (!recoveryPending && signal is SignalStrength.Unavailable),
            )
        }

        private fun resolveCenterIndicator(
            snapshot: StatusStateStore.Snapshot,
            presentation: PresentationStore.Snapshot,
            selectedSubscriptionId: Int,
            dualSignalEnabled: Boolean,
        ): CenterIndicator? {
            val nativeNoSimVisible = presentation.statusIcons.noSimVisible
            val withheld = snapshot.mobileRecoveryPending || nativeNoSimVisible
            val nativeMobile = presentation.mobilePresentation
            // A native aggregate root may belong to a different SIM.
            val typeMismatch =
                dualSignalEnabled &&
                    (nativeMobile?.platformActiveSubscriptionIds?.size ?: 0) > 1 &&
                    nativeMobile?.networkTypeSubscriptionId != selectedSubscriptionId
            return ConnectivityPolicy.resolve(
                wifi = snapshot.wifi,
                airplaneMode = snapshot.airplaneMode == true,
                connectivity = presentation.connectivity,
                mobileType = if (withheld || typeMismatch) null else nativeMobile?.networkType,
                noSimIcon = presentation.statusIcons.noSimIcon?.takeIf { nativeNoSimVisible },
            ) ?: if (withheld || (typeMismatch && !presentation.connectivity.known)) {
                CenterIndicator.Empty
            } else {
                null
            }
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
                is SignalStrength.Level -> selectedSignal.value.coerceIn(0, 4)
                else -> null
            }
        }

        private fun resolveNetworkActivityResource(
            snapshot: StatusStateStore.Snapshot,
            presentation: PresentationStore.Snapshot,
            selectedSubscriptionId: Int,
            centerIndicator: CenterIndicator,
            keepMobileActivityInEmpty: Boolean,
        ): Int? {
            val activitySubscriptionId = resolveActivitySubscriptionId(
                presentation, selectedSubscriptionId,
            )
            // Native visibility, not a drawable event, authorizes activity display.
            val nativeMobileActivityId =
                snapshot.mobileActivityResIds[activitySubscriptionId]
                    ?.takeIf { activitySubscriptionId in snapshot.mobileActivityShownIds }
            return when (centerIndicator) {
                is CenterIndicator.Wifi -> snapshot.wifiActivityResId
                is CenterIndicator.MobileType -> nativeMobileActivityId
                CenterIndicator.Empty ->
                    if (keepMobileActivityInEmpty) nativeMobileActivityId else null
                else -> null
            }
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
                    val platformActive = native?.platformActiveSubscriptionIds
                    val slots = native?.slotSubscriptionIds
                    val sim1Id = slots?.get(0)
                    val sim2Id = slots?.get(1)
                    if (native != null &&
                        native.activeSubsAuthoritative &&
                        (native.mode == NativePresentationResolver.Mode.DUAL_AGGREGATED ||
                            native.mode == NativePresentationResolver.Mode.DUAL_SEPARATE) &&
                        native.activeSubscriptionIds.size == 2 &&
                        native.activeSubscriptionIds[0] != native.activeSubscriptionIds[1] &&
                        platformActive != null &&
                        platformActive.size == 2 &&
                        native.activeSubscriptionIds.all { subscriptionId ->
                            subscriptionId in platformActive
                        } &&
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
