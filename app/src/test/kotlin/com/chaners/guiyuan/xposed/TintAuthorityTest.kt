package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TintAuthorityTest {
    @Test
    fun statusIconCanBootstrapBeforeNativeTintArrives() {
        val tint = 0xe6ffffff.toInt()
        val resolved =
            TintAuthority.resolveStatusIconEvent(
                nativeTintObserved = false,
                liveStatusIconTint = tint,
            )
        assertEquals(tint, resolved?.appliedTint)
        assertEquals(tint, resolved?.statusIconTint)
    }

    @Test
    fun nativeTintCannotBeOverwrittenByLaterStatusIconEvents() {
        assertNull(
            TintAuthority.resolveStatusIconEvent(
                nativeTintObserved = true,
                liveStatusIconTint = 0xe6ffffff.toInt(),
            ),
        )
    }

    @Test
    fun missingAndTransparentStatusTintCannotBootstrap() {
        assertNull(
            TintAuthority.resolveStatusIconEvent(
                nativeTintObserved = false,
                liveStatusIconTint = null,
            ),
        )
        assertNull(
            TintAuthority.resolveStatusIconEvent(
                nativeTintObserved = false,
                liveStatusIconTint = 0x00112233,
            ),
        )
    }

    @Test
    fun hotReloadUsesFreshPeerOnlyAsProvisionalTint() {
        val transferred =
            TintState(
                appliedTint = 0xbf000000.toInt(),
                statusIconTint = 0xbf000000.toInt(),
            )
        val resolved =
            TintAuthority.rebaseTransferred(
                transferred = transferred,
                liveStatusIconTint = 0xe6ffffff.toInt(),
            )
        assertEquals(0xe6ffffff.toInt(), resolved.appliedTint)
        assertEquals(0xe6ffffff.toInt(), resolved.statusIconTint)
    }

    @Test
    fun hotReloadRetainsTransferWhenLivePeerUnavailable() {
        val transferred =
            TintState(
                appliedTint = 0xbf000000.toInt(),
                statusIconTint = 0xe6ffffff.toInt(),
            )
        assertEquals(
            transferred,
            TintAuthority.rebaseTransferred(
                transferred = transferred,
                liveStatusIconTint = null,
            ),
        )
    }

    @Test
    fun diagnosticTintHexRetainsUnsignedArgbAndStablePrefixes() {
        val tint = 0xf01234ab.toInt()
        assertEquals("f01234ab", tintHexDigits(tint))
        assertEquals("#f01234ab", colorHex(tint))
        assertEquals("00000000", tintHexDigits(0))
        assertEquals("00112233", tintHexDigits(0x00112233))
    }
}
