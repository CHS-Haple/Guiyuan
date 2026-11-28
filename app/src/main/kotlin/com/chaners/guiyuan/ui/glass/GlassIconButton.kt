// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Retains existing Guiyuan long-press, disabled and hold-down API.

package com.chaners.guiyuan.ui.glass

import top.yukonga.miuix.kmp.glass.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.material.drawGlassMask
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.interfaces.HoldDownInteraction
import top.yukonga.miuix.kmp.utils.MiuixIndication
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults

/**
 * A round glass button for a bar's leading or trailing controls.
 *
 * With [glassPopupAnchor] attached, the button supplies its resolved backdrop and material to
 * [GlassTransformPopup]'s collapsed endpoint. The panel blends into its own material as it grows.
 *
 * @param onClick Called when the button is tapped.
 * @param backdrop Optional [Backdrop] behind the glass. Inside [GlassTopAppBar] the button inherits
 *   the bar's backdrop and effective blur; elsewhere `null` gives the traditional fill.
 * @param modifier The modifier applied to the button.
 * @param enabled Whether the button receives interaction.
 * @param holdDownState Keeps press feedback while a long-running action is active.
 * @param size Diameter of the button.
 * @param content The icon inside.
 */
@Composable
internal fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    backdrop: Backdrop? = null,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    size: Dp = GlassTopAppBarDefaults.ButtonSize,
    content: @Composable () -> Unit,
) {
    val topBarContext = LocalGlassTopAppBarContext.current
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, holdDownState, enabled) {
        if (holdDownState && enabled) {
            val hold = HoldDownInteraction.HoldDown()
            interactionSource.emit(hold)
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { interactionSource.emit(HoldDownInteraction.Release(hold)) }
            }
        }
    }
    DisposableEffect(topBarContext) {
        val callback = topBarContext?.onButtonBackdropConsumerChange
        callback?.invoke(1)
        onDispose { callback?.invoke(-1) }
    }
    GlassButtonSurface(
        backdrop = backdrop,
        topBarContext = topBarContext,
        enabled = enabled,
        size = size,
        interactionSource = interactionSource,
        modifier = modifier.glassButtonClick(
            onClick = onClick,
            onLongClick = onLongClick,
            onLongClickLabel = onLongClickLabel,
            enabled = enabled,
            interactionSource = interactionSource,
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
    topBarContext: GlassTopAppBarContext?,
    enabled: Boolean,
    size: Dp,
    modifier: Modifier,
    interactionSource: InteractionSource,
    content: @Composable () -> Unit,
) {
    val style = GlassDefaults.Style
    val shape = topBarContext?.shape ?: remember(size) { GlassShape(size / 2) }
    val fill = topBarContext?.fill ?: GlassTopAppBarDefaults.buttonFill()
    val stroke = if (topBarContext != null) topBarContext.stroke else GlassTopAppBarDefaults.buttonStroke()
    val shadow = GlassShadows.Regular
    val material = topBarContext?.material ?: GlassTopAppBarDefaults.buttonMaterial()
    val underlayMaterial = topBarContext?.buttonUnderlayMaterial
    val popupAnchor = modifier.foldIn<GlassPopupAnchor?>(null) { found, element ->
        if (element is GlassPopupAnchorElement) element.anchor else found
    }
    DisposableEffect(popupAnchor) {
        onDispose { popupAnchor?.surface = null }
    }
    // Standalone buttons are fully floating; the header is the only changing material clock.
    val floating = topBarContext?.floating ?: true
    val progress = topBarContext?.materialProgress
    val keepMaterial = topBarContext?.keepMaterial ?: true
    val opacity = if (enabled) 1f else 0.38f
    val shadowAlpha = remember(opacity, progress) {
        { opacity * (progress?.value ?: 1f) }
    }
    val effectiveBackdrop = (topBarContext?.buttonBackdrop ?: backdrop)?.takeIf { isRuntimeShaderSupported() }
    val anchorSurface = if (popupAnchor != null) {
        remember(effectiveBackdrop, material, underlayMaterial, stroke, fill) {
            GlassAnchorSurface(effectiveBackdrop, material, underlayMaterial, stroke, fill)
        }
    } else {
        null
    }
    SideEffect {
        if (popupAnchor != null) {
            popupAnchor.surface = anchorSurface
            popupAnchor.surfaceProgress = progress
            popupAnchor.surfaceMaterialAlpha = opacity
            popupAnchor.surfaceFloating = floating
        }
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
                .graphicsLayer { alpha = progress?.value ?: 1f }
                .then(
                    if (effectiveBackdrop != null) {
                        if (underlayMaterial != null) {
                            Modifier.glassOnActionBar(
                                backdrop = effectiveBackdrop,
                                shape = shape,
                                alpha = opacity,
                                material = material,
                                underlayMaterial = underlayMaterial,
                                stroke = stroke.takeIf { keepMaterial },
                                enabled = keepMaterial,
                            )
                        } else {
                            Modifier.glass(
                                backdrop = effectiveBackdrop,
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
        val indication = remember(pressedOverlay) {
            MiuixIndication(
                color = pressedOverlay.copy(alpha = 1f),
                pressAlpha = pressedOverlay.alpha,
                hoverAlpha = 0f,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .indication(interactionSource, indication),
        )
        content()
    }
}

/** Shared input feedback stays on the one actual button root; no new interaction owner. */
private fun Modifier.glassButtonClick(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
): Modifier = then(
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
)
