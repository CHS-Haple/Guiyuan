// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified from upstream (2026): compact title, title padding, and unclipped action controls.

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Default values for [GlassTopAppBar] and [GlassIconButton]. */
object GlassTopAppBarDefaults {

    /** Start/end inset of the glass controls, aligned with the page's cards and tabs. */
    val HorizontalPadding: Dp = 12.dp

    /** Scroll threshold for the legacy shadow ramp. Material visibility is driven separately. */
    val RampStart: Dp = 0.dp

    /**
     * Scroll distance for the retained Compose shadow ramp. The mask and button material use timed
     * transitions independent of this distance.
     */
    val RampDistance: Dp = 32.dp

    /** How far the large title blurs out as it collapses. */
    val LargeTitleBlurRadius: Dp = 12.dp

    /** Diameter of the glass pill behind a bar button. */
    val ButtonSize: Dp = 44.dp

    /** Icon opacity while an action-bar button is held. */
    val PressedContentAlpha: Float = 0.6f

    private val NESTED_BLUR_LIGHT: Dp = 44.72.dp
    private val NESTED_BLUR_DARK: Dp = 63.25.dp

    /**
     * How far a bar has collapsed, in `[0, 1]`.
     *
     * @param scrollBehavior The behavior driving the bar. `null` reports a fully collapsed bar,
     *   which is what a bar over content that does not scroll wants.
     */
    @Composable
    fun collapseRamp(scrollBehavior: ScrollBehavior?): State<Float> {
        val density = LocalDensity.current
        val startPx = with(density) { RampStart.toPx() }
        val rampPx = with(density) { RampDistance.toPx() }
        return remember(scrollBehavior, startPx, rampPx) {
            if (scrollBehavior == null) {
                mutableFloatStateOf(1f)
            } else {
                derivedStateOf {
                    ((-scrollBehavior.state.contentOffset - startPx) / rampPx).coerceIn(0f, 1f)
                }
            }
        }
    }

    /** The traditional fill used when backdrop material is unavailable. */
    @Composable
    fun buttonFill(): Color = if (isDarkTheme()) Color(0xFF2C2C2C) else Color.White

    /** The `internal-pured-thin-glass` action-button material. */
    @Composable
    fun buttonMaterial(): GlassMaterial = GlassMaterials.puredThinGlass(isDarkTheme())

    /** Effective blur of the action bar's parent mask and a child's own 20dp mask blur. */
    @Composable
    internal fun nestedMaterialBlurRadius(): Dp = if (isDarkTheme()) NESTED_BLUR_DARK else NESTED_BLUR_LIGHT

    /** The parent material applied before child button and tab materials. */
    @Composable
    internal fun actionBarUnderlayMaterial(): GlassMaterial = GlassMaterials.actionBarMask(isDarkTheme())

    /** The small bloom stroke paired with [buttonMaterial]. */
    @Composable
    fun buttonStroke(): GlassStroke = GlassStrokes.forTheme(
        isDark = isDarkTheme(),
        light = GlassStrokes.SmallLight,
        dark = GlassStrokes.SmallDark,
    )

    /** The state overlay painted behind a held action-bar icon. */
    @Composable
    fun buttonPressedOverlay(): Color = if (isDarkTheme()) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    /**
     * The band that dims the content as it passes under the bar: the page's own colour, laid back
     * over it, solid at the top edge and gone by the bottom.
     *
     * @param color The page's background colour.
     */
    @Stable
    fun bandBrush(color: Color): Brush = Brush.verticalGradient(
        0.00f to color,
        0.45f to color,
        0.58f to color.copy(alpha = 0.82f),
        0.72f to color.copy(alpha = 0.58f),
        0.86f to color.copy(alpha = 0.34f),
        1.00f to color.copy(alpha = 0f),
    )

    /** How far the band reaches past the bottom of the bar. */
    val BandOverhang: Dp = 28.dp

