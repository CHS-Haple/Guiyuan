package com.chaners.guiyuan.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.IntrinsicSize
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
                when (state.dataMobileNetwork) {
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
    val mobileSignal =
        if (state.dualLevels != null) {
            stringResource(
                R.string.home_preview_signal_dual,
                state.mobileSignalLevel,
                state.sim2SignalLevel,
            )
        } else {
            signalValueText(state.displayedSignalLevel)
        }
    val networkSignal =
        when (center) {
            PreviewCenterSource.WIFI -> signalValueText(state.wifiSignalLevel)
            PreviewCenterSource.MOBILE -> mobileSignal
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
                SummaryDetail(
                    icon = R.drawable.ic_material_symbol_signal_cellular_alt,
                    value = stringResource(R.string.home_preview_summary_mobile_title),
                    alongside = mobileSignal,
                )
            } else {
                null
            },
            if (state.networkMode == PreviewNetworkMode.WIFI && state.airplaneMode) {
                SummaryDetail(
                    icon = R.drawable.ic_material_symbol_airplanemode_active,
                    value = stringResource(R.string.home_preview_airplane_title),
                )
            } else {
                null
            },
            if (!state.hasSim && center != PreviewCenterSource.NO_SIM) {
                SummaryDetail(
                    icon = R.drawable.ic_material_symbol_sim_card,
                    value = stringResource(R.string.home_preview_sim_absent),
                )
            } else {
                null
            },
        )
    val batteryMode =
        when (state.batteryMode) {
            PreviewBatteryMode.BALANCED -> R.string.home_preview_battery_mode_balanced
            PreviewBatteryMode.POWER_SAVE -> R.string.home_preview_battery_mode_power_save_compact
            PreviewBatteryMode.PERFORMANCE -> R.string.home_preview_battery_mode_performance_compact
            PreviewBatteryMode.SUPER_POWER_SAVE -> R.string.home_preview_battery_mode_super_power_save_compact
        }
    val charging =
        when (state.chargingState) {
            PreviewChargingState.NOT_CHARGING -> R.string.home_preview_charging_none
            PreviewChargingState.CHARGING -> R.string.home_preview_charging_normal
            PreviewChargingState.SUPER_FAST_CHARGING -> R.string.home_preview_charging_super_fast_compact
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
                    SummaryDetail(
                        icon = R.drawable.ic_material_symbol_bolt,
                        value = stringResource(charging),
                    ),
                ),
            modifier = Modifier.width(IntrinsicSize.Max),
        )
    }
}

private data class SummaryDetail(
    @DrawableRes val icon: Int,
    val value: String,
    val alongside: String? = null,
)

@Composable
private fun SummaryField(
    @DrawableRes icon: Int,
    value: String,
    alongside: String?,
    details: List<SummaryDetail>,
    modifier: Modifier,
) {
    val primary = MiuixTheme.colorScheme.onSurfaceContainer
    val companion = primary.copy(alpha = 0.76f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(primary),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = value,
                style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                color = primary,
            )
            if (alongside != null) {
                Text(
                    text = alongside,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MiuixTheme.textStyles.body2,
                    color = companion,
                )
            }
        }
        details.forEach { detail ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(detail.icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(primary),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = detail.value,
                    style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                    color = primary,
                )
                if (detail.alongside != null) {
                    Text(
                        text = detail.alongside,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MiuixTheme.textStyles.body2,
                        color = companion,
                    )
                }
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
