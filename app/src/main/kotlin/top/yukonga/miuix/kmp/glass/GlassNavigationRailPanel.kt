// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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

/** A single glass surface body and its physical appearance. */
internal class RailPanelSurface(
    val backdrop: Backdrop?,
    val style: GlassStyle,
    val alpha: Float,
    val material: GlassMaterial?,
    val stroke: EdgeStroke?,
    val shadow: GlassShadow?,
)

/** Colors and typography shared between rail rows and their indicator. */
internal class RailPanelColors(
    val selectedColor: Color,
    val unselectedColor: Color,
    val indicatorColor: Color,
    val indicatorPressedColor: Color,
    val labelStyle: TextStyle,
)

/** One permanent surface, gesture owner, set of rows and toggle for both ends of the morph. */
@Composable
internal fun GlassNavigationRailPanel(
    context: RailPanelContext,
    geometry: RailPanelGeometry,
    motion: RailPanelMotion,
    surface: RailPanelSurface,
    colors: RailPanelColors,
) {
    val items = context.selection.items
    val selectedIndex = context.selection.selectedIndex
    val state = context.state
    val showToggle = context.showToggle
    val toggleContentDescription = context.toggleContentDescription
    val onSelect = context.selection.onSelect
    val scrollState = context.scrollState
    val tabWidths = geometry.tabWidths
    val labelWidths = geometry.labelWidths
    val contentStart = geometry.contentStart
    val collapsedWidth = geometry.endpoints.collapsedWidth
    val expandedWidth = geometry.endpoints.expandedWidth
    val collapsedTop = geometry.endpoints.collapsedTop
    val expandedTop = geometry.endpoints.expandedTop
    val rowProgress = motion.rowProgress
    val iconAlpha = motion.iconAlpha
    val panelStart = motion.panelStart
    val panelHeight = motion.panelHeight
    val backdrop = surface.backdrop
    val style = surface.style
    val alpha = surface.alpha
    val material = surface.material
    val stroke = surface.stroke
    val shadow = surface.shadow
    val selectedColor = colors.selectedColor
    val unselectedColor = colors.unselectedColor
    val indicatorColor = colors.indicatorColor
    val indicatorPressedColor = colors.indicatorPressedColor
    val labelStyle = colors.labelStyle

    val expanded = state.expanded
    // The corner eases from the capsule's half-height to the panel's radius on the morph's clock,
    // read when the outline is built rather than in composition.
    val shape = remember(state) {
        val corner = RailMorphCorner { state.progress }
        GlassShape(corner, corner, corner, corner)
    }
    val itemWidth = expandedWidth - RailItemHorizontalMargin * 2
    val contentHeight = RailExpandedRowTop + RailRowPitch * items.lastIndex + RailExpandedRowHeight +
        GlassNavigationRailDefaults.Margin
    // The column only scrolls once it has settled into the panel; while rows fly they keep their
    // own geometry.
    val scrollEnabled by remember(state, rowProgress) {
        derivedStateOf { state.expanded && state.morph.value == 1f && rowProgress.all { it.value == 1f } }
    }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val widths = remember(tabWidths, density) { with(density) { tabWidths.map { it.roundToPx().toFloat() } } }
    val startPx = with(density) { contentStart.roundToPx().toFloat() }
    val endPx = with(density) { RailCollapsedEndPadding.toPx() }
    val headerHeightPx = with(density) { RailHeaderHeight.toPx() }
    val interaction = remember(widths, startPx, endPx, scope) {
        NavigationRailInteraction(widths, startPx, endPx, selectedIndex, scope)
    }
    val currentOnSelect = rememberUpdatedState(onSelect)
    val currentExpanded = rememberUpdatedState(expanded)
    val currentHeaderBottom = rememberUpdatedState { scrollState.value + headerHeightPx * state.progress }
    val pressedIndex = interaction.pressedIndex
    val pressed = pressedIndex >= 0
    val indicatorIndex = if (pressed) pressedIndex else selectedIndex
    LaunchedEffect(interaction, selectedIndex, pressed, interaction.releaseGeneration) {
        if (!pressed) interaction.settle(selectedIndex)
    }

    Box(
        Modifier
            .layout { measurable, _ ->
                val fraction = state.progress.coerceIn(0f, 1f)
                val width = lerp(collapsedWidth.toPx(), expandedWidth.toPx(), fraction).roundToInt()
                val height = panelHeight().roundToPx().coerceAtLeast(0)
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(width, height) { placeable.place(0, 0) }
            }
            .offset {
                IntOffset(
                    x = panelStart().roundToPx(),
                    y = lerp(collapsedTop.toPx(), expandedTop.toPx(), state.progress).roundToInt(),
                )
            }
            .glassPanel(
                backdrop = backdrop,
                shape = shape,
                style = style,
                alpha = alpha,
                material = material,
                stroke = stroke,
                shadow = shadow,
                shading = false,
                fallback = Modifier
                    .clip(shape)
                    .background(MiuixTheme.colorScheme.surfaceContainer),
            )
            .clip(shape),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // Rows scrolled up pass under the toggle's header, not over it.
                .drawWithContent {
                    clipRect(top = RailHeaderHeight.toPx() * state.progress) {
                        this@drawWithContent.drawContent()
                    }
                }
                .verticalScroll(scrollState, enabled = scrollEnabled),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(contentHeight)
                    .selectableGroup()
                    .navigationRailGestures(
                        interaction = interaction,
                        layoutDirection = layoutDirection,
                        expanded = currentExpanded,
                        headerBottom = currentHeaderBottom,
                        onSelect = currentOnSelect,
                    ),
            ) {
                NavigationRailIndicator(
                    selection = RailIndicatorSelection(indicatorIndex, interaction, pressed),
                    motion = RailIndicatorMotion(
                        progress = { rowProgress[indicatorIndex].value },
                        scrollOffset = { scrollState.value },
                        expandedItemWidth = itemWidth,
                    ),
                    appearance = GlassIndicatorAppearance(indicatorColor, indicatorPressedColor),
                )
                items.forEachIndexed { position, item ->
                    key(position, item) {
                        NavigationRailRow(
                            identity = RailRowIdentity(
                                item = item,
                                selected = position == selectedIndex,
                                position = position,
                                slot = interaction.slots[position],
                                interactionSource = interaction.rowInteractions[position],
                                onClick = { onSelect(position) },
                            ),
                            motion = RailRowMotion(
                                expanded = expanded,
                                pressed = pressedIndex == position,
                                rowProgress = { rowProgress[position].value },
                                iconAlpha = iconAlpha,
                                scrollOffset = { scrollState.value },
                            ),
                            layout = RailRowLayoutSpec(
                                tabWidths = tabWidths,
                                contentStart = contentStart,
                                labelWidth = labelWidths[position],
                                itemWidth = itemWidth,
                            ),
                            appearance = RailRowAppearance(
                                selectedColor = selectedColor,
                                unselectedColor = unselectedColor,
                                labelStyle = labelStyle,
                            ),
                        )
                    }
                }
            }
        }
        if (showToggle) {
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
                    tint = unselectedColor,
                    contentDescription = toggleContentDescription,
                )
            }
        }
    }
}
