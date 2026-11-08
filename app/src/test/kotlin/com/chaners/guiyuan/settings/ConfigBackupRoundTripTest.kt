package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigBackupRoundTripTest {
    @Test fun roundTripKeepsEveryProfileAndActiveMode() {
        val source = sampleBackup()
        assertEquals(source, ConfigBackupFormat.decode(ConfigBackupFormat.encode(source)))
        assertEquals(6, source.visual.profiles.size)
    }

    @Test fun mobileWeight150RoundTripsInEveryProfile() {
        val archive = sampleBackup().let { source ->
            source.copy(visual = source.visual.copy(
                profiles = source.visual.profiles.map { it.copy(mobileTypeWeight = 150) },
            ))
        }
        assertEquals(archive, ConfigBackupFormat.decode(ConfigBackupFormat.encode(archive)))
    }

    @Test fun batteryWeight150RoundTripsInEveryProfile() {
        val archive = sampleBackup().let { source ->
            source.copy(visual = source.visual.copy(
                profiles = source.visual.profiles.map { it.copy(topTextWeight = 150) },
            ))
        }
        assertEquals(archive, ConfigBackupFormat.decode(ConfigBackupFormat.encode(archive)))
    }

    @Test fun legacySchemaOneWithoutWifiWeightRetainsNativeDefault() {
        val source = ConfigBackupFormat.encode(sampleBackup())
        val withoutWeight = source.replace("\"wifiWeight\":100,", "")
        assertEquals(6, ConfigBackupFormat.decode(withoutWeight).visual.profiles.size)
        assertTrue(ConfigBackupFormat.decode(withoutWeight).visual.profiles.all {
            it.wifiWeight == WIFI_WEIGHT_DEFAULT
        })
    }

    @Test fun extendedWifiWeightsSurviveArchiveRoundTrip() {
        val source = sampleBackup()
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
        val source = sampleBackup()
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
        val json = ConfigBackupFormat.encode(sampleBackup())
        val previous = json.replace("\"airplaneWeight\":100,", "")
        assertTrue(ConfigBackupFormat.decode(previous).visual.profiles.all {
            it.airplaneWeight == AIRPLANE_WEIGHT_DEFAULT
        })
    }

    @Test fun missingProfileAndDuplicateSchemeIdAreRejected() {
        val source = sampleBackup()
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
}
