package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SysUiSceneSourceTest {
    @Test
    fun batteryStatusStatesRemainReadOnlyClassifications() {
        assertEquals(
            SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR,
            SysUiSceneSource.classifyRawState(0),
        )
        assertEquals(
            SysUiSceneSource.Surface.KEYGUARD,
            SysUiSceneSource.classifyRawState(1),
        )
        assertEquals(
            SysUiSceneSource.Surface.SHADE_LOCKED,
            SysUiSceneSource.classifyRawState(2),
        )
        assertEquals(
            SysUiSceneSource.Surface.UNKNOWN,
            SysUiSceneSource.classifyRawState(99),
        )
    }

    @Test
    fun steadySourceAuthorityRequiresMatchingStructuralHost() {
        assertEquals(
            SourceScene.HOME,
            SysUiSceneSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.statusbar.phone.MiuiNotificationStatusContainer",
                ),
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            SysUiSceneSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
                ),
            ),
        )
        assertEquals(
            SourceScene.UNKNOWN,
            SysUiSceneSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons",
                ),
            ),
        )
    }
}
