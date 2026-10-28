package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.settings.uiStyleMenuOrder
import com.chaners.guiyuan.settings.palettesFor
import com.chaners.guiyuan.ui.components.NavContentItem
import com.chaners.guiyuan.ui.components.floatingNavMaterial
import com.chaners.guiyuan.ui.components.liquid.LiquidNavBar
import com.chaners.guiyuan.ui.components.liquid.LiquidNavEntry
import com.chaners.guiyuan.ui.components.liquid.LiquidNavSpec
import com.chaners.guiyuan.ui.components.liquid.liquidNavBackdropSource
import com.chaners.guiyuan.ui.components.liquid.liquidNavBottomPadding
import com.chaners.guiyuan.ui.components.liquid.liquidNavSupported
import com.chaners.guiyuan.ui.components.liquid.rememberLiquidNavBackdrop
import com.chaners.guiyuan.ui.components.requiresTextureBackdrop
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
    val floatingContentOptions =
        listOf(
            stringResource(R.string.floating_navigation_content_icon_only),
            stringResource(R.string.floating_navigation_content_icon_and_text),
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
                AnimatedPreferenceGroup(
                    visible = appearance.navStyle == NavStyle.Liquid ||
                        (appearance.uiStyle == UiStyle.OS4 && appearance.navStyle == NavStyle.Glass),
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.floating_navigation_touch_extension),
                        summary = stringResource(R.string.floating_navigation_touch_extension_summary),
                        startAction = {
                            SemanticLeadingIcon(R.drawable.ic_material_symbol_target)
                        },
                        checked = appearance.navTouchExtension,
                        onCheckedChange = onNavTouchExtensionChange,
                    )
                }
                UiDropdownPreference(
                    items = floatingContentOptions,
                    selectedIndex = appearance.activeNavContent.ordinal,
                    title = stringResource(R.string.floating_navigation_content),
                    summary = stringResource(R.string.floating_navigation_content_summary),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_format_list_bulleted,
                        )
                    },
                    showValue = true,
                    onSelectedIndexChange = { index ->
                        NavContent.entries.getOrNull(index)?.let { content ->
                            if (content != appearance.activeNavContent) {
                                onNavContentChange(content)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AppearanceThemePreview(
    appearance: Appearance,
    dark: Boolean,
) {
    Card(
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.theme_preview),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            Text(
                text = stringResource(R.string.theme_preview_summary),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )

            AppearanceMiniPreview(
                appearance = appearance,
                dark = dark,
            )
        }
    }
}

@Composable
private fun AppearanceMiniPreview(
    appearance: Appearance,
    dark: Boolean,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = MiuixTheme.colorScheme.surface,
        border =
            BorderStroke(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.18f),
            ),
    ) {
        ScaledPreviewContent(
            scale = MiniPreviewScale,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 13.dp, end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniPreviewHeader()
                MiniSwitchSettingPreview()
                MiniSliderSettingPreview()
                MiniNavigationPreview(
                    uiStyle = appearance.uiStyle,
                    floating = appearance.navEnabled,
                    style = appearance.navStyle,
                    content = appearance.activeNavContent,
                    liquidMode = appearance.liquidMode,
                    dark = dark,
                )
            }
        }
    }
}

@Composable
private fun MiniPreviewHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiniTextBar(width = 88.dp, role = MiniTextRole.Title)
            MiniTextBar(width = 54.dp, role = MiniTextRole.Body2)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            MiniThemeSwatch(MiuixTheme.colorScheme.primary)
            MiniThemeSwatch(MiuixTheme.colorScheme.secondary)
            MiniThemeSwatch(MiuixTheme.colorScheme.surfaceContainerHigh)
        }
    }
}

@Composable
private fun MiniSwitchSettingPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MiniTextBar(width = 82.dp, role = MiniTextRole.Body1)
                MiniTextBar(width = 56.dp, role = MiniTextRole.Body2)
            }
            Switch(
                checked = true,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun MiniSliderSettingPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(
                    modifier = Modifier.size(14.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = 0.20f),
                ) {}
                MiniTextBar(width = 72.dp, role = MiniTextRole.Body1)
            }
            Slider(
                value = 0.43f,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors =
                    SliderDefaults.sliderColors(
                        disabledForegroundColor = MiuixTheme.colorScheme.primary,
                        disabledBackgroundColor = MiuixTheme.colorScheme.sliderBackground,
                        disabledThumbColor = MiuixTheme.colorScheme.onPrimary,
                    ),
            )
        }
    }
}

private enum class MiniTextRole {
    Title,
    Body1,
    Body2,
}

