package com.chaners.guiyuan.xposed

internal sealed interface SignalStrength {
    data object Unknown : SignalStrength
    data object Unavailable : SignalStrength

    data class Level(
        val value: Int,
    ) : SignalStrength

    val logToken: String
        get() = when (this) {
            Unknown -> "unknown"
            Unavailable -> "unavailable"
            is Level -> "level=" + value
        }
}

internal object SignalParser {
    private val mobileLevelPattern = Regex("^stat_sys_signal_([0-4])$")
    private val wifiLevelPattern = Regex("^stat_sys_wifi_signal_([0-3])$")
    private val hotspotWifiLevelPattern =
        Regex("^stat_sys_hotspot_signal_([0-3])$")
    private val wifiFamilyLevelPattern =
        Regex("(?:^|_)([0-3])(?:$|_)")
    private val wifiResourcePrefixes =
        listOf(
            "stat_sys_wifi_signal_",
            "stat_sys_hotspot_signal_",
        )

    fun mobile(resourceName: String?): SignalStrength {
        val entry = resourceEntry(resourceName) ?: return SignalStrength.Unknown
        if (entry == "stat_sys_signal_null") {
            return SignalStrength.Unavailable
        }

        val level = mobileLevelPattern.matchEntire(entry)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: return SignalStrength.Unknown
        return SignalStrength.Level(level)
    }

    fun wifi(resourceName: String?): SignalStrength {
        val entry = resourceEntry(resourceName) ?: return SignalStrength.Unknown
        val prefix =
            wifiResourcePrefixes.firstOrNull(entry::startsWith)
                ?: return SignalStrength.Unknown

        val exactLevel =
            (
                wifiLevelPattern.matchEntire(entry)
                    ?: hotspotWifiLevelPattern.matchEntire(entry)
            )
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
        val variantLevel =
            wifiFamilyLevelPattern
                .find(entry.removePrefix(prefix))
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
        val level = exactLevel ?: variantLevel ?: return SignalStrength.Unknown
        return SignalStrength.Level(level)
    }

    fun isWifiFamilyResource(resourceName: String?): Boolean {
        val entry = resourceEntry(resourceName) ?: return false
        return wifiResourcePrefixes.any(entry::startsWith)
    }

    fun isHotspotWifiResource(resourceName: String?): Boolean {
        val entry = resourceEntry(resourceName) ?: return false
        return entry.startsWith("stat_sys_hotspot_signal_")
    }

    fun wifiInternetValidated(resourceName: String?): Boolean? {
        val entry = resourceEntry(resourceName) ?: return null
        if (
            wifiLevelPattern.matches(entry) ||
            hotspotWifiLevelPattern.matches(entry)
        ) {
            return true
        }
        if (wifiResourcePrefixes.none(entry::startsWith)) {
            return null
        }

        val normalized = entry.lowercase()
        return when {
            normalized.contains("unavailable") -> false
            normalized.contains("no_internet") -> false
            normalized.contains("nointernet") -> false
            normalized.contains("no_network") -> false
            else -> null
        }
    }

    private fun resourceEntry(resourceName: String?): String? {
        if (resourceName.isNullOrBlank()) {
            return null
        }
        return resourceName.substringAfterLast('/').takeIf { it.isNotBlank() }
    }
}
