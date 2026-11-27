package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualBackupPrefsTest {
    @Test
    fun importedVisualKeysPreserveUnrelatedPrefsAndRollbackLosslessly() {
        val prefs = VisualTestPreferences()
        val schemeKey = "battery_color_scheme_library.custom.1.name"
        prefs.edit()
            .putString("application_ui_scale", "90")
            .putString("preview_mode", "normal")
            .putString("root_permission", "granted")
            .putFloat(visualProfileKey(ContentLayout.NETWORK_CENTER, COMBINED_SCALE_KEY), 0.85f)
            .putString(schemeKey, "Previous scheme")
            .apply()
        val before = prefs.all.toMap()
        val snapshot = prefs.backupVisualSnapshot()

        val editor = prefs.editBackupVisuals()
        editor.putVisualCfg(VisualCfg(combinedScale = 0.95f))
        editor.putString(schemeKey, "Imported scheme")
        assertTrue(editor.commit())

        assertEquals("90", prefs.getString("application_ui_scale", null))
        assertEquals("normal", prefs.getString("preview_mode", null))
        assertEquals("granted", prefs.getString("root_permission", null))
        assertEquals("Imported scheme", prefs.getString(schemeKey, null))
        assertEquals(0.95f, prefs.readVisualCfg(dual = false).combinedScale, 0.0001f)

        assertTrue(prefs.rollbackBackupVisuals(snapshot))
        assertEquals(before, prefs.all)
    }

    @Test
    fun featureRollbackPreservesOriginalKeysTimestampAndUnrelatedPreferences() {
        val prefs = VisualTestPreferences()
        prefs.edit()
            .putBoolean(FEATURE_ENABLED_KEY, false)
            .putLong(FEATURE_CHANGED_AT_NS_KEY, 42L)
            .putString("settings_ui_scale", "90")
            .apply()
        val original = prefs.all.toMap()
        val snapshot = prefs.backupFeatureSnapshot()

        // A feature commit can update memory even if its disk write later fails.
        prefs.edit()
            .putBoolean(FEATURE_ENABLED_KEY, true)
            .putBoolean(FEATURE_KEYGUARD_KEY, false)
            .putBoolean(FEATURE_AOD_KEY, false)
            .putLong(FEATURE_CHANGED_AT_NS_KEY, 99L)
            .apply()

        assertTrue(prefs.rollbackBackupFeatures(snapshot))
        assertEquals(original, prefs.all)
        assertFalse(prefs.contains(FEATURE_KEYGUARD_KEY))
        assertFalse(prefs.contains(FEATURE_AOD_KEY))
    }

    @Test
    fun featureRollbackDoesNotCreateKeysThatWereOriginallyAbsent() {
        val prefs = VisualTestPreferences()
        val snapshot = prefs.backupFeatureSnapshot()
        prefs.edit()
            .putBoolean(FEATURE_ENABLED_KEY, true)
            .putLong(FEATURE_CHANGED_AT_NS_KEY, 99L)
            .apply()

        assertTrue(prefs.rollbackBackupFeatures(snapshot))
        assertTrue(prefs.all.isEmpty())
    }

    @Test
    fun backupRestorationUsesArchivedActiveColorsForBothModes() {
        val fixture = sampleBackup()
        val cases = archivedVisualCases(fixture.visual)
        val profiles = fixture.visual.profiles.map { profile ->
            val expected = cases.getValue(profile.dualSignal).first
            profile.copy(
                combinedScale = expected.combinedScale - profile.layout.ordinal * 0.02f,
                batteryColorPreset = expected.batteryColorPreset,
                fillFollowsRetract = expected.fillFollowsRetract,
                ccTintTransition = expected.ccTintTransition,
            )
        }
        for ((dual, case) in cases) {
            val (expected, schemeKey) = case
            val prefs = VisualTestPreferences()
            prefs.edit().putString("unrelated", "keep").apply()
            val original = prefs.all.toMap()
            val snapshot = prefs.backupVisualSnapshot()
            val writes = prefs.writeCount
            val backup = fixture.visual.copy(dualEnabled = dual, profiles = profiles)
            assertTrue(prefs.restoreVisualBackup(backup, fixture.batterySchemes))
            assertEquals(writes + 1, prefs.writeCount)
            assertEquals(expected.layout, prefs.readLayout())
            assertEquals(dual, prefs.dualSignalEnabled())
            assertEquals("keep", prefs.getString("unrelated", null))
            val expectedLayouts = profiles.filter { it.dualSignal == dual }.associateBy { it.layout }
            for (profile in prefs.readVisualBackup().profiles) {
                val archived = expectedLayouts.getValue(profile.layout)
                assertEquals(archived.normalized().copy(dualSignal = profile.dualSignal), profile)
            }
            assertEquals(schemeKey, BatterySchemeRepo(prefs).current().activeSchemeKey)
            assertTrue(prefs.rollbackBackupVisuals(snapshot))
            assertEquals(original, prefs.all)
        }
    }

    @Test
    fun applyingAndEditingSharedSchemeNeverSplitsBatteryColors() {
        val prefs = VisualTestPreferences()
        val schemes = BatterySchemeRepo(prefs)

        schemes.activateScheme(BATTERY_COLOR_SCHEME_IOS_KEY)
        for (dual in listOf(false, true)) {
            assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = dual).batteryColorPreset)
        }

        val id = requireNotNull(schemes.createCustom("Shared scheme", BATTERY_COLOR_SCHEME_IOS_KEY))
        val customKey = customSchemeKey(id)
        schemes.activateScheme(customKey)
        schemes.setCustomColor(id, BatteryColorSlot.CHARGING, 0xFF123456.toInt())
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        for (dual in listOf(false, true)) {
            val visual = prefs.readVisualCfg(dual = dual)
            assertEquals(0xFF123456.toInt(), visual.batteryColorOverrides.charging)
            assertEquals(BatteryColorMode.CUSTOM, visual.batteryColorModes.charging)
        }
        assertEquals(customKey, schemes.current().activeSchemeKey)
        assertEquals(1, schemes.current().customSchemes.size)

        schemes.deleteCustom(id)
        for (dual in listOf(false, true)) {
            val visual = prefs.readVisualCfg(dual = dual)
            assertEquals(BatteryColorPreset.IOS_STYLE, visual.batteryColorPreset)
            assertEquals(BatteryColorMode.PRESET, visual.batteryColorModes.charging)
        }
        assertEquals(BATTERY_COLOR_SCHEME_IOS_KEY, schemes.current().activeSchemeKey)
        assertTrue(schemes.current().customSchemes.isEmpty())
    }

    @Test
    fun matchingSchemeUsesVisiblePaletteNotTheOtherSimSelection() {
        val schemes = BatteryColorSchemeLibrary()
        val oldDual = VisualCfg(
            dualSignal = true,
            batteryColorPreset = BatteryColorPreset.IOS_STYLE,
        )
        assertEquals(BATTERY_COLOR_SCHEME_IOS_KEY, schemes.matchingKey(oldDual))

        val unmatched = oldDual.copy(
            batteryColorModes = BatteryColorModes(charging = BatteryColorMode.CUSTOM),
            batteryColorOverrides = BatteryColorOverrides(charging = 0xFF2468AC.toInt()),
        )
        assertEquals(null, schemes.matchingKey(unmatched))
    }
}

/** Legacy archives may keep different values in each SIM bank. */
private fun archivedVisualCases(visual: VisualBackup) = mapOf(
    false to (VisualCfg(
        layout = visual.singleLayout,
        dualSignal = false,
        combinedScale = 0.85f,
        batteryColorPreset = BatteryColorPreset.HYPEROS,
    ) to BATTERY_COLOR_SCHEME_HYPEROS_KEY),
    true to (VisualCfg(
        layout = visual.dualLayout,
        dualSignal = true,
        combinedScale = 0.95f,
        batteryColorPreset = BatteryColorPreset.IOS_STYLE,
        fillFollowsRetract = false,
        ccTintTransition = false,
    ) to BATTERY_COLOR_SCHEME_IOS_KEY),
)
