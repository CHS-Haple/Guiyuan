// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults

import top.yukonga.miuix.kmp.glass.GlassMotion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BadgeDefaults
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.ceil
import androidx.compose.ui.util.fastCoerceIn

/**
 * State of a [GlassNavigationRail].
 *
 * @param initialExpanded Whether the rail starts expanded.
 */
@Stable
class GlassNavigationRailState(initialExpanded: Boolean) {

    /** Whether the rail is the expanded panel. Setting it starts the morph. */
    var expanded: Boolean by mutableStateOf(initialExpanded)

    internal val morph = Animatable(if (initialExpanded) 1f else 0f)

    /**
     * The morph's position, from `0` (the collapsed capsule) to `1` (the expanded panel). It
     * changes every frame of the morph, so read it in a layout or draw block, for example to move
     * the page clear of the panel without recomposing it.
     */
    val progress: Float get() = morph.value

    companion object {
        /** The default [Saver], keeping [expanded]. */
        val Saver: Saver<GlassNavigationRailState, Boolean> = Saver(
            save = { it.expanded },
            restore = { GlassNavigationRailState(it) },
        )
    }
}

/**
 * Creates and remembers a [GlassNavigationRailState], kept across configuration changes.
 *
 * @param initialExpanded Whether the rail starts expanded.
 */
@Composable
fun rememberGlassNavigationRailState(initialExpanded: Boolean = true): GlassNavigationRailState = rememberSaveable(saver = GlassNavigationRailState.Saver) {
    GlassNavigationRailState(initialExpanded)
}

/** Existing top-level pager owns selection; this is only the visual rail's adapter. */
internal class GlassRailDestinations(
    val items: List<GlassNavigationItem>,
    val selectedIndex: Int,
    val onSelect: (Int) -> Unit,
)

/** Preserves one native morph and one set of rows during the whole adaptive-window session. */
private class RailWindowRequest(
    val destinations: GlassRailDestinations,
    val state: GlassNavigationRailState,
    val backdrop: Backdrop?,
    val showToggle: Boolean,
    val materialOverride: RailLiquidPaint?,
    val trailingAction: (@Composable () -> Unit)?,
)

private class RailSourceWidths(val labels: List<Dp>, val slots: List<Dp>)

private class RailAnimationState(
    val rows: List<Animatable<Float, AnimationVector1D>>,
    val iconAlpha: Animatable<Float, AnimationVector1D>,
    val liquid: RailLiquidMotion,
)

/**
 * Upstream rail material and animation, with a smaller app-local API: only options actually used
 * by Guiyuan are exposed. Other appearance and inset values follow the pinned MIUIX defaults.
 */
@Composable
internal fun GlassNavigationRail(
    destinations: GlassRailDestinations,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    state: GlassNavigationRailState = rememberGlassNavigationRailState(),
    showToggle: Boolean = true,
    materialOverride: RailLiquidPaint? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val items = destinations.items
    if (items.isEmpty()) return
    val expanded = state.expanded
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MiuixTheme.textStyles.body1
    val labelWidths = remember(items, labelStyle, density) {
        items.map { item ->
            item.label?.let {
                with(density) {
                    ceil(textMeasurer.measure(it, labelStyle).size.width.toFloat()).toDp()
                }
            } ?: 0.dp
        }
    }
    val sourceWidths = remember(items, labelWidths) {
        labelWidths.mapIndexed { index, width ->
            width + RailSlotPadding +
                if (items[index].badge != null) BadgeDefaults.LargeSize * 2 else 0.dp
        }
    }
    val rowProgress = remember(items.size) { List(items.size) { Animatable(if (expanded) 1f else 0f) } }
    val iconAlpha = remember { Animatable(if (expanded) 1f else 0f) }
    // One press animation drives both the Liquid panel scale and the independent search position.
    val liquid = remember { RailLiquidMotion(Animatable(0f), RailLiquidLensLink()) }
    LaunchedEffect(expanded, rowProgress) {
        val target = if (expanded) 1f else 0f
        launch { state.morph.animateTo(target, RailMorphMotion) }
        launch { iconAlpha.animateTo(target, if (expanded) RailIconEnterMotion else RailIconExitMotion) }
        rowProgress.forEachIndexed { position, progress ->
            launch { progress.animateTo(target, railRowMotion(position, expanded)) }
        }
    }
    RailNavigationWindow(
        request = RailWindowRequest(destinations, state, backdrop, showToggle, materialOverride, trailingAction),
        widths = RailSourceWidths(labelWidths, sourceWidths),
        animation = RailAnimationState(rowProgress, iconAlpha, liquid),
        modifier = modifier,
    )
}

