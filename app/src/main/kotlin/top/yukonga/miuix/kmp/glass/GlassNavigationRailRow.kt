// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.chaners.guiyuan.ui.components.liquid.LiquidFeedbackSpec
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

/** A row's selection, press source and click callback, retained by its caller. */
internal class RailRowIdentity(
    val item: GlassNavigationItem,
    val selected: Boolean,
    val position: Int,
    val slot: NavigationRailSlot,
    val interactionSource: MutableInteractionSource,
    val onClick: () -> Unit,
)

/** The upstream row's per-frame layout and press animation inputs. */
internal class RailRowMotion(
    val expanded: Boolean,
    val pressed: Boolean,
    val rowProgress: () -> Float,
    val iconAlpha: () -> Float,
    val scrollOffset: () -> Int,
)

/** The source and destination widths of a flying rail row. */
internal class RailRowLayoutSpec(
    val tabWidths: List<Dp>,
    val contentStart: Dp,
    val labelWidth: Dp,
    val itemWidth: Dp,
)

/** Common row tint and typography. */
internal class RailRowAppearance(
    val selectedColor: Color,
    val unselectedColor: Color,
    val labelStyle: TextStyle,
    val liquidIndicator: Boolean = false,
)

/** The resolved geometry and tint are shared by the row's frame and its contents. */
private class RailRowFrame(val geometry: RailRowGeometry, val tint: Color)

@Composable
internal fun NavigationRailRow(
    identity: RailRowIdentity,
    motion: RailRowMotion,
    layout: RailRowLayoutSpec,
    appearance: RailRowAppearance,
) {
    val tint by animateColorAsState(
        targetValue = if (identity.selected) appearance.selectedColor else appearance.unselectedColor,
        animationSpec = GlassMotion.navContent(),
        label = "glassNavigationRailTint",
    )
    val pressAlpha = animateFloatAsState(
        targetValue = if (motion.pressed && !appearance.liquidIndicator) {
            GlassNavigationBarDefaults.PressedAlpha
        } else 1f,
        animationSpec = GlassMotion.navContentFloat(),
        label = "glassNavigationRailPress",
    )
    val frame = RailRowFrame(rememberRailRowGeometry(identity, layout), tint)
    Box(
        modifier = Modifier
            .railRowPlacement(identity, motion, layout, frame.geometry)
            .railRowInteraction(identity, motion, pressAlpha.value, appearance.liquidIndicator),
    ) {
        RailRowContent(identity, motion, layout, appearance, frame)
    }
}

@Composable
private fun rememberRailRowGeometry(
    identity: RailRowIdentity,
    layout: RailRowLayoutSpec,
): RailRowGeometry {
    val density = LocalDensity.current
    return remember(density, identity.position, layout.tabWidths, layout.contentStart, layout.labelWidth, layout.itemWidth) {
        with(density) {
            val padding = RailItemContentPadding.roundToPx()
            val labelStart = padding + RailIconColumnWidth.roundToPx()
            var slotStart = layout.contentStart.roundToPx()
            for (index in 0 until identity.position) slotStart += layout.tabWidths[index].roundToPx()
            val slotWidth = layout.tabWidths[identity.position].roundToPx()
            val labelPx = layout.labelWidth.roundToPx().coerceAtMost(slotWidth)
            val expandedLabelWidth = (layout.itemWidth - RailItemContentPadding * 2 - RailIconColumnWidth).roundToPx()
            RailRowGeometry(
                slotStart = slotStart.toFloat(),
                slotWidth = slotWidth.toFloat(),
                sourceStart = (slotStart + ((slotWidth - labelPx) / 2f).roundToInt() - labelStart).toFloat(),
                sourceWidth = (labelStart + labelPx + padding).toFloat(),
                expandedTop = (RailExpandedRowTop + RailRowPitch * identity.position).toPx(),
                labelMinWidth = if (layout.labelWidth.roundToPx() > slotWidth) minOf(labelPx, expandedLabelWidth) else expandedLabelWidth,
            )
        }
    }
}

/** Updates the same hit rectangle during layout as the upstream flying row. */
private fun Modifier.railRowPlacement(
    identity: RailRowIdentity,
    motion: RailRowMotion,
    spec: RailRowLayoutSpec,
    geometry: RailRowGeometry,
): Modifier = this.layout { measurable, constraints ->
    val fraction = motion.rowProgress().coerceIn(0f, 1f)
    val width = lerp(geometry.slotWidth, spec.itemWidth.toPx(), fraction).roundToInt().coerceAtLeast(0)
    val height = (RailCollapsedRowHeight + RailRowGrowth * fraction).roundToPx()
    val x = lerp(geometry.slotStart, RailItemHorizontalMargin.toPx(), fraction).roundToInt()
    val y = if (fraction == 1f) {
        geometry.expandedTop.roundToInt()
    } else {
        lerp(RailRowTop.toPx() + motion.scrollOffset(), geometry.expandedTop, fraction).roundToInt()
    }
    identity.slot.left = x.toFloat()
    identity.slot.top = y.toFloat()
    identity.slot.width = width.toFloat()
    identity.slot.height = height.toFloat()
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(x, y) }
}

