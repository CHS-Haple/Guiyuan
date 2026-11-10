package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.InternetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionSourceContractTest {
    @Test
    fun liveSourceChangesInvalidateHeldReservation() {
        val airplane = RenderModel(
            batteryPercent = 63,
            charging = false,
            centerIndicator = CenterIndicator.Airplane,
            mobileLevel = null,
            effectiveDataSubscriptionId = 4,
        )
        val wifi = airplane.copy(
            centerIndicator = CenterIndicator.Wifi(3, InternetState.VALIDATED),
        )
        fun changed(model: RenderModel, visualChanged: Boolean = false) =
            CcReservationPolicy.reservationSourceChanged(airplane, model, visualChanged)

        assertTrue(changed(wifi))
        assertTrue(CcReservationPolicy.reservationSourceChanged(wifi, airplane, false))
        assertTrue(
            CcReservationPolicy.reservationSourceChanged(
                wifi,
                wifi.copy(centerIndicator = CenterIndicator.Wifi(1, InternetState.NO_INTERNET)),
                false,
            ),
        )
        assertTrue(changed(airplane.copy(centerIndicator = CenterIndicator.Empty)))
        assertTrue(changed(airplane.copy(mobileLevel = 2)))
        assertTrue(changed(airplane.copy(mobileUnavailableMark = true)))
        assertTrue(changed(airplane.copy(effectiveDataSubscriptionId = 1)))
        assertTrue(changed(airplane.copy(dualMobileLevels = DualMobileLevels(3, 4))))
        assertTrue(changed(airplane.copy(batteryPercent = 64)))
        assertTrue(changed(airplane.copy(charging = true, chargingIconResId = 123)))
        assertTrue(changed(airplane, visualChanged = true))
        assertFalse(changed(airplane))
        assertFalse(changed(airplane.copy(batterySystemSemanticColor = 0xff00ff00.toInt())))
    }

    @Test
    fun sourceRetargetKeepsHeldReservationContinuous() {
        val compact = 100
        val previous = listOf(CcReservationPolicy.ReservationSpan(-20f, -10f, -150f, -130f))
        val current = listOf(CcReservationPolicy.ReservationSpan(-20f, -10f, -210f, -190f))
        val held = 0.5f
        val before = CcReservationPolicy.resolveTransitionReservationWidth(compact, previous, held)
        val after = CcReservationPolicy.resolveTransitionReservationWidth(compact, current, held)
        val shift = CcTransitionPolicy.retargetShift(0f, null, held, (after - before).toFloat())

        assertEquals(
            before.toFloat(),
            after + shift * CcTransitionPolicy.retargetWeight(held, held),
            0.001f,
        )
        assertEquals(0f, shift * CcTransitionPolicy.retargetWeight(1f, held), 0.001f)
    }

    @Test
    fun dualSignalTargetNeedsVerifiedAggregateOrTwoMatchingSubscriptions() {
        val policy = CcTransitionPolicy
        val active = setOf(1, 4)
        assertEquals(
            CcDualMobilePolicy.DualMobileTarget.AGGREGATED,
            policy.dualMobileTarget(active, listOf(null), aggregateProven = true),
        )
        assertEquals(
            CcDualMobilePolicy.DualMobileTarget.AGGREGATED,
            policy.dualMobileTarget(active, listOf(4), aggregateProven = true),
        )
        assertEquals(
            CcDualMobilePolicy.DualMobileTarget.NONE,
            policy.dualMobileTarget(active, listOf(7), aggregateProven = true),
        )
        assertEquals(
            CcDualMobilePolicy.DualMobileTarget.NONE,
            policy.dualMobileTarget(active, listOf(1), aggregateProven = false),
        )
        assertEquals(
            CcDualMobilePolicy.DualMobileTarget.SEPARATE,
            policy.dualMobileTarget(active, listOf(4, 1), aggregateProven = false),
        )
        listOf(listOf(1, 1), listOf(1, null), listOf(1, 7), emptyList(), listOf(1, 4, 7))
            .forEach { ids ->
                assertEquals(
                    CcDualMobilePolicy.DualMobileTarget.NONE,
                    policy.dualMobileTarget(active, ids, aggregateProven = true),
                )
            }
        assertEquals(
            CcDualMobilePolicy.DualMobileTarget.NONE,
            policy.dualMobileTarget(setOf(1), listOf(null), aggregateProven = true),
        )
    }

    @Test
    fun physicalSimSlotIdentityMapsTwoNativeRootsRegardlessOfViewOrder() {
        val slots = mapOf(0 to 4, 1 to 1)
        val separate = NativePresentationResolver.Mode.DUAL_SEPARATE
        assertEquals(
            listOf(1, 0),
            CcDualMobilePolicy.dualRootRowIndices(separate, slots, listOf(1, 4)),
        )
        assertEquals(
            listOf(0, 1),
            CcDualMobilePolicy.dualRootRowIndices(separate, slots, listOf(4, 1)),
        )
        for (ids in listOf(listOf(1, 1), listOf(1, null), listOf(1, 7))) {
            assertEquals(
                null,
                CcDualMobilePolicy.dualRootRowIndices(separate, slots, ids),
            )
        }
        assertEquals(
            null,
            CcDualMobilePolicy.dualRootRowIndices(
                NativePresentationResolver.Mode.DUAL_AGGREGATED,
                slots,
                listOf(1, 4),
            ),
        )
        assertEquals(
            null,
            CcDualMobilePolicy.dualRootRowIndices(
                separate,
                mapOf(0 to 4),
                listOf(1, 4),
            ),
        )
    }

    @Test
    fun dualNativeGroupUsesBothTargetsWithoutGuessingRows() {
        val first = floatArrayOf(10f, 20f, 8f, 0f, 0f, 6f)
        val second = floatArrayOf(30f, 20f, 8f, 0f, 0f, 6f)
        val merged = CcDualMobilePolicy.dualMobileGroupGeometry(listOf(first, second))
        val bounds = requireNotNull(merged)
        listOf(20f, 20f, 28f, 0f, 0f, 6f).forEachIndexed { index, value ->
            assertEquals(value, bounds[index], 0.0001f)
        }
        assertTrue(CcDualMobilePolicy.dualMobileGroupGeometry(listOf(first))!!.contentEquals(first))
        assertEquals(null, CcDualMobilePolicy.dualMobileGroupGeometry(listOf(first, second, first)))
        assertEquals(
            null,
            CcDualMobilePolicy.dualMobileGroupGeometry(
                listOf(first, second.copyOf().also { it[3] = 6f }),
            ),
        )
        val mirrored = CcDualMobilePolicy.dualMobileGroupGeometry(
            listOf(
                first.copyOf().also { it[2] = -8f },
                second.copyOf().also { it[2] = -8f },
            ),
        )
        assertEquals(-28f, requireNotNull(mirrored)[2], 0.0001f)
        assertEquals(
            null,
            CcDualMobilePolicy.dualMobileGroupGeometry(
                listOf(first, floatArrayOf(Float.NaN, 2f, 8f, 0f, 0f, 6f)),
            ),
        )
    }
}