    @Composable
    private fun isDarkTheme(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f
}

internal data class GlassTopAppBarContext(
    val backdrop: Backdrop?,
    val material: GlassMaterial,
    val underlayMaterial: GlassMaterial?,
    val floating: Boolean,
    val alpha: Float,
    val ramp: State<Float>,
    val materialProgress: State<Float>,
    val keepMaterial: Boolean,
    val style: GlassStyle,
    val size: Dp,
    val shape: GlassShape,
    val fill: Color,
    val stroke: GlassStroke?,
    val shadow: GlassShadow?,
)

internal val LocalGlassTopAppBarContext = staticCompositionLocalOf<GlassTopAppBarContext?> { null }

/**
 * A top bar the content scrolls *under*, deriving material visibility from [ScrollBehavior].
 * For an exact page-top signal, use the overload with `isContentScrolled` and pass the list's
 * `canScrollBackward` value. Both overloads retain the same public visual parameters.
 *
 * @param title The compact title, centred once the bar has collapsed.
 * @param backdrop The page behind the action bar. `null` uses [fill].
 * @param modifier The modifier applied to the bar.
 * @param largeTitle The title shown large before the content scrolls. Defaults to [title].
 * @param subtitle Optional line under the title.
 * @param largeTitleBlurRadius How far the large title blurs out as it collapses.
 * @param scrollBehavior The behavior driving the collapse. Without one the bar stays at full
 *   strength, which is what a bar over non-scrolling content wants.
 * @param bandBrush The band that dims the content passing under the bar.
 * @param bandOverhang How far the band reaches past the bottom of the bar.
 * @param style Compatibility style forwarded to the renderer. The default button body uses
 *   [GlassTopAppBarDefaults.buttonMaterial] with bionic shading disabled.
 * @param alpha Opacity multiplier for the button pills, on top of their floating transition.
 * @param buttonShape Silhouette of the button pills. Defaults to a circle of [buttonSize].
 * @param buttonSize Diameter of the button pills.
 * @param buttonShadow The shadow under the button pills. `null` removes it.
 * @param fill Traditional fill behind the button pills when runtime shaders are unavailable.
 * @param stroke Optional bloom stroke on the button pills. The default is the
 *   small action-button stroke.
 * @param defaultWindowInsetsPadding Whether to inset the bar for the status bar.
 * @param contentModifier Applied to the foreground bar, leaving its background band stationary.
 * @param titlePadding Native compact and large title padding, forwarded without geometry overrides.
 * @param titleAlpha Draw-phase opacity of the compact and large title containers.
 * @param navigationIcon The leading control. Use [GlassIconButton] for the same glass surface and
 *   press feedback as trailing controls; the slot adds no surface of its own.
 * @param actions Trailing controls. Wrap them in [GlassIconButton] to give them the same pill.
 * @param bottomContent Content pinned under the title, inside the bar. A tab row goes here.
 */
@Composable
fun GlassTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    largeTitle: String = title,
    subtitle: String = "",
    scrollBehavior: ScrollBehavior? = null,
    bandBrush: Brush = GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface),
    bandOverhang: Dp = GlassTopAppBarDefaults.BandOverhang,
    largeTitleBlurRadius: Dp = GlassTopAppBarDefaults.LargeTitleBlurRadius,
    style: GlassStyle = GlassDefaults.Style,
    alpha: Float = 1f,
    buttonSize: Dp = GlassTopAppBarDefaults.ButtonSize,
    buttonShape: GlassShape = GlassShape(buttonSize / 2),
    fill: Color = GlassTopAppBarDefaults.buttonFill(),
    stroke: GlassStroke? = GlassTopAppBarDefaults.buttonStroke(),
    buttonShadow: GlassShadow? = GlassShadows.Regular,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    titleAlpha: () -> Float = { 1f },
    compact: Boolean = false,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    unclippedNavigationIcon: Boolean = false,
    unclippedActions: Boolean = false,
) {
    val isContentScrolled by remember(scrollBehavior) {
        derivedStateOf { scrollBehavior?.state?.let { it.contentOffset < 0f } ?: true }
    }
    GlassTopAppBar(
        title = title,
        isContentScrolled = isContentScrolled,
        modifier = modifier,
        backdrop = backdrop,
        largeTitle = largeTitle,
        subtitle = subtitle,
        scrollBehavior = scrollBehavior,
        bandBrush = bandBrush,
        bandOverhang = bandOverhang,
        largeTitleBlurRadius = largeTitleBlurRadius,
        style = style,
        alpha = alpha,
        buttonSize = buttonSize,
        buttonShape = buttonShape,
        fill = fill,
        stroke = stroke,
        buttonShadow = buttonShadow,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        titlePadding = titlePadding,
        contentModifier = contentModifier,
        titleAlpha = titleAlpha,
        compact = compact,
        navigationIcon = navigationIcon,
        actions = actions,
        bottomContent = bottomContent,
        unclippedNavigationIcon = unclippedNavigationIcon,
        unclippedActions = unclippedActions,
    )
}

