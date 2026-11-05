package com.chaners.guiyuan.ui.screens

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_DEFAULT
import com.chaners.guiyuan.sandbox.rethrowSandboxFatal
import com.chaners.guiyuan.xposed.PresentationStore
import com.chaners.guiyuan.xposed.DualMobileLevels
import com.chaners.guiyuan.xposed.RenderModel
import com.chaners.guiyuan.xposed.network.InternetState

internal enum class PreviewNetworkMode {
    MOBILE,
    WIFI,
    NONE, // Keep the two saved ordinals stable.
}

internal val previewNetworkTabs =
    listOf(PreviewNetworkMode.NONE, PreviewNetworkMode.MOBILE, PreviewNetworkMode.WIFI)

internal enum class PreviewMobileNetwork(
    val systemLabel: String,
) {
    NONE(""),
    FOUR_G("4G"),
    FIVE_G("5G"),
    FIVE_GA("5G-A"),

    // Keep the original four ordinals stable for rememberSaveable restoration.
    TWO_G("2G"),
    EDGE("E"),
    THREE_G("3G"),
    H_PLUS("H+"),
    LTE("LTE"),
}

internal enum class PreviewDataSim {
    SIM1,
    SIM2,
}

internal enum class PreviewSimCards {
    BOTH,
    SIM1_ONLY,
    SIM2_ONLY,
    NONE,
}

internal enum class PreviewWifiState {
    CONNECTED,
    NO_INTERNET,
    HOTSPOT,
    DUAL_WIFI,
}

// Sandbox-only selection; real status-bar activity still comes from native collectors.
internal enum class PreviewNetworkActivity {
    UPLOAD,
    DOWNLOAD,
    BOTH,
    IDLE,
}

internal enum class PreviewBatteryMode {
    BALANCED,
    POWER_SAVE,
    PERFORMANCE,
    SUPER_POWER_SAVE,
}

internal enum class PreviewChargingState {
    NOT_CHARGING,
    CHARGING,
    SUPER_FAST_CHARGING,
}

internal enum class PreviewCenterSource {
    WIFI,
    AIRPLANE,
    NO_SIM,
    EMPTY,
    MOBILE,
}

internal data class PreviewSandboxUiState(
    val airplaneMode: Boolean = false,
    val networkMode: PreviewNetworkMode = PreviewNetworkMode.WIFI,
    val mobileNetwork: PreviewMobileNetwork = PreviewMobileNetwork.FIVE_G,
    val mobileSignalLevel: Int = 4,
    val sim2SignalLevel: Int = 4,
    val sim2MobileNetwork: PreviewMobileNetwork = PreviewMobileNetwork.FOUR_G,
    val dataSim: PreviewDataSim = PreviewDataSim.SIM1,
    val simCards: PreviewSimCards = PreviewSimCards.SIM1_ONLY,
    val wifiState: PreviewWifiState = PreviewWifiState.CONNECTED,
    val wifiSignalLevel: Int = 3,
    val networkActivity: PreviewNetworkActivity = PreviewNetworkActivity.BOTH,
    val batteryPercent: Int = 87,
    val batteryMode: PreviewBatteryMode = PreviewBatteryMode.BALANCED,
    val chargingState: PreviewChargingState = PreviewChargingState.NOT_CHARGING,
) {
    val sim1Active: Boolean
        get() = simCards == PreviewSimCards.BOTH || simCards == PreviewSimCards.SIM1_ONLY

    val sim2Active: Boolean
        get() = simCards == PreviewSimCards.BOTH || simCards == PreviewSimCards.SIM2_ONLY

    val hasSim: Boolean
        get() = sim1Active || sim2Active

    val mobileControlsEnabled: Boolean
        get() = hasSim && !airplaneMode

    val effectiveDataSim: PreviewDataSim?
        get() = when (simCards) {
            PreviewSimCards.BOTH -> dataSim
            PreviewSimCards.SIM1_ONLY -> PreviewDataSim.SIM1
            PreviewSimCards.SIM2_ONLY -> PreviewDataSim.SIM2
            PreviewSimCards.NONE -> null
        }

    val dataMobileNetwork: PreviewMobileNetwork
        get() = when (effectiveDataSim) {
            PreviewDataSim.SIM1 -> mobileNetwork
            PreviewDataSim.SIM2 -> sim2MobileNetwork
            null -> PreviewMobileNetwork.NONE
        }

    val dataSignalLevel: Int
        get() = when (effectiveDataSim) {
            PreviewDataSim.SIM1 -> mobileSignalLevel
            PreviewDataSim.SIM2 -> sim2SignalLevel
            null -> 0
        }

    val displayedSignalLevel: Int
        get() =
            when {
                sim1Active && !sim2Active -> mobileSignalLevel
                sim2Active && !sim1Active -> sim2SignalLevel
                else -> dataSignalLevel
            }

    val dualLevels: DualMobileLevels?
        get() =
            if (mobileControlsEnabled && sim1Active && sim2Active) {
                DualMobileLevels(
                    sim1 = mobileSignalLevel.coerceIn(0, 4),
                    sim2 = sim2SignalLevel.coerceIn(0, 4),
                )
            } else null

    val mobileOptionsVisible: Boolean
        get() = networkMode == PreviewNetworkMode.MOBILE && mobileControlsEnabled
}

