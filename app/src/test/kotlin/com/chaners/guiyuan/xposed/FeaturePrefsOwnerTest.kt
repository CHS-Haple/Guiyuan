package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.isFeatureKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeaturePrefsOwnerTest {
    @Test
    fun clearNotificationParticipatesInFeatureRuntimeSync() {
        assertEquals(true, isFeatureKey(null))
        assertEquals(true, isFeatureKey("combined_status_enabled"))
        assertEquals(true, isFeatureKey("combined_status_keyguard_enabled"))
        assertEquals(true, isFeatureKey("combined_status_aod_enabled"))
        assertEquals(false, isFeatureKey("unrelated"))
    }

    @Test
    fun defaultsFailNative() {
        val cfg = com.chaners.guiyuan.settings.FeatureCfg()
        assertEquals(true, cfg.enabled)
        assertEquals(false, cfg.keyguard)
        assertEquals(false, cfg.aod)
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
