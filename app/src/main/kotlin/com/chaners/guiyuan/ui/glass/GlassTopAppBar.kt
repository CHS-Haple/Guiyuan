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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
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

    /** How far the band reaches past the bottom of the bar. */
    val BandOverhang: Dp = UpstreamGlassTopAppBarDefaults.BandOverhang

    @Composable
    @ReadOnlyComposable
    private fun isDarkTheme(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f
}

/** The page-color fade palette, cached independently of the measured band height. */
private data class TopAppBarBandPalette(val color: Color) {
    val colors = opacities.map { color.copy(alpha = color.alpha * it) }

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
    private val band: TopAppBarBandPalette,
    private val fadeHeight: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val fadeStart = (size.height - fadeHeight).fastCoerceAtLeast(0f)
        return LinearGradientShader(
            from = Offset(0f, fadeStart),
            to = Offset(0f, fadeStart + fadeHeight.fastCoerceAtLeast(1f)),
            colors = band.colors,
            colorStops = TopAppBarBandPalette.stops,
        )
    }
}

internal data class GlassTopAppBarContext(
    val buttonBackdrop: Backdrop?,
    val material: GlassMaterial,
    val buttonUnderlayMaterial: GlassMaterial?,
    val floating: Boolean,
    val materialProgress: State<Float>,
    val keepMaterial: Boolean,
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
internal fun GlassTopAppBar(
    title: String,
    isContentScrolled: Boolean,
    contentModifier: Modifier = Modifier,
    progressiveBlur: Boolean = false,
    backdrop: Backdrop? = null,
    largeTitle: String = title,
    scrollBehavior: ScrollBehavior? = null,
    bandOverhang: Dp = GlassTopAppBarDefaults.BandOverhang,
    titleAlpha: () -> Float = { 1f },
    compact: Boolean = false,
    expandable: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    unclippedNavigationIcon: Boolean = false,
    unclippedActions: Boolean = false,
) {
    val bandPalette = MiuixTheme.colorScheme.surface.let { surface ->
        remember(surface) { TopAppBarBandPalette(surface) }
    }
    val material = GlassTopAppBarDefaults.buttonMaterial()
    val buttonSize = GlassTopAppBarDefaults.ButtonSize
    val buttonShape = remember(buttonSize) { GlassShape(buttonSize / 2) }
    val fill = GlassTopAppBarDefaults.buttonFill()
    val stroke = GlassTopAppBarDefaults.buttonStroke()
    val buttonShadow = GlassShadows.Regular
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
            { scrollBehavior?.state?.collapsedFraction?.fastCoerceIn(0f, 1f) ?: 0f }
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
        { delta -> buttonBackdropConsumers = (buttonBackdropConsumers + delta).fastCoerceAtLeast(0) }
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

    Box(modifier = Modifier.onGloballyPositioned { barOrigin = it.positionInRoot() }) {
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
            val bandActive by remember(useCollapseProgress, scrollBehavior, maskProgress) {
                derivedStateOf {
                    bandProgress() > 0.001f
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
                        val state = if (useCollapseProgress) scrollBehavior?.state else null
                        val coveredHeight = topBarCoveredHeight(
                            measuredHeight = constraints.maxHeight.toFloat(),
                            fallbackHeight = fallbackCoveredHeight,
                            collapsedHeight = TopAppBarDefaults.CollapsedHeight.toPx(),
                            offsetLimit = state?.heightOffsetLimit,
                            offset = { state?.heightOffset ?: 0f },
                        )
                        val height = (coveredHeight + bandOverhang.toPx().fastCoerceAtLeast(0f)).fastRoundToInt()
                        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, 0) }
                    }
                    .then(backdropCaptureModifier)
                    .graphicsLayer {
                        this.alpha = bandProgress()
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
            val overhangPx = with(bandDensity) { bandOverhang.roundToPx().fastCoerceAtLeast(0).toFloat() }
            val fadeHeight = overhangPx + with(bandDensity) { 36.dp.toPx() }
            val fadeBrush = remember(bandPalette, fadeHeight) {
                TopAppBarFixedFadeBrush(bandPalette, fadeHeight)
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        val bandHeight = constraints.maxHeight + overhangPx.roundToInt()
                        val placeable = measurable.measure(constraints.copy(minHeight = bandHeight, maxHeight = bandHeight))
                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, 0) }
                    }
                    .graphicsLayer {
                        this.alpha = bandProgress()
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        scaleY = 1f
                    }
                    .drawBehind { drawRect(brush = fadeBrush) },
            )
        }
        val consumerChangeCallback = if (progressiveBackdrop != null) onButtonBackdropConsumerChange else null
        val topBarContext = remember(
            buttonBackdrop,
            resolvedMaterial,
            buttonUnderlay,
            contentScrolled,
            materialProgress,
            keepMaterial,
            buttonShape,
            fill,
            stroke,
            buttonShadow,
            consumerChangeCallback,
        ) {
            GlassTopAppBarContext(
                buttonBackdrop = buttonBackdrop,
                material = resolvedMaterial,
                buttonUnderlayMaterial = buttonUnderlay,
                floating = contentScrolled,
                materialProgress = materialProgress,
                keepMaterial = keepMaterial,
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
                titleAlpha = titleAlpha,
                compact = compact,
                expandable = expandable,
                scrollBehavior = scrollBehavior,
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

/** Measured native title coverage; a provisional collapse limit must not shrink the band. */
internal inline fun topBarCoveredHeight(
    measuredHeight: Float,
    fallbackHeight: Float,
    collapsedHeight: Float,
    offsetLimit: Float?,
    offset: () -> Float,
): Float {
    if (offsetLimit == null || !(offsetLimit <= 0f && offsetLimit >= -measuredHeight)) return fallbackHeight
    val current = offset()
    val bounded = if (current.isNaN()) 0f else current.fastCoerceIn(offsetLimit, 0f)
    return (measuredHeight + offsetLimit - bounded).fastCoerceAtLeast(collapsedHeight)
}
