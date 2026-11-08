package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle
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
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val MiniNavigationViewportHeight = 76.dp

@Composable
internal fun MiniNavigationPreview(
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
