package com.chaners.guiyuan.settings

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualPersistenceTest {

    @Test
    fun resettingVisualDefaultsPreservesSavedBatterySchemes() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putString(CONTENT_LAYOUT_KEY, ContentLayout.BATTERY_CENTER.persistedValue)
            .putFloat(WIFI_SIZE_SCALE_KEY, 1.4f)
            .apply()
        val custom = BatteryCustomColorScheme(
            id = 2,
            name = "Saved colors",
            baseTemplate = BatteryBuiltInColorScheme.IOS,
            entries = entriesFromBuiltIn(BatteryBuiltInColorScheme.IOS).withEntry(
                BatteryColorSlot.CHARGING,
                BatteryColorSchemeEntry(
                    source = BatteryColorSchemeSource.CUSTOM,
                    customColor = 0xFF2468AC.toInt(),
                ),
            ),
        )
        val second = BatteryCustomColorScheme(
            id = 4,
            name = "Another scheme",
            baseTemplate = BatteryBuiltInColorScheme.LOW_SATURATION,
            entries = entriesFromBuiltIn(BatteryBuiltInColorScheme.LOW_SATURATION),
        )
        val library = BatteryColorSchemeLibrary(
            activeSchemeKey = custom.key,
            customSchemes = listOf(custom, second),
            dualActiveSchemeKey = custom.key,
        )

        resetVisualPreferences(prefs, library)

        assertFalse(prefs.contains(DUAL_SIGNAL_ENABLED_KEY))
        assertFalse(prefs.contains(CONTENT_LAYOUT_KEY))
        assertFalse(prefs.contains(WIFI_SIZE_SCALE_KEY))
        assertEquals(ContentLayout.NETWORK_CENTER, prefs.readLayout())
        assertEquals(
            BATTERY_COLOR_SCHEME_HYPEROS_KEY,
            prefs.getString("battery_color_scheme_library.active", null),
        )
        assertEquals(
            BATTERY_COLOR_SCHEME_HYPEROS_KEY,
            prefs.getString("battery_color_scheme_library.dual_active", null),
        )
        assertEquals("2,4", prefs.getString("battery_color_scheme_library.custom_order", null))
        assertEquals("Saved colors", prefs.getString("battery_color_scheme_library.custom.2.name", null))
        assertEquals(
            BatteryBuiltInColorScheme.IOS.key,
            prefs.getString("battery_color_scheme_library.custom.2.base", null),
        )
        assertEquals(
            BatteryColorSchemeSource.CUSTOM.persistedValue,
            prefs.getString("battery_color_scheme_library.custom.2.source.charging", null),
        )
        assertEquals(
            0xFF2468AC.toInt(),
            prefs.getInt("battery_color_scheme_library.custom.2.color.charging", 0),
        )
        assertEquals("Another scheme", prefs.getString("battery_color_scheme_library.custom.4.name", null))
        assertEquals(BatteryColorPreset.HYPEROS, prefs.readVisualCfg().batteryColorPreset)
        assertEquals(1, prefs.getInt("battery_color_scheme_library.schema", 0))
    }

    @Test
    fun firstRuntimeSnapshotUsesMigratedVisualPreferences() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.BATTERY_CENTER
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putString(visualModeKey(true, CONTENT_LAYOUT_KEY), layout.persistedValue)
            .putBoolean(visualModeKey(true, BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY), false)
            .putBoolean(visualModeKey(true, CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY), false)
            .putInt(visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY, dual = true), 1400)
            .putString("other_setting", "untouched")
            .apply()

        // The first runtime snapshot must not observe legacy transition defaults.
        assertTrue(prefs.readVisualCfg().fillFollowsRetract)
        migrateVisualPrefsIfNeeded(prefs)
        val migrated = prefs.readVisualCfg()
        assertEquals(layout, migrated.layout)
        assertFalse(migrated.fillFollowsRetract)
        assertFalse(migrated.ccTintTransition)
        assertEquals(1000, migrated.mobileTypeWeight)
        assertEquals(BatteryColorPreset.HYPEROS.persistedValue, prefs.getString(BATTERY_COLOR_PRESET_KEY, null))
        assertEquals("untouched", prefs.getString("other_setting", null))

        val once = prefs.all.toMap()
        migrateVisualPrefsIfNeeded(prefs)
        assertEquals(once, prefs.all)
    }

    @Test
    fun displayTransitionsUseActiveModeOnMigrationAndRemainShared() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, true)
            .putBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, false)
            .putBoolean(visualModeKey(true, BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY), false)
            .putBoolean(visualModeKey(true, CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY), true)
            .apply()
        migrateSharedTransitionsIfNeeded(prefs)
        assertFalse(prefs.contains(visualModeKey(true, BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY)))
        assertFalse(prefs.contains(visualModeKey(true, CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY)))
        assertEquals(1, prefs.getInt(SHARED_TRANSITIONS_SCHEMA_KEY, 0))
        for (dual in listOf(false, true)) {
            for (layout in ContentLayout.entries) {
                val cfg = prefs.readVisualCfg(dual = dual, layoutOverride = layout)
                assertFalse(cfg.fillFollowsRetract)
                assertTrue(cfg.ccTintTransition)
            }
        }
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        migrateSharedTransitionsIfNeeded(prefs)
        assertFalse(prefs.readVisualCfg().fillFollowsRetract)
        prefs.edit().putVisualCfg(VisualCfg(
            dualSignal = true,
            fillFollowsRetract = true,
            ccTintTransition = false,
        )).apply()
        assertTrue(prefs.readVisualCfg(dual = false).fillFollowsRetract)
        assertFalse(prefs.readVisualCfg(dual = false).ccTintTransition)
    }

    @Test
    fun displayTransitionMigrationPreservesLegacySingleMode() {
        val prefs = MemoryPreferences()
        prefs.edit().putBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, false)
            .putBoolean(visualModeKey(true, BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY), true)
            .apply()
        migrateSharedTransitionsIfNeeded(prefs)
        assertFalse(prefs.readVisualCfg(dual = false).fillFollowsRetract)
        assertFalse(prefs.readVisualCfg(dual = true).fillFollowsRetract)
    }
    @Test
    fun duoStyleIsModeWideAndSurvivesLayoutSwitches() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putString(visualModeKey(true, DUAL_DOT_STYLE_KEY), DualDotStyle.DUO.persistedValue)
            .apply()
        for (layout in ContentLayout.entries) {
            assertEquals(DualDotStyle.DUO, prefs.readVisualCfg(dual = true, layoutOverride = layout).dualDotStyle)
            assertEquals(DualDotStyle.DEFAULT, prefs.readVisualCfg(dual = false, layoutOverride = layout).dualDotStyle)
        }
        prefs.edit()
            .putString(visualModeKey(true, DUAL_DOT_STYLE_KEY), "invalid")
            .apply()
        assertEquals(DualDotStyle.DEFAULT, prefs.readVisualCfg(dual = true).dualDotStyle)
    }

    @Test
    fun dualOffsetsStayIndependentAcrossStylesAndLayouts() {
        val prefs = MemoryPreferences()
        val dual = VisualCfg(
            dualSignal = true,
            dualDefaultOffsetY = 4f,
            dualDuoOffsetY = -7f,
        )
        prefs.edit().putVisualCfg(dual).apply()
        val network = prefs.readVisualCfg(dual = true, layoutOverride = ContentLayout.NETWORK_CENTER)
        assertEquals(4f, network.dualMobileOffsetY, 0f)
        assertEquals(-7f, network.copy(dualDotStyle = DualDotStyle.DUO).dualMobileOffsetY, 0f)

        prefs.edit().putVisualCfg(
            dual.copy(
                layout = ContentLayout.BATTERY_CENTER,
                dualDotStyle = DualDotStyle.DUO,
                dualDefaultOffsetY = -2f,
                dualDuoOffsetY = 5f,
            ),
        ).apply()
        val battery = prefs.readVisualCfg(dual = true, layoutOverride = ContentLayout.BATTERY_CENTER)
        assertEquals(5f, battery.dualMobileOffsetY, 0f)
        assertEquals(-2f, battery.copy(dualDotStyle = DualDotStyle.DEFAULT).dualMobileOffsetY, 0f)
        val original = prefs.readVisualCfg(dual = true, layoutOverride = ContentLayout.NETWORK_CENTER)
        assertEquals(4f, original.dualDefaultOffsetY, 0f)
        assertEquals(-7f, original.dualDuoOffsetY, 0f)
        assertEquals(0f, prefs.readVisualCfg(dual = false).dualMobileOffsetY, 0f)
    }

    @Test
    fun batteryReadoutDefaultsOnOnlyForUnconfiguredBatteryCenteredProfiles() {
        val prefs = MemoryPreferences()
        for (dual in listOf(false, true)) {
            assertFalse(prefs.readVisualCfg(dual = dual, layoutOverride = ContentLayout.NETWORK_CENTER).showTopReadout)
            assertTrue(prefs.readVisualCfg(dual = dual, layoutOverride = ContentLayout.BATTERY_CENTER).showTopReadout)
        }
        prefs.edit()
            .putBoolean(visualProfileKey(ContentLayout.BATTERY_CENTER, BATTERY_TOP_READOUT_ENABLED_KEY), false)
            .putBoolean(visualProfileKey(ContentLayout.BATTERY_CENTER, BATTERY_TOP_READOUT_ENABLED_KEY, dual = true), false)
            .apply()
        assertFalse(prefs.readVisualCfg(dual = false, layoutOverride = ContentLayout.BATTERY_CENTER).showTopReadout)
        assertFalse(prefs.readVisualCfg(dual = true, layoutOverride = ContentLayout.BATTERY_CENTER).showTopReadout)
    }

    @Test
    fun legacyDualLayoutMigratesToSingleSourceWithoutOverridingLaterChanges() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putString(CONTENT_LAYOUT_KEY, ContentLayout.NETWORK_CENTER.persistedValue)
            .putString(visualModeKey(true, CONTENT_LAYOUT_KEY), ContentLayout.BATTERY_CENTER.persistedValue)
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .apply()
        migrateSharedLayoutIfNeeded(prefs)
        assertEquals(ContentLayout.BATTERY_CENTER, prefs.readLayout())
        assertTrue(prefs.getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false))
        assertFalse(prefs.contains(visualModeKey(true, CONTENT_LAYOUT_KEY)))
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, false)
            .putString(CONTENT_LAYOUT_KEY, ContentLayout.NETWORK_CENTER.persistedValue)
            .apply()
        migrateSharedLayoutIfNeeded(prefs)
        assertEquals(ContentLayout.NETWORK_CENTER, prefs.readLayout())

        val previouslySingle = MemoryPreferences()
        previouslySingle.edit()
            .putString(CONTENT_LAYOUT_KEY, ContentLayout.NETWORK_CENTER.persistedValue)
            .putString(visualModeKey(true, CONTENT_LAYOUT_KEY), ContentLayout.BATTERY_CENTER.persistedValue)
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, false)
            .apply()
        migrateSharedLayoutIfNeeded(previouslySingle)
        assertEquals(ContentLayout.NETWORK_CENTER, previouslySingle.readLayout())
    }

    @Test
    fun singleAndDualKeepIndependentVisualProfilesAndShareCurrentLayout() {
        val prefs = MemoryPreferences()
        val single = VisualCfg(
            layout = ContentLayout.BATTERY_CENTER,
            combinedScale = 0.9f,
            outerWeightScale = 1.1f,
            wifiOffsetY = -3f,
            wifiWeight = 60,
            airplaneWeight = 60,
            mobileTypeOffsetY = 6f,
            batteryColorPreset = BatteryColorPreset.RECOMMENDED,
            batteryColorModes = BatteryColorModes(charging = BatteryColorMode.CUSTOM),
            batteryColorOverrides = BatteryColorOverrides(charging = 0xFF123456.toInt()),
        )
        val dual = VisualCfg(
            layout = ContentLayout.BATTERY_CENTER,
            dualSignal = true,
            combinedScale = 1.2f,
            outerWeightScale = 0.8f,
            wifiOffsetY = 4f,
            wifiWeight = 130,
            airplaneWeight = 130,
            mobileTypeOffsetY = -2f,
            batteryColorPreset = BatteryColorPreset.IOS_STYLE,
            batteryColorModes = BatteryColorModes(powerSave = BatteryColorMode.FOLLOW_SYSTEM),
        )
        prefs.edit().putVisualCfg(single).apply()
        prefs.edit().putVisualCfg(dual).apply()

        assertEquals(dual.normalized(), prefs.readVisualCfg())
        assertEquals(single.normalized(), prefs.readVisualCfg(dual = false))
        assertEquals(ContentLayout.BATTERY_CENTER, prefs.readLayout())
        assertEquals(
            single.batteryColorOverrides.charging,
            prefs.readVisualCfg(dual = false).batteryColorOverrides.charging,
        )
        assertEquals(
            BatteryColorMode.FOLLOW_SYSTEM,
            prefs.readVisualCfg(dual = true).batteryColorModes.powerSave,
        )

        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        assertEquals(single.normalized(), prefs.readVisualCfg())
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        assertEquals(dual.normalized(), prefs.readVisualCfg())
    }

    @Test
    fun savedNativeWeightsMigrateAllBanksWithoutTouchingOtherKeys() {
        val prefs = MemoryPreferences()
        val network = ContentLayout.NETWORK_CENTER
        val battery = ContentLayout.BATTERY_CENTER
        prefs.edit()
            .putInt(MOBILE_TYPE_WEIGHT_KEY, 1400)
            .putInt(visualProfileKey(network, MOBILE_TYPE_WEIGHT_KEY), 1200)
            .putInt(visualProfileKey(battery, MOBILE_TYPE_WEIGHT_KEY), 200)
            .putInt(visualProfileKey(network, MOBILE_TYPE_WEIGHT_KEY, dual = true), 1001)
            .putInt(visualProfileKey(battery, MOBILE_TYPE_WEIGHT_KEY, dual = true), 1000)
            .putInt(BATTERY_TOP_TEXT_WEIGHT_KEY, 1400)
            .putInt(visualProfileKey(network, BATTERY_TOP_TEXT_WEIGHT_KEY), 1100)
            .putInt(visualProfileKey(battery, BATTERY_TOP_TEXT_WEIGHT_KEY), 200)
            .putInt(visualProfileKey(network, BATTERY_TOP_TEXT_WEIGHT_KEY, dual = true), 1001)
            .putInt(visualProfileKey(battery, BATTERY_TOP_TEXT_WEIGHT_KEY, dual = true), 1000)
            .putInt(visualProfileKey(network, WIFI_WEIGHT_KEY), 80)
            .putString("application_ui_scale", "90")
            .apply()

        migrateTextWeightRanges(prefs)

        assertEquals(1000, prefs.getInt(MOBILE_TYPE_WEIGHT_KEY, 0))
        assertEquals(1000, prefs.readVisualCfg(dual = false, layoutOverride = network).mobileTypeWeight)
        assertEquals(200, prefs.readVisualCfg(dual = false, layoutOverride = battery).mobileTypeWeight)
        assertEquals(1000, prefs.readVisualCfg(dual = true, layoutOverride = network).mobileTypeWeight)
        assertEquals(1000, prefs.readVisualCfg(dual = true, layoutOverride = battery).mobileTypeWeight)
        assertEquals(1000, prefs.getInt(BATTERY_TOP_TEXT_WEIGHT_KEY, 0))
        assertEquals(1000, prefs.readVisualCfg(dual = false, layoutOverride = network).topTextWeight)
        assertEquals(200, prefs.readVisualCfg(dual = false, layoutOverride = battery).topTextWeight)
        assertEquals(1000, prefs.readVisualCfg(dual = true, layoutOverride = network).topTextWeight)
        assertEquals(1000, prefs.readVisualCfg(dual = true, layoutOverride = battery).topTextWeight)
        migrateTextWeightRanges(prefs)
        assertEquals(1000, prefs.getInt(BATTERY_TOP_TEXT_WEIGHT_KEY, 0))
        assertEquals(80, prefs.getInt(visualProfileKey(network, WIFI_WEIGHT_KEY), 0))
        assertEquals("90", prefs.getString("application_ui_scale", null))
    }

    @Test
    fun corruptedSavedThicknessIsBoundedBeforeItReachesTheRenderer() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.NETWORK_CENTER
        prefs.edit()
            .putInt(visualProfileKey(layout, BATTERY_TOP_TEXT_WEIGHT_KEY), -1)
            .putInt(visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY), 1401)
            .putInt(visualProfileKey(layout, BATTERY_TOP_TEXT_WEIGHT_KEY, dual = true), 1401)
            .putInt(visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY, dual = true), -1)
            .apply()

        assertEquals(1, prefs.readVisualCfg(dual = false, layoutOverride = layout).topTextWeight)
        assertEquals(1000, prefs.readVisualCfg(dual = false, layoutOverride = layout).mobileTypeWeight)
        assertEquals(1000, prefs.readVisualCfg(dual = true, layoutOverride = layout).topTextWeight)
        assertEquals(1, prefs.readVisualCfg(dual = true, layoutOverride = layout).mobileTypeWeight)

        migrateTextWeightRanges(prefs)
        assertEquals(1, prefs.getInt(visualProfileKey(layout, BATTERY_TOP_TEXT_WEIGHT_KEY), -1))
        assertEquals(1000, prefs.getInt(visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY), -1))
        assertEquals(1000, prefs.getInt(visualProfileKey(layout, BATTERY_TOP_TEXT_WEIGHT_KEY, dual = true), -1))
        assertEquals(1, prefs.getInt(visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY, dual = true), -1))
    }

    @Test
    fun remoteMirrorPreservesBothBanksWhenSwitchingModes() {
        val local = MemoryPreferences()
        val remote = MemoryPreferences()
        val single = VisualCfg(combinedScale = 0.8f, batteryColorPreset = BatteryColorPreset.RECOMMENDED)
        val dual = VisualCfg(
            dualSignal = true,
            combinedScale = 1.2f,
            batteryColorPreset = BatteryColorPreset.IOS_STYLE,
        )
        local.edit().putVisualCfg(single).apply()
        local.edit().putVisualCfg(dual).apply()
        remote.edit().putVisualCfg(local.readVisualCfg()).apply()
        assertEquals(dual.normalized(), remote.readVisualCfg())

        local.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        remote.edit().putVisualCfg(local.readVisualCfg()).apply()
        assertEquals(single.normalized(), remote.readVisualCfg())
        assertEquals(dual.normalized(), remote.readVisualCfg(dual = true))

        local.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        remote.edit().putVisualCfg(local.readVisualCfg()).apply()
        assertEquals(dual.normalized(), remote.readVisualCfg())
        assertEquals(single.normalized(), remote.readVisualCfg(dual = false))
    }

    @Test
    fun missingDualKeysCannotBorrowLegacySingleValues() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putFloat(COMBINED_SCALE_KEY, 0.75f)
            .putFloat(WIFI_VERTICAL_OFFSET_KEY, -3f)
            .putString(BATTERY_COLOR_PRESET_KEY, BatteryColorPreset.IOS_STYLE.persistedValue)
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .apply()
        assertEquals(COMBINED_SCALE_DEFAULT, prefs.readVisualCfg().combinedScale, 0.0001f)
        assertEquals(0f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(-3f, prefs.readVisualCfg(dual = false).wifiOffsetY, 0f)
        assertEquals(BatteryColorPreset.HYPEROS, prefs.readVisualCfg().batteryColorPreset)
        assertEquals(0.75f, prefs.readVisualCfg(dual = false).combinedScale, 0.0001f)
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = false).batteryColorPreset)
        assertTrue(prefs.dualSignalEnabled())
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        assertFalse(prefs.dualSignalEnabled())
    }

    @Test
    fun oldBatteryCenteredOffsetIsInheritedUntilEachNewSliderIsSaved() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.BATTERY_CENTER
        prefs.edit()
            .putString(visualModeKey(false, CONTENT_LAYOUT_KEY), layout.persistedValue)
            .putFloat(visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY), topOffsetYRaw(5f))
            .apply()

        val initial = prefs.readVisualCfg()
        assertEquals(5f, initial.wifiOffsetY, 0f)
        assertEquals(5f, initial.mobileTypeOffsetY, 0f)
        assertEquals(topOffsetYRaw(5f), initial.topOffsetY, 0f)

        prefs.edit()
            .putFloat(visualProfileKey(layout, WIFI_VERTICAL_OFFSET_KEY), -1f)
            .apply()
        assertEquals(-1f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(5f, prefs.readVisualCfg().mobileTypeOffsetY, 0f)

        val result = prefs.readVisualCfg().copy(mobileTypeOffsetY = 3f)
        prefs.edit().putVisualCfg(result).apply()
        assertEquals(-1f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(3f, prefs.readVisualCfg().mobileTypeOffsetY, 0f)
    }

    @Test
    fun legacyNetworkCenteredReadoutDoesNotAffectNewNetworkOffsets() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putFloat(visualProfileKey(ContentLayout.NETWORK_CENTER, BATTERY_TOP_VERTICAL_OFFSET_KEY), topOffsetYRaw(6f))
            .apply()
        assertEquals(0f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(0f, prefs.readVisualCfg().mobileTypeOffsetY, 0f)
        assertEquals(topOffsetYRaw(6f), prefs.readVisualCfg().topOffsetY, 0f)
    }

    @Test
    fun importedVisualKeysPreserveUnrelatedPrefsAndRollbackLosslessly() {
        val prefs = MemoryPreferences()
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
        val prefs = MemoryPreferences()
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
        val prefs = MemoryPreferences()
        val snapshot = prefs.backupFeatureSnapshot()
        prefs.edit()
            .putBoolean(FEATURE_ENABLED_KEY, true)
            .putLong(FEATURE_CHANGED_AT_NS_KEY, 99L)
            .apply()

        assertTrue(prefs.rollbackBackupFeatures(snapshot))
        assertTrue(prefs.all.isEmpty())
    }

    @Test
    fun activatingBatterySchemeUpdatesOnlyTheCurrentSimMode() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putString(BATTERY_COLOR_PRESET_KEY, BatteryColorPreset.RECOMMENDED.persistedValue)
            .putString(
                batteryColorModeKey(BatteryColorSlot.CHARGING),
                BatteryColorMode.CUSTOM.persistedValue,
            )
            .putInt(batteryColorOverrideKey(BatteryColorSlot.CHARGING), 0xFF123456.toInt())
            .putString(
                visualModeKey(true, BATTERY_COLOR_PRESET_KEY),
                BatteryColorPreset.HYPEROS.persistedValue,
            )
            .apply()
        val editor = prefs.edit()
        writeBatterySchemeLibrary(editor, BatteryColorSchemeLibrary())
        editor.apply()

        val schemes = BatterySchemeRepo(prefs)
        schemes.activateScheme(BATTERY_COLOR_SCHEME_IOS_KEY)

        assertEquals(BATTERY_COLOR_SCHEME_IOS_KEY, schemes.current().activeKey(true))
        assertEquals(BATTERY_COLOR_SCHEME_HYPEROS_KEY, schemes.current().activeKey(false))
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = true).batteryColorPreset)
        assertEquals(BatteryColorMode.PRESET, prefs.readVisualCfg(dual = true).batteryColorModes.charging)
        assertFalse(prefs.contains(visualModeKey(true, batteryColorOverrideKey(BatteryColorSlot.CHARGING))))
        assertEquals(BatteryColorPreset.RECOMMENDED, prefs.readVisualCfg(dual = false).batteryColorPreset)
        assertEquals(BatteryColorMode.CUSTOM, prefs.readVisualCfg(dual = false).batteryColorModes.charging)
        assertEquals(0xFF123456.toInt(), prefs.readVisualCfg(dual = false).batteryColorOverrides.charging)

        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        schemes.activateScheme(BATTERY_COLOR_SCHEME_HYPEROS_KEY)
        assertEquals(BatteryColorPreset.HYPEROS, prefs.readVisualCfg(dual = false).batteryColorPreset)
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = true).batteryColorPreset)
        assertEquals(BATTERY_COLOR_SCHEME_HYPEROS_KEY, schemes.current().activeKey(false))
        assertEquals(BATTERY_COLOR_SCHEME_IOS_KEY, schemes.current().activeKey(true))
    }

    @Test
    fun editingActiveCustomSchemeUsesDualBankWithoutTouchingSingle() {
        val prefs = MemoryPreferences()
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        val schemes = BatterySchemeRepo(prefs)
        val id = requireNotNull(schemes.createCustom("Dual colors", BATTERY_COLOR_SCHEME_HYPEROS_KEY))

        schemes.activateScheme(customSchemeKey(id))
        schemes.setCustomColor(id, BatteryColorSlot.CHARGING, 0xFF2468AC.toInt())

        assertEquals(BatteryColorMode.CUSTOM, prefs.readVisualCfg(dual = true).batteryColorModes.charging)
        assertEquals(0xFF2468AC.toInt(), prefs.readVisualCfg(dual = true).batteryColorOverrides.charging)
        assertEquals(BatteryColorMode.PRESET, prefs.readVisualCfg(dual = false).batteryColorModes.charging)
        assertFalse(prefs.contains(batteryColorOverrideKey(BatteryColorSlot.CHARGING)))
    }

    @Test
    fun migratingBatterySchemeWhileDualDoesNotRewriteSingleColorBank() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putString(BATTERY_COLOR_PRESET_KEY, BatteryColorPreset.RECOMMENDED.persistedValue)
            .putString(
                visualModeKey(true, BATTERY_COLOR_PRESET_KEY),
                BatteryColorPreset.IOS_STYLE.persistedValue,
            )
            .apply()

        val schemes = BatterySchemeRepo(prefs)

        assertEquals(BATTERY_COLOR_SCHEME_IOS_KEY, schemes.current().activeKey(true))
        assertEquals(BATTERY_COLOR_SCHEME_LOW_SATURATION_KEY, schemes.current().activeKey(false))
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = true).batteryColorPreset)
        assertEquals(BatteryColorPreset.RECOMMENDED, prefs.readVisualCfg(dual = false).batteryColorPreset)
    }

    private class MemoryPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun getString(key: String?, defValue: String?): String? =
            values[key] as? String ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(
            key: String?,
            defValues: MutableSet<String>?,
        ): MutableSet<String>? = (values[key] as? Set<String>)?.toMutableSet() ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long =
            values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float =
            values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            values[key] as? Boolean ?: defValue

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        override fun edit(): SharedPreferences.Editor = Editor()

        private inner class Editor : SharedPreferences.Editor {
            private val changes = linkedMapOf<String, Any?>()
            private var clearFirst = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor =
                put(key, value)
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
                put(key, values?.toSet())
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor =
                put(key, value)
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor =
                put(key, value)
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
                put(key, value)
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor =
                put(key, value)
            override fun remove(key: String?): SharedPreferences.Editor = put(key, null)

            override fun clear(): SharedPreferences.Editor {
                clearFirst = true
                return this
            }

            private fun put(key: String?, value: Any?): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun commit(): Boolean {
                if (clearFirst) values.clear()
                changes.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
                return true
            }

            override fun apply() {
                commit()
            }
        }
    }
}
