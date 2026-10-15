package com.chaners.guiyuan.xposed.network

internal object WifiOpticalReference {
    // Hotspot has its own native optical silhouette; regular Wi-Fi keeps level matching.
    fun fitReferenceEntry(resourceName: String?): String? =
        if (SysUiSignalParser.isHotspotWifiResource(resourceName)) null
        else connectedReferenceEntry(resourceName)

    fun connectedReferenceEntry(resourceName: String?): String? {
        if (!SysUiSignalParser.isWifiFamilyResource(resourceName)) {
            return null
        }
        val level =
            (SysUiSignalParser.wifi(resourceName) as? SignalStrength.Level)
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
