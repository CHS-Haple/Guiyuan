// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified from upstream (2026): compact title, title padding, and unclipped action controls.

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.RectangleShape
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
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
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

    /** Maximum radius of the upstream progressive background band. */
    val ProgressiveBlurRadius: Dp = 80.dp

    /** Scroll distance for the upstream progressive band ramp. */
    val ProgressiveBlurRampDistance: Dp = 48.dp

    /** Icon opacity while an action-bar button is held. */
    val PressedContentAlpha: Float = 0.6f

    /**
     * Visual scroll progress in `[0, 1]`, including elastic displacement.
     * [visualContentOffset] overrides the behavior's visual offset; without either source it is fully active.
     */
    @Composable
    fun collapseRamp(
        scrollBehavior: ScrollBehavior?,
        visualContentOffset: (() -> Float)? = null,
    ): State<Float> {
        val density = LocalDensity.current
        val startPx = with(density) { RampStart.toPx() }
        val rampPx = with(density) { RampDistance.toPx() }
        val currentOffset by rememberUpdatedState(visualContentOffset)
        return remember(scrollBehavior, startPx, rampPx) {
            derivedStateOf {
                val offset = currentOffset?.invoke() ?: scrollBehavior?.state?.visualContentOffset
                if (offset == null) 1f else ((-offset - startPx) / rampPx).coerceIn(0f, 1f)
            }
        }
    }

    /** The traditional fill used when backdrop material is unavailable. */
    @Composable
    fun buttonFill(): Color = if (isDarkTheme()) Color(0xFF2C2C2C) else Color.White

    /** The `internal-pured-thin-glass` action-button material. */
    @Composable
    fun buttonMaterial(): GlassMaterial = GlassMaterials.puredThinGlass(isDarkTheme())

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
    fun bandBrush(color: Color): Brush = TopAppBarBandBrush(color)

    /** How far the band reaches past the bottom of the bar. */
    val BandOverhang: Dp = 48.dp

    @Composable
    private fun isDarkTheme(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f
}

/** Starts fading inside the measured bar so its trailing edge does not form an opaque shelf. */
private data class TopAppBarBandBrush(val color: Color) : ShaderBrush() {
    private val colors = opacities.map { color.copy(alpha = color.alpha * it) }

    override fun createShader(size: Size): Shader = createShader(0f, size.height)

    fun createShader(fadeStart: Float, fadeHeight: Float): Shader = LinearGradientShader(
        from = Offset(0f, fadeStart),
        to = Offset(0f, fadeStart + fadeHeight.coerceAtLeast(1f)),
        colors = colors,
        colorStops = stops,
    )

    private companion object {
        val stops = List(17) { it / 16f }
        val opacities = stops.map { 1f - it * it * (3f - 2f * it) }
    }
}

