package com.chaners.guiyuan.xposed

// Pure native Keyguard/AOD projection facts; the original source owns Hook state.
internal object SysUiKeyguardAodPolicy {
    internal fun blocksKeyguardProjection(
        toAod: Boolean,
        isAodAnimate: Boolean,
    ): Boolean =
        toAod || isAodAnimate

    internal fun isStableAod(
        toAod: Boolean,
        isAodAnimate: Boolean,
    ): Boolean =
        toAod && !isAodAnimate
}
