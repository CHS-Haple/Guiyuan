package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.color.api.toHsv

// The pinned MIUIX ColorPalette always includes alpha. Battery colors are opaque.
@Composable
internal fun BatteryColorField(
    color: Color?,
    hue: Float,
    onPreview: (Color?, Float) -> Unit,
    onSelect: (Color, Float) -> Unit,
) {
    val onPreviewNow by rememberUpdatedState(onPreview)
    val onSelectNow by rememberUpdatedState(onSelect)
    val colorNow by rememberUpdatedState(color)
    val hueNow by rememberUpdatedState(hue)
    val paletteDescription = stringResource(R.string.battery_color_visual_picker)

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .semantics { contentDescription = paletteDescription }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (size.width <= 0 || size.height <= 0) return@awaitEachGesture

                        fun sample(point: Offset): Pair<Color, Float> {
                            val hue = (point.x / size.width).coerceIn(0f, 1f) * 359.99f
                            val saturation = (point.y / size.height).coerceIn(0f, 1f)
                            return Color.hsv(hue, saturation, 1f) to hue
                        }

                        val before = colorNow
                        val beforeHue = hueNow
                        var last = sample(down.position)
                        onPreviewNow(last.first, last.second)
                        var applied = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!change.isConsumed) {
                                    onSelectNow(last.first, last.second)
                                    applied = true
                                }
                                break
                            }
                            if (change.isConsumed) break
                            val next = sample(change.position)
                            if (next != last) {
                                last = next
                                onPreviewNow(next.first, next.second)
                            }
                            change.consume()
                        }
                        if (!applied) onPreviewNow(before, beforeHue)
                    }
                },
    ) {
        val round = CornerRadius(16.dp.toPx())
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red,
                ),
            ),
            cornerRadius = round,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.White, Color.Transparent)),
            cornerRadius = round,
        )

        color?.toHsv()?.let { hsv ->
            val radius = 11.dp.toPx()
            val center =
                Offset(
                    (hue / 360f * size.width).coerceIn(radius, (size.width - radius).coerceAtLeast(radius)),
                    (hsv.s / 100f * size.height).coerceIn(radius, (size.height - radius).coerceAtLeast(radius)),
                )
            drawCircle(
                color = Color.Black.copy(alpha = 0.28f),
                radius = radius + 1.dp.toPx(),
                center = center,
                style = Stroke(width = 4.dp.toPx()),
            )
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}
