package com.chaners.guiyuan.xposed

import android.os.Bundle
import com.chaners.guiyuan.xposed.StatusStateStore.BatteryState
import com.chaners.guiyuan.xposed.StatusStateStore.MobileState
import com.chaners.guiyuan.xposed.StatusStateStore.Snapshot
import com.chaners.guiyuan.xposed.StatusStateStore.WifiState
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.xposed.network.SignalStrength

// Pure Bundle wire-format owner; StatusStateStore remains the only state writer.
internal object HotReloadSnapshotCodec {
    fun export(snapshot: Snapshot): Bundle = Bundle().apply {
        writeBattery(snapshot.battery)
        writeWifi(snapshot.wifi)
        writeMobile(snapshot)
    }

    fun restore(bundle: Bundle): Snapshot {
        val battery = readBattery(bundle)
        val wifi = readWifi(bundle)
        return readMobile(bundle, battery, wifi)
    }

    private fun Bundle.writeBattery(battery: BatteryState?) {
        battery?.let { battery ->
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
    }

    private fun Bundle.writeWifi(wifi: WifiState) {
        when (wifi) {
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
    }

    private fun Bundle.writeMobile(snapshot: Snapshot) {
        putInt(KEY_WIFI_ACTIVITY_RES_ID, snapshot.wifiActivityResId ?: 0)
        putIntArray(
            KEY_MOBILE_ACTIVITY_RES_IDS,
            snapshot.mobileActivityResIds.entries.flatMap { (id, resourceId) ->
                listOf(id, resourceId)
            }.toIntArray(),
        )
        putIntArray(
            KEY_MOBILE_ACTIVITY_SHOWN_IDS,
            snapshot.mobileActivityShownIds.toIntArray(),
        )
        putIntArray(
            KEY_MOBILE_ACTIVITY_VISIBILITY_OBSERVED_IDS,
            snapshot.mobileActivityVisibilityObservedIds.toIntArray(),
        )
        putBoolean(KEY_MOBILE_RECOVERY_PENDING, snapshot.mobileRecoveryPending)
        putInt(
            KEY_AIRPLANE,
            when (snapshot.airplaneMode) {
                null -> AIRPLANE_UNKNOWN
                false -> AIRPLANE_OFF
                true -> AIRPLANE_ON
            },
        )

        val flattened = IntArray(snapshot.mobile.size * MOBILE_STRIDE)
        snapshot.mobile.entries.forEachIndexed { index, (subscriptionId, state) ->
            val base = index * MOBILE_STRIDE
            flattened[base] = subscriptionId
            flattened[base + 2] = encodeSignal(state.signal)
        }
        putIntArray(KEY_MOBILE, flattened)
    }

    private fun readBattery(bundle: Bundle): BatteryState? =
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

    private fun readWifi(bundle: Bundle): WifiState =
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

    private fun readMobile(bundle: Bundle, battery: BatteryState?, wifi: WifiState): Snapshot {
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
    val visibilityObserved =
        (bundle.getIntArray(KEY_MOBILE_ACTIVITY_VISIBILITY_OBSERVED_IDS)
            ?: IntArray(0)).filter { it >= 0 }.toSet()
    val shown =
        (bundle.getIntArray(KEY_MOBILE_ACTIVITY_SHOWN_IDS)
            ?: IntArray(0)).filter { it in visibilityObserved }.toSet()
        return Snapshot(
        battery = battery,
        wifi = wifi,
        wifiActivityResId = bundle.getInt(KEY_WIFI_ACTIVITY_RES_ID).takeIf { it > 0 },
        mobile = mobile,
        mobileActivityResIds = transferredActivity,
        mobileActivityShownIds = shown,
        mobileActivityVisibilityObservedIds = visibilityObserved,
        airplaneMode = airplane,
        mobileRecoveryPending = bundle.getBoolean(KEY_MOBILE_RECOVERY_PENDING, false),

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
    private const val KEY_MOBILE_ACTIVITY_SHOWN_IDS = "mobileActivityShownIds"
    private const val KEY_MOBILE_ACTIVITY_VISIBILITY_OBSERVED_IDS =
        "mobileActivityVisibilityObservedIds"
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
