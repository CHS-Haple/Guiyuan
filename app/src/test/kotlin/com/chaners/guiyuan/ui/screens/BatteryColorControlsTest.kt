package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.settings.BatterySchemeEntry
import com.chaners.guiyuan.settings.BatterySchemeSource
import com.chaners.guiyuan.settings.BatteryColorSlot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryColorControlsTest {
    @Test
    fun hexParserAcceptsOpaqueColor() {
        assertEquals(0xFF34C759.toInt(), batteryColorFromHex("#34C759"))
        assertEquals(0xFF34C759.toInt(), batteryColorFromHex("34c759"))
        assertNull(batteryColorFromHex("FF34C759"))
        assertNull(batteryColorFromHex("GG0000"))
    }

    @Test
    fun rgbParserAcceptsOpaqueColor() {
        assertEquals(
            0xFFFF0080.toInt(),
            batteryColorFromRgb("255", "0", "128"),
        )
        assertNull(batteryColorFromRgb("256", "0", "0"))
        assertNull(batteryColorFromRgb("", "0", "0"))
    }

    @Test
    fun rgbBreakdownRoundTripsOpaqueColor() {
        assertEquals(
            Triple(36, 104, 172),
            batteryColorRgb(0xFF2468AC.toInt()),
        )
        assertEquals(
            0xFF2468AC.toInt(),
            batteryColorFromRgb("36", "104", "172"),
        )
    }

    @Test
    fun followSystemWithoutCustomHasNoSeed() {
        assertNull(
            batteryColorEditorSeed(
                BatterySchemeEntry(
                    source = BatterySchemeSource.FOLLOW_SYSTEM,
                    customColor = null,
                ),
                BatteryColorSlot.NORMAL,
            ),
        )
    }

    @Test
    fun followSystemKeepsCustomEditorSeed() {
        assertEquals(
            0xFF2468AC.toInt(),
            batteryColorEditorSeed(
                BatterySchemeEntry(
                    source = BatterySchemeSource.FOLLOW_SYSTEM,
                    customColor = 0xFF2468AC.toInt(),
                ),
                BatteryColorSlot.NORMAL,
            ),
        )
    }

    @Test
    fun hexFormatterDropsAlphaButKeepsRgb() {
        assertEquals("#2468AC", batteryColorHex(0x7F2468AC))
    }
}
