// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.components.glass

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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults

private val RailTopBarIcon: ImageVector = ImageVector.Builder(
    name = "RailTopBar",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(6f, 3f)
        lineTo(18f, 3f)
        curveTo(21f, 3f, 22f, 4f, 22f, 7f)
        lineTo(22f, 17f)
        curveTo(22f, 20f, 21f, 21f, 18f, 21f)
        lineTo(6f, 21f)
        curveTo(3f, 21f, 2f, 20f, 2f, 17f)
        lineTo(2f, 7f)
        curveTo(2f, 4f, 3f, 3f, 6f, 3f)
        close()
    }
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.8f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(9f, 7f)
        lineTo(15f, 7f)
    }
}.build()

private val RailSideBarIcon: ImageVector = ImageVector.Builder(
    name = "RailSideBar",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(6f, 3f)
        lineTo(18f, 3f)
        curveTo(21f, 3f, 22f, 4f, 22f, 7f)
        lineTo(22f, 17f)
        curveTo(22f, 20f, 21f, 21f, 18f, 21f)
        lineTo(6f, 21f)
        curveTo(3f, 21f, 2f, 20f, 2f, 17f)
        lineTo(2f, 7f)
        curveTo(2f, 4f, 3f, 3f, 6f, 3f)
        close()
    }
    path(fill = SolidColor(Color.White)) {
        moveTo(4f, 6.6f)
        curveTo(4f, 6.03995f, 4f, 5.75992f, 4.10899f, 5.54601f)
        curveTo(4.20487f, 5.35785f, 4.35785f, 5.20487f, 4.54601f, 5.10899f)
        curveTo(4.75992f, 5f, 5.03995f, 5f, 5.6f, 5f)
        lineTo(8.4f, 5f)
        curveTo(8.96005f, 5f, 9.24008f, 5f, 9.45399f, 5.10899f)
        curveTo(9.64215f, 5.20487f, 9.79513f, 5.35785f, 9.89101f, 5.54601f)
        curveTo(10f, 5.75992f, 10f, 6.03995f, 10f, 6.6f)
        lineTo(10f, 17.4f)
        curveTo(10f, 17.9601f, 10f, 18.2401f, 9.89101f, 18.454f)
        curveTo(9.79513f, 18.6422f, 9.64215f, 18.7951f, 9.45399f, 18.891f)
        curveTo(9.24008f, 19f, 8.96005f, 19f, 8.4f, 19f)
        lineTo(5.6f, 19f)
        curveTo(5.03995f, 19f, 4.75992f, 19f, 4.54601f, 18.891f)
        curveTo(4.35785f, 18.7951f, 4.20487f, 18.6422f, 4.10899f, 18.454f)
        curveTo(4f, 18.2401f, 4f, 17.9601f, 4f, 17.4f)
        close()
    }
}.build()


/** The expand/collapse toggle with its embedded top-bar or sidebar glyph. */
@Composable
internal fun RailToggle(
    onClick: () -> Unit,
    expanded: Boolean,
    size: Dp,
    iconSize: Dp,
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
        GlassIconButton(
            onClick = onClick,
            inheritTopBarMaterial = false,
            size = size,
            surfaceAlpha = 0f,
            shadow = null,
        ) {
            RailToggleGlyph(
                iconSize = iconSize,
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
                iconSize = iconSize,
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
    iconSize: Dp,
    tint: Color,
    topBarAlpha: () -> Float,
    sideBarAlpha: () -> Float,
    contentAlpha: () -> Float,
    contentDescription: String?,
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = RailTopBarIcon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { alpha = topBarAlpha() * contentAlpha() },
            tint = tint,
        )
        Icon(
            imageVector = RailSideBarIcon,
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { alpha = sideBarAlpha() * contentAlpha() },
            tint = tint,
        )
    }
}

/**
 * One destination row. Its slot is interpolated in the layout phase from the collapsed bar's
 * destination into the expanded column; only the icon's opacity changes in the draw phase.
 */
