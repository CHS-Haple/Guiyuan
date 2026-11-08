package com.chaners.guiyuan.settings

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal fun sampleBackup(): ConfigBackup = ConfigBackup(
        format = "guiyuan-config",
        schemaVersion = 1,
        appVersion = "0.7.0",
        createdAt = "2026-10-16T20:43:00+08:00",
        feature = FeatureCfg(),
        visual = VisualBackup(
            singleLayout = ContentLayout.NETWORK_CENTER,
            dualLayout = ContentLayout.BATTERY_CENTER,
            dualEnabled = true,
            dualInitialized = true,
            profiles = listOf(false, true).flatMap { dual ->
                ContentLayout.entries.map { layout -> VisualCfg(layout = layout, dualSignal = dual) }
            },
        ),
        batterySchemes = BatteryColorSchemeLibrary(),
    )

private val wire = Json { encodeDefaults = true }
    internal fun rawBackup(backup: ConfigBackup): String = wire.encodeToString(backup)
