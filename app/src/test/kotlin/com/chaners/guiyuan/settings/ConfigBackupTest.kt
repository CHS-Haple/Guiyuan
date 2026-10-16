package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun extendedWifiWeightsSurviveArchiveRoundTrip() {
        val source = sample()
        val archive = source.copy(
            visual = source.visual.copy(
                profiles = source.visual.profiles.mapIndexed { i, cfg ->
                    cfg.copy(wifiWeight = if (i % 2 == 0) WIFI_WEIGHT_MIN else WIFI_WEIGHT_MAX)
                },
            ),
        )
        assertEquals(archive, ConfigBackupFormat.decode(ConfigBackupFormat.encode(archive)))
    }

    @Test fun airplaneWeightRoundTripsInIndependentProfiles() {
        val source = sample()
        val archive = source.copy(
            visual = source.visual.copy(
                profiles = source.visual.profiles.mapIndexed { index, cfg ->
                    cfg.copy(airplaneWeight = if (index % 2 == 0) AIRPLANE_WEIGHT_MIN else AIRPLANE_WEIGHT_MAX)
                },
            ),
        )
        assertEquals(archive, ConfigBackupFormat.decode(ConfigBackupFormat.encode(archive)))
    }

    @Test fun legacySchemaOneWithoutAirplaneWeightDefaultsToNative() {
        val json = ConfigBackupFormat.encode(sample())
        val previous = json.replace("\"airplaneWeight\":100,", "")
        assertTrue(ConfigBackupFormat.decode(previous).visual.profiles.all {
            it.airplaneWeight == AIRPLANE_WEIGHT_DEFAULT
        })
    }

    @Test fun outOfRangeAirplaneWeightIsRejected() {
        val source = sample()
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(source.copy(
                visual = source.visual.copy(
                    profiles = source.visual.profiles.mapIndexed { i, cfg ->
                        if (i == 0) cfg.copy(airplaneWeight = 500) else cfg
                    },
                ),
            ))
        }
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


    @Test fun duplicateAndEscapedJsonKeysAreRejected() {
        val json = ConfigBackupFormat.encode(sample())
        val duplicated = json.replaceFirst(
            "\"format\":",
            "\"format\":\"spoofed\",\"format\":",
        )
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(duplicated)
        }
        val escaped = json.replaceFirst(
            "\"format\":",
            "\"format\":\"spoofed\",\"\\u0066ormat\":",
        )
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(escaped)
        }
    }

    @Test fun deepOrUnknownJsonStructuresAreRejected() {
        val json = ConfigBackupFormat.encode(sample())
        val nested = "[".repeat(17) + "0" + "]".repeat(17)
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(json.replaceFirst("{", "{\"unexpected\":" + nested + ","))
        }
        assertThrows(Exception::class.java) {
            ConfigBackupFormat.decode(json.replaceFirst("{", "{\"command\":\"reboot\","))
        }
    }

    @Test fun invalidUtf8AndNoncanonicalSchemeKeysAreRejected() {
        assertThrows(Exception::class.java) {
            ConfigBackupFormat.decode(byteArrayOf(0xC3.toByte(), 0x28))
        }
        val custom = BatteryCustomColorScheme(
            id = 1,
            name = "A",
            baseTemplate = BatteryBuiltInColorScheme.HYPEROS,
            entries = BatteryColorSchemeEntries(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(sample().copy(
                batterySchemes = BatteryColorSchemeLibrary(
                    activeSchemeKey = "custom:01",
                    customSchemes = listOf(custom),
                ),
            ))
        }
    }

    @Test fun unsafeSchemeNamesAndMetadataAreRejected() {
        val custom = BatteryCustomColorScheme(
            id = 1,
            name = "Name\nInjected",
            baseTemplate = BatteryBuiltInColorScheme.HYPEROS,
            entries = BatteryColorSchemeEntries(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(sample().copy(
                batterySchemes = BatteryColorSchemeLibrary(customSchemes = listOf(custom)),
            ))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(sample().copy(appVersion = "0.7\u202Eexe"))
        }
    }

    @Test fun importOwnsOnlyFeatureVisualAndSchemeKeys() {
        assertTrue(isBackupVisualKey(
            visualProfileKey(ContentLayout.NETWORK_CENTER, WIFI_WEIGHT_KEY, dual = true),
        ))
        assertTrue(isBackupVisualKey(BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY))
        assertTrue(isBackupVisualKey("battery_color_scheme_library.custom.1.name"))
        assertFalse(isBackupVisualKey("application_ui_scale"))
        assertFalse(isBackupVisualKey("scope_authorization"))
        assertFalse(isBackupVisualKey("root_command"))
    }

    @Test fun appSettingsNeverEnterArchive() {
        val json = ConfigBackupFormat.encode(sample())
        assertTrue(!json.contains("uiScalePercent"))
        assertTrue(!json.contains("navStyle"))
        assertTrue(!json.contains("preview"))
    }
}
