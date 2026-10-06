package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SceneSourceTest {
    @Test
    fun batteryStatusStatesRemainReadOnlyClassifications() {
        assertEquals(
            SceneSource.Surface.UNLOCKED_STATUS_BAR,
            SceneSource.classifyRawState(0),
        )
        assertEquals(
            SceneSource.Surface.KEYGUARD,
            SceneSource.classifyRawState(1),
        )
        assertEquals(
            SceneSource.Surface.SHADE_LOCKED,
            SceneSource.classifyRawState(2),
        )
        assertEquals(
            SceneSource.Surface.UNKNOWN,
            SceneSource.classifyRawState(99),
        )
    }

    @Test
    fun steadySourceAuthorityRequiresMatchingStructuralHost() {
        assertEquals(
            SourceScene.HOME,
            SceneSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.statusbar.phone.MiuiNotificationStatusContainer",
                ),
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            SceneSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
                ),
            ),
        )
        assertEquals(
            SourceScene.UNKNOWN,
            SceneSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons",
                ),
            ),
        )
    }
}
