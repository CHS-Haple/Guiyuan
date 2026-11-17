// Adapted from compose-miuix-ui Liquid Glass example and AndroidLiquidGlass (Apache-2.0).
package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.runtime.Composable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.layout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
import top.yukonga.miuix.kmp.basic.NavigationRailDefaults
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.roundToPx
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.chaners.guiyuan.settings.LiquidMode
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

internal fun liquidNavSupported(): Boolean = isRuntimeShaderSupported()

@Composable
internal fun rememberLiquidNavBackdrop(surfaceColor: Color): LayerBackdrop =
    rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

internal fun Modifier.liquidNavBackdropSource(backdrop: LayerBackdrop): Modifier =
    layerBackdrop(backdrop)

internal fun BackdropEffectScope.vibrancy() {
    colorControls(brightness = 0f, contrast = 1f, saturation = 1.5f)
}

@Stable
internal class CombinedBackdrop(
    private val first: Backdrop,
    private val second: Backdrop,
) : Backdrop {
    override val isCoordinatesDependent: Boolean =
        first.isCoordinatesDependent || second.isCoordinatesDependent
    override val offsetResidualX: Float get() = first.offsetResidualX
    override val offsetResidualY: Float get() = first.offsetResidualY

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Float,
    ) {
        with(first) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
        with(second) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
    }
}

@Composable
internal fun rememberCombinedBackdrop(first: Backdrop, second: Backdrop): Backdrop =
    remember(first, second) { CombinedBackdrop(first, second) }

/**
 * Material-only adapter for the OS3 native and OS4 Glass navigation rails.
 * Geometry, expansion, selection, motion and semantics stay with the original rail.
 * This surface is outside the recorded page backdrop, never inside its own source.
 */
@Composable
internal fun Modifier.liquidRailSurface(
    backdrop: Backdrop?,
    shape: CornerBasedShape,
    mode: LiquidMode,
): Modifier {
    val color = MiuixTheme.colorScheme.surfaceContainer
    if (backdrop == null) return this.clip(shape).background(color)
    val params = mode.params()
    val tilt = rememberGlassTilt(interactive = true)
    val rim = remember(tilt) { derivedStateOf { litHighlight(tilt.value, -45f) } }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            padding = maxOf(padding, 40.dp.toPx())
            vibrancy()
            blur(params.blur.toPx(), params.blur.toPx())
            lens(params.refractionHeight.toPx(), params.refractionAmount.toPx())
        },
        onDrawFront = {
            drawEdgeStroke(shape = shape, stroke = rim.value, alpha = 0.75f)
        },
        onDrawSurface = {
            drawRect(color.copy(alpha = params.surfaceAlpha))
        },
    )
}

/**
 * The OS3 selection lens uses the upstream NavigationRailItem's measured indicator endpoints.
 * It never owns selection, accessibility, keyboard input or the click recognizer.
 */
internal data class NativeRailSelectionPlacement(
    val start: Dp,
    val top: Dp,
    val width: Dp,
    val height: Dp,
)

internal fun nativeRailSelectionPlacement(itemWidth: Dp): NativeRailSelectionPlacement {
    val defaults = NavigationRailDefaults
    val progress = (
        (itemWidth.value - defaults.MinWidth.value) /
            (defaults.ExpandedWidth.value - defaults.MinWidth.value)
        ).coerceIn(0f, 1f)
    val margin = defaults.ExpandedItemHorizontalMargin
    return NativeRailSelectionPlacement(
        start = margin,
        top = lerp(defaults.ItemVerticalPadding, 0.dp, progress),
        width = lerp(
            defaults.IconSize + defaults.ExpandedItemContentHorizontalPadding * 2,
            (itemWidth - margin * 2).coerceAtLeast(0.dp),
            progress,
        ),
        height = lerp(
            defaults.IconSize + defaults.CollapsedIndicatorVerticalPadding * 2,
            defaults.IconSize + defaults.ExpandedItemContentVerticalPadding * 2,
            progress,
        ),
    )
}

/** Passive read of the upstream pointer: never consume or commit another selection. */
internal fun Modifier.liquidRailPressObserver(onPressChange: (Boolean) -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            onPressChange(true)
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                }
            } finally {
                onPressChange(false)
            }
        }
    }

/**
 * The real lens is sized to the native pill, not to the whole OS3 destination row.
 * The outer matchParentSize node doesn't affect upstream layout or item height.
 */
@Composable
internal fun BoxScope.LiquidNativeRailSelection(
    backdrop: Backdrop,
    mode: LiquidMode,
    pressed: Boolean,
) {
    Box(Modifier.matchParentSize()) {
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val placement = nativeRailSelectionPlacement(constraints.maxWidth.toDp())
                    val child = measurable.measure(
                        androidx.compose.ui.unit.Constraints.fixed(
                            placement.width.roundToPx().coerceAtLeast(0),
                            placement.height.roundToPx().coerceAtLeast(0),
                        ),
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        child.placeRelative(placement.start.roundToPx(), placement.top.roundToPx())
                    }
                }
                .liquidRailSelectedSurface(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(NavigationRailDefaults.ExpandedItemCornerRadius),
                    mode = mode,
                    pressed = pressed,
                ),
        )
    }
}

/**
 * The shared OS3/OS4 selected lens. Its opacity, refraction and rim track the existing
 * selected and pressed state; only the material is new.
 */
@Composable
internal fun Modifier.liquidRailSelectedSurface(
    backdrop: Backdrop?,
    shape: Shape,
    mode: LiquidMode,
    pressed: Boolean,
): Modifier {
    if (backdrop == null) return this
    val params = mode.params()
    val dark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val tilt = rememberGlassTilt(interactive = true)
    val rim = remember(tilt) { derivedStateOf { litHighlight(tilt.value, 90f) } }
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "liquidRailSelectionPress",
    )
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            padding = maxOf(padding, 40.dp.toPx())
            vibrancy()
            blur(params.blur.toPx(), params.blur.toPx())
            lens(
                (6.dp + 8.dp * progress).toPx(),
                (10.dp + 8.dp * progress).toPx(),
                depthEffect = true,
                chromaticAberration = 0.3f,
            )
        },
        onDrawSurface = {
            drawRect(
                if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f),
                alpha = 1f - progress * 0.3f,
            )
            if (progress > 0f) drawRect(Color.White.copy(alpha = 0.06f * progress))
        },
        onDrawFront = {
            drawEdgeStroke(shape = shape, stroke = rim.value, alpha = 0.65f + progress * 0.3f)
        },
    )
}
