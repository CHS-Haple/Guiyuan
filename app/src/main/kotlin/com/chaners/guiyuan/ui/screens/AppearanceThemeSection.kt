package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.settings.supportsSpec2025
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
internal fun ColumnScope.appearanceThemeSection(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    PreferenceSection(R.string.section_theme) {
        appearanceThemeMode(appearance, actions)
        appearanceDynamicColor(appearance, actions)
    }
}

@Composable
private fun ColumnScope.appearanceThemeMode(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    val themeOptions = listOf(
        stringResource(R.string.theme_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark),
    )
    UiDropdownPreference(
        items = themeOptions,
        selectedIndex = appearance.theme.ordinal,
        title = stringResource(R.string.theme_mode),
        summary = stringResource(R.string.theme_mode_summary),
        startAction = { SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_contrast) },
        showValue = true,
        onSelectedIndexChange = { index ->
            ThemeMode.entries.getOrNull(index)?.let { mode ->
                if (mode != appearance.theme) actions.onThemeChange(mode)
            }
        },
    )
}

@Composable
private fun ColumnScope.appearanceDynamicColor(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    SwitchPreference(
        title = stringResource(R.string.dynamic_color),
        summary = stringResource(R.string.dynamic_color_summary),
        startAction = { SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_palette) },
        checked = appearance.dynamicColor,
        onCheckedChange = actions.onDynamicChange,
    )
    AnimatedPreferenceGroup(visible = appearance.dynamicColor) {
        appearancePaletteSource(appearance, actions)
        AnimatedPreferenceGroup(visible = appearance.customPalette) {
            appearancePaletteStyle(appearance, actions)
            appearanceColorSpec(appearance, actions)
        }
    }
}

@Composable
private fun ColumnScope.appearancePaletteSource(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    UiDropdownPreference(
        items = listOf(
            stringResource(R.string.theme_palette_source_system),
            stringResource(R.string.theme_palette_source_custom),
        ),
        selectedIndex = if (appearance.customPalette) 1 else 0,
        title = stringResource(R.string.theme_palette_source),
        summary = stringResource(R.string.theme_palette_source_summary),
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_colorize) },
        onSelectedIndexChange = { index ->
            if (index in 0..1 && (index == 1) != appearance.customPalette) {
                actions.onCustomPaletteChange(index == 1)
            }
        },
    )
}

@Composable
private fun ColumnScope.appearanceColorSpec(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    val enabled = appearance.paletteStyle.supportsSpec2025()
    val options = when {
        appearance.paletteStyle == ThemePaletteStyle.Cmf -> listOf("Spec2026")
        !enabled -> listOf(ThemeColorSpec.Spec2021.name)
        else -> ThemeColorSpec.entries.map { it.name }
    }
    UiDropdownPreference(
        items = options,
        selectedIndex = if (enabled) appearance.colorSpec.ordinal else 0,
        title = stringResource(R.string.theme_color_spec),
        summary = stringResource(R.string.theme_color_spec_summary),
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_category) },
        enabled = enabled,
        onSelectedIndexChange = { index ->
            if (enabled) {
                ThemeColorSpec.entries.getOrNull(index)?.let { spec ->
                    if (spec != appearance.colorSpec) actions.onColorSpecChange(spec)
                }
            }
        },
    )
}

@Composable
private fun ColumnScope.appearancePaletteStyle(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    val paletteStyles = ThemePaletteStyle.entries
    val paletteNames = paletteStyles.map { style ->
        stringResource(
            when (style) {
                ThemePaletteStyle.TonalSpot -> R.string.theme_palette_tonal_spot
                ThemePaletteStyle.Neutral -> R.string.theme_palette_neutral
                ThemePaletteStyle.Vibrant -> R.string.theme_palette_vibrant
                ThemePaletteStyle.Expressive -> R.string.theme_palette_expressive
                ThemePaletteStyle.Rainbow -> R.string.theme_palette_rainbow
                ThemePaletteStyle.FruitSalad -> R.string.theme_palette_fruit_salad
                ThemePaletteStyle.Monochrome -> R.string.theme_palette_monochrome
                ThemePaletteStyle.Fidelity -> R.string.theme_palette_fidelity
                ThemePaletteStyle.Content -> R.string.theme_palette_content
                ThemePaletteStyle.Cmf -> R.string.theme_palette_cmf
            },
        )
    }
    UiDropdownPreference(
        items = paletteNames,
        selectedIndex = appearance.paletteStyle.ordinal,
        title = stringResource(R.string.theme_palette_style),
        summary = stringResource(R.string.theme_palette_style_summary),
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_style) },
        onSelectedIndexChange = { index ->
            paletteStyles.getOrNull(index)?.let { style ->
                if (style != appearance.paletteStyle) actions.onPaletteStyleChange(style)
            }
        },
    )
}
