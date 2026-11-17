// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.key
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.GlassShadow
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/** Single selected-page owner and the interaction contract, independent of surface animation. */
internal class RailPanelSelection(
    val items: List<GlassNavigationItem>,
    val selectedIndex: Int,
    val onSelect: (Int) -> Unit,
)

internal class RailPanelContext(
    val selection: RailPanelSelection,
    val state: GlassNavigationRailState,
    val showToggle: Boolean,
    val toggleContentDescription: String,
    val scrollState: ScrollState,
)

/** The panel's measured endpoints. */
internal class RailPanelEndpoints(
    val collapsedWidth: Dp,
    val expandedWidth: Dp,
    val collapsedTop: Dp,
    val expandedTop: Dp,
)

internal class RailPanelGeometry(
    val tabWidths: List<Dp>,
    val labelWidths: List<Dp>,
    val contentStart: Dp,
    val endpoints: RailPanelEndpoints,
)

/** Values read during layout and drawing rather than recomposing the rows each frame. */
internal class RailPanelMotion(
    val rowProgress: List<Animatable<Float, AnimationVector1D>>,
    val iconAlpha: () -> Float,
    val panelStart: () -> Dp,
    val panelHeight: () -> Dp,
)

/** The native body and optional Liquid substitute are one material choice. */
internal class RailLiquidPaint(
    val panel: @Composable (Backdrop?, GlassShape) -> Modifier,
    val indicator: @Composable (Boolean) -> Modifier,
)

internal class RailPanelMaterial(
    val glass: GlassMaterial?,
    val replacement: RailLiquidPaint?,
)

/** A single glass surface body and its physical appearance. */
internal class RailPanelSurface(
    val backdrop: Backdrop?,
    val style: GlassStyle,
    val alpha: Float,
    val material: RailPanelMaterial,
    val stroke: EdgeStroke?,
    val shadow: GlassShadow?,
)

/** Colors and typography shared between rail rows and their indicator. */
internal class RailPanelColors(
    val selectedColor: Color,
    val unselectedColor: Color,
    val indicator: GlassIndicatorAppearance,
    val labelStyle: TextStyle,
)

/** One interaction/scroll owner retained throughout the rail morph. */
private class RailPanelPointer(
    val interaction: NavigationRailInteraction,
    val expanded: androidx.compose.runtime.State<Boolean>,
    val headerBottom: androidx.compose.runtime.State<() -> Float>,
    val onSelect: androidx.compose.runtime.State<(Int) -> Unit>,
    val scrollEnabled: Boolean,
)

/** One permanent surface; layout and gesture children are composables with distinct ownership. */
@Composable
internal fun GlassNavigationRailPanel(
    context: RailPanelContext,
    geometry: RailPanelGeometry,
    motion: RailPanelMotion,
    surface: RailPanelSurface,
    colors: RailPanelColors,
) {
    val state = context.state
    val shape = remember(state) {
        val corner = RailMorphCorner { state.progress }
        GlassShape(corner, corner, corner, corner)
    }
    Box(
        modifier = Modifier.railPanelSurfaceModifier(state, geometry, motion, surface, shape),
    ) {
        RailPanelContents(context, geometry, motion, colors)
        if (context.showToggle) RailPanelToggle(context, colors)
    }
}