@Composable
private fun Modifier.railRowInteraction(
    identity: RailRowIdentity,
    motion: RailRowMotion,
    pressAlpha: Float,
    liquidIndicator: Boolean,
): Modifier = this
    .clip(RoundedCornerShape(percent = 50))
    // The selected Liquid lens already tracks this exact interaction source.
    // Keeping the native row-wide indication would paint a second solid grey pill on press.
    .then(if (liquidIndicator) Modifier else Modifier.indication(identity.interactionSource, LocalIndication.current))
    .hoverable(identity.interactionSource, enabled = motion.expanded)
    .semantics(mergeDescendants = true) {
        role = Role.Tab
        this.selected = identity.selected
        (identity.item.contentDescription ?: identity.item.label)?.let { contentDescription = it }
        onClick {
            identity.onClick()
            true
        }
    }
    .onKeyEvent { event ->
        if (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Spacebar) {
            if (event.type == KeyEventType.KeyUp) identity.onClick()
            true
        } else {
            false
        }
    }
    .focusable(interactionSource = identity.interactionSource)
    .graphicsLayer { alpha = pressAlpha }

/** The label and badge fly as one unit; the press/keyboard owner remains the slot above. */
@Composable
private fun RailRowContent(
    identity: RailRowIdentity,
    motion: RailRowMotion,
    spec: RailRowLayoutSpec,
    appearance: RailRowAppearance,
    frame: RailRowFrame,
) {
    val liquidScale by animateFloatAsState(
        targetValue = if (appearance.liquidIndicator && motion.pressed) {
            LiquidFeedbackSpec.PressedItemScale
        } else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 250f),
        label = "liquidRailItemPress",
    )
    Row(
        modifier = Modifier
            .layout { measurable, constraints ->
                val fraction = motion.rowProgress().coerceIn(0f, 1f)
                val width = lerp(frame.geometry.sourceWidth, spec.itemWidth.toPx(), fraction).roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                val x = lerp(frame.geometry.sourceStart - frame.geometry.slotStart, 0f, fraction).roundToInt()
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(x, 0) }
            }
            .padding(horizontal = RailItemContentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(RailIconColumnWidth)
                .graphicsLayer { this.alpha = motion.iconAlpha() },
        ) {
            BadgedBox(badge = { identity.item.badge?.invoke() }) {
                Icon(
                    imageVector = identity.item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(RailIconSize).graphicsLayer {
                        scaleX = liquidScale
                        scaleY = liquidScale
                    },
                    tint = frame.tint,
                )
            }
        }
        if (identity.item.label != null) {
            Text(
                text = identity.item.label,
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(maxWidth = maxOf(constraints.maxWidth, frame.geometry.labelMinWidth)),
                    )
                    layout(placeable.width.coerceAtMost(constraints.maxWidth), placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                }.graphicsLayer {
                    scaleX = liquidScale
                    scaleY = liquidScale
                },
                color = frame.tint,
                style = appearance.labelStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The same indicator follows the active destination through the entire morph. */
internal class RailIndicatorSelection(
    val index: Int,
    val interaction: NavigationRailInteraction,
    val pressed: Boolean,
)

internal class RailIndicatorMotion(
    val progress: () -> Float,
    val scrollOffset: () -> Int,
    val expandedItemWidth: Dp,
    val rowPosition: () -> Float,
)

@Composable
internal fun NavigationRailIndicator(
    selection: RailIndicatorSelection,
    motion: RailIndicatorMotion,
    appearance: GlassIndicatorAppearance,
) {
    GlassNavigationIndicator(
        pressed = selection.pressed,
        height = RailCollapsedRowHeight,
        appearance = appearance,
        modifier = Modifier.layout { measurable, constraints ->
            val fraction = motion.progress().coerceIn(0f, 1f)
            val width: Int
            val x: Float
            val y: Float
            val expandedY = RailExpandedRowTop.toPx() + RailRowPitch.toPx() * motion.rowPosition()
            if (fraction == 1f) {
                // Collapsed springs and scroll compensation cannot affect the expanded indicator.
                width = motion.expandedItemWidth.roundToPx().coerceAtLeast(0)
                x = RailItemHorizontalMargin.toPx()
                y = expandedY
            } else {
                val bounds = navigationIndicatorBounds(
                    selection.interaction.left.value,
                    selection.interaction.right.value,
                    selection.interaction.width,
                    selection.interaction.contentStart,
                    selection.interaction.endPadding,
                )
                width = lerp(bounds.y - bounds.x, motion.expandedItemWidth.toPx(), fraction).roundToInt().coerceAtLeast(0)
                x = lerp(bounds.x, RailItemHorizontalMargin.toPx(), fraction)
                y = lerp(RailRowTop.toPx() + motion.scrollOffset(), expandedY, fraction)
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
