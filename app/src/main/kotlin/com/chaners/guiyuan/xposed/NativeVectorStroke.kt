package com.chaners.guiyuan.xposed

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Xml
import androidx.core.graphics.PathParser
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import org.xmlpull.v1.XmlPullParser

// Reads only flat, filled native vectors. Unknown vector features keep the original Drawable.
internal object NativeVectorStroke {
    private const val NS = "http://schemas.android.com/apk/res/android"
    private val pathAttrs = intArrayOf(
        android.R.attr.fillColor, android.R.attr.fillAlpha, android.R.attr.fillType,
    )
    private val vectorKeys = setOf("width", "height", "viewportWidth", "viewportHeight")
    private val pathKeys = setOf("pathData", "fillColor", "fillAlpha", "fillType")

    data class Layer(val path: Path, val alpha: Int)
    data class Source(val viewportWidth: Float, val viewportHeight: Float, val layers: List<Layer>)
    data class Styled(
        val viewportWidth: Float,
        val viewportHeight: Float,
        val layers: List<Layer>,
        val opticalBounds: RectF,
        val outwardStroke: Float,
    )

    fun duringTransition(weight: Int, progress: Float): Int =
        (weight + (100 - weight) * progress.coerceIn(0f, 1f)).roundToInt()

    // Edge displacement is in vector viewport units, independent of display density or zoom.
    fun edgeOffset(weight: Int, width: Float, height: Float): Float {
        val unit = min(width, height)
        val requested = (weight.coerceIn(60, 130) - 100) * unit / 2000f
        // The native Wi-Fi end curves are tiny; 60% must not erode their rounding.
        // A proportional inset keeps every slider step distinct without a hard plateau.
        return if (requested < 0f) requested * 0.6f else requested
    }

    fun load(resources: Resources, resourceId: Int): Source? =
        runCatching {
            resources.getXml(resourceId).use { xml ->
                var depth = 0
                var viewportWidth = Float.NaN
                var viewportHeight = Float.NaN
                val layers = ArrayList<Layer>(5)
                while (xml.next() != XmlPullParser.END_DOCUMENT) {
                    when (xml.eventType) {
                        XmlPullParser.START_TAG -> {
                            when {
                                depth == 0 && xml.name == "vector" && onlyKeys(xml, vectorKeys) -> {
                                    viewportWidth = xml.getAttributeFloatValue(NS, "viewportWidth", Float.NaN)
                                    viewportHeight = xml.getAttributeFloatValue(NS, "viewportHeight", Float.NaN)
                                    if (!viewportWidth.isFinite() || !viewportHeight.isFinite() ||
                                        viewportWidth <= 0f || viewportHeight <= 0f
                                    ) return@runCatching null
                                }
                                depth == 1 && xml.name == "path" && onlyKeys(xml, pathKeys) -> {
                                    if (layers.size >= 12) return@runCatching null
                                    val data = xml.getAttributeValue(NS, "pathData")
                                        ?.takeIf { it.isNotBlank() && it.length <= 4096 }
                                        ?: return@runCatching null
                                    val values = resources.obtainAttributes(Xml.asAttributeSet(xml), pathAttrs)
                                    val alpha: Int
                                    val fillType: Int
                                    try {
                                        if (!values.hasValue(0)) return@runCatching null
                                        val fill = values.getColor(0, Color.TRANSPARENT)
                                        val fillAlpha = values.getFloat(1, 1f)
                                        if (!fillAlpha.isFinite() || fillAlpha !in 0f..1f) {
                                            return@runCatching null
                                        }
                                        alpha = (Color.alpha(fill) * fillAlpha).roundToInt()
                                        fillType = values.getInt(2, 0)
                                    } finally {
                                        values.recycle()
                                    }
                                    if (fillType !in 0..1) return@runCatching null
                                    val path = PathParser.createPathFromPathData(data)
                                    if (path.isEmpty) return@runCatching null
                                    path.fillType =
                                        if (fillType == 1) Path.FillType.EVEN_ODD else Path.FillType.WINDING
                                    layers.add(Layer(path, alpha))
                                }
                                else -> return@runCatching null
                            }
                            depth++
                        }
                        XmlPullParser.END_TAG -> depth--
                    }
                }
                if (depth != 0 || layers.isEmpty()) null
                else Source(viewportWidth, viewportHeight, layers)
            }
        }.onFailure { rethrowRenderFatal(it) }.getOrNull()

    fun style(source: Source, weight: Int): Styled? =
        runCatching {
            val offset = edgeOffset(weight, source.viewportWidth, source.viewportHeight)
            if (!offset.isFinite() || offset == 0f) return@runCatching null
            val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f * abs(offset)
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            val bounds = RectF()
            val rimBounds = RectF()
            val combined = RectF()
            val styled = ArrayList<Layer>(source.layers.size)
            source.layers.forEachIndexed { index, layer ->
                val rim = Path()
                if (!rimPaint.getFillPath(layer.path, rim)) return@runCatching null
                val changed =
                    if (offset > 0f) {
                        // Keep the native Bezier paths in the final draw: filling and
                        // stroking directly avoids merged-outline corner artifacts.
                        Path(layer.path).also {
                            it.computeBounds(bounds, true)
                            rim.computeBounds(rimBounds, true)
                            bounds.union(rimBounds)
                        }
                    } else {
                        Path(layer.path).apply {
                            if (!op(rim, Path.Op.DIFFERENCE)) return@runCatching null
                            computeBounds(bounds, true)
                        }
                    }
                if (changed.isEmpty || !bounds.left.isFinite() || !bounds.top.isFinite() ||
                    !bounds.right.isFinite() || !bounds.bottom.isFinite() || bounds.isEmpty
                ) return@runCatching null
                if (index == 0) combined.set(bounds) else combined.union(bounds)
                styled.add(Layer(changed, layer.alpha))
            }
            Styled(
                source.viewportWidth, source.viewportHeight, styled,
                RectF(
                    combined.left / source.viewportWidth,
                    combined.top / source.viewportHeight,
                    combined.right / source.viewportWidth,
                    combined.bottom / source.viewportHeight,
                ),
                offset.coerceAtLeast(0f) * 2f,
            )
        }.onFailure { rethrowRenderFatal(it) }.getOrNull()

    private fun onlyKeys(xml: android.content.res.XmlResourceParser, allowed: Set<String>): Boolean =
        (0 until xml.attributeCount).all { xml.getAttributeName(it) in allowed }
}
