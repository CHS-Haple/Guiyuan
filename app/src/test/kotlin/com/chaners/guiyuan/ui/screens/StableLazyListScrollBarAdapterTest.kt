package com.chaners.guiyuan.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StableLazyListScrollBarAdapterTest {
    @Test fun samePixelTravelHasSameOffsetAcrossDifferentRowHeights() {
        val heights = intArrayOf(100, 400, 100)
        val inShort = observedPixelOffset(heights, 0, 40, 200.0, 0) -
            observedPixelOffset(heights, 0, 0, 200.0, 0)
        val inTall = observedPixelOffset(heights, 1, 40, 200.0, 0) -
            observedPixelOffset(heights, 1, 0, 200.0, 0)
        assertEquals(40.0, inShort, 0.0001)
        assertEquals(inShort, inTall, 0.0001)
    }

    @Test fun crossingRowBoundariesPreservesPixelContinuity() {
        val heights = intArrayOf(100, 400, 120)
        assertEquals(
            observedPixelOffset(heights, 0, 100, 200.0, 0),
            observedPixelOffset(heights, 1, 0, 200.0, 0),
            0.0001,
        )
        assertEquals(
            observedPixelOffset(heights, 1, 400, 200.0, 0),
            observedPixelOffset(heights, 2, 0, 200.0, 0),
            0.0001,
        )
    }

    @Test fun inverseMappingMatchesTheDisplayedMeasuredPixelScale() {
        val heights = intArrayOf(100, 400, 120)
        val offsets = listOf(0.0, 40.0, 99.0, 100.0, 220.0, 499.0, 500.0, 560.0)
        for (offset in offsets) {
            val (index, within) = observedPixelTarget(heights, offset, 200.0, 0)
            assertEquals(
                offset,
                observedPixelOffset(heights, index, within, 200.0, 0),
                0.0001,
            )
        }
    }

    @Test fun unknownItemsUseMeasuredBaselineRatherThanFixedSize() {
        val partiallySeen = intArrayOf(100, 0, 400)
        assertEquals(700.0, observedPixelContentSize(partiallySeen, 200.0, 0), 0.0001)
        assertEquals(300.0, observedPixelOffset(partiallySeen, 2, 0, 200.0, 0), 0.0001)
        assertEquals(0.0, observedPixelContentSize(intArrayOf(), 200.0, 0), 0.0001)
    }

    @Test fun measuredPixelRangePreservesItemSpacing() {
        val heights = intArrayOf(100, 400, 100)
        assertEquals(616.0, observedPixelContentSize(heights, 100.0, 8), 0.0001)
        assertEquals(108.0, observedPixelOffset(heights, 1, 0, 100.0, 8), 0.0001)
        val (index, within) = observedPixelTarget(heights, 148.0, 100.0, 8)
        assertEquals(1, index)
        assertEquals(40, within)
        assertTrue(observedPixelContentSize(heights, 100.0, 8) > observedPixelContentSize(heights, 100.0, 0))
    }
}
