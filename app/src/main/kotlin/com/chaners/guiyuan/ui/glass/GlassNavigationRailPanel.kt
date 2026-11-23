// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationItem


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import com.chaners.guiyuan.ui.components.liquid.liquidRailVisibleLensOverhangPx
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.key
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.chaners.guiyuan.ui.components.liquid.rememberCombinedBackdrop
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.GlassShadow
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassStyle
import androidx.compose.ui.util.fastRoundToInt

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

/** Borrow the rendered lens's current edge and original X spring without duplicating motion. */
internal class RailLiquidLensLink {
    internal class Source(val scaleX: State<Float>, val rightEdge: State<(() -> Float)?>)
    private var source: Source? by mutableStateOf(null)

    fun overhangPx(panelWidthPx: Float, panelScale: Float, lensGrowthPx: Float): Float {
        val current = source ?: return 0f
        val right = current.rightEdge.value?.invoke() ?: return 0f
        return liquidRailVisibleLensOverhangPx(
            panelWidthPx, panelScale, right, current.scaleX.value, lensGrowthPx,
        )
    }

    internal fun attach(value: Source) { source = value }
    internal fun detach(value: Source) { if (source === value) source = null }
}

internal class RailLiquidMotion(
    val press: Animatable<Float, AnimationVector1D>,
    val lensEdge: RailLiquidLensLink,
)

/** Values read during layout and drawing rather than recomposing the rows each frame. */
internal class RailPanelMotion(
    val rowProgress: List<Animatable<Float, AnimationVector1D>>,
    val iconAlpha: () -> Float,
    val panelStart: () -> Dp,
    val panelHeight: () -> Dp,
    val liquid: RailLiquidMotion,
)

/** The native body and optional Liquid substitute are one material choice. */
internal class RailLiquidPaint(
    val panel: @Composable (Backdrop?, GlassShape) -> Modifier,
    val indicator: @Composable (Backdrop?, Boolean) -> Modifier,
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
    val liquidPaint = surface.material.replacement
    // One native interaction state serves both the panel's press expansion and
    // the selected lens. Do not create a second pointer or feedback writer.
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val widths = remember(geometry.tabWidths, density) {
        with(density) { geometry.tabWidths.map { it.roundToPx().toFloat() } }
    }
    val startPx = with(density) { geometry.contentStart.roundToPx().toFloat() }
    val endPx = with(density) { RailCollapsedEndPadding.toPx() }
    val interaction = remember(widths.size, startPx, endPx, scope) {
        NavigationRailInteraction(widths, startPx, endPx, context.selection.selectedIndex, scope)
    }
    val selectedIndex = context.selection.selectedIndex
    val pressed = interaction.pressedIndex >= 0
    LaunchedEffect(interaction, widths, selectedIndex, pressed, interaction.releaseGeneration) {
        interaction.updateWidths(widths)
        if (!pressed) interaction.settle(selectedIndex)
    }
    // The panel is the sole writer of this press animation. The sibling search
    // action reads its value to match the scaled panel's physical edge.
    LaunchedEffect(motion.liquid.press, liquidPaint != null, pressed) {
        motion.liquid.press.animateTo(
            if (liquidPaint != null && pressed) 1f else 0f,
            spring(dampingRatio = 1f, stiffness = 1000f),
        )
    }
    // Record only the physical Liquid panel, never the selected lens itself.
    // The selected indicator samples page + recorded panel, as in portrait LiquidNavBar.
    val materialBackdrop = if (liquidPaint != null && surface.backdrop != null) {
        rememberLayerBackdrop()
    } else null
    val selectedBackdrop = if (materialBackdrop != null && surface.backdrop != null) {
        rememberCombinedBackdrop(surface.backdrop, materialBackdrop)
    } else null
    val paintColors = if (selectedBackdrop != null && liquidPaint != null) {
        RailPanelColors(
            selectedColor = colors.selectedColor,
            unselectedColor = colors.unselectedColor,
            indicator = GlassIndicatorAppearance(
                color = colors.indicator.color,
                pressedColor = colors.indicator.pressedColor,
                scaleOnPress = colors.indicator.scaleOnPress,
                liquidPaint = { pressed -> liquidPaint.indicator(selectedBackdrop, pressed) },
            ),
            labelStyle = colors.labelStyle,
        )
    } else colors
    Box(
        modifier = Modifier.railPanelSurfaceModifier(state, geometry, motion, surface, shape),
    ) {
        if (materialBackdrop != null && liquidPaint != null) {
            // Draw once, visibly, and record this same surface for selected optics.
            // Keep it separate from rows and indicator: no self-referential capture.
            Box(
                Modifier.matchParentSize()
                    .layerBackdrop(materialBackdrop)
                    .then(liquidPaint.panel(surface.backdrop, shape)),
            )
        }
        if (liquidPaint != null) {
            // Only the pressed lens may exceed the idle panel silhouette.
            // Keep native rows, the scroll viewport and toggle shape-clipped.
            RailLiquidIndicator(context, geometry, motion, paintColors, interaction)
            Box(Modifier.matchParentSize().clip(shape)) {
                RailPanelContents(context, geometry, motion, paintColors, interaction)
                if (context.showToggle) RailPanelToggle(context, paintColors)
            }
        } else {
            RailPanelContents(context, geometry, motion, paintColors, interaction)
            if (context.showToggle) RailPanelToggle(context, paintColors)
        }
    }
}