internal class PreviewSysUiResources(
    context: Context,
) {
    private val resources: Resources? =
        runCatching {
            context
                .createPackageContext(SYSTEM_UI_PACKAGE, 0)
                .resources
        }.onFailure(::rethrowSandboxFatal).getOrNull()

    fun drawableId(vararg names: String): Int? =
        resources
            ?.let { systemResources ->
                names
                    .asSequence()
                    .map { name ->
                        systemResources.getIdentifier(
                            name,
                            "drawable",
                            SYSTEM_UI_PACKAGE,
                        )
                    }
                    .firstOrNull { id -> id != 0 }
            }

    fun color(vararg names: String): Int? =
        resources
            ?.let { systemResources ->
                names
                    .asSequence()
                    .mapNotNull { name ->
                        val id =
                            systemResources.getIdentifier(
                                name,
                                "color",
                                SYSTEM_UI_PACKAGE,
                            )
                        id.takeIf { it != 0 }
                    }
                    .mapNotNull { id ->
                        runCatching {
                            systemResources.getColor(id, null)
                        }.onFailure(::rethrowSandboxFatal).getOrNull()
                    }
                    .firstOrNull()
            }

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}

internal fun PreviewSandboxUiState.previewCenterSource(): PreviewCenterSource =
    when {
        networkMode == PreviewNetworkMode.WIFI -> PreviewCenterSource.WIFI
        airplaneMode -> PreviewCenterSource.AIRPLANE
        !hasSim -> PreviewCenterSource.NO_SIM
        networkMode == PreviewNetworkMode.NONE ||
            dataMobileNetwork == PreviewMobileNetwork.NONE -> PreviewCenterSource.EMPTY
        else -> PreviewCenterSource.MOBILE
    }

internal fun PreviewSandboxUiState.toRenderModel(
    resources: PreviewSysUiResources,
    mobileTypeface: Typeface? = null,
    lowBatteryThreshold: Int = LOW_BATTERY_THRESHOLD_DEFAULT,
    nativeLowColor: Int? = null,
): RenderModel {
    val wifiLevel = wifiSignalLevel.coerceIn(0, 3)
    val mobileLevel =
        if (mobileControlsEnabled) {
            displayedSignalLevel.coerceIn(0, 4)
        } else {
            null
        }

    val center =
        when (previewCenterSource()) {
            PreviewCenterSource.WIFI ->
                CenterIndicator.Wifi(
                    segments = wifiLevel,
                    internet =
                        if (wifiState == PreviewWifiState.NO_INTERNET) {
                            InternetState.NO_INTERNET
                        } else {
                            InternetState.VALIDATED
                        },
                    nativeResourceId =
                        previewWifiResourceId(
                            resources = resources,
                            state = wifiState,
                            level = wifiLevel,
                        ),
                )

            PreviewCenterSource.AIRPLANE -> CenterIndicator.Airplane

            PreviewCenterSource.NO_SIM ->
                resources
                    .drawableId("stat_sys_no_sim")
                    ?.let { resourceId ->
                        CenterIndicator.NoSim(
                            PresentationStore.NativeIconResource(
                                packageName = SYSTEM_UI_PACKAGE,
                                resourceId = resourceId,
                            ),
                        )
                    }
                    ?: CenterIndicator.Empty

            PreviewCenterSource.EMPTY -> CenterIndicator.Empty

            PreviewCenterSource.MOBILE ->
                CenterIndicator.MobileType(
                    label = dataMobileNetwork.systemLabel,
                    enhanced = false,
                    internet = InternetState.VALIDATED,
                    typeface = mobileTypeface,
                )
        }

    val charging = chargingState != PreviewChargingState.NOT_CHARGING
    val baseState =
        if (charging) {
            BatterySemanticState.CHARGING
        } else {
            when (batteryMode) {
                PreviewBatteryMode.BALANCED -> BatterySemanticState.NORMAL
                PreviewBatteryMode.POWER_SAVE -> BatterySemanticState.POWER_SAVE
                PreviewBatteryMode.PERFORMANCE -> BatterySemanticState.PERFORMANCE
                PreviewBatteryMode.SUPER_POWER_SAVE ->
                    BatterySemanticState.SUPER_POWER_SAVE
            }
        }
    val semanticState = BatterySemanticState.atThreshold(
        baseState,
        batteryPercent,
        lowBatteryThreshold,
    )

    return RenderModel(
        batteryPercent = batteryPercent.coerceIn(0, 100),
        charging = charging,
        centerIndicator = center,
        chargingIconResId =
            previewChargingResourceId(
                resources = resources,
                state = chargingState,
            ),
        networkActivityResId = if (networkActivity == PreviewNetworkActivity.IDLE) {
            // Null is the native idle contract. The existing user setting
            // decides whether the dim idle arrows should remain visible.
            null
        } else {
            resources.drawableId(
                *previewNetworkActivityResourceNames(
                    centerSource = previewCenterSource(),
                    state = networkActivity,
                ).toTypedArray(),
            )
        },
        mobileLevel = mobileLevel,
        dualMobileLevels = dualLevels,
        mobileUnavailableMark = airplaneMode || !hasSim,
        effectiveDataSubscriptionId = -1,
        batterySemanticState = semanticState,
        batterySystemSemanticColor =
            previewBatterySemanticColor(
                resources = resources,
                batteryMode = batteryMode,
                chargingState = chargingState,
                semanticState = semanticState,
            ) ?: nativeLowColor.takeIf { semanticState == BatterySemanticState.LOW },
    )
}