@Composable
private fun MiniTextBar(
    width: androidx.compose.ui.unit.Dp,
    role: MiniTextRole,
    modifier: Modifier = Modifier,
) {
    val fontSize =
        when (role) {
            MiniTextRole.Title -> MiuixTheme.textStyles.title3.fontSize
            MiniTextRole.Body1 -> MiuixTheme.textStyles.body1.fontSize
            MiniTextRole.Body2 -> MiuixTheme.textStyles.body2.fontSize
        }
    val height = (fontSize.value * 0.35f).dp
    val alpha =
        when (role) {
            MiniTextRole.Title -> 0.34f
            MiniTextRole.Body1 -> 0.27f
            MiniTextRole.Body2 -> 0.17f
        }

    Surface(
        modifier =
            modifier
                .width(width)
                .height(height),
        shape = RoundedCornerShape(height / 2f),
        color = MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = alpha),
    ) {}
}

@Composable
private fun MiniThemeSwatch(color: Color) {
    Surface(
        modifier = Modifier.size(14.dp),
        shape = CircleShape,
        color = color,
        border =
            BorderStroke(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.26f),
            ),
    ) {}
}

private const val MiniPreviewScale = 0.82f
private val MiniNavigationViewportHeight = 76.dp

@Composable
private fun ScaledPreviewContent(
    scale: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        val maxWidth =
            if (constraints.hasBoundedWidth) {
                (constraints.maxWidth / scale).roundToInt()
            } else {
                constraints.maxWidth
            }
        val placeable =
            measurables.single().measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = maxWidth,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            )
        val scaledHeight = (placeable.height * scale).roundToInt()
        val layoutHeight =
            scaledHeight.coerceIn(
                constraints.minHeight,
                if (constraints.hasBoundedHeight) constraints.maxHeight else scaledHeight,
            )

        layout(constraints.maxWidth, layoutHeight) {
            placeable.placeRelativeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

@Composable
private fun MiniNavigationPreview(
    uiStyle: UiStyle,
    floating: Boolean,
    style: NavStyle,
    content: NavContent,
    liquidMode: LiquidMode,
    dark: Boolean,
) {
    val os4Glass = uiStyle == UiStyle.OS4 && floating && style == NavStyle.Glass
    val miuixMaterialActive =
        floating && style.requiresTextureBackdrop && isRuntimeShaderSupported()
    val liquidMaterialActive =
        floating && style == NavStyle.Liquid &&
            liquidNavSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val miuixBackdrop =
        if (miuixMaterialActive) {
            rememberLayerBackdrop {
                drawRect(surfaceColor)
                drawContent()
            }
        } else {
            null
        }
    val liquidBackdrop =
        if (liquidMaterialActive) {
            rememberLiquidNavBackdrop(surfaceColor)
        } else {
            null
        }
    val liquidBottomSpace =
        if (liquidBackdrop != null) {
            liquidNavBottomPadding()
        } else {
            0.dp
        }
    val floatingModifier =
        if (miuixBackdrop != null && !os4Glass) {
            Modifier.floatingNavMaterial(
                backdrop = miuixBackdrop,
                dark = dark,
                style = style,
            )
        } else {
            Modifier
        }

    val standardNav = !floating || (os4Glass && miuixBackdrop == null)

    if (os4Glass && miuixBackdrop != null) {
        // The glass bar can grow for captions. Let the preview measure its actual
        // height and keep its shadow inside the preview surface.
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(miuixBackdrop),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MiuixTheme.colorScheme.surface,
                ) {}
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    MiniTextBar(width = 92.dp, role = MiniTextRole.Body1)
                    MiniTextBar(width = 58.dp, role = MiniTextRole.Body2)
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(20.dp))
                GlassNavigationBar(
                    items = listOf(
                        GlassNavigationItem(MiuixIcons.Normal.Home, stringResource(R.string.nav_home).takeIf { content == NavContent.IconAndText }),
                        GlassNavigationItem(MiuixIcons.Normal.Tune, stringResource(R.string.nav_features).takeIf { content == NavContent.IconAndText }),
                        GlassNavigationItem(MiuixIcons.Medium.Settings, stringResource(R.string.nav_settings).takeIf { content == NavContent.IconAndText }),
                    ),
                    selectedIndex = 2,
                    onSelect = {},
                    backdrop = miuixBackdrop,
                    interactive = false,
                    defaultWindowInsetsPadding = false,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                )
            }
        }
        return
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(MiniNavigationViewportHeight + liquidBottomSpace),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (miuixBackdrop != null) {
                            Modifier.layerBackdrop(miuixBackdrop)
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (liquidBackdrop != null) {
                            Modifier.liquidNavBackdropSource(liquidBackdrop)
                        } else {
                            Modifier
                        },
                    ),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MiuixTheme.colorScheme.surface,
            ) {}
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                MiniTextBar(width = 92.dp, role = MiniTextRole.Body1)
                MiniTextBar(width = 58.dp, role = MiniTextRole.Body2)
            }
        }

        if (!standardNav) {
            MiniNavViewport(bottomPadding = liquidBottomSpace) {
                if (liquidBackdrop != null) {
                    val labels =
                        listOf(
                            stringResource(R.string.nav_home),
                            stringResource(R.string.nav_features),
                            stringResource(R.string.nav_settings),
                        )
                    val icons =
                        listOf(
                            MiuixIcons.Normal.Home,
                            MiuixIcons.Normal.Tune,
                            MiuixIcons.Normal.Settings,
                        )
                    LiquidNavBar(
                        selectedIndex = { 2 },
                        onSelected = {},
                        backdrop = liquidBackdrop,
                        tabsCount = labels.size,
                        dark = dark,
                        mode = liquidMode,
                        interactive = false,
                        modifier = Modifier.padding(horizontal = LiquidNavSpec.sidePadding),
                    ) {
                        labels.indices.forEach { index ->
                            LiquidNavEntry(
                                contentMode = content,
                                onClick = {},
                                icon = icons[index],
                                label = labels[index],
                                interactive = false,
                            )
                        }
                    }
                } else {
                    val previewInteractive = uiStyle != UiStyle.OS4
                    FloatingNavigationBar(
                        modifier = if (previewInteractive) floatingModifier else floatingModifier.clearAndSetSemantics {},
                        color =
                            if (miuixBackdrop != null) {
                                Color.Transparent
                            } else {
                                MiuixTheme.colorScheme.surfaceContainer
                            },
                        defaultWindowInsetsPadding = false,
                    ) {
                        NavContentItem(
                            content = content,
                            selected = false,
                            onClick = {},
                            icon = MiuixIcons.Normal.Home,
                            label = stringResource(R.string.nav_home),
                            interactive = previewInteractive,
                        )
                        NavContentItem(
                            content = content,
                            selected = false,
                            onClick = {},
                            icon = MiuixIcons.Normal.Tune,
                            label = stringResource(R.string.nav_features),
                            interactive = previewInteractive,
                        )
                        NavContentItem(
                            content = content,
                            selected = true,
                            onClick = {},
                            icon = MiuixIcons.Medium.Settings,
                            label = stringResource(R.string.nav_settings),
                            interactive = previewInteractive,
                        )
                    }
                }
            }
        } else {
            val showLabel = !os4Glass || content == NavContent.IconAndText
            MiniNavViewport {
                NavigationBar(
                    color = MiuixTheme.colorScheme.surface,
                    showDivider = true,
                    defaultWindowInsetsPadding = false,
                ) {
                    MiniStandardNavigationItem(selected = false, icon = MiuixIcons.Normal.Home, showLabel = showLabel)
                    MiniStandardNavigationItem(selected = false, icon = MiuixIcons.Normal.Tune, showLabel = showLabel)
                    MiniStandardNavigationItem(selected = true, icon = MiuixIcons.Medium.Settings, showLabel = showLabel)
                }
            }
        }
    }
}

