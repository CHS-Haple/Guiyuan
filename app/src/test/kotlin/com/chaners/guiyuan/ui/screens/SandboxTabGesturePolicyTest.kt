package com.chaners.guiyuan.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class SandboxTabGesturePolicyTest {
    @Test
    fun verticalSwipeWithSmallHorizontalDriftBelongsToPage() {
        assertEquals(SandboxTabDragAxis.Pending, sandboxTabDragAxis(5f, 9f, 10f))
        assertEquals(SandboxTabDragAxis.Vertical, sandboxTabDragAxis(14f, 52f, 10f))
        assertEquals(SandboxTabDragAxis.Vertical, sandboxTabDragAxis(-18f, -54f, 10f))
    }

    @Test
    fun horizontalSwipeAndTapRemainTabInteractions() {
        assertEquals(SandboxTabDragAxis.Horizontal, sandboxTabDragAxis(25f, 4f, 10f))
        assertEquals(SandboxTabDragAxis.Horizontal, sandboxTabDragAxis(-36f, 12f, 10f))
        assertEquals(SandboxTabDragAxis.Vertical, sandboxTabDragAxis(30f, 30f, 10f))
        assertEquals(SandboxTabDragAxis.Pending, sandboxTabDragAxis(2f, 3f, 10f))
    }

    @Test
    fun equalWidthSlotsMapToVisualIndexInBothDirections() {
        assertEquals(0, sandboxTabIndexAt(25f, 312f, 3, 4f, 8f, false))
        assertEquals(1, sandboxTabIndexAt(160f, 312f, 3, 4f, 8f, false))
        assertEquals(2, sandboxTabIndexAt(287f, 312f, 3, 4f, 8f, false))
        assertEquals(2, sandboxTabIndexAt(25f, 312f, 3, 4f, 8f, true))
        assertEquals(0, sandboxTabIndexAt(287f, 312f, 3, 4f, 8f, true))
        assertEquals(-1, sandboxTabIndexAt(10f, 20f, 3, 4f, 8f, false))
    }
}
