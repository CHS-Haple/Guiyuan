// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified from upstream (2026): compact title, title padding, and unclipped action controls.

package com.chaners.guiyuan.ui.glass

import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults as UpstreamGlassTopAppBarDefaults

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
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
import top.yukonga.miuix.kmp.material.withUnderlayBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.GlassMaterials
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassStyle
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadow
import top.yukonga.miuix.kmp.material.GlassShadows

/** Default values for [GlassTopAppBar] and [GlassIconButton]. */
object GlassTopAppBarDefaults {

    /** Start/end inset of the glass controls, aligned with the page's cards and tabs. */
    val HorizontalPadding: Dp = UpstreamGlassTopAppBarDefaults.HorizontalPadding

    /** How far the large title blurs out as it collapses. */
    val TitleBlurRadius: Dp = UpstreamGlassTopAppBarDefaults.TitleBlurRadius

    /** Diameter of the glass pill behind a bar button. */
    val ButtonSize: Dp = UpstreamGlassTopAppBarDefaults.ButtonSize

    /** Maximum radius of the upstream progressive background band. */
    val ProgressiveBlurRadius: Dp = UpstreamGlassTopAppBarDefaults.ProgressiveBlurRadius

    /** Icon opacity while an action-bar button is held. */
    val PressedContentAlpha: Float = UpstreamGlassTopAppBarDefaults.PressedContentAlpha

    /** The traditional fill used when backdrop material is unavailable. */
    @Composable
    @ReadOnlyComposable
    fun buttonFill(): Color = UpstreamGlassTopAppBarDefaults.buttonFill()

    /** The `internal-pured-thin-glass` action-button material. */
    @Composable
    fun buttonMaterial(): GlassMaterial = UpstreamGlassTopAppBarDefaults.buttonMaterial()

    /** The parent material applied before child button and tab materials. */
    @Composable
    internal fun actionBarUnderlayMaterial(): GlassMaterial = GlassMaterials.actionBarMask(isDarkTheme())

    /** The small bloom stroke paired with [buttonMaterial]. */
    @Composable
    @ReadOnlyComposable
    fun buttonStroke(): EdgeStroke = UpstreamGlassTopAppBarDefaults.buttonStroke()

    /** The state overlay painted behind a held action-bar icon. */
    @Composable
    @ReadOnlyComposable
    fun buttonPressedOverlay(): Color = UpstreamGlassTopAppBarDefaults.buttonPressedOverlay()

    /**
     * The band that dims the content as it passes under the bar: the page's own colour, laid back
     * over it, solid at the top edge and gone by the bottom.
     *
     * @param color The page's background colour.
     */
    @Stable
    fun bandBrush(color: Color): Brush = TopAppBarBandBrush(color)

    /** How far the band reaches past the bottom of the bar. */
    val BandOverhang: Dp = UpstreamGlassTopAppBarDefaults.BandOverhang

    @Composable
    @ReadOnlyComposable
    private fun isDarkTheme(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f
}

private data class TopAppBarBandBrush(val color: Color) : ShaderBrush() {
    val colors = opacities.map { color.copy(alpha = color.alpha * it) }

    override fun createShader(size: Size): Shader = LinearGradientShader(
        from = Offset.Zero,
        to = Offset(0f, size.height.coerceAtLeast(1f)),
        colors = colors,
        colorStops = stops,
    )

