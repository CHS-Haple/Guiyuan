package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VisualLayoutMigrationTest {
    @Test
    fun migratingSingleModeDoesNotOverwriteItsSavedLayoutValues() {
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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
        val prefs = VisualTestPreferences()
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


    @Test
    fun isolatedDotArrangementMigrationReportsFailedDiskCommit() {
        val prefs = VisualTestPreferences()
        prefs.edit().putInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 1).apply()
        prefs.failNextCommit = true

        val failure = runCatching { migrateVisualPrefsIfNeeded(prefs) }.exceptionOrNull()

        assertEquals("Could not commit dot arrangement migration", failure?.message)
    }

    @Test
    fun isolatedSharedLayoutMigrationReportsFailedDiskCommit() {
        val prefs = VisualTestPreferences()
        prefs.edit().putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4).apply()
        prefs.failNextCommit = true

        val failure = runCatching { migrateVisualPrefsIfNeeded(prefs) }.exceptionOrNull()

        assertEquals("Could not commit shared layout migration", failure?.message)
    }
}
