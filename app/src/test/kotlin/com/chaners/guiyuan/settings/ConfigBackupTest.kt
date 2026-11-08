package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class ConfigBackupTest {
    @Test
    fun rejectsTwoSidedArrangementInSingleRowBackup() {
        val valid = ConfigBackupFormat.encode(sampleBackup())
        val invalid = valid.replaceFirst(
            "\"singleRowDotStyle\":\"DEFAULT\"",
            "\"singleRowDotStyle\":\"LEFT_RIGHT\"",
        )
        assertTrue(invalid != valid)
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(invalid)
        }
    }

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

    @Test
    fun backupValidationRejectsNonfiniteBottomIndicatorFields() {
        val invalidProfiles = listOf<(VisualCfg) -> VisualCfg>(
            { it.copy(bothSidesActivityScale = Float.NaN) },
            { it.copy(bothSidesActivityOffsetY = Float.POSITIVE_INFINITY) },
            { it.copy(bothSidesChargingOffsetY = Float.NEGATIVE_INFINITY) },
        )
        for (makeInvalid in invalidProfiles) {
            val original = sampleBackup()
            val invalid = original.copy(
                visual = original.visual.copy(
                    profiles = original.visual.profiles.map(makeInvalid),
                ),
            )
            assertThrows(IllegalArgumentException::class.java) {
                ConfigBackupFormat.validate(invalid)
            }
        }
    }



    @Test fun chargingMasterKeepsExistingBackupFieldName() {
        val original = sampleBackup().let { backup ->
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
        val original = sampleBackup().let { source ->
            source.copy(visual = source.visual.copy(profiles = source.visual.profiles.map { cfg ->
                if (cfg.dualSignal && cfg.layout == ContentLayout.NETWORK_CENTER) selected else cfg
            }))
        }
        assertEquals(original, ConfigBackupFormat.decode(ConfigBackupFormat.encode(original)))
    }

    @Test fun retiredIdleShowBackupDoesNotBecomeHide() {
        val old = ConfigBackupFormat.encode(sampleBackup())
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
        val original = sampleBackup().let { source ->
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
        val source = sampleBackup().let { backup ->
            backup.copy(visual = backup.visual.copy(profiles = backup.visual.profiles.map { cfg ->
                if (cfg.dualSignal && cfg.layout == ContentLayout.NETWORK_CENTER) selected else cfg
            }))
        }
        assertEquals(source, ConfigBackupFormat.decode(ConfigBackupFormat.encode(source)))
    }
}
