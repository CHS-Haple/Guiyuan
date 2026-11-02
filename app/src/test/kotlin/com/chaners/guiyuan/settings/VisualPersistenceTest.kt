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
    fun contentStyleIsSharedAcrossSignalModesAndLayouts() {
        val prefs = MemoryPreferences()
        prefs.edit().putString(DUAL_DOT_STYLE_KEY, DualDotStyle.DUO.persistedValue).apply()
        for (dual in listOf(false, true)) {
            for (layout in ContentLayout.entries) {
                assertEquals(DualDotStyle.DUO, prefs.readVisualCfg(dual = dual, layoutOverride = layout).dualDotStyle)
            }
        }
        prefs.edit().putString(DUAL_DOT_STYLE_KEY, "invalid").apply()
        assertEquals(DualDotStyle.DEFAULT, prefs.readVisualCfg(dual = true).dualDotStyle)
    }

    @Test
    fun contentStyleMigrationPreservesActivePreferenceAndIsIdempotent() {
        for (dual in listOf(false, true)) {
            val prefs = MemoryPreferences()
            prefs.edit()
                .putBoolean(DUAL_SIGNAL_ENABLED_KEY, dual)
                .putString(DUAL_DOT_STYLE_KEY, DualDotStyle.DEFAULT.persistedValue)
                .putString(visualModeKey(true, DUAL_DOT_STYLE_KEY), DualDotStyle.DUO.persistedValue)
                .apply()
            migrateSharedContentStyleIfNeeded(prefs)
            val expected = if (dual) DualDotStyle.DUO else DualDotStyle.DEFAULT
            assertEquals(expected, prefs.readVisualCfg(dual = true).dualDotStyle)
            assertEquals(expected, prefs.readVisualCfg(dual = false).dualDotStyle)
            assertFalse(prefs.contains(visualModeKey(true, DUAL_DOT_STYLE_KEY)))
            val saved = prefs.all.toMap()
            migrateSharedContentStyleIfNeeded(prefs)
            assertEquals(saved, prefs.all)
        }
        val oldDualOnly = MemoryPreferences()
        oldDualOnly.edit()
            .putString(visualModeKey(true, DUAL_DOT_STYLE_KEY), DualDotStyle.DUO.persistedValue)
            .apply()
        migrateSharedContentStyleIfNeeded(oldDualOnly)
        assertEquals(DualDotStyle.DUO, oldDualOnly.readVisualCfg(dual = false).dualDotStyle)
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
    fun unconfiguredNetworkTopOffsetIsPlusThreeForBothSignalProfiles() {
        val prefs = MemoryPreferences()
        for (dual in listOf(false, true)) {
            val network = prefs.readVisualCfg(dual = dual, layoutOverride = ContentLayout.NETWORK_CENTER)
            assertFalse(network.showTopReadout)
            assertEquals(3f, topOffsetYUi(network.topOffsetY), 0f)
            assertEquals(0f, topOffsetYUi(
                prefs.readVisualCfg(dual = dual, layoutOverride = ContentLayout.BATTERY_CENTER).topOffsetY
            ), 0f)
        }
        val saved = topOffsetYRaw(-2f)
        prefs.edit()
            .putFloat(visualProfileKey(ContentLayout.NETWORK_CENTER, BATTERY_TOP_VERTICAL_OFFSET_KEY), saved)
            .apply()
        assertEquals(
            -2f,
            topOffsetYUi(prefs.readVisualCfg(dual = false, layoutOverride = ContentLayout.NETWORK_CENTER).topOffsetY),
            0f,
        )
        assertEquals(
            3f,
            topOffsetYUi(prefs.readVisualCfg(dual = true, layoutOverride = ContentLayout.NETWORK_CENTER).topOffsetY),
            0f,
        )
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
    fun singleAndDualKeepSeparateGeometryButShareBatteryColors() {
        val prefs = MemoryPreferences()
        val single = VisualCfg(
            layout = ContentLayout.BATTERY_CENTER,
            combinedScale = 0.9f,
            wifiOffsetY = -3f,
            batteryColorPreset = BatteryColorPreset.RECOMMENDED,
            batteryColorModes = BatteryColorModes(charging = BatteryColorMode.CUSTOM),
            batteryColorOverrides = BatteryColorOverrides(charging = 0xFF123456.toInt()),
        )
        val dual = VisualCfg(
            layout = ContentLayout.BATTERY_CENTER,
            dualSignal = true,
            combinedScale = 1.2f,
            wifiOffsetY = 4f,
            batteryColorPreset = BatteryColorPreset.IOS_STYLE,
            batteryColorModes = BatteryColorModes(powerSave = BatteryColorMode.FOLLOW_SYSTEM),
        )
        prefs.edit().putVisualCfg(single).apply()
        prefs.edit().putVisualCfg(dual).apply()

        assertEquals(dual.normalized(), prefs.readVisualCfg(dual = true))
        assertEquals(
            single.copy(
                batteryColorPreset = dual.batteryColorPreset,
                batteryColorModes = dual.batteryColorModes,
                batteryColorOverrides = dual.batteryColorOverrides,
            ).normalized(),
            prefs.readVisualCfg(dual = false),
        )
        assertEquals(ContentLayout.BATTERY_CENTER, prefs.readLayout())
        assertFalse(prefs.all.keys.any { it.startsWith("dual.battery_color_") })

        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg().batteryColorPreset)
        assertEquals(0.9f, prefs.readVisualCfg().combinedScale, 0.001f)
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg().batteryColorPreset)
        assertEquals(1f, prefs.readVisualCfg().combinedScale, 0.001f)
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
    fun remoteMirrorPreservesSharedColorsWhileSwitchingGeometry() {
        val local = MemoryPreferences()
        val remote = MemoryPreferences()
        val single = VisualCfg(
            combinedScale = 0.8f,
            batteryColorPreset = BatteryColorPreset.RECOMMENDED,
        )
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
        assertEquals(0.8f, remote.readVisualCfg().combinedScale, 0.001f)
        assertEquals(BatteryColorPreset.IOS_STYLE, remote.readVisualCfg().batteryColorPreset)
        assertEquals(BatteryColorPreset.IOS_STYLE, remote.readVisualCfg(dual = true).batteryColorPreset)

        local.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        remote.edit().putVisualCfg(local.readVisualCfg()).apply()
        assertEquals(dual.normalized(), remote.readVisualCfg())
        assertEquals(BatteryColorPreset.IOS_STYLE, remote.readVisualCfg(dual = false).batteryColorPreset)
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
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg().batteryColorPreset)
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
    fun activeDualMigrationKeepsItsColorsAndRetiresLegacyKeys() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putString(BATTERY_COLOR_PRESET_KEY, BatteryColorPreset.RECOMMENDED.persistedValue)
            .putString(
                BATTERY_COLOR_MODE_CHARGING_KEY,
                BatteryColorMode.CUSTOM.persistedValue,
            )
            .putInt(BATTERY_COLOR_CHARGING_KEY, 0xFF223344.toInt())
            .putString(
                visualModeKey(true, BATTERY_COLOR_PRESET_KEY),
                BatteryColorPreset.IOS_STYLE.persistedValue,
            )
            .putString(
                visualModeKey(true, BATTERY_COLOR_MODE_CHARGING_KEY),
                BatteryColorMode.CUSTOM.persistedValue,
            )
            .putInt(visualModeKey(true, BATTERY_COLOR_CHARGING_KEY), 0xFF112233.toInt())
            .putFloat(visualProfileKey(ContentLayout.NETWORK_CENTER, WIFI_SIZE_SCALE_KEY, true), 0.8f)
            .apply()

        migrateSharedBatteryColorsIfNeeded(prefs)
        val single = prefs.readVisualCfg(dual = false)
        val dual = prefs.readVisualCfg(dual = true)
        assertEquals(single.batteryColorPreset, dual.batteryColorPreset)
        assertEquals(BatteryColorPreset.IOS_STYLE, single.batteryColorPreset)
        assertEquals(BatteryColorMode.CUSTOM, single.batteryColorModes.charging)
        assertEquals(0xFF112233.toInt(), dual.batteryColorOverrides.charging)
        assertTrue(prefs.getInt(SHARED_BATTERY_COLORS_SCHEMA_KEY, 0) == 1)
        assertFalse(prefs.all.keys.any { it.startsWith("dual.battery_color_") })
        assertEquals(0.8f, prefs.readVisualCfg(dual = true).wifiScale, 0.001f)

        val once = prefs.all.toMap()
        migrateSharedBatteryColorsIfNeeded(prefs)
        assertEquals(once, prefs.all)
    }

    @Test
    fun activeSingleMigrationKeepsSingleColorsAndRemovesDualBank() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, false)
            .putString(BATTERY_COLOR_PRESET_KEY, BatteryColorPreset.IOS_STYLE.persistedValue)
            .putString(
                visualModeKey(true, BATTERY_COLOR_PRESET_KEY),
                BatteryColorPreset.RECOMMENDED.persistedValue,
            )
            .putInt(visualModeKey(true, BATTERY_COLOR_CHARGING_KEY), 0xFF112233.toInt())
            .apply()
        migrateSharedBatteryColorsIfNeeded(prefs)

        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = false).batteryColorPreset)
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = true).batteryColorPreset)
        assertFalse(prefs.contains(visualModeKey(true, BATTERY_COLOR_CHARGING_KEY)))
        assertFalse(prefs.contains(visualModeKey(true, BATTERY_COLOR_PRESET_KEY)))
    }

    @Test
    fun absentActiveDualKeysDoNotAccidentallyBorrowSingleOverrides() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putInt(BATTERY_COLOR_CHARGING_KEY, 0xFF112233.toInt())
            .putString(BATTERY_COLOR_PRESET_KEY, BatteryColorPreset.IOS_STYLE.persistedValue)
            .apply()

        migrateSharedBatteryColorsIfNeeded(prefs)
        assertFalse(prefs.contains(BATTERY_COLOR_CHARGING_KEY))
        assertFalse(prefs.contains(BATTERY_COLOR_PRESET_KEY))
        assertEquals(BatteryColorPreset.HYPEROS, prefs.readVisualCfg(dual = false).batteryColorPreset)
        assertEquals(BatteryColorPreset.HYPEROS, prefs.readVisualCfg(dual = true).batteryColorPreset)
    }

    @Test
    fun b1601DualSelectionMigrationKeepsOnlyOneAppliedSchemeKey() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putString(BATTERY_COLOR_SCHEME_ACTIVE_KEY, BATTERY_COLOR_SCHEME_HYPEROS_KEY)
            .putString(BATTERY_COLOR_SCHEME_DUAL_ACTIVE_KEY, BATTERY_COLOR_SCHEME_IOS_KEY)
            .putString(visualModeKey(true, BATTERY_COLOR_PRESET_KEY), BatteryColorPreset.IOS_STYLE.persistedValue)
            .apply()

        migrateSharedBatteryColorsIfNeeded(prefs)
        assertEquals(BATTERY_COLOR_SCHEME_IOS_KEY, prefs.getString(BATTERY_COLOR_SCHEME_ACTIVE_KEY, null))
        assertFalse(prefs.contains(BATTERY_COLOR_SCHEME_DUAL_ACTIVE_KEY))
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = false).batteryColorPreset)
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = true).batteryColorPreset)
    }

    @Test
    fun backupRestorationUsesArchivedActiveColorsForBothModes() {
        val prefs = MemoryPreferences()
        val editor = prefs.edit()
        editor.putVisualCfg(VisualCfg(
            dualSignal = false,
            combinedScale = 0.8f,
            batteryColorPreset = BatteryColorPreset.RECOMMENDED,
        ))
        editor.putVisualCfg(VisualCfg(
            dualSignal = true,
            combinedScale = 1f,
            batteryColorPreset = BatteryColorPreset.IOS_STYLE,
        ))
        editor.putBatteryColors(VisualCfg(
            dualSignal = false,
            batteryColorPreset = BatteryColorPreset.RECOMMENDED,
        ))
        editor.apply()

        assertEquals(BatteryColorPreset.RECOMMENDED, prefs.readVisualCfg(dual = false).batteryColorPreset)
        assertEquals(BatteryColorPreset.RECOMMENDED, prefs.readVisualCfg(dual = true).batteryColorPreset)
        assertEquals(0.8f, prefs.readVisualCfg(dual = false).combinedScale, 0.001f)
        assertEquals(1f, prefs.readVisualCfg(dual = true).combinedScale, 0.001f)
    }

    @Test
    fun resetThenReapplyInDualModeSurvivesNextMigrationPass() {
        val prefs = MemoryPreferences()
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        migrateSharedBatteryColorsIfNeeded(prefs)
        resetVisualPreferences(prefs, BatteryColorSchemeLibrary())
        assertEquals(1, prefs.getInt(SHARED_BATTERY_COLORS_SCHEMA_KEY, 0))

        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        val schemes = BatterySchemeRepo(prefs)
        schemes.activateScheme(BATTERY_COLOR_SCHEME_IOS_KEY)
        val saved = prefs.all.toMap()
        migrateVisualPrefsIfNeeded(prefs)

        assertEquals(BATTERY_COLOR_SCHEME_IOS_KEY, schemes.current().activeSchemeKey)
        for (dual in listOf(false, true)) {
            assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = dual).batteryColorPreset)
        }
        assertEquals(saved[BATTERY_COLOR_PRESET_KEY], prefs.getString(BATTERY_COLOR_PRESET_KEY, null))
        assertFalse(prefs.all.keys.any { it.startsWith("dual.battery_color_") })
    }

    @Test
    fun applyingAndEditingSharedSchemeNeverSplitsBatteryColors() {
        val prefs = MemoryPreferences()
        migrateSharedBatteryColorsIfNeeded(prefs)
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

    @Test
    fun activeDualMigrationLinksAnExistingMatchingCustomScheme() {
        val prefs = MemoryPreferences()
        migrateSharedBatteryColorsIfNeeded(prefs)
        val schemes = BatterySchemeRepo(prefs)
        val id = requireNotNull(schemes.createCustom("Existing palette", BATTERY_COLOR_SCHEME_IOS_KEY))
        val key = customSchemeKey(id)
        schemes.activateScheme(key)
        schemes.setCustomColor(id, BatteryColorSlot.CHARGING, 0xFF2468AC.toInt())
        val customColors = prefs.readVisualCfg()
        val original = requireNotNull(schemes.current().customById(id))
        val duplicate = original.copy(id = id + 1)
        val withDuplicate = schemes.current().copy(
            activeSchemeKey = duplicate.key,
            customSchemes = listOf(original, duplicate),
        )
        assertEquals(duplicate.key, withDuplicate.matchingKey(customColors))

        val editor = prefs.edit()
            .putInt(SHARED_BATTERY_COLORS_SCHEMA_KEY, 0)
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putString(BATTERY_COLOR_SCHEME_ACTIVE_KEY, BATTERY_COLOR_SCHEME_HYPEROS_KEY)
            .putString(
                visualModeKey(true, BATTERY_COLOR_PRESET_KEY),
                customColors.batteryColorPreset.persistedValue,
            )
        BatteryColorSlot.entries.forEach { slot ->
            editor.putString(
                visualModeKey(true, batteryColorModeKey(slot)),
                customColors.batteryColorModes.modeFor(slot).persistedValue,
            )
            customColors.batteryColorOverrides.colorFor(slot)?.let { color ->
                editor.putInt(visualModeKey(true, batteryColorOverrideKey(slot)), color)
            }
        }
        editor.apply()

        migrateSharedBatteryColorsIfNeeded(prefs)

        assertEquals(key, schemes.current().activeSchemeKey)
        assertEquals(customColors.batteryColorModes, prefs.readVisualCfg(dual = false).batteryColorModes)
        assertEquals(customColors.batteryColorOverrides, prefs.readVisualCfg(dual = true).batteryColorOverrides)
        assertFalse(prefs.all.keys.any { it.startsWith("dual.battery_color_") })
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
