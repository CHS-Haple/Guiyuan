package com.chaners.guiyuan.settings

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualPersistenceTest {
    @Test
    fun invalidStoredSingleRowStyleDoesNotReachSettingsOrRenderer() {
        val prefs = MemoryPreferences()
        prefs.edit().putString(
            visualProfileKey(ContentLayout.NETWORK_CENTER, SINGLE_ROW_DOT_STYLE_KEY),
            SignalDotArrangement.BOTH_SIDES.persistedValue,
        ).apply()
        assertEquals(SignalDotArrangement.CURVED, prefs.readVisualCfg().singleRowDotArrangement)
    }


    @Test
    fun firstDualEnableDefaultsToBothSidesWithoutOverwritingSavedArrangement() {
        val prefs = MemoryPreferences()
        assertFalse(prefs.dualSignalEnabled())
        assertEquals(SignalDotArrangement.BOTH_SIDES, prefs.readVisualCfg().dualRowDotArrangement)
        migrateVisualPrefsIfNeeded(prefs)
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        assertEquals(SignalDotArrangement.BOTH_SIDES, prefs.readVisualCfg().dualRowDotArrangement)

        val saved = visualProfileKey(ContentLayout.NETWORK_CENTER, DUAL_ROW_DOT_ARRANGEMENT_KEY)
        prefs.edit().putString(saved, SignalDotArrangement.CURVED.persistedValue).apply()
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        assertEquals(SignalDotArrangement.CURVED, prefs.readVisualCfg().dualRowDotArrangement)
    }

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
    fun resetKeepsNewSharedValuesAfterDualModeAndReinitialization() {
        val prefs = MemoryPreferences()
        resetVisualPreferences(prefs, BatteryColorSchemeLibrary())
        assertEquals(1, prefs.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0))
        assertEquals(4, prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0))
        assertTrue(prefs.getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false))

        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putFloat(visualProfileKey(ContentLayout.NETWORK_CENTER, WIFI_SIZE_SCALE_KEY), 0.86f)
            .apply()
        val before = prefs.all.toMap()
        migrateVisualPrefsIfNeeded(prefs)
        assertEquals(before, prefs.all)
        assertEquals(0.86f, prefs.readVisualCfg(dual = true).wifiScale, 0f)
        assertEquals(0.86f, prefs.readVisualCfg(dual = false).wifiScale, 0f)
    }

    @Test
    fun dualOffsetsStayIndependentAcrossStylesAndLayouts() {
        val prefs = MemoryPreferences()
        val dual = VisualCfg(
            dualSignal = true,
            dualRowDotArrangement = SignalDotArrangement.CURVED,
            curvedSignalDotOffsetY = 4f,
            horizontalSignalDotOffsetY = -7f,
        )
        prefs.edit().putVisualCfg(dual).apply()
        val network = prefs.readVisualCfg(dual = true, layoutOverride = ContentLayout.NETWORK_CENTER)
        assertEquals(4f, network.activeSignalDotOffsetY, 0f)
        assertEquals(-7f, network.copy(dualRowDotArrangement = SignalDotArrangement.HORIZONTAL).activeSignalDotOffsetY, 0f)

        prefs.edit().putVisualCfg(
            dual.copy(
                layout = ContentLayout.BATTERY_CENTER,
                dualRowDotArrangement = SignalDotArrangement.HORIZONTAL,
                curvedSignalDotOffsetY = -2f,
                horizontalSignalDotOffsetY = 5f,
            ),
        ).apply()
        val battery = prefs.readVisualCfg(dual = true, layoutOverride = ContentLayout.BATTERY_CENTER)
        assertEquals(5f, battery.activeSignalDotOffsetY, 0f)
        assertEquals(-2f, battery.copy(dualRowDotArrangement = SignalDotArrangement.CURVED).activeSignalDotOffsetY, 0f)
        val original = prefs.readVisualCfg(dual = true, layoutOverride = ContentLayout.NETWORK_CENTER)
        assertEquals(4f, original.curvedSignalDotOffsetY, 0f)
        assertEquals(-7f, original.horizontalSignalDotOffsetY, 0f)
        assertEquals(-2f, prefs.readVisualCfg(dual = false).activeSignalDotOffsetY, 0f)
    }

    @Test
    fun signalDotOffsetSliderWritesVisibleDualRowBankInBatteryCenteredLayout() {
        val prefs = MemoryPreferences()
        for (layout in listOf(ContentLayout.NETWORK_CENTER, ContentLayout.BATTERY_CENTER)) {
            for (dual in listOf(false, true)) {
                prefs.edit().putVisualCfg(
                    VisualCfg(
                        layout = layout,
                        dualSignal = dual,
                        dualRowDotArrangement = SignalDotArrangement.HORIZONTAL,
                        curvedSignalDotOffsetY = 1f,
                        horizontalSignalDotOffsetY = 2f,
                    ),
                ).apply()
            }
        }

        // Reproduce B1726: current layout uses a profile-scoped horizontal arrangement,
        // but the retired global style preference is absent.
        prefs.edit()
            .putString(CONTENT_LAYOUT_KEY, ContentLayout.BATTERY_CENTER.persistedValue)
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .apply()
        assertFalse(prefs.contains(DUAL_ROW_DOT_ARRANGEMENT_KEY))
        assertEquals(SignalDotArrangement.HORIZONTAL, prefs.readVisualCfg().dualRowDotArrangement)
        prefs.writeSignalDotOffsetY(-6f)

        val batteryDual = prefs.readVisualCfg()
        assertEquals(-6f, batteryDual.activeSignalDotOffsetY, 0f)
        assertEquals(-6f, batteryDual.horizontalSignalDotOffsetY, 0f)
        assertEquals(1f, batteryDual.curvedSignalDotOffsetY, 0f)
        assertEquals(2f, prefs.readVisualCfg(
            dual = true,
            layoutOverride = ContentLayout.NETWORK_CENTER,
        ).horizontalSignalDotOffsetY, 0f)
        assertEquals(-6f, prefs.readVisualCfg(
            dual = false,
            layoutOverride = ContentLayout.BATTERY_CENTER,
        ).horizontalSignalDotOffsetY, 0f)

        // The single-row arrangement controls the single-mode slider's offset bank.
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false)
            .putString(
                visualProfileKey(ContentLayout.BATTERY_CENTER, SINGLE_ROW_DOT_STYLE_KEY),
                SignalDotArrangement.CURVED.persistedValue,
            ).apply()
        prefs.writeSignalDotOffsetY(4f)
        val batterySingle = prefs.readVisualCfg()
        assertEquals(4f, batterySingle.activeSignalDotOffsetY, 0f)
        assertEquals(-6f, batterySingle.horizontalSignalDotOffsetY, 0f)
        assertEquals(-6f, prefs.readVisualCfg(dual = true).horizontalSignalDotOffsetY, 0f)

        // Switching back to the other layout cannot overwrite the battery bank.
        prefs.edit()
            .putString(CONTENT_LAYOUT_KEY, ContentLayout.NETWORK_CENTER.persistedValue)
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .apply()
        prefs.writeSignalDotOffsetY(7f)
        assertEquals(7f, prefs.readVisualCfg().activeSignalDotOffsetY, 0f)
        assertEquals(-6f, prefs.readVisualCfg(
            dual = true,
            layoutOverride = ContentLayout.BATTERY_CENTER,
        ).horizontalSignalDotOffsetY, 0f)
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
            -2f,
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
            .apply()
        assertFalse(prefs.readVisualCfg(dual = false, layoutOverride = ContentLayout.BATTERY_CENTER).showTopReadout)
        assertFalse(prefs.readVisualCfg(dual = true, layoutOverride = ContentLayout.BATTERY_CENTER).showTopReadout)
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
        assertEquals(dual.normalized().copy(dualSignal = false), prefs.readVisualCfg(dual = false))
        assertEquals(ContentLayout.BATTERY_CENTER, prefs.readLayout())
        assertFalse(prefs.all.keys.any { it.startsWith("dual.battery_color_") })

        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg().batteryColorPreset)
        assertEquals(1f, prefs.readVisualCfg().combinedScale, 0.001f)
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg().batteryColorPreset)
        assertEquals(1f, prefs.readVisualCfg().combinedScale, 0.001f)
    }


    @Test
    fun oldRemoteDualBankRemainsReadableUntilTheSharedMirrorIsPublished() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.NETWORK_CENTER
        val sharedScale = visualProfileKey(layout, WIFI_SIZE_SCALE_KEY)
        val oldDualScale = visualModeKey(true, sharedScale)
        val sharedStyle = visualProfileKey(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY)
        val oldDualStyle = visualModeKey(true, sharedStyle)
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putFloat(sharedScale, 0.71f)
            .putFloat(oldDualScale, 1.13f)
            .putFloat(visualProfileKey(layout, COMBINED_SCALE_KEY), 0.75f)
            .putInt(visualModeKey(true, visualProfileKey(layout, WIFI_WEIGHT_KEY)), 120)
            .putString(sharedStyle, SignalDotArrangement.CURVED.persistedValue)
            .putString(oldDualStyle, SignalDotArrangement.HORIZONTAL.persistedValue)
            .apply()

        // SystemUI can read the untouched old remote store before app startup.
        val before = prefs.all.toMap()
        val legacy = prefs.readVisualCfg()
        assertEquals(1.13f, legacy.wifiScale, 0f)
        assertEquals(120, legacy.wifiWeight)
        assertEquals(SignalDotArrangement.HORIZONTAL, legacy.dualRowDotArrangement)
        assertEquals(COMBINED_SCALE_DEFAULT, legacy.combinedScale, 0f)
        assertEquals(0.71f, prefs.readVisualCfg(dual = false).wifiScale, 0f)
        assertEquals(before, prefs.all)

        // The same writer mirrors new settings and their schema atomically.
        prefs.edit().putVisualCfg(
            VisualCfg(
                layout = layout,
                dualSignal = true,
                wifiScale = 0.89f,
                wifiWeight = 86,
                combinedScale = 0.83f,
                dualRowDotArrangement = SignalDotArrangement.CURVED,
            ),
        ).apply()
        assertEquals(4, prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0))
        assertEquals(1, prefs.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0))
        assertTrue(prefs.contains(oldDualScale))
        for (dual in listOf(false, true)) {
            val current = prefs.readVisualCfg(dual = dual, layoutOverride = layout)
            assertEquals(0.89f, current.wifiScale, 0f)
            assertEquals(86, current.wifiWeight)
            assertEquals(0.83f, current.combinedScale, 0f)
            assertEquals(SignalDotArrangement.CURVED, current.dualRowDotArrangement)
        }
    }

    @Test
    fun freshSharedPreferencesDoNotRequireAnOldDualBank() {
        val prefs = MemoryPreferences()
        val key = visualProfileKey(ContentLayout.NETWORK_CENTER, WIFI_SIZE_SCALE_KEY)
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putFloat(key, 0.92f)
            .apply()
        assertEquals(0.92f, prefs.readVisualCfg().wifiScale, 0f)
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
        assertEquals(1f, remote.readVisualCfg().combinedScale, 0.001f)
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
        assertEquals(0.75f, prefs.readVisualCfg().combinedScale, 0.0001f)
        assertEquals(-3f, prefs.readVisualCfg().wifiOffsetY, 0f)
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
        assertEquals(1f, prefs.readVisualCfg(dual = false).combinedScale, 0.001f)
        assertEquals(1f, prefs.readVisualCfg(dual = true).combinedScale, 0.001f)
    }

    @Test
    fun applyingAndEditingSharedSchemeNeverSplitsBatteryColors() {
        val prefs = MemoryPreferences()
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
    fun releasedVersionDotStyleMigratesIntoEachLayoutWithoutLosingSimChoice() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putString(visualModeKey(false, LEGACY_DUAL_DOT_STYLE_KEY), SignalDotArrangement.CURVED.persistedValue)
            .putString(visualModeKey(true, LEGACY_DUAL_DOT_STYLE_KEY), SignalDotArrangement.HORIZONTAL.persistedValue)
            .apply()
        migrateSignalDotArrangementsIfNeeded(prefs)
        for (layout in ContentLayout.entries) {
            assertEquals(SignalDotArrangement.CURVED, prefs.readVisualCfg(false, layout).dualRowDotArrangement)
            assertEquals(SignalDotArrangement.CURVED, prefs.readVisualCfg(true, layout).dualRowDotArrangement)
        }
        assertEquals(4, prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0))
        assertFalse(prefs.contains(visualModeKey(false, LEGACY_DUAL_DOT_STYLE_KEY)))
        assertFalse(prefs.contains(visualModeKey(true, LEGACY_DUAL_DOT_STYLE_KEY)))
        val snapshot = prefs.all.toMap()
        migrateSignalDotArrangementsIfNeeded(prefs)
        assertEquals(snapshot, prefs.all)
    }

    @Test
    fun legacySingleRowPreferenceWorksBeforeAppMigration() {
        val prefs = MemoryPreferences()
        val key = visualProfileKey(ContentLayout.NETWORK_CENTER, DUAL_ROW_DOT_ARRANGEMENT_KEY)
        val flat = visualProfileKey(ContentLayout.NETWORK_CENTER, LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY)
        prefs.edit().putString(key, SignalDotArrangement.HORIZONTAL.persistedValue)
            .putBoolean(flat, true).apply()
        val beforeMigration = prefs.readVisualCfg(true, ContentLayout.NETWORK_CENTER)
        assertEquals(SignalDotArrangement.HORIZONTAL, beforeMigration.singleRowDotArrangement)
        assertEquals(SignalDotArrangement.HORIZONTAL, beforeMigration.dualRowDotArrangement)
        prefs.edit().putBoolean(flat, false).apply()
        assertEquals(
            SignalDotArrangement.CURVED,
            prefs.readVisualCfg(true, ContentLayout.NETWORK_CENTER).singleRowDotArrangement,
        )
    }

    @Test
    fun legacySingleRowMigrationPreservesActiveModeAndSharesItsChoice() {
        for (activeDual in listOf(false, true)) {
            val prefs = MemoryPreferences()
            val layout = ContentLayout.NETWORK_CENTER
            prefs.edit().putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 3)
                .putBoolean(DUAL_SIGNAL_ENABLED_KEY, activeDual)
                .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
                .putString(
                    visualModeKey(true, visualProfileKey(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY)),
                    SignalDotArrangement.HORIZONTAL.persistedValue,
                )
                .putBoolean(
                    visualModeKey(true, visualProfileKey(layout, LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY)),
                    true,
                ).apply()
            migrateSignalDotArrangementsIfNeeded(prefs)
            val chosen = if (activeDual) SignalDotArrangement.HORIZONTAL else SignalDotArrangement.CURVED
            for (dual in listOf(false, true)) {
                assertEquals(chosen, prefs.readVisualCfg(dual, layout).singleRowDotArrangement)
                assertFalse(prefs.contains(
                    visualProfileKey(layout, LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY),
                ))
            }
            assertEquals(
                if (activeDual) SignalDotArrangement.HORIZONTAL else SignalDotArrangement.BOTH_SIDES,
                prefs.readVisualCfg(true, layout).dualRowDotArrangement,
            )
            assertEquals(4, prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0))
            val snapshot = prefs.all.toMap()
            migrateSignalDotArrangementsIfNeeded(prefs)
            assertEquals(snapshot, prefs.all)
        }
    }

    @Test
    fun threeLayoutsRememberSizesOffsetsAndStylesIndependently() {
        val prefs = MemoryPreferences()
        migrateSignalDotArrangementsIfNeeded(prefs)
        assertEquals(0.85f, prefs.readVisualCfg(false, ContentLayout.MOBILE_CENTER).mobileTypeScale, 0f)
        assertEquals(-3f, prefs.readVisualCfg(false, ContentLayout.MOBILE_CENTER).mobileTypeBottomOffsetY, 0f)
        val centered = VisualCfg(
            layout = ContentLayout.MOBILE_CENTER,
            dualRowDotArrangement = SignalDotArrangement.HORIZONTAL,
            mobileTypeScale = 0.92f,
            mobileTypeBottomOffsetY = 6f,
            singleRowDotArrangement = SignalDotArrangement.HORIZONTAL,
        )
        prefs.edit().putVisualCfg(centered).apply()
        val battery = VisualCfg(
            layout = ContentLayout.BATTERY_CENTER,
            dualRowDotArrangement = SignalDotArrangement.CURVED,
            mobileTypeScale = 0.77f,
        )
        prefs.edit().putVisualCfg(battery).apply()
        val centeredSaved = prefs.readVisualCfg(false, ContentLayout.MOBILE_CENTER)
        val batterySaved = prefs.readVisualCfg(false, ContentLayout.BATTERY_CENTER)
        val normalSaved = prefs.readVisualCfg(false, ContentLayout.NETWORK_CENTER)
        assertTrue(centeredSaved.centeredMobileSignal)
        assertEquals(SignalDotArrangement.HORIZONTAL, centeredSaved.dualRowDotArrangement)
        assertEquals(SignalDotArrangement.HORIZONTAL, centeredSaved.singleRowDotArrangement)
        assertEquals(0.92f, centeredSaved.mobileTypeScale, 0f)
        assertEquals(6f, centeredSaved.mobileTypeBottomOffsetY, 0f)
        assertEquals(SignalDotArrangement.CURVED, batterySaved.dualRowDotArrangement)
        assertEquals(0.77f, batterySaved.mobileTypeScale, 0f)
        assertFalse(normalSaved.centeredMobileSignal)
        assertEquals(SignalDotArrangement.BOTH_SIDES, normalSaved.dualRowDotArrangement)
        assertEquals(1f, normalSaved.mobileTypeScale, 0f)
    }

    @Test
    fun bottomLabelOffsetAndStyleAreIsolatedAcrossSingleAndDualBanks() {
        val prefs = MemoryPreferences()
        migrateSignalDotArrangementsIfNeeded(prefs)
        prefs.edit().putVisualCfg(
            VisualCfg(layout = ContentLayout.MOBILE_CENTER, mobileTypeBottomOffsetY = 2f),
        ).apply()
        prefs.edit().putVisualCfg(
            VisualCfg(
                layout = ContentLayout.MOBILE_CENTER,
                dualSignal = true,
                mobileTypeBottomOffsetY = -7f,
                dualRowDotArrangement = SignalDotArrangement.HORIZONTAL,
            ),
        ).apply()
        assertEquals(-7f, prefs.readVisualCfg(false, ContentLayout.MOBILE_CENTER).mobileTypeBottomOffsetY, 0f)
        assertEquals(-7f, prefs.readVisualCfg(true, ContentLayout.MOBILE_CENTER).mobileTypeBottomOffsetY, 0f)
        assertEquals(SignalDotArrangement.HORIZONTAL, prefs.readVisualCfg(false, ContentLayout.MOBILE_CENTER).dualRowDotArrangement)
        assertEquals(SignalDotArrangement.HORIZONTAL, prefs.readVisualCfg(true, ContentLayout.MOBILE_CENTER).dualRowDotArrangement)
    }


    @Test
    fun migratedLayoutsKeepVisibleDualValuesAndShareAcrossSimSwitches() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.MOBILE_CENTER
        val shared = visualProfileKey(layout, WIFI_SIZE_SCALE_KEY)
        val legacyDual = visualModeKey(true, shared)
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putFloat(shared, 0.72f)
            .putFloat(legacyDual, 1.18f)
            .putString(CONTENT_LAYOUT_KEY, layout.persistedValue)
            .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
            .apply()
        migrateSharedLayoutVisualsIfNeeded(prefs)
        assertEquals(1.18f, prefs.readVisualCfg(dual = true).wifiScale, 0f)
        assertEquals(1.18f, prefs.readVisualCfg(dual = false).wifiScale, 0f)
        assertFalse(prefs.contains(legacyDual))
        val snapshot = prefs.all.toMap()
        migrateSharedLayoutVisualsIfNeeded(prefs)
        assertEquals(snapshot, prefs.all)
    }

    @Test
    fun migratingSingleModeDoesNotOverwriteItsSavedLayoutValues() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.BATTERY_CENTER
        val shared = visualProfileKey(layout, WIFI_VERTICAL_OFFSET_KEY)
        prefs.edit()
            .putFloat(shared, -4f)
            .putFloat(visualModeKey(true, shared), 7f)
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, false)
            .apply()
        migrateSharedLayoutVisualsIfNeeded(prefs)
        assertEquals(-4f, prefs.readVisualCfg(false, layout).wifiOffsetY, 0f)
        assertEquals(-4f, prefs.readVisualCfg(true, layout).wifiOffsetY, 0f)
    }

    @Test
    fun migratingActiveDualKeepsDefaultWhenItsOldBankIsMissing() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.NETWORK_CENTER
        val canonical = visualProfileKey(layout, WIFI_SIZE_SCALE_KEY)
        val dualKey = visualModeKey(true, canonical)
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
            .putFloat(canonical, 0.7f)
            .putFloat(WIFI_SIZE_SCALE_KEY, 0.6f)
            .apply()

        migrateVisualPrefsIfNeeded(prefs)
        assertEquals(WIFI_SIZE_SCALE_DEFAULT, prefs.readVisualCfg(dual = true).wifiScale, 0f)
        assertEquals(WIFI_SIZE_SCALE_DEFAULT, prefs.readVisualCfg(dual = false).wifiScale, 0f)
        assertFalse(prefs.contains(canonical))
        assertFalse(prefs.contains(dualKey))
        assertFalse(prefs.contains(WIFI_SIZE_SCALE_KEY))
    }

    @Test
    fun migratingLegacySingleRootValueMakesItLayoutScoped() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, false)
            .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
            .putFloat(WIFI_SIZE_SCALE_KEY, 0.82f)
            .apply()

        migrateVisualPrefsIfNeeded(prefs)
        for (layout in ContentLayout.entries) {
            assertEquals(0.82f, prefs.readVisualCfg(false, layout).wifiScale, 0f)
            assertEquals(0.82f, prefs.readVisualCfg(true, layout).wifiScale, 0f)
        }
        assertFalse(prefs.contains(WIFI_SIZE_SCALE_KEY))
        assertEquals(1, prefs.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0))
    }

    @Test
    fun b1734V4DualDotBanksSurviveSharedLayoutMigration() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.MOBILE_CENTER
        val singleDotKey = visualProfileKey(layout, SINGLE_ROW_DOT_STYLE_KEY)
        val dualDotKey = visualProfileKey(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY)
        val oldDualSingleKey = visualModeKey(true, singleDotKey)
        val oldDualDotKey = visualModeKey(true, dualDotKey)
        // B1734 finished the dot arrangement schema, but still kept
        // separate single/dual preference banks for the same layout.
        prefs.edit()
            .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putString(singleDotKey, SignalDotArrangement.CURVED.persistedValue)
            .putString(dualDotKey, SignalDotArrangement.CURVED.persistedValue)
            .putString(oldDualSingleKey, SignalDotArrangement.HORIZONTAL.persistedValue)
            .putString(oldDualDotKey, SignalDotArrangement.HORIZONTAL.persistedValue)
            .apply()

        migrateVisualPrefsIfNeeded(prefs)

        assertEquals(1, prefs.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0))
        assertEquals(4, prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0))
        for (dual in listOf(false, true)) {
            val result = prefs.readVisualCfg(dual, layout)
            assertEquals(SignalDotArrangement.HORIZONTAL, result.singleRowDotArrangement)
            assertEquals(SignalDotArrangement.HORIZONTAL, result.dualRowDotArrangement)
        }
        assertFalse(prefs.contains(oldDualSingleKey))
        assertFalse(prefs.contains(oldDualDotKey))
        val snapshot = prefs.all.toMap()
        migrateVisualPrefsIfNeeded(prefs)
        assertEquals(snapshot, prefs.all)
    }

    @Test
    fun combinedLegacyMigrationDoesNotRestoreStaleSingleModeRootStyles() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.NETWORK_CENTER
        // Legacy per-mode selection is horizontal; these unscoped fields
        // must not override the dot-style selection staged just before
        // layout profile flattening in the same SharedPreferences.Editor.
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, false)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 3)
            .putString(
                visualModeKey(false, LEGACY_DUAL_DOT_STYLE_KEY),
                SignalDotArrangement.HORIZONTAL.persistedValue,
            )
            .putString(DUAL_ROW_DOT_ARRANGEMENT_KEY, SignalDotArrangement.CURVED.persistedValue)
            .putString(SINGLE_ROW_DOT_STYLE_KEY, SignalDotArrangement.HORIZONTAL.persistedValue)
            .apply()

        migrateVisualPrefsIfNeeded(prefs)

        for (dual in listOf(false, true)) {
            val cfg = prefs.readVisualCfg(dual, layout)
            assertEquals(SignalDotArrangement.HORIZONTAL, cfg.dualRowDotArrangement)
            assertEquals(SignalDotArrangement.CURVED, cfg.singleRowDotArrangement)
        }
        assertFalse(prefs.contains(DUAL_ROW_DOT_ARRANGEMENT_KEY))
        assertFalse(prefs.contains(SINGLE_ROW_DOT_STYLE_KEY))
        assertEquals(4, prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0))
        assertEquals(1, prefs.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0))
        val migrated = prefs.all.toMap()
        migrateVisualPrefsIfNeeded(prefs)
        assertEquals(migrated, prefs.all)
    }

    @Test
    fun freshLegacyMigrationPublishesBothSchemasInOneWrite() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.MOBILE_CENTER
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 3)
            .putString(
                visualModeKey(true, LEGACY_DUAL_DOT_STYLE_KEY),
                SignalDotArrangement.HORIZONTAL.persistedValue,
            )
            .putBoolean(
                visualModeKey(true, visualProfileKey(layout, LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY)),
                true,
            )
            .putFloat(
                visualModeKey(true, visualProfileKey(layout, WIFI_SIZE_SCALE_KEY)),
                0.91f,
            )
            .apply()
        val priorWrites = prefs.writeCount

        migrateVisualPrefsIfNeeded(prefs)

        // The old dot-style conversion and profile flattening are one durable write.
        assertEquals(priorWrites + 1, prefs.writeCount)
        assertEquals(4, prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0))
        assertEquals(1, prefs.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0))
        for (dual in listOf(false, true)) {
            val cfg = prefs.readVisualCfg(dual, layout)
            assertEquals(SignalDotArrangement.HORIZONTAL, cfg.dualRowDotArrangement)
            assertEquals(SignalDotArrangement.HORIZONTAL, cfg.singleRowDotArrangement)
            assertEquals(0.91f, cfg.wifiScale, 0f)
        }
        assertFalse(prefs.contains(visualModeKey(true, visualProfileKey(layout, WIFI_SIZE_SCALE_KEY))))
        val migrated = prefs.all.toMap()
        migrateVisualPrefsIfNeeded(prefs)
        assertEquals(migrated, prefs.all)
        assertEquals(priorWrites + 1, prefs.writeCount)
    }

    @Test
    fun preV4DualDotStyleIsRetainedWhenFlatteningOldProfiles() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.MOBILE_CENTER
        prefs.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 3)
            .putString(
                visualModeKey(true, LEGACY_DUAL_DOT_STYLE_KEY),
                SignalDotArrangement.HORIZONTAL.persistedValue,
            )
            .putBoolean(
                visualModeKey(true, visualProfileKey(layout, LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY)),
                true,
            )
            .apply()

        migrateVisualPrefsIfNeeded(prefs)
        val migrated = prefs.readVisualCfg(dual = true, layoutOverride = layout)
        assertEquals(SignalDotArrangement.HORIZONTAL, migrated.singleRowDotArrangement)
        assertEquals(SignalDotArrangement.HORIZONTAL, migrated.dualRowDotArrangement)
        assertEquals(
            migrated.copy(dualSignal = false),
            prefs.readVisualCfg(dual = false, layoutOverride = layout),
        )
        assertFalse(prefs.contains(visualModeKey(true, LEGACY_DUAL_DOT_STYLE_KEY)))
        assertEquals(4, prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0))
        assertEquals(1, prefs.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0))
    }

    private class MemoryPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any>()
        var writeCount = 0
            private set

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
                writeCount++
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
