package top.yukonga.miuix.kmp.glass

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Upstream capsule geometry is pure and must not depend on a frame/press. */
class GlassNavigationRailGeometryTest {
    @Test fun roomyWindowUsesUncompressedTabWidths() {
        val geometry = railCapsuleGeometry(1200.dp, listOf(60.dp, 70.dp, 80.dp), false)
        assertEquals(1f, geometry.widthRatio, 0.001f)
        assertEquals(geometry.contentStart + RailCollapsedEndPadding + 210.dp, geometry.width)
    }

    @Test fun toggleReservesLeadingSlotWithoutChangingBarSide() {
        val plain = railCapsuleGeometry(1200.dp, listOf(40.dp, 40.dp), false)
        val toggled = railCapsuleGeometry(1200.dp, listOf(40.dp, 40.dp), true)
        assertEquals(RailLeadingActionWidth, toggled.contentStart - plain.contentStart)
        assertEquals(RailLeadingActionWidth, toggled.width - plain.width)
    }

    @Test fun narrowWindowClampsCapsuleWithoutNegativeWidths() {
        val geometry = railCapsuleGeometry(180.dp, listOf(200.dp, 200.dp, 200.dp), true)
        assertTrue(geometry.width <= 180.dp)
        assertTrue(geometry.widthRatio >= 0f)
    }

    @Test fun emptyDestinationsKeepUnitCompressionRatio() {
        assertEquals(1f, railCapsuleGeometry(600.dp, emptyList(), false).widthRatio, 0.001f)
    }

    @Test fun liquidPanelMovesMonotonicallyAlongTheSameMorphAsItsWidth() {
        val collapsed = 450.dp
        val expanded = GlassNavigationRailDefaults.Margin
        assertEquals(collapsed, railLiquidPanelStart(collapsed, expanded, 0f))
        assertEquals(expanded, railLiquidPanelStart(collapsed, expanded, 1f))
        assertEquals(231.dp, railLiquidPanelStart(collapsed, expanded, 0.5f))
        // Expanded -> collapsed moves toward the screen centre on every frame,
        // never rebounds toward the left while the width is shrinking.
        assertTrue(railLiquidPanelStart(collapsed, expanded, 0.75f) <
            railLiquidPanelStart(collapsed, expanded, 0.5f))
        assertTrue(railLiquidPanelStart(collapsed, expanded, 0.25f) >
            railLiquidPanelStart(collapsed, expanded, 0.5f))
    }

    @Test fun liquidPanelClampsMorphWhenUpdatedDuringResizeOrInterruptedTransition() {
        val collapsed = 490.dp
        val expanded = GlassNavigationRailDefaults.Margin
        assertEquals(collapsed, railLiquidPanelStart(collapsed, expanded, -0.05f))
        assertEquals(expanded, railLiquidPanelStart(collapsed, expanded, 1.05f))
        assertEquals(590.dp, railLiquidPanelStart(590.dp, expanded, 0f))
    }

    @Test fun longRailLabelsCannotPushActionOutsideSafeWindowIncludingRtl() {
        val viewport = 420.dp
        val capsule = railCapsuleGeometry(viewport, listOf(400.dp, 500.dp, 600.dp), true)
        val size = GlassNavigationRailDefaults.CollapsedHeight
        val logicalX = (viewport - capsule.width) / 2f +
            railSearchActionSideOffset(capsule.width, 0f, size)
        assertTrue(logicalX + size <= viewport)
        // Compose's start-relative offset mirrors that rect when RTL.
        val rtlPhysicalX = viewport - logicalX - size
        assertTrue(rtlPhysicalX >= 0.dp)
    }

    @Test fun liquidSearchActionFollowsExactlyTheSamePanelPressScale() {
        // Collapsed: the button is right of the physical panel centre and
        // must follow its 16dp press growth rather than remain at idle X.
        val panelWidth = 320f
        val panelHeight = 54f
        val expanded = com.chaners.guiyuan.ui.components.liquid.liquidPanelPressScale(
            panelWidth, panelHeight, 16f, 1f,
        )
        assertEquals(0f, railSearchActionPressShift(390f, 160f, 1f), 0.001f)
        assertEquals(11.5f, railSearchActionPressShift(390f, 160f, expanded), 0.001f)
        assertTrue(railSearchActionPressShift(390f, 160f, expanded) > 0f)
        // Expanded: the header action lies above the tall panel centre.
        assertTrue(railSearchActionPressShift(50f, 500f, 1.04f) < 0f)
    }

