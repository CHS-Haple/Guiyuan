package com.chaners.guiyuan.xposed

import android.view.View

// Read-only native Keyguard signals; host discovery and Scene ownership stay with the resolver.
internal object SysUiKeyguardNativeSignals {
    private const val TO_LOCK_SCREEN_FIELD = "mToLockScreen"

    internal fun nativeToLockScreenTarget(
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
    ): Boolean? =
        SysUiKeyguardHostResolver.findField(resolved.host.javaClass, TO_LOCK_SCREEN_FIELD)
            ?.let { field ->
                SysUiPresentationRuntime.optionalNativeRead { field.get(resolved.host) as? Boolean }
            }

    internal fun statusIconsPresentationAlpha(
        resolved: SysUiKeyguardHostResolver.ResolvedHost,
    ): Float? {
        val statusIcons = resolved.statusIcons
        if (!statusIcons.isAttachedToWindow) return null
        return resolveStatusIconsPresentationAlpha(
            visible = statusIcons.visibility == View.VISIBLE,
            alpha = statusIcons.alpha,
        )
    }

    internal fun resolveStatusIconsPresentationAlpha(
        visible: Boolean,
        alpha: Float,
    ): Float =
        if (visible) {
            alpha.coerceIn(0f, 1f)
        } else {
            0f
        }
}
