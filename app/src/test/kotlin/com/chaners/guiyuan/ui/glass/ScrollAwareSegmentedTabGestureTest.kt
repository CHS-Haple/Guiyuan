package com.chaners.guiyuan.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollAwareSegmentedTabGestureTest {
    @Test
    fun verticalSwipeWithSmallHorizontalDriftBelongsToPage() {
        assertEquals(SegmentedDragAxis.Pending, segmentedDragAxis(5f, 9f, 10f))
        assertEquals(SegmentedDragAxis.Vertical, segmentedDragAxis(14f, 52f, 10f))
        assertEquals(SegmentedDragAxis.Vertical, segmentedDragAxis(-18f, -54f, 10f))
    }

    @Test
    fun horizontalSwipeAndTapRemainTabInteractions() {
        assertEquals(SegmentedDragAxis.Horizontal, segmentedDragAxis(25f, 4f, 10f))
        assertEquals(SegmentedDragAxis.Horizontal, segmentedDragAxis(-36f, 12f, 10f))
        assertEquals(SegmentedDragAxis.Vertical, segmentedDragAxis(30f, 30f, 10f))
        assertEquals(SegmentedDragAxis.Pending, segmentedDragAxis(2f, 3f, 10f))
    }

    @Test
    fun equalWidthSlotsMapToVisualIndexInBothDirections() {
        val ltr = SegmentedSlotGeometry(312f, 3, 4f, 8f, false)
        val rtl = ltr.copy(rtl = true)
        assertEquals(0, ltr.indexAt(25f))
        assertEquals(1, ltr.indexAt(160f))
        assertEquals(2, ltr.indexAt(287f))
        assertEquals(2, rtl.indexAt(25f))
        assertEquals(0, rtl.indexAt(287f))
        assertEquals(-1, SegmentedSlotGeometry(20f, 3, 4f, 8f, false).indexAt(10f))
    }
}
