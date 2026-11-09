package com.chaners.guiyuan.ui.icons

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Keeps both OS4 source variants aligned with the accepted MIUIX Glass vector canvas. */
class GlassIconCanvasTest {
    @Test
    fun regularGlassIconsKeep28DpCanvasAndViewport() {
        val icons = listOf(
            "Back" to Os4GlassBack,
            "Search" to Os4GlassSearch,
            "Close" to Os4GlassClose,
            "Refresh" to Os4GlassRefresh,
            "Filter" to Os4GlassFilter,
            "More" to Os4GlassMore,
            "Edit" to Os4GlassEdit,
            "Copy" to Os4GlassCopy,
            "Delete" to Os4GlassDelete,
            "Share" to Os4GlassShare,
        )
        for ((name, icon) in icons) {
            assertEquals("$name width", 28.dp, icon.defaultWidth)
            assertEquals("$name height", 28.dp, icon.defaultHeight)
            assertEquals("$name viewport width", 1333.3833f, icon.viewportWidth, 0.001f)
            assertEquals("$name viewport height", 1333.3833f, icon.viewportHeight, 0.001f)
        }
    }
}
