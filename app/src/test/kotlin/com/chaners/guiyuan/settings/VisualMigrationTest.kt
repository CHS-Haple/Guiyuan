package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualMigrationTest {
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
    fun legacyVisualMigrationRejectsUnsupportedValueTypesWithoutPublishingSchema() {
        val layout = ContentLayout.NETWORK_CENTER
        for (activeDual in listOf(false, true)) {
            val prefs = MemoryPreferences()
            val sourceKey =
                if (activeDual) {
                    visualModeKey(true, visualProfileKey(layout, WIFI_SIZE_SCALE_KEY))
                } else {
                    WIFI_SIZE_SCALE_KEY
                }
            prefs.edit()
                .putBoolean(DUAL_SIGNAL_ENABLED_KEY, activeDual)
                .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
                .putLong(sourceKey, 42L)
                .apply()
            val before = prefs.all.toMap()

            val failure =
                runCatching { migrateSharedLayoutVisualsIfNeeded(prefs) }.exceptionOrNull()

            assertEquals("Unexpected saved visual preference type: $WIFI_SIZE_SCALE_KEY", failure?.message)
            assertEquals(before, prefs.all)
            assertFalse(prefs.contains(SHARED_LAYOUT_VISUAL_SCHEMA_KEY))
        }
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
}
