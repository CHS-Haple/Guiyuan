package com.chaners.guiyuan.xposed.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val WIFI_RESOURCE_LEVEL_2 = "stat_sys_wifi_signal_2"
private const val WIFI_RESOURCE_LEVEL_3 = "stat_sys_wifi_signal_3"
private const val WIFI_RESOURCE_UNAVAILABLE_2 = "stat_sys_wifi_signal_unavailable_2"

class WifiOpticalReferenceTest {
    @Test
    fun connectedResourceKeepsItsOwnLevelReference() {
        assertEquals(
            WIFI_RESOURCE_LEVEL_3,
            WifiOpticalReference.connectedReferenceEntry(
                WIFI_RESOURCE_LEVEL_3,
            ),
        )
    }

    @Test
    fun unavailableResourceMapsToConnectedPeerAtSameLevel() {
        assertEquals(
            WIFI_RESOURCE_LEVEL_2,
            WifiOpticalReference.connectedReferenceEntry(
                WIFI_RESOURCE_UNAVAILABLE_2,
            ),
        )
    }

    @Test
    fun hotspotResourceMapsToConnectedPeerAtSameLevel() {
        assertEquals(
            "stat_sys_wifi_signal_1",
            WifiOpticalReference.connectedReferenceEntry(
                "stat_sys_hotspot_signal_1",
            ),
        )
    }

    @Test
    fun qualifiedUnavailableResourceMapsToConnectedPeer() {
        assertEquals(
            WIFI_RESOURCE_LEVEL_3,
            WifiOpticalReference.connectedReferenceEntry(
                "com.android.systemui:drawable/stat_sys_wifi_signal_unavailable_3",
            ),
        )
    }

    @Test
    fun tintLikeConnectedVariantMapsBackToBaseReference() {
        assertEquals(
            WIFI_RESOURCE_LEVEL_2,
            WifiOpticalReference.connectedReferenceEntry(
                "stat_sys_wifi_signal_2_tint",
            ),
        )
    }

    @Test
    fun nonWifiResourceHasNoReference() {
        assertNull(
            WifiOpticalReference.connectedReferenceEntry(
                "stat_sys_signal_4",
            ),
        )
    }

    @Test
    fun nativeHotspotFitsItsOwnOpticalBounds() {
        assertNull(WifiOpticalReference.fitReferenceEntry("stat_sys_hotspot_signal_2"))
        assertNull(
            WifiOpticalReference.fitReferenceEntry(
                "com.android.systemui:drawable/stat_sys_hotspot_signal_3_tint",
            ),
        )
        assertEquals(
            WIFI_RESOURCE_LEVEL_2,
            WifiOpticalReference.fitReferenceEntry(WIFI_RESOURCE_LEVEL_2),
        )
        assertEquals(
            WIFI_RESOURCE_LEVEL_2,
            WifiOpticalReference.fitReferenceEntry(WIFI_RESOURCE_UNAVAILABLE_2),
        )
        assertNull(WifiOpticalReference.fitReferenceEntry(null))
    }

    @Test
    fun referenceViewportMustMatchExactly() {
        assertTrue(
            WifiOpticalReference.canShareReferenceViewport(
                currentWidth = 24,
                currentHeight = 24,
                referenceWidth = 24,
                referenceHeight = 24,
            ),
        )
        assertFalse(
            WifiOpticalReference.canShareReferenceViewport(
                currentWidth = 24,
                currentHeight = 20,
                referenceWidth = 24,
                referenceHeight = 24,
            ),
        )
    }
}
