// Guiyuan's data-only compatibility layer for MIUIX's native scrollbar.
// AndroidX LazyListState.scrollIndicatorState estimates total length from
// currently visible item heights. Unequal heights can make the thumb reverse
// direction while content continues scrolling forward.
package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.basic.ScrollBarAdapter
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

internal data class StableListIndicatorRange(
    val content: Double,
    val viewport: Double,
    val offset: Double,
)

// Count item ordinal + visible fraction at the *bottom* of the viewport.
// Unlike AndroidX's visible-item-average pixel offset, this is continuous
// across differently sized item boundaries.
internal fun itemEndOrdinal(index: Int, offset: Int, size: Int, viewportEnd: Int): Double =
    index + ((viewportEnd.toDouble() - offset) / size.coerceAtLeast(1))
        .coerceIn(0.0, 1.0)

internal fun stableListIndicatorRange(
    itemCount: Int,
    initialViewportOrdinal: Double,
    bottomOrdinal: Double,
): StableListIndicatorRange {
    if (itemCount <= 0) return StableListIndicatorRange(0.0, 0.0, 0.0)
    val content = itemCount.toDouble()
    val viewport = initialViewportOrdinal.coerceIn(0.0, content)
    val offset = (bottomOrdinal - viewport).coerceIn(0.0, content - viewport)
    return StableListIndicatorRange(content, viewport, offset)
}

/** Keep MIUIX's drawing, touch capture, drag and scroll-to logic unchanged. */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun rememberStableLazyListScrollBarAdapter(
    state: LazyListState,
    contentKey: Any? = null,
): ScrollBarAdapter {
    val native = rememberScrollBarAdapter(state)
    return remember(state, native, contentKey) {
        StableLazyListScrollBarAdapter(state, native)
    }
}

@OptIn(ExperimentalScrollBarApi::class)
private class StableLazyListScrollBarAdapter(
    private val list: LazyListState,
    private val native: ScrollBarAdapter,
) : ScrollBarAdapter {
    private var observedCount = -1
    private var initialViewportOrdinal: Double? = null

    private fun range(): StableListIndicatorRange {
        val info = list.layoutInfo
        val count = info.totalItemsCount
        if (count <= 0) return stableListIndicatorRange(0, 0.0, 0.0)
        if (!list.canScrollBackward && !list.canScrollForward) {
            return stableListIndicatorRange(count, count.toDouble(), count.toDouble())
        }
        val last = info.visibleItemsInfo
            .filter { it.size > 0 && it.offset < info.viewportEndOffset &&
                it.offset + it.size > info.viewportStartOffset }
            .maxByOrNull { it.index }
            ?: return stableListIndicatorRange(0, 0.0, 0.0)
        val bottom = itemEndOrdinal(last.index, last.offset, last.size, info.viewportEndOffset)
        if (count != observedCount) {
            observedCount = count
            initialViewportOrdinal = null
        }
        if (initialViewportOrdinal == null || !list.canScrollBackward) {
            val firstIndex = list.firstVisibleItemIndex
            val firstSize = info.visibleItemsInfo.firstOrNull { it.index == firstIndex }?.size ?: 1
            val top = firstIndex +
                (list.firstVisibleItemScrollOffset.toDouble() / firstSize.coerceAtLeast(1))
                    .coerceIn(0.0, 1.0)
            initialViewportOrdinal = if (!list.canScrollBackward) bottom else (bottom - top).coerceAtLeast(0.0)
        }
        return stableListIndicatorRange(count, initialViewportOrdinal ?: 0.0, bottom)
    }

    override val contentSize: Double get() = range().content
    override val viewportSize: Double get() = range().viewport
    override val scrollOffset: Double get() = range().offset

    override suspend fun scrollTo(scrollOffset: Double) {
        val current = range()
        val max = current.content - current.viewport
        if (max <= 0.0) return
        val upstreamMax = (native.contentSize - native.viewportSize).coerceAtLeast(0.0)
        if (upstreamMax <= 0.0) return
        // The upstream adapter retains the sole actual scroll writer. Only the
        // displayed thumb uses item ordinals instead of volatile pixel estimates.
        native.scrollTo((scrollOffset / max).coerceIn(0.0, 1.0) * upstreamMax)
    }
}
