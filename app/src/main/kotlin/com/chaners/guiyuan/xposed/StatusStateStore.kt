package com.chaners.guiyuan.xposed

import android.os.Bundle

internal object StatusStateStore {
    @Volatile
    private var current = Snapshot()

    fun snapshot(): Snapshot = current

    @Synchronized
    fun updateBattery(state: BatteryState): Snapshot? {
        if (current.battery == state) {
            return null
        }

        current = current.copy(battery = state)
        return current
    }

    @Synchronized
    fun updateBatteryChargingIcon(resourceId: Int?): Snapshot? {
        val battery = current.battery ?: return null
        val next =
            battery.copy(
                chargingIconResId =
                    if (battery.charging) {
                        resourceId?.takeIf { it != 0 }
                    } else {
                        null
                    },
            )
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

        current = current.copy(wifi = state)
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
                        state.copy(
                            signalResId = null,
                            signal = SignalStrength.Unknown,
                        )
                    }
                    .toSortedMap()
            } else {
                current.mobile
            }

        current =
            current.copy(
                mobile = mobile,
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
    fun updateMobile(update: MobileIconUpdate): Snapshot? {
        val previous = current.mobile[update.subscriptionId] ?: MobileState()
        val resourceId = update.resourceId?.takeIf { it != 0 }
        val next = when (update.kind) {
            MobileIconKind.SIGNAL -> previous.copy(
                signalResId = resourceId,
                signal = update.signal ?: SignalStrength.Unknown,
            )
            MobileIconKind.VOLTE -> previous.copy(volteResId = resourceId)
            MobileIconKind.VOWIFI -> previous.copy(vowifiResId = resourceId)
        }

        if (previous == next) {
            return null
        }

        val mobile = current.mobile.toMutableMap()
        mobile[update.subscriptionId] = next
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
                putInt(KEY_CHARGING_ICON_RES, battery.chargingIconResId ?: 0)
                battery.semanticState?.let { putString(KEY_BATTERY_SEMANTIC, it.name) }
                battery.systemSemanticColor?.let { color ->
                    putBoolean(KEY_SYSTEM_COLOR_PRESENT, true)
                    putInt(KEY_BATTERY_SYSTEM_COLOR, color)
                }
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
                flattened[base + 1] = state.signalResId ?: 0
                flattened[base + 2] = encodeSignal(state.signal)
                flattened[base + 3] = state.volteResId ?: 0
                flattened[base + 4] = state.vowifiResId ?: 0
            }
            putIntArray(KEY_MOBILE, flattened)
        }

    @Synchronized
    fun restoreHotReloadState(bundle: Bundle?): Snapshot {
        if (bundle == null) {
            current = Snapshot()
            return current
        }

        val battery =
            if (bundle.getBoolean(KEY_BATTERY_PRESENT, false)) {
                BatteryState(
                    percent = bundle.getInt(KEY_BATTERY_PERCENT),
                    charging = bundle.getBoolean(KEY_BATTERY_CHARGING),
                    chargingIconResId =
                        bundle.getInt(KEY_CHARGING_ICON_RES)
                            .takeIf { it != 0 },
                    semanticState =
                        bundle.getString(KEY_BATTERY_SEMANTIC)?.let { name ->
                            runCatching {
                                BatterySemanticState.valueOf(name)
                            }.getOrNull()
                        },
                    systemSemanticColor =
                        if (bundle.getBoolean(KEY_SYSTEM_COLOR_PRESENT, false)) {
                            bundle.getInt(KEY_BATTERY_SYSTEM_COLOR)
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
                    signalResId = flattened[offset + 1].takeIf { it != 0 },
                    signal = decodeSignal(flattened[offset + 2]),
                    volteResId = flattened[offset + 3].takeIf { it != 0 },
                    vowifiResId = flattened[offset + 4].takeIf { it != 0 },
                )
            offset += MOBILE_STRIDE
        }

        current =
            Snapshot(
                battery = battery,
                wifi = wifi,
                mobile = mobile,
                airplaneMode = airplane,
                mobileRecoveryPending =
                    bundle.getBoolean(KEY_MOBILE_RECOVERY_PENDING, false),
            )
        return current
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
        val mobile: Map<Int, MobileState> = emptyMap(),
        val airplaneMode: Boolean? = null,
        val mobileRecoveryPending: Boolean = false,
    ) {
        val logLine: String
            get() {
                val batteryText = battery?.let { state ->
                    state.percent.toString() + ":" +
                        (if (state.charging) "charging" else "discharging")
                } ?: "unknown"

                val wifiText = when (val state = wifi) {
                    WifiState.Unknown -> "unknown"
                    WifiState.Hidden -> "hidden"
                    is WifiState.Visible ->
                        "visible:" + state.signal.logToken +
                            ":internet=" +
                            (
                                state.internetValidated
                                    ?.let { validated ->
                                        if (validated) "validated" else "no-internet"
                                    }
                                    ?: "unknown"
                            ) +
                            ":res=" + (state.iconResId ?: 0)
                }

                val mobileText = mobile.entries.joinToString(
                    prefix = "[",
                    postfix = "]",
                    separator = ";",
                ) { (subscriptionId, state) ->
                    subscriptionId.toString() +
                        ":signal=" + state.signal.logToken + ":res=" + (state.signalResId ?: 0) +
                        ",volte=" + (state.volteResId ?: 0) +
                        ",vowifi=" + (state.vowifiResId ?: 0)
                }

                return "battery=$batteryText wifi=$wifiText mobile=$mobileText " +
                    "airplane=" + (airplaneMode?.toString() ?: "unknown") +
                    " mobileRecovery=" +
                    (if (mobileRecoveryPending) "searching" else "ready")
            }
    }

    internal data class BatteryState(
        val percent: Int,
        val charging: Boolean,
        val semanticState: BatterySemanticState? = null,
        val systemSemanticColor: Int? = null,
        val chargingIconResId: Int? = null,
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

    internal enum class MobileIconKind {
        SIGNAL,
        VOLTE,
        VOWIFI,
    }

    internal data class MobileIconUpdate(
        val subscriptionId: Int,
        val kind: MobileIconKind,
        val resourceId: Int?,
        val signal: SignalStrength? = null,
    )

    internal data class MobileState(
        val signalResId: Int? = null,
        val signal: SignalStrength = SignalStrength.Unknown,
        val volteResId: Int? = null,
        val vowifiResId: Int? = null,
    )

    private const val KEY_BATTERY_PRESENT = "batteryPresent"
    private const val KEY_BATTERY_PERCENT = "batteryPercent"
    private const val KEY_BATTERY_CHARGING = "batteryCharging"
    private const val KEY_CHARGING_ICON_RES = "batteryChargingIconResId"
    private const val KEY_BATTERY_SEMANTIC = "batterySemantic"
    private const val KEY_SYSTEM_COLOR_PRESENT = "batterySystemColorPresent"
    private const val KEY_BATTERY_SYSTEM_COLOR = "batterySystemColor"
    private const val KEY_WIFI_KIND = "wifiKind"
    private const val KEY_WIFI_RES_ID = "wifiResId"
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
    private const val MOBILE_STRIDE = 5
}
