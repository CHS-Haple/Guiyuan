package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionGeometryTest {
    @Test
    fun rotationInvalidatesFrozenCcGeometryEvenWithSameNativeViewIdentities() {
        assertTrue(
            CcCarrierGeometryPolicy.sameNativeRootFrame(
                capturedWidth = 1440,
                capturedHeight = 3200,
                liveWidth = 1440,
                liveHeight = 3200,
            ),
        )
        // The original CC session matched only the identities of its Views,
        // so it could continue carrying portrait source frames into landscape.
        assertFalse(
            CcCarrierGeometryPolicy.sameNativeRootFrame(
                capturedWidth = 1440,
                capturedHeight = 3200,
                liveWidth = 3200,
                liveHeight = 1440,
            ),
        )
        assertFalse(
            CcCarrierGeometryPolicy.sameNativeRootFrame(
                capturedWidth = 3200,
                capturedHeight = 1440,
                liveWidth = 1440,
                liveHeight = 3200,
            ),
        )
        assertFalse(CcCarrierGeometryPolicy.sameNativeRootFrame(0, 0, 1440, 3200))
        assertTrue(CcCarrierGeometryPolicy.sameNativeRootFrame(3200, 1440, 3200, 1440))
    }

    @Test
    fun settledNativeFakeHostReflowRecapturesCachedCarrierOffset() {
        val policy = CcCarrierGeometryPolicy
        assertFalse(policy.shouldRecaptureFakeRootOffset(587, 1361, true))
        assertFalse(policy.shouldRecaptureFakeRootOffset(587, 0, false))
        assertFalse(policy.shouldRecaptureFakeRootOffset(587, 587, false))
        assertTrue(policy.shouldRecaptureFakeRootOffset(587, 1361, false))
        assertTrue(policy.shouldRecaptureFakeRootOffset(null, 587, false))
        assertTrue(policy.shouldRecaptureFakeRootOffset(1361, 587, false))

        val carrier = floatArrayOf(180f, 70f, 105f, 0f, 0f, 108f)
        val fakeRoot = floatArrayOf(100f, 70f, 587f, 0f, 0f, 108f)
        val stale = CcTransitionSpatialPolicy.rootAnchoredCarrier(
            carrier = carrier, root = fakeRoot, rootRelativeX = 30f,
        )
        val corrected = CcTransitionSpatialPolicy.rootAnchoredCarrier(
            carrier = carrier, root = fakeRoot,
            rootRelativeX = carrier[0] - fakeRoot[0],
        )
        assertEquals(130f, stale[0], 0.0001f)
        assertEquals(180f, corrected[0], 0.0001f)
    }

    @Test
    fun nativeWifiReflowUsesLastCoherentBatteryRelativeTarget() {
        val normalWifi = floatArrayOf(1195.5f, 240f, 75f, 0f, 0f, 75f)
        val battery = floatArrayOf(1330.5f, 240f, 135f, 0f, 0f, 169f)
        val provisionalWifi = floatArrayOf(1265.5f, 240f, 75f, 0f, 0f, 75f)
        assertTrue(CcTransitionSpatialPolicy.wifiBeforeBattery(normalWifi, battery, false))
        assertEquals(false, CcTransitionSpatialPolicy.wifiBeforeBattery(provisionalWifi, battery, false))
        val movedBattery = battery.copyOf().apply { this[0] += 5f }
        val carried = CcTransitionSpatialPolicy.followBatteryAnchor(
            normalWifi, battery, movedBattery,
        )
        assertEquals(1200.5f, carried[0], 0.0001f)
        assertTrue(CcTransitionSpatialPolicy.wifiBeforeBattery(carried, movedBattery, false))
        assertTrue(
            CcTransitionSpatialPolicy.wifiBeforeBattery(
                floatArrayOf(1300f, 240f, 75f, 0f, 0f, 75f),
                floatArrayOf(1120f, 240f, 135f, 0f, 0f, 169f),
                true,
            ),
        )
    }

    @Test
    fun mobileTypeHandoffComparesCurrentNativeGlyphNotDataSimOwnership() {
        val native5G = NativePresentationResolver.NetworkType(
            label = "5G",
            enhanced = false,
            source = NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
        )
        val native5GA = native5G.copy(label = "5GA")
        val nativePlus = native5G.copy(enhanced = true)

        assertEquals(false, CcDualMobilePolicy.mobileTypeMatches("5GA", false, native5G))
        assertEquals(true, CcDualMobilePolicy.mobileTypeMatches("5GA", false, native5GA))
        assertEquals(true, CcDualMobilePolicy.mobileTypeMatches("5G-A", false, native5GA))
        assertEquals(true, CcDualMobilePolicy.mobileTypeMatches("5G++", false, nativePlus))
        assertEquals(false, CcDualMobilePolicy.mobileTypeMatches("4G", false, native5G))
        assertNull(CcDualMobilePolicy.mobileTypeMatches("5GA", false, null))
    }

    @Test
    fun missingNativeEndpointUsesOneWholeComponentFadeCurve() {
        val policy = CcTransitionRevealPolicy
        assertEquals(1f, policy.unmatchedExitOpacity(0f), 0f)
        assertEquals(0.125f, policy.unmatchedExitOpacity(0.5f), 0.0001f)
        assertEquals(0f, policy.unmatchedExitOpacity(1f), 0f)
        assertEquals(1f, policy.unmatchedExitOpacity(Float.NaN), 0f)

        // No target geometry means the source keeps its native carrier basis.
        val source = floatArrayOf(120f, 50f, 24f, 0f, 0f, 30f)
        val carrier = floatArrayOf(100f, 50f, 80f, 0f, 0f, 40f)
        val moved = carrier.copyOf().apply { this[0] += 20f }
        val carried = CcCarrierGeometryPolicy.rebaseSourceToCurrentCarrier(source, carrier, moved)
        assertEquals(140f, carried[0], 0.0001f)
        assertEquals(24f, carried[2], 0.0001f)
        assertEquals(30f, carried[5], 0.0001f)
    }

    @Test
    fun missingTextDoesNotReverseBatteryRingRetraction() {
        val none = com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy.ExitDirection.NONE
        val left = com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy.ExitDirection.LEFT
        val right = com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy.ExitDirection.RIGHT

        assertEquals(
            left,
            CcTransitionPolicy.batteryRingExitDirection(
                liveCenterDirection = none,
                nativeBatteryIslandActive = false,
                targetRowRtl = false,
            ),
        )
        assertEquals(
            right,
            CcTransitionPolicy.batteryRingExitDirection(
                liveCenterDirection = none,
                nativeBatteryIslandActive = false,
                targetRowRtl = true,
            ),
        )
        assertEquals(
            right,
            CcTransitionPolicy.batteryRingExitDirection(
                liveCenterDirection = right,
                nativeBatteryIslandActive = false,
                targetRowRtl = false,
            ),
        )
        assertEquals(
            left,
            CcTransitionPolicy.batteryRingExitDirection(
                liveCenterDirection = right,
                nativeBatteryIslandActive = true,
                targetRowRtl = false,
            ),
        )
    }

    @Test
    fun fakeCapacityLeaseDoesNotChangeEndAnchoredMotionCarrierCenter() {
        val expandedCarrier =
            floatArrayOf(
                872f,
                129.5f,
                728f,
                0f,
                0f,
                169f,
            )
        val logicalCarrier =
            CcCarrierGeometryPolicy
                .endAnchoredMotionCarrierGeometry(
                    carrierGeometry = expandedCarrier,
                    carrierWidth = 728,
                    carrierHeight = 169,
                    logicalWidth = 478,
                    isRtl = false,
                )
        requireNotNull(logicalCarrier)

        assertEquals(997f, logicalCarrier[0], 0.0001f)
        assertEquals(478f, logicalCarrier[2], 0.0001f)

        val source = floatArrayOf(1240f, 55f, 105f, 0f, 0f, 108f)
        val sourceCarrier = floatArrayOf(997f, 54f, 478f, 0f, 0f, 108f)
        val carried =
            CcCarrierGeometryPolicy
                .rebaseSourceToCurrentCarrier(
                    source = source,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = logicalCarrier,
                )
        assertEquals(1240f, carried[0], 0.0001f)
    }

    @Test
    fun virtualMotionCarrierRetainsEndAnchorWhenNativeHostContracts() {
        val host = floatArrayOf(872f, 129.5f, 710f, 0f, 0f, 169f)
        val ltr = requireNotNull(
            CcCarrierGeometryPolicy.endAnchoredMotionCarrierGeometry(
                host, 710, 169, 1220, false,
            ),
        )
        val rtl = requireNotNull(
            CcCarrierGeometryPolicy.endAnchoredMotionCarrierGeometry(
                host, 710, 169, 1220, true,
            ),
        )
        assertEquals(617f, ltr[0], 0.0001f)
        assertEquals(1127f, rtl[0], 0.0001f)
        assertEquals(1220f, ltr[2], 0.0001f)
        assertEquals(1220f, rtl[2], 0.0001f)

        val sourceCarrier = floatArrayOf(2240f, 54f, 1220f, 0f, 0f, 108f)
        val source = floatArrayOf(2902.5f, 55f, 105f, 0f, 0f, 108f)
        val carried = CcCarrierGeometryPolicy.rebaseSourceToCurrentCarrier(
            source, sourceCarrier, ltr,
        )
        assertEquals(1279.5f, carried[0], 0.0001f)
        assertEquals(105f, carried[2], 0.0001f)
        assertNull(CcCarrierGeometryPolicy.endAnchoredSlotGeometry(host, 710, 169, 1220, false))
        assertNull(CcCarrierGeometryPolicy.endAnchoredSlotGeometry(host, 710, 169, 1220, true))
    }

    @Test
    fun fakeCapacityLeaseKeepsRtlMotionCarrierStartAnchored() {
        val expandedCarrier =
            floatArrayOf(
                872f,
                129.5f,
                728f,
                0f,
                0f,
                169f,
            )
        val logicalCarrier =
            CcCarrierGeometryPolicy
                .endAnchoredMotionCarrierGeometry(
                    carrierGeometry = expandedCarrier,
                    carrierWidth = 728,
                    carrierHeight = 169,
                    logicalWidth = 478,
                    isRtl = true,
                )
        requireNotNull(logicalCarrier)
        assertEquals(747f, logicalCarrier[0], 0.0001f)
        assertEquals(478f, logicalCarrier[2], 0.0001f)
    }

}
