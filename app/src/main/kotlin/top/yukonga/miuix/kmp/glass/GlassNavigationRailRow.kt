// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import kotlin.math.roundToInt

@Composable
internal fun NavigationRailRow(
    item: GlassNavigationItem,
    selected: Boolean,
    position: Int,
    slot: NavigationRailSlot,
    interactionSource: MutableInteractionSource,
    expanded: Boolean,
    pressed: Boolean,
    rowProgress: () -> Float,
    iconAlpha: () -> Float,
    scrollOffset: () -> Int,
    tabWidths: List<Dp>,
    contentStart: Dp,
    labelWidth: Dp,
    itemWidth: Dp,
    selectedColor: Color,
    unselectedColor: Color,
    labelStyle: TextStyle,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = GlassMotion.navContent(),
        label = "glassNavigationRailTint",
    )
    val pressAlpha = animateFloatAsState(
        targetValue = if (pressed) GlassNavigationBarDefaults.PressedAlpha else 1f,
        animationSpec = GlassMotion.navContentFloat(),
        label = "glassNavigationRailPress",
    )
    val density = LocalDensity.current
    val geometry = remember(density, position, tabWidths, contentStart, labelWidth, itemWidth) {
        with(density) {
            val padding = RailItemContentPadding.roundToPx()
            val labelStart = padding + RailIconColumnWidth.roundToPx()
            var slotStart = contentStart.roundToPx()
            for (index in 0 until position) slotStart += tabWidths[index].roundToPx()
            val slotWidth = tabWidths[position].roundToPx()
            val labelPx = labelWidth.roundToPx().coerceAtMost(slotWidth)
            val expandedLabelWidth = (itemWidth - RailItemContentPadding * 2 - RailIconColumnWidth).roundToPx()
            RailRowGeometry(
                slotStart = slotStart.toFloat(),
                slotWidth = slotWidth.toFloat(),
                sourceStart = (slotStart + ((slotWidth - labelPx) / 2f).roundToInt() - labelStart).toFloat(),
                sourceWidth = (labelStart + labelPx + padding).toFloat(),
                expandedTop = (RailExpandedRowTop + RailRowPitch * position).toPx(),
                labelMinWidth = if (labelWidth.roundToPx() > slotWidth) minOf(labelPx, expandedLabelWidth) else expandedLabelWidth,
            )
        }
    }
    // This box is the actual slot, not the wider row retaining its invisible icon column.
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val fraction = rowProgress().coerceIn(0f, 1f)
                val width = lerp(geometry.slotWidth, itemWidth.toPx(), fraction).roundToInt().coerceAtLeast(0)
                val height = (RailCollapsedRowHeight + RailRowGrowth * fraction).roundToPx()
                val x = lerp(geometry.slotStart, RailItemHorizontalMargin.toPx(), fraction).roundToInt()
                // The scrolling parent places settled rows; only the morph needs scroll compensation.
                val y = if (fraction == 1f) {
                    geometry.expandedTop.roundToInt()
                } else {
                    lerp(RailRowTop.toPx() + scrollOffset(), geometry.expandedTop, fraction).roundToInt()
                }
                slot.left = x.toFloat()
                slot.top = y.toFloat()
                slot.width = width.toFloat()
                slot.height = height.toFloat()
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(x, y) }
            }
            .clip(RoundedCornerShape(percent = 50))
            .indication(interactionSource, LocalIndication.current)
            .hoverable(interactionSource, enabled = expanded)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                (item.contentDescription ?: item.label)?.let { contentDescription = it }
                onClick {
                    onClick()
                    true
                }
            }
            .onKeyEvent { event ->
                if (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Spacebar) {
                    if (event.type == KeyEventType.KeyUp) onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(interactionSource = interactionSource)
            .graphicsLayer { alpha = pressAlpha.value },
    ) {
        Row(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val fraction = rowProgress().coerceIn(0f, 1f)
                    val width = lerp(geometry.sourceWidth, itemWidth.toPx(), fraction).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                    val x = lerp(geometry.sourceStart - geometry.slotStart, 0f, fraction).roundToInt()
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(x, 0) }
                }
                .padding(horizontal = RailItemContentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(RailIconColumnWidth)
                    .graphicsLayer { this.alpha = iconAlpha() },
            ) {
                BadgedBox(badge = { item.badge?.invoke() }) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(RailIconSize),
                        tint = tint,
                    )
                }
            }
            if (item.label != null) {
                Text(
                    text = item.label,
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            // Keep rounding from truncating a fitting label, without exceeding either endpoint.
                            constraints.copy(maxWidth = maxOf(constraints.maxWidth, geometry.labelMinWidth)),
                        )
                        layout(placeable.width.coerceAtMost(constraints.maxWidth), placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    },
                    color = tint,
                    style = labelStyle,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The same pressed indicator follows the current slot through the entire morph. */
@Composable
internal fun NavigationRailIndicator(
    selectedIndex: Int,
    rowProgress: () -> Float,
    scrollOffset: () -> Int,
    interaction: NavigationRailInteraction,
    itemWidth: Dp,
    color: Color,
    pressed: Boolean,
    pressedColor: Color,
) {
    GlassNavigationIndicator(
        pressed = pressed,
        height = RailCollapsedRowHeight,
        color = color,
        pressedColor = pressedColor,
        modifier = Modifier.layout { measurable, constraints ->
            val fraction = rowProgress().coerceIn(0f, 1f)
            val width: Int
            val x: Float
            val y: Float
            val expandedY = RailExpandedRowTop.toPx() + RailRowPitch.toPx() * selectedIndex
            if (fraction == 1f) {
                // Collapsed springs and scroll compensation cannot affect the expanded indicator.
                width = itemWidth.roundToPx().coerceAtLeast(0)
                x = RailItemHorizontalMargin.toPx()
                y = expandedY
            } else {
                val bounds = navigationIndicatorBounds(
                    interaction.left.value,
                    interaction.right.value,
                    interaction.width,
                    interaction.contentStart,
                    interaction.endPadding,
                )
                width = lerp(bounds.y - bounds.x, itemWidth.toPx(), fraction).roundToInt().coerceAtLeast(0)
                x = lerp(bounds.x, RailItemHorizontalMargin.toPx(), fraction)
                y = lerp(RailRowTop.toPx() + scrollOffset(), expandedY, fraction)
            }
            val height = (RailCollapsedRowHeight + RailRowGrowth * fraction).roundToPx()
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.placeRelative(x.roundToInt(), y.roundToInt())
            }
        },
    )
}

/** A row's fixed flight ends, in pixels. */
private class RailRowGeometry(
    val slotStart: Float,
    val slotWidth: Float,
    val sourceStart: Float,
    val sourceWidth: Float,
    val expandedTop: Float,
    val labelMinWidth: Int,
)
