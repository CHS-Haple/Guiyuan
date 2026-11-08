package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualPersistenceTest {
    @Test
    fun invalidStoredSingleRowStyleDoesNotReachSettingsOrRenderer() {
        val prefs = VisualTestPreferences()
        prefs.edit().putString(
            visualProfileKey(ContentLayout.NETWORK_CENTER, SINGLE_ROW_DOT_STYLE_KEY),
            SignalDotArrangement.BOTH_SIDES.persistedValue,
        ).apply()
        assertEquals(SignalDotArrangement.CURVED, prefs.readVisualCfg().singleRowDotArrangement)
    }


    @Test
    fun firstDualEnableDefaultsToBothSidesWithoutOverwritingSavedArrangement() {
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
}
