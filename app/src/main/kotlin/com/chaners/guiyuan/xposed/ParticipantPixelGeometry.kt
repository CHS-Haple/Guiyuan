package com.chaners.guiyuan.xposed

import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import com.chaners.guiyuan.xposed.ParticipantVisualSnapshot.NormalizedPoint
import com.chaners.guiyuan.xposed.ParticipantVisualSnapshot.NormalizedRect

internal object ParticipantPixelGeometry {
    private const val ALPHA_THRESHOLD = 8

    internal fun resolveAlphaWeightedCenter(
        pixels: IntArray,
        width: Int,
        height: Int,
    ): NormalizedPoint? {
        if (width <= 0 || height <= 0) return null
        val expectedSize = width.toLong() * height.toLong()
        if (expectedSize > pixels.size.toLong()) return null

        var totalAlpha = 0.0
        var weightedX = 0.0
        var weightedY = 0.0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = (pixels[y * width + x] ushr 24) and 0xff
                if (alpha <= ALPHA_THRESHOLD) continue
                val weight = alpha.toDouble()
                totalAlpha += weight
                weightedX += (x + 0.5) * weight
                weightedY += (y + 0.5) * weight
            }
        }
        if (totalAlpha <= 0.0) return null

        return NormalizedPoint(
            x = (weightedX / totalAlpha / width).toFloat().coerceIn(0f, 1f),
            y = (weightedY / totalAlpha / height).toFloat().coerceIn(0f, 1f),
        )
    }

    internal fun connectedComponents(
        pixels: IntArray,
        width: Int,
        height: Int,
    ): List<NormalizedRect> {
        val active =
            BooleanArray(pixels.size) { index ->
                Color.alpha(pixels[index]) > ALPHA_THRESHOLD
            }
        val visited = BooleanArray(pixels.size)
        val stack = IntArray(pixels.size)
        val components = ArrayList<NormalizedRect>()

        for (start in active.indices) {
            if (!active[start] || visited[start]) continue
            var top = 0
            stack[top++] = start
            visited[start] = true
            var minX = width
            var minY = height
            var maxX = -1
            var maxY = -1

            while (top > 0) {
                val index = stack[--top]
                val x = index % width
                val y = index / width
                minX = min(minX, x)
                minY = min(minY, y)
                maxX = max(maxX, x)
                maxY = max(maxY, y)

                fun push(next: Int) {
                    if (
                        next >= 0 &&
                        next < active.size &&
                        active[next] &&
                        !visited[next]
                    ) {
                        visited[next] = true
                        stack[top++] = next
                    }
                }

                if (x > 0) push(index - 1)
                if (x + 1 < width) push(index + 1)
                if (y > 0) push(index - width)
                if (y + 1 < height) push(index + width)
            }

            if (maxX >= minX && maxY >= minY) {
                components +=
                    NormalizedRect(
                        left = minX / width.toFloat(),
                        top = minY / height.toFloat(),
                        right = (maxX + 1) / width.toFloat(),
                        bottom = (maxY + 1) / height.toFloat(),
                    )
            }
        }
        return components
    }
}
