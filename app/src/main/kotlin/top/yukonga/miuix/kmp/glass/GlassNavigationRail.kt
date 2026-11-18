// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
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
import top.yukonga.miuix.kmp.basic.BadgeDefaults
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassEdgeStrokes
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.GlassMaterials
import top.yukonga.miuix.kmp.material.GlassShadow
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.material.GlassStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.chaners.guiyuan.ui.components.liquid.LiquidFeedbackSpec
import com.chaners.guiyuan.ui.components.liquid.liquidPanelPressScale
import com.chaners.guiyuan.ui.components.liquid.liquidActionPressReachPx
import kotlin.math.ceil
import kotlin.math.roundToInt

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
    val liquidPress: Animatable<Float, AnimationVector1D>,
)

private class RailPlacement(
    val geometry: RailPanelGeometry,
    val motion: RailPanelMotion,
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
    val liquidPress = remember { Animatable(0f) }
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
        animation = RailAnimationState(rowProgress, iconAlpha, liquidPress),
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
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().windowInsetsPadding(
            WindowInsets.systemBars.union(WindowInsets.captionBar).union(WindowInsets.displayCutout),
        ),
    ) {
        val expanded = request.state.expanded
        val capsule = railCapsuleGeometry(maxWidth, widths.slots, request.showToggle)
        val tabWidths = remember(widths.slots, capsule.widthRatio) {
            widths.slots.map { it * capsule.widthRatio }
        }
        val collapsedHeight = GlassNavigationRailDefaults.CollapsedHeight
        // The published MIUIX Glass capsule animates changed label/window widths;
        // Liquid continues using the single native state.morph for its own surface.
        val nativeAnimatedWidth by animateDpAsState(
            capsule.width, RailMorphDpMotion, label = "railCapsuleWidth",
        )
        val currentCollapsedWidth = if (request.materialOverride == null) {
            nativeAnimatedWidth
        } else capsule.width
        val collapsedStart = (maxWidth - capsule.width) / 2
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
        val nativePanelStart = if (request.materialOverride == null) {
            rememberRailMotion(
                target = if (expanded) GlassNavigationRailDefaults.Margin else collapsedStart,
                expanded = expanded,
                targetChangeSpec = RailMorphDpMotion,
            ) { expanding -> if (expanding) RailMorphDpMotion else RailReboundMotion }
        } else null
        RailWindowPanel(
            request = request,
            placement = RailPlacement(
                geometry = RailPanelGeometry(
                    tabWidths = tabWidths,
                    labelWidths = widths.labels,
                    contentStart = capsule.contentStart,
                    endpoints = RailPanelEndpoints(currentCollapsedWidth, GlassNavigationRailDefaults.ExpandedWidth,
                        GlassNavigationRailDefaults.CollapsedTop, expandedTop),
                ),
                motion = RailPanelMotion(animation.rows, { animation.iconAlpha.value },
                    {
                        nativePanelStart?.value ?: railLiquidPanelStart(
                            collapsedStart,
                            GlassNavigationRailDefaults.Margin,
                            request.state.progress,
                        )
                    }, { panelHeight.value }, animation.liquidPress),
            ),
        )
        RailWindowTrailingAction(
            action = request.trailingAction,
            state = request.state,
            capsuleWidth = currentCollapsedWidth,
            liquid = request.materialOverride != null,
            panelHeight = { panelHeight.value },
            liquidPress = animation.liquidPress,
            panelStart = {
                nativePanelStart?.value ?: railLiquidPanelStart(
                    collapsedStart, GlassNavigationRailDefaults.Margin, request.state.progress,
                )
            },
        )
    }
}

/** This action is not a rail destination and never owns the rail's pointer recognizer. */
@Composable
private fun RailWindowTrailingAction(
    action: (@Composable () -> Unit)?,
    state: GlassNavigationRailState,
    capsuleWidth: Dp,
    liquid: Boolean,
    panelHeight: () -> Dp,
    liquidPress: Animatable<Float, AnimationVector1D>,
    panelStart: () -> Dp,
) {
    // An independent search action shares the rail's safe-area coordinates and
    // follows the same native panel morph; no new page or gesture writer.
    val actionProgress = animateFloatAsState(
        targetValue = if (action != null) 1f else 0f,
        animationSpec = GlassMotion.navShowHide(),
        label = "railSearchActionVisibility",
    )
    val retainedAction = remember { mutableStateOf(action) }
    SideEffect {
        if (action != null || actionProgress.value == 0f) {
            retainedAction.value = action
        }
    }
    val presentedAction = action ?:
        retainedAction.value.takeIf { actionProgress.value > 0f }
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
                    val progress = state.progress.coerceIn(0f, 1f)
                    val panelWidth = capsuleWidth +
                        (GlassNavigationRailDefaults.ExpandedWidth - capsuleWidth) * progress
                    val currentPanelStart = panelStart()
                    // The original Liquid rail grows about its centre on press.
                    // Keep the independent search button beside that *transformed*
                    // panel, not beside the idle geometry; the button itself is not scaled.
                    val x = (currentPanelStart + railSearchActionSideOffset(
                        panelWidth, progress, actionSize,
                    )).toPx()
                    val y = (GlassNavigationRailDefaults.CollapsedTop +
                        RailExpandedToggleTop * progress).toPx()
                    val panelWidthPx = panelWidth.toPx()
                    val panelHeightPx = panelHeight().toPx()
                    val actionSizePx = actionSize.toPx()
                    // Soft Glass keeps the upstream panel/action position untouched.
                    // Liquid alone preplans its extra lens reach on the original press.
                    val press = if (liquid) liquidPress.value.coerceIn(0f, 1f) else 0f
                    val scale = if (liquid) {
                        liquidPanelPressScale(
                            panelWidthPx, panelHeightPx, LiquidFeedbackSpec.PressedPanelGrowth.toPx(), press,
                        )
                    } else 1f
                    val lensReach = if (liquid) liquidActionPressReachPx(
                        LiquidFeedbackSpec.PressedPanelGrowth.toPx(),
                        LiquidFeedbackSpec.PressedLensGrowth.toPx(),
                        RailCollapsedEndPadding.toPx(),
                    ) else 0f
                    IntOffset(
                        x = (x + railSearchActionPlannedShift(
                            x + actionSizePx / 2f,
                            currentPanelStart.toPx() + panelWidthPx / 2f,
                            scale, press, 1f - progress, lensReach,
                        )).roundToInt(),
                        y = (y + railSearchActionPressShift(
                            y + actionSizePx / 2f,
                            GlassNavigationRailDefaults.CollapsedTop.toPx() + panelHeightPx / 2f,
                            scale,
                        )).roundToInt(),
                    )
                }
                .focusProperties { canFocus = action != null }
                .graphicsLayer {
                    outsets = shadowOutsets
                    val fraction = actionProgress.value.coerceIn(0f, 1f)
                    alpha = fraction
                    scaleX = 0.85f + fraction * 0.15f
                    scaleY = 0.85f + fraction * 0.15f
                }
                .then(
                    if (action == null) {
                        Modifier.clearAndSetSemantics { }.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                        .changes.forEach { it.consume() }
                                }
                            }
                        }
                    } else Modifier,
                ),
        ) { presentedAction() }
    }
}

