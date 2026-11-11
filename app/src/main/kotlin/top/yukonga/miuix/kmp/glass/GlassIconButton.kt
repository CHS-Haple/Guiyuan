// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Retains existing Guiyuan long-press, disabled and hold-down API.

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.material.drawGlassMask

/**
 * A round glass button for a bar's leading or trailing controls.
 *
 * With [glassPopupAnchor] attached, the button supplies its resolved backdrop and material to
 * [GlassTransformPopup]'s collapsed endpoint. The panel blends into its own material as it grows.
 *
 * @param onClick Called when the button is tapped.
 * @param inheritTopBarMaterial Whether to use the parent top bar's resolved progressive backdrop.
 * @param material Optional button material over the resolved parent underlay.
 * @param backdrop Optional [Backdrop] behind the glass. Inside [GlassTopAppBar] the button inherits
 *   the bar's backdrop and effective blur; elsewhere `null` gives the traditional [fill].
 * @param modifier The modifier applied to the button.
 * @param surfaceAlpha Opacity of the pill. Inside [GlassTopAppBar], leave this at its default.
 * @param enabled Whether the button receives interaction.
 * @param holdDownState Keeps press feedback while a long-running action is active.
 * @param style Compatibility style forwarded to the renderer.
 * @param size Diameter of the button.
 * @param shape The button's silhouette. Defaults to a circle of [size].
 * @param shadow The shadow under the button. `null` removes it.
 * @param stroke Optional bloom stroke along the rim.
 * @param content The icon inside.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    inheritTopBarMaterial: Boolean = true,
    backdrop: Backdrop? = null,
    surfaceAlpha: Float = 1f,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    style: GlassStyle = LocalGlassTopAppBarContext.current?.style ?: GlassDefaults.Style,
    material: GlassMaterial? = null,
    size: Dp = LocalGlassTopAppBarContext.current?.size ?: GlassTopAppBarDefaults.ButtonSize,
    shape: GlassShape = LocalGlassTopAppBarContext.current?.shape ?: GlassShape(size / 2),
    fill: Color = LocalGlassTopAppBarContext.current?.fill ?: GlassTopAppBarDefaults.buttonFill(),
    stroke: GlassStroke? = LocalGlassTopAppBarContext.current.let { if (it != null) it.stroke else GlassTopAppBarDefaults.buttonStroke() },
    shadow: GlassShadow? = LocalGlassTopAppBarContext.current.let { if (it != null) it.shadow else GlassShadows.Regular },
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val topBarContext = LocalGlassTopAppBarContext.current
    val materialContext = topBarContext.takeIf { inheritTopBarMaterial }
    val resolvedSurfaceAlpha = surfaceAlpha * (if (enabled) 1f else 0.38f) * (topBarContext?.alpha ?: 1f)
    val topBarRamp = topBarContext?.ramp
    GlassButtonSurface(
        backdrop = materialContext?.buttonBackdrop ?: backdrop ?: topBarContext?.buttonBackdrop,
        floating = topBarContext?.floating ?: true,
        surfaceAlpha = resolvedSurfaceAlpha,
        shadowAlpha = { resolvedSurfaceAlpha * (topBarRamp?.value ?: 1f) },
        style = style,
        material = material?.withUnderlayBlur(materialContext?.buttonUnderlayMaterial)
            ?: materialContext?.material ?: GlassTopAppBarDefaults.buttonMaterial(),
        underlayMaterial = materialContext?.buttonUnderlayMaterial,
        shape = shape,
        size = size,
        fill = fill,
        stroke = stroke,
        shadow = shadow,
        sharedProgress = topBarContext?.materialProgress,
        sharedKeepMaterial = topBarContext?.keepMaterial,
        pressed = pressed || holdDownState,
        modifier = modifier.then(
            if (onLongClick == null) {
                Modifier.clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
            } else {
                Modifier.combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = null,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
            },
        ),
        content = content,
    )
}

/** The surface and press feedback used by [GlassIconButton] in either top-bar slot. */
@Composable
// foldIn only reads the anchor marker; the modifier is still applied to exactly one root Box.
@Suppress("ktlint:compose:modifier-reused-check", "ktlint:compose:state-param-check")
private fun GlassButtonSurface(
    backdrop: Backdrop?,
    floating: Boolean,
    surfaceAlpha: Float,
    shadowAlpha: () -> Float,
    style: GlassStyle,
    material: GlassMaterial,
    underlayMaterial: GlassMaterial?,
    fill: Color,
    shape: GlassShape,
    size: Dp,
    stroke: GlassStroke?,
    shadow: GlassShadow?,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    // Retain State for draw-phase reads; the bar owns the shared animation clock.
    sharedProgress: State<Float>? = null,
    sharedKeepMaterial: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val popupAnchor = modifier.foldIn<GlassPopupAnchor?>(null) { found, element ->
        if (element is GlassPopupAnchorElement) element.anchor else found
    }
    DisposableEffect(popupAnchor) {
        onDispose { popupAnchor?.surface = null }
    }
    val progress = sharedProgress ?: run {
        val floatingTransition = updateTransition(targetState = floating, label = "glassTopBarButtonFloat")
        floatingTransition.animateFloat(
            transitionSpec = { GlassMotion.topBarButtonFloat() },
            label = "glassTopBarButtonMaterial",
        ) { active ->
            if (active) 1f else 0f
        }
    }
    val ownKeepMaterial by remember(floating, progress) {
        derivedStateOf { floating || progress.value > 0f }
    }
    val keepMaterial = sharedKeepMaterial ?: ownKeepMaterial
    val opacity = surfaceAlpha.coerceIn(0f, 1f)
    val anchorSurface = remember(backdrop, style, material, underlayMaterial, stroke, fill) {
        GlassAnchorSurface(backdrop, style, material, underlayMaterial, stroke, fill)
    }
    SideEffect {
        if (popupAnchor != null) {
            popupAnchor.surface = anchorSurface
            popupAnchor.surfaceProgress = progress
            popupAnchor.surfaceMaterialAlpha = opacity
            popupAnchor.surfaceFloating = floating
        }
    }
    val pressTransition = updateTransition(targetState = pressed, label = "glassTopBarButtonPress")
    val contentAlpha = pressTransition.animateFloat(
        transitionSpec = { if (targetState) GlassMotion.pressDown() else GlassMotion.pressUp() },
        label = "glassTopBarButtonContentAlpha",
    ) { down ->
        if (down) GlassTopAppBarDefaults.PressedContentAlpha else 1f
    }
    val overlayAlpha = pressTransition.animateFloat(
        transitionSpec = { if (targetState) GlassMotion.pressDown() else GlassMotion.pressUp() },
        label = "glassTopBarButtonOverlayAlpha",
    ) { down ->
        if (down) 1f else 0f
    }
    val pressedOverlay = GlassTopAppBarDefaults.buttonPressedOverlay()

    Box(
        modifier = modifier
            .size(size)
            .glassShadow(shape, shadow, shadowAlpha),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = progress.value }
                .then(
                    if (backdrop != null) {
                        if (underlayMaterial != null) {
                            Modifier.glassOnActionBar(
                                backdrop = backdrop,
                                shape = shape,
                                style = style,
                                alpha = opacity,
                                material = material,
                                underlayMaterial = underlayMaterial,
                                stroke = stroke.takeIf { keepMaterial },
                                enabled = keepMaterial,
                            )
                        } else {
                            Modifier.glass(
                                backdrop = backdrop,
                                shape = shape,
                                style = style,
                                alpha = opacity,
                                material = material,
                                stroke = stroke.takeIf { keepMaterial },
                                enabled = keepMaterial,
                                shading = false,
                            )
                        }
                    } else {
                        Modifier
                            .then(
                                if (isRuntimeShaderSupported()) {
                                    Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                } else {
                                    Modifier.clip(shape)
                                },
                            )
                            .background(fill.copy(alpha = fill.alpha * opacity))
                            .drawWithContent {
                                drawContent()
                                if (stroke != null && keepMaterial) {
                                    drawEdgeStroke(shape, stroke, opacity)
                                }
                                drawGlassMask(shape)
                            }
                    },
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .drawBehind {
                    drawRect(pressedOverlay.copy(alpha = pressedOverlay.alpha * overlayAlpha.value.coerceIn(0f, 1f)))
                },
        )
        Box(
            modifier = Modifier.graphicsLayer { alpha = contentAlpha.value.coerceIn(0f, 1f) },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
