package top.yukonga.miuix.kmp.glass

import androidx.compose.ui.unit.dp
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

    @Test fun resizingWideWindowKeepsContentOriginAndShrinksSlots() {
        val widths = listOf(100.dp, 100.dp, 100.dp)
        val roomy = railCapsuleGeometry(1200.dp, widths, true)
        val narrow = railCapsuleGeometry(420.dp, widths, true)
        assertEquals(roomy.contentStart, narrow.contentStart)
        assertTrue(narrow.widthRatio < roomy.widthRatio)
    }
}
