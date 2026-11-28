// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.glass.GlassIconButton as UpstreamGlassIconButton

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.symbol.SidebarExpand
import top.yukonga.miuix.kmp.icon.symbol.TopNavigation
import top.yukonga.miuix.kmp.symbol.rememberSymbolPainter
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults

/** The expand/collapse toggle with its embedded top-bar or sidebar glyph. */
@Composable
internal fun RailToggle(
    onClick: () -> Unit,
    expanded: Boolean,
    size: Dp,
    tint: Color,
    contentDescription: String,
) {
    val collapsedInteractionSource = remember { MutableInteractionSource() }
    val collapsedPressed by collapsedInteractionSource.collectIsPressedAsState()
    val collapsedContentAlpha = animateFloatAsState(
        targetValue = if (collapsedPressed) GlassTopAppBarDefaults.PressedContentAlpha else 1f,
        animationSpec = if (collapsedPressed) GlassMotion.pressDown() else GlassMotion.pressUp(),
        label = "railCollapsedToggleContentAlpha",
    )
    val topBarAlpha = animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = GlassMotion.navShowHide(),
        label = "railTopBarIconAlpha",
    )
    val sideBarAlpha = animateFloatAsState(
        targetValue = if (expanded) 0f else 1f,
        animationSpec = GlassMotion.navShowHide(),
        label = "railSideBarIconAlpha",
    )
    if (expanded) {
        UpstreamGlassIconButton(
            onClick = onClick,
            inheritTopBarMaterial = false,
            size = size,
            surfaceAlpha = 0f,
            shadow = null,
        ) {
            RailToggleGlyph(
                tint = tint,
                topBarAlpha = { topBarAlpha.value },
                sideBarAlpha = { sideBarAlpha.value },
                contentAlpha = { 1f },
                contentDescription = contentDescription,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clickable(
                    interactionSource = collapsedInteractionSource,
                    indication = null,
                    onClick = onClick,
                )
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            RailToggleGlyph(
                tint = tint,
                topBarAlpha = { topBarAlpha.value },
                sideBarAlpha = { sideBarAlpha.value },
                contentAlpha = { collapsedContentAlpha.value },
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun RailToggleGlyph(
    tint: Color,
    topBarAlpha: () -> Float,
    sideBarAlpha: () -> Float,
    contentAlpha: () -> Float,
    contentDescription: String?,
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            painter = rememberSymbolPainter(MiuixIcons.Symbol.TopNavigation),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(RailSideIconSize)
                .graphicsLayer { alpha = topBarAlpha() * contentAlpha() },
            tint = tint,
        )
        Icon(
            painter = rememberSymbolPainter(MiuixIcons.Symbol.SidebarExpand),
            contentDescription = null,
            modifier = Modifier
                .size(RailSideIconSize)
                .graphicsLayer { alpha = sideBarAlpha() * contentAlpha() },
            tint = tint,
        )
    }
}

/**
 * One destination row. Its slot is interpolated in the layout phase from the collapsed bar's
 * destination into the expanded column; only the icon's opacity changes in the draw phase.
 */
