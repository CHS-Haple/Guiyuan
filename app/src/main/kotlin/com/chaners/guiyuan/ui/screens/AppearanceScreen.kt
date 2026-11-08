package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.settings.uiStyleMenuOrder
import com.chaners.guiyuan.settings.palettesFor
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
internal fun AppearanceScreen(
    appearance: Appearance,
    dark: Boolean,
    onUiStyleChange: (UiStyle) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicChange: (Boolean) -> Unit,
    onColorSpecChange: (ThemeColorSpec?) -> Unit,
    onPaletteStyleChange: (ThemePaletteStyle) -> Unit,
    onNavEnabledChange: (Boolean) -> Unit,
    onNavStyleChange: (NavStyle) -> Unit,
    onNavTouchExtensionChange: (Boolean) -> Unit,
    onNavContentChange: (NavContent) -> Unit,
    onLiquidModeChange: (LiquidMode) -> Unit,
    onUiScaleChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val themeOptions =
        listOf(
            stringResource(R.string.theme_system),
            stringResource(R.string.theme_light),
            stringResource(R.string.theme_dark),
        )
    val colorSpecOptions = listOf(
        stringResource(R.string.theme_color_spec_system),
        "Spec2021",
        "Spec2025",
    )
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
    val floatingStyleOptions =
        listOf(
            stringResource(R.string.floating_navigation_style_standard),
            stringResource(R.string.floating_navigation_style_blur),
            stringResource(
                if (appearance.uiStyle == UiStyle.OS4) R.string.floating_navigation_style_soft_glass
                else R.string.floating_navigation_style_glass,
            ),
            stringResource(R.string.floating_navigation_style_liquid),
        )
    val liquidModeOptions =
        listOf(
            stringResource(R.string.floating_navigation_liquid_mode_blur),
            stringResource(R.string.floating_navigation_liquid_mode_balanced),
            stringResource(R.string.floating_navigation_liquid_mode_clear),
        )

    var sliderPercent by remember(appearance.uiScalePercent) {
        mutableFloatStateOf(appearance.uiScalePercent.toFloat())
    }
    var inputOpen by remember { mutableStateOf(false) }

    SettingsPage(
        title = stringResource(R.string.appearance_title),
        onBack = onBack,
        overlay = {
            IntegerInputDialog(
                show = inputOpen,
                title = stringResource(R.string.ui_scale_title),
                summary = "80% - 110%",
                value = appearance.uiScalePercent,
                range = 80..110,
                suffix = "%",
                onDismiss = { inputOpen = false },
                onConfirm = { percent ->
                    inputOpen = false
                    if (percent != appearance.uiScalePercent) onUiScaleChange(percent)
                },
            )
        },
    ) {
        item {
            AppearanceThemePreview(
                appearance = appearance,
                dark = dark,
            )
        }

        Section(R.string.section_theme) {
            UiDropdownPreference(
                items = themeOptions,
                selectedIndex = appearance.theme.ordinal,
                title = stringResource(R.string.theme_mode),
                summary = stringResource(R.string.theme_mode_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_contrast,
                    )
                },
                showValue = true,
                onSelectedIndexChange = { index ->
                    ThemeMode.entries.getOrNull(index)?.let { mode ->
                        if (mode != appearance.theme) {
                            onThemeChange(mode)
                        }
                    }
                },
            )
            SwitchPreference(
                title = stringResource(R.string.dynamic_color),
                summary = stringResource(R.string.dynamic_color_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_palette,
                    )
                },
                checked = appearance.dynamicColor,
                onCheckedChange = onDynamicChange,
            )
            AnimatedPreferenceGroup(visible = appearance.dynamicColor) {
                UiDropdownPreference(
                    items = colorSpecOptions,
                    selectedIndex = appearance.colorSpec?.ordinal?.plus(1) ?: 0,
                    title = stringResource(R.string.theme_color_spec),
                    summary = stringResource(R.string.theme_color_spec_summary),
                    startAction = {
                        SemanticLeadingIcon(R.drawable.ic_material_symbol_category)
                    },
                    showValue = true,
                    onSelectedIndexChange = { index ->
                        if (index in colorSpecOptions.indices) {
                            val spec = if (index == 0) null else ThemeColorSpec.entries[index - 1]
                            if (spec != appearance.colorSpec) onColorSpecChange(spec)
                        }
                    },
                )
                AnimatedPreferenceGroup(visible = appearance.colorSpec != null) {
                    if (paletteStyles.isNotEmpty()) {
                        UiDropdownPreference(
                            items = paletteStyles.map { paletteNames.getValue(it) },
                            selectedIndex = paletteStyles.indexOf(appearance.paletteStyle).coerceAtLeast(0),
                            title = stringResource(R.string.theme_palette_style),
                            summary = stringResource(R.string.theme_palette_style_summary),
                            startAction = {
                                SemanticLeadingIcon(R.drawable.ic_material_symbol_style)
                            },
                            showValue = true,
                            onSelectedIndexChange = { index ->
                                paletteStyles.getOrNull(index)?.let { style ->
                                    if (style != appearance.paletteStyle) onPaletteStyleChange(style)
                                }
                            },
                        )
                    }
                }
            }
        }

        Section(R.string.section_visual_effects) {
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
                        if (style != appearance.uiStyle) onUiStyleChange(style)
                    }
                },
            )
            SliderPreference(
                value = sliderPercent,
                onValueChange = { sliderPercent = it.roundToInt().toFloat() },
                onValueChangeFinished = {
                    val percent = sliderPercent.roundToInt().coerceIn(80, 110)
                    if (percent != appearance.uiScalePercent) onUiScaleChange(percent)
                },
                title = stringResource(R.string.ui_scale_title),
                summary = stringResource(R.string.ui_scale_summary),
                startAction = {
                    SemanticLeadingIcon(R.drawable.ic_material_symbol_display_settings)
                },
                valueText = stringResource(R.string.percent_value, sliderPercent.roundToInt()),
                valueRange = 80f..110f,
                steps = 29,
                showKeyPoints = true,
                keyPoints = listOf(80f, 90f, 100f, 110f),
                magnetThreshold = 0.01f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                holdDownState = inputOpen,
                onClick = { inputOpen = true },
            )
            SwitchPreference(
                title = stringResource(R.string.floating_navigation_bar),
                summary = stringResource(R.string.floating_navigation_bar_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_bottom_navigation,
                    )
                },
                checked = appearance.navEnabled,
                onCheckedChange = onNavEnabledChange,
            )
            AnimatedPreferenceGroup(visible = appearance.navEnabled) {
                UiDropdownPreference(
                    items = floatingStyleOptions,
                    selectedIndex = appearance.navStyle.ordinal,
                    title = stringResource(R.string.floating_navigation_style),
                    summary = stringResource(R.string.floating_navigation_style_summary),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_style,
                        )
                    },
                    showValue = true,
                    onSelectedIndexChange = { index ->
                        NavStyle.entries.getOrNull(index)?.let { style ->
                            if (style != appearance.navStyle) {
                                onNavStyleChange(style)
                            }
                        }
                    },
                )
                AnimatedPreferenceGroup(visible = appearance.navStyle == NavStyle.Liquid) {
                    UiDropdownPreference(
                        items = liquidModeOptions,
                        selectedIndex = appearance.liquidMode.ordinal,
                        title = stringResource(R.string.floating_navigation_liquid_mode),
                        summary = stringResource(R.string.floating_navigation_liquid_mode_summary),
                        startAction = {
                            SemanticLeadingIcon(
                                iconRes = R.drawable.ic_material_symbol_blur_circular,
                            )
                        },
                        showValue = true,
                        onSelectedIndexChange = { index ->
                            LiquidMode.entries.getOrNull(index)?.let { mode ->
                                if (mode != appearance.liquidMode) {
                                    onLiquidModeChange(mode)
                                }
                            }
                        },
                    )
                }
                SwitchPreference(
                    title = stringResource(R.string.floating_navigation_content),
                    summary = stringResource(R.string.floating_navigation_content_summary),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_text_fields,
                        )
                    },
                    checked = appearance.activeNavContent == NavContent.IconAndText,
                    onCheckedChange = { showLabels ->
                        val content = if (showLabels) NavContent.IconAndText else NavContent.IconOnly
                        if (content != appearance.activeNavContent) onNavContentChange(content)
                    },
                )
                SwitchPreference(
                    title = stringResource(R.string.floating_navigation_touch_extension),
                    summary = stringResource(R.string.floating_navigation_touch_extension_summary),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_fit_screen,
                        )
                    },
                    checked = appearance.navTouchExtension,
                    onCheckedChange = onNavTouchExtensionChange,
                )
            }
        }
    }
}
