package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.color.space.Hsv

// MIUIX ColorPalette always exposes alpha; battery colors only support opaque RGB.
private const val ROWS = 7
private const val COLS = 13

@Composable
internal fun BatteryOpaquePalette(
    color: Color,
    onPreview: (Color) -> Unit,
    onCommit: (Color) -> Unit,
) {
    val colors = remember {
        val saturation = floatArrayOf(10f, 35f, 70f, 100f, 100f, 100f, 100f)
        val value = floatArrayOf(100f, 100f, 100f, 85f, 65f, 45f, 20f)
        List(ROWS * COLS) { index ->
            val row = index / COLS
            val col = index % COLS
            if (col == COLS - 1) {
                Hsv(0f, 0f, 100f * (1f - row / (ROWS - 1f))).toColor()
            } else {
                Hsv(col * 30f, saturation[row], value[row]).toColor()
            }
        }
    }
    val selected = remember(color, colors) {
        colors.indices.minByOrNull { i ->
            val sample = colors[i]
            val r = sample.red - color.red
            val g = sample.green - color.green
            val b = sample.blue - color.blue
            r * r + g * g + b * b
        } ?: 0
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val preview by rememberUpdatedState(onPreview)
    val commit by rememberUpdatedState(onCommit)
    val currentColor by rememberUpdatedState(color)

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(rtl) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (size.width == 0 || size.height == 0) return@awaitEachGesture

                        fun cell(x: Float, y: Float): Int {
                            val col = ((x.coerceIn(0f, size.width - 1f) / size.width) * COLS)
                                .toInt().coerceIn(0, COLS - 1)
                            val row = ((y.coerceIn(0f, size.height - 1f) / size.height) * ROWS)
                                .toInt().coerceIn(0, ROWS - 1)
                            return row * COLS + if (rtl) COLS - 1 - col else col
                        }

                        val before = currentColor
                        var current = cell(down.position.x, down.position.y)
                        preview(colors[current])
                        var saved = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!change.isConsumed) {
                                    commit(colors[current])
                                    saved = true
                                }
                                break
                            }
                            val next = cell(change.position.x, change.position.y)
                            if (next != current) {
                                current = next
                                preview(colors[next])
                            }
                            change.consume()
                        }
                        if (!saved) preview(before)
                    }
                },
    ) {
        val cellW = size.width / COLS
        val cellH = size.height / ROWS
        colors.forEachIndexed { i, sample ->
            val row = i / COLS
            val col = i % COLS
            val visualCol = if (rtl) COLS - 1 - col else col
            drawRect(
                color = sample,
                topLeft = Offset(visualCol * cellW, row * cellH),
                size = Size(cellW + 0.5f, cellH + 0.5f),
            )
        }
        val row = selected / COLS
        val col = selected % COLS
        val visualCol = if (rtl) COLS - 1 - col else col
        val center = Offset((visualCol + 0.5f) * cellW, (row + 0.5f) * cellH)
        drawCircle(
            color = Color.Black.copy(alpha = 0.24f),
            radius = 11.dp.toPx(),
            center = center,
            style = Stroke(width = 4.dp.toPx()),
        )
        drawCircle(
            color = Color.White,
            radius = 10.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
