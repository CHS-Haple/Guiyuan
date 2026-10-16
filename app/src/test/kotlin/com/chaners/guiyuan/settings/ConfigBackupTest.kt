package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigBackupTest {
    private fun sample(): ConfigBackup = ConfigBackup(
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

    @Test fun roundTripKeepsEveryProfileAndActiveMode() {
        val source = sample()
        assertEquals(source, ConfigBackupFormat.decode(ConfigBackupFormat.encode(source)))
        assertEquals(4, source.visual.profiles.size)
    }

    @Test fun legacySchemaOneWithoutWifiWeightRetainsNativeDefault() {
        val source = ConfigBackupFormat.encode(sample())
        val withoutWeight = source.replace("\"wifiWeight\":100,", "")
        assertEquals(4, ConfigBackupFormat.decode(withoutWeight).visual.profiles.size)
        assertTrue(ConfigBackupFormat.decode(withoutWeight).visual.profiles.all {
            it.wifiWeight == WIFI_WEIGHT_DEFAULT
        })
    }

    @Test fun tamperedWifiWeightIsRejected() {
        val value = sample()
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(value.copy(
                visual = value.visual.copy(
                    profiles = value.visual.profiles.mapIndexed { i, cfg ->
                        if (i == 0) cfg.copy(wifiWeight = 999) else cfg
                    },
                ),
            ))
        }
    }

    @Test fun alteredScaleIsRejectedWithoutClamping() {
        val source = sample()
        val invalid = source.copy(
            visual = source.visual.copy(
                profiles = source.visual.profiles.mapIndexed { index, value ->
                    if (index == 0) value.copy(combinedScale = 5f) else value
                },
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(invalid)
        }
    }

    @Test fun missingProfileAndDuplicateSchemeIdAreRejected() {
        val source = sample()
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(source.copy(
                visual = source.visual.copy(profiles = source.visual.profiles.dropLast(1)),
            ))
        }
        val custom = BatteryCustomColorScheme(
            1, "A", BatteryBuiltInColorScheme.HYPEROS, BatteryColorSchemeEntries(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(source.copy(
                batterySchemes = BatteryColorSchemeLibrary(customSchemes = listOf(custom, custom)),
            ))
        }
    }

    @Test fun unknownFutureSchemaIsNotGuessed() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(sample().copy(schemaVersion = 2))
        }
    }

    @Test fun unknownEnumAndOversizedFileAreRejected() {
        val json = ConfigBackupFormat.encode(sample())
        assertThrows(Exception::class.java) {
            ConfigBackupFormat.decode(json.replace("NETWORK_CENTER", "UNKNOWN_LAYOUT"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(" ".repeat(ConfigBackupFormat.MAX_BYTES + 1))
        }
    }

    @Test fun appSettingsNeverEnterArchive() {
        val json = ConfigBackupFormat.encode(sample())
        assertTrue(!json.contains("uiScalePercent"))
        assertTrue(!json.contains("navStyle"))
        assertTrue(!json.contains("preview"))
    }
}
