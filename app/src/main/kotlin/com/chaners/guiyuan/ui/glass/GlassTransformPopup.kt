// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupSizing

import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.anim.folmeSpring
import androidx.compose.animation.core.SpringSpec

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import top.yukonga.miuix.kmp.blur.Backdrop
import androidx.compose.ui.util.fastRoundToInt

// GlassMotion.TRANSFORM_VISIBILITY_THRESHOLD is internal to MIUIX.
private const val TransformVisibilityThreshold = 0.0015f
private const val GuiyuanPopupMaskVisibleChange = 1f / 256f

// Upstream GlassMotion keeps this spring internal; the adapted popup owns its mask.
private fun guiyuanPopupMaskSpring(expanding: Boolean): SpringSpec<Float> = folmeSpring(
    damping = 0.95f,
    response = if (expanding) 0.35f else 0.2f,
    visibilityThreshold = GuiyuanPopupMaskVisibleChange * 0.75f,
)

/**
 * A glass menu that expands from an anchored control with a content crossfade.
 *
 * Place it in the same root [BoxScope] as the anchor and keep it composed through exit.
 * Rows accept input while open unless [stacked]. Predictive Back previews collapse to the anchor
 * and restores on cancellation; a stacked secondary menu handles Back instead.
 *
 * @param show Whether the menu is open.
 * @param onDismissRequest Called when an outside tap or Back requests dismissal.
 * @param anchor Control used as the collapsed endpoint; [glassPopupAnchor] reports its bounds in
 *   window coordinates.
 * @param backdrop Scene sampled by the expanded menu, including bars but excluding the popup.
 *   `null` falls back to the button's backdrop. The collapsed endpoint uses the button's surface.
 * @param anchorContent Copy of the control, or the portion identified by [glassPopupAnchorContent].
 * @param stacked Whether a secondary menu is open. Scales down this panel and applies the native mask.
 * @param horizontalAlignment Horizontal alignment of the expanded panel relative to [anchor].
 * @param onMeasured Called with the final panel size. Use its width as the submenu's minimum width.
 * @param content The items.
 */
