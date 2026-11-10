package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionNativeCapacityTest {
@Test
    fun chargingPeerReservationHoldsThenConvergesToLiveNativeEndpoint() {
        val compact = 105
        val unplugged = listOf(
            CcReservationPolicy.ReservationSpan(
                sourceLeft = -105f, sourceRight = 0f,
                targetLeft = -220f, targetRight = 0f,
            ),
        )
        val plugged = listOf(
            CcReservationPolicy.ReservationSpan(
                sourceLeft = -105f, sourceRight = 0f,
                targetLeft = -250f, targetRight = 0f,
            ),
        )
        fun width(spans: List<CcReservationPolicy.ReservationSpan>, p: Float) =
            CcReservationPolicy.resolveTransitionReservationWidth(
                compactWidthPx = compact, spans = spans, progress = p,
            )
        val held = 0.6f
        val oldWidth = width(unplugged, held)
        val newWidth = width(plugged, held)
        val shift = CcTransitionPolicy.retargetShift(
            priorShift = 0f,
            priorAnchor = null,
            progress = held,
            nativeDelta = (newWidth - oldWidth).toFloat(),
        )
        assertEquals(oldWidth, (newWidth + shift).toInt())

        // Retargeting from the same held fraction in the reverse direction
        // restores the earlier gap without accumulating another displacement.
        val restored = CcTransitionPolicy.retargetShift(
            priorShift = shift,
            priorAnchor = held,
            progress = held,
            nativeDelta = (oldWidth - newWidth).toFloat(),
        )
        assertEquals(0f, restored, 0.0001f)

        // The original compact endpoint and the new fully-expanded native
        // endpoint remain authoritative, regardless of the starting charge.
        assertEquals(compact, width(plugged, 0f))
        assertEquals(0f, CcTransitionPolicy.retargetWeight(0f, held), 0.0001f)
        assertEquals(0f, CcTransitionPolicy.retargetWeight(1f, held), 0.0001f)
        val nearEnd = 0.98f
        val visual = width(plugged, nearEnd) +
            (shift * CcTransitionPolicy.retargetWeight(nearEnd, held))
                .toInt()
        assertTrue(kotlin.math.abs(visual - width(plugged, 1f)) < 10)
    }

    @Test
    fun transitionReservationInterpolatesTotalWidthFromNativeProgress() {
        val spans =
            listOf(
                CcReservationPolicy.ReservationSpan(
                    sourceLeft = -10f,
                    sourceRight = 0f,
                    targetLeft = -30f,
                    targetRight = 0f,
                ),
            )

        assertEquals(
            10,
            CcTransitionPolicy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 0f,
                ),
        )
        assertEquals(
            20,
            CcTransitionPolicy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 0.5f,
                ),
        )
        assertEquals(
            30,
            CcTransitionPolicy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 1f,
                ),
        )
    }

    @Test
    fun totalWidthInterpolationAvoidsOverlappingSpanDeadZone() {
        val spans =
            listOf(
                CcReservationPolicy.ReservationSpan(
                    sourceLeft = 0f,
                    sourceRight = 0f,
                    targetLeft = -180f,
                    targetRight = -105f,
                ),
            )

        val oldGeometryUnion =
            CcReservationPolicy.resolveReservationWidth(
                compactWidthPx = 105,
                spans = spans,
                progress = 0.25f,
            )
        val transitionWidth =
            CcTransitionPolicy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    progress = 0.25f,
                )

        assertEquals(105, oldGeometryUnion)
        assertTrue(transitionWidth > 105)
        assertTrue(transitionWidth < 180)
    }

    @Test
    fun batteryIslandNativePeerReservationDoesNotPreReserveLatentGap() {
        val spans =
            listOf(
                CcReservationPolicy.ReservationSpan(
                    sourceLeft = 0f,
                    sourceRight = 0f,
                    targetLeft = -180f,
                    targetRight = -105f,
                ),
            )
        val semantic =
            CcTransitionPolicy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    progress = 0.25f,
                )
        val native =
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = semantic,
                    progress = 0.25f,
                )

        assertTrue(semantic > 105)
        assertEquals(105, native)
    }

    @Test
    fun batteryIslandNativePeerReservationProjectsTargetIntoCurrentFakeEndFrame() {
        val spans =
            listOf(
                CcReservationPolicy.ReservationSpan(
                    sourceLeft = -105f,
                    sourceRight = 0f,
                    targetLeft = -240f,
                    targetRight = -135f,
                ),
            )

        val unprojected =
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 220,
                    progress = 0.5f,
                )
        val projected =
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 220,
                    progress = 0.5f,
                    targetEndOffsetPx = 120f,
                )

        assertEquals(173, unprojected)
        assertEquals(113, projected)
    }

    @Test
    fun batteryIslandNativePeerReservationIgnoresEndSideProjection() {
        val spans =
            listOf(
                CcReservationPolicy.ReservationSpan(
                    sourceLeft = -40f,
                    sourceRight = 0f,
                    targetLeft = -140f,
                    targetRight = 0f,
                ),
            )

        assertEquals(
            80,
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 40,
                    spans = spans,
                    semanticWidthPx = 140,
                    progress = 1f,
                    targetEndOffsetPx = 60f,
                ),
        )
    }

    @Test
    fun batteryIslandNativePeerReservationFollowsCurrentSpanAndConverges() {
        val spans =
            listOf(
                CcReservationPolicy.ReservationSpan(
                    sourceLeft = 0f,
                    sourceRight = 0f,
                    targetLeft = -180f,
                    targetRight = -105f,
                ),
            )

        assertEquals(
            135,
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 161,
                    progress = 0.75f,
                ),
        )
        assertEquals(
            180,
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 180,
                    progress = 1f,
                ),
        )
        assertEquals(
            150,
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans =
                        listOf(
                            CcReservationPolicy.ReservationSpan(
                                sourceLeft = -105f,
                                sourceRight = 0f,
                                targetLeft = -220f,
                                targetRight = 0f,
                            ),
                        ),
                    semanticWidthPx = 150,
                    progress = 1f,
                ),
        )
    }
}
