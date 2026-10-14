package com.chaners.guiyuan.xposed

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Read-only visual geometry extracted from the real native participant.
 *
 * The snapshot is deliberately module-agnostic. It describes what is actually
 * visible, not which package or compatibility implementation produced it.
 */
internal object ParticipantVisualSnapshot {
    private const val PROBE_MAX = 96f
    private const val ALPHA_THRESHOLD = 8
    private const val MIN_COMPONENT_PIXELS = 2f

    internal enum class Topology {
        SINGLE_GLYPH,
        FOUR_VERTICAL_BARS,
        COMPOSITE,
        UNKNOWN,
    }

    internal data class NormalizedRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        val width: Float
            get() = (right - left).coerceAtLeast(0f)
        val height: Float
            get() = (bottom - top).coerceAtLeast(0f)
        val centerX: Float
            get() = (left + right) / 2f
        val centerY: Float
            get() = (top + bottom) / 2f
    }

    internal enum class RowShape { DOTS, BARS }

    internal data class DualMobileRows(
        val top: List<NormalizedRect>,
        val bottom: List<NormalizedRect>,
        val topShape: RowShape,
        val bottomShape: RowShape,
    )

    internal data class Snapshot(
        val envelope: NormalizedRect,
        val components: List<NormalizedRect>,
        val topology: Topology,
        val inkCenterX: Float? = null,
        val inkCenterY: Float? = null,
    ) {
        // Only resolve independent rows when all eight native shapes are separable.
        fun dualMobileRowsWithinEnvelope(): DualMobileRows? {
            if (components.size != 8 || envelope.width <= 0f || envelope.height <= 0f) {
                return null
            }
            val byBottom = components.sortedBy { it.bottom }
            val top = byBottom.take(4).sortedBy { it.centerX }
            val bottom = byBottom.drop(4).sortedBy { it.centerX }
            if (top.maxOf { it.bottom } >= bottom.minOf { it.top }) return null
            // Overlapping columns cannot prove four independent signal elements.
            if (top.zipWithNext().any { (left, right) -> left.right >= right.left } ||
                bottom.zipWithNext().any { (left, right) -> left.right >= right.left }
            ) return null

            val pitch = (top.last().centerX - top.first().centerX) / 3f
            if (pitch <= 0f || top.zip(bottom).any { (a, b) ->
                    abs(a.centerX - b.centerX) > pitch * 0.2f
                }
            ) return null

            fun rowShape(row: List<NormalizedRect>): RowShape? {
                return when {
                    hasDotRow(row) -> RowShape.DOTS
                    classifyComponents(row) == Topology.FOUR_VERTICAL_BARS &&
                        row.any { it.height > it.width * 1.5f } -> RowShape.BARS
                    else -> null
                }
            }

            val topShape = rowShape(top) ?: return null
            val bottomShape = rowShape(bottom) ?: return null

            fun relative(rect: NormalizedRect): NormalizedRect =
                NormalizedRect(
                    left = (rect.left - envelope.left) / envelope.width,
                    top = (rect.top - envelope.top) / envelope.height,
                    right = (rect.right - envelope.left) / envelope.width,
                    bottom = (rect.bottom - envelope.top) / envelope.height,
                )
            return DualMobileRows(
                top = top.map(::relative),
                bottom = bottom.map(::relative),
                topShape = topShape,
                bottomShape = bottomShape,
            )
        }

        private fun hasDotRow(row: List<NormalizedRect>): Boolean =
            row.size == 4 && envelope.height > 0f &&
                row.all { dot ->
                    dot.width > 0f && dot.height > 0f &&
                        dot.height in (dot.width * 0.75f)..(dot.width * 1.3f)
                } && (row.maxOf { it.height } - row.minOf { it.height }) <
                envelope.height * 0.12f

        // A sampled four-dot glyph is known not to be the four-bar native endpoint.
        fun hasFourDots(): Boolean {
            if (envelope.width <= 0f || !hasDotRow(components)) return false
            val dots = components.sortedBy { it.centerX }
            return dots.zipWithNext().all { (left, right) -> left.right < right.left }
        }

        fun fourVerticalBarsWithinEnvelope(): List<NormalizedRect>? {
            if (topology != Topology.FOUR_VERTICAL_BARS || components.size != 4) {
                return null
            }
            val width = envelope.width
            val height = envelope.height
            if (width <= 0f || height <= 0f) return null
            return components
                .sortedBy { component -> component.centerX }
                .map { component ->
                    NormalizedRect(
                        left = ((component.left - envelope.left) / width).coerceIn(0f, 1f),
                        top = ((component.top - envelope.top) / height).coerceIn(0f, 1f),
                        right = ((component.right - envelope.left) / width).coerceIn(0f, 1f),
                        bottom = ((component.bottom - envelope.top) / height).coerceIn(0f, 1f),
                    )
                }
        }
    }

    private data class DrawableVariantKey(
        val level: Int,
        val stateHash: Int,
        val layoutDirection: Int,
    )

    private val drawableCache =
        WeakHashMap<
            Drawable.ConstantState,
            MutableMap<DrawableVariantKey, Snapshot?>,
        >()

    @Synchronized
    fun resolveDrawable(
        drawable: Drawable,
        resources: Resources,
    ): Snapshot? {
        val state = drawable.constantState ?: return null
        val variant =
            DrawableVariantKey(
                level = drawable.level,
                stateHash = drawable.state.contentHashCode(),
                layoutDirection = drawable.layoutDirection,
            )
        drawableCache[state]
            ?.takeIf { perVariant -> perVariant.containsKey(variant) }
            ?.let { perVariant -> return perVariant[variant] }

        val resolved =
            runCatching {
                probeDrawable(
                    state = state,
                    source = drawable,
                    resources = resources,
                )
            }.getOrElse { error ->
                if (error is VirtualMachineError || error is ThreadDeath) {
                    throw error
                }
                null
            }
        drawableCache
            .getOrPut(state) { HashMap() }[variant] = resolved
        return resolved
    }

    fun resolveView(view: View): Snapshot? =
        when (view) {
            is ImageView -> resolveImageView(view)
            is ViewGroup -> resolveViewGroup(view)
            else -> null
        }

    internal data class NormalizedPoint(
        val x: Float,
        val y: Float,
    )

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

    internal fun filterProbeComponents(
        components: List<NormalizedRect>,
        probeWidth: Int,
        probeHeight: Int,
    ): List<NormalizedRect> {
        if (probeWidth <= 0 || probeHeight <= 0) return emptyList()
        val probeArea = probeWidth.toFloat() * probeHeight.toFloat()
        return components.filter { component ->
            component.width *
                component.height *
                probeArea >= MIN_COMPONENT_PIXELS
        }
    }

    internal fun classifyComponents(
        components: List<NormalizedRect>,
    ): Topology {
        if (components.isEmpty()) return Topology.UNKNOWN
        if (components.size == 1) return Topology.SINGLE_GLYPH
        if (components.size != 4) return Topology.COMPOSITE

        val ordered = components.sortedBy { component -> component.centerX }
        val envelope = union(ordered) ?: return Topology.UNKNOWN
        if (envelope.width <= 0f || envelope.height <= 0f) return Topology.UNKNOWN

        val vertical =
            ordered.all { component ->
                component.width > 0f &&
                    component.height > 0f &&
                    component.height >= component.width
            }
        // Equal-sized dots or crossing columns cannot be exact bar endpoints.
        if (!vertical ||
            ordered.none { it.height > it.width * 1.5f } ||
            ordered.zipWithNext().any { (a, b) -> a.right >= b.left }
        ) return Topology.COMPOSITE

        val bottoms = ordered.map { component -> component.bottom }
        val bottomSpread =
            (bottoms.maxOrNull() ?: 0f) - (bottoms.minOrNull() ?: 0f)
        if (bottomSpread > envelope.height * 0.18f) return Topology.COMPOSITE

        val heights = ordered.map { component -> component.height }
        val monotonic =
            heights.zipWithNext().all { (left, right) ->
                right + envelope.height * 0.08f >= left
            }
        return if (monotonic) {
            Topology.FOUR_VERTICAL_BARS
        } else {
            Topology.COMPOSITE
        }
    }

    private fun resolveImageView(image: ImageView): Snapshot? {
        if (image.width <= 0 || image.height <= 0) return null
        val drawable = image.drawable ?: return null
        val drawableSnapshot =
            resolveDrawable(
                drawable = drawable,
                resources = image.resources,
            ) ?: return null

        val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: return null
        val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: return null
        val bounds = drawable.bounds
        val frame =
            RectF(
                if (bounds.width() > 0) bounds.left.toFloat() else 0f,
                if (bounds.height() > 0) bounds.top.toFloat() else 0f,
                if (bounds.width() > 0) bounds.right.toFloat() else intrinsicWidth.toFloat(),
                if (bounds.height() > 0) bounds.bottom.toFloat() else intrinsicHeight.toFloat(),
            )
        image.imageMatrix.mapRect(frame)
        frame.offset(
            image.paddingLeft.toFloat(),
            image.paddingTop.toFloat(),
        )
        if (frame.width() <= 0f || frame.height() <= 0f) return null

        fun map(rect: NormalizedRect): NormalizedRect =
            NormalizedRect(
                left = (frame.left + rect.left * frame.width()) / image.width,
                top = (frame.top + rect.top * frame.height()) / image.height,
                right = (frame.left + rect.right * frame.width()) / image.width,
                bottom = (frame.top + rect.bottom * frame.height()) / image.height,
            )

        val components =
            drawableSnapshot.components
                .map(::map)
                .filter { component -> component.width > 0f && component.height > 0f }
        val envelope = union(components) ?: map(drawableSnapshot.envelope)
        return Snapshot(
            envelope = envelope,
            components = components,
            topology = classifyComponents(components),
            inkCenterX =
                drawableSnapshot.inkCenterX?.let { centerX ->
                    (frame.left + centerX * frame.width()) / image.width
                },
            inkCenterY =
                drawableSnapshot.inkCenterY?.let { centerY ->
                    (frame.top + centerY * frame.height()) / image.height
                },
        )
    }

    private fun resolveViewGroup(group: ViewGroup): Snapshot? {
        if (group.width <= 0 || group.height <= 0) return null
        val components = ArrayList<NormalizedRect>()

        fun collect(view: View) {
            if (
                view.visibility != View.VISIBLE ||
                view.width <= 0 ||
                view.height <= 0
            ) {
                return
            }
            if (view is ImageView) {
                val snapshot = resolveImageView(view) ?: return
                val matrix = Matrix()
                view.transformMatrixToGlobal(matrix)
                group.transformMatrixToLocal(matrix)
                snapshot.components.forEach { component ->
                    val rect =
                        RectF(
                            component.left * view.width,
                            component.top * view.height,
                            component.right * view.width,
                            component.bottom * view.height,
                        )
                    matrix.mapRect(rect)
                    components +=
                        NormalizedRect(
                            left = rect.left / group.width,
                            top = rect.top / group.height,
                            right = rect.right / group.width,
                            bottom = rect.bottom / group.height,
                        )
                }
                return
            }
            val childGroup = view as? ViewGroup ?: return
            for (index in 0 until childGroup.childCount) {
                collect(childGroup.getChildAt(index))
            }
        }

        for (index in 0 until group.childCount) {
            collect(group.getChildAt(index))
        }
        val meaningful =
            components.filter { component ->
                component.width > 0f && component.height > 0f
            }
        val envelope = union(meaningful) ?: return null
        return Snapshot(
            envelope = envelope,
            components = meaningful,
            topology = classifyComponents(meaningful),
        )
    }

    private fun probeDrawable(
        state: Drawable.ConstantState,
        source: Drawable,
        resources: Resources,
    ): Snapshot? {
        val intrinsicWidth = source.intrinsicWidth
        val intrinsicHeight = source.intrinsicHeight
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) return null

        val probeScale =
            PROBE_MAX /
                max(intrinsicWidth, intrinsicHeight).toFloat()
        val probeWidth = max(1, (intrinsicWidth * probeScale).roundToInt())
        val probeHeight = max(1, (intrinsicHeight * probeScale).roundToInt())
        val probeDrawable =
            state
                .newDrawable(resources)
                .mutate()
        val bitmap =
            Bitmap.createBitmap(
                probeWidth,
                probeHeight,
                Bitmap.Config.ARGB_8888,
            )
        try {
            probeDrawable.state = source.state
            probeDrawable.level = source.level
            probeDrawable.layoutDirection = source.layoutDirection
            probeDrawable.setTint(Color.WHITE)
            probeDrawable.alpha = 255
            probeDrawable.setBounds(0, 0, probeWidth, probeHeight)
            probeDrawable.draw(Canvas(bitmap))

            val pixels = IntArray(probeWidth * probeHeight)
            bitmap.getPixels(
                pixels,
                0,
                probeWidth,
                0,
                0,
                probeWidth,
                probeHeight,
            )
            val raw =
                connectedComponents(
                    pixels = pixels,
                    width = probeWidth,
                    height = probeHeight,
                )
            if (raw.isEmpty()) return null
            val inkCenter =
                resolveAlphaWeightedCenter(
                    pixels = pixels,
                    width = probeWidth,
                    height = probeHeight,
                )
            val meaningful =
                filterProbeComponents(
                    components = raw,
                    probeWidth = probeWidth,
                    probeHeight = probeHeight,
                )
            val finalComponents =
                (meaningful.takeIf { it.isNotEmpty() } ?: raw)
                    .sortedBy { component -> component.centerX }
            val envelope = union(finalComponents) ?: return null
            return Snapshot(
                envelope = envelope,
                components = finalComponents,
                topology = classifyComponents(finalComponents),
                inkCenterX = inkCenter?.x,
                inkCenterY = inkCenter?.y,
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun connectedComponents(
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

    private fun union(
        components: List<NormalizedRect>,
    ): NormalizedRect? {
        if (components.isEmpty()) return null
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        components.forEach { component ->
            left = min(left, component.left)
            top = min(top, component.top)
            right = max(right, component.right)
            bottom = max(bottom, component.bottom)
        }
        if (
            !left.isFinite() ||
            !top.isFinite() ||
            !right.isFinite() ||
            !bottom.isFinite() ||
            right <= left ||
            bottom <= top
        ) {
            return null
        }
        return NormalizedRect(
            left = left.coerceIn(0f, 1f),
            top = top.coerceIn(0f, 1f),
            right = right.coerceIn(0f, 1f),
            bottom = bottom.coerceIn(0f, 1f),
        )
    }
}
