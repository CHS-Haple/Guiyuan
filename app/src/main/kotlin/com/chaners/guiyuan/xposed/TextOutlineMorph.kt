package com.chaners.guiyuan.xposed

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Experimental single-outline morph. No bitmap, alpha crossfade or API 37 font axis.
internal class TextOutlineMorph {
    private data class Key(
        val text: String,
        val size: Float,
        val source: Typeface,
        val target: StatusPainter.TransitionTextStyle,
    )

    private data class Contour(val points: FloatArray, val length: Float)

    private data class Pairing(val source: FloatArray, val target: FloatArray)

    private val sourcePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sourcePath = Path()
    private val targetPath = Path()
    private val framePath = Path()
    private val frameBounds = RectF()
    private var key: Key? = null
    private var pairs: List<Pairing>? = null

    fun clear() {
        key = null
        pairs = null
    }

    fun prepare(
        text: String,
        size: Float,
        source: Typeface,
        target: StatusPainter.TransitionTextStyle,
    ): Boolean {
        if (
            text.isEmpty() ||
            !size.isFinite() || size <= 0f ||
            text.any { !it.isLetterOrDigit() } ||
            target.typeface == null ||
            target.fakeBoldText ||
            target.paintStyle != Paint.Style.FILL ||
            target.strokeWidth != 0f ||
            !target.textScaleX.isFinite() || target.textScaleX <= 0f ||
            !target.textSkewX.isFinite() ||
            !target.letterSpacing.isFinite()
        ) return false

        val request = Key(text, size, source, target)
        if (key == request) return pairs != null
        key = request
        pairs = null

        sourcePaint.reset()
        sourcePaint.isAntiAlias = true
        sourcePaint.typeface = source
        sourcePaint.textSize = size
        sourcePaint.textAlign = Paint.Align.LEFT
        sourcePaint.style = Paint.Style.FILL
        sourcePaint.textScaleX = 1f
        sourcePaint.textSkewX = 0f
        sourcePaint.letterSpacing = 0f

        targetPaint.set(sourcePaint)
        targetPaint.typeface = target.typeface
        targetPaint.textScaleX = target.textScaleX
        targetPaint.textSkewX = target.textSkewX
        targetPaint.letterSpacing = target.letterSpacing

        sourcePath.reset()
        targetPath.reset()
        sourcePaint.getTextPath(text, 0, text.length, 0f, 0f, sourcePath)
        targetPaint.getTextPath(text, 0, text.length, 0f, 0f, targetPath)
        if (sourcePath.isEmpty || targetPath.isEmpty) return false

        val from = contours(sourcePath) ?: return false
        val to = contours(targetPath) ?: return false
        if (from.size != to.size || from.isEmpty()) return false

        val aligned = ArrayList<Pairing>(from.size)
        for (i in from.indices) {
            val a = from[i]
            val b = to[i]
            val ratio = a.length / b.length
            if (ratio !in 0.5f..2f) return false
            val ap = a.points
            val bp = b.points
            val n = ap.size / 2
            if (bp.size != ap.size) return false
            var ax = 0f
            var ay = 0f
            var bx = 0f
            var by = 0f
            for (j in 0 until n) {
                ax += ap[j * 2]
                ay += ap[j * 2 + 1]
                bx += bp[j * 2]
                by += bp[j * 2 + 1]
            }
            // Do not morph unrelated contours simply because their counts match.
            if (
                abs(ax / n - bx / n) > size * 0.45f ||
                abs(ay / n - by / n) > size * 0.45f
            ) return false

            var best = Float.POSITIVE_INFINITY
            var bestShift = 0
            var bestDirection = 1
            for (direction in intArrayOf(1, -1)) {
                for (shift in 0 until n) {
                    var error = 0f
                    for (sample in 0 until 16) {
                        val j = sample * n / 16
                        val k = (shift + direction * j + n) % n
                        val dx = ap[j * 2] - bp[k * 2]
                        val dy = ap[j * 2 + 1] - bp[k * 2 + 1]
                        error += dx * dx + dy * dy
                    }
                    if (error < best) {
                        best = error
                        bestShift = shift
                        bestDirection = direction
                    }
                }
            }
            if (!best.isFinite() || best / 16f > size * size * 0.20f) return false
            val shifted = FloatArray(bp.size)
            for (j in 0 until n) {
                val k = (bestShift + bestDirection * j + n) % n
                shifted[j * 2] = bp[k * 2]
                shifted[j * 2 + 1] = bp[k * 2 + 1]
            }
            aligned += Pairing(ap, shifted)
        }
        pairs = aligned
        return true
    }

    private fun contours(path: Path): List<Contour>? {
        val measure = PathMeasure(path, true)
        val contours = ArrayList<Contour>()
        val point = FloatArray(2)
        do {
            val length = measure.length
            if (!length.isFinite() || length <= 0.5f) return null
            val count = (length / 0.4f).roundToInt().coerceIn(96, 384)
            val points = FloatArray(count * 2)
            for (j in 0 until count) {
                if (!measure.getPosTan(length * j / count, point, null)) return null
                points[2 * j] = point[0]
                points[2 * j + 1] = point[1]
            }
            contours += Contour(points, length)
            if (contours.size > 24) return null
        } while (measure.nextContour())
        return contours
    }

    fun draw(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        fraction: Float,
        paint: Paint,
    ): Boolean {
        val shapes = pairs ?: return false
        if (!fraction.isFinite() || fraction <= 0f || fraction >= 1f) return false
        framePath.reset()
        framePath.fillType = Path.FillType.EVEN_ODD
        for ((source, target) in shapes) {
            // The prepared contours must already have matching topology.
            for (j in source.indices step 2) {
                val x = source[j] + (target[j] - source[j]) * fraction
                val y = source[j + 1] + (target[j + 1] - source[j + 1]) * fraction
                if (j == 0) framePath.moveTo(x, y) else framePath.lineTo(x, y)
            }
            framePath.close()
        }
        framePath.computeBounds(frameBounds, true)
        if (frameBounds.isEmpty) return false
        val saved = canvas.save()
        canvas.translate(centerX - frameBounds.centerX(), centerY - frameBounds.centerY())
        val previousStyle = paint.style
        val previousStroke = paint.strokeWidth
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        canvas.drawPath(framePath, paint)
        paint.style = previousStyle
        paint.strokeWidth = previousStroke
        canvas.restoreToCount(saved)
        return true
    }
}
