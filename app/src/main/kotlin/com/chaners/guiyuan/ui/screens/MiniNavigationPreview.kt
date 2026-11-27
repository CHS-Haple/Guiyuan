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
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.ui.MainNavItem
import com.chaners.guiyuan.ui.components.NavContentItem
import com.chaners.guiyuan.ui.components.floatingNavBlur
import com.chaners.guiyuan.ui.components.liquid.LiquidNavBar
import com.chaners.guiyuan.ui.components.liquid.LiquidNavEntry
import com.chaners.guiyuan.ui.components.liquid.LiquidNavSpec
import com.chaners.guiyuan.ui.components.liquid.liquidNavBottomPadding
import com.chaners.guiyuan.ui.components.liquid.rememberLiquidNavBackdrop
import com.chaners.guiyuan.ui.components.requiresTextureBackdrop
import com.chaners.guiyuan.ui.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.LayerBackdrop
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
    appearance: Appearance,
    dark: Boolean,
) {
    val glassNavigation = appearance.navEnabled && appearance.navStyle == NavStyle.Glass
    val miuixMaterialActive =
        appearance.navEnabled && appearance.navStyle.requiresTextureBackdrop && isRuntimeShaderSupported()
    val liquidMaterialActive =
        appearance.navEnabled && appearance.navStyle == NavStyle.Liquid &&
            isRuntimeShaderSupported()
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
    val standardNav = !appearance.navEnabled || (glassNavigation && miuixBackdrop == null)

    if (glassNavigation && miuixBackdrop != null) {
        MiniGlassNavigationPreview(appearance.navContent, miuixBackdrop)
        return
    }

    MiniPreviewFrame(
        miuixBackdrop = miuixBackdrop,
        liquidBackdrop = liquidBackdrop,
        bottomPadding = liquidBottomSpace,
    ) {
        when {
            standardNav -> MiniStandardNavigationPreview(appearance.navContent, glassNavigation)
            liquidBackdrop != null -> MiniLiquidNavigationPreview(appearance.navContent, appearance.liquidMode, dark, liquidBackdrop)
            else -> MiniFloatingNavigationPreview(appearance.navContent, miuixBackdrop)
        }
    }
}


@Composable
private fun MiniGlassNavigationPreview(content: NavContent, miuixBackdrop: LayerBackdrop) {
    // The glass bar can grow for captions. Let the preview measure its actual
    // height and keep its shadow inside the preview surface.
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(miuixBackdrop),
        ) {
            MiniPreviewBackdropContent()
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(20.dp))
            GlassNavigationBar(
                items = miniNavItems().map { item ->
                    GlassNavigationItem(item.icon, item.label.takeIf { content == NavContent.IconAndText })
                },
                selectedIndex = 2,
                onSelect = {},
                backdrop = miuixBackdrop,
                interactive = false,
                previewLabel = { selected -> MiniNavLabelBar(selected) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun MiniPreviewFrame(
    miuixBackdrop: LayerBackdrop?,
    liquidBackdrop: LayerBackdrop?,
    bottomPadding: Dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .then(
                        if (miuixBackdrop != null) {
                            Modifier.layerBackdrop(miuixBackdrop)
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (liquidBackdrop != null) {
                            Modifier.layerBackdrop(liquidBackdrop)
                        } else {
                            Modifier
                        },
                    ),
        ) {
            MiniPreviewBackdropContent()
        }
        MiniNavViewport(bottomPadding = bottomPadding) {
            content()
        }
    }
}

@Composable
private fun MiniLiquidNavigationPreview(
    content: NavContent,
    liquidMode: LiquidMode,
    dark: Boolean,
    liquidBackdrop: LayerBackdrop,
) {
    val items = miniNavItems()
    LiquidNavBar(
        selectedIndex = { 2 },
        onSelected = {},
        backdrop = liquidBackdrop,
        tabsCount = items.size,
        dark = dark,
        mode = liquidMode,
        interactive = false,
        modifier = Modifier.padding(horizontal = LiquidNavSpec.sidePadding),
    ) {
        items.forEachIndexed { index, item ->
            LiquidNavEntry(
                contentMode = content,
                selected = index == 2,
                onClick = null,
                item = item,
                previewLabel = { MiniNavLabelBar(index == 2) },
            )
        }
    }
}

@Composable
private fun MiniFloatingNavigationPreview(
    content: NavContent,
    miuixBackdrop: LayerBackdrop?,
) {
    val floatingModifier =
        if (miuixBackdrop != null) {
            Modifier.floatingNavBlur(
                backdrop = miuixBackdrop,
            )
        } else {
            Modifier
        }
    FloatingNavigationBar(
        modifier = floatingModifier.clearAndSetSemantics {},
        color =
            if (miuixBackdrop != null) {
                Color.Transparent
            } else {
                MiuixTheme.colorScheme.surfaceContainer
            },
        defaultWindowInsetsPadding = true,
    ) {
        miniNavItems().forEachIndexed { index, item ->
            NavContentItem(
                content = content,
                selected = index == 2,
                onClick = null,
                item = item,
                previewLabel = { MiniNavLabelBar(index == 2) },
            )
        }
    }
}

@Composable
private fun MiniStandardNavigationPreview(content: NavContent, glassNavigation: Boolean) {
    val showLabel = !glassNavigation || content == NavContent.IconAndText
    NavigationBar(
        color = MiuixTheme.colorScheme.surface,
        showDivider = true,
        defaultWindowInsetsPadding = false,
    ) {
        MiniStandardNavigationItem(selected = false, icon = MiuixIcons.Normal.Home, showLabel = showLabel)
        MiniStandardNavigationItem(selected = false, icon = MiuixIcons.Normal.Tune, showLabel = showLabel)
        MiniStandardNavigationItem(selected = true, icon = MiuixIcons.Normal.Settings, showLabel = showLabel)
    }
}


@Composable
private fun MiniNavViewport(
    bottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = content,
    ) { measurables, constraints ->
        // Measure the native component's own bottom inset; do not approximate its margin.
        val placeable =
            measurables.single().measure(
                constraints.copy(
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            )
        val bottomPx = bottomPadding.roundToPx()
        val minHeightPx = (MiniNavigationViewportHeight + bottomPadding).roundToPx()
        val viewportHeight = maxOf(minHeightPx, placeable.height + bottomPx)
        layout(constraints.maxWidth, viewportHeight) {
            placeable.placeRelative(
                x = ((constraints.maxWidth - placeable.width) / 2).coerceAtLeast(0),
                y = (viewportHeight - placeable.height - bottomPx).coerceAtLeast(0),
            )
        }
    }
}

@Composable
private fun miniNavItems(): List<MainNavItem> = listOf(
    MainNavItem(stringResource(R.string.nav_home), MiuixIcons.Normal.Home),
    MainNavItem(stringResource(R.string.nav_features), MiuixIcons.Normal.Tune),
    MainNavItem(stringResource(R.string.nav_settings), MiuixIcons.Normal.Settings),
)

@Composable
private fun MiniNavLabelBar(selected: Boolean) {
    MiniTextBar(
        width = if (selected) 30.dp else 26.dp,
        role = MiniTextRole.Body2,
    )
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
            MiniNavLabelBar(selected)
        }
    }
}

@Composable
private fun MiniPreviewBackdropContent() {
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