internal data class GlassTopAppBarContext(
    val backdrop: Backdrop?,
    val buttonBackdrop: Backdrop?,
    val material: GlassMaterial,
    val underlayMaterial: GlassMaterial?,
    val buttonUnderlayMaterial: GlassMaterial?,
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
 * @param visualContentOffset Visual offset in pixels, including elastic displacement.
 *   Affects band and shadow without modifying native title collapse state.
 * @param bandBrush The band that dims the content passing under the bar.
 * @param bandOverhang How far the band reaches past the bottom of the bar.
 * @param progressiveBlur Render the upstream progressive blurred band behind the controls.
 * @param material Button material over the resolved band and its underlay.
 * @param backgroundAlpha Draw-phase alpha multiplier for the band.
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
    progressiveBlur: Boolean = false,
    backdrop: Backdrop? = null,
    largeTitle: String = title,
    subtitle: String = "",
    scrollBehavior: ScrollBehavior? = null,
    visualContentOffset: (() -> Float)? = null,
    bandBrush: Brush = GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface),
    bandOverhang: Dp = GlassTopAppBarDefaults.BandOverhang,
    largeTitleBlurRadius: Dp = GlassTopAppBarDefaults.LargeTitleBlurRadius,
    style: GlassStyle = GlassDefaults.Style,
    material: GlassMaterial = GlassTopAppBarDefaults.buttonMaterial(),
    alpha: Float = 1f,
    buttonSize: Dp = GlassTopAppBarDefaults.ButtonSize,
    buttonShape: GlassShape = GlassShape(buttonSize / 2),
    fill: Color = GlassTopAppBarDefaults.buttonFill(),
    stroke: GlassStroke? = GlassTopAppBarDefaults.buttonStroke(),
    buttonShadow: GlassShadow? = GlassShadows.Regular,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    titleAlpha: () -> Float = { 1f },
    backgroundAlpha: () -> Float = { 1f },
    compact: Boolean = false,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    unclippedNavigationIcon: Boolean = false,
    unclippedActions: Boolean = false,
) {
    val currentOffset by rememberUpdatedState(visualContentOffset)
    val isContentScrolled by remember(scrollBehavior) {
        derivedStateOf {
            val offset = scrollBehavior?.state?.contentOffset ?: currentOffset?.invoke()
            offset?.let { it < 0f } ?: true
        }
    }
    GlassTopAppBar(
        title = title,
        isContentScrolled = isContentScrolled,
        modifier = modifier,
        progressiveBlur = progressiveBlur,
        backdrop = backdrop,
        largeTitle = largeTitle,
        subtitle = subtitle,
        scrollBehavior = scrollBehavior,
        visualContentOffset = visualContentOffset,
        bandBrush = bandBrush,
        bandOverhang = bandOverhang,
        largeTitleBlurRadius = largeTitleBlurRadius,
        style = style,
        material = material,
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
        backgroundAlpha = backgroundAlpha,
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
 * @param isContentScrolled Actual content overlap, such as `listState.canScrollBackward`.
 * @param visualContentOffset Visual content offset including elastic displacement.
 */
@Composable
fun GlassTopAppBar(
    title: String,
    isContentScrolled: Boolean,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    progressiveBlur: Boolean = false,
    backdrop: Backdrop? = null,
    largeTitle: String = title,
    subtitle: String = "",
    scrollBehavior: ScrollBehavior? = null,
    visualContentOffset: (() -> Float)? = null,
    bandBrush: Brush = GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface),
    bandOverhang: Dp = GlassTopAppBarDefaults.BandOverhang,
    largeTitleBlurRadius: Dp = GlassTopAppBarDefaults.LargeTitleBlurRadius,
    style: GlassStyle = GlassDefaults.Style,
    material: GlassMaterial = GlassTopAppBarDefaults.buttonMaterial(),
    alpha: Float = 1f,
    buttonSize: Dp = GlassTopAppBarDefaults.ButtonSize,
    buttonShape: GlassShape = GlassShape(buttonSize / 2),
    fill: Color = GlassTopAppBarDefaults.buttonFill(),
    stroke: GlassStroke? = GlassTopAppBarDefaults.buttonStroke(),
    buttonShadow: GlassShadow? = GlassShadows.Regular,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    titleAlpha: () -> Float = { 1f },
    backgroundAlpha: () -> Float = { 1f },
    compact: Boolean = false,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    unclippedNavigationIcon: Boolean = false,
    unclippedActions: Boolean = false,
) {
    val ramp = GlassTopAppBarDefaults.collapseRamp(scrollBehavior, visualContentOffset)
    val transition = updateTransition(isContentScrolled, label = "glassTopBarFloat")
    val maskProgress = transition.animateFloat(
        transitionSpec = { GlassMotion.topBarMask() },
        label = "glassTopBarMask",
    ) { if (it) 1f else 0f }
    val scrolledMaterialProgress = transition.animateFloat(
        transitionSpec = { GlassMotion.topBarButtonFloat() },
        label = "glassTopBarMaterial",
    ) { if (it) 1f else 0f }
    val materialProgress = remember(scrolledMaterialProgress, scrollBehavior, ramp) {
        derivedStateOf {
            val elasticProgress = if (scrollBehavior != null && scrollBehavior.state.overscrollOffset != 0f) ramp.value else 0f
            maxOf(scrolledMaterialProgress.value, elasticProgress)
        }
    }
    val keepMaterial by remember(isContentScrolled, materialProgress) {
        derivedStateOf { isContentScrolled || materialProgress.value > 0f }
    }
    val rawBackdrop = backdrop?.takeIf { isRuntimeShaderSupported() }
    val underlayMaterial = GlassTopAppBarDefaults.actionBarUnderlayMaterial().takeIf { keepMaterial && rawBackdrop != null }
    var backgroundCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val progressiveBackdrop = if (progressiveBlur && rawBackdrop != null) {
        rememberLayerBackdrop {
            val coordinates = backgroundCoordinates?.takeIf { it.isAttached }
            with(rawBackdrop) { drawBackdrop(this@rememberLayerBackdrop, coordinates) }
            drawContent()
        }
    } else {
        null
    }
    val buttonUnderlay = underlayMaterial.takeIf { progressiveBackdrop == null }
    val resolvedMaterial = material.withUnderlayBlur(buttonUnderlay)
    val buttonBackdrop = progressiveBackdrop ?: rawBackdrop

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
        if (progressiveBackdrop != null && rawBackdrop != null) {
            val topInsets = WindowInsets.statusBars
            val blurColors = BlurDefaults.blurColors(
                blendColors = listOf(BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.3f))),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        val barHeight = topInsets.getTop(this) + TopAppBarDefaults.CollapsedHeight.toPx()
                        // External overlay rows extend the sampled band without changing the bar's layout height.
                        val extraOverhang = (bandOverhang - GlassTopAppBarDefaults.BandOverhang)
                            .coerceAtLeast(0.dp).toPx()
                        val height = ((barHeight + 20.dp.toPx()) * 1.2f + extraOverhang).roundToInt()
                        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, 0) }
                    }
                    .onGloballyPositioned { backgroundCoordinates = it }
                    .layerBackdrop(progressiveBackdrop)
                    .graphicsLayer {
                        val offset = visualContentOffset?.invoke() ?: scrollBehavior?.state?.visualContentOffset
                        val ramp = offset?.let {
                            (-it / GlassTopAppBarDefaults.ProgressiveBlurRampDistance.toPx()).coerceIn(0f, 1f)
                        } ?: maskProgress.value
                        this.alpha = ramp * backgroundAlpha().coerceIn(0f, 1f)
                    }
                    .progressiveTextureBlur(
                        backdrop = rawBackdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 0.5f),
                        blurRadius = GlassTopAppBarDefaults.ProgressiveBlurRadius.value,
                        colors = blurColors,
                    ),
            )
        } else {
            val overhangPx = with(LocalDensity.current) { bandOverhang.roundToPx().coerceAtLeast(0).toFloat() }
            val fadeHeight = overhangPx + with(LocalDensity.current) { 12.dp.toPx() }
            val fadeBrush = remember(bandBrush, fadeHeight) {
                if (bandBrush is TopAppBarBandBrush) ShaderBrush(bandBrush.createShader(0f, fadeHeight)) else null
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        val bandHeight = constraints.maxHeight + if (bandBrush is TopAppBarBandBrush) overhangPx.roundToInt() else 0
                        val placeable = measurable.measure(constraints.copy(minHeight = bandHeight, maxHeight = bandHeight))
                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, 0) }
                    }
                    .graphicsLayer {
                        val progress = if (visualContentOffset != null || scrollBehavior != null) ramp.value else maskProgress.value
                        this.alpha = progress * backgroundAlpha().coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        scaleY = if (bandBrush !is TopAppBarBandBrush && size.height > 0f) (size.height + overhangPx) / size.height else 1f
                    }
                    .then(
                        if (bandBrush is TopAppBarBandBrush && fadeBrush != null) {
                            Modifier.drawBehind {
                                val fadeStart = (size.height - fadeHeight).coerceAtLeast(0f)
                                drawRect(bandBrush.color, size = Size(size.width, fadeStart))
                                translate(top = fadeStart) {
                                    scale(scaleX = 1f, scaleY = (size.height - fadeStart) / fadeHeight, pivot = Offset.Zero) {
                                        drawRect(fadeBrush, size = Size(size.width, fadeHeight))
                                    }
                                }
                            }
                        } else {
                            Modifier.background(bandBrush)
                        },
                    ),
            )
        }
        CompositionLocalProvider(
            LocalGlassTopAppBarContext provides GlassTopAppBarContext(
                backdrop = rawBackdrop,
                buttonBackdrop = buttonBackdrop,
                material = resolvedMaterial,
                underlayMaterial = underlayMaterial,
                buttonUnderlayMaterial = buttonUnderlay,
                floating = isContentScrolled,
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
