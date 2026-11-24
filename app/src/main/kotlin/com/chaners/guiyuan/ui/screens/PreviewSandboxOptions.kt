package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R

internal data class PreviewSandboxNetworkOptions(
    val networkMode: List<String>,
    val dataSim: List<String>,
    val dualSim: List<String>,
    val mobileChoices: List<Pair<PreviewMobileNetwork, String>>,
    val mobileOptions: List<String>,
    val wifi: List<String>,
    val activity: List<String>,
)

internal data class PreviewSandboxBatteryOptions(
    val modes: List<String>,
    val charging: List<String>,
)

internal data class PreviewSandboxOptions(
    val network: PreviewSandboxNetworkOptions,
    val battery: PreviewSandboxBatteryOptions,
)

@Composable
internal fun previewSandboxOptions(): PreviewSandboxOptions =
    PreviewSandboxOptions(previewSandboxNetworkOptions(), previewSandboxBatteryOptions())

@Composable
private fun previewSandboxNetworkOptions(): PreviewSandboxNetworkOptions {
    val networkModeOptions =
        listOf(
            stringResource(R.string.home_preview_network_none),
            stringResource(R.string.home_preview_network_mode_mobile),
            stringResource(R.string.home_preview_network_mode_wifi),
        )
    val dataSimOptions =
        listOf(
            stringResource(R.string.home_preview_data_sim1),
            stringResource(R.string.home_preview_data_sim2),
        )
    val dualSimOptions =
        listOf(
            stringResource(R.string.home_preview_sim_both),
            stringResource(R.string.home_preview_sim1_only),
            stringResource(R.string.home_preview_sim2_only),
            stringResource(R.string.home_preview_sim_none),
        )
    val mobileNetworkChoices =
        listOf(
            PreviewMobileNetwork.TWO_G to stringResource(R.string.home_preview_network_2g),
            PreviewMobileNetwork.EDGE to stringResource(R.string.home_preview_network_edge),
            PreviewMobileNetwork.THREE_G to stringResource(R.string.home_preview_network_3g),
            PreviewMobileNetwork.H_PLUS to stringResource(R.string.home_preview_network_h_plus),
            PreviewMobileNetwork.FOUR_G to stringResource(R.string.home_preview_network_4g),
            PreviewMobileNetwork.LTE to stringResource(R.string.home_preview_network_lte),
            PreviewMobileNetwork.FIVE_G to stringResource(R.string.home_preview_network_5g),
            PreviewMobileNetwork.FIVE_GA to stringResource(R.string.home_preview_network_5ga),
        )
    val mobileNetworkOptions = mobileNetworkChoices.map { it.second }
    val wifiOptions =
        listOf(
            stringResource(R.string.home_preview_wifi_connected),
            stringResource(R.string.home_preview_wifi_no_internet),
            stringResource(R.string.home_preview_wifi_hotspot),
            stringResource(R.string.home_preview_wifi_dual_acceleration),
        )
    val networkActivityOptions = listOf(
        stringResource(R.string.home_preview_activity_upload),
        stringResource(R.string.home_preview_activity_download),
        stringResource(R.string.home_preview_activity_both),
        stringResource(R.string.home_preview_activity_idle),
    )

    return PreviewSandboxNetworkOptions(
        networkModeOptions, dataSimOptions, dualSimOptions, mobileNetworkChoices,
        mobileNetworkOptions, wifiOptions, networkActivityOptions,
    )
}

@Composable
private fun previewSandboxBatteryOptions(): PreviewSandboxBatteryOptions {
    val batteryModeOptions =
        listOf(
            stringResource(R.string.home_preview_battery_mode_balanced),
            stringResource(R.string.home_preview_battery_mode_power_save_compact),
            stringResource(R.string.home_preview_battery_mode_performance_compact),
            stringResource(R.string.home_preview_battery_mode_super_power_save_compact),
        )
    val chargingOptions =
        listOf(
            stringResource(R.string.home_preview_charging_none),
            stringResource(R.string.home_preview_charging_normal),
            stringResource(R.string.home_preview_charging_super_fast_compact),
        )


    return PreviewSandboxBatteryOptions(batteryModeOptions, chargingOptions)
}
