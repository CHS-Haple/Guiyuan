package com.chaners.guiyuan.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.screens.PreviewBatteryMode
import com.chaners.guiyuan.ui.screens.PreviewCenterSource
import com.chaners.guiyuan.ui.screens.PreviewChargingState
import com.chaners.guiyuan.ui.screens.PreviewMobileNetwork
import com.chaners.guiyuan.ui.screens.PreviewNetworkMode
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import com.chaners.guiyuan.ui.screens.PreviewWifiState
import com.chaners.guiyuan.ui.screens.previewCenterSource
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PreviewSummary(
    state: PreviewSandboxUiState,
    modifier: Modifier = Modifier,
) {
    val center = state.previewCenterSource()
    val network =
        when (center) {
            PreviewCenterSource.WIFI ->
                when (state.wifiState) {
                    PreviewWifiState.CONNECTED -> stringResource(R.string.home_preview_network_mode_wifi)
                    PreviewWifiState.NO_INTERNET -> stringResource(R.string.home_preview_wifi_no_internet)
                    PreviewWifiState.HOTSPOT -> stringResource(R.string.home_preview_wifi_hotspot)
                }
            PreviewCenterSource.AIRPLANE -> stringResource(R.string.home_preview_airplane_title)
            PreviewCenterSource.NO_SIM -> stringResource(R.string.home_preview_sim_absent)
            PreviewCenterSource.EMPTY -> stringResource(R.string.home_preview_network_none)
            PreviewCenterSource.MOBILE ->
                when (state.mobileNetwork) {
                    PreviewMobileNetwork.NONE -> stringResource(R.string.home_preview_network_none)
                    PreviewMobileNetwork.TWO_G -> stringResource(R.string.home_preview_network_2g)
                    PreviewMobileNetwork.EDGE -> stringResource(R.string.home_preview_network_edge)
                    PreviewMobileNetwork.THREE_G -> stringResource(R.string.home_preview_network_3g)
                    PreviewMobileNetwork.H_PLUS -> stringResource(R.string.home_preview_network_h_plus)
                    PreviewMobileNetwork.FOUR_G -> stringResource(R.string.home_preview_network_4g)
                    PreviewMobileNetwork.LTE -> stringResource(R.string.home_preview_network_lte)
                    PreviewMobileNetwork.FIVE_G -> stringResource(R.string.home_preview_network_5g)
                    PreviewMobileNetwork.FIVE_GA -> stringResource(R.string.home_preview_network_5ga)
                }
        }
    val networkSignal =
        when (center) {
            PreviewCenterSource.WIFI -> signalValueText(state.wifiSignalLevel)
            PreviewCenterSource.MOBILE -> signalValueText(state.mobileSignalLevel)
            else -> null
        }
    val networkIcon =
        when (center) {
            PreviewCenterSource.WIFI -> R.drawable.ic_material_symbol_wifi
            PreviewCenterSource.MOBILE,
            PreviewCenterSource.EMPTY -> R.drawable.ic_material_symbol_signal_cellular_alt
            PreviewCenterSource.AIRPLANE -> R.drawable.ic_material_symbol_airplanemode_active
            PreviewCenterSource.NO_SIM -> R.drawable.ic_material_symbol_sim_card
        }
    val networkDetails =
        listOfNotNull(
            if (state.mobileControlsEnabled && center != PreviewCenterSource.MOBILE) {
                R.drawable.ic_material_symbol_signal_cellular_alt to
                    stringResource(
                        R.string.home_preview_summary_mobile_signal,
                        signalValueText(state.mobileSignalLevel),
                    )
            } else {
                null
            },
            if (state.networkMode == PreviewNetworkMode.WIFI && state.airplaneMode) {
                R.drawable.ic_material_symbol_airplanemode_active to
                    stringResource(R.string.home_preview_airplane_title)
            } else {
                null
            },
            if (!state.simPresent && center != PreviewCenterSource.NO_SIM) {
                R.drawable.ic_material_symbol_sim_card to
                    stringResource(R.string.home_preview_sim_absent)
            } else {
                null
            },
        )
    val batteryMode =
        when (state.batteryMode) {
            PreviewBatteryMode.BALANCED -> R.string.home_preview_battery_mode_balanced
            PreviewBatteryMode.POWER_SAVE -> R.string.home_preview_battery_mode_power_save
            PreviewBatteryMode.PERFORMANCE -> R.string.home_preview_battery_mode_performance
            PreviewBatteryMode.SUPER_POWER_SAVE -> R.string.home_preview_battery_mode_super_power_save
        }
    val charging =
        when (state.chargingState) {
            PreviewChargingState.NOT_CHARGING -> R.string.home_preview_charging_none
            PreviewChargingState.CHARGING -> R.string.home_preview_charging_normal
            PreviewChargingState.SUPER_FAST_CHARGING -> R.string.home_preview_charging_super_fast
        }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryField(
            icon = networkIcon,
            value = network,
            alongside = networkSignal,
            details = networkDetails,
            modifier = Modifier.weight(1f),
        )
        SummaryField(
            icon = R.drawable.ic_material_symbol_battery_5_bar,
            value = stringResource(R.string.home_preview_battery_percent, state.batteryPercent),
            alongside = stringResource(batteryMode),
            details =
                listOf(
                    R.drawable.ic_material_symbol_bolt to stringResource(charging),
                ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryField(
    @DrawableRes icon: Int,
    value: String,
    alongside: String?,
    details: List<Pair<Int, String>>,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceContainer),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = value,
                style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            if (alongside != null) {
                Text(
                    text = alongside,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
        }
        details.forEach { (detailIcon, detail) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(detailIcon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceContainerVariant),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = detail,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
        }
    }
}

@Composable
internal fun signalValueText(level: Int): String =
    if (level <= 0) {
        stringResource(R.string.home_preview_signal_none)
    } else {
        stringResource(R.string.home_preview_signal_level, level)
    }
