package com.chaners.guiyuan.xposed.prefs

import com.chaners.guiyuan.settings.FEATURE_AOD_KEY
import com.chaners.guiyuan.settings.FEATURE_CHANGED_AT_NS_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_KEY
import com.chaners.guiyuan.settings.isFeatureKey
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
        assertFalse(isFeatureKey(FEATURE_CHANGED_AT_NS_KEY))
        assertFalse(isFeatureKey("unrelated"))
    }

    @Test
    fun defaultScenesAreEnabled() {
        val cfg = com.chaners.guiyuan.settings.FeatureCfg()
        assertEquals(true, cfg.enabled)
        assertEquals(true, cfg.keyguard)
        assertEquals(true, cfg.aod)
    }

    @Test
    fun keyguardAndAodStayIndependent() {
        val keyguard =
            com.chaners.guiyuan.settings.FeatureCfg(
                enabled = true,
                keyguard = true,
                aod = false,
            )
        val aod =
            com.chaners.guiyuan.settings.FeatureCfg(
                enabled = true,
                keyguard = false,
                aod = true,
            )
        val masterOff =
            com.chaners.guiyuan.settings.FeatureCfg(
                enabled = false,
                keyguard = true,
                aod = true,
            )

        assertEquals(true, keyguard.keyguard)
        assertEquals(false, keyguard.aod)
        assertEquals(false, aod.keyguard)
        assertEquals(true, aod.aod)
        assertEquals(false, masterOff.enabled)
        assertEquals(true, masterOff.keyguard)
        assertEquals(true, masterOff.aod)
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