@Composable
private fun RailPanelContents(
    context: RailPanelContext,
    geometry: RailPanelGeometry,
    motion: RailPanelMotion,
    colors: RailPanelColors,
    interaction: NavigationRailInteraction,
) {
    val state = context.state
    val rowProgress = motion.rowProgress
    val scrollEnabled by remember(state, rowProgress) {
        derivedStateOf { state.expanded && state.morph.value == 1f && rowProgress.all { it.value == 1f } }
    }
    val headerHeightPx = with(LocalDensity.current) { RailHeaderHeight.toPx() }
    val pointer = RailPanelPointer(
        interaction = interaction,
        expanded = rememberUpdatedState(state.expanded),
        headerBottom = rememberUpdatedState { context.scrollState.value + headerHeightPx * state.progress },
        onSelect = rememberUpdatedState(context.selection.onSelect),
        scrollEnabled = scrollEnabled,
    )
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
    // A scrollable, overflowing rail yields vertical drag to its parent ScrollState.
    // Keep this as a read-only State: no second gesture/scroll writer and no pointer restart.
    val verticalScrollAvailable = rememberUpdatedState(pointer.scrollEnabled && context.scrollState.maxValue > 0)
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
                    liquid = colors.indicator.liquidPaint != null,
                    verticalScrollAvailable = verticalScrollAvailable,
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
    val itemWidth = geometry.endpoints.expandedWidth - RailItemHorizontalMargin * 2
    // Keep native Glass exactly on the upstream press ownership path:
    // expanded row PressInteraction drives only the row's indication,
    // NOT the selected lens or its press scaling. Liquid owns its lens separately.
    if (colors.indicator.liquidPaint == null) {
        val indicatorIndex = if (pressedIndex >= 0) pressedIndex else selectedIndex
        NavigationRailIndicator(
            selection = RailIndicatorSelection(indicatorIndex, interaction, pressedIndex >= 0),
            motion = RailIndicatorMotion(
                progress = { motion.rowProgress[indicatorIndex].value },
                scrollOffset = { context.scrollState.value },
                expandedItemWidth = geometry.endpoints.expandedWidth - RailItemHorizontalMargin * 2,
                rowPosition = { indicatorIndex.toFloat() },
            ),
            appearance = colors.indicator,
        )
    }
    val onSelectState = rememberUpdatedState(context.selection.onSelect)
    val rowProgress = motion.rowProgress
    val scrollState = context.scrollState
    val scrollOffset = remember(scrollState) { { scrollState.value } }
    context.selection.items.forEachIndexed { position, item ->
        key(position, item) {
            val rowProgressProvider = remember(rowProgress, position) { { rowProgress[position].value } }
            val onRowClick = remember(onSelectState, position) { { onSelectState.value(position) } }
            NavigationRailRow(
                identity = RailRowIdentity(
                    item = item,
                    selected = position == selectedIndex,
                    position = position,
                    slot = interaction.slots[position],
                    interactionSource = interaction.rowInteractions[position],
                    onClick = onRowClick,
                ),
                motion = RailRowMotion(
                    expanded = context.state.expanded,
                    pressed = pressedIndex == position,
                    rowProgress = rowProgressProvider,
                    iconAlpha = motion.iconAlpha,
                    scrollOffset = scrollOffset,
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
                    x = lerp(RailCollapsedToggleBoxStart.toPx(), RailExpandedToggleStart.toPx(), fraction).fastRoundToInt(),
                    y = lerp(0f, RailExpandedToggleTop.toPx(), fraction).fastRoundToInt(),
                )
            }
            .size(RailToggleBoxSize),
        contentAlignment = Alignment.Center,
    ) {
        RailToggle(
            onClick = { state.expanded = !expanded },
            expanded = expanded,
            size = if (expanded) RailToggleBoxSize else RailCollapsedRowHeight,
            iconSize = RailIconSize,
            tint = colors.unselectedColor,
            contentDescription = context.toggleContentDescription,
        )
    }
}
