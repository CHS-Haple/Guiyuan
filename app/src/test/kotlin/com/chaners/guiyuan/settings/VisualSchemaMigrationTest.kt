package com.chaners.guiyuan.settings

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VisualSchemaMigrationTest {
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


    @Test
    fun isolatedDotArrangementMigrationReportsFailedDiskCommit() {
        val prefs = MemoryPreferences()
        prefs.edit().putInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 1).apply()
        prefs.failNextCommit = true

        val failure = runCatching { migrateVisualPrefsIfNeeded(prefs) }.exceptionOrNull()

        assertEquals("Could not commit dot arrangement migration", failure?.message)
    }

    @Test
    fun isolatedSharedLayoutMigrationReportsFailedDiskCommit() {
        val prefs = MemoryPreferences()
        prefs.edit().putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4).apply()
        prefs.failNextCommit = true

        val failure = runCatching { migrateVisualPrefsIfNeeded(prefs) }.exceptionOrNull()

        assertEquals("Could not commit shared layout migration", failure?.message)
    }
}
