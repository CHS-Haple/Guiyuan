package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
internal fun BatteryColorGrid(
    color: Color,
    onPreview: (Color) -> Unit,
    onSelect: (Color) -> Unit,
) {
    val rows = 7
    val columns = 13 // 12 hue columns and one grayscale column
    val palette = remember {
        List(rows) { row ->
            List(columns) { column ->
                if (column == 12) {
                    Color.hsv(0f, 0f, (1f - row * 0.14f).coerceAtLeast(0.16f))
                } else {
                    val saturation = (0.25f + row.coerceAtMost(4) * 0.1875f).coerceAtMost(1f)
                    val value = when (row) {
                        5 -> 0.73f
                        6 -> 0.48f
                        else -> 1f
                    }
                    Color.hsv(column * 30f, saturation, value)
                }
            }
        }
    }
    val onPreviewCurrent by rememberUpdatedState(onPreview)
    val onSelectCurrent by rememberUpdatedState(onSelect)
    val currentColor by rememberUpdatedState(color)
    val selected = remember(color) {
        var closest = 0 to 0
        var distance = Float.MAX_VALUE
        palette.forEachIndexed { row, cells ->
            cells.forEachIndexed { column, sample ->
                val d =
                    (sample.red - color.red) * (sample.red - color.red) +
                    (sample.green - color.green) * (sample.green - color.green) +
                    (sample.blue - color.blue) * (sample.blue - color.blue)
                if (d < distance) {
                    distance = d
                    closest = row to column
                }
            }
        }
        closest
    }
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(168.dp)
                .pointerInput(palette) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var lastCell: Pair<Int, Int>? = null
                        fun previewCell(position: Offset) {
                            val column = (position.x / size.width * columns).toInt().coerceIn(0, columns - 1)
                            val row = (position.y / size.height * rows).toInt().coerceIn(0, rows - 1)
                            if (lastCell != (row to column)) {
                                lastCell = row to column
                                onPreviewCurrent(palette[row][column])
                            }
                        }
                        val before = currentColor
                        previewCell(down.position)
                        var applied = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!change.isConsumed) {
                                    lastCell?.let { (row, column) -> onSelectCurrent(palette[row][column]) }
                                    applied = true
                                }
                                break
                            }
                            previewCell(change.position)
                            change.consume()
                        }
                        if (!applied) onPreviewCurrent(before)
                    }
                },
    ) {
        val cellWidth = size.width / columns
        val cellHeight = size.height / rows
        val gap = 2.dp.toPx()
        palette.forEachIndexed { row, cells ->
            cells.forEachIndexed { column, sample ->
                drawRoundRect(
                    color = sample,
                    topLeft = Offset(column * cellWidth + gap, row * cellHeight + gap),
                    size = Size(cellWidth - gap * 2f, cellHeight - gap * 2f),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
            }
        }
        val center =
            Offset((selected.second + 0.5f) * cellWidth, (selected.first + 0.5f) * cellHeight)
        drawCircle(color = Color.White, radius = 10.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
        drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = 12.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
    }
}
