package com.chaners.guiyuan.ui.screens

import android.content.Context
import android.content.res.Resources
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.xposed.PresentationStore
import com.chaners.guiyuan.xposed.RenderModel
import com.chaners.guiyuan.xposed.network.InternetState

internal enum class PreviewNetworkMode {
    MOBILE,
    WIFI,
}

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

internal enum class PreviewWifiState {
    CONNECTED,
    NO_INTERNET,
    HOTSPOT,
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
    val simPresent: Boolean = true,
    val airplaneMode: Boolean = false,
    val networkMode: PreviewNetworkMode = PreviewNetworkMode.WIFI,
    val mobileNetwork: PreviewMobileNetwork = PreviewMobileNetwork.FIVE_G,
    val mobileSignalLevel: Int = 4,
    val wifiState: PreviewWifiState = PreviewWifiState.CONNECTED,
    val wifiSignalLevel: Int = 3,
    val batteryPercent: Int = 87,
    val batteryMode: PreviewBatteryMode = PreviewBatteryMode.BALANCED,
    val chargingState: PreviewChargingState = PreviewChargingState.NOT_CHARGING,
) {
    val mobileControlsEnabled: Boolean
        get() = simPresent && !airplaneMode

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
        }.getOrNull()

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
                        }.getOrNull()
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
        !simPresent -> PreviewCenterSource.NO_SIM
        mobileNetwork == PreviewMobileNetwork.NONE -> PreviewCenterSource.EMPTY
        else -> PreviewCenterSource.MOBILE
    }

internal fun PreviewSandboxUiState.toRenderModel(
    resources: PreviewSysUiResources,
): RenderModel {
    val wifiLevel = wifiSignalLevel.coerceIn(0, 3)
    val mobileLevel =
        if (mobileControlsEnabled) {
            mobileSignalLevel.coerceIn(0, 4)
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
                    label = mobileNetwork.systemLabel,
                    enhanced = false,
                    internet = InternetState.VALIDATED,
                )
        }

    val charging = chargingState != PreviewChargingState.NOT_CHARGING
    val semanticState =
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

    return RenderModel(
        batteryPercent = batteryPercent.coerceIn(0, 100),
        charging = charging,
        centerIndicator = center,
        chargingIconResId =
            previewChargingResourceId(
                resources = resources,
                state = chargingState,
            ),
        mobileLevel = mobileLevel,
        mobileUnavailableMark = airplaneMode || !simPresent,
        effectiveDataSubscriptionId = -1,
        batterySemanticState = semanticState,
        batterySystemSemanticColor =
            previewBatterySemanticColor(
                resources = resources,
                batteryMode = batteryMode,
                chargingState = chargingState,
                semanticState = semanticState,
            ),
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
