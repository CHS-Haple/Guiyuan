package com.chaners.guiyuan.xposed.prefs

import com.chaners.guiyuan.settings.FEATURE_AOD_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_KEY
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.VisualTestPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The feature owner is a separate SystemUI listener generation, not a new config writer.
 * These tests cover callbacks/retirement absent from the feature decoder tests.
 */
class FeaturePrefsOwnerLifecycleTest {
    @After
    fun tearDown() {
        FeaturePrefsOwner.unbind()
    }

    @Test
    fun bindingDeliversChangedSnapshotsWithoutDuplicateOrUnrelatedCallbacks() {
        val prefs = VisualTestPreferences()
        val observed = mutableListOf<Pair<FeatureCfg, Long?>>()
        FeaturePrefsOwner.bind(prefs) { cfg, latency -> observed += cfg to latency }

        prefs.edit().putBoolean(FEATURE_ENABLED_KEY, false).apply()
        prefs.notifyPreferenceChanged(FEATURE_ENABLED_KEY)
        prefs.edit().putString("unrelated", "changed").apply()

        assertEquals(
            listOf(FeatureCfg() to null, FeatureCfg(enabled = false) to null),
            observed,
        )
        assertEquals(FeatureCfg(enabled = false), FeaturePrefsOwner.current())
        assertEquals(1, prefs.listenerCount)
    }

    @Test
    fun rebindRetiresOldGenerationAndIgnoresItsLateCallback() {
        val old = VisualTestPreferences()
        val next = VisualTestPreferences()
        old.edit().putBoolean(FEATURE_ENABLED_KEY, false).apply()
        next.edit().putBoolean(FEATURE_KEYGUARD_KEY, false).apply()
        val observed = mutableListOf<FeatureCfg>()

        FeaturePrefsOwner.bind(old) { cfg, _ -> observed += cfg }
        val stale = old.lastRegisteredListener
        FeaturePrefsOwner.bind(next) { cfg, _ -> observed += cfg }

        old.edit().putBoolean(FEATURE_ENABLED_KEY, true).apply()
        stale?.onSharedPreferenceChanged(old, FEATURE_ENABLED_KEY)

        assertEquals(0, old.listenerCount)
        assertEquals(1, next.listenerCount)
        assertEquals(
            listOf(FeatureCfg(enabled = false), FeatureCfg(keyguard = false)),
            observed,
        )
        assertEquals(FeatureCfg(keyguard = false), FeaturePrefsOwner.current())
    }

    @Test
    fun failedInitialDeliveryRetiresListenerAndRestoresDisabledFallback() {
        val prefs = VisualTestPreferences()
        val failure = IllegalStateException("initial-feature-delivery")

        assertSame(
            failure,
            runCatching {
                FeaturePrefsOwner.bind(prefs) { _, _ -> throw failure }
            }.exceptionOrNull(),
        )
        assertEquals(0, prefs.listenerCount)
        assertEquals(
            FeatureCfg(enabled = false, keyguard = false, aod = false),
            FeaturePrefsOwner.current(),
        )
    }

    @Test
    fun failedUnregisterKeepsRetirementPendingAndBlocksNextBinding() {
        val old = VisualTestPreferences()
        val next = VisualTestPreferences()
        next.edit().putBoolean(FEATURE_AOD_KEY, false).apply()
        val observed = mutableListOf<FeatureCfg>()
        FeaturePrefsOwner.bind(old) { cfg, _ -> observed += cfg }

        val failure = IllegalStateException("unregister-feature")
        old.unregisterFailure = failure
        try {
            assertSame(failure, runCatching { FeaturePrefsOwner.unbind() }.exceptionOrNull())
            assertEquals(1, old.listenerCount)
            assertSame(
                failure,
                runCatching {
                    FeaturePrefsOwner.bind(next) { cfg, _ -> observed += cfg }
                }.exceptionOrNull(),
            )
            assertEquals(0, next.listenerCount)
        } finally {
            old.unregisterFailure = null
        }

        FeaturePrefsOwner.bind(next) { cfg, _ -> observed += cfg }
        assertEquals(0, old.listenerCount)
        assertEquals(1, next.listenerCount)
        assertEquals(listOf(FeatureCfg(), FeatureCfg(aod = false)), observed)
    }

    @Test
    fun failureFromRetiredCallbackCannotUndoNewGeneration() {
        val old = VisualTestPreferences()
        val next = VisualTestPreferences()
        next.edit().putBoolean(FEATURE_KEYGUARD_KEY, false).apply()
        val failure = IllegalStateException("old-generation-delivery")

        FeaturePrefsOwner.bind(old) { cfg, _ ->
            if (!cfg.enabled) {
                FeaturePrefsOwner.bind(next) { _, _ -> }
                throw failure
            }
        }
        assertSame(
            failure,
            runCatching {
                old.edit().putBoolean(FEATURE_ENABLED_KEY, false).apply()
            }.exceptionOrNull(),
        )
        assertEquals(0, old.listenerCount)
        assertEquals(1, next.listenerCount)
        assertEquals(FeatureCfg(keyguard = false), FeaturePrefsOwner.current())
    }
}
