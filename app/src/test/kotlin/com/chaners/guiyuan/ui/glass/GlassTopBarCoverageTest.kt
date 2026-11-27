package com.chaners.guiyuan.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassTopBarCoverageTest {
    @Test
    fun missingOrProvisionalLimitKeepsMeasuredFallback() {
        for (limit in listOf(null, 1f, -201f, Float.NaN, Float.NEGATIVE_INFINITY)) {
            assertEquals(220f, topBarCoveredHeight(200f, 220f, 56f, limit) {
                error("Invalid limit must not read the scroll offset")
            }, 0f)
        }
    }

    @Test
    fun validCollapseTracksMeasuredTitleAndClampsOffset() {
        assertEquals(120f, topBarCoveredHeight(200f, 220f, 56f, -80f) { 0f }, 0f)
        assertEquals(160f, topBarCoveredHeight(200f, 220f, 56f, -80f) { -40f }, 0f)
        assertEquals(200f, topBarCoveredHeight(200f, 220f, 56f, -80f) { -80f }, 0f)
        assertEquals(200f, topBarCoveredHeight(200f, 220f, 56f, -80f) { -120f }, 0f)
        assertEquals(120f, topBarCoveredHeight(200f, 220f, 56f, -80f) { 10f }, 0f)
        assertEquals(120f, topBarCoveredHeight(200f, 220f, 56f, -80f) { Float.NaN }, 0f)
        assertEquals(200f, topBarCoveredHeight(200f, 220f, 56f, 0f) { -10f }, 0f)
    }

    @Test
    fun validBandNeverFallsBelowCollapsedTitleHeight() {
        assertEquals(56f, topBarCoveredHeight(200f, 220f, 56f, -200f) { 0f }, 0f)
        assertEquals(56f, topBarCoveredHeight(200f, 220f, 56f, -180f) { -10f }, 0f)
    }
}
