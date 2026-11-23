// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified from upstream (2026): preview interactivity and bottom hit-area extension.

package com.chaners.guiyuan.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.RowScope.GlassNavigationBarItemContent(
    item: GlassNavigationItem,
    position: Int,
    selected: Boolean,
    pressedIndexProvider: () -> Int,
    selectedColor: Color,
    unselectedColor: Color,
    labelSize: androidx.compose.ui.unit.TextUnit,
    height: Dp,
    interactionEnabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val tintState = animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = GlassMotion.navContent(),
        label = "glassNavigationTint",
    )
    val tintProducer = remember { ColorProducer { tintState.value } }
    val isPressed by remember(position) {
        computedStateOf { pressedIndexProvider() == position }
    }
    val pressAlpha = animateFloatAsState(
        targetValue = if (isPressed) {
            GlassNavigationBarDefaults.PressedAlpha
        } else {
            1f
        },
        animationSpec = GlassMotion.navContentFloat(),
        label = "glassNavigationPress",
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = height)
            .graphicsLayer { this.alpha = pressAlpha.value }
            .padding(
                horizontal = GlassNavigationBarDefaults.IndicatorOverhang,
                vertical = GlassNavigationBarDefaults.ContentPaddingVertical,
            )
            .then(
                if (interactionEnabled) {
                    Modifier
                        .navigationTabActivation(
                            selected = selected,
                            description = item.contentDescription ?: item.label,
                            onActivate = { onSelect(position) },
                        )
                        .focusable()
                } else {
                    Modifier.clearAndSetSemantics { }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GlassNavigationBarDefaults.LabelSpacing),
        ) {
            BadgedBox(badge = { item.badge?.invoke() }) {
                Icon(
                    painter = rememberVectorPainter(item.icon),
                    tint = tintProducer,
                    contentDescription = null,
                    modifier = Modifier.size(GlassNavigationBarDefaults.IconSize),
                )
            }
            item.label?.let { label ->
                val baseLabelStyle = MiuixTheme.textStyles.footnote2
                val labelTextStyle = remember(baseLabelStyle, labelSize) {
                    baseLabelStyle.copy(fontSize = labelSize)
                }
                Text(
                    text = label,
                    color = tintProducer,
                    style = labelTextStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
