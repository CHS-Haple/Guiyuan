package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.settings.UiStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidRailEligibilityTest {
    @Test fun pointerBoundaryUsesItemCentresAndCrossesAtActualDivider() {
        val inset = 4f
        val height = 72f
        assertEquals(0f, liquidRailPointerTarget(inset + height - 0.1f, height, inset, 3).toInt().toFloat(), 0f)
        assertEquals(0.5f, liquidRailPointerTarget(inset + height, height, inset, 3), 0.0001f)
        assertEquals(1f, kotlin.math.round(liquidRailPointerTarget(inset + height + 0.1f, height, inset, 3)), 0f)
        assertEquals(2f, liquidRailPointerTarget(999f, height, inset, 3), 0f)
    }

    @Test fun asymmetricVerticalInsetsStillConstrainWideRailEligibility() {
        assertFalse(canPlaceLiquidRail(DpSize(720.dp, 370.dp), UiStyle.OS4, 3,
            LiquidRailSafeArea(0.dp, 0.dp, 72.dp)))
        assertTrue(canPlaceLiquidRail(DpSize(720.dp, 440.dp), UiStyle.OS4, 3,
            LiquidRailSafeArea(0.dp, 0.dp, 72.dp)))
    }
    @Test fun compactOs3AndSpaciousOs4UseTheSameMaterialWithDifferentWidth() {
        assertTrue(canPlaceLiquidRail(DpSize(500.dp, 400.dp), UiStyle.OS3, 3,
            LiquidRailSafeArea(0.dp, 0.dp, 0.dp)))
        assertFalse(canPlaceLiquidRail(DpSize(500.dp, 400.dp), UiStyle.OS4, 3,
            LiquidRailSafeArea(0.dp, 0.dp, 0.dp)))
        assertTrue(canPlaceLiquidRail(DpSize(570.dp, 400.dp), UiStyle.OS4, 3,
            LiquidRailSafeArea(0.dp, 0.dp, 0.dp)))
    }

    @Test fun landscapeLabelDoesNotOverrideTheActualSafeWindow() {
        assertFalse(canPlaceLiquidRail(DpSize(560.dp, 400.dp), UiStyle.OS4, 3,
            LiquidRailSafeArea(18.dp, 12.dp, 0.dp)))
        assertTrue(canPlaceLiquidRail(DpSize(598.dp, 400.dp), UiStyle.OS4, 3,
            LiquidRailSafeArea(18.dp, 12.dp, 0.dp)))
    }

    @Test fun narrowAndVerticallyConstrainedWindowsKeepTheirBottomBar() {
        assertFalse(canPlaceLiquidRail(DpSize(430.dp, 800.dp), UiStyle.OS3, 3,
            LiquidRailSafeArea(0.dp, 0.dp, 0.dp)))
        assertFalse(canPlaceLiquidRail(DpSize(700.dp, 340.dp), UiStyle.OS4, 3,
            LiquidRailSafeArea(0.dp, 0.dp, 30.dp)))
        assertFalse(canPlaceLiquidRail(DpSize(700.dp, 450.dp), UiStyle.OS4, 0,
            LiquidRailSafeArea(0.dp, 0.dp, 0.dp)))
    }
}
