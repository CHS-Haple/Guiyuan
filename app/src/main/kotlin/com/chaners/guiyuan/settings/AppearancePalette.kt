package com.chaners.guiyuan.settings

import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

// Presets only choose the seed; pinned MIUIX ThemeController still generates every role.
internal enum class PaletteSource { SYSTEM, SYSTEM_SEED, PRESET }

internal enum class PaletteSeedPreset(val argb: Int) {
    BLUE(0xFF007AFF.toInt()),
    CYAN(0xFF32ADE6.toInt()),
    GREEN(0xFF34C759.toInt()),
    PURPLE(0xFFAF52DE.toInt()),
    ORANGE(0xFFFF9500.toInt()),
    PINK(0xFFFF2D55.toInt()),
}

internal fun paletteSource(customPalette: Boolean, usePresetSeed: Boolean): PaletteSource =
    when {
        !customPalette -> PaletteSource.SYSTEM
        usePresetSeed -> PaletteSource.PRESET
        else -> PaletteSource.SYSTEM_SEED
    }

internal fun decodePaletteSeedPreset(stored: String?): PaletteSeedPreset =
    PaletteSeedPreset.entries.firstOrNull { it.name == stored } ?: PaletteSeedPreset.BLUE

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
