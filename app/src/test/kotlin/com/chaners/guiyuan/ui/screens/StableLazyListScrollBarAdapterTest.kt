package com.chaners.guiyuan.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StableLazyListScrollBarAdapterTest {
    @Test fun crossedItemBoundaryHasNoJumpWhenHeightsDiffer() {
        val before = itemEndOrdinal(5, -300, 400, 100)
        val after = itemEndOrdinal(6, 100, 120, 100)
        assertEquals(6.0, before, 0.0001)
        assertEquals(before, after, 0.0001)
    }

    @Test fun downwardScrollNeverReversesWhenVisibleItemHeightChanges() {
        val start = 4.5
        val total = 38
        val ends = listOf(
            itemEndOrdinal(5, -200, 400, 100),
            itemEndOrdinal(5, -240, 400, 100),
            itemEndOrdinal(5, -300, 400, 100),
            itemEndOrdinal(6, 100, 120, 100),
            itemEndOrdinal(6, 70, 120, 100),
            itemEndOrdinal(6, 10, 120, 100),
        )
        val offsets = ends.map { stableListIndicatorRange(total, start, it).offset }
        assertTrue(offsets.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test fun thumbIsAtBothEndsAndKeepsFixedLength() {
        val top = stableListIndicatorRange(38, 4.5, 4.5)
        val end = stableListIndicatorRange(38, 4.5, 38.0)
        assertEquals(0.0, top.offset, 0.0001)
        assertEquals(33.5, end.offset, 0.0001)
        assertEquals(top.viewport, end.viewport, 0.0)
    }

    @Test fun nonScrollableAndEmptyListsHideTheIndicator() {
        assertEquals(0.0, stableListIndicatorRange(0, 0.0, 0.0).content, 0.0)
        val noScroll = stableListIndicatorRange(3, 3.0, 3.0)
        assertEquals(noScroll.viewport, noScroll.content, 0.0)
    }
}
