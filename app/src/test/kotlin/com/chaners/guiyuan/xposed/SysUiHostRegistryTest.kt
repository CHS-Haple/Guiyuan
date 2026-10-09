package com.chaners.guiyuan.xposed

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SysUiHostRegistryTest {
    @Test
    fun oldTransferDoesNotReplaceNewHost() {
        val old = Any()
        val replacement = Any()
        SysUiHostRegistry.capture(old)
        SysUiHostRegistry.capture(replacement)

        assertNull(SysUiHostRegistry.restore(old))
        assertSame(replacement, SysUiHostRegistry.current())
    }

    @Test
    fun currentHostCanBeRestored() {
        val host = Any()
        SysUiHostRegistry.capture(host)

        val restored = SysUiHostRegistry.restore(host)

        assertSame(host, restored?.host)
        assertSame(host, SysUiHostRegistry.current())
    }
}
