package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.isCombinedStatusFeaturePreferenceKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeFeaturePreferencesOwnerTest {
    @Test
    fun clearNotificationParticipatesInFeatureRuntimeSync() {
        assertEquals(true, isCombinedStatusFeaturePreferenceKey(null))
        assertEquals(true, isCombinedStatusFeaturePreferenceKey("combined_status_enabled"))
        assertEquals(true, isCombinedStatusFeaturePreferenceKey("combined_status_keyguard_enabled"))
        assertEquals(true, isCombinedStatusFeaturePreferenceKey("combined_status_aod_enabled"))
        assertEquals(false, isCombinedStatusFeaturePreferenceKey("unrelated"))
    }

    @Test
    fun keyguardFeatureDefaultsFailNative() {
        val settings = com.chaners.guiyuan.settings.CombinedStatusFeatureSettings()
        assertEquals(true, settings.enabled)
        assertEquals(false, settings.keyguardEnabled)
        assertEquals(false, settings.aodEnabled)
    }

    @Test
    fun keyguardAndAodPreferencesRemainIndependentOfEachOtherAndMasterGate() {
        val keyguardOnly =
            com.chaners.guiyuan.settings.CombinedStatusFeatureSettings(
                enabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
            )
        val aodOnly =
            com.chaners.guiyuan.settings.CombinedStatusFeatureSettings(
                enabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
            )
        val masterDisabledWithChildrenPreserved =
            com.chaners.guiyuan.settings.CombinedStatusFeatureSettings(
                enabled = false,
                keyguardEnabled = true,
                aodEnabled = true,
            )

        assertEquals(true, keyguardOnly.keyguardEnabled)
        assertEquals(false, keyguardOnly.aodEnabled)
        assertEquals(false, aodOnly.keyguardEnabled)
        assertEquals(true, aodOnly.aodEnabled)
        assertEquals(false, masterDisabledWithChildrenPreserved.enabled)
        assertEquals(true, masterDisabledWithChildrenPreserved.keyguardEnabled)
        assertEquals(true, masterDisabledWithChildrenPreserved.aodEnabled)
    }

    @Test
    fun validCrossProcessTimestampProducesTransportLatency() {
        assertEquals(
            6_000_000L,
            RuntimeFeaturePreferencesOwner.resolveTransportLatencyNanos(
                changedAtElapsedRealtimeNanos = 1_000_000_000L,
                receivedAtElapsedRealtimeNanos = 1_006_000_000L,
            ),
        )
    }

    @Test
    fun missingTimestampDoesNotInventLatency() {
        assertNull(
            RuntimeFeaturePreferencesOwner.resolveTransportLatencyNanos(
                changedAtElapsedRealtimeNanos = 0L,
                receivedAtElapsedRealtimeNanos = 1_006_000_000L,
            ),
        )
    }

    @Test
    fun invalidFutureTimestampDoesNotInventLatency() {
        assertNull(
            RuntimeFeaturePreferencesOwner.resolveTransportLatencyNanos(
                changedAtElapsedRealtimeNanos = 2_000_000_000L,
                receivedAtElapsedRealtimeNanos = 1_000_000_000L,
            ),
        )
    }
}