@Composable
fun BoxScope.GlassTransformPopup(
    show: Boolean,
    onDismissRequest: () -> Unit,
    anchor: GlassPopupAnchor,
    backdrop: Backdrop?,
    anchorContent: @Composable () -> Unit,
    stacked: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    onMeasured: ((Size) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sizing = GlassPopupSizing()
    val visuals = GlassPopupDefaults.visuals()
    val transition = updateTransition(show, label = "glassTransformPopup")
    val bounds = transition.animateFloat(
        transitionSpec = { GlassMotion.transformBounds(targetState) },
        label = "glassTransformBounds",
    ) { if (it) 1f else 0f }
    val center = transition.animateFloat(
        transitionSpec = { GlassMotion.transformCenter(targetState) },
        label = "glassTransformCenter",
    ) { if (it) 1f else 0f }
    val iconMaterial = transition.animateFloat(
        transitionSpec = { GlassMotion.transformIconMaterial(targetState) },
        label = "glassTransformIcon",
    ) { if (it) 1f else 0f }
    val contentMaterial = transition.animateFloat(
        transitionSpec = { GlassMotion.transformContentMaterial(targetState) },
        label = "glassTransformContent",
    ) { if (it) 1f else 0f }

    val pushedBack = animateFloatAsState(
        targetValue = if (stacked) 1f else 0f,
        animationSpec = GlassMotion.secondaryPopup(stacked),
        label = "glassTransformStacked",
    )
    val maskAlpha = animateFloatAsState(
        targetValue = if (stacked) 1f else 0f,
        animationSpec = guiyuanPopupMaskSpring(stacked),
        label = "glassTransformMask",
    )
    val active by remember(show, bounds, center) {
        derivedStateOf {
            show ||
                bounds.value > TransformVisibilityThreshold ||
                center.value > TransformVisibilityThreshold
        }
    }
    val travel = rememberTransformTravel(show, active, anchor, bounds, center)
    val backProgress = rememberGlassPopupBackProgress(
        show = show,
        active = active,
        enabled = show && !stacked,
        resetSpec = GlassMotion.transformBounds(true),
        onDismissRequest = onDismissRequest,
    )
    SideEffect { anchor.popupVisuals = visuals }
    DisposableEffect(anchor) {
        onDispose { anchor.popupVisuals = null }
    }
    if (!active) return

    val maskColor = GlassPopupDefaults.maskColor()
    val anchorSurface = anchor.surface
    val anchorVisuals = anchorSurface?.let {
        visuals.copy(
            material = it.material,
            stroke = it.stroke,
            containerColor = it.fill.copy(alpha = it.fill.alpha * anchor.surfaceMaterialAlpha),
            alpha = anchor.surfaceMaterialAlpha,
        )
    }
    val menuBackdrop = backdrop ?: anchorSurface?.backdrop
    val materialProgress = remember(bounds, backProgress) {
        derivedStateOf { popupFractionWithBack(bounds.value, backProgress.value).coerceIn(0f, 1f) }
    }
    val drawAnchorSurface by remember(anchorSurface, materialProgress) {
        derivedStateOf { anchorSurface != null && materialProgress.value < 1f }
    }
    val drawMenuSurface by remember(anchorSurface, materialProgress) {
        derivedStateOf { anchorSurface == null || materialProgress.value > 0f }
    }

    val startRect = anchor.containerBounds
    val layoutDirection = LocalLayoutDirection.current
    val iconRect = anchor.contentBounds.takeUnless { it.isEmpty } ?: startRect
    val startRadius = anchor.cornerRadius
    fun geometryProgress() = popupFractionWithBack(bounds.value, backProgress.value)
    fun centerProgress() = popupFractionWithBack(center.value, backProgress.value)
    fun iconProgress() = popupFractionWithBack(iconMaterial.value, backProgress.value)
    fun stackedProgress() = popupFractionWithBack(pushedBack.value, anchor.secondaryBackProgress)
    fun panelAlpha() = transformPanelAlpha(
        floating = if (anchorSurface != null) anchor.surfaceFloating || anchor.surfaceVisibility > 0f else anchor.floating,
        anchorAlpha = if (anchorSurface != null) {
            anchor.surfaceVisibility
        } else {
            1f
        },
        geometryProgress = geometryProgress(),
        iconMaterial = iconProgress(),
    )
    val hostOrigin = remember { mutableStateOf(Offset.Zero) }

    GlassPopupSurface(
        onDismissRequest = onDismissRequest,
        sizing = sizing,
        panelLayer = {
            alpha = panelAlpha()
            transformOrigin = TransformOrigin(
                if (startRect.center.x < travel.endCenterX) 0f else 1f,
                if (startRect.center.y < travel.endCenterY) 0f else 1f,
            )
            val s = 1f - 0.05f * stackedProgress()
            scaleX = s
            scaleY = s
        },
        overlay = {
            val fraction = popupFractionWithBack(maskAlpha.value, anchor.secondaryBackProgress).coerceIn(0f, 1f)
            if (fraction > GuiyuanPopupMaskVisibleChange) {
                drawRect(maskColor.copy(alpha = maskColor.alpha * fraction))
            }
        },
        frame = { end, windowBounds ->
            val rect = startRect
            val settled = placeGlassPopup(
                rect,
                end,
                sizing.safeMargin.toPx(),
                windowBounds,
                layoutDirection,
                horizontalAlignment,
            )
            travel.endCenterX = settled.rect.center.x
            travel.endCenterY = settled.rect.center.y
            travel.endWidth = end.width
            travel.endHeight = end.height
            transformFrame(
                anchor = rect,
                end = end,
                placement = settled,
                sizeFraction = geometryProgress(),
                positionFraction = centerProgress(),
                startRadius = startRadius,
                endRadius = GlassPopupDefaults.CornerRadius,
            )
        },
        contentLayer = { end ->
            val width = startRect.width + (end.width - startRect.width) * geometryProgress()
            val scale = if (end.width > 0f) (width / end.width).coerceAtLeast(0f) else 1f
            scaleX = scale
            scaleY = scale
            val fraction = popupFractionWithBack(contentMaterial.value, backProgress.value)
            alpha = fraction
            val blur = GlassMotion.TRANSFORM_BLUR_PX * (1f - fraction)
            renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
        },
        surface = { shape ->
            if (drawAnchorSurface && anchorSurface != null && anchorVisuals != null) {
                GlassPopupBackground(
                    backdrop = anchorSurface.backdrop,
                    shape = shape,
                    visuals = anchorVisuals,
                    underlayMaterial = anchorSurface.underlayMaterial,
                )
            }
            if (drawMenuSurface) {
                GlassPopupBackground(
                    backdrop = menuBackdrop,
                    shape = shape,
                    visuals = visuals,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (anchorSurface != null) materialProgress.value else 1f
                    },
                )
            }
        },
        onMeasured = onMeasured,
        interactive = isTransformPopupInteractive(show, stacked),
        content = content,
    )

    val renderAnchorContent by remember(iconMaterial, backProgress) {
        derivedStateOf {
            popupFractionWithBack(iconMaterial.value, backProgress.value) < 0.999f
        }
    }
    if (renderAnchorContent) {
        Box(
            modifier = Modifier
                // Outside the placement modifier so it reports the host origin, not the copy offset.
                .onGloballyPositioned { hostOrigin.value = it.positionInWindow() }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints.fixed(
                            width = iconRect.width.fastRoundToInt().coerceAtLeast(0),
                            height = iconRect.height.fastRoundToInt().coerceAtLeast(0),
                        ),
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(
                            (iconRect.left - hostOrigin.value.x).fastRoundToInt(),
                            (iconRect.top - hostOrigin.value.y).fastRoundToInt(),
                        )
                    }
                }
                .graphicsLayer {
                    translationX = (travel.endCenterX - startRect.center.x) * centerProgress()
                    translationY = (travel.endCenterY - startRect.center.y) * centerProgress()
                    val width = startRect.width + (travel.endWidth - startRect.width) * geometryProgress()
                    val growth = if (startRect.width > 0f) width / startRect.width else 1f
                    scaleX = growth
                    scaleY = growth
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    val fraction = iconProgress()
                    alpha = 1f - fraction
                    val blur = GlassMotion.TRANSFORM_BLUR_PX * fraction
                    renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
                },
            content = { anchorContent() },
        )
    }
}

