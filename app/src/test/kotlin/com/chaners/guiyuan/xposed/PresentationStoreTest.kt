package com.chaners.guiyuan.xposed

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PresentationStoreTest {
    @Test
    fun repeatedConnectivityObservationIsDeduplicatedByValue() {
        PresentationStore.reset()
        val state =
            SysUiConnectivitySource.State(
                known = true,
                transport = SysUiConnectivitySource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )

        val first =
            PresentationStore.updateConnectivity(state)
        val repeated =
            PresentationStore.updateConnectivity(state)

        assertSame(state, first?.connectivity)
        assertNull(repeated)
    }
}
