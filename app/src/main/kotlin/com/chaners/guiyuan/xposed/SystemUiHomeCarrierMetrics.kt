package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup

internal object HomeCarrierMetrics {
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val CARRIER_ID_NAME = "battery_icon_container"

    fun resolveCarrierView(anchor: View): View? {
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

    fun resolveCarrierWidthPx(carrier: View): Int? =
        StatusSlotGeometry.resolveStableChildWidth(
            layoutWidth = carrier.width,
            measuredWidth = carrier.measuredWidth,
        )
}
