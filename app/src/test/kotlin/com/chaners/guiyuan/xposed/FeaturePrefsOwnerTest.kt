package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.isFeaturePreferenceKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeaturePrefsOwnerTest {
    @Test
    fun clearNotificationSyncsFeature() {
        assertEquals(true, isFeaturePreferenceKey(null))
        assertEquals(true, isFeaturePreferenceKey("combined_status_enabled"))
        assertEquals(true, isFeaturePreferenceKey("combined_status_keyguard_enabled"))
        assertEquals(true, isFeaturePreferenceKey("combined_status_aod_enabled"))
        assertEquals(false, isFeaturePreferenceKey("unrelated"))
    }

    @Test
    fun keyguardFeatureDefaultsFailNative() {
        val settings = com.chaners.guiyuan.settings.FeatureSettings()
        assertEquals(true, settings.enabled)
        assertEquals(false, settings.keyguardEnabled)
        assertEquals(false, settings.aodEnabled)
    }

    @Test
    fun keyguardAndAodPrefsStayIndependent() {
        val keyguardOnly =
            com.chaners.guiyuan.settings.FeatureSettings(
                enabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
            )
        val aodOnly =
            com.chaners.guiyuan.settings.FeatureSettings(
                enabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
            )
        val masterDisabledWithChildrenPreserved =
            com.chaners.guiyuan.settings.FeatureSettings(
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
    fun validTimestampProducesLatency() {
        assertEquals(
            6_000_000L,
            FeaturePrefsOwner.resolveTransportLatencyNanos(
                changedAtElapsedRealtimeNanos = 1_000_000_000L,
                receivedAtElapsedRealtimeNanos = 1_006_000_000L,
            ),
        )
    }

    @Test
    fun missingTimestampDoesNotInventLatency() {
        assertNull(
            FeaturePrefsOwner.resolveTransportLatencyNanos(
                changedAtElapsedRealtimeNanos = 0L,
                receivedAtElapsedRealtimeNanos = 1_006_000_000L,
            ),
        )
    }

    @Test
    fun futureTimestampHasNoLatency() {
        assertNull(
            FeaturePrefsOwner.resolveTransportLatencyNanos(
                changedAtElapsedRealtimeNanos = 2_000_000_000L,
                receivedAtElapsedRealtimeNanos = 1_000_000_000L,
            ),
        )
    }
}
