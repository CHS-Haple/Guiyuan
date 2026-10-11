package com.chaners.guiyuan.xposed

internal object NativeSlotGeometry {
    internal fun resolveStableChildWidth(
        layoutWidth: Int,
        measuredWidth: Int,
    ): Int? =
        when {
            layoutWidth > 0 -> layoutWidth
            measuredWidth > 0 -> measuredWidth
            else -> null
        }
}
