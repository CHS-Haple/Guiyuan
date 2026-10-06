package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiOpticalPolicyTest {
    @Test
    fun connectedKeepsLevelRef() {
        assertEquals(
            "stat_sys_wifi_signal_3",
            WifiOpticalPolicy.connectedReferenceEntry(
                "stat_sys_wifi_signal_3",
            ),
        )
    }

    @Test
    fun unavailableMapsToConnectedPeer() {
        assertEquals(
            "stat_sys_wifi_signal_2",
            WifiOpticalPolicy.connectedReferenceEntry(
                "stat_sys_wifi_signal_unavailable_2",
            ),
        )
    }

    @Test
    fun hotspotMapsToConnectedPeer() {
        assertEquals(
            "stat_sys_wifi_signal_1",
            WifiOpticalPolicy.connectedReferenceEntry(
                "stat_sys_hotspot_signal_1",
            ),
        )
    }

    @Test
    fun qualifiedUnavailableMapsToPeer() {
        assertEquals(
            "stat_sys_wifi_signal_3",
            WifiOpticalPolicy.connectedReferenceEntry(
                "com.android.systemui:drawable/stat_sys_wifi_signal_unavailable_3",
            ),
        )
    }

    @Test
    fun tintVariantMapsToBaseRef() {
        assertEquals(
            "stat_sys_wifi_signal_2",
            WifiOpticalPolicy.connectedReferenceEntry(
                "stat_sys_wifi_signal_2_tint",
            ),
        )
    }

    @Test
    fun nonWifiResourceHasNoReference() {
        assertNull(
            WifiOpticalPolicy.connectedReferenceEntry(
                "stat_sys_signal_4",
            ),
        )
    }

    @Test
    fun referenceViewportMustMatchExactly() {
        assertTrue(
            WifiOpticalPolicy.canShareReferenceViewport(
                currentWidth = 24,
                currentHeight = 24,
                referenceWidth = 24,
                referenceHeight = 24,
            ),
        )
        assertFalse(
            WifiOpticalPolicy.canShareReferenceViewport(
                currentWidth = 24,
                currentHeight = 20,
                referenceWidth = 24,
                referenceHeight = 24,
            ),
        )
    }
}
