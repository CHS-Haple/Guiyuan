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
            return i to remaining.coerceIn(0.0, size).toInt()
        }
        remaining -= size + spacing
    }
    return sizes.lastIndex to 0
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

    private fun reset(itemCount: Int) {
        count = itemCount
        itemPixels = IntArray(itemCount)
        itemKeys = arrayOfNulls(itemCount)
        unseenPixels = 0.0
    }

    private fun observe() {
        val info = list.layoutInfo
        if (info.totalItemsCount != count) reset(info.totalItemsCount)
        val visible = info.visibleItemsInfo.filter { it.index in itemPixels.indices && it.size > 0 }
        if (visible.any { itemKeys[it.index] != null && itemKeys[it.index] != it.key }) {
            // Equal item count can still mean a different result set or row order.
            reset(info.totalItemsCount)
        }
        if (unseenPixels <= 0.0 && visible.isNotEmpty()) {
            unseenPixels = visible.map { it.size.toDouble() }.average()
        }
        for (item in visible) {
            itemKeys[item.index] = item.key
            itemPixels[item.index] = item.size
        }
    }

    private fun range(): StableListIndicatorRange {
        observe()
        val info = list.layoutInfo
        if (count <= 0 || (!list.canScrollForward && !list.canScrollBackward) || unseenPixels <= 0.0) {
            return StableListIndicatorRange(0.0, 0.0, 0.0)
        }
        val viewport = info.viewportSize.height.toDouble().coerceAtLeast(0.0)
        val measured = observedPixelContentSize(itemPixels, unseenPixels, info.mainAxisItemSpacing)
        val content = (measured + info.beforeContentPadding + info.afterContentPadding).coerceAtLeast(viewport)
        val max = (content - viewport).coerceAtLeast(0.0)
        val progress = observedPixelOffset(
            itemPixels,
            list.firstVisibleItemIndex,
            list.firstVisibleItemScrollOffset,
            unseenPixels,
            info.mainAxisItemSpacing,
        ).coerceIn(0.0, max)
        val offset = when {
            !list.canScrollBackward -> 0.0
            !list.canScrollForward -> max
            else -> progress
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
        } else if (abs(target - current.offset) <= current.viewport) {
            // A nearby thumb drag is a direct pixel delta, never a remapped ordinal.
            list.scrollBy((target - current.offset).toFloat())
        } else {
            val (index, itemOffset) = observedPixelTarget(
                itemPixels, target, unseenPixels, list.layoutInfo.mainAxisItemSpacing,
            )
            list.scrollToItem(index, itemOffset)
        }
    }
}
