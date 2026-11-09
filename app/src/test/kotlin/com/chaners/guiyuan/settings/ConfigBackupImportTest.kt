package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigBackupImportTest {
    @Test
    fun releasedV1TwoLayoutBackupAddsThirdDefaultsAndPreservesEveryExistingField() {
        val full = sampleBackup()
        val original = full.copy(visual = full.visual.copy(
            profiles = full.visual.profiles.filter { it.layout != ContentLayout.MOBILE_CENTER },
        ))
        val imported = ConfigBackupFormat.decode(rawBackup(original))
        assertEquals(6, imported.visual.profiles.size)
        for (old in original.visual.profiles) {
            assertEquals(old, imported.visual.profiles.first {
                it.dualSignal == old.dualSignal && it.layout == old.layout
            })
        }
        for (dual in listOf(false, true)) {
            val third = imported.visual.profiles.first {
                it.dualSignal == dual && it.layout == ContentLayout.MOBILE_CENTER
            }
            assertEquals(0.85f, third.mobileTypeScale, 0f)
            assertEquals(-3f, third.mobileTypeBottomOffsetY, 0f)
            assertTrue(third.centeredMobileSignal)
        }
        assertEquals(imported, ConfigBackupFormat.decode(ConfigBackupFormat.encode(imported)))
    }

    @Test
    fun importClampsAllBoundedVisualControlsAndLowBatteryThreshold() {
        val original = sampleBackup()
        val damaged = original.copy(
            feature = original.feature.copy(lowBatteryThreshold = 900),
            visual = original.visual.copy(profiles = original.visual.profiles.mapIndexed { i, c ->
                if (i != 0) c else c.copy(
                    topTextScale = 900f,
                    topTextWeight = 5000,
                    topOffsetY = 900f,
                    wifiOffsetY = -500f,
                    mobileTypeOffsetY = 500f,
                    curvedSignalDotOffsetY = -500f,
                    horizontalSignalDotOffsetY = 500f,
                    mobileTypeBottomOffsetY = -500f,
                    bothSidesActivityScale = 500f,
                    bothSidesActivityOffsetY = -500f,
                    bothSidesChargingOffsetY = 500f,
                    topChargingIconScale = 500f,
                    combinedScale = 500f,
                    outerWeightScale = 500f,
                    wifiScale = 500f,
                    wifiWeight = 999,
                    airplaneScale = 500f,
                    airplaneWeight = 999,
                    noSimScale = 500f,
                    mobileTypeScale = 500f,
                    mobileTypeWeight = 5000,
                )
            }),
        )
        val imported = ConfigBackupFormat.decode(rawBackup(damaged))
        assertEquals(LOW_BATTERY_THRESHOLD_MAX, imported.feature.lowBatteryThreshold)
        val visual = imported.visual.profiles.first()
        assertEquals(BATTERY_TOP_TEXT_SCALE_MAX, visual.topTextScale, 0f)
        assertEquals(BATTERY_TOP_TEXT_WEIGHT_MAX, visual.topTextWeight)
        assertEquals(BATTERY_TOP_VERTICAL_OFFSET_MAX, visual.topOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MIN, visual.wifiOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MAX, visual.mobileTypeOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MIN, visual.curvedSignalDotOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MAX, visual.horizontalSignalDotOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MIN, visual.mobileTypeBottomOffsetY, 0f)
        assertEquals(BOTH_SIDES_BOTTOM_INFO_SCALE_MAX, visual.bothSidesActivityScale, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MIN, visual.bothSidesActivityOffsetY, 0f)
        assertEquals(NETWORK_TOP_OFFSET_MAX, visual.bothSidesChargingOffsetY, 0f)
        assertEquals(BATTERY_TOP_CHARGING_ICON_SCALE_MAX, visual.topChargingIconScale, 0f)
        assertEquals(COMBINED_SCALE_MAX, visual.combinedScale, 0f)
        assertEquals(OUTER_WEIGHT_SCALE_MAX, visual.outerWeightScale, 0f)
        assertEquals(WIFI_SIZE_SCALE_MAX, visual.wifiScale, 0f)
        assertEquals(WIFI_WEIGHT_MAX, visual.wifiWeight)
        assertEquals(AIRPLANE_SIZE_SCALE_MAX, visual.airplaneScale, 0f)
        assertEquals(AIRPLANE_WEIGHT_MAX, visual.airplaneWeight)
        assertEquals(NO_SIM_SIZE_SCALE_MAX, visual.noSimScale, 0f)
        assertEquals(MOBILE_TYPE_SIZE_SCALE_MAX, visual.mobileTypeScale, 0f)
        assertEquals(MOBILE_TYPE_WEIGHT_MAX, visual.mobileTypeWeight)
        assertEquals(original.visual.profiles.last(), imported.visual.profiles.last())
    }

    @Test
    fun importRejectsWrongTypesAcrossFeatureGeometryEnumsAndColorEntries() {
        val canonical = ConfigBackupFormat.encode(sampleBackup())
        val attempts = listOf(
            "\"lowBatteryThreshold\":20" to "\"lowBatteryThreshold\":\"20\"",
            "\"wifiScale\":1.0" to "\"wifiScale\":\"1.0\"",
            "\"mobileTypeWeight\":600" to "\"mobileTypeWeight\":\"600\"",
            "\"enabled\":true" to "\"enabled\":\"true\"",
            "\"layout\":\"NETWORK_CENTER\"" to "\"layout\":123",
            "\"layout\":\"NETWORK_CENTER\"" to "\"layout\":\"NOT_A_LAYOUT\"",
            "\"mobileTypeBottomOffsetY\":0.0" to "\"mobileTypeBottomOffsetY\":null",
        )
        for ((original, bad) in attempts) {
            assertTrue("Fixture missing: $original", canonical.contains(original))
            assertThrows(Exception::class.java) {
                ConfigBackupFormat.decode(canonical.replaceFirst(original, bad))
            }
        }
    }

    @Test
    fun importRejectsMissingDuplicateAndConflictingProfilesWithoutRepair() {
        val source = sampleBackup()
        for (invalid in listOf(
            source.copy(visual = source.visual.copy(profiles = source.visual.profiles.dropLast(1))),
            source.copy(visual = source.visual.copy(profiles =
                source.visual.profiles.dropLast(1) + source.visual.profiles.first())),
            source.copy(visual = source.visual.copy(profiles = source.visual.profiles.mapIndexed { i, cfg ->
                if (i == 1) cfg.copy(fillFollowsRetract = !cfg.fillFollowsRetract) else cfg
            })),
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                ConfigBackupFormat.decode(rawBackup(invalid))
            }
        }
    }

    @Test
    fun independentStyleAndLabelOffsetsRoundTripInAllSixProfiles() {
        val source = sampleBackup()
        val varied = source.copy(visual = source.visual.copy(profiles =
            source.visual.profiles.mapIndexed { index, cfg ->
                cfg.copy(
                    dualRowDotArrangement = if (index % 2 == 0) SignalDotArrangement.HORIZONTAL else SignalDotArrangement.CURVED,
                    singleRowDotArrangement = if (index % 3 == 0) SignalDotArrangement.HORIZONTAL else SignalDotArrangement.CURVED,
                    mobileTypeBottomOffsetY = (index - 3).toFloat(),
                    mobileTypeScale = 0.75f + index * 0.05f,
                )
            },
        ))
        assertEquals(varied, ConfigBackupFormat.decode(ConfigBackupFormat.encode(varied)))
    }

    @Test
    fun horizontalOpeningSurvivesBackupsAndDefaultsOffInOlderArchives() {
        val source = sampleBackup()
        val changed = source.copy(visual = source.visual.copy(
            profiles = source.visual.profiles.map { cfg ->
                cfg.copy(fixedLowerOpening = cfg.layout != ContentLayout.NETWORK_CENTER)
            },
        ))
        val encoded = ConfigBackupFormat.encode(changed)
        assertEquals(changed, ConfigBackupFormat.decode(encoded))

        // Older V1 archives have no field for this newly added visual option.
        val oldJson = encoded
            .replace("\"horizontalFixedOpening\":true,", "")
            .replace("\"horizontalFixedOpening\":false,", "")
        assertFalse(oldJson.contains("horizontalFixedOpening"))
        assertTrue(ConfigBackupFormat.decode(oldJson).visual.profiles.all { !it.fixedLowerOpening })

        val invalid = encoded.replaceFirst(
            "\"horizontalFixedOpening\":true",
            "\"horizontalFixedOpening\":\"true\"",
        )
        assertThrows(IllegalArgumentException::class.java) { ConfigBackupFormat.decode(invalid) }
    }

    @Test
    fun importedSingleRowChoiceFollowsActiveModeWithoutChangingDualStyle() {
        val source = sampleBackup()
        val mismatched = source.copy(
            visual = source.visual.copy(profiles = source.visual.profiles.map { cfg ->
                cfg.copy(
                    singleRowDotArrangement =
                        if (cfg.dualSignal) SignalDotArrangement.HORIZONTAL
                        else SignalDotArrangement.CURVED,
                    dualRowDotArrangement =
                        if (cfg.dualSignal) SignalDotArrangement.CURVED
                        else SignalDotArrangement.HORIZONTAL,
                )
            }),
        )
        val restored = ConfigBackupFormat.decode(ConfigBackupFormat.encode(mismatched))
        assertTrue(restored.visual.profiles.all {
            it.singleRowDotArrangement == SignalDotArrangement.HORIZONTAL
        })
        assertEquals(
            mismatched.visual.profiles.map { it.dualRowDotArrangement },
            restored.visual.profiles.map { it.dualRowDotArrangement },
        )
        val singleEnabled = mismatched.copy(
            visual = mismatched.visual.copy(dualEnabled = false),
        )
        val singleRestored = ConfigBackupFormat.decode(ConfigBackupFormat.encode(singleEnabled))
        assertTrue(singleRestored.visual.profiles.all {
            it.singleRowDotArrangement == SignalDotArrangement.CURVED
        })
    }

    @Test
    fun malformedColorAndNonFiniteInputCannotBeSilentlyNormalized() {
        val normal = sampleBackup()
        val badVisual = normal.copy(visual = normal.visual.copy(profiles =
            normal.visual.profiles.mapIndexed { i, v ->
                if (i == 0) v.copy(batteryColorOverrides =
                    v.batteryColorOverrides.copy(charging = 0x00112233)) else v
            },
        ))
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(rawBackup(badVisual))
        }
        val encoded = ConfigBackupFormat.encode(normal)
        for (special in listOf("NaN", "Infinity", "-Infinity")) {
            assertThrows(Exception::class.java) {
                ConfigBackupFormat.decode(
                    encoded.replaceFirst("\"wifiScale\":1.0", "\"wifiScale\":$special"),
                )
            }
        }
    }

    @Test
    fun missingBottomOffsetUsesThirdLayoutDefault() {
        val third = ConfigBackupFormat.encode(sampleBackup())
            .replace(",\"mobileTypeBottomOffsetY\":-3.0", "")
        val imported = ConfigBackupFormat.decode(third)
        assertTrue(imported.visual.profiles.filter { it.layout == ContentLayout.MOBILE_CENTER }
            .all { it.mobileTypeBottomOffsetY == -3f })
    }
}
