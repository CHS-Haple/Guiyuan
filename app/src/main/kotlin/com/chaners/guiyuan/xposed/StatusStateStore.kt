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
        val next = resourceId?.takeIf { it > 0 && current.wifi !is WifiState.Hidden }
        val changed = current.wifiActivityResId != next
        current = current.copy(
            wifiActivityResId = next,
            wifiActivityObserved = true,
        )
        return current.takeIf { changed }
    }

    @Synchronized
    fun updateMobileActivity(subscriptionId: Int, resourceId: Int?): Snapshot? {
        if (subscriptionId < 0) return null
        val next = current.mobileActivityResIds.toMutableMap()
        if (resourceId == null || resourceId <= 0) next.remove(subscriptionId)
        else next[subscriptionId] = resourceId
        val changed = next != current.mobileActivityResIds
        current = current.copy(
            mobileActivityResIds = if (changed) next.toSortedMap() else current.mobileActivityResIds,
            mobileActivityObservedIds = current.mobileActivityObservedIds + subscriptionId,
        )
        return current.takeIf { changed }
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
    fun exportHotReloadState(): Bundle =
        Bundle().apply {
            current.battery?.let { battery ->
                putBoolean(KEY_BATTERY_PRESENT, true)
                putInt(KEY_BATTERY_PERCENT, battery.percent)
                putBoolean(KEY_BATTERY_CHARGING, battery.charging)
                putInt(KEY_BATTERY_CHARGING_ICON_RES_ID, battery.chargingIconResId ?: 0)
                battery.semanticState?.let { putString(KEY_BATTERY_SEMANTIC, it.name) }
                battery.systemSemanticColor?.let { color ->
                    putBoolean(KEY_BATTERY_SYSTEM_COLOR_PRESENT, true)
                    putInt(KEY_BATTERY_SYSTEM_COLOR, color)
                }
                battery.lowSystemColor?.let { putInt(KEY_BATTERY_LOW_SYSTEM_COLOR, it) }
            }
            when (val wifi = current.wifi) {
                WifiState.Unknown -> putInt(KEY_WIFI_KIND, WIFI_KIND_UNKNOWN)
                WifiState.Hidden -> putInt(KEY_WIFI_KIND, WIFI_KIND_HIDDEN)
                is WifiState.Visible -> {
                    putInt(KEY_WIFI_KIND, WIFI_KIND_VISIBLE)
                    putInt(KEY_WIFI_RES_ID, wifi.iconResId ?: 0)
                    putInt(KEY_WIFI_SIGNAL, encodeSignal(wifi.signal))
                    putInt(
                        KEY_WIFI_INTERNET,
                        when (wifi.internetValidated) {
                            null -> WIFI_INTERNET_UNKNOWN
                            false -> WIFI_INTERNET_NO
                            true -> WIFI_INTERNET_YES
                        },
                    )
                }
            }
            putInt(KEY_WIFI_ACTIVITY_RES_ID, current.wifiActivityResId ?: 0)
            putIntArray(
                KEY_MOBILE_ACTIVITY_RES_IDS,
                current.mobileActivityResIds.entries.flatMap { (id, resourceId) ->
                    listOf(id, resourceId)
                }.toIntArray(),
            )
            putBoolean(KEY_MOBILE_RECOVERY_PENDING, current.mobileRecoveryPending)
            putInt(
                KEY_AIRPLANE,
                when (current.airplaneMode) {
                    null -> AIRPLANE_UNKNOWN
                    false -> AIRPLANE_OFF
                    true -> AIRPLANE_ON
                },
            )

            val flattened = IntArray(current.mobile.size * MOBILE_STRIDE)
            current.mobile.entries.forEachIndexed { index, (subscriptionId, state) ->
                val base = index * MOBILE_STRIDE
                flattened[base] = subscriptionId
                flattened[base + 2] = encodeSignal(state.signal)
            }
            putIntArray(KEY_MOBILE, flattened)
        }

    @Synchronized
    fun restoreHotReloadState(bundle: Bundle?): Snapshot {
        if (bundle == null) {
            current = Snapshot()
            return current
        }

        // New hooks can report state before the saved snapshot is restored.
        val observed = current
        val battery =
            if (bundle.getBoolean(KEY_BATTERY_PRESENT, false)) {
                BatteryState(
                    percent = bundle.getInt(KEY_BATTERY_PERCENT),
                    charging = bundle.getBoolean(KEY_BATTERY_CHARGING),
                    chargingIconResId =
                        bundle.getInt(KEY_BATTERY_CHARGING_ICON_RES_ID)
                            .takeIf { it != 0 },
                    semanticState =
                        bundle.getString(KEY_BATTERY_SEMANTIC)?.let { name ->
                            runCatching {
                                BatterySemanticState.valueOf(name)
                            }.getOrNull()
                        },
                    systemSemanticColor =
                        if (bundle.getBoolean(KEY_BATTERY_SYSTEM_COLOR_PRESENT, false)) {
                            bundle.getInt(KEY_BATTERY_SYSTEM_COLOR)
                        } else null,
                    lowSystemColor =
                        if (bundle.containsKey(KEY_BATTERY_LOW_SYSTEM_COLOR)) {
                            bundle.getInt(KEY_BATTERY_LOW_SYSTEM_COLOR)
                        } else null,
                )
            } else {
                null
            }

        val wifi =
            when (bundle.getInt(KEY_WIFI_KIND, WIFI_KIND_UNKNOWN)) {
                WIFI_KIND_HIDDEN -> WifiState.Hidden
                WIFI_KIND_VISIBLE ->
                    WifiState.Visible(
                        iconResId = bundle.getInt(KEY_WIFI_RES_ID).takeIf { it != 0 },
                        signal = decodeSignal(bundle.getInt(KEY_WIFI_SIGNAL, SIGNAL_UNKNOWN)),
                        internetValidated =
                            when (
                                bundle.getInt(
                                    KEY_WIFI_INTERNET,
                                    WIFI_INTERNET_UNKNOWN,
                                )
                            ) {
                                WIFI_INTERNET_NO -> false
                                WIFI_INTERNET_YES -> true
                                else -> null
                            },
                    )
                else -> WifiState.Unknown
            }

        val airplane =
            when (bundle.getInt(KEY_AIRPLANE, AIRPLANE_UNKNOWN)) {
                AIRPLANE_OFF -> false
                AIRPLANE_ON -> true
                else -> null
            }

        val mobile = sortedMapOf<Int, MobileState>()
        val flattened = bundle.getIntArray(KEY_MOBILE) ?: IntArray(0)
        var offset = 0
        while (offset + MOBILE_STRIDE <= flattened.size) {
            val subscriptionId = flattened[offset]
            mobile[subscriptionId] =
                MobileState(
                    signal = decodeSignal(flattened[offset + 2]),
                )
            offset += MOBILE_STRIDE
        }

        val activity = bundle.getIntArray(KEY_MOBILE_ACTIVITY_RES_IDS) ?: IntArray(0)
        val transferredActivity = buildMap {
            for (index in 0 until activity.size - 1 step 2) {
                if (activity[index] >= 0 && activity[index + 1] > 0) {
                    put(activity[index], activity[index + 1])
                }
            }
        }
        current = mergeHotReloadSnapshots(
            saved = Snapshot(
                battery = battery,
                wifi = wifi,
                wifiActivityResId = bundle.getInt(KEY_WIFI_ACTIVITY_RES_ID).takeIf { it > 0 },
                mobile = mobile,
                mobileActivityResIds = transferredActivity,
                airplaneMode = airplane,
                mobileRecoveryPending = bundle.getBoolean(KEY_MOBILE_RECOVERY_PENDING, false),
            ),
            fresh = observed,
        )
        observed.battery?.let(::updateBattery)
        return current
    }

    // Reconcile two generations without involving Android Bundle serialization.
    internal fun mergeHotReloadSnapshots(saved: Snapshot, fresh: Snapshot): Snapshot {
        val wifi = fresh.wifi.takeUnless { it == WifiState.Unknown } ?: saved.wifi
        return saved.copy(
            wifi = wifi,
            wifiActivityResId = if (wifi is WifiState.Visible) {
                if (fresh.wifiActivityObserved) fresh.wifiActivityResId
                else saved.wifiActivityResId
            } else null,
            wifiActivityObserved = fresh.wifiActivityObserved,
            mobile = saved.mobile.toSortedMap().apply { putAll(fresh.mobile) },
            mobileActivityResIds = buildMap {
                if (fresh.airplaneMode != true) putAll(saved.mobileActivityResIds)
                fresh.mobileActivityObservedIds.forEach { remove(it) }
                if (fresh.airplaneMode != true) putAll(fresh.mobileActivityResIds)
            },
            mobileActivityObservedIds = fresh.mobileActivityObservedIds,
            airplaneMode = fresh.airplaneMode ?: saved.airplaneMode,
        )
    }

    private fun encodeSignal(signal: SignalStrength): Int =
        when (signal) {
            SignalStrength.Unknown -> SIGNAL_UNKNOWN
            SignalStrength.Unavailable -> SIGNAL_UNAVAILABLE
            is SignalStrength.Level -> signal.value.coerceIn(0, 4)
        }

    private fun decodeSignal(value: Int): SignalStrength =
        when (value) {
            SIGNAL_UNKNOWN -> SignalStrength.Unknown
            SIGNAL_UNAVAILABLE -> SignalStrength.Unavailable
            else -> SignalStrength.Level(value.coerceIn(0, 4))
        }

    internal data class Snapshot(
        val battery: BatteryState? = null,
        val wifi: WifiState = WifiState.Unknown,
        val wifiActivityResId: Int? = null,
        val wifiActivityObserved: Boolean = false,
        val mobile: Map<Int, MobileState> = emptyMap(),
        val mobileActivityResIds: Map<Int, Int> = emptyMap(),
        val mobileActivityObservedIds: Set<Int> = emptySet(),
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

    private const val KEY_BATTERY_PRESENT = "batteryPresent"
    private const val KEY_BATTERY_PERCENT = "batteryPercent"
    private const val KEY_BATTERY_CHARGING = "batteryCharging"
    private const val KEY_BATTERY_CHARGING_ICON_RES_ID = "batteryChargingIconResId"
    private const val KEY_BATTERY_SEMANTIC = "batterySemantic"
    private const val KEY_BATTERY_SYSTEM_COLOR_PRESENT = "batterySystemColorPresent"
    private const val KEY_BATTERY_SYSTEM_COLOR = "batterySystemColor"
    private const val KEY_BATTERY_LOW_SYSTEM_COLOR = "batteryLowSystemColor"
    private const val KEY_WIFI_KIND = "wifiKind"
    private const val KEY_WIFI_RES_ID = "wifiResId"
    private const val KEY_WIFI_ACTIVITY_RES_ID = "wifiActivityResId"
    private const val KEY_MOBILE_ACTIVITY_RES_IDS = "mobileActivityResIds"
    private const val KEY_WIFI_SIGNAL = "wifiSignal"
    private const val KEY_WIFI_INTERNET = "wifiInternet"
    private const val KEY_AIRPLANE = "airplane"
    private const val KEY_MOBILE_RECOVERY_PENDING = "mobileRecoveryPending"
    private const val KEY_MOBILE = "mobile"

    private const val WIFI_KIND_UNKNOWN = 0
    private const val WIFI_KIND_HIDDEN = 1
    private const val WIFI_KIND_VISIBLE = 2
    private const val WIFI_INTERNET_UNKNOWN = -1
    private const val WIFI_INTERNET_NO = 0
    private const val WIFI_INTERNET_YES = 1

    private const val AIRPLANE_UNKNOWN = -1
    private const val AIRPLANE_OFF = 0
    private const val AIRPLANE_ON = 1

    private const val SIGNAL_UNKNOWN = -2
    private const val SIGNAL_UNAVAILABLE = -1
    // Keep the old Hot Reload layout so adjacent generations can still hand off.
    private const val MOBILE_STRIDE = 5
}
