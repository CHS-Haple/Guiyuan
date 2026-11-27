// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import top.yukonga.miuix.kmp.glass.GlassPopupFrame

import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupSizing

import top.yukonga.miuix.kmp.glass.GlassMotion

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.first
import top.yukonga.miuix.kmp.blur.Backdrop

/**
 * A glass submenu that expands from a primary menu row, aligned to its start edge in LTR and RTL.
 *
 * Rows retain their size during the transition and stop accepting input as soon as dismissal is
 * requested. Predictive Back previews collapse, restores on cancellation and dismisses on completion.
 *
 * @param show Whether the secondary menu is expanded. Keep this call composed during collapse.
 * @param onDismissRequest Called by a tap outside or Back to return to the primary menu.
 * @param anchorBounds Resting trigger-row bounds in window coordinates (`boundsInWindow()`).
 *   Freeze them before setting the primary [GlassTransformPopup]'s `stacked` flag.
 * @param backdrop Background sampled by the menu. Include the primary popup but exclude this
 *   submenu. `null` uses the anchor's backdrop.
 * @param materialAnchor The primary popup's button anchor, shared across both levels.
 * @param sizing Panel limits. Set `minWidth` to the primary panel's measured width.
 * @param onDismissFinished Called when the collapse animation has finished.
 * @param content Header row followed by secondary choices. The header should request collapse.
 */
@Composable
fun BoxScope.GlassSecondaryPopup(
    show: Boolean,
    onDismissRequest: () -> Unit,
    anchorBounds: Rect,
    backdrop: Backdrop?,
    materialAnchor: GlassPopupAnchor,
    sizing: GlassPopupSizing = GlassPopupSizing(),
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val contentPadding = PaddingValues(vertical = GlassPopupDefaults.ContentPaddingVertical)
    val visuals = GlassPopupDefaults.visuals()
    val state = remember { MutableTransitionState(false) }
    state.targetState = show
    val transition = rememberTransition(state, label = "glassSecondaryPopup")
    val progress = transition.animateFloat(
        transitionSpec = { GlassMotion.secondaryPopup(targetState) },
        label = "glassSecondaryBounds",
    ) { if (it) 1f else 0f }
    val active = show || state.currentState || !state.isIdle
    val backProgress = rememberSecondaryPopupBackProgress(show, active, onDismissRequest, materialAnchor)
    ObserveSecondaryPopupFinish(show, state, onDismissFinished)
    if (!active) return
    val direction = LocalLayoutDirection.current
    val source = materialAnchor.surface
    val resolvedVisuals = materialAnchor.popupVisuals ?: visuals
    GlassPopupSurface(
        onDismissRequest = onDismissRequest,
        sizing = sizing,
        panelLayer = {},
        overlay = {},
        frame = { end, windowBounds ->
            val settled = placeGlassSecondaryPopup(anchorBounds, end, sizing.safeMargin.toPx(), windowBounds, direction)
            GlassPopupFrame(
                secondaryPopupRect(
                    anchorBounds,
                    settled,
                    contentPadding.calculateTopPadding().toPx(),
                    contentPadding.calculateBottomPadding().toPx(),
                    popupFractionWithBack(progress.value, backProgress.value),
                ),
                GlassPopupDefaults.CornerRadius,
            )
        },
        contentLayer = {},
        surface = { shape -> GlassPopupBackground(backdrop ?: source?.backdrop, shape, resolvedVisuals) },
        interactive = show,
        content = content,
    )
}

/** Shares the one predictive Back owner with its primary anchor until detachment. */
@Composable
private fun rememberSecondaryPopupBackProgress(
    show: Boolean,
    active: Boolean,
    onDismissRequest: () -> Unit,
    materialAnchor: GlassPopupAnchor,
): androidx.compose.runtime.State<Float> {
    val backProgress = rememberGlassPopupBackProgress(
        show = show,
        active = active,
        enabled = show,
        resetSpec = GlassMotion.secondaryPopup(true),
        onDismissRequest = onDismissRequest,
    )
    DisposableEffect(materialAnchor, backProgress) {
        materialAnchor.secondaryBackProgressState = backProgress
        onDispose {
            if (materialAnchor.secondaryBackProgressState === backProgress) {
                materialAnchor.secondaryBackProgressState = null
            }
        }
    }
    return backProgress
}

// One effect owner for completion; the panel may remain composed during its exit.
@Composable
private fun ObserveSecondaryPopupFinish(
    show: Boolean,
    state: MutableTransitionState<Boolean>,
    onDismissFinished: (() -> Unit)?,
) {
    val onFinished by rememberUpdatedState(onDismissFinished)
    var hasOpened by remember { mutableStateOf(false) }
    LaunchedEffect(show) {
        if (show) {
            hasOpened = true
        } else if (hasOpened) {
            snapshotFlow { state.isIdle }.first { it }
            hasOpened = false
            onFinished?.invoke()
        }
    }
}

/** Start-edge alignment, then a shift upward to fit the window's safe area. */
internal fun placeGlassSecondaryPopup(anchor: Rect, size: Size, margin: Float, window: Rect, direction: LayoutDirection): Rect {
    val width = size.width.coerceAtMost((window.width - margin * 2f).coerceAtLeast(0f))
    val height = size.height.coerceAtMost((window.height - margin * 2f).coerceAtLeast(0f))
    val x = if (direction == LayoutDirection.Ltr) anchor.left else anchor.right - width
    val minLeft = window.left + margin
    val left = x.coerceIn(minLeft, (window.right - margin - width).coerceAtLeast(minLeft))
    val minTop = window.top + margin
    val top = anchor.top.coerceIn(minTop, (window.bottom - margin - height).coerceAtLeast(minTop))
    return Rect(left, top, left + width, top + height)
}

/** Full-size content moves with a rounded clip grown from the padded trigger row. */
internal fun secondaryPopupRect(anchor: Rect, end: Rect, topPadding: Float, bottomPadding: Float, fraction: Float): Rect {
    val t = fraction.coerceIn(0f, 1f)
    return Rect(
        lerp(anchor.left, end.left, t),
        lerp(anchor.top - topPadding, end.top, t),
        lerp(anchor.right, end.right, t),
        lerp(anchor.bottom + bottomPadding, end.bottom, t),
    )
}
