// Guiyuan's measurement-only adapter for MIUIX's native scrollbar.
// LazyColumn cannot know the exact total pixel extent of uncomposed items.
// Seen item heights are real measurements; unseen items use a fixed average
// of the first viewport's measured heights until those items are composed.
package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.ScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

internal data class StableListIndicatorRange(
    val content: Double,
    val viewport: Double,
    val offset: Double,
)

internal fun observedItemPixels(sizes: IntArray, index: Int, unseenPixels: Double): Double =
    sizes[index].takeIf { it > 0 }?.toDouble() ?: unseenPixels.coerceAtLeast(1.0)

// A physical scroll delta remains the same number of pixels in short/long items.
// The ordinal approach instead changed speed by the inverse of item height.
internal fun observedPixelOffset(
    sizes: IntArray,
    index: Int,
    itemOffset: Int,
    unseenPixels: Double,
    spacing: Int,
): Double {
    val count = index.coerceIn(0, sizes.size)
    var pixels = itemOffset.coerceAtLeast(0).toDouble()
    for (i in 0 until count) pixels += observedItemPixels(sizes, i, unseenPixels) + spacing
    return pixels
}

internal fun observedPixelContentSize(sizes: IntArray, unseenPixels: Double, spacing: Int): Double =
    (observedPixelOffset(sizes, sizes.size, 0, unseenPixels, spacing) -
        if (sizes.isEmpty()) 0 else spacing).coerceAtLeast(0.0)

internal fun observedPixelTarget(
    sizes: IntArray,
    offset: Double,
    unseenPixels: Double,
    spacing: Int,
): Pair<Int, Int> {
    if (sizes.isEmpty()) return 0 to 0
    var remaining = offset.coerceAtLeast(0.0)
    for (i in sizes.indices) {
        val size = observedItemPixels(sizes, i, unseenPixels)
        if (remaining < size + spacing || i == sizes.lastIndex) {
            val lastGap = if (i == sizes.lastIndex) 0 else spacing
            return i to remaining.coerceIn(0.0, size + lastGap).toInt()
        }
        remaining -= size + spacing
    }
    return sizes.lastIndex to 0
}

/**
 * Monotone, endpoint-fixed projection for revisions to the *estimated* total.
 * Preserve the thumb's normalized location at the current measured anchor when
 * a newly composed row discloses its real height. No timers or easing are used.
 * Endpoints remain exact even after unknown rows are encountered.
 */
internal data class PixelScrollProjection(
    val anchor: Double,
    val fraction: Double,
    val maximum: Double,
) {
    fun toFraction(pixelOffset: Double): Double {
        if (maximum <= 0.0) return 0.0
        val p = pixelOffset.coerceIn(0.0, maximum)
        val a = anchor.coerceIn(0.0, maximum)
        val f = fraction.coerceIn(0.0, 1.0)
        return if (p <= a && a > 0.0) {
            f * p / a
        } else if (a < maximum) {
            f + (1.0 - f) * (p - a) / (maximum - a)
        } else {
            f
        }
    }

    fun toPixel(fraction: Double): Double {
        if (maximum <= 0.0) return 0.0
        val f = this.fraction.coerceIn(0.0, 1.0)
        val target = fraction.coerceIn(0.0, 1.0)
        val a = anchor.coerceIn(0.0, maximum)
        return if (target <= f && f > 0.0) {
            a * target / f
        } else if (f < 1.0) {
            a + (maximum - a) * (target - f) / (1.0 - f)
        } else {
            a
        }
    }
}

/** Keep the upstream scrollbar's drawing, pointer handling and animations. */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun rememberStableLazyListScrollBarAdapter(
    state: LazyListState,
    contentKey: Any? = null,
): ScrollBarAdapter = remember(state, contentKey) {
    ObservedPixelLazyListScrollBarAdapter(state)
}

