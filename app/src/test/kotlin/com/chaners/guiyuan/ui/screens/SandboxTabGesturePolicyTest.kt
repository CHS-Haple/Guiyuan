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
        val ltr = SandboxTabSlotGeometry(312f, 3, 4f, 8f, false)
        val rtl = ltr.copy(rtl = true)
        assertEquals(0, ltr.indexAt(25f))
        assertEquals(1, ltr.indexAt(160f))
        assertEquals(2, ltr.indexAt(287f))
        assertEquals(2, rtl.indexAt(25f))
        assertEquals(0, rtl.indexAt(287f))
        assertEquals(-1, SandboxTabSlotGeometry(20f, 3, 4f, 8f, false).indexAt(10f))
    }
}
