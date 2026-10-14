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
        assertEquals(ParticipantVisualSnapshot.RowShape.BARS, resolved?.topShape)
        assertEquals(ParticipantVisualSnapshot.RowShape.DOTS, resolved?.bottomShape)
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
        val resolved = snapshot.dualMobileRowsWithinEnvelope()
        assertEquals(ParticipantVisualSnapshot.RowShape.BARS, resolved?.topShape)
        assertEquals(ParticipantVisualSnapshot.RowShape.BARS, resolved?.bottomShape)
    }

    @Test
    fun recognizesOtherModulesWithDotsOnTopAndBarsBelow() {
        val xs = listOf(0.12f, 0.35f, 0.58f, 0.81f)
        val dots = xs.map { x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.17f, x + 0.08f, 0.25f)
        }
        val bars = xs.mapIndexed { i, x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.70f - i * 0.07f, x + 0.08f, 0.79f)
        }
        val resolved = ParticipantVisualSnapshot.Snapshot(
            envelope = ParticipantVisualSnapshot.NormalizedRect(0f, 0f, 1f, 1f),
            components = dots + bars,
            topology = ParticipantVisualSnapshot.Topology.COMPOSITE,
        ).dualMobileRowsWithinEnvelope()
        assertEquals(ParticipantVisualSnapshot.RowShape.DOTS, resolved?.topShape)
        assertEquals(ParticipantVisualSnapshot.RowShape.BARS, resolved?.bottomShape)
    }

    @Test
    fun twoDotRowsAreNeverMistakenForSignalBars() {
        val xs = listOf(0.12f, 0.35f, 0.58f, 0.81f)
        val dots = xs.map { x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.12f, x + 0.08f, 0.20f)
        }
        val bottom = dots.map { it.copy(top = 0.65f, bottom = 0.73f) }
        val resolved = ParticipantVisualSnapshot.Snapshot(
            envelope = ParticipantVisualSnapshot.NormalizedRect(0f, 0f, 1f, 1f),
            components = dots + bottom,
            topology = ParticipantVisualSnapshot.Topology.COMPOSITE,
        ).dualMobileRowsWithinEnvelope()
        assertEquals(ParticipantVisualSnapshot.RowShape.DOTS, resolved?.topShape)
        assertEquals(ParticipantVisualSnapshot.RowShape.DOTS, resolved?.bottomShape)
    }

    @Test
    fun overlappingSignalColumnsRejectIndependentDualRows() {
        val envelope = ParticipantVisualSnapshot.NormalizedRect(0f, 0f, 1f, 1f)
        val overlapCases = listOf(
            listOf(0.12f, 0.18f, 0.58f, 0.81f),
            listOf(0.12f, 0.35f, 0.39f, 0.81f),
        )
        for (xs in overlapCases) {
            val top = xs.map { x ->
                ParticipantVisualSnapshot.NormalizedRect(x, 0.15f, x + 0.08f, 0.23f)
            }
            val bottom = xs.map { x ->
                ParticipantVisualSnapshot.NormalizedRect(x, 0.65f, x + 0.08f, 0.73f)
            }
            val snapshot = ParticipantVisualSnapshot.Snapshot(
                envelope = envelope,
                components = top + bottom,
                topology = ParticipantVisualSnapshot.Topology.COMPOSITE,
            )
            assertNull(snapshot.dualMobileRowsWithinEnvelope())
        }
    }

    @Test
    fun unknownMixedRowReturnsNullInsteadOfGuessingBars() {
        val xs = listOf(0.12f, 0.35f, 0.58f, 0.81f)
        val bars = xs.mapIndexed { i, x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.38f - i * 0.04f, x + 0.08f, 0.47f)
        }
        val mixed = xs.mapIndexed { i, x ->
            ParticipantVisualSnapshot.NormalizedRect(
                x, 0.81f - i * 0.035f, x + 0.08f, 0.87f,
            )
        }
        assertNull(
            ParticipantVisualSnapshot.Snapshot(
                envelope = ParticipantVisualSnapshot.NormalizedRect(0f, 0f, 1f, 1f),
                components = bars + mixed,
                topology = ParticipantVisualSnapshot.Topology.COMPOSITE,
            ).dualMobileRowsWithinEnvelope(),
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
    fun fourEqualDotsAreNotNativeSignalBars() {
        val xs = listOf(0.12f, 0.35f, 0.58f, 0.81f)
        val dots = xs.map { x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.65f, x + 0.08f, 0.73f)
        }
        val topology = ParticipantVisualSnapshot.classifyComponents(dots)
        assertEquals(ParticipantVisualSnapshot.Topology.COMPOSITE, topology)
        assertNull(
            ParticipantVisualSnapshot.Snapshot(
                envelope = ParticipantVisualSnapshot.NormalizedRect(0f, 0f, 1f, 1f),
                components = dots,
                topology = topology,
            ).fourVerticalBarsWithinEnvelope(),
        )
    }

    @Test
    fun nativeFourBarsRequireDistinctHorizontalColumns() {
        fun bars(xs: List<Float>) = xs.mapIndexed { index, x ->
            ParticipantVisualSnapshot.NormalizedRect(
                left = x,
                top = 0.65f - index * 0.11f,
                right = x + 0.08f,
                bottom = 0.80f,
            )
        }
        assertEquals(
            ParticipantVisualSnapshot.Topology.FOUR_VERTICAL_BARS,
            ParticipantVisualSnapshot.classifyComponents(
                bars(listOf(0.12f, 0.35f, 0.58f, 0.81f)),
            ),
        )
        assertEquals(
            ParticipantVisualSnapshot.Topology.COMPOSITE,
            ParticipantVisualSnapshot.classifyComponents(
                bars(listOf(0.12f, 0.18f, 0.58f, 0.81f)),
            ),
        )
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
