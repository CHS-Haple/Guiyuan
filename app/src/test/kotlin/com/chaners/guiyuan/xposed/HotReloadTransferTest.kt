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
