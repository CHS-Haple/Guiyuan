package com.chaners.guiyuan.xposed

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
) {
    companion object {
        fun from(
            snapshot: CombinedStatusStateStore.Snapshot,
            presentation: PresentationStore.Snapshot,
            defaultDataSubscriptionId: Int,
        ): RenderModel? {
            val battery = snapshot.battery ?: return null

            val nativeNoSimVisible = presentation.statusIcons.noSimVisible
            val noSimIcon =
                presentation.statusIcons.noSimIcon
                    ?.takeIf { nativeNoSimVisible }

            val preferredDataSubscriptionId =
                presentation.mobilePresentation
                    ?.effectiveDataSubscriptionId
                    ?.takeIf { subscriptionId -> subscriptionId >= 0 }
                    ?: defaultDataSubscriptionId

            val selectedMobile =
                if (nativeNoSimVisible) {
                    null
                } else {
                    snapshot.mobile[preferredDataSubscriptionId]
                        ?.takeIf { it.signal !is SignalStrength.Unknown }
                        ?.let { preferredDataSubscriptionId to it }
                        ?: snapshot.mobile.entries
                            .firstOrNull { it.value.signal !is SignalStrength.Unknown }
                            ?.let { it.key to it.value }
                }

            val selectedSubscriptionId =
                if (nativeNoSimVisible) {
                    -1
                } else {
                    selectedMobile?.first
                        ?: preferredDataSubscriptionId.takeIf { it >= 0 }
                        ?: snapshot.mobile.keys.firstOrNull()
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
                            presentation.mobilePresentation?.networkType
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
                batterySemanticState = batterySemanticState,
                batterySystemSemanticColor = battery.systemSemanticColor,
            )
        }
    }
}
