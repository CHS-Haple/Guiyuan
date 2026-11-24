// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlinx.coroutines.flow.first

@Composable
internal fun RailLiquidIndicator(
    context: RailPanelContext,
    geometry: RailPanelGeometry,
    motion: RailPanelMotion,
    colors: RailPanelColors,
    interaction: NavigationRailInteraction,
) {
    val pressedIndex = interaction.pressedIndex
    val selectedIndex = context.selection.selectedIndex
    val index = if (pressedIndex >= 0) pressedIndex else selectedIndex
    // The existing indicator spring owns position; no separate optical velocity sampler.
    val rowPosition = animateFloatAsState(
        targetValue = if (context.state.expanded && pressedIndex >= 0) interaction.previewRow
            else index.toFloat(),
        animationSpec = spring(dampingRatio = 1f, stiffness = 1000f),
        label = "liquidRailIndicatorRow",
    )
    val base = colors.indicator
    // Preserve the visual press until the selected lens has settled.
    var settlingPress by remember(interaction) { mutableStateOf(false) }
    val pressing = pressedIndex >= 0
    // The geometry writer changes the rail slot endpoints on resize. Restart the
    // release-settle observer with those endpoints instead of awaiting a stale target.
    LaunchedEffect(interaction, pressing, selectedIndex, context.state.expanded, geometry.tabWidths) {
        if (pressing) {
            settlingPress = true
        } else {
            if (context.state.expanded) {
                val tolerance = (context.selection.items.size - 1) * 0.025f
                snapshotFlow { rowPosition.value }
                    .first { abs(it - selectedIndex.toFloat()) <= tolerance }
            } else {
                // Slot endpoints can change while the release spring is running.
                // Compare against the live geometry on every animation observation.
                snapshotFlow {
                    val left = interaction.start(selectedIndex)
                    val right = interaction.start(selectedIndex + 1)
                    val tolerance = (interaction.width * 0.025f).coerceAtLeast(1f)
                    abs(interaction.left.value - left) <= tolerance &&
                        abs(interaction.right.value - right) <= tolerance
                }.first { it }
            }
            settlingPress = false
        }
    }
    NavigationRailIndicator(
        selection = RailIndicatorSelection(index, interaction, pressing || settlingPress),
        motion = RailIndicatorMotion(
            progress = { motion.rowProgress[index].value },
            scrollOffset = { context.scrollState.value },
            expandedItemWidth = geometry.endpoints.expandedWidth - RailItemHorizontalMargin * 2,
            rowPosition = { rowPosition.value },
            outsideScrollViewport = true,
        ),
        appearance = GlassIndicatorAppearance(
            color = base.color,
            pressedColor = base.pressedColor,
            scaleOnPress = base.scaleOnPress,
            liquidPaint = base.liquidPaint,
            lensTracking = railLensTracking(motion, geometry, index, interaction),
        ),
    )
}

/** Track the exact same horizontal lens bounds used by NavigationRailIndicator's layout. */
@Composable
private fun railLensTracking(
    motion: RailPanelMotion,
    geometry: RailPanelGeometry,
    index: Int,
    interaction: NavigationRailInteraction,
): RailLensTracking {
    val density = LocalDensity.current
    return RailLensTracking(motion.liquid.lensEdge) {
        with(density) {
            val fraction = motion.rowProgress[index].value.fastCoerceIn(0f, 1f)
            val bounds = navigationIndicatorBounds(
                interaction.left.value, interaction.right.value, interaction.width,
                interaction.contentStart, interaction.endPadding,
            )
            val expandedWidth = (geometry.endpoints.expandedWidth - RailItemHorizontalMargin * 2).toPx()
            lerp(bounds.x, RailItemHorizontalMargin.toPx(), fraction) +
                lerp(bounds.y - bounds.x, expandedWidth, fraction)
        }
    }
}
