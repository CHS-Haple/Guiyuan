package com.chaners.guiyuan.xposed.prefs

import com.chaners.guiyuan.settings.FEATURE_AOD_KEY
import com.chaners.guiyuan.settings.FEATURE_CHANGED_AT_NS_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_KEY
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_KEY
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_DEFAULT
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_MIN
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_MAX
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.VisualTestPreferences
import com.chaners.guiyuan.settings.isFeatureKey
import com.chaners.guiyuan.settings.readFeatureCfg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturePrefsOwnerTest {
    @Test
    fun featureKeyFilterIncludesClearAndExcludesTimestamp() {
        assertTrue(isFeatureKey(null))
        assertTrue(isFeatureKey(FEATURE_ENABLED_KEY))
        assertTrue(isFeatureKey(FEATURE_KEYGUARD_KEY))
        assertTrue(isFeatureKey(FEATURE_AOD_KEY))
        assertTrue(isFeatureKey(LOW_BATTERY_THRESHOLD_KEY))
        assertFalse(isFeatureKey(FEATURE_CHANGED_AT_NS_KEY))
        assertFalse(isFeatureKey("unrelated"))
    }

    @Test
    fun defaultScenesAreEnabled() {
        val cfg = com.chaners.guiyuan.settings.FeatureCfg()
        assertEquals(true, cfg.enabled)
        assertEquals(true, cfg.keyguard)
        assertEquals(true, cfg.aod)
        assertEquals(LOW_BATTERY_THRESHOLD_DEFAULT, cfg.lowBatteryThreshold)
    }

    @Test
    fun missingFeaturePreferencesDecodeToPublishedDefaults() {
        val prefs = VisualTestPreferences()
        assertEquals(FeatureCfg(), prefs.readFeatureCfg())
    }

    @Test
    fun featureDecoderPreservesStoredFlagsAndValidatesLowBatteryThreshold() {
        val prefs = VisualTestPreferences()
        prefs.edit()
            .putBoolean(FEATURE_ENABLED_KEY, false)
            .putBoolean(FEATURE_KEYGUARD_KEY, false)
            .putBoolean(FEATURE_AOD_KEY, false)
            .putInt(LOW_BATTERY_THRESHOLD_KEY, LOW_BATTERY_THRESHOLD_MIN)
            .apply()

        assertEquals(
            FeatureCfg(
                enabled = false,
                keyguard = false,
                aod = false,
                lowBatteryThreshold = LOW_BATTERY_THRESHOLD_MIN,
            ),
            prefs.readFeatureCfg(),
        )

        prefs.edit().putInt(LOW_BATTERY_THRESHOLD_KEY, LOW_BATTERY_THRESHOLD_MAX).apply()
        assertEquals(LOW_BATTERY_THRESHOLD_MAX, prefs.readFeatureCfg().lowBatteryThreshold)

        prefs.edit().putInt(LOW_BATTERY_THRESHOLD_KEY, LOW_BATTERY_THRESHOLD_MAX + 1).apply()
        assertEquals(LOW_BATTERY_THRESHOLD_DEFAULT, prefs.readFeatureCfg().lowBatteryThreshold)
    }

    @Test
    fun validTimestampProducesLatency() {
        assertEquals(
            6_000_000L,
            FeaturePrefsOwner.transportLatencyNs(
                changedAtNs = 1_000_000_000L,
                receivedAtNs = 1_006_000_000L,
            ),
        )
    }

    @Test
    fun missingTimestampHasNoLatency() {
        assertNull(
            FeaturePrefsOwner.transportLatencyNs(
                changedAtNs = 0L,
                receivedAtNs = 1_006_000_000L,
            ),
        )
    }

    @Test
    fun futureTimestampHasNoLatency() {
        assertNull(
            FeaturePrefsOwner.transportLatencyNs(
                changedAtNs = 2_000_000_000L,
                receivedAtNs = 1_000_000_000L,
            ),
        )
    }
}
