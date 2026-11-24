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
    fun nativeSceneRequiresMatchingHostAndStateForReloadAndHook() {
        assertEquals(SourceScene.HOME, SysUiSceneSource.steadySourceScene(SourceScene.HOME, SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR))
        assertEquals(SourceScene.UNKNOWN, SysUiSceneSource.steadySourceScene(SourceScene.HOME, SysUiSceneSource.Surface.KEYGUARD))
        assertEquals(SourceScene.UNKNOWN, SysUiSceneSource.steadySourceScene(SourceScene.HOME, SysUiSceneSource.Surface.SHADE_LOCKED))
        assertEquals(SourceScene.UNKNOWN, SysUiSceneSource.steadySourceScene(SourceScene.HOME, null))
        assertEquals(SourceScene.UNKNOWN, SysUiSceneSource.steadySourceScene(SourceScene.UNKNOWN, SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR))
        assertEquals(SourceScene.UNKNOWN, SysUiSceneSource.steadySourceScene(SourceScene.KEYGUARD, SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR))
        assertEquals(SourceScene.KEYGUARD, SysUiSceneSource.steadySourceScene(SourceScene.KEYGUARD, SysUiSceneSource.Surface.KEYGUARD))
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