/** A point follows the panel's existing centre-origin scale; no independent press animation. */
internal fun railSearchActionPressShift(actionCenter: Float, panelCenter: Float, scale: Float): Float =
    (actionCenter - panelCenter) * (scale - 1f)

/** Reserve the rightmost Liquid lens during the same press clock regardless of dragged tab. */
internal fun railSearchActionPlannedShift(
    actionCenter: Float,
    panelCenter: Float,
    scale: Float,
    press: Float,
    collapsedFraction: Float,
    maxLensReach: Float,
): Float {
    val panelShift = railSearchActionPressShift(actionCenter, panelCenter, scale)
    val extra = (maxLensReach * press.coerceIn(0f, 1f) - panelShift).coerceAtLeast(0f)
    return panelShift + extra * collapsedFraction.coerceIn(0f, 1f)
}

/** Logical end of the capsule when collapsed; right-side header slot when expanded. */
internal fun railSearchActionSideOffset(panelWidth: Dp, progress: Float, actionSize: Dp): Dp {
    val fraction = progress.coerceIn(0f, 1f)
    return panelWidth + GlassNavigationBarDefaults.ActionSpacing * (1f - fraction) -
        (RailExpandedToggleStart + actionSize) * fraction
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

/** Deterministic capsule width and slot compression; does not own motion or touch state. */
internal class RailCapsuleGeometry(
    val contentStart: Dp,
    val width: Dp,
    val widthRatio: Float,
)

internal fun railCapsuleGeometry(
    viewportWidth: Dp,
    sourceWidths: List<Dp>,
    showToggle: Boolean,
): RailCapsuleGeometry {
    val totalSlots = sourceWidths.fold(0.dp) { sum, width -> sum + width }
    val leadingActionWidth = if (showToggle) RailLeadingActionWidth else 0.dp
    val contentStart = GlassNavigationBarDefaults.ContentPaddingHorizontal + leadingActionWidth
    val chrome = contentStart + RailCollapsedEndPadding
    // Preserve upstream's side reserve and compression for narrow but nonzero windows.
    val neededWidth = chrome + totalSlots
    val capsuleWidth = neededWidth
        .coerceAtMost((viewportWidth - RailCollapsedSideReserve * 2).coerceAtLeast(RailMinCollapsedWidth))
        .coerceAtMost(viewportWidth)
    val widthRatio = if (totalSlots > 0.dp) {
        ((capsuleWidth - chrome) / totalSlots).coerceAtLeast(0f)
    } else {
        1f
    }
    return RailCapsuleGeometry(contentStart, capsuleWidth, widthRatio)
}

/** The Liquid panel's physical X follows its width's morph clock in both directions. */
internal fun railLiquidPanelStart(collapsed: Dp, expanded: Dp, progress: Float): Dp =
    collapsed + (expanded - collapsed) * progress.coerceIn(0f, 1f)

/** A corner that eases from the capsule's half-height to the panel's radius as [progress] rises. */
@Stable
internal class RailMorphCorner(private val progress: () -> Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = with(density) {
        lerp(RailCollapsedCornerRadius.toPx(), RailExpandedCornerRadius.toPx(), progress().coerceIn(0f, 1f))
    }
}

/**
 * Keeps one geometry value on [target]; the native Glass rail may animate window/label
 * target changes, matching upstream. The Liquid rail still owns its single morph clock.
 */
@Composable
private fun rememberRailMotion(
    target: Dp,
    expanded: Boolean,
    targetChangeSpec: AnimationSpec<Dp>? = null,
    spec: (expanding: Boolean) -> AnimationSpec<Dp>,
): Animatable<Dp, AnimationVector1D> {
    val animatable = remember { Animatable(target, Dp.VectorConverter) }
    val currentTarget by rememberUpdatedState(target)
    val currentExpanded by rememberUpdatedState(expanded)
    val currentSpec by rememberUpdatedState(spec)
    val currentTargetChangeSpec by rememberUpdatedState(targetChangeSpec)
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
                val targetSpec = currentTargetChangeSpec
                if (targetSpec != null) animatable.animateTo(value, targetSpec)
                else animatable.snapTo(value)
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
internal val RailItemContentPadding = 12.dp
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
