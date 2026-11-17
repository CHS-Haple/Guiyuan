// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import com.chaners.guiyuan.ui.components.liquid.LiquidFeedbackSpec

/**
 * The selected destination's own surface, shared by the glass navigation bar and
 * [GlassNavigationRail].
 *
 * It is only ever a tint of the glass under it: this composable draws a capsule and owns the press
 * feedback (the fill and the pressed scale). Size and placement belong to the caller, so the same
 * surface can hug an icon, stretch across a destination or travel between rows.
 *
 * @param pressed Whether the destination the indicator covers is held down.
 * @param modifier The modifier sizing and placing the indicator.
 * @param height The indicator's own height, used to derive the pressed scale.
 * @param color Resting fill.
 * @param pressedColor Fill while the destination is held down.
 * @param scaleOnPress Whether a press scales the indicator down. Disable it when the press belongs
 *   to a row the indicator only spans visually, so the press feedback does not move the indicator.
 */
/** The pill's two colors and press policy have one rendering owner. */
internal class GlassIndicatorAppearance(
    val color: Color,
    val pressedColor: Color,
    val scaleOnPress: Boolean = true,
    val liquidPaint: (@Composable (Boolean) -> Modifier)? = null,
    val dragStretch: () -> Float = { 0f },
)

@Composable
internal fun GlassNavigationIndicator(
    pressed: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = GlassNavigationBarDefaults.Height - GlassNavigationBarDefaults.IndicatorPaddingVertical * 2,
    appearance: GlassIndicatorAppearance = GlassIndicatorAppearance(
        color = GlassNavigationBarDefaults.indicatorColor(),
        pressedColor = GlassNavigationBarDefaults.indicatorPressedColor(),
    ),
) {
    val density = LocalDensity.current
    val scale by animateFloatAsState(
        targetValue = when {
            pressed && appearance.liquidPaint != null -> LiquidFeedbackSpec.PressedLensScale
            pressed && appearance.scaleOnPress ->
                glassPressScale(with(density) { height.toPx() }, density.density)
            else -> 1f
        },
        animationSpec = if (appearance.liquidPaint != null) {
            spring(dampingRatio = 0.7f, stiffness = 300f)
        } else if (pressed) GlassMotion.pressDown() else GlassMotion.pressUp(),
        label = "glassNavigationIndicatorPress",
    )
    val fill = animateColorAsState(
        targetValue = if (pressed) appearance.pressedColor else appearance.color,
        animationSpec = if (pressed) GlassMotion.navPressEnter() else GlassMotion.navPressExit(),
        label = "glassNavigationIndicatorFill",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                if (appearance.liquidPaint != null) {
                    // Portrait: a 56dp selection grows to 78dp on press.
                    // Keep physical growth, not width-proportional scaling,
                    // when the OS4 pill morphs from compact to a full row.
                    val fraction = ((scale - 1f) /
                        (LiquidFeedbackSpec.PressedLensScale - 1f)).coerceIn(0f, 1f)
                    val growth = 22.dp.toPx() * fraction
                    val velocity = abs(appearance.dragStretch().coerceIn(-0.25f, 0.25f))
                    scaleX = (1f + growth / size.width.coerceAtLeast(1f)) *
                        (1f - velocity * 0.12f)
                    scaleY = (1f + growth / size.height.coerceAtLeast(1f)) *
                        (1f + velocity * 0.25f)
                } else {
                    scaleX = scale
                    scaleY = scale
                }
            }
            .then(appearance.liquidPaint?.invoke(pressed) ?:
                Modifier.clip(RoundedCornerShape(percent = 50))
                    .drawBehind { drawRect(fill.value) }),
    )
}
