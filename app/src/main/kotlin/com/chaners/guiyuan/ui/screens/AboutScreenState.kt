package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
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

    fun atRest(scroll: ScrollState): Boolean = scroll.value == 0

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
    val scroll: ScrollState,
    val layout: AboutHeroLayout,
    val progress: Float,
    val visible: Boolean,
    val collapse: Triple<Float, Float, Float>,
)

@Composable
internal fun rememberAboutScrollState(): AboutScrollState {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val layout = remember { AboutHeroLayout() }
    val progress by remember(density, scroll) {
        derivedStateOf {
            (scroll.value / with(density) { layout.spacerHeight.toPx() }).coerceIn(0f, 1f)
        }
    }
    val visible by remember(density, scroll) {
        derivedStateOf { scroll.value < with(density) { layout.spacerHeight.toPx() } }
    }
    val collapse by remember(density, scroll) {
        derivedStateOf {
            val index = if (scroll.value < with(density) { layout.spacerHeight.toPx() }) 0 else 1
            layout.collapse(index, scroll.value)
        }
    }
    return AboutScrollState(scroll, layout, progress, visible, collapse)
}
