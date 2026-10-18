package com.chaners.guiyuan.xposed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

// Observes the verified native 5G font's 'wght' axis, without changing live drawing.
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
                textAlign = Paint.Align.LEFT
                clearShadowLayer()
            }
            val nativeBounds = Rect()
            val nativeInk = raster(bitmap, canvas, pixels, nativeStyle, nativeBounds)
            if (nativeInk <= 0L) return null
            val reference = pixels.copyOf()
            // Typeface.create() selected two static outlines on this device.
            // Test the font's real 'wght' axis independently, without switching faces.
            val samples = intArrayOf(
                400, 450, 500, 550, 600, 660, 700, 750, 800, 850, 900, 950, 1000,
            )
            val style = Paint(nativeStyle)
            val variations = samples.joinToString(",") { weight ->
                style.typeface = nativeFace
                val recognized = style.setFontVariationSettings("'wght' $weight")
                val bounds = Rect()
                val ink = raster(bitmap, canvas, pixels, style, bounds)
                var alphaDelta = 0L
                for (i in pixels.indices) {
                    val expected = (reference[i] ushr 24) and 255
                    val observed = (pixels[i] ushr 24) and 255
                    alphaDelta += kotlin.math.abs(expected - observed)
                }
                // On newer Android, 'recognized' may be true without a usable axis.
                // Raster ink and pixel delta, not the API boolean, are authoritative.
                val difference = alphaDelta * 100L / nativeInk
                "$weight:$recognized:$difference:$ink:${bounds.width()}x${bounds.height()}"
            }
            return "nativeWeight=${nativeFace.weight}:size=$nativeSize:" +
                "nativeInk=$nativeInk:nativeBounds=${nativeBounds.width()}x${nativeBounds.height()}:" +
                "axisWght=[$variations]"
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