@Composable
private fun MiniNavViewport(
    bottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = content,
    ) { measurables, constraints ->
        val placeable =
            measurables.single().measure(
                constraints.copy(
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            )
        val bottomPx = bottomPadding.roundToPx()
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(
                x = ((constraints.maxWidth - placeable.width) / 2).coerceAtLeast(0),
                y =
                    (constraints.maxHeight - placeable.height - bottomPx)
                        .coerceAtLeast(0),
            )
        }
    }
}

@Composable
private fun RowScope.MiniStandardNavigationItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    showLabel: Boolean,
) {
    val contentColor =
        MiuixTheme.colorScheme.onSurface.copy(
            alpha = if (selected) 1f else 0.4f,
        )

    Column(
        modifier =
            Modifier
                .height(NavigationBarDefaults.ItemHeight)
                .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (showLabel) Arrangement.Top else Arrangement.Center,
    ) {
        if (showLabel) Spacer(modifier = Modifier.height(NavigationBarDefaults.IconTopPadding))
        Icon(
            modifier = Modifier.size(NavigationBarDefaults.IconSize),
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
        )
        if (showLabel) {
            Spacer(modifier = Modifier.height(3.dp))
            MiniTextBar(
                width = if (selected) 30.dp else 26.dp,
                role = MiniTextRole.Body2,
            )
        }
    }
}
