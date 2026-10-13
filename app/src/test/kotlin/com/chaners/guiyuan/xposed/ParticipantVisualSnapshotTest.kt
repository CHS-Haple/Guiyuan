package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticipantVisualSnapshotTest {

    @Test
    fun aggregatedMobileTargetCanDistinguishFourBarsAboveFourDots() {
        val xs = listOf(0.12f, 0.35f, 0.58f, 0.81f)
        val top = xs.mapIndexed { i, x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.48f - i * 0.09f, x + 0.08f, 0.57f)
        }
        val bottom = xs.map { x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.70f, x + 0.08f, 0.78f)
        }
        val snapshot = ParticipantVisualSnapshot.Snapshot(
            envelope = ParticipantVisualSnapshot.NormalizedRect(0f, 0f, 1f, 1f),
            components = top + bottom,
            topology = ParticipantVisualSnapshot.Topology.COMPOSITE,
        )
        val resolved = snapshot.dualMobileRowsWithinEnvelope()
        assertEquals(ParticipantVisualSnapshot.DualBottomShape.DOTS, resolved?.bottomShape)
        assertEquals(4, resolved?.top?.size)
        assertEquals(4, resolved?.bottom?.size)
        assertEquals(0.70f, resolved?.bottom?.first()?.top ?: -1f, 0.0001f)
    }

    @Test
    fun aggregatedMobileTargetAlsoRecognizesTwoSeparateBarRows() {
        val xs = listOf(0.12f, 0.35f, 0.58f, 0.81f)
        val top = xs.mapIndexed { i, x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.40f - i * 0.07f, x + 0.08f, 0.49f)
        }
        val bottom = xs.mapIndexed { i, x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.78f - i * 0.06f, x + 0.08f, 0.90f)
        }
        val snapshot = ParticipantVisualSnapshot.Snapshot(
            envelope = ParticipantVisualSnapshot.NormalizedRect(0f, 0f, 1f, 1f),
            components = top + bottom,
            topology = ParticipantVisualSnapshot.Topology.COMPOSITE,
        )
        assertEquals(
            ParticipantVisualSnapshot.DualBottomShape.BARS,
            snapshot.dualMobileRowsWithinEnvelope()?.bottomShape,
        )
    }

    @Test
    fun uncertainAggregateShapeDoesNotClaimDualRowMorph() {
        val xs = listOf(0.12f, 0.35f, 0.58f, 0.81f)
        val bars = xs.mapIndexed { i, x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.46f - i * 0.07f, x + 0.08f, 0.55f)
        }
        val merged = xs.map { x ->
            ParticipantVisualSnapshot.NormalizedRect(x + 0.10f, 0.70f, x + 0.18f, 0.78f)
        }
        val snapshot = ParticipantVisualSnapshot.Snapshot(
            envelope = ParticipantVisualSnapshot.NormalizedRect(0f, 0f, 1f, 1f),
            components = bars + merged,
            topology = ParticipantVisualSnapshot.Topology.COMPOSITE,
        )
        assertEquals(null, snapshot.dualMobileRowsWithinEnvelope())
    }

    @Test
    fun alphaWeightedCenterTracksInkMassInsteadOfEnvelopeMidpoint() {
        val center =
            ParticipantVisualSnapshot.resolveAlphaWeightedCenter(
                pixels =
                    intArrayOf(
                        0xff000000.toInt(),
                        0x40000000,
                        0x00000000,
                    ),
                width = 1,
                height = 3,
            )

        requireNotNull(center)
        assertEquals(0.5f, center.x, 0.0001f)
        assertTrue(center.y < 1f / 3f)
        assertEquals(0.23354232f, center.y, 0.0001f)
    }

    @Test
    fun alphaWeightedCenterRejectsFullyTransparentInput() {
        val center =
            ParticipantVisualSnapshot.resolveAlphaWeightedCenter(
                pixels = IntArray(4),
                width = 2,
                height = 2,
            )

        assertNull(center)
    }
}
