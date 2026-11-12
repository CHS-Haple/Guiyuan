package com.chaners.guiyuan.xposed

import android.os.Bundle
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.xposed.network.SignalStrength

internal object StatusStateStore {
    @Volatile
    private var current = Snapshot()

    fun snapshot(): Snapshot = current

    @Synchronized
    fun updateBattery(state: BatteryState): Snapshot? {
        val previous = current.battery
        val next =
            if (state.charging && state.chargingIconResId == null && previous?.charging == true) {
                state.copy(chargingIconResId = previous.chargingIconResId)
            } else {
                state
            }
        if (previous == next) return null

        current = current.copy(battery = next)
        return current
    }

    @Synchronized
    fun updateBatteryChargingIcon(resourceId: Int): Snapshot? {
        val battery = current.battery ?: return null
        if (!battery.charging || resourceId == 0) return null

        val next = battery.copy(chargingIconResId = resourceId)
        if (battery == next) {
            return null
        }

        current = current.copy(battery = next)
        return current
    }

    @Synchronized
    fun updateWifi(state: WifiState): Snapshot? {
        if (current.wifi == state) {
            return null
        }

        current = current.copy(
            wifi = state,
            wifiActivityResId = if (state is WifiState.Visible) current.wifiActivityResId else null,
            wifiActivityObserved = current.wifiActivityObserved || state is WifiState.Hidden,
        )
        return current
    }

    @Synchronized
    fun updateWifiActivity(resourceId: Int?): Snapshot? {
        // Explicit absence is an observation too: an older Hot Reload snapshot
        // must not restore a resource after a newer collector has cleared it.
        // The OEM activity drawable can arrive before WifiIcon.Visible during a
        // network handoff. Keep that fresh resource independently of the Wi-Fi
        // icon's visibility; the RenderModel gates actual drawing on Wifi.
        // Entering WifiState.Hidden already clears an older stale resource.
        val next = resourceId?.takeIf { it > 0 }
        val changed = current.wifiActivityResId != next
        if (!changed && current.wifiActivityObserved) return null
        current = current.copy(
            wifiActivityResId = next,
            wifiActivityObserved = true,
        )
        return current.takeIf { changed }
    }

    @Synchronized
    fun updateMobileActivity(subscriptionId: Int, resourceId: Int?): Snapshot? {
        if (subscriptionId < 0) return null
        val nextResourceId = resourceId?.takeIf { it > 0 }
        val changed = current.mobileActivityResIds[subscriptionId] != nextResourceId
        val firstObservation = subscriptionId !in current.mobileActivityObservedIds
        if (!changed && !firstObservation) return null

        val nextResources = if (changed) {
            current.mobileActivityResIds.toMutableMap().apply {
                if (nextResourceId == null) remove(subscriptionId)
                else put(subscriptionId, nextResourceId)
            }.toSortedMap()
        } else current.mobileActivityResIds
        current = current.copy(
            mobileActivityResIds = nextResources,
            mobileActivityObservedIds = if (firstObservation) {
                current.mobileActivityObservedIds + subscriptionId
            } else current.mobileActivityObservedIds,
        )
        return current.takeIf { changed }
    }

    // Only the resource collector can explicitly clear the drawable; coalesce
    // its paired visibility receipt before notifying the renderer.
    @Synchronized
    fun updateMobileActivityResource(
        subscriptionId: Int,
        resourceId: Int?,
        visible: Boolean,
    ): Snapshot? {
        if (subscriptionId < 0) return null
        val before = current
        updateMobileActivity(subscriptionId, resourceId)
        updateMobileActivityVisibility(subscriptionId, visible)
        return current.takeIf { it != before }
    }

    // The native activity drawable and the native visibility Flow have separate collectors.
    // Preserve their independent observations so a later drawable update cannot
    // resurrect activity while the native mobile root remains hidden.
    @Synchronized
    fun updateMobileActivityVisibility(
        subscriptionId: Int,
        visible: Boolean,
    ): Snapshot? {
        if (subscriptionId < 0) return null
        val before = current
        val wasShown = subscriptionId in before.mobileActivityShownIds
        val wasObserved = subscriptionId in before.mobileActivityVisibilityObservedIds
        if (wasShown == visible && wasObserved) return null

        // Only the native resource collector may change drawable identity.
        current = before.copy(
            mobileActivityShownIds =
                if (wasShown == visible) before.mobileActivityShownIds
                else if (visible) before.mobileActivityShownIds + subscriptionId
                else before.mobileActivityShownIds - subscriptionId,
            mobileActivityVisibilityObservedIds =
                if (wasObserved) before.mobileActivityVisibilityObservedIds
                else before.mobileActivityVisibilityObservedIds + subscriptionId,
        )
        return current
    }

    @Synchronized
    fun updateAirplaneMode(enabled: Boolean): Snapshot? {
        if (current.airplaneMode == enabled) {
            return null
        }

        val recoveryPending =
            current.airplaneMode == true &&
                !enabled
        val mobile =
            if (recoveryPending) {
                current.mobile
                    .mapValues { (_, state) ->
                        state.copy(signal = SignalStrength.Unknown)
                    }
                    .toSortedMap()
            } else {
                current.mobile
            }

        current =
            current.copy(
                mobile = mobile,
                mobileActivityResIds = if (enabled) emptyMap() else current.mobileActivityResIds,
                mobileActivityShownIds = if (enabled) emptySet() else current.mobileActivityShownIds,
                mobileActivityObservedIds = if (enabled) {
                    current.mobileActivityObservedIds + current.mobileActivityResIds.keys
                } else current.mobileActivityObservedIds,
                airplaneMode = enabled,
                mobileRecoveryPending = recoveryPending,
            )
        return current
    }

