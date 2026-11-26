package com.chaners.guiyuan.xposed

/** Native scene identities and raw state matching are pure; hook/cache ownership stays in SysUiSceneSource. */
internal object SceneClassificationPolicy {
    private const val KEYGUARD_HOST_CLASS_NAME =
        "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView"
    private const val STATUS_BAR_STATE_SHADE = 0
    private const val STATUS_BAR_STATE_KEYGUARD = 1
    private const val STATUS_BAR_STATE_SHADE_LOCKED = 2

    internal fun steadySourceScene(structural: SourceScene, surface: SysUiSceneSource.Surface?): SourceScene =
        when (structural) {
            SourceScene.HOME ->
                if (surface == SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR) {
                    SourceScene.HOME
                } else {
                    SourceScene.UNKNOWN
                }

            SourceScene.KEYGUARD ->
                if (
                    surface == SysUiSceneSource.Surface.KEYGUARD ||
                    surface == SysUiSceneSource.Surface.SHADE_LOCKED
                ) {
                    SourceScene.KEYGUARD
                } else {
                    SourceScene.UNKNOWN
                }

            SourceScene.UNKNOWN -> SourceScene.UNKNOWN
        }

    internal fun classifyHost(className: String): SourceScene =
        when (className) {
            StatusBarHostCapture.HOST_CLASS_NAME -> SourceScene.HOME
            KEYGUARD_HOST_CLASS_NAME -> SourceScene.KEYGUARD
            else -> SourceScene.UNKNOWN
        }

    internal fun classifyRawState(rawState: Int): SysUiSceneSource.Surface =
        when (rawState) {
            STATUS_BAR_STATE_SHADE -> SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR
            STATUS_BAR_STATE_KEYGUARD -> SysUiSceneSource.Surface.KEYGUARD
            STATUS_BAR_STATE_SHADE_LOCKED -> SysUiSceneSource.Surface.SHADE_LOCKED
            else -> SysUiSceneSource.Surface.UNKNOWN
        }
}