/** Keeps the closing motion handoff in the anchor lifecycle, separate from drawing. */
@Composable
private fun rememberTransformTravel(
    show: Boolean,
    active: Boolean,
    anchor: GlassPopupAnchor,
    bounds: State<Float>,
    center: State<Float>,
): TransformTravel {
    val currentShow by rememberUpdatedState(show)
    val closeVelocity = remember { CloseVelocityTracker() }
    val travel = remember { TransformTravel() }
    LaunchedEffect(active, show) {
        if (!show && active) {
            var prevBounds = bounds.value
            var prevCenter = center.value
            var prevNanos = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val currBounds = bounds.value
                val currCenter = center.value
                val dt = (now - prevNanos) / 1_000_000_000f
                if (dt > 0f) {
                    closeVelocity.boundsVelocity = (currBounds - prevBounds) / dt
                    closeVelocity.centerVelocity = (currCenter - prevCenter) / dt
                }
                prevBounds = currBounds
                prevCenter = currCenter
                prevNanos = now
            }
        } else {
            closeVelocity.boundsVelocity = 0f
            closeVelocity.centerVelocity = 0f
        }
    }
    DisposableEffect(active, anchor) {
        anchor.contentHidden = active
        if (active) anchor.cancelCloseRebound()
        onDispose {
            anchor.contentHidden = false
            // The root host retires as soon as the anchor is restored; do not hand off twice.
            if (active && !currentShow) {
                anchor.handoffCloseRebound(travel, bounds, center, closeVelocity)
            }
        }
    }
    return travel
}

/** The anchor owns the closing rebound; no additional gesture or motion writer is created. */
private fun GlassPopupAnchor.handoffCloseRebound(
    travel: TransformTravel,
    bounds: State<Float>,
    center: State<Float>,
    closeVelocity: CloseVelocityTracker,
) {
    val anchorRect = containerBounds
    val scaleXFactor = if (anchorRect.width > 0f && travel.endWidth > anchorRect.width) {
        (travel.endWidth / anchorRect.width) - 1f
    } else {
        0f
    }
    val scaleYFactor = if (anchorRect.height > 0f && travel.endHeight > anchorRect.height) {
        (travel.endHeight / anchorRect.height) - 1f
    } else {
        0f
    }
    val offsetX = if (anchorRect.width > 0f) travel.endCenterX - anchorRect.center.x else 0f
    val offsetY = if (anchorRect.height > 0f) travel.endCenterY - anchorRect.center.y else 0f
    startCloseRebound(
        boundsValue = bounds.value,
        boundsVelocity = closeVelocity.boundsVelocity,
        centerValue = center.value,
        centerVelocity = closeVelocity.centerVelocity,
        scaleXFactor = scaleXFactor,
        scaleYFactor = scaleYFactor,
        offsetX = offsetX,
        offsetY = offsetY,
    )
}

/**
 * Geometry written during measurement and read during drawing to align the anchor content.
 * Not snapshot state: writes during measurement must not invalidate that pass.
 */
private class TransformTravel {
    var endCenterX: Float = 0f
    var endCenterY: Float = 0f
    var endWidth: Float = 0f
    var endHeight: Float = 0f
}

private class CloseVelocityTracker {
    var boundsVelocity: Float = 0f
    var centerVelocity: Float = 0f
}
