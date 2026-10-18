package com.chaners.guiyuan.xposed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface

// A bounded glyph-ink comparison, not a font-weight calibration or replacement renderer.
internal object NativeFontInkProbe {
    private const val TEXT = "5G"
    private const val WIDTH = 96
    private const val HEIGHT = 64

    fun compare(native: Paint): String? {
        val nativeFace = native.typeface ?: return null
        val nativeSize = native.textSize.takeIf { it.isFinite() && it > 0f } ?: return null
        val size = nativeSize.coerceAtMost(48f)
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val pixels = IntArray(WIDTH * HEIGHT)
            val nativeStyle = Paint(native).apply {
                textSize = size
                color = Color.WHITE
                alpha = 255
                isAntiAlias = true
                clearShadowLayer()
            }
            val nativeBounds = Rect()
            val nativeInk = raster(bitmap, canvas, pixels, nativeStyle, nativeBounds)
            if (nativeInk <= 0L) return null
            val reference = pixels.copyOf()
            val weights = sortedSetOf(400, 500, nativeFace.weight, 800, 950, 1000)
                .filter { it in 1..1000 }
            val source = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                color = Color.WHITE
            }
            fun candidates(base: Typeface, style: Paint): String =
                weights.joinToString(",") { weight ->
                    style.typeface = Typeface.create(base, weight, base.isItalic)
                    val bounds = Rect()
                    val ink = raster(bitmap, canvas, pixels, style, bounds)
                    var alphaDelta = 0L
                    for (i in pixels.indices) {
                        val expected = (reference[i] ushr 24) and 255
                        val observed = (pixels[i] ushr 24) and 255
                        alphaDelta += kotlin.math.abs(expected - observed)
                    }
                    // Actual raster alpha difference relative to native ink;
                    // smaller is closer, but not a visual acceptance score.
                    val difference = alphaDelta * 100L / nativeInk
                    "$weight:$difference:$ink:${bounds.width()}x${bounds.height()}"
                }
            val sourceResults = candidates(Typeface.DEFAULT, source)
            val nativeResults = candidates(nativeFace, Paint(nativeStyle))
            return "nativeWeight=${nativeFace.weight}:size=$nativeSize:" +
                "nativeInk=$nativeInk:nativeBounds=${nativeBounds.width()}x${nativeBounds.height()}:" +
                "deltaPer100Ink-default=[$sourceResults]:" +
                "deltaPer100Ink-native=[$nativeResults]"
        } finally {
            bitmap.recycle()
        }
    }

    private fun raster(
        bitmap: Bitmap,
        canvas: Canvas,
        pixels: IntArray,
        paint: Paint,
        bounds: Rect,
    ): Long {
        paint.getTextBounds(TEXT, 0, TEXT.length, bounds)
        bitmap.eraseColor(Color.TRANSPARENT)
        canvas.drawText(
            TEXT,
            WIDTH / 2f - (bounds.left + bounds.right) / 2f,
            HEIGHT / 2f - (bounds.top + bounds.bottom) / 2f,
            paint,
        )
        bitmap.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        var ink = 0L
        for (pixel in pixels) ink += ((pixel ushr 24) and 255)
        return ink
    }
}
