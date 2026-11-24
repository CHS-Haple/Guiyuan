package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.components.signalValueText
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.preference.SliderPreference

@Composable
internal fun SandboxMobileNetworkDropdown(
    @StringRes titleRes: Int,
    choices: List<Pair<PreviewMobileNetwork, String>>,
    options: List<String>,
    network: PreviewMobileNetwork,
    onChange: (PreviewMobileNetwork) -> Unit,
) {
    UiDropdownPreference(
        title = stringResource(titleRes),
        startAction = {
            SemanticLeadingIcon(
                iconRes = R.drawable.ic_material_symbol_signal_cellular_alt,
                detailRes = R.drawable.ic_material_symbol_category_w500,
            )
        },
        items = options,
        selectedIndex = choices.indexOfFirst { it.first == network }.coerceAtLeast(0),
        onSelectedIndexChange = { index ->
            choices.getOrNull(index)?.first?.let(onChange)
        },
        insideMargin = SandboxPreferenceInsideMargin,
        maxHeight = 360.dp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
internal fun SandboxMobileSignalSlider(
    @StringRes titleRes: Int,
    level: Int,
    enabled: Boolean,
    summary: String?,
    onChange: (Int) -> Unit,
) {
    SliderPreference(
        value = level.toFloat(),
        onValueChange = { value ->
            if (enabled) onChange(value.roundToInt().coerceIn(0, 4))
        },
        title = stringResource(titleRes),
        summary = summary,
        enabled = enabled,
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_signal_cellular_alt) },
        valueText = signalValueText(level),
        insideMargin = SandboxPreferenceInsideMargin,
        valueRange = 0f..4f,
        steps = 3,
        showKeyPoints = true,
        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        keyPoints = listOf(0f, 1f, 2f, 3f, 4f),
    )
}
