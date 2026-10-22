package com.chaners.guiyuan.xposed

import kotlin.math.max
import kotlin.math.min

// Path.approximate supplies (fraction, x, y) triples; repeated fractions mark contour moves.
internal object MobileGlyphFit {
    fun contactOffset(
        main: FloatArray,
        suffix: FloatArray,
        suffixY: Float,
        clearance: Float,
    ): Float? {
        if (
            main.size < 6 || suffix.size < 6 ||
            main.size % 3 != 0 || suffix.size % 3 != 0 ||
            main.size > 2048 || suffix.size > 2048 ||
            !suffixY.isFinite() || !clearance.isFinite() ||
            !main.all(Float::isFinite) || !suffix.all(Float::isFinite)
        ) return null

        var offset = Float.NEGATIVE_INFINITY
        fun sample(y: Float) {
            val right = edgeAt(main, y, right = true) ?: return
            val left = edgeAt(suffix, y - suffixY, right = false) ?: return
            offset = max(offset, right - left)
        }

        // For linearized contours the extreme envelope separation occurs at a vertex height.
        for (i in main.indices step 3) sample(main[i + 2])
        for (i in suffix.indices step 3) sample(suffix[i + 2] + suffixY)
        return offset.takeIf(Float::isFinite)?.plus(clearance.coerceAtLeast(0f))
            ?.takeIf(Float::isFinite)
    }

    private fun edgeAt(points: FloatArray, y: Float, right: Boolean): Float? {
        var result = if (right) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY
        for (i in 0 until points.size - 3 step 3) {
            if (points[i + 3] <= points[i]) continue
            val y0 = points[i + 2]
            val y1 = points[i + 5]
            if (y < min(y0, y1) - 0.0001f || y > max(y0, y1) + 0.0001f) continue
            val x0 = points[i + 1]
            val x1 = points[i + 4]
            val x =
                if (y0 == y1) {
                    if (right) max(x0, x1) else min(x0, x1)
                } else {
                    x0 + (x1 - x0) * ((y - y0) / (y1 - y0)).coerceIn(0f, 1f)
                }
            result = if (right) max(result, x) else min(result, x)
        }
        return result.takeIf(Float::isFinite)
    }
}
