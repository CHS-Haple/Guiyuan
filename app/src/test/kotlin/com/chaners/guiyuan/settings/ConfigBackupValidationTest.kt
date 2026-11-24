package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigBackupValidationTest {
    @Test fun unknownFutureSchemaIsNotGuessed() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(sampleBackup().copy(schemaVersion = 2))
        }
    }

    @Test fun unknownEnumAndOversizedFileAreRejected() {
        val json = ConfigBackupFormat.encode(sampleBackup())
        assertThrows(Exception::class.java) {
            ConfigBackupFormat.decode(json.replace("NETWORK_CENTER", "UNKNOWN_LAYOUT"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.decode(" ".repeat(ConfigBackupFormat.MAX_BYTES + 1))
        }
    }


    @Test
    fun escapedQuoteInValidArchiveStringSurvivesStructureScan() {
        val backup = sampleBackup().copy(appVersion = "1.2.3 \"stable\"")
        assertEquals(backup, ConfigBackupFormat.decode(ConfigBackupFormat.encode(backup)))
    }

    @Test fun duplicateAndEscapedJsonKeysAreRejected() {
        val json = ConfigBackupFormat.encode(sampleBackup())
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
        val json = ConfigBackupFormat.encode(sampleBackup())
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
            ConfigBackupFormat.validate(sampleBackup().copy(
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
            ConfigBackupFormat.validate(sampleBackup().copy(
                batterySchemes = BatteryColorSchemeLibrary(customSchemes = listOf(custom)),
            ))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConfigBackupFormat.validate(sampleBackup().copy(appVersion = "0.7\u202Eexe"))
        }
        for (unsafeName in listOf("Alpha\u2028Beta", "Alpha\u2029Beta", "Alpha\u202EBeta", "Alpha\u200FBeta", "Alpha\u061CBeta")) {
            assertThrows(IllegalArgumentException::class.java) {
                ConfigBackupFormat.validate(sampleBackup().copy(
                    batterySchemes = BatteryColorSchemeLibrary(
                        customSchemes = listOf(custom.copy(name = unsafeName)),
                    ),
                ))
            }
        }
    }

    @Test
    fun archiveTextUsesSamePortableControlRulesAndPreservesValidEmoji() {
        val valid = sampleBackup().copy(appVersion = "2.0 \uD83D\uDE80")
        ConfigBackupFormat.validate(valid)
        for (unsafe in listOf("\u2066", "\u206F", "\u200E", "\uD800")) {
            assertThrows(IllegalArgumentException::class.java) {
                ConfigBackupFormat.validate(
                    sampleBackup().copy(appVersion = "2.0" + unsafe + "spoof"),
                )
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
        val json = ConfigBackupFormat.encode(sampleBackup())
        assertTrue(!json.contains("uiScalePercent"))
        assertTrue(!json.contains("navStyle"))
        assertTrue(!json.contains("preview"))
    }
}