    companion object {
        const val MAX_ALPHA = 0.90f
        val stops = List(17) { it / 16f }
        private val opacities = stops.map { t ->
            val fade = t * t * t * (t * (t * 6f - 15f) + 10f)
            MAX_ALPHA * (1f - fade)
        }
    }
}

private class TopAppBarFixedFadeBrush(
    private val band: TopAppBarBandBrush,
    private val fadeHeight: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val fadeStart = (size.height - fadeHeight).coerceAtLeast(0f)
        return LinearGradientShader(
            from = Offset(0f, fadeStart),
            to = Offset(0f, fadeStart + fadeHeight.coerceAtLeast(1f)),
            colors = band.colors,
            colorStops = TopAppBarBandBrush.stops,
        )
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
    val materialProgress: State<Float>,
    val keepMaterial: Boolean,
    val style: GlassStyle,
    val size: Dp,
    val shape: GlassShape,
    val fill: Color,
    val stroke: EdgeStroke?,
    val shadow: GlassShadow?,
    val onButtonBackdropConsumerChange: ((Int) -> Unit)? = null,
)

internal val LocalGlassTopAppBarContext = compositionLocalOf<GlassTopAppBarContext?> { null }

/**
 * A glass top bar with an explicit page-overlap state.
 *
 * @param title The compact title.
 * @param isContentScrolled Actual content overlap, such as `listState.canScrollBackward`.
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
    bandBrush: Brush = MiuixTheme.colorScheme.surface.let { surface ->
        remember(surface) { GlassTopAppBarDefaults.bandBrush(surface) }
    },
    bandOverhang: Dp = GlassTopAppBarDefaults.BandOverhang,
    titleBlurRadius: Dp = GlassTopAppBarDefaults.TitleBlurRadius,
    style: GlassStyle = GlassDefaults.Style,
    material: GlassMaterial = GlassTopAppBarDefaults.buttonMaterial(),
    alpha: Float = 1f,
    buttonSize: Dp = GlassTopAppBarDefaults.ButtonSize,
    buttonShape: GlassShape = remember(buttonSize) { GlassShape(buttonSize / 2) },
    fill: Color = GlassTopAppBarDefaults.buttonFill(),
    stroke: EdgeStroke? = GlassTopAppBarDefaults.buttonStroke(),
    buttonShadow: GlassShadow? = GlassShadows.Regular,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    titleAlpha: () -> Float = { 1f },
    backgroundAlpha: () -> Float = { 1f },
    compact: Boolean = false,
    expandable: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    unclippedNavigationIcon: Boolean = false,
    unclippedActions: Boolean = false,
) {
    val contentScrolled by remember(isContentScrolled, scrollBehavior) {
        derivedStateOf {
            isContentScrolled || (scrollBehavior?.state?.overscrollOffset ?: 0f) < -1f
        }
    }
    val transition = updateTransition(contentScrolled, label = "glassTopBarFloat")
    // SmallTopAppBar pins the height range to zero; fixed titles use the upstream overlap fade.
    val maskProgress = transition.animateFloat(
        transitionSpec = { tween(durationMillis = 100, easing = LinearEasing) },
        label = "glassTopBarMask",
    ) { if (it) 1f else 0f }
    val useCollapseProgress = !compact && expandable && scrollBehavior != null
    val bandProgress: () -> Float = remember(useCollapseProgress, scrollBehavior, maskProgress) {
        if (useCollapseProgress) {
            { scrollBehavior?.state?.collapsedFraction?.coerceIn(0f, 1f) ?: 0f }
        } else {
            { maskProgress.value }
        }
    }
    val materialProgress = transition.animateFloat(
        transitionSpec = { GlassMotion.topBarButtonFloat() },
        label = "glassTopBarMaterial",
    ) { if (it) 1f else 0f }
    val keepMaterial by remember(contentScrolled, materialProgress) {
        derivedStateOf { contentScrolled || materialProgress.value > 0f }
    }
    val rawBackdrop = backdrop?.takeIf { isRuntimeShaderSupported() }
    val underlayMaterial = GlassTopAppBarDefaults.actionBarUnderlayMaterial().takeIf { rawBackdrop != null }
    var backgroundCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var buttonBackdropConsumers by remember { mutableIntStateOf(0) }
    val hasButtonBackdropConsumers = buttonBackdropConsumers > 0
    val onButtonBackdropConsumerChange: (Int) -> Unit = remember {
        { delta -> buttonBackdropConsumers = (buttonBackdropConsumers + delta).coerceAtLeast(0) }
    }
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
    val resolvedMaterial = remember(material, buttonUnderlay) { material.withUnderlayBlur(buttonUnderlay) }
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
    // The native title clips only title layers, but compact mode
    // still delegates to SmallTopAppBar, which clips protruding glass controls.
    val overlayNavigation = unclippedNavigationIcon && compact
    val overlayActions = unclippedActions && compact

    Box(modifier = modifier.onGloballyPositioned { barOrigin = it.positionInRoot() }) {
        if (progressiveBackdrop != null && rawBackdrop != null) {
            val topInsets = WindowInsets.systemBars
            val surfaceColor = MiuixTheme.colorScheme.surface
            val blendColors = remember(surfaceColor) {
                listOf(BlendColorEntry(surfaceColor.copy(alpha = 0.2f)))
            }
            val blurColors = BlurDefaults.blurColors(blendColors = blendColors)
            val scrimBrush = remember(surfaceColor) {
                Brush.verticalGradient(
                    0f to surfaceColor.copy(alpha = 0.50f),
                    0.25f to surfaceColor.copy(alpha = 0.44f),
                    0.5f to surfaceColor.copy(alpha = 0.32f),
                    0.75f to surfaceColor.copy(alpha = 0.16f),
                    0.9f to surfaceColor.copy(alpha = 0.05f),
                    1f to Color.Transparent,
                )
            }
            val currentBgAlpha by rememberUpdatedState(backgroundAlpha)
            val bandActive by remember(useCollapseProgress, scrollBehavior, maskProgress) {
                derivedStateOf {
                    bandProgress() * currentBgAlpha().coerceIn(0f, 1f) > 0.001f
                }
            }
            val backdropCaptureModifier = if (hasButtonBackdropConsumers && (keepMaterial || bandActive)) {
                Modifier
                    .onGloballyPositioned { backgroundCoordinates = it }
                    .layerBackdrop(progressiveBackdrop)
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        // Track the actual native title layout, including compact and bottom-content rows.
                        // The upstream blur band uses the measured height plus its gradient overhang.
                        val fallbackCoveredHeight = maxOf(
                            constraints.maxHeight.toFloat(),
                            topInsets.getTop(this) + TopAppBarDefaults.CollapsedHeight.toPx(),
                        )
                        val coveredHeight = if (useCollapseProgress && scrollBehavior != null) {
                            val state = scrollBehavior.state
                            val limit = state.heightOffsetLimit
                            if (limit <= 0f && limit >= -constraints.maxHeight.toFloat()) {
                                val offset = state.heightOffset
                                val boundedOffset = if (offset.isNaN()) 0f else offset.coerceIn(limit, 0f)
                                (constraints.maxHeight.toFloat() + limit - boundedOffset)
                                    .coerceAtLeast(TopAppBarDefaults.CollapsedHeight.toPx())
                            } else {
                                fallbackCoveredHeight
                            }
                        } else {
                            fallbackCoveredHeight
                        }
                        val height = (coveredHeight + bandOverhang.toPx().coerceAtLeast(0f)).fastRoundToInt()
                        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, 0) }
                    }
                    .then(backdropCaptureModifier)
                    .graphicsLayer {
                        this.alpha = bandProgress() * backgroundAlpha().coerceIn(0f, 1f)
                    }
                    .progressiveTextureBlur(
                        backdrop = rawBackdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top,
                        blurRadius = GlassTopAppBarDefaults.ProgressiveBlurRadius.value,
                        colors = blurColors,
                        enabled = bandActive,
                    )
                    .drawWithContent {
                        drawContent()
                        if (bandActive) {
                            drawRect(brush = scrimBrush)
                        }
                    },
            )
        } else {
            val bandDensity = LocalDensity.current
            val overhangPx = with(bandDensity) { bandOverhang.roundToPx().coerceAtLeast(0).toFloat() }
            val fadeHeight = overhangPx + with(bandDensity) { 36.dp.toPx() }
            val fadeBrush = remember(bandBrush, fadeHeight) {
                if (bandBrush is TopAppBarBandBrush) TopAppBarFixedFadeBrush(bandBrush, fadeHeight) else null
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
                        this.alpha = bandProgress() * backgroundAlpha().coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        scaleY = if (bandBrush !is TopAppBarBandBrush && size.height > 0f) (size.height + overhangPx) / size.height else 1f
                    }
                    .then(
                        if (fadeBrush != null) {
                            Modifier.drawBehind {
                                drawRect(brush = fadeBrush)
                            }
                        } else {
                            Modifier.background(bandBrush)
                        },
                    ),
            )
        }
        val consumerChangeCallback = if (progressiveBackdrop != null) onButtonBackdropConsumerChange else null
        val topBarContext = remember(
            rawBackdrop,
            buttonBackdrop,
            resolvedMaterial,
            underlayMaterial,
            buttonUnderlay,
            contentScrolled,
            alpha,
            materialProgress,
            keepMaterial,
            style,
            buttonSize,
            buttonShape,
            fill,
            stroke,
            buttonShadow,
            consumerChangeCallback,
        ) {
            GlassTopAppBarContext(
                backdrop = rawBackdrop,
                buttonBackdrop = buttonBackdrop,
                material = resolvedMaterial,
                underlayMaterial = underlayMaterial,
                buttonUnderlayMaterial = buttonUnderlay,
                floating = contentScrolled,
                alpha = alpha,
                materialProgress = materialProgress,
                keepMaterial = keepMaterial,
                style = style,
                size = buttonSize,
                shape = buttonShape,
                fill = fill,
                stroke = stroke,
                shadow = buttonShadow,
                onButtonBackdropConsumerChange = consumerChangeCallback,
            )
        }
        CompositionLocalProvider(
            LocalGlassTopAppBarContext provides topBarContext,
        ) {
            // The published/native API keeps its legacy clip; PR423 provides native title blur.
            UiNativeTitleBar(
                title = title,
                modifier = contentModifier,
                largeTitle = largeTitle,
                titleBlurRadius = titleBlurRadius,
                titleAlpha = titleAlpha,
                compact = compact,
                expandable = expandable,
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
