// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassEdgeStrokes
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.GlassMaterials
import top.yukonga.miuix.kmp.material.GlassShadow
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.material.GlassStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.ceil

/** Default values for [GlassNavigationRail]. */
object GlassNavigationRailDefaults {

    /** Panel width while expanded. */
    val ExpandedWidth: Dp = 250.dp

    /** Distance between the expanded panel and the window edges. */
    val Margin: Dp = 12.dp

    /** Height of the collapsed capsule. */
    val CollapsedHeight: Dp = GlassTopAppBarDefaults.ButtonSize

    /** Top offset of the collapsed capsule, centered on the collapsed top bar. */
    val CollapsedTop: Dp = (TopAppBarDefaults.CollapsedHeight - CollapsedHeight) / 2

    /** The default content description of the built-in toggle while the rail is collapsed. */
    val ExpandContentDescription: String = "Expand navigation rail"

    /** The default content description of the built-in toggle while the rail is expanded. */
    val CollapseContentDescription: String = "Collapse navigation rail"

    /** The panel's rim. */
    @Composable
    fun stroke(): EdgeStroke = GlassEdgeStrokes.forTheme(
        isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f,
    )

    /** The panel's body. */
    @Composable
    fun material(): GlassMaterial = GlassMaterials.puredThinGlass(
        isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f,
    )
}

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

/**
 * A floating sidebar on glass for wide layouts: a panel at the start edge that, collapsing, travels
 * to the top centre and becomes the navigation bar itself.
 *
 * Expanded, the rail lists destinations as icon and label. Collapsed, it shows labels only, with
 * the expand toggle in its leading slot. Its persistent gesture owner and indicator use the same
 * drag physics as [GlassNavigationBar], including presses made during the morph.
 *
 * Between those settled states every row flies from its place in the column into its slot in the
 * bar's row. The morph is read in layout and draw blocks, so its frames do not recompose the rows.
 *
 * The rail is an overlay: it fills the space it is given and draws the surface inside it. Place it
 * over the page, outside the recorded backdrop. Move the page clear of the panel with
 * [GlassNavigationRailState.progress], and hide a top bar's title while the capsule covers it.
 *
 * @param items The destinations, in order.
 * @param selectedIndex The index of the current destination.
 * @param onSelect Called with the destination the user selected.
 * @param backdrop The [Backdrop] supplying the content behind the glass. `null` fills the surface
 *   with the theme's surfaceContainer color instead of sampling.
 * @param modifier The modifier applied to the rail's overlay box.
 * @param state The rail's expanded state and morph progress.
 * @param showToggle Whether to show the built-in expand/collapse toggle. Without it, the caller
 *   drives [GlassNavigationRailState.expanded].
 * @param defaultWindowInsetsPadding Whether to keep the surface inside the system bars, the caption
 *   bar and the display cutout. Disable when the caller insets the rail itself.
 * @param collapsedTop Top of the collapsed capsule, relative to the safe area.
 * @param expandedWidth Panel width while expanded.
 * @param style The glass material style.
 * @param alpha Opacity multiplier for the material.
 * @param material The surface's own body when sampling a backdrop. `null` omits explicit material
 *   layers; it does not disable sampling. Ignored when [backdrop] is `null`.
 * @param stroke Optional bloom stroke along the rim.
 * @param shadow The shadow the floating surface casts. `null` removes it.
 * @param selectedColor Tint of the selected icon and label. Dynamic colour themes use primary;
 *   other themes use onSurface.
 * @param unselectedColor Tint of the unselected icons and labels.
 * @param indicatorColor Fill behind the selected destination.
 * @param indicatorPressedColor Fill behind the destination held down in the collapsed capsule.
 * @param labelStyle The destinations' label style. It also decides the width of a collapsed slot,
 *   so the capsule hugs its labels.
 * @param expandContentDescription The accessible description of the toggle while collapsed.
 * @param collapseContentDescription The accessible description of the toggle while expanded.
 * @param scrollState The [ScrollState] of the expanded destination column.
 */