@Composable
private fun RailNavigationWindow(
    request: RailWindowRequest,
    widths: RailSourceWidths,
    animation: RailAnimationState,
    modifier: Modifier,
) {
    val systemBars = WindowInsets.systemBars
    val captionBar = WindowInsets.captionBar
    val displayCutout = WindowInsets.displayCutout
    val windowInsets = remember(systemBars, captionBar, displayCutout) {
        systemBars.union(captionBar).union(displayCutout)
    }
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().windowInsetsPadding(windowInsets),
    ) {
        val actionProgress = animateFloatAsState(
            targetValue = if (request.trailingAction != null) 1f else 0f,
            animationSpec = GlassMotion.navShowHide(),
            label = "railSearchActionVisibility",
        )
        val placement = rememberRailWindowPlacement(request, widths, animation, actionProgress)
        RailWindowPanel(request = request, placement = placement)
        RailWindowTrailingAction(
            action = request.trailingAction,
            state = request.state,
            placement = placement,
            liquid = request.materialOverride != null,
            actionProgress = actionProgress,
        )
    }
}


/** Upstream capsule width motion runs even when Liquid draws its own surface. */
@Composable
private fun rememberCollapsedRailWidth(capsuleWidth: Dp, liquid: Boolean): Dp {
    val animated by animateDpAsState(
        capsuleWidth, RailMorphDpMotion, label = "railCapsuleWidth",
    )
    return if (liquid) capsuleWidth else animated
}

/** Native Glass owns the panel rebound; Liquid continues using the existing morph clock. */
@Composable
private fun rememberNativeRailPanelStart(
    expanded: Boolean,
    collapsedStart: Dp,
    liquid: Boolean,
): Animatable<Dp, AnimationVector1D>? =
    if (liquid) null else rememberRailMotion(
        target = if (expanded) GlassNavigationRailDefaults.Margin else collapsedStart,
        expanded = expanded,
        targetChangeSpec = RailMorphDpMotion,
    ) { expanding -> if (expanding) RailMorphDpMotion else RailReboundMotion }

/** Resolve one measured rail placement without changing row, rail or search action ownership. */
@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.rememberRailWindowPlacement(
    request: RailWindowRequest,
    widths: RailSourceWidths,
    animation: RailAnimationState,
    actionProgress: State<Float>,
): RailPlacement {
    val expanded = request.state.expanded
    val capsule = railCapsuleGeometry(maxWidth, widths.slots, request.showToggle)
    val tabWidths = remember(widths.slots, capsule.widthRatio) {
        widths.slots.map { it * capsule.widthRatio }
    }
    val collapsedHeight = GlassNavigationRailDefaults.CollapsedHeight
    // The published MIUIX Glass capsule animates changed label/window widths;
    // Liquid continues using the single native state.morph for its own surface.
    val currentCollapsedWidth = rememberCollapsedRailWidth(capsule.width, request.materialOverride != null)
    val collapsedStart = (maxWidth - capsule.width) / 2
    val density = LocalDensity.current
    val expandedTop = GlassNavigationRailDefaults.CollapsedTop
    val expandedHeight = (maxHeight - expandedTop - GlassNavigationRailDefaults.Margin)
        .coerceAtLeast(collapsedHeight)
    val panelHeight = rememberRailMotion(
        target = if (expanded) expandedHeight else collapsedHeight,
        expanded = expanded,
    ) { expanding -> railHeightMotion(expanding, expandedHeight - collapsedHeight) }
    // Liquid's surface width is already driven by state.progress. Moving its
    // whole capsule on a separate rebound spring makes a collapsing panel
    // shrink at the left before it arrives at the centred bar destination.
    // Keep the upstream Glass spring; Liquid shares the single morph clock.
    val nativePanelStart = rememberNativeRailPanelStart(expanded, collapsedStart, request.materialOverride != null)
    val iconAlphaProvider = remember(animation.iconAlpha) { { animation.iconAlpha.value } }
    val placement = RailPlacement(
            geometry = RailPanelGeometry(
                tabWidths = tabWidths,
                labelWidths = widths.labels,
                contentStart = capsule.contentStart,
                endpoints = RailPanelEndpoints(currentCollapsedWidth, GlassNavigationRailDefaults.ExpandedWidth,
                    GlassNavigationRailDefaults.CollapsedTop, expandedTop),
            ),
            motion = RailPanelMotion(animation.rows, iconAlphaProvider,
                {
                    val start = nativePanelStart?.value ?: railLiquidPanelStart(
                        collapsedStart,
                        GlassNavigationRailDefaults.Margin,
                        request.state.progress,
                    )
                    start + with(density) {
                        railCollapsedVisualCenterOffset(
                            progress = request.state.progress,
                            actionFraction = actionProgress.value,
                            collapsedWidth = currentCollapsedWidth,
                            panelHeight = panelHeight.value,
                            liquid = animation.liquid.takeIf { request.materialOverride != null },
                        )
                    }
                }, { panelHeight.value }, animation.liquid),
    )

    return placement
}