    @Test fun liquidSearchPreplansLensReachWithTheOriginalPressClock() {
        val actionCenter = 359f
        val panelCenter = 160f
        // 16dp panel growth + 22dp lens growth, including 10% X-spring overshoot.
        val reach = com.chaners.guiyuan.ui.components.liquid.liquidActionPressReachPx(
            16f, 22f, 3f,
        )
        val fullScale = com.chaners.guiyuan.ui.components.liquid.liquidPanelPressScale(
            320f, 54f, 16f, 1f,
        )
        val halfScale = com.chaners.guiyuan.ui.components.liquid.liquidPanelPressScale(
            320f, 54f, 16f, 0.5f,
        )
        assertEquals(0f, railSearchActionPlannedShift(
            actionCenter, panelCenter, 1f, 0f, 1f, reach,
        ), 0.001f)
        assertEquals(8.55f, railSearchActionPlannedShift(
            actionCenter, panelCenter, halfScale, 0.5f, 1f, reach,
        ), 0.001f)
        assertEquals(17.1f, railSearchActionPlannedShift(
            actionCenter, panelCenter, fullScale, 1f, 1f, reach,
        ), 0.001f)
        assertEquals(railSearchActionPressShift(50f, 500f, 1.04f),
            railSearchActionPlannedShift(50f, 500f, 1.04f, 1f, 0f, reach), 0.001f)
        // The action has no pressed-tab or lens-animation dependency.
    }

    @Test fun nativeGlassActionNeverReceivesLiquidPressClearance() {
        val density = Density(1f)
        val atRest = with(density) { railTrailingActionOffset(
            0f, 280.dp, 100.dp, 54.dp, 54.dp, 0f, liquid = false,
        ) }
        val pressed = with(density) { railTrailingActionOffset(
            0f, 280.dp, 100.dp, 54.dp, 54.dp, 1f, liquid = false,
        ) }
        assertEquals(atRest, pressed)
        assertEquals(100 + 280 + GlassNavigationBarDefaults.ActionSpacing.value.toInt(), atRest.x)
    }

    @Test fun liquidActionUsesSinglePressClockAndNoSecondLensChase() {
        val density = Density(1f)
        fun position(press: Float) = with(density) {
            railTrailingActionOffset(0f, 280.dp, 100.dp, 54.dp, 54.dp, press, liquid = true)
        }
        val rest = position(0f)
        val half = position(0.5f)
        val full = position(1f)
        assertTrue(half.x > rest.x)
        assertTrue(full.x > half.x)
        assertTrue(full.x > rest.x)
        val expanded = with(density) {
            railTrailingActionOffset(1f, 280.dp, 12.dp, 400.dp, 54.dp, 1f, liquid = true)
        }
        val nativeExpanded = with(density) {
            railTrailingActionOffset(1f, 280.dp, 12.dp, 400.dp, 54.dp, 0f, liquid = false)
        }
        assertEquals(nativeExpanded, expanded)
    }

    @Test fun collapsedRailAlwaysReservesSpaceForIndependentSearchButton() {
        val needed = GlassNavigationBarDefaults.ActionSpacing + GlassNavigationRailDefaults.CollapsedHeight
        assertTrue(needed <= RailCollapsedSideReserve)
    }

    @Test fun searchActionUsesNativeCapsuleEndAndExpandedHeaderSlot() {
        val actionSize = GlassNavigationRailDefaults.CollapsedHeight
        assertEquals(
            180.dp + GlassNavigationBarDefaults.ActionSpacing,
            railSearchActionSideOffset(180.dp, 0f, actionSize),
        )
        assertEquals(
            GlassNavigationRailDefaults.ExpandedWidth - RailExpandedToggleStart - actionSize,
            railSearchActionSideOffset(GlassNavigationRailDefaults.ExpandedWidth, 1f, actionSize),
        )
        assertEquals(
            railSearchActionSideOffset(180.dp, 0f, actionSize),
            railSearchActionSideOffset(180.dp, -1f, actionSize),
        )
        assertEquals(
            railSearchActionSideOffset(GlassNavigationRailDefaults.ExpandedWidth, 1f, actionSize),
            railSearchActionSideOffset(GlassNavigationRailDefaults.ExpandedWidth, 2f, actionSize),
        )
    }

    @Test fun resizingWideWindowKeepsContentOriginAndShrinksSlots() {
        val widths = listOf(100.dp, 100.dp, 100.dp)
        val roomy = railCapsuleGeometry(1200.dp, widths, true)
        val narrow = railCapsuleGeometry(420.dp, widths, true)
        assertEquals(roomy.contentStart, narrow.contentStart)
        assertTrue(narrow.widthRatio < roomy.widthRatio)
    }
}
