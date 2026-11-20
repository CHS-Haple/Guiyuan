package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.settings.palettesFor
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

internal fun LazyListScope.appearanceThemeSection(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    Section(R.string.section_theme) {
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
        appearanceColorSpec(appearance, actions)
        AnimatedPreferenceGroup(visible = appearance.colorSpec != null) {
            appearancePaletteStyle(appearance, actions)
        }
    }
}

@Composable
private fun ColumnScope.appearanceColorSpec(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    val colorSpecOptions = listOf(
        stringResource(R.string.theme_color_spec_system),
        "Spec2021",
        "Spec2025",
    )
    UiDropdownPreference(
        items = colorSpecOptions,
        selectedIndex = appearance.colorSpec?.ordinal?.plus(1) ?: 0,
        title = stringResource(R.string.theme_color_spec),
        summary = stringResource(R.string.theme_color_spec_summary),
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_category) },
        showValue = true,
        onSelectedIndexChange = { index ->
            if (index in colorSpecOptions.indices) {
                val spec = if (index == 0) null else ThemeColorSpec.entries[index - 1]
                if (spec != appearance.colorSpec) actions.onColorSpecChange(spec)
            }
        },
    )
}

@Composable
private fun ColumnScope.appearancePaletteStyle(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    val paletteStyles = appearance.colorSpec?.let(::palettesFor).orEmpty()
    val paletteNames = mapOf(
        ThemePaletteStyle.TonalSpot to stringResource(R.string.theme_palette_tonal_spot),
        ThemePaletteStyle.Neutral to stringResource(R.string.theme_palette_neutral),
        ThemePaletteStyle.Vibrant to stringResource(R.string.theme_palette_vibrant),
        ThemePaletteStyle.Expressive to stringResource(R.string.theme_palette_expressive),
        ThemePaletteStyle.Rainbow to stringResource(R.string.theme_palette_rainbow),
        ThemePaletteStyle.FruitSalad to stringResource(R.string.theme_palette_fruit_salad),
        ThemePaletteStyle.Monochrome to stringResource(R.string.theme_palette_monochrome),
        ThemePaletteStyle.Fidelity to stringResource(R.string.theme_palette_fidelity),
        ThemePaletteStyle.Content to stringResource(R.string.theme_palette_content),
        ThemePaletteStyle.Cmf to stringResource(R.string.theme_palette_cmf),
    )
    if (paletteStyles.isNotEmpty()) {
        UiDropdownPreference(
            items = paletteStyles.map { paletteNames.getValue(it) },
            selectedIndex = paletteStyles.indexOf(appearance.paletteStyle).coerceAtLeast(0),
            title = stringResource(R.string.theme_palette_style),
            summary = stringResource(R.string.theme_palette_style_summary),
            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_style) },
            showValue = true,
            onSelectedIndexChange = { index ->
                paletteStyles.getOrNull(index)?.let { style ->
                    if (style != appearance.paletteStyle) actions.onPaletteStyleChange(style)
                }
            },
        )
    }
}