@Composable
fun GlassNavigationRail(
    items: List<GlassNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    state: GlassNavigationRailState = rememberGlassNavigationRailState(),
    showToggle: Boolean = true,
    defaultWindowInsetsPadding: Boolean = true,
    collapsedTop: Dp = GlassNavigationRailDefaults.CollapsedTop,
    expandedWidth: Dp = GlassNavigationRailDefaults.ExpandedWidth,
    style: GlassStyle = GlassDefaults.Style,
    alpha: Float = 1f,
    material: GlassMaterial? = GlassNavigationRailDefaults.material(),
    stroke: EdgeStroke? = GlassNavigationRailDefaults.stroke(),
    shadow: GlassShadow? = GlassShadows.Float,
    selectedColor: Color = if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
    unselectedColor: Color = MiuixTheme.colorScheme.onSurface,
    indicatorColor: Color = GlassNavigationBarDefaults.indicatorColor(),
    indicatorPressedColor: Color = GlassNavigationBarDefaults.indicatorPressedColor(),
    labelStyle: TextStyle = MiuixTheme.textStyles.body1,
    expandContentDescription: String = GlassNavigationRailDefaults.ExpandContentDescription,
    collapseContentDescription: String = GlassNavigationRailDefaults.CollapseContentDescription,
    scrollState: ScrollState = rememberScrollState(),
) {
    if (items.isEmpty()) return
    val index = selectedIndex.coerceIn(0, items.lastIndex)
    val expanded = state.expanded
    val density = LocalDensity.current

    val textMeasurer = rememberTextMeasurer()
    // A collapsed slot is as wide as its own label plus the bar's indicator reach, so the capsule
    // hugs its text.
    val labelWidths = remember(items, labelStyle, density) {
        items.map { item ->
            item.label?.let { with(density) { ceil(textMeasurer.measure(it, labelStyle).size.width.toFloat()).toDp() } } ?: 0.dp
        }
    }
    val sourceWidths = remember(labelWidths) { labelWidths.map { it + RailSlotPadding } }

    // One clock for the surface's width, top and corner; each row and the panel's height and start
    // edge settle on their own springs, as in the reference motion.
    val rowProgress = remember(items.size) { List(items.size) { Animatable(if (expanded) 1f else 0f) } }
    val iconAlpha = remember { Animatable(if (expanded) 1f else 0f) }
    LaunchedEffect(expanded, rowProgress) {
        val target = if (expanded) 1f else 0f
        launch { state.morph.animateTo(target, RailMorphMotion) }
        launch { iconAlpha.animateTo(target, if (expanded) RailIconEnterMotion else RailIconExitMotion) }
        rowProgress.forEachIndexed { position, progress ->
            launch { progress.animateTo(target, railRowMotion(position, expanded)) }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (defaultWindowInsetsPadding) {
                    Modifier.windowInsetsPadding(
                        WindowInsets.systemBars
                            .union(WindowInsets.captionBar)
                            .union(WindowInsets.displayCutout),
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        val totalSlots = sourceWidths.fold(0.dp) { sum, width -> sum + width }
        // The bar lays out its first destination after its content padding and the toggle's slot.
        val leadingActionWidth = if (showToggle) RailLeadingActionWidth else 0.dp
        val contentStart = GlassNavigationBarDefaults.ContentPaddingHorizontal + leadingActionWidth
        val chrome = contentStart + RailCollapsedEndPadding
        // The capsule leaves room on both sides for a top bar's navigation icon and actions, and
        // compresses its slots when the labels need more than that.
        val neededWidth = chrome + totalSlots
        val capsuleWidth = neededWidth
            .coerceAtMost((viewportWidth - RailCollapsedSideReserve * 2).coerceAtLeast(RailMinCollapsedWidth))
            .coerceAtMost(viewportWidth)
        val widthRatio = if (totalSlots > 0.dp) {
            ((capsuleWidth - chrome) / totalSlots).coerceAtLeast(0f)
        } else {
            1f
        }
        val tabWidths = remember(sourceWidths, widthRatio) { sourceWidths.map { it * widthRatio } }
        val collapsedHeight = GlassNavigationRailDefaults.CollapsedHeight
        val collapsedStart = (viewportWidth - capsuleWidth) / 2
        val expandedStart = GlassNavigationRailDefaults.Margin
        val expandedTop = GlassNavigationRailDefaults.CollapsedTop
        val expandedHeight = (viewportHeight - expandedTop - GlassNavigationRailDefaults.Margin)
            .coerceAtLeast(collapsedHeight)

        val panelHeight = rememberRailMotion(
            target = if (expanded) expandedHeight else collapsedHeight,
            expanded = expanded,
        ) { expanding -> railHeightMotion(expanding, expandedHeight - collapsedHeight) }
        val panelStart = rememberRailMotion(
            target = if (expanded) expandedStart else collapsedStart,
            expanded = expanded,
        ) { expanding -> if (expanding) RailMorphDpMotion else RailReboundMotion }

        GlassNavigationRailPanel(
            context = RailPanelContext(
                items = items,
                selectedIndex = index,
                state = state,
                showToggle = showToggle,
                toggleContentDescription = if (expanded) collapseContentDescription else expandContentDescription,
                onSelect = onSelect,
                scrollState = scrollState,
            ),
            geometry = RailPanelGeometry(
                tabWidths = tabWidths,
                labelWidths = labelWidths,
                contentStart = contentStart,
                collapsedWidth = capsuleWidth,
                expandedWidth = expandedWidth,
                collapsedTop = collapsedTop,
                expandedTop = expandedTop,
            ),
            motion = RailPanelMotion(
                rowProgress = rowProgress,
                iconAlpha = { iconAlpha.value },
                panelStart = { panelStart.value },
                panelHeight = { panelHeight.value },
            ),
            surface = RailPanelSurface(
                backdrop = backdrop,
                style = style,
                alpha = alpha,
                material = material,
                stroke = stroke,
                shadow = shadow,
            ),
            colors = RailPanelColors(
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                indicatorColor = indicatorColor,
                indicatorPressedColor = indicatorPressedColor,
                labelStyle = labelStyle,
            ),
        )
    }
}


/** A corner that eases from the capsule's half-height to the panel's radius as [progress] rises. */
@Stable
internal class RailMorphCorner(private val progress: () -> Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = with(density) {
        lerp(RailCollapsedCornerRadius.toPx(), RailExpandedCornerRadius.toPx(), progress().coerceIn(0f, 1f))
    }
}

/**
 * Keeps a geometry value on [target]. Flipping [expanded] animates to it with [spec]; a target that
 * moves on its own, as a window resize does, is followed at once unless a flip is still settling.
 */
@Composable
private fun rememberRailMotion(
    target: Dp,
    expanded: Boolean,
    spec: (expanding: Boolean) -> AnimationSpec<Dp>,
): Animatable<Dp, AnimationVector1D> {
    val animatable = remember { Animatable(target, Dp.VectorConverter) }
    val currentTarget by rememberUpdatedState(target)
    val currentExpanded by rememberUpdatedState(expanded)
    val currentSpec by rememberUpdatedState(spec)
    LaunchedEffect(animatable) {
        var settledExpanded = currentExpanded
        var animating = false
        snapshotFlow { currentExpanded to currentTarget }.collectLatest { (expanding, value) ->
            if (expanding != settledExpanded || animating) {
                settledExpanded = expanding
                animating = true
                animatable.animateTo(value, currentSpec(expanding))
                animating = false
            } else {
                animatable.snapTo(value)
            }
        }
    }
    return animatable
}

/** One row's flight. Later rows respond slightly slower, so the column follows its leader. */
private fun railRowMotion(position: Int, expanded: Boolean): SpringSpec<Float> {
    val frequency = (if (expanded) 26.75f else 21.5f) / (1f + 0.03f * position)
    return spring(stiffness = frequency * frequency, dampingRatio = 0.97f, visibilityThreshold = 0.001f)
}

/** The panel's height settle: a short rebound when expanding, crisp when collapsing. */
private fun railHeightMotion(expanded: Boolean, distance: Dp): SpringSpec<Dp> {
    val (stiffness, dampingRatio) = if (expanded) 220f to 0.83f else 462.25f to 0.97f
    return spring(
        stiffness = stiffness,
        dampingRatio = dampingRatio,
        visibilityThreshold = (distance * 0.001f).coerceAtLeast(0.1.dp),
    )
}

internal val RailMorphEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
internal val RailMorphMotion = tween<Float>(durationMillis = 300, easing = RailMorphEasing)
internal val RailMorphDpMotion = tween<Dp>(durationMillis = 300, easing = RailMorphEasing)
internal val RailReboundMotion = spring<Dp>(stiffness = 200f, dampingRatio = 0.8f)
internal val RailIconEnterMotion = tween<Float>(durationMillis = 200, easing = RailMorphEasing)
internal val RailIconExitMotion = tween<Float>(durationMillis = 120, easing = RailMorphEasing)

internal val RailIconSize = 24.dp
internal val RailIconColumnWidth = RailIconSize + 12.dp
internal val RailItemContentPadding = 14.dp
internal val RailItemHorizontalMargin = 12.dp
internal val RailCollapsedRowHeight = GlassNavigationRailDefaults.CollapsedHeight -
    GlassNavigationBarDefaults.IndicatorPaddingVertical * 2
internal val RailRowGrowth = 4.dp
internal val RailExpandedRowHeight = RailCollapsedRowHeight + RailRowGrowth
internal val RailRowPitch = RailExpandedRowHeight + 4.dp
internal val RailRowTop = GlassNavigationBarDefaults.IndicatorPaddingVertical
internal val RailExpandedRowTop = 64.dp
internal val RailHeaderHeight = RailExpandedRowTop - (RailRowPitch - RailExpandedRowHeight)
internal val RailCollapsedCornerRadius = GlassNavigationRailDefaults.CollapsedHeight / 2
internal val RailExpandedCornerRadius = 20.dp

/** The toggle's box: the bar's leading slot while collapsed, the header corner while expanded. */
internal val RailToggleBoxSize = GlassTopAppBarDefaults.ButtonSize
internal val RailLeadingActionWidth = RailToggleBoxSize + 4.dp
internal val RailCollapsedToggleBoxStart = GlassNavigationBarDefaults.ContentPaddingHorizontal - 2.dp
internal val RailExpandedToggleStart = 16.dp
internal val RailExpandedToggleTop = 12.dp
internal val RailCollapsedEndPadding = 3.dp

/** A collapsed slot's padding around its label, the bar's indicator reach on each side. */
internal val RailSlotPadding = RailIconSize + GlassNavigationBarDefaults.IndicatorOverhang * 2
internal val RailCollapsedSideReserve = 81.dp
internal val RailMinCollapsedWidth = 192.dp