internal fun previewWifiResourceNames(
    state: PreviewWifiState,
    level: Int,
): List<String> =
    when (state) {
        PreviewWifiState.CONNECTED ->
            listOf("stat_sys_wifi_signal_$level")

        PreviewWifiState.NO_INTERNET ->
            listOf("stat_sys_wifi_signal_unavailable_$level")

        PreviewWifiState.HOTSPOT ->
            listOf("stat_sys_hotspot_signal_$level")

        PreviewWifiState.DUAL_WIFI ->
            listOf("stat_sys_slave_wifi_signal_$level")
    }

private fun previewWifiResourceId(
    resources: PreviewSysUiResources,
    state: PreviewWifiState,
    level: Int,
): Int? =
    resources.drawableId(
        *previewWifiResourceNames(
            state = state,
            level = level.coerceIn(0, 3),
        ).toTypedArray(),
    )

// All IDs are resolved against the installed SystemUI. Never substitute an
// app-owned approximation when a requested native variant is unavailable.
internal fun previewNetworkActivityResourceNames(
    centerSource: PreviewCenterSource,
    state: PreviewNetworkActivity,
): List<String> =
    when (centerSource) {
        PreviewCenterSource.WIFI -> when (state) {
            PreviewNetworkActivity.UPLOAD -> listOf("stat_sys_wifi_out")
            PreviewNetworkActivity.DOWNLOAD -> listOf("stat_sys_wifi_in")
            PreviewNetworkActivity.BOTH -> listOf("stat_sys_wifi_inout")
            PreviewNetworkActivity.IDLE -> emptyList()
        }
        PreviewCenterSource.MOBILE -> when (state) {
            PreviewNetworkActivity.UPLOAD ->
                listOf("stat_sys_signal_out_left", "stat_sys_signal_out")
            PreviewNetworkActivity.DOWNLOAD ->
                listOf("stat_sys_signal_in_left", "stat_sys_signal_in")
            PreviewNetworkActivity.BOTH ->
                listOf("stat_sys_signal_inout_left", "stat_sys_signal_inout")
            PreviewNetworkActivity.IDLE -> emptyList()
        }
        else -> emptyList()
    }

internal fun previewChargingResourceNames(
    state: PreviewChargingState,
): List<String> =
    when (state) {
        PreviewChargingState.NOT_CHARGING -> emptyList()
        PreviewChargingState.CHARGING ->
            listOf(
                "hollow_battery_meter_charging",
                "tiny_battery_charging",
            )
        PreviewChargingState.SUPER_FAST_CHARGING ->
            listOf(
                "hollow_battery_meter_quick_charging",
                "tiny_battery_quick_charging",
            )
    }

private fun previewChargingResourceId(
    resources: PreviewSysUiResources,
    state: PreviewChargingState,
): Int? {
    val names = previewChargingResourceNames(state)
    if (names.isEmpty()) return null
    return resources.drawableId(*names.toTypedArray())
}

private fun previewBatterySemanticColor(
    resources: PreviewSysUiResources,
    batteryMode: PreviewBatteryMode,
    chargingState: PreviewChargingState,
    semanticState: BatterySemanticState,
): Int? =
    when {
        chargingState == PreviewChargingState.SUPER_FAST_CHARGING ->
            resources.color(
                "status_bar_battery_super_fast_charging",
                "status_bar_battery_quick_charging",
                "status_bar_battery_charging",
            )

        chargingState == PreviewChargingState.CHARGING ->
            resources.color("status_bar_battery_charging")

        batteryMode == PreviewBatteryMode.SUPER_POWER_SAVE ->
            resources.color(
                "status_bar_battery_super_power_save",
                "status_bar_battery_super_save",
                "status_bar_battery_power_save",
            )

        semanticState == BatterySemanticState.POWER_SAVE ->
            resources.color("status_bar_battery_power_save")

        semanticState == BatterySemanticState.PERFORMANCE ->
            resources.color("status_bar_battery_performance")

        else -> null
    }

private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