    @Synchronized
    fun completeMobileRecoveryIfReady(
        preferredSubscriptionId: Int,
        mobileTypeReady: Boolean,
        mobileDataEnabled: Boolean?,
    ): Snapshot? {
        if (!current.mobileRecoveryPending) {
            return null
        }

        val preferredReady =
            preferredSubscriptionId >= 0 &&
                current.mobile[preferredSubscriptionId]?.signal is SignalStrength.Level
        val signalReady =
            preferredReady ||
                (
                    preferredSubscriptionId < 0 &&
                        current.mobile.values.any { state ->
                            state.signal is SignalStrength.Level
                        }
                )
        val presentationReady =
            mobileTypeReady ||
                mobileDataEnabled == false

        if (!signalReady || !presentationReady) {
            return null
        }

        current = current.copy(mobileRecoveryPending = false)
        return current
    }

    @Synchronized
    fun updateMobileSignal(
        subscriptionId: Int,
        signal: SignalStrength,
    ): Snapshot? {
        val previous = current.mobile[subscriptionId] ?: MobileState()
        val next = previous.copy(signal = signal)
        if (previous == next) {
            return null
        }

        val mobile = current.mobile.toMutableMap()
        mobile[subscriptionId] = next
        current = current.copy(mobile = mobile.toSortedMap())
        return current
    }

    @Synchronized
    fun exportHotReloadState(): Bundle = HotReloadSnapshotCodec.export(current)

    @Synchronized
    fun restoreHotReloadState(bundle: Bundle?): Snapshot {
        if (bundle == null) {
            current = Snapshot()
            return current
        }

        // New hooks can report state before the saved snapshot is restored.
        val observed = current
        current = mergeHotReloadSnapshots(
            saved = HotReloadSnapshotCodec.restore(bundle),
            fresh = observed,
        )
        observed.battery?.let(::updateBattery)
        return current
    }

    // Reconcile two generations without involving Android Bundle serialization.
    internal fun mergeHotReloadSnapshots(saved: Snapshot, fresh: Snapshot): Snapshot {
        val wifi = fresh.wifi.takeUnless { it == WifiState.Unknown } ?: saved.wifi
        val airplaneMode = fresh.airplaneMode ?: saved.airplaneMode
        // Fresh recovery must survive handoff; an active airplane mode cancels it.
        val mobileRecoveryPending =
            airplaneMode != true && (saved.mobileRecoveryPending || fresh.mobileRecoveryPending)
        return saved.copy(
            wifi = wifi,
            // A fresh native resource can predate WifiIcon.Visible. Honor
            // that observation even if the semantic icon is still Hidden;
            // never resurrect an older saved drawable without a new receipt.
            wifiActivityResId = if (fresh.wifiActivityObserved) {
                fresh.wifiActivityResId
            } else if (wifi is WifiState.Visible) {
                saved.wifiActivityResId
            } else null,
            wifiActivityObserved = fresh.wifiActivityObserved,
            mobile = saved.mobile.toSortedMap().apply { putAll(fresh.mobile) },
            mobileActivityResIds = buildMap {
                if (airplaneMode != true) putAll(saved.mobileActivityResIds)
                fresh.mobileActivityObservedIds.forEach { remove(it) }
                if (airplaneMode != true) putAll(fresh.mobileActivityResIds)
            },
            mobileActivityObservedIds = fresh.mobileActivityObservedIds,
            mobileActivityShownIds = if (airplaneMode == true) emptySet() else {
                (saved.mobileActivityShownIds - fresh.mobileActivityVisibilityObservedIds) +
                    fresh.mobileActivityShownIds
            },
            mobileActivityVisibilityObservedIds =
                saved.mobileActivityVisibilityObservedIds +
                    fresh.mobileActivityVisibilityObservedIds,
            mobileRecoveryPending = mobileRecoveryPending,
            airplaneMode = airplaneMode,
        )
    }

    internal data class Snapshot(
        val battery: BatteryState? = null,
        val wifi: WifiState = WifiState.Unknown,
        val wifiActivityResId: Int? = null,
        val wifiActivityObserved: Boolean = false,
        val mobile: Map<Int, MobileState> = emptyMap(),
        val mobileActivityResIds: Map<Int, Int> = emptyMap(),
        val mobileActivityObservedIds: Set<Int> = emptySet(),
        val mobileActivityShownIds: Set<Int> = emptySet(),
        val mobileActivityVisibilityObservedIds: Set<Int> = emptySet(),
        val airplaneMode: Boolean? = null,
        val mobileRecoveryPending: Boolean = false,
    )

    internal data class BatteryState(
        val percent: Int,
        val charging: Boolean,
        val semanticState: BatterySemanticState? = null,
        val systemSemanticColor: Int? = null,
        val chargingIconResId: Int? = null,
        val lowSystemColor: Int? = null,
    )

    internal sealed interface WifiState {
        data object Unknown : WifiState
        data object Hidden : WifiState

        data class Visible(
            val iconResId: Int?,
            val signal: SignalStrength,
            val internetValidated: Boolean? = null,
        ) : WifiState
    }

    internal data class MobileState(
        val signal: SignalStrength = SignalStrength.Unknown,
    )

}