/** Keep the material/shadow in the same layout and draw phase as upstream. */
@Composable
private fun Modifier.railPanelSurfaceModifier(
    state: GlassNavigationRailState,
    geometry: RailPanelGeometry,
    motion: RailPanelMotion,
    surface: RailPanelSurface,
    shape: GlassShape,
): Modifier {
    // Do not replace the MIUIX rail's layout, morph, input or lifecycle owner.
    // Only the surface paint differs; the default Glass material path is unchanged.
    val override = surface.material.replacement?.panel
    val surfacePaint = if (override != null) {
        // Retain the upstream rail's exact silhouette and Float shadow with Liquid optics.
        Modifier.glassShadow(shape, surface.shadow).then(override(surface.backdrop, shape))
    } else {
        Modifier.glassPanel(
            backdrop = surface.backdrop,
            shape = shape,
            style = surface.style,
            alpha = surface.alpha,
            material = surface.material.glass,
            stroke = surface.stroke,
            shadow = surface.shadow,
            shading = false,
            fallback = Modifier.clip(shape).background(MiuixTheme.colorScheme.surfaceContainer),
        )
    }
    return this
        .layout { measurable, _ ->
            val fraction = state.progress.coerceIn(0f, 1f)
            val width = lerp(geometry.endpoints.collapsedWidth.toPx(), geometry.endpoints.expandedWidth.toPx(), fraction).roundToInt()
            val height = motion.panelHeight().roundToPx().coerceAtLeast(0)
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(width, height) { placeable.place(0, 0) }
        }
        .offset {
            IntOffset(
                x = motion.panelStart().roundToPx(),
                y = lerp(geometry.endpoints.collapsedTop.toPx(), geometry.endpoints.expandedTop.toPx(), state.progress).roundToInt(),
            )
        }
        .then(surfacePaint)
        .clip(shape)
}

@Composable
private fun RailPanelContents(
    context: RailPanelContext,
    geometry: RailPanelGeometry,
    motion: RailPanelMotion,
    colors: RailPanelColors,
) {
    val state = context.state
    val rowProgress = motion.rowProgress
    val scrollEnabled by remember(state, rowProgress) {
        derivedStateOf { state.expanded && state.morph.value == 1f && rowProgress.all { it.value == 1f } }
    }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val widths = remember(geometry.tabWidths, density) {
        with(density) { geometry.tabWidths.map { it.roundToPx().toFloat() } }
    }
    val startPx = with(density) { geometry.contentStart.roundToPx().toFloat() }
    val endPx = with(density) { RailCollapsedEndPadding.toPx() }
    val headerHeightPx = with(density) { RailHeaderHeight.toPx() }
    val interaction = remember(widths, startPx, endPx, scope) {
        NavigationRailInteraction(widths, startPx, endPx, context.selection.selectedIndex, scope)
    }
    val pointer = RailPanelPointer(
        interaction = interaction,
        expanded = rememberUpdatedState(state.expanded),
        headerBottom = rememberUpdatedState { context.scrollState.value + headerHeightPx * state.progress },
        onSelect = rememberUpdatedState(context.selection.onSelect),
        scrollEnabled = scrollEnabled,
    )
    val pressed = interaction.pressedIndex >= 0
    LaunchedEffect(interaction, context.selection.selectedIndex, pressed, interaction.releaseGeneration) {
        if (!pressed) interaction.settle(context.selection.selectedIndex)
    }
    RailPanelScroll(context, geometry, motion, colors, pointer)
}

@Composable
private fun RailPanelScroll(
    context: RailPanelContext,
    geometry: RailPanelGeometry,
    motion: RailPanelMotion,
    colors: RailPanelColors,
    pointer: RailPanelPointer,
) {
    val state = context.state
    val contentHeight = RailExpandedRowTop + RailRowPitch * context.selection.items.lastIndex +
        RailExpandedRowHeight + GlassNavigationRailDefaults.Margin
    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                clipRect(top = RailHeaderHeight.toPx() * state.progress) {
                    this@drawWithContent.drawContent()
                }
            }
            .verticalScroll(context.scrollState, enabled = pointer.scrollEnabled),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(contentHeight)
                .selectableGroup()
                .navigationRailGestures(
                    interaction = pointer.interaction,
                    layoutDirection = LocalLayoutDirection.current,
                    expanded = pointer.expanded,
                    headerBottom = pointer.headerBottom,
                    onSelect = pointer.onSelect,
                ),
        ) {
            RailPanelRows(context, geometry, motion, colors, pointer)
        }
    }
}

