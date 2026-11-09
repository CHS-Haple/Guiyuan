package com.chaners.guiyuan.settings

import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

private val modernPalettes = listOf(
    ThemePaletteStyle.TonalSpot,
    ThemePaletteStyle.Neutral,
    ThemePaletteStyle.Vibrant,
    ThemePaletteStyle.Expressive,
)

internal fun palettesFor(spec: ThemeColorSpec): List<ThemePaletteStyle> = when (spec) {
    ThemeColorSpec.Spec2021 -> modernPalettes + listOf(
        ThemePaletteStyle.Rainbow,
        ThemePaletteStyle.FruitSalad,
        ThemePaletteStyle.Monochrome,
        ThemePaletteStyle.Fidelity,
        ThemePaletteStyle.Content,
    )
    // CMF always uses its own 2026 scheme in newer MIUIX.
    ThemeColorSpec.Spec2025 -> modernPalettes + ThemePaletteStyle.Cmf
}

internal fun decodeColorSpec(value: String?): ThemeColorSpec? =
    if (value == "Spec2026") ThemeColorSpec.Spec2025
    else ThemeColorSpec.entries.firstOrNull { it.name == value }

internal fun decodePalette(value: String?): ThemePaletteStyle =
    ThemePaletteStyle.entries.firstOrNull { it.name == value } ?: ThemePaletteStyle.TonalSpot

internal fun compatiblePalette(spec: ThemeColorSpec?, palette: ThemePaletteStyle): ThemePaletteStyle =
    if (spec != null && palette !in palettesFor(spec)) ThemePaletteStyle.TonalSpot else palette
