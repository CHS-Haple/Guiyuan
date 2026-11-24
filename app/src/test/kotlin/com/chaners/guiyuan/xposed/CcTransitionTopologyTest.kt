package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CcTransitionTopologyTest {
    @Test
    fun tinySecondaryComponentsRemainVisibleToTopologyClassifier() {
        val fourBars =
            listOf(
                ParticipantVisualSnapshot.NormalizedRect(0.05f, 0.60f, 0.15f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.30f, 0.48f, 0.40f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.55f, 0.34f, 0.65f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.80f, 0.18f, 0.90f, 0.95f),
            )
        val tinyDots =
            listOf(
                ParticipantVisualSnapshot.NormalizedRect(0.12f, 0.05f, 0.14f, 0.07f),
                ParticipantVisualSnapshot.NormalizedRect(0.42f, 0.05f, 0.44f, 0.07f),
                ParticipantVisualSnapshot.NormalizedRect(0.72f, 0.05f, 0.74f, 0.07f),
            )

        val retained =
            ParticipantVisualSnapshot.filterProbeComponents(
                components = fourBars + tinyDots,
                probeWidth = 96,
                probeHeight = 96,
            )

        assertEquals(7, retained.size)
        assertEquals(
            ParticipantVisualSnapshot.Topology.COMPOSITE,
            ParticipantVisualSnapshot.classifyComponents(retained),
        )
    }

    @Test
    fun dualRowCompositeCannotExposeExactFourBarCapability() {
        val components =
            listOf(
                ParticipantVisualSnapshot.NormalizedRect(0.05f, 0.60f, 0.15f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.30f, 0.48f, 0.40f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.55f, 0.34f, 0.65f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.80f, 0.18f, 0.90f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.05f, 0.05f, 0.15f, 0.12f),
                ParticipantVisualSnapshot.NormalizedRect(0.30f, 0.05f, 0.40f, 0.12f),
                ParticipantVisualSnapshot.NormalizedRect(0.55f, 0.05f, 0.65f, 0.12f),
                ParticipantVisualSnapshot.NormalizedRect(0.80f, 0.05f, 0.90f, 0.12f),
            )
        val envelope =
            ParticipantVisualSnapshot.NormalizedRect(
                left = 0.05f,
                top = 0.05f,
                right = 0.90f,
                bottom = 0.95f,
            )
        val snapshot =
            ParticipantVisualSnapshot.Snapshot(
                envelope = envelope,
                components = components,
                topology = ParticipantVisualSnapshot.classifyComponents(components),
            )

        assertEquals(
            ParticipantVisualSnapshot.Topology.COMPOSITE,
            snapshot.topology,
        )
        assertNull(snapshot.fourVerticalBarsWithinEnvelope())
    }

    @Test
    fun participantVisualTopologyDistinguishesFourBarsFromComposite() {
        val fourBars =
            listOf(
                ParticipantVisualSnapshot.NormalizedRect(0.05f, 0.60f, 0.15f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.30f, 0.48f, 0.40f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.55f, 0.34f, 0.65f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.80f, 0.18f, 0.90f, 0.95f),
            )
        assertEquals(
            ParticipantVisualSnapshot.Topology.FOUR_VERTICAL_BARS,
            ParticipantVisualSnapshot.classifyComponents(fourBars),
        )

        val composite =
            fourBars +
                listOf(
                    ParticipantVisualSnapshot.NormalizedRect(0.05f, 0.05f, 0.15f, 0.12f),
                    ParticipantVisualSnapshot.NormalizedRect(0.30f, 0.05f, 0.40f, 0.12f),
                )
        assertEquals(
            ParticipantVisualSnapshot.Topology.COMPOSITE,
            ParticipantVisualSnapshot.classifyComponents(composite),
        )
    }
}
