package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class HotReloadTransferTest {
    @Test
    fun v9CallbackTransferCannotEnterNewGeneration() {
        var invoked = false
        val oldCallback = Runnable { invoked = true }
        val legacy = arrayOf<Any?>(
            9, Any(), Any(), Any(), null, null, null, null, null, true, oldCallback,
        )

        assertNull(HotReloadTransfer.payloadSizeFor(9))
        assertNull(HotReloadTransfer.restore(legacy))
        assertFalse(invoked)
    }

    @Test
    fun wireVersionRejectsNumericCoercionAndKeepsLegacyIntVersions() {
        for (version in 1..10) {
            assertEquals(version, HotReloadTransfer.readPayloadVersion(version))
        }
        assertNull(HotReloadTransfer.readPayloadVersion(10.5))
        assertNull(HotReloadTransfer.readPayloadVersion(10.0f))
        assertNull(HotReloadTransfer.readPayloadVersion(10L))
        assertNull(HotReloadTransfer.readPayloadVersion("10"))
        assertNull(HotReloadTransfer.readPayloadVersion(null))
        // Version 9 remains a well-formed integer but is unsafe to transfer.
        assertEquals(9, HotReloadTransfer.readPayloadVersion(9))
        assertNull(HotReloadTransfer.payloadSizeFor(9))
    }

    @Test
    fun restoredTintsRequireIntWireValuesAndVisibleAlpha() {
        val opaque = 0xff123456.toInt()
        val translucent = 0x0f123456
        val transparent = 0x00123456
        assertEquals(opaque, HotReloadTransfer.readOpaqueTint(arrayOf(opaque), 0))
        assertEquals(translucent, HotReloadTransfer.readOpaqueTint(arrayOf(translucent), 0))
        assertNull(HotReloadTransfer.readOpaqueTint(arrayOf(transparent), 0))
        assertNull(HotReloadTransfer.readOpaqueTint(arrayOf(0xff123456L), 0))
        assertNull(HotReloadTransfer.readOpaqueTint(arrayOf(opaque.toDouble()), 0))
        assertNull(HotReloadTransfer.readOpaqueTint(arrayOf("0xff123456"), 0))
        assertNull(HotReloadTransfer.readOpaqueTint(arrayOf(opaque), 1))
    }

    @Test
    fun neutralTransferVersionsRetainTheirOriginalWireSizes() {
        assertEquals(10, HotReloadTransfer.payloadSizeFor(10))
        assertEquals(9, HotReloadTransfer.payloadSizeFor(8))
        assertEquals(8, HotReloadTransfer.payloadSizeFor(7))
        assertEquals(6, HotReloadTransfer.payloadSizeFor(6))
        assertEquals(5, HotReloadTransfer.payloadSizeFor(5))
        assertEquals(4, HotReloadTransfer.payloadSizeFor(4))
        assertEquals(6, HotReloadTransfer.payloadSizeFor(3))
        assertEquals(5, HotReloadTransfer.payloadSizeFor(2))
        assertEquals(4, HotReloadTransfer.payloadSizeFor(1))
        assertNull(HotReloadTransfer.payloadSizeFor(11))
        assertNull(HotReloadTransfer.payloadSizeFor(0))
    }
}
