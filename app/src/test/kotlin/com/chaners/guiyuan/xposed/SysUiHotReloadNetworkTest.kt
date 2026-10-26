package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiHotReloadNetworkTest {
    @Test
    fun hiddenWifiNeedsNoLiveBinding() {
        assertTrue(SysUiHotReload.networkTransferReady(StatusStateStore.WifiState.Hidden, 0))
    }

    @Test
    fun unknownWifiNeedsNoLiveBinding() {
        assertTrue(SysUiHotReload.networkTransferReady(StatusStateStore.WifiState.Unknown, 0))
    }

    @Test
    fun visibleWifiNeedsLiveBindingForSeed() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = null,
                signal = SignalStrength.Level(3),
            )
        assertFalse(SysUiHotReload.networkTransferReady(wifi, 0))
        assertTrue(SysUiHotReload.networkTransferReady(wifi, 1))
    }
}
