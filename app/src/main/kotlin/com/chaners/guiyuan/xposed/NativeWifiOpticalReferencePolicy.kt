package com.chaners.guiyuan.xposed

internal object WifiOpticalPolicy {
    fun connectedReferenceEntry(resourceName: String?): String? {
        if (!SignalParser.isWifiFamilyResource(resourceName)) {
            return null
        }
        val level =
            (SignalParser.wifi(resourceName) as? SignalStrength.Level)
                ?.value
                ?: return null
        return "stat_sys_wifi_signal_" + level.coerceIn(0, 3)
    }

    fun canShareReferenceViewport(
        currentWidth: Int,
        currentHeight: Int,
        referenceWidth: Int,
        referenceHeight: Int,
    ): Boolean =
        currentWidth > 0 &&
            currentHeight > 0 &&
            currentWidth == referenceWidth &&
            currentHeight == referenceHeight
}
