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
    fun steadyHostClassificationUsesVerifiedNativeHost() {
        assertEquals(
            SourceScene.HOME,
            SysUiSceneSource.classifyHost(StatusBarHostCapture.HOST_CLASS_NAME),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            SysUiSceneSource.classifyHost(
                "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
            ),
        )
        assertEquals(
            SourceScene.UNKNOWN,
            SysUiSceneSource.classifyHost(
                "com.android.systemui.statusbar.phone.MiuiNotificationStatusContainer",
            ),
        )
        assertEquals(
            SourceScene.UNKNOWN,
            SysUiSceneSource.classifyHost(
                "com.miui.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons",
            ),
        )
    }
}
