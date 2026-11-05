package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConfigBackupTest {
    @Test fun bothSidesNamesKeepReleasedBackupSchema() {
        val config = VisualCfg(
            dualRowDotArrangement = SignalDotArrangement.BOTH_SIDES,
            bothSidesShowActivity = false,
            bothSidesHideIdleActivity = true,
            bothSidesBottomCharging = true,
            bothSidesActivityScale = 1.1f,
            bothSidesActivityOffsetY = 2f,
            bothSidesChargingOffsetY = -2f,
            bothSidesSignalDotOffsetY = 3f,
        )
        val codec = Json { encodeDefaults = true }
        val encoded = codec.encodeToString(config)
        for (name in listOf(
            "dualDotStyle", "leftRightShowActivity", "leftRightHideIdleActivity",
            "leftRightBottomCharging", "leftRightBottomInfoScale",
            "leftRightActivityOffsetY", "leftRightChargingOffsetY", "leftRightSignalDotOffsetY",
        )) {
            assertTrue("Missing previous backup field: $name", encoded.contains("\"$name\""))
        }
        assertTrue(encoded.contains("\"LEFT_RIGHT\""))
        assertFalse(encoded.contains("\"bothSidesShowActivity\""))
        assertEquals(config, codec.decodeFromString(VisualCfg.serializer(), encoded))
    }

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

    @Test fun chargingMasterKeepsExistingBackupFieldName() {
        val original = sample().let { backup ->
            backup.copy(visual = backup.visual.copy(profiles =
                backup.visual.profiles.map { it.copy(showChargingIcon = false) }))
        }
        val encoded = ConfigBackupFormat.encode(original)
        assertTrue(encoded.contains("\"showTopChargingIcon\":false"))
        assertFalse(encoded.contains("\"showChargingIcon\""))
        assertEquals(original, ConfigBackupFormat.decode(encoded))
    }

    @Test fun idleActivityHidingIsOptInAndSurvivesBackups() {
        val selected = VisualCfg(
            layout = ContentLayout.NETWORK_CENTER,
            dualSignal = true,
            dualRowDotArrangement = SignalDotArrangement.BOTH_SIDES,
            bothSidesShowActivity = true,
            bothSidesHideIdleActivity = true,
        )
        assertFalse(VisualCfg().bothSidesHideIdleActivity)
        val original = sample().let { source ->
            source.copy(visual = source.visual.copy(profiles = source.visual.profiles.map { cfg ->
                if (cfg.dualSignal && cfg.layout == ContentLayout.NETWORK_CENTER) selected else cfg
            }))
        }
        assertEquals(original, ConfigBackupFormat.decode(ConfigBackupFormat.encode(original)))
    }

    @Test fun retiredIdleShowBackupDoesNotBecomeHide() {
        val old = ConfigBackupFormat.encode(sample())
            .replace("\"leftRightHideIdleActivity\":false", "\"leftRightShowIdleActivity\":true")
        val restored = ConfigBackupFormat.decode(old)
        assertTrue(restored.visual.profiles.none { it.bothSidesHideIdleActivity })
        val conflict = old.replaceFirst(
            "\"leftRightShowIdleActivity\":true",
            "\"leftRightShowIdleActivity\":true,\"leftRightHideIdleActivity\":false",
        )
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(conflict)
        }
    }

    @Test fun bothSidesOffsetIsIndependentOfCurvedAndSurvivesBackups() {
        val value = VisualCfg(
            dualSignal = true,
            dualRowDotArrangement = SignalDotArrangement.BOTH_SIDES,
            curvedSignalDotOffsetY = -3f,
            horizontalSignalDotOffsetY = 2f,
            bothSidesSignalDotOffsetY = 6f,
        )
        assertEquals(0f, value.dualRowDotOffsetY, 0f)
        assertEquals(-3f,
            value.copy(dualRowDotArrangement = SignalDotArrangement.CURVED).dualRowDotOffsetY, 0f)
        assertEquals(2f,
            value.copy(dualRowDotArrangement = SignalDotArrangement.HORIZONTAL).dualRowDotOffsetY, 0f)
        assertEquals(10f, value.copy(bothSidesSignalDotOffsetY = 100f)
            .normalized().bothSidesSignalDotOffsetY, 0f)
        val original = sample().let { source ->
            source.copy(visual = source.visual.copy(profiles = source.visual.profiles.map { cfg ->
                if (cfg.dualSignal) value.copy(layout = cfg.layout) else cfg
            }))
        }
        assertEquals(original, ConfigBackupFormat.decode(ConfigBackupFormat.encode(original)))
    }

    @Test fun bothSidesBottomSizeNormalizesAndPersistsInLayoutProfile() {
        val selected = VisualCfg(
            layout = ContentLayout.NETWORK_CENTER,
            dualSignal = true,
            dualRowDotArrangement = SignalDotArrangement.BOTH_SIDES,
            bothSidesActivityScale = 1.15f,
            bothSidesActivityOffsetY = 3f,
            bothSidesChargingOffsetY = -4f,
        )
        assertEquals(0.7f, selected.copy(bothSidesActivityScale = 0.2f)
            .normalized().bothSidesActivityScale, 0.0001f)
        assertEquals(1.2f, selected.copy(bothSidesActivityScale = 4f)
            .normalized().bothSidesActivityScale, 0.0001f)
        assertEquals(BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT, selected.copy(bothSidesActivityScale = Float.NaN)
            .normalized().bothSidesActivityScale, 0.0001f)
        assertEquals(-10f, selected.copy(bothSidesActivityOffsetY = -100f)
            .normalized().bothSidesActivityOffsetY, 0f)
        assertEquals(10f, selected.copy(bothSidesChargingOffsetY = 100f)
            .normalized().bothSidesChargingOffsetY, 0f)
        val source = sample().let { backup ->
            backup.copy(visual = backup.visual.copy(profiles = backup.visual.profiles.map { cfg ->
                if (cfg.dualSignal && cfg.layout == ContentLayout.NETWORK_CENTER) selected else cfg
            }))
        }
        assertEquals(source, ConfigBackupFormat.decode(ConfigBackupFormat.encode(source)))
    }

    @Test fun rejectsConflictingModeWideValues() {
        val source = sample()
        val first = source.visual.profiles.first()
        val altered = listOf(
            first.copy(fillFollowsRetract = false),
            first.copy(ccTintTransition = false),
            first.copy(batteryColorPreset = BatteryColorPreset.IOS_STYLE),
            first.copy(batteryColorModes = first.batteryColorModes.copy(charging = BatteryColorMode.CUSTOM)),
            first.copy(batteryColorOverrides = first.batteryColorOverrides.copy(charging = 0xFF123456.toInt())),
        )
        altered.forEach { cfg ->
            val invalid = source.copy(
                visual = source.visual.copy(profiles = listOf(cfg) + source.visual.profiles.drop(1)),
            )
            assertThrows(IllegalArgumentException::class.java) {
                ConfigBackupFormat.validate(invalid)
            }
        }
        val json = ConfigBackupFormat.encode(source)
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(
                json.replaceFirst("\"fillFollowsRetract\":true", "\"fillFollowsRetract\":false"),
            )
        }
    }

    @Test fun keepsSingleAndDualModeValuesIndependent() {
        val source = sample()
        val independent = source.copy(
            visual = source.visual.copy(profiles = source.visual.profiles.map { cfg ->
                if (cfg.dualSignal) cfg.copy(
                    fillFollowsRetract = false,
                    batteryColorPreset = BatteryColorPreset.IOS_STYLE,
                ) else cfg
            }),
        )
        assertEquals(independent, ConfigBackupFormat.decode(ConfigBackupFormat.encode(independent)))
    }

    @Test fun descriptiveNamesKeepReleasedBackupJsonStable() {
        val original = sample().let { backup ->
            backup.copy(visual = backup.visual.copy(profiles = backup.visual.profiles.map { profile ->
                profile.copy(
                    dualRowDotArrangement = SignalDotArrangement.HORIZONTAL,
                    singleRowDotArrangement = SignalDotArrangement.HORIZONTAL,
                    curvedSignalDotOffsetY = 2f,
                    horizontalSignalDotOffsetY = -3f,
                )
            }))
        }
        val encoded = ConfigBackupFormat.encode(original)
        assertTrue(encoded.contains("\"dualDotStyle\":\"DUO\""))
        assertTrue(encoded.contains("\"singleRowDotStyle\":\"DUO\""))
        assertTrue(encoded.contains("\"dualDefaultOffsetY\":2.0"))
        assertTrue(encoded.contains("\"dualDuoOffsetY\":-3.0"))
        assertFalse(encoded.contains("\"dualRowDotArrangement\""))
        assertFalse(encoded.contains("\"singleRowDotArrangement\""))
        assertFalse(encoded.contains("\"singleRowFlatDots\""))
        assertEquals(original, ConfigBackupFormat.decode(encoded))
        assertEquals(SignalDotArrangement.HORIZONTAL, SignalDotArrangement.fromPersisted("duo"))
        assertEquals(SignalDotArrangement.CURVED, SignalDotArrangement.fromPersisted("default"))
    }

    @Test fun legacySingleRowBooleanBackupsUpgradeExactlyAndRejectConflicts() {
        val original = sample().let { backup ->
            backup.copy(visual = backup.visual.copy(profiles = backup.visual.profiles.map { profile ->
                profile.copy(
                    dualRowDotArrangement = SignalDotArrangement.HORIZONTAL,
                    singleRowDotArrangement = SignalDotArrangement.HORIZONTAL,
                )
            }))
        }
        val encoded = ConfigBackupFormat.encode(original)
        val old = encoded.replace("\"singleRowDotStyle\":\"DUO\"", "\"singleRowFlatDots\":true")
        assertEquals(original, ConfigBackupFormat.decode(old))
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(old.replaceFirst(
                "\"singleRowFlatDots\":true",
                "\"singleRowFlatDots\":true,\"singleRowDotStyle\":\"DUO\"",
            ))
        }
        val curved = old.replace("\"singleRowFlatDots\":true", "\"singleRowFlatDots\":false")
        assertEquals(SignalDotArrangement.CURVED, ConfigBackupFormat.decode(curved).visual.profiles.first().singleRowDotArrangement)
        val invalid = old.replaceFirst("\"singleRowFlatDots\":true", "\"singleRowFlatDots\":\"true\"")
        assertThrows(IllegalArgumentException::class.java) { ConfigBackupFormat.decode(invalid) }
    }

    @Test fun legacyArchivesDefaultToOriginalDotStyle() {
        val json = ConfigBackupFormat.encode(sample())
            .replace("\"dualDotStyle\":\"LEFT_RIGHT\",", "")
        assertTrue(ConfigBackupFormat.decode(json).visual.profiles.all {
            it.dualRowDotArrangement == SignalDotArrangement.CURVED
        })
    }

    @Test fun offsetValuesSurviveBackupsAndRejectInvalidRanges() {
        val original = sample()
        val changed = original.copy(visual = original.visual.copy(
            profiles = original.visual.profiles.map { cfg ->
                if (cfg.dualSignal) {
                    cfg.copy(
                        curvedSignalDotOffsetY = if (cfg.layout == ContentLayout.NETWORK_CENTER) 4f else -3f,
                        horizontalSignalDotOffsetY = if (cfg.layout == ContentLayout.NETWORK_CENTER) -8f else 6f,
                    )
                } else cfg
            },
        ))
        assertEquals(changed, ConfigBackupFormat.decode(ConfigBackupFormat.encode(changed)))
        val corrupt = changed.copy(visual = changed.visual.copy(
            profiles = changed.visual.profiles.mapIndexed { index, cfg ->
                if (index == 2) cfg.copy(horizontalSignalDotOffsetY = 11f) else cfg
            },
        ))
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(corrupt)
        }
    }

    @Test fun roundTripKeepsEveryProfileAndActiveMode() {
        val source = sample()
        assertEquals(source, ConfigBackupFormat.decode(ConfigBackupFormat.encode(source)))
        assertEquals(6, source.visual.profiles.size)
    }

    @Test fun mobileWeight150RoundTripsInEveryProfile() {
        val archive = sample().let { source ->
            source.copy(visual = source.visual.copy(
                profiles = source.visual.profiles.map { it.copy(mobileTypeWeight = 150) },
            ))
        }
        assertEquals(archive, ConfigBackupFormat.decode(ConfigBackupFormat.encode(archive)))
    }

    @Test fun batteryWeight150RoundTripsInEveryProfile() {
        val archive = sample().let { source ->
            source.copy(visual = source.visual.copy(
                profiles = source.visual.profiles.map { it.copy(topTextWeight = 150) },
            ))
        }
        assertEquals(archive, ConfigBackupFormat.decode(ConfigBackupFormat.encode(archive)))
    }

    @Test fun legacySchemaOneWithoutWifiWeightRetainsNativeDefault() {
        val source = ConfigBackupFormat.encode(sample())
        val withoutWeight = source.replace("\"wifiWeight\":100,", "")
        assertEquals(6, ConfigBackupFormat.decode(withoutWeight).visual.profiles.size)
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
        for (unsafeName in listOf("Alpha\u2028Beta", "Alpha\u2029Beta", "Alpha\u202EBeta", "Alpha\u200FBeta", "Alpha\u061CBeta")) {
            assertThrows(IllegalArgumentException::class.java) {
                ConfigBackupFormat.validate(sample().copy(
                    batterySchemes = BatteryColorSchemeLibrary(
                        customSchemes = listOf(custom.copy(name = unsafeName)),
                    ),
                ))
            }
        }
    }

    @Test fun importOwnsOnlyFeatureVisualAndSchemeKeys() {
        assertTrue(isBackupVisualKey(
            visualProfileKey(ContentLayout.NETWORK_CENTER, WIFI_WEIGHT_KEY),
        ))
        assertTrue(isBackupVisualKey(visualProfileKey(ContentLayout.MOBILE_CENTER, MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY)))
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

    private val wire = Json { encodeDefaults = true }
    private fun raw(backup: ConfigBackup): String = wire.encodeToString(backup)

    @Test
    fun releasedV1TwoLayoutBackupAddsThirdDefaultsAndPreservesEveryExistingField() {
        val full = sample()
        val original = full.copy(visual = full.visual.copy(
            profiles = full.visual.profiles.filter { it.layout != ContentLayout.MOBILE_CENTER },
        ))
        val imported = ConfigBackupFormat.decode(raw(original))
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
        val original = sample()
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
        val imported = ConfigBackupFormat.decode(raw(damaged))
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
        val canonical = ConfigBackupFormat.encode(sample())
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
        val source = sample()
        for (invalid in listOf(
            source.copy(visual = source.visual.copy(profiles = source.visual.profiles.dropLast(1))),
            source.copy(visual = source.visual.copy(profiles =
                source.visual.profiles.dropLast(1) + source.visual.profiles.first())),
            source.copy(visual = source.visual.copy(profiles = source.visual.profiles.mapIndexed { i, cfg ->
                if (i == 1) cfg.copy(fillFollowsRetract = !cfg.fillFollowsRetract) else cfg
            })),
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                ConfigBackupFormat.decode(raw(invalid))
            }
        }
    }

    @Test
    fun independentStyleAndLabelOffsetsRoundTripInAllSixProfiles() {
        val source = sample()
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
        val source = sample()
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
        val source = sample()
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
        val normal = sample()
        val badVisual = normal.copy(visual = normal.visual.copy(profiles =
            normal.visual.profiles.mapIndexed { i, v ->
                if (i == 0) v.copy(batteryColorOverrides =
                    v.batteryColorOverrides.copy(charging = 0x00112233)) else v
            },
        ))
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(raw(badVisual))
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
        val third = ConfigBackupFormat.encode(sample())
            .replace(",\"mobileTypeBottomOffsetY\":-3.0", "")
        val imported = ConfigBackupFormat.decode(third)
        assertTrue(imported.visual.profiles.filter { it.layout == ContentLayout.MOBILE_CENTER }
            .all { it.mobileTypeBottomOffsetY == -3f })
    }

}
