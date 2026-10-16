package com.chaners.guiyuan.xposed

import kotlin.math.abs
import kotlin.math.roundToInt

// Operates on the native icon's alpha only; keep its geometry and state markers intact.
internal object WifiStrokeMask {
    fun duringTransition(weight: Int, progress: Float): Int =
        (weight + (100 - weight) * progress.coerceIn(0f, 1f)).roundToInt()

    fun adjust(pixels: IntArray, width: Int, height: Int, weight: Int): IntArray {
        require(width > 0 && height > 0 && width * height == pixels.size)
        val amount = abs(weight.coerceIn(80, 120) - 100) / 20f
        if (amount <= 0f) return pixels.copyOf()
        val thicker = weight > 100
        return IntArray(pixels.size) { index ->
            val x = index % width
            val y = index / width
            val alpha = pixels[index] ushr 24
            var extreme = if (thicker) 0 else 255
            for (dy in -1..1) {
                val ny = y + dy
                if (ny !in 0 until height) {
                    if (!thicker) extreme = 0
                    continue
                }
                for (dx in -1..1) {
                    val nx = x + dx
                    val a = if (nx in 0 until width) pixels[ny * width + nx] ushr 24 else 0
                    extreme = if (thicker) maxOf(extreme, a) else minOf(extreme, a)
                }
            }
            val adjusted = (alpha + (extreme - alpha) * amount).roundToInt().coerceIn(0, 255)
            (adjusted shl 24) or 0x00FFFFFF
        }
    }
}