/**
 * A glass top bar with an explicit page-overlap state.
 *
 * @param title The compact title.
 * @param isContentScrolled Whether page content has left its resting position. For a lazy list,
 *   pass `listState.canScrollBackward`. This clears the material when the page returns to the top
 *   even if the large title stays collapsed. Other parameters behave as in the scroll-behaviour
 *   overload.
 */
@Composable
fun GlassTopAppBar(
    title: String,
    isContentScrolled: Boolean,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    largeTitle: String = title,
    subtitle: String = "",
    scrollBehavior: ScrollBehavior? = null,
    bandBrush: Brush = GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface),
    bandOverhang: Dp = GlassTopAppBarDefaults.BandOverhang,
    largeTitleBlurRadius: Dp = GlassTopAppBarDefaults.LargeTitleBlurRadius,
    style: GlassStyle = GlassDefaults.Style,
    alpha: Float = 1f,
    buttonSize: Dp = GlassTopAppBarDefaults.ButtonSize,
    buttonShape: GlassShape = GlassShape(buttonSize / 2),
    fill: Color = GlassTopAppBarDefaults.buttonFill(),
    stroke: GlassStroke? = GlassTopAppBarDefaults.buttonStroke(),
    buttonShadow: GlassShadow? = GlassShadows.Regular,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    titleAlpha: () -> Float = { 1f },
    compact: Boolean = false,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    unclippedNavigationIcon: Boolean = false,
    unclippedActions: Boolean = false,
) {
    val ramp = GlassTopAppBarDefaults.collapseRamp(scrollBehavior)
    val floating = isContentScrolled
    val transition = updateTransition(floating, label = "glassTopBarFloat")
    val maskProgress = transition.animateFloat(
        transitionSpec = { GlassMotion.topBarMask() },
        label = "glassTopBarMask",
    ) { if (it) 1f else 0f }
    val materialProgress = transition.animateFloat(
        transitionSpec = { GlassMotion.topBarButtonFloat() },
        label = "glassTopBarMaterial",
    ) { if (it) 1f else 0f }
    val keepMaterial by remember(floating, materialProgress) {
        derivedStateOf { floating || materialProgress.value > 0f }
    }
    val baseMaterial = GlassTopAppBarDefaults.buttonMaterial()
    val material = if (keepMaterial && backdrop != null) {
        baseMaterial.copy(blurRadius = GlassTopAppBarDefaults.nestedMaterialBlurRadius())
    } else {
        baseMaterial
    }
    val underlayMaterial = GlassTopAppBarDefaults.actionBarUnderlayMaterial().takeIf { keepMaterial && backdrop != null }
    val buttonBackdrop = backdrop?.takeIf { isRuntimeShaderSupported() }

    var barOrigin by remember { mutableStateOf(Offset.Zero) }
    var navigationBounds by remember { mutableStateOf<Rect?>(null) }
    var actionBounds by remember { mutableStateOf<Rect?>(null) }
    var navigationSize by remember { mutableStateOf(IntSize.Zero) }
    var actionSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val navigationWidth = if (navigationSize.width > 0) with(density) { navigationSize.width.toDp() } else buttonSize
    val navigationHeight = if (navigationSize.height > 0) with(density) { navigationSize.height.toDp() } else buttonSize
    val actionWidth = if (actionSize.width > 0) with(density) { actionSize.width.toDp() } else buttonSize
    val actionHeight = if (actionSize.height > 0) with(density) { actionSize.height.toDp() } else buttonSize
    // The accepted OS4 native title clips only title layers, but compact mode
    // still delegates to SmallTopAppBar, which clips protruding glass controls.
    val useUnclippedSlots = compact || !NativeTitleLocalClip
    val overlayNavigation = unclippedNavigationIcon && useUnclippedSlots
    val overlayActions = unclippedActions && useUnclippedSlots

    Box(modifier = modifier.onGloballyPositioned { barOrigin = it.positionInRoot() }) {
        val overhangPx = with(LocalDensity.current) { bandOverhang.toPx() }
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    this.alpha = maskProgress.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    scaleY = if (size.height > 0f) (size.height + overhangPx) / size.height else 1f
                }
                .background(bandBrush),
        )
        CompositionLocalProvider(
            LocalGlassTopAppBarContext provides GlassTopAppBarContext(
                backdrop = buttonBackdrop,
                material = material,
                underlayMaterial = underlayMaterial,
                floating = floating,
                alpha = alpha,
                ramp = ramp,
                materialProgress = materialProgress,
                keepMaterial = keepMaterial,
                style = style,
                size = buttonSize,
                shape = buttonShape,
                fill = fill,
                stroke = stroke,
                shadow = buttonShadow,
            ),
        ) {
            // The published/native API keeps its legacy clip; PR423 provides native title blur.
            UiNativeTitleBar(
                title = title,
                modifier = contentModifier,
                largeTitle = largeTitle,
                largeTitleBlurRadius = largeTitleBlurRadius,
                titleAlpha = titleAlpha,
                compact = compact,
                subtitle = subtitle,
                scrollBehavior = scrollBehavior,
                defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                titlePadding = titlePadding,
                navigationIconPadding = GlassTopAppBarDefaults.HorizontalPadding,
                actionIconPadding = GlassTopAppBarDefaults.HorizontalPadding,
                navigationIcon = {
                    if (overlayNavigation) {
                        Box(Modifier.width(navigationWidth).height(navigationHeight).onGloballyPositioned { navigationBounds = it.boundsInRoot() })
                    } else {
                        navigationIcon()
                    }
                },
                actions = {
                    if (overlayActions) {
                        Box(Modifier.width(actionWidth).height(actionHeight).onGloballyPositioned { actionBounds = it.boundsInRoot() })
                    } else {
                        actions()
                    }
                },
                bottomContent = bottomContent,
            )
            // Published TopAppBar clips its whole layout. Retain native slot measurement,
            // but draw each interactive glass button once above that outdated clip.
            navigationBounds?.takeIf { overlayNavigation }?.let { bounds ->
                Box(Modifier.onSizeChanged { navigationSize = it }.absoluteOffset {
                    IntOffset(
                        (bounds.left - barOrigin.x).roundToInt(),
                        (bounds.top - barOrigin.y).roundToInt(),
                    )
                }) {
                    navigationIcon()
                }
            }
            actionBounds?.takeIf { overlayActions }?.let { bounds ->
                Row(Modifier.onSizeChanged { actionSize = it }.absoluteOffset {
                    IntOffset(
                        (bounds.left - barOrigin.x).roundToInt(),
                        (bounds.top - barOrigin.y).roundToInt(),
                    )
                }) {
                    actions()
                }
            }
        }
    }
}
