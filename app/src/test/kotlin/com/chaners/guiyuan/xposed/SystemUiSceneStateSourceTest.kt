package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUiSceneStateSourceTest {
    @Test
    fun batteryStatusStatesRemainReadOnlyClassifications() {
        assertEquals(
            SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
            SystemUiSceneStateSource.classifyRawState(0),
        )
        assertEquals(
            SystemUiSceneStateSource.Surface.KEYGUARD,
            SystemUiSceneStateSource.classifyRawState(1),
        )
        assertEquals(
            SystemUiSceneStateSource.Surface.SHADE_LOCKED,
            SystemUiSceneStateSource.classifyRawState(2),
        )
        assertEquals(
            SystemUiSceneStateSource.Surface.UNKNOWN,
            SystemUiSceneStateSource.classifyRawState(99),
        )
    }

    @Test
    fun steadySourceAuthorityRequiresMatchingStructuralHost() {
        assertEquals(
            SourceScene.HOME,
            SystemUiSceneStateSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.statusbar.phone.MiuiNotificationStatusContainer",
                ),
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            SystemUiSceneStateSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
                ),
            ),
        )
        assertEquals(
            SourceScene.UNKNOWN,
            SystemUiSceneStateSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons",
                ),
            ),
        )
    }
}