@OptIn(ExperimentalScrollBarApi::class)
private class ObservedPixelLazyListScrollBarAdapter(
    private val list: LazyListState,
) : ScrollBarAdapter {
    private var count = -1
    private var itemPixels = IntArray(0)
    private var itemKeys = arrayOfNulls<Any>(0)
    private var unseenPixels = 0.0
    private var cacheGeneration = 0
    private var measurementRevision = 0
    private var projection: PixelScrollProjection? = null

    private fun reset(itemCount: Int) {
        count = itemCount
        itemPixels = IntArray(itemCount)
        itemKeys = arrayOfNulls(itemCount)
        unseenPixels = 0.0
        cacheGeneration++
    }

    private fun observe() {
        val info = list.layoutInfo
        if (info.totalItemsCount != count) reset(info.totalItemsCount)
        val visible = info.visibleItemsInfo.filter { it.index in itemPixels.indices && it.size > 0 }
        if (visible.any { itemKeys[it.index] != null && itemKeys[it.index] != it.key }) {
            // Equal count can also represent a new result set or a reordered row.
            reset(info.totalItemsCount)
        }
        if (unseenPixels <= 0.0 && visible.isNotEmpty()) {
            unseenPixels = visible.map { it.size.toDouble() }.average()
            measurementRevision++
        }
        for (item in visible) {
            if (itemPixels[item.index] != item.size) {
                itemPixels[item.index] = item.size
                measurementRevision++
            }
            itemKeys[item.index] = item.key
        }
    }

    private fun physicalPosition(): Double = observedPixelOffset(
        itemPixels, list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset,
        unseenPixels, list.layoutInfo.mainAxisItemSpacing,
    )

    private fun range(): StableListIndicatorRange {
        val previous = projection
        val beforePosition = if (previous != null && unseenPixels > 0.0) physicalPosition() else 0.0
        val generation = cacheGeneration
        val revision = measurementRevision
        observe()
        val info = list.layoutInfo
        if (count <= 0 || (!list.canScrollForward && !list.canScrollBackward) || unseenPixels <= 0.0) {
            projection = null
            return StableListIndicatorRange(0.0, 0.0, 0.0)
        }
        val viewport = info.viewportSize.height.toDouble().coerceAtLeast(0.0)
        val measured = observedPixelContentSize(itemPixels, unseenPixels, info.mainAxisItemSpacing)
        val content = (measured + info.beforeContentPadding + info.afterContentPadding).coerceAtLeast(viewport)
        val max = (content - viewport).coerceAtLeast(0.0)
        val position = physicalPosition().coerceIn(0.0, max)
        projection = when {
            previous == null || generation != cacheGeneration -> PixelScrollProjection(0.0, 0.0, max)
            revision != measurementRevision || previous.maximum != max ->
                PixelScrollProjection(position, previous.toFraction(beforePosition), max)
            else -> previous
        }
        val offset = when {
            !list.canScrollBackward -> 0.0
            !list.canScrollForward -> max
            else -> (projection?.toFraction(position) ?: 0.0) * max
        }
        return StableListIndicatorRange(content, viewport, offset)
    }

    override val contentSize: Double get() = range().content
    override val viewportSize: Double get() = range().viewport
    override val scrollOffset: Double get() = range().offset

    override suspend fun scrollTo(scrollOffset: Double) {
        val current = range()
        val max = current.content - current.viewport
        if (max <= 0.0 || count <= 0) return
        val target = scrollOffset.coerceIn(0.0, max)
        if (target <= 0.5) {
            list.scrollToItem(0)
        } else if (target >= max - 0.5) {
            list.scrollToItem(count - 1)
            val last = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == count - 1 }
            if (last != null && list.canScrollForward) list.scrollBy(last.size.toFloat())
        } else {
            val rawTarget = projection?.toPixel(target / max) ?: return
            val rawDelta = rawTarget - physicalPosition()
            if (abs(rawDelta) <= current.viewport) {
                list.scrollBy(rawDelta.toFloat())
            } else {
                val (index, itemOffset) = observedPixelTarget(
                    itemPixels, rawTarget, unseenPixels, list.layoutInfo.mainAxisItemSpacing,
                )
                list.scrollToItem(index, itemOffset)
            }
        }
    }
}
