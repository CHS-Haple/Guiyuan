// Adapted from compose-miuix-ui Liquid Glass example and AndroidLiquidGlass (Apache-2.0).
package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.runtime.Composable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
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
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import top.yukonga.miuix.kmp.material.EdgeLightSource
import top.yukonga.miuix.kmp.material.EdgePeak
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.sensor.rememberDeviceTilt
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.abs

// Liquid optics adapted from the MIUIX Liquid Glass example (Apache-2.0).
// Guiyuan retains navigation content, gestures and fallback ownership.
private val glassHighlight = EdgeStroke(
    width = 1.dp,
    bevel = 2.dp,
    color = Color.White.copy(alpha = 0.12f),
    primary = EdgeLightSource(0.5f, -0.3f, -0.05f, intensity = 1f),
    secondary = EdgeLightSource(0.5f, 0.8f, -0.5f, intensity = 0.4f),
    peak = EdgePeak.Dual,
)

private const val GRAVITY_THRESHOLD_SQ = 0.01f
private val GRAVITY_STEP = (3.0 * PI / 180.0).toFloat()
private const val TILT_LIGHT_SENSITIVITY = 0.12f
private const val PITCH_SWEEP_PER_RAD = 2f / 3f
private val PITCH_NEUTRAL = (PI / 4).toFloat()
private val PITCH_SWEEP_LIMIT = (30.0 * PI / 180.0).toFloat()

internal data class GlassTilt(val angle: Float, val pitch: Float, val roll: Float)

@Composable
internal fun rememberGlassTilt(interactive: Boolean): State<GlassTilt> {
    if (!interactive) {
        return remember { mutableStateOf(GlassTilt((-PI / 2).toFloat(), PITCH_NEUTRAL, 0f)) }
    }
    // Reuse MIUIX's smoothed, lifecycle-owned sensor state; previews stay static.
    val tilt = rememberDeviceTilt()
    return remember(tilt) {
        derivedStateOf {
            val pose = tilt.value
            val validGravity =
                pose.gravityX * pose.gravityX + pose.gravityY * pose.gravityY > GRAVITY_THRESHOLD_SQ
            GlassTilt(
                if (validGravity) {
                    (atan2(pose.gravityY, pose.gravityX) / GRAVITY_STEP).roundToInt() * GRAVITY_STEP
                } else {
                    (-PI / 2).toFloat()
                },
                if (validGravity) {
                    (pose.pitch / GRAVITY_STEP).roundToInt() * GRAVITY_STEP
                } else {
                    PITCH_NEUTRAL
                },
                if (validGravity) {
                    (pose.roll / GRAVITY_STEP).roundToInt() * GRAVITY_STEP
                } else {
                    0f
                },
            )
        }
    }
}

internal fun litHighlight(tilt: GlassTilt, offsetDegrees: Float): EdgeStroke {
    // Pitch spans ±90°; center the light sweep at 45° to avoid saturating normal upright use.
    val pitchSweep =
        ((abs(tilt.pitch) - PITCH_NEUTRAL) * PITCH_SWEEP_PER_RAD)
            .coerceIn(-PITCH_SWEEP_LIMIT, PITCH_SWEEP_LIMIT)
    val rad = tilt.angle + (offsetDegrees * PI / 180.0).toFloat() + pitchSweep
    val dx = (tilt.roll * TILT_LIGHT_SENSITIVITY).coerceIn(-0.12f, 0.12f)
    return glassHighlight.copy(
        primary = glassHighlight.primary.copy(
            x = 0.5f + cos(rad) + dx,
            y = 0.7f + sin(rad),
        ),
    )
}

internal data class LiquidParams(
    val blur: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val surfaceAlpha: Float,
)

internal fun LiquidMode.params(): LiquidParams =
    when (this) {
        LiquidMode.Blur ->
            LiquidParams(
                blur = 8.dp,
                refractionHeight = 24.dp,
                refractionAmount = 24.dp,
                surfaceAlpha = 0.40f,
            )
        LiquidMode.Balanced ->
            LiquidParams(
                blur = 4.dp,
                refractionHeight = 24.dp,
                refractionAmount = 24.dp,
                surfaceAlpha = 0.40f,
            )
        LiquidMode.Clear ->
            LiquidParams(
                blur = 2.dp,
                refractionHeight = 24.dp,
                refractionAmount = 24.dp,
                surfaceAlpha = 0.30f,
            )
    }

@Composable
internal fun rememberLiquidNavBackdrop(surfaceColor: Color): LayerBackdrop =
    rememberLayerBackdrop(onDrawUnderlay = { drawRect(surfaceColor) })

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
 * Liquid material adapter for OS4 Glass rail and independent navigation actions.
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
            lens(
                params.refractionHeight.toPx(),
                params.refractionAmount.toPx(),
            )
        },
        onDrawFront = {
            // Match portrait Liquid's panel optics; the rail owns its panel silhouette.
            drawEdgeStroke(shape = shape, stroke = rim.value, alpha = 0.75f)
        },
        onDrawSurface = {
            drawRect(color.copy(alpha = params.surfaceAlpha))
        },
    )
}

/**
 * Portrait Liquid navigation and OS4 Glass Rail share these optical coefficients.
 * Geometry and gesture ownership remain with the corresponding MIUIX navigation style.
 */
internal data class LiquidSelectionOptics(
    val lensHeight: Dp,
    val lensAmount: Dp,
    val rimAlpha: Float,
    val shadowRadius: Dp,
    val shadowAlpha: Float,
    val restAlpha: Float,
    val pressedTintAlpha: Float,
)

internal fun liquidSelectionOptics(progress: Float): LiquidSelectionOptics {
    val fraction = progress.coerceIn(0f, 1f)
    return LiquidSelectionOptics(
        lensHeight = 10.dp * fraction,
        lensAmount = 14.dp * fraction,
        rimAlpha = fraction,
        shadowRadius = 8.dp * fraction,
        shadowAlpha = fraction,
        restAlpha = 1f - fraction,
        pressedTintAlpha = 0.03f * fraction,
    )
}

/** Material-only selected lens for OS4 Glass Rail, driven by its existing press state. */
@Composable
internal fun Modifier.liquidRailSelectedSurface(
    backdrop: Backdrop?,
    shape: Shape,
    pressed: Boolean,
): Modifier {
    if (backdrop == null) return this
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
            val optics = liquidSelectionOptics(progress)
            // As on the portrait bar, idle selection is a calm tint and the
            // lens refraction builds only while pressed, not permanently.
            lens(
                optics.lensHeight.toPx(),
                optics.lensAmount.toPx(),
                depthEffect = true,
                chromaticAberration = 0.5f,
            )
        },
        onDrawSurface = {
            val optics = liquidSelectionOptics(progress)
            drawRect(
                if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f),
                alpha = optics.restAlpha,
            )
            drawRect(Color.Black.copy(alpha = optics.pressedTintAlpha))
        },
        onDrawFront = {
            drawEdgeStroke(shape = shape, stroke = rim.value, alpha = liquidSelectionOptics(progress).rimAlpha)
        },
    ).innerShadow(shape) {
        val optics = liquidSelectionOptics(progress)
        InnerShadow(
            radius = optics.shadowRadius,
            color = Color.Black.copy(alpha = 0.15f),
            alpha = optics.shadowAlpha,
        )
    }
}