/** This action is not a rail destination and never owns the rail's pointer recognizer. */
@Composable
private fun RailWindowTrailingAction(
    action: (@Composable () -> Unit)?,
    state: GlassNavigationRailState,
    placement: RailPlacement,
    liquid: Boolean,
    actionProgress: State<Float>,
) {
    // Keep the exiting content mounted until the shared native show/hide clock finishes.
    val presentedAction = rememberNavigationTrailingAction(action, actionProgress)
    if (presentedAction != null) {
        val actionSize = GlassNavigationRailDefaults.CollapsedHeight
        val density = LocalDensity.current
        val shadowOutsets = remember(density) {
            with(density) { popupShadowOutsets(GlassShadows.Regular) }
        }
        Box(
            modifier = Modifier
                .size(actionSize)
                .offset {
                    with(density) {
                        railTrailingActionOffset(state.progress.fastCoerceIn(0f, 1f), placement, actionSize, liquid)
                    }
                }
                .focusProperties { canFocus = action != null }
                .graphicsLayer {
                    outsets = shadowOutsets
                    val fraction = actionProgress.value.fastCoerceIn(0f, 1f)
                    alpha = fraction
                    scaleX = 0.85f + fraction * 0.15f
                    scaleY = 0.85f + fraction * 0.15f
                }
                .then(
                    if (action == null) {
                        Modifier.blockVisualOnlyInput()
                    } else Modifier,
                ),
        ) { presentedAction() }
    }
}

@Composable
private fun RailWindowPanel(
    request: RailWindowRequest,
    placement: RailPlacement,
) {
    val colorScheme = MiuixTheme.colorScheme
    GlassNavigationRailPanel(
        context = RailPanelContext(
            selection = RailPanelSelection(
                request.destinations.items,
                request.destinations.selectedIndex.coerceIn(0, request.destinations.items.lastIndex),
                request.destinations.onSelect,
            ),
            state = request.state,
            showToggle = request.showToggle,
            toggleContentDescription = if (request.state.expanded) {
                GlassNavigationRailDefaults.CollapseContentDescription
            } else {
                GlassNavigationRailDefaults.ExpandContentDescription
            },
            scrollState = rememberScrollState(),
        ),
        geometry = placement.geometry,
        motion = placement.motion,
        surface = RailPanelSurface(
            backdrop = request.backdrop,
            style = GlassDefaults.Style,
            alpha = 1f,
            material = RailPanelMaterial(
                glass = GlassNavigationRailDefaults.material(),
                replacement = request.materialOverride,
            ),
            stroke = GlassNavigationRailDefaults.stroke(),
            shadow = GlassShadows.Float,
        ),
        colors = RailPanelColors(
            // Liquid shares the portrait navigation's selected primary tint while
            // normal Glass keeps its original dynamic/non-dynamic color contract.
            selectedColor = if (request.materialOverride != null || MiuixTheme.isDynamicColor) {
                colorScheme.primary
            } else colorScheme.onSurface,
            unselectedColor = colorScheme.onSurface,
            indicator = GlassIndicatorAppearance(
                color = GlassNavigationBarDefaults.indicatorColor(),
                pressedColor = GlassNavigationBarDefaults.indicatorPressedColor(),
                liquidPaint = request.materialOverride?.let { paint ->
                    { pressed -> paint.indicator(request.backdrop, pressed) }
                },
            ),
            labelStyle = MiuixTheme.textStyles.body1,
        ),
    )
}
