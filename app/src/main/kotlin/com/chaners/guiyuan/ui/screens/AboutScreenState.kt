package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal class AboutHeroLayout {
    var height by mutableStateOf(200.dp)
    var spacerBottom by mutableFloatStateOf(0f)
    var iconBottom by mutableFloatStateOf(0f)
    var nameBottom by mutableFloatStateOf(0f)
    var versionBottom by mutableFloatStateOf(0f)

    val spacerHeight: Dp get() = height + 218.dp

    fun atRest(list: LazyListState): Boolean =
        list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0

    fun collapse(index: Int, offsetPx: Int): Triple<Float, Float, Float> {
        val offset = offsetPx.toFloat()
        if (index > 0) return Triple(1f, 1f, 1f)
        if (spacerBottom <= versionBottom ||
            versionBottom <= nameBottom || nameBottom <= iconBottom
        ) {
            // Layout may not be measured yet. Scrolling must not keep the mark opaque.
            return if (offset > 0f) Triple(1f, 1f, 1f) else Triple(0f, 0f, 0f)
        }
        val first = spacerBottom - versionBottom
        val second = versionBottom - nameBottom
        val third = nameBottom - iconBottom
        return Triple(
            ((offset - first * 0.5f) / (first * 0.5f)).coerceIn(0f, 1f),
            ((offset - first) / second).coerceIn(0f, 1f),
            ((offset - first - second) / third).coerceIn(0f, 1f),
        )
    }
}

internal data class AboutScrollState(
    val list: LazyListState,
    val layout: AboutHeroLayout,
    val contentMinHeight: Dp,
    val progress: Float,
    val visible: Boolean,
    val collapse: Triple<Float, Float, Float>,
)

@Composable
internal fun rememberAboutScrollState(): AboutScrollState {
    val list = rememberLazyListState()
    val density = LocalDensity.current
    val layout = remember { AboutHeroLayout() }
    val contentMinHeight by remember(density, list) {
        derivedStateOf {
            val info = list.layoutInfo
            val available = (
                info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
            ).coerceAtLeast(0)
            with(density) { available.toDp() }
        }
    }
    val progress by remember(density) {
        derivedStateOf {
            if (list.firstVisibleItemIndex > 0) 1f
            else (list.firstVisibleItemScrollOffset /
                with(density) { layout.spacerHeight.toPx() }).coerceIn(0f, 1f)
        }
    }
    val visible by remember { derivedStateOf { list.firstVisibleItemIndex == 0 } }
    val collapse by remember {
        derivedStateOf { layout.collapse(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset) }
    }
    return AboutScrollState(list, layout, contentMinHeight, progress, visible, collapse)
}
