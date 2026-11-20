package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private const val RELEASED_SINGLE_ROW_DUO = "\"singleRowDotStyle\":\"DUO\""

class ConfigBackupLegacyTest {
    private fun horizontalDotsBackup(): ConfigBackup =
        sampleBackup().let { backup ->
            backup.copy(visual = backup.visual.copy(profiles = backup.visual.profiles.map { profile ->
                profile.copy(
                    dualRowDotArrangement = SignalDotArrangement.HORIZONTAL,
                    singleRowDotArrangement = SignalDotArrangement.HORIZONTAL,
                )
            }))
        }

    @Test fun rejectsConflictingModeWideValues() {
        val source = sampleBackup()
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
        val source = sampleBackup()
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
        val original = sampleBackup().let { backup ->
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
        assertTrue(encoded.contains(RELEASED_SINGLE_ROW_DUO))
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
        val original = horizontalDotsBackup()
        val encoded = ConfigBackupFormat.encode(original)
        val releasedStyle = RELEASED_SINGLE_ROW_DUO
        val oldStyle = "\"singleRowFlatDots\":true"
        val old = encoded.replace(releasedStyle, oldStyle)
        assertEquals(original, ConfigBackupFormat.decode(old))
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(old.replaceFirst(oldStyle, "$oldStyle,$releasedStyle"))
        }
        val curved = old.replace(oldStyle, "\"singleRowFlatDots\":false")
        assertEquals(SignalDotArrangement.CURVED, ConfigBackupFormat.decode(curved).visual.profiles.first().singleRowDotArrangement)
        val invalid = old.replaceFirst(oldStyle, "\"singleRowFlatDots\":\"true\"")
        assertThrows(IllegalArgumentException::class.java) { ConfigBackupFormat.decode(invalid) }
    }

    @Test fun combinedLegacyFieldsKeepMigrationOrderAndModeNormalization() {
        val source = horizontalDotsBackup().let { backup ->
            backup.copy(visual = backup.visual.copy(dualEnabled = false))
        }
        val old = ConfigBackupFormat.encode(source)
            .replace(RELEASED_SINGLE_ROW_DUO, "\"singleRowFlatDots\":true")
            .replaceFirst("\"dualDotStyle\":\"DUO\",", "")
            .replaceFirst("\"leftRightHideIdleActivity\":false", "\"leftRightShowIdleActivity\":true")
        val restored = ConfigBackupFormat.decode(old)
        assertEquals(SignalDotArrangement.CURVED, restored.visual.profiles.first().dualRowDotArrangement)
        assertTrue(restored.visual.profiles.drop(1).all {
            it.dualRowDotArrangement == SignalDotArrangement.HORIZONTAL
        })
        // The active single-row mode is propagated to its corresponding layout profiles.
        assertTrue(restored.visual.profiles.filter {
            it.layout == ContentLayout.NETWORK_CENTER
        }.all { it.singleRowDotArrangement == SignalDotArrangement.CURVED })
        assertTrue(restored.visual.profiles.filter {
            it.layout != ContentLayout.NETWORK_CENTER
        }.all { it.singleRowDotArrangement == SignalDotArrangement.HORIZONTAL })
        assertFalse(restored.visual.profiles.first().bothSidesHideIdleActivity)
    }

    @Test fun legacyArchivesDefaultToOriginalDotStyle() {
        val json = ConfigBackupFormat.encode(sampleBackup())
            .replace("\"dualDotStyle\":\"LEFT_RIGHT\",", "")
        assertTrue(ConfigBackupFormat.decode(json).visual.profiles.all {
            it.dualRowDotArrangement == SignalDotArrangement.CURVED
        })
    }

    @Test fun offsetValuesSurviveBackupsAndRejectInvalidRanges() {
        val original = sampleBackup()
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
}
