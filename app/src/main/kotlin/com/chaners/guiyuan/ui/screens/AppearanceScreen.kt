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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppThemeMode
import com.chaners.guiyuan.settings.AppearanceSettings
import com.chaners.guiyuan.settings.FloatingNavContent
import com.chaners.guiyuan.settings.FloatingNavStyle
import com.chaners.guiyuan.ui.components.FloatingNavItem
import com.chaners.guiyuan.ui.components.floatingNavigationMaterial
import com.chaners.guiyuan.ui.components.requiresTextureBackdrop
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
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
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppearanceScreen(
    settings: AppearanceSettings,
    darkMode: Boolean,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorEnabledChange: (Boolean) -> Unit,
    onFloatingNavigationBarEnabledChange: (Boolean) -> Unit,
    onFloatingNavStyleChange: (FloatingNavStyle) -> Unit,
    onFloatingNavContentChange: (FloatingNavContent) -> Unit,
    onBack: () -> Unit,
) {
    val themeOptions =
        listOf(
            stringResource(R.string.theme_system),
            stringResource(R.string.theme_light),
            stringResource(R.string.theme_dark),
        )
    val floatingStyleOptions =
        listOf(
            stringResource(R.string.floating_navigation_style_standard),
            stringResource(R.string.floating_navigation_style_blur),
            stringResource(R.string.floating_navigation_style_glass),
        )
    val floatingContentOptions =
        listOf(
            stringResource(R.string.floating_navigation_content_icon_only),
            stringResource(R.string.floating_navigation_content_icon_and_text),
        )

    SettingsPage(title = stringResource(R.string.appearance_title), onBack = onBack) {
        item {
            AppearanceThemePreview(
                settings = settings,
                darkMode = darkMode,
            )
        }

        Section(R.string.section_theme) {
            OverlayDropdownPreference(
                items = themeOptions,
                selectedIndex = settings.themeMode.ordinal,
                title = stringResource(R.string.theme_mode),
                summary = stringResource(R.string.theme_mode_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_contrast,
                    )
                },
                showValue = true,
                onSelectedIndexChange = { index ->
                    AppThemeMode.entries.getOrNull(index)?.let { mode ->
                        if (mode != settings.themeMode) {
                            onThemeModeChange(mode)
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
                checked = settings.dynamicColorEnabled,
                onCheckedChange = onDynamicColorEnabledChange,
            )
        }

        Section(R.string.section_visual_effects) {
            SwitchPreference(
                title = stringResource(R.string.floating_navigation_bar),
                summary = stringResource(R.string.floating_navigation_bar_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_bottom_navigation,
                    )
                },
                checked = settings.floatingNavigationBarEnabled,
                onCheckedChange = onFloatingNavigationBarEnabledChange,
            )
            AnimatedPreferenceGroup(visible = settings.floatingNavigationBarEnabled) {
                OverlayDropdownPreference(
                    items = floatingStyleOptions,
                    selectedIndex = settings.floatingNavigationStyle.ordinal,
                    title = stringResource(R.string.floating_navigation_style),
                    summary = stringResource(R.string.floating_navigation_style_summary),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_style,
                        )
                    },
                    showValue = true,
                    onSelectedIndexChange = { index ->
                        FloatingNavStyle.entries.getOrNull(index)?.let { style ->
                            if (style != settings.floatingNavigationStyle) {
                                onFloatingNavStyleChange(style)
                            }
                        }
                    },
                )
                OverlayDropdownPreference(
                    items = floatingContentOptions,
                    selectedIndex = settings.floatingNavigationContent.ordinal,
                    title = stringResource(R.string.floating_navigation_content),
                    summary = stringResource(R.string.floating_navigation_content_summary),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_format_list_bulleted,
                        )
                    },
                    showValue = true,
                    onSelectedIndexChange = { index ->
                        FloatingNavContent.entries.getOrNull(index)?.let { content ->
                            if (content != settings.floatingNavigationContent) {
                                onFloatingNavContentChange(content)
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
    settings: AppearanceSettings,
    darkMode: Boolean,
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
                settings = settings,
                darkMode = darkMode,
            )
        }
    }
}

@Composable
private fun AppearanceMiniPreview(
    settings: AppearanceSettings,
    darkMode: Boolean,
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
                    floating = settings.floatingNavigationBarEnabled,
                    style = settings.floatingNavigationStyle,
                    content = settings.floatingNavigationContent,
                    darkMode = darkMode,
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
    floating: Boolean,
    style: FloatingNavStyle,
    content: FloatingNavContent,
    darkMode: Boolean,
) {
    val materialActive =
        floating &&
            style.requiresTextureBackdrop &&
            isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop =
        if (materialActive) {
            rememberLayerBackdrop {
                drawRect(surfaceColor)
                drawContent()
            }
        } else {
            null
        }
    val floatingModifier =
        if (backdrop != null) {
            Modifier.floatingNavigationMaterial(
                backdrop = backdrop,
                darkMode = darkMode,
                style = style,
            )
        } else {
            Modifier
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(MiniNavigationViewportHeight),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (backdrop != null) {
                            Modifier.layerBackdrop(backdrop)
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

        if (floating) {
            MiniNavigationViewportContent {
                FloatingNavigationBar(
                    modifier = floatingModifier,
                    color =
                        if (backdrop != null) {
                            Color.Transparent
                        } else {
                            MiuixTheme.colorScheme.surfaceContainer
                        },
                    defaultWindowInsetsPadding = false,
                ) {
                    FloatingNavItem(
                        content = content,
                        selected = false,
                        onClick = {},
                        icon = MiuixIcons.Normal.Home,
                        label = stringResource(R.string.nav_home),
                    )
                    FloatingNavItem(
                        content = content,
                        selected = false,
                        onClick = {},
                        icon = MiuixIcons.Normal.Tune,
                        label = stringResource(R.string.nav_features),
                    )
                    FloatingNavItem(
                        content = content,
                        selected = true,
                        onClick = {},
                        icon = MiuixIcons.Medium.Settings,
                        label = stringResource(R.string.nav_settings),
                    )
                }
            }
        } else {
            MiniNavigationViewportContent {
                NavigationBar(
                    color = MiuixTheme.colorScheme.surface,
                    showDivider = true,
                    defaultWindowInsetsPadding = false,
                ) {
                    MiniStandardNavigationItem(selected = false, icon = MiuixIcons.Normal.Home)
                    MiniStandardNavigationItem(selected = false, icon = MiuixIcons.Normal.Tune)
                    MiniStandardNavigationItem(selected = true, icon = MiuixIcons.Medium.Settings)
                }
            }
        }
    }
}

@Composable
private fun MiniNavigationViewportContent(
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
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(
                x = ((constraints.maxWidth - placeable.width) / 2).coerceAtLeast(0),
                y = (constraints.maxHeight - placeable.height).coerceAtLeast(0),
            )
        }
    }
}

@Composable
private fun RowScope.MiniStandardNavigationItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    ) {
        Spacer(modifier = Modifier.height(NavigationBarDefaults.IconTopPadding))
        Icon(
            modifier = Modifier.size(NavigationBarDefaults.IconSize),
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
        )
        Spacer(modifier = Modifier.height(3.dp))
        MiniTextBar(
            width = if (selected) 30.dp else 26.dp,
            role = MiniTextRole.Body2,
        )
    }
}
