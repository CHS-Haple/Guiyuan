package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SysUiSceneSourceTest {
    @Test
    fun batteryStatusStatesRemainReadOnlyClassifications() {
        assertEquals(
            SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR,
            SceneClassificationPolicy.classifyRawState(0),
        )
        assertEquals(
            SysUiSceneSource.Surface.KEYGUARD,
            SceneClassificationPolicy.classifyRawState(1),
        )
        assertEquals(
            SysUiSceneSource.Surface.SHADE_LOCKED,
            SceneClassificationPolicy.classifyRawState(2),
        )
        assertEquals(
            SysUiSceneSource.Surface.UNKNOWN,
            SceneClassificationPolicy.classifyRawState(99),
        )
    }

    @Test
    fun nativeSceneRequiresMatchingHostAndStateForReloadAndHook() {
        assertEquals(SourceScene.HOME, SceneClassificationPolicy.steadySourceScene(SourceScene.HOME, SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR))
        assertEquals(SourceScene.UNKNOWN, SceneClassificationPolicy.steadySourceScene(SourceScene.HOME, SysUiSceneSource.Surface.KEYGUARD))
        assertEquals(SourceScene.UNKNOWN, SceneClassificationPolicy.steadySourceScene(SourceScene.HOME, SysUiSceneSource.Surface.SHADE_LOCKED))
        assertEquals(SourceScene.UNKNOWN, SceneClassificationPolicy.steadySourceScene(SourceScene.HOME, null))
        assertEquals(SourceScene.UNKNOWN, SceneClassificationPolicy.steadySourceScene(SourceScene.UNKNOWN, SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR))
        assertEquals(SourceScene.UNKNOWN, SceneClassificationPolicy.steadySourceScene(SourceScene.KEYGUARD, SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR))
        assertEquals(SourceScene.KEYGUARD, SceneClassificationPolicy.steadySourceScene(SourceScene.KEYGUARD, SysUiSceneSource.Surface.KEYGUARD))
    }

    @Test
    fun steadyHostClassificationUsesVerifiedNativeHost() {
        assertEquals(
            SourceScene.HOME,
            SceneClassificationPolicy.classifyHost(StatusBarHostCapture.HOST_CLASS_NAME),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            SceneClassificationPolicy.classifyHost(
                "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
            ),
        )
        assertEquals(
            SourceScene.UNKNOWN,
            SceneClassificationPolicy.classifyHost(
                "com.android.systemui.statusbar.phone.MiuiNotificationStatusContainer",
            ),
        )
        assertEquals(
            SourceScene.UNKNOWN,
            SceneClassificationPolicy.classifyHost(
                "com.miui.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons",
            ),
        )
    }
}
