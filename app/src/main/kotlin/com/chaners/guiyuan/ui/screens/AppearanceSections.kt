package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.settings.uiStyleMenuOrder
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

internal fun LazyListScope.appearanceVisualSection(
    appearance: Appearance,
    actions: AppearanceVisualActions,
    scale: AppearanceScaleUiState,
) {
    Section(R.string.section_visual_effects) {
        appearanceStyle(appearance, actions.style)
        appearanceScale(appearance, actions.style, scale)
        appearanceNavigation(appearance, actions.navigation)
    }
}

@Composable
private fun ColumnScope.appearanceStyle(
    appearance: Appearance,
    actions: AppearanceStyleActions,
) {
    UiDropdownPreference(
        items = listOf(
            stringResource(R.string.ui_style_os4),
            stringResource(R.string.ui_style_os3),
        ),
        selectedIndex = uiStyleMenuOrder.indexOf(appearance.uiStyle),
        title = stringResource(R.string.ui_style_title),
        summary = stringResource(R.string.ui_style_summary),
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_layers) },
        showValue = true,
        onSelectedIndexChange = { index ->
            uiStyleMenuOrder.getOrNull(index)?.let { style ->
                if (style != appearance.uiStyle) actions.onUiStyleChange(style)
            }
        },
    )
    SwitchPreference(
        title = stringResource(R.string.top_bar_progressive_blur),
        summary = stringResource(R.string.top_bar_progressive_blur_summary),
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_gradient) },
        checked = appearance.topBarProgressiveBlur,
        onCheckedChange = actions.onTopBarProgressiveBlurChange,
    )
}

@Composable
private fun ColumnScope.appearanceScale(
    appearance: Appearance,
    actions: AppearanceStyleActions,
    state: AppearanceScaleUiState,
) {
    SliderPreference(
        value = state.slider.floatValue,
        onValueChange = { state.slider.floatValue = it.roundToInt().toFloat() },
        onValueChangeFinished = {
            val percent = state.slider.floatValue.roundToInt().coerceIn(80, 110)
            if (percent != appearance.uiScalePercent) actions.onUiScaleChange(percent)
        },
        title = stringResource(R.string.ui_scale_title),
        summary = stringResource(R.string.ui_scale_summary),
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_display_settings) },
        valueText = stringResource(R.string.percent_value, state.slider.floatValue.roundToInt()),
        valueRange = 80f..110f,
        steps = 29,
        showKeyPoints = true,
        keyPoints = listOf(80f, 90f, 100f, 110f),
        magnetThreshold = 0.01f,
        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        holdDownState = state.dialogOpen.value,
        onClick = { state.dialogOpen.value = true },
    )
}

@Composable
private fun ColumnScope.appearanceNavigation(
    appearance: Appearance,
    actions: AppearanceNavigationActions,
) {
    SwitchPreference(
        title = stringResource(R.string.floating_navigation_bar),
        summary = stringResource(R.string.floating_navigation_bar_summary),
        startAction = { SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_bottom_navigation) },
        checked = appearance.navEnabled,
        onCheckedChange = actions.onNavEnabledChange,
    )
    AnimatedPreferenceGroup(visible = appearance.navEnabled) {
        appearanceNavigationStyle(appearance, actions)
        AnimatedPreferenceGroup(visible = appearance.navStyle == NavStyle.Liquid) {
            appearanceLiquidMode(appearance, actions)
        }
        appearanceNavigationOptions(appearance, actions)
    }
}

@Composable
private fun ColumnScope.appearanceNavigationStyle(
    appearance: Appearance,
    actions: AppearanceNavigationActions,
) {
    val floatingStyleOptions = listOf(
        stringResource(R.string.floating_navigation_style_standard),
        stringResource(R.string.floating_navigation_style_blur),
        stringResource(
            if (appearance.uiStyle == UiStyle.OS4) R.string.floating_navigation_style_soft_glass
            else R.string.floating_navigation_style_glass,
        ),
        stringResource(R.string.floating_navigation_style_liquid),
    )
    UiDropdownPreference(
        items = floatingStyleOptions,
        selectedIndex = appearance.navStyle.ordinal,
        title = stringResource(R.string.floating_navigation_style),
        summary = stringResource(R.string.floating_navigation_style_summary),
        startAction = { SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_style) },
        showValue = true,
        onSelectedIndexChange = { index ->
            NavStyle.entries.getOrNull(index)?.let { style ->
                if (style != appearance.navStyle) actions.onNavStyleChange(style)
            }
        },
    )
}

@Composable
private fun ColumnScope.appearanceLiquidMode(
    appearance: Appearance,
    actions: AppearanceNavigationActions,
) {
    val liquidModeOptions = listOf(
        stringResource(R.string.floating_navigation_liquid_mode_blur),
        stringResource(R.string.floating_navigation_liquid_mode_balanced),
        stringResource(R.string.floating_navigation_liquid_mode_clear),
    )
    UiDropdownPreference(
        items = liquidModeOptions,
        selectedIndex = appearance.liquidMode.ordinal,
        title = stringResource(R.string.floating_navigation_liquid_mode),
        summary = stringResource(R.string.floating_navigation_liquid_mode_summary),
        startAction = { SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_blur_circular) },
        showValue = true,
        onSelectedIndexChange = { index ->
            LiquidMode.entries.getOrNull(index)?.let { mode ->
                if (mode != appearance.liquidMode) actions.onLiquidModeChange(mode)
            }
        },
    )
}

@Composable
private fun ColumnScope.appearanceNavigationOptions(
    appearance: Appearance,
    actions: AppearanceNavigationActions,
) {
    SwitchPreference(
        title = stringResource(R.string.floating_navigation_content),
        summary = stringResource(R.string.floating_navigation_content_summary),
        startAction = { SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_text_fields) },
        checked = appearance.activeNavContent == NavContent.IconAndText,
        onCheckedChange = { showLabels ->
            val content = if (showLabels) NavContent.IconAndText else NavContent.IconOnly
            if (content != appearance.activeNavContent) actions.onNavContentChange(content)
        },
    )
    SwitchPreference(
        title = stringResource(R.string.floating_navigation_touch_extension),
        summary = stringResource(R.string.floating_navigation_touch_extension_summary),
        startAction = { SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_fit_screen) },
        checked = appearance.navTouchExtension,
        onCheckedChange = actions.onNavTouchExtensionChange,
    )
}
