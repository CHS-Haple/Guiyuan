package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Keep Liquid optics exactly over the pinned MIUIX OS3 indicator, including mid-morph. */
class NativeRailSelectionPlacementTest {
    @Test fun collapsedRailKeepsTheIconIndicatorBounds() {
        val selected = nativeRailSelectionPlacement(80.dp)
        assertEquals(12.dp, selected.start)
        assertEquals(12.dp, selected.top)
        assertEquals(56.dp, selected.width)
        assertEquals(36.dp, selected.height)
    }

    @Test fun expandedRailMatchesTheFullNativePill() {
        val selected = nativeRailSelectionPlacement(240.dp)
        assertEquals(12.dp, selected.start)
        assertEquals(0.dp, selected.top)
        assertEquals(216.dp, selected.width)
        assertEquals(56.dp, selected.height)
    }

    @Test fun halfExpandedStaysInsideTheMorphEndpoints() {
        val selected = nativeRailSelectionPlacement(160.dp)
        assertEquals(12.dp, selected.start)
        assertEquals(6.dp, selected.top)
        assertEquals(96.dp, selected.width)
        assertEquals(46.dp, selected.height)
    }
}