/** Visual destinations and indicator share the one press/selection owner above. */
@Composable
private fun RailPanelRows(
    context: RailPanelContext,
    geometry: RailPanelGeometry,
    motion: RailPanelMotion,
    colors: RailPanelColors,
    pointer: RailPanelPointer,
) {
    val interaction = pointer.interaction
    val pressedIndex = interaction.pressedIndex
    val selectedIndex = context.selection.selectedIndex
    // The upstream rail already owns PressInteraction. Observe it rather than installing a
    // second gesture recognizer; only Liquid previews the pressed expanded destination.
    val rowPresses = interaction.rowInteractions.map { it.collectIsPressedAsState() }
    val expandedPress = if (colors.indicator.liquidPaint != null) {
        rowPresses.indexOfFirst { it.value }
    } else -1
    val indicatorIndex = when {
        pressedIndex >= 0 -> pressedIndex
        expandedPress >= 0 -> expandedPress
        else -> selectedIndex
    }
    val indicatorPressed = pressedIndex >= 0 || expandedPress >= 0
    val liquidPosition by animateFloatAsState(
        targetValue = indicatorIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
        label = "liquidRailIndicatorRow",
    )
    val itemWidth = geometry.endpoints.expandedWidth - RailItemHorizontalMargin * 2
    NavigationRailIndicator(
        selection = RailIndicatorSelection(indicatorIndex, interaction, indicatorPressed),
        motion = RailIndicatorMotion(
            progress = { motion.rowProgress[indicatorIndex].value },
            scrollOffset = { context.scrollState.value },
            expandedItemWidth = itemWidth,
            rowPosition = {
                if (colors.indicator.liquidPaint != null) liquidPosition else indicatorIndex.toFloat()
            },
        ),
        appearance = colors.indicator,
    )
    context.selection.items.forEachIndexed { position, item ->
        key(position, item) {
            NavigationRailRow(
                identity = RailRowIdentity(
                    item = item,
                    selected = position == selectedIndex,
                    position = position,
                    slot = interaction.slots[position],
                    interactionSource = interaction.rowInteractions[position],
                    onClick = { context.selection.onSelect(position) },
                ),
                motion = RailRowMotion(
                    expanded = context.state.expanded,
                    pressed = pressedIndex == position || (expandedPress == position),
                    rowProgress = { motion.rowProgress[position].value },
                    iconAlpha = motion.iconAlpha,
                    scrollOffset = { context.scrollState.value },
                ),
                layout = RailRowLayoutSpec(
                    tabWidths = geometry.tabWidths,
                    contentStart = geometry.contentStart,
                    labelWidth = geometry.labelWidths[position],
                    itemWidth = itemWidth,
                ),
                appearance = RailRowAppearance(
                    selectedColor = colors.selectedColor,
                    unselectedColor = colors.unselectedColor,
                    labelStyle = colors.labelStyle,
                    liquidIndicator = colors.indicator.liquidPaint != null,
                ),
            )
        }
    }
}

@Composable
private fun RailPanelToggle(
    context: RailPanelContext,
    colors: RailPanelColors,
) {
    val state = context.state
    val expanded = state.expanded
    Box(
        modifier = Modifier
            .offset {
                val fraction = state.progress
                IntOffset(
                    x = lerp(RailCollapsedToggleBoxStart.toPx(), RailExpandedToggleStart.toPx(), fraction).roundToInt(),
                    y = lerp(0f, RailExpandedToggleTop.toPx(), fraction).roundToInt(),
                )
            }
            .size(RailToggleBoxSize),
        contentAlignment = Alignment.Center,
    ) {
        RailToggle(
            onClick = { state.expanded = !expanded },
            expanded = expanded,
            size = if (expanded) RailToggleBoxSize else RailCollapsedRowHeight,
            tint = colors.unselectedColor,
            contentDescription = context.toggleContentDescription,
        )
    }
}
