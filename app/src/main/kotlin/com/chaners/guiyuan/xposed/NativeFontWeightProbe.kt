package com.chaners.guiyuan.xposed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.text.PositionedGlyphs
import android.graphics.text.TextRunShaper
import android.os.Build
import com.chaners.guiyuan.settings.FontAxes
import com.chaners.guiyuan.settings.FontWeightAxis
import com.chaners.guiyuan.settings.NativeFontAxisPolicy
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

// Only invoked at the existing native font report boundary, never during drawing.
internal object NativeFontWeightProbe {
    private const val MAX_FONT_BYTES = 64 * 1024 * 1024
    private const val MOBILE_GLYPHS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+-_"

    fun mobileCoverage(label: String): String? =
        if (label.length <= 24) (MOBILE_GLYPHS + label).toSet().joinToString("") else null

    fun mobile(face: Typeface, coverage: String): FontWeightAxis? =
        probe(face, coverage, "5GA")

    fun battery(face: Typeface): FontWeightAxis? = probe(face, "0123456789", "0123456789")

    private fun probe(face: Typeface, coverage: String, sample: String): FontWeightAxis? =
        SysUiPresentationRuntime.optionalNativeRead {
            if (Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.BAKLAVA_1) {
                return@optionalNativeRead null
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = face
                textSize = 32f
                color = Color.BLACK
            }
            val original = shape(coverage, paint)
            val axis = commonAxis(original) ?: return@optionalNativeRead null
            val low = ceil(axis.min.toDouble()).toInt()
            val high = floor(axis.max.toDouble()).toInt()
            val middle = low + (high - low) / 2
            if (middle <= low || middle >= high) return@optionalNativeRead null
            if (listOf(low, middle, high).any { weight ->
                    paint.setFontVariationOverride("'wght' $weight")
                    !matches(original, shape(coverage, paint), weight)
                }
            ) return@optionalNativeRead null
            axis.takeIf { distinctInk(paint, sample, low, middle, high) }
        }

    private fun shape(text: String, paint: Paint): PositionedGlyphs =
        TextRunShaper.shapeTextRun(text, 0, text.length, 0, text.length, 0f, 0f, false, paint)

    // Every tested glyph must have the same usable raw range; do not borrow a fallback face's axis.
    private fun commonAxis(glyphs: PositionedGlyphs): FontWeightAxis? {
        if (glyphs.glyphCount() == 0) return null
        val axes = mutableMapOf<Pair<Int, Int>, FontWeightAxis>()
        for (index in 0 until glyphs.glyphCount()) {
            if (glyphs.getGlyphId(index) == 0) return null
            val font = glyphs.getFont(index)
            val key = font.sourceIdentifier to font.ttcIndex
            if (key in axes) continue
            val buffer = font.buffer
            if (buffer.remaining() !in 1..MAX_FONT_BYTES) return null
            val axis = NativeFontAxisPolicy.checked(FontAxes.weight(buffer, font.ttcIndex)) ?: return null
            axes[key] = axis
        }
        return axes.values.distinct().singleOrNull()
    }

    private fun matches(original: PositionedGlyphs, candidate: PositionedGlyphs, weight: Int): Boolean {
        if (original.glyphCount() != candidate.glyphCount()) return false
        for (index in 0 until candidate.glyphCount()) {
            val before = original.getFont(index)
            val after = candidate.getFont(index)
            if (before.sourceIdentifier != after.sourceIdentifier || before.ttcIndex != after.ttcIndex ||
                original.getGlyphId(index) != candidate.getGlyphId(index) ||
                original.getFakeBold(index) != candidate.getFakeBold(index)
            ) return false
            val overrideWeight = candidate.getWeightOverride(index)
            val effective = if (overrideWeight != PositionedGlyphs.NO_OVERRIDE) overrideWeight
            else after.axes?.firstOrNull { it.tag == "wght" }?.styleValue ?: return false
            if (!effective.isFinite() || abs(effective - weight) > 0.5f) return false
        }
        return true
    }

    // API acceptance and axis metadata alone do not establish visible variation.
    private fun distinctInk(paint: Paint, text: String, low: Int, middle: Int, high: Int): Boolean {
        val bitmap = Bitmap.createBitmap(512, 96, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val bounds = Rect()
            val samples = listOf(low, middle, high).map { weight ->
                paint.setFontVariationOverride("'wght' $weight")
                paint.getTextBounds(text, 0, text.length, bounds)
                if (bounds.isEmpty || bounds.left + 8 < 0 || bounds.right + 8 >= bitmap.width ||
                    bounds.top + 64 < 0 || bounds.bottom + 64 >= bitmap.height
                ) return false
                bitmap.eraseColor(Color.TRANSPARENT)
                canvas.drawText(text, 8f, 64f, paint)
                IntArray(bitmap.width * bitmap.height).also { pixels ->
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    if (pixels.none { it ushr 24 != 0 }) return false
                }
            }
            return !samples[0].contentEquals(samples[1]) &&
                !samples[1].contentEquals(samples[2]) && !samples[0].contentEquals(samples[2])
        } finally {
            bitmap.recycle()
        }
    }
}
