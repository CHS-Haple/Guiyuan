package com.chaners.guiyuan.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.junit.Assert.assertEquals
import org.junit.Test

class BrandWaveGlintColorTest {
    private val accent = Color(0xff4b96df)

    @Test
    fun darkSurfaceRetainsExistingThirtyFivePercentBlackBlend() {
        assertEquals(
            lerp(accent, Color.Black, 0.35f),
            brandWaveGlintColor(Color.Black, accent),
        )
    }

    @Test
    fun lightSurfaceRetainsExistingSeventyPercentWhiteBlend() {
        assertEquals(
            lerp(accent, Color.White, 0.70f),
            brandWaveGlintColor(Color.White, accent),
        )
    }
}
