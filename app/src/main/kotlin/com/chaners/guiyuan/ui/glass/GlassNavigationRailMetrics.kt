// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults

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
    collapsed + (expanded - collapsed) * progress.fastCoerceIn(0f, 1f)

/** A corner that eases from the capsule's half-height to the panel's radius as [progress] rises. */
@Stable
internal class RailMorphCorner(private val progress: () -> Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = with(density) {
        lerp(RailCollapsedCornerRadius.toPx(), RailExpandedCornerRadius.toPx(), progress().fastCoerceIn(0f, 1f))
    }
}

/**
 * Keeps one geometry value on [target]; the native Glass rail may animate window/label
 * target changes, matching upstream. The Liquid rail still owns its single morph clock.
 */
@Composable
internal fun rememberRailMotion(
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
internal fun railRowMotion(position: Int, expanded: Boolean): SpringSpec<Float> {
    val frequency = (if (expanded) 26.75f else 21.5f) / (1f + 0.03f * position)
    return spring(stiffness = frequency * frequency, dampingRatio = 0.97f, visibilityThreshold = 0.001f)
}

/** The panel's height settle: a short rebound when expanding, crisp when collapsing. */
internal fun railHeightMotion(expanded: Boolean, distance: Dp): SpringSpec<Dp> {
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
