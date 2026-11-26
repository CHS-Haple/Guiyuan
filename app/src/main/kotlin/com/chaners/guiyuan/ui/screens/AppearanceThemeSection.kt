package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.PaletteSource
import com.chaners.guiyuan.settings.PaletteSeedPreset
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.settings.supportsSpec2025
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.basic.SmallTitle
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
        AnimatedPreferenceGroup(visible = appearance.paletteSource == PaletteSource.PRESET) {
            appearancePalettePresets(appearance, actions)
        }
        AnimatedPreferenceGroup(visible = appearance.customPalette) {
            appearancePaletteStyle(appearance, actions)
            // Unsupported styles have a fixed effective spec; the selected spec is retained.
            AnimatedPreferenceGroup(visible = appearance.paletteStyle.supportsSpec2025()) {
                appearanceColorSpec(appearance, actions)
            }
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
            stringResource(R.string.theme_palette_source_preset),
        ),
        selectedIndex = appearance.paletteSource.ordinal,
        title = stringResource(R.string.theme_palette_source),
        summary = stringResource(R.string.theme_palette_source_summary),
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_colorize) },
        onSelectedIndexChange = { index ->
            PaletteSource.entries.getOrNull(index)?.let { source ->
                if (source != appearance.paletteSource) actions.onPaletteSourceChange(source)
            }
        },
    )
}

@Composable
private fun ColumnScope.appearancePalettePresets(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    SmallTitle(text = stringResource(R.string.theme_palette_preset_title))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        PaletteSeedPreset.entries.forEach { preset ->
            BatteryCommonColorButton(
                color = preset.argb,
                selected = appearance.paletteSeedPreset == preset,
                onClick = {
                    if (preset != appearance.paletteSeedPreset) {
                        actions.onPaletteSeedPresetChange(preset)
                    }
                },
            )
        }
    }
}

@Composable
private fun ColumnScope.appearanceColorSpec(
    appearance: Appearance,
    actions: AppearanceThemeActions,
) {
    val enabled = appearance.paletteStyle.supportsSpec2025()
    val options = when {
        appearance.paletteStyle == ThemePaletteStyle.Cmf -> listOf(stringResource(R.string.theme_spec_2026))
        !enabled -> listOf(stringResource(R.string.theme_spec_2021))
        else -> ThemeColorSpec.entries.map { spec ->
            stringResource(if (spec == ThemeColorSpec.Spec2021) R.string.theme_spec_2021 else R.string.theme_spec_2025)
        }
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
    val description = when (appearance.paletteStyle) {
        ThemePaletteStyle.TonalSpot -> R.string.theme_palette_tonal_spot_description
        ThemePaletteStyle.Neutral -> R.string.theme_palette_neutral_description
        ThemePaletteStyle.Vibrant -> R.string.theme_palette_vibrant_description
        ThemePaletteStyle.Expressive -> R.string.theme_palette_expressive_description
        ThemePaletteStyle.Rainbow -> R.string.theme_palette_rainbow_description
        ThemePaletteStyle.FruitSalad -> R.string.theme_palette_fruit_salad_description
        ThemePaletteStyle.Monochrome -> R.string.theme_palette_monochrome_description
        ThemePaletteStyle.Fidelity -> R.string.theme_palette_fidelity_description
        ThemePaletteStyle.Content -> R.string.theme_palette_content_description
        ThemePaletteStyle.Cmf -> R.string.theme_palette_cmf_description
    }
    UiDropdownPreference(
        items = paletteNames,
        selectedIndex = appearance.paletteStyle.ordinal,
        title = stringResource(R.string.theme_palette_style),
        summary = stringResource(description),
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_style) },
        onSelectedIndexChange = { index ->
            paletteStyles.getOrNull(index)?.let { style ->
                if (style != appearance.paletteStyle) actions.onPaletteStyleChange(style)
            }
        },
    )
}
