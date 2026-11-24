package com.chaners.guiyuan.settings

import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

// Mirrors the pinned MIUIX selector contract; ThemeController owns actual generation.
internal fun ThemePaletteStyle.supportsSpec2025(): Boolean = when (this) {
    ThemePaletteStyle.TonalSpot,
    ThemePaletteStyle.Neutral,
    ThemePaletteStyle.Vibrant,
    ThemePaletteStyle.Expressive,
    -> true

    else -> false
}

// Older versions selected the system palette by omitting the specification key.
internal fun decodeCustomPalette(stored: Boolean?, legacySpec: ThemeColorSpec?): Boolean =
    stored ?: (legacySpec != null)

internal fun decodeColorSpec(value: String?): ThemeColorSpec? =
    if (value == "Spec2026") ThemeColorSpec.Spec2025
    else ThemeColorSpec.entries.firstOrNull { it.name == value }

internal fun decodePalette(value: String?): ThemePaletteStyle =
    ThemePaletteStyle.entries.firstOrNull { it.name == value } ?: ThemePaletteStyle.TonalSpot
