package com.chaners.guiyuan.xposed.network

import android.view.View
import android.view.ViewGroup
import com.chaners.guiyuan.xposed.NativeSlotGeometry

internal object SysUiCarrierMetrics {
    // This battery-carrier contract is shared by Home, Keyguard and QS_FAKE.
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val CARRIER_ID_NAME = "battery_icon_container"

    fun resolveView(anchor: View): View? {
        val group = anchor as? ViewGroup ?: return null
        val resourceId =
            anchor.resources.getIdentifier(
                CARRIER_ID_NAME,
                "id",
                SYSTEM_UI_PACKAGE,
            )
        if (resourceId == 0) {
            return null
        }
        return group.findViewById<View>(resourceId)
            ?.takeIf { candidate -> candidate !== anchor }
    }

    // Width can be transient mid-layout, so use the stable native child width.
    fun resolveWidthPx(carrier: View): Int? =
        NativeSlotGeometry.resolveStableChildWidth(
            layoutWidth = carrier.width,
            measuredWidth = carrier.measuredWidth,
        )
}
