package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.NETWORK_TOP_OFFSET_MAX
import com.chaners.guiyuan.settings.NETWORK_TOP_OFFSET_MIN
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.preference.SliderPreference

@Composable
internal fun IndicatorVerticalOffsetSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    enabled: Boolean,
) {
    SliderPreference(
        value = value,
        onValueChange = onValueChange,
        title = stringResource(titleRes),
        startAction = {
            SemanticLeadingIcon(iconRes, enabled, R.drawable.ic_material_symbol_height_w500)
        },
        valueText = stringResource(
            R.string.battery_top_offset_value,
            value.roundToInt(),
        ),
        valueRange = NETWORK_TOP_OFFSET_MIN..NETWORK_TOP_OFFSET_MAX,
        steps = 19,
        showKeyPoints = true,
        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        keyPoints = listOf(0f),
        magnetThreshold = 0.035f,
        enabled = enabled,
    )
}
