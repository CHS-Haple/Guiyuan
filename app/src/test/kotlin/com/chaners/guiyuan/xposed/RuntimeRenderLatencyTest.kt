package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeRenderLatencyTest {
    @Test
    fun sampleReportsStageDurationsInMicroseconds() {
        val trace =
            RuntimeRenderTrace(
                id = 7L,
                source = "mobile",
                sourceNanos = 1_000_000L,
            ).withStateCommitted(1_500_000L)
                .withPresentationCommitted(2_500_000L)

        val sample =
            RuntimeRenderLatencySample.from(
                trace = trace,
                modelCommittedNanos = 3_000_000L,
                drawNanos = 5_000_000L,
                committedOnMainThread = true,
            )

        assertEquals(7L, sample.traceId)
        assertEquals("mobile", sample.source)
        assertEquals(500L, sample.sourceToStateUs)
        assertEquals(1_500L, sample.sourceToPresentationUs)
        assertEquals(1_000L, sample.stateToPresentationUs)
        assertEquals(1_500L, sample.stateToModelUs)
        assertEquals(500L, sample.presentationToModelUs)
        assertEquals(2_000L, sample.modelToDrawUs)
        assertEquals(4_000L, sample.sourceToDrawUs)
        assertEquals(true, sample.committedOnMainThread)
    }

    @Test
    fun sampleSupportsStateOnlyTrace() {
        val trace =
            RuntimeRenderTrace(
                id = 3L,
                source = "wifi",
                sourceNanos = 10_000_000L,
            ).withStateCommitted(10_250_000L)

        val sample =
            RuntimeRenderLatencySample.from(
                trace = trace,
                modelCommittedNanos = 10_500_000L,
                drawNanos = 11_000_000L,
                committedOnMainThread = false,
            )

        assertEquals(250L, sample.sourceToStateUs)
        assertNull(sample.sourceToPresentationUs)
        assertNull(sample.stateToPresentationUs)
        assertEquals(250L, sample.stateToModelUs)
        assertNull(sample.presentationToModelUs)
        assertEquals(500L, sample.modelToDrawUs)
        assertEquals(1_000L, sample.sourceToDrawUs)
    }

    @Test
    fun sampleSupportsPresentationOnlyTrace() {
        val trace =
            RuntimeRenderTrace(
                id = 9L,
                source = "mobileType",
                sourceNanos = 20_000_000L,
            ).withPresentationCommitted(20_400_000L)

        val sample =
            RuntimeRenderLatencySample.from(
                trace = trace,
                modelCommittedNanos = 20_800_000L,
                drawNanos = 21_300_000L,
                committedOnMainThread = true,
            )

        assertNull(sample.sourceToStateUs)
        assertEquals(400L, sample.sourceToPresentationUs)
        assertNull(sample.stateToPresentationUs)
        assertNull(sample.stateToModelUs)
        assertEquals(400L, sample.presentationToModelUs)
        assertEquals(500L, sample.modelToDrawUs)
        assertEquals(1_300L, sample.sourceToDrawUs)
    }

    @Test
    fun stageMarkersKeepFirstCommitTimestamp() {
        val initial =
            RuntimeRenderTrace(
                id = 1L,
                source = "connectivity",
                sourceNanos = 1_000L,
            )

        val state = initial.withStateCommitted(2_000L).withStateCommitted(3_000L)
        val presentation =
            state
                .withPresentationCommitted(4_000L)
                .withPresentationCommitted(5_000L)

        assertEquals(2_000L, state.stateCommittedNanos)
        assertEquals(4_000L, presentation.presentationCommittedNanos)
    }

    @Test
    fun outOfOrderStagesRetainSignedDeltas() {
        val trace =
            RuntimeRenderTrace(
                id = 1L,
                source = "test",
                sourceNanos = 5_000_000L,
            ).withStateCommitted(4_000_000L)

        val sample =
            RuntimeRenderLatencySample.from(
                trace = trace,
                modelCommittedNanos = 3_000_000L,
                drawNanos = 2_000_000L,
                committedOnMainThread = true,
            )

        assertEquals(-1_000L, sample.sourceToStateUs)
        assertEquals(-1_000L, sample.stateToModelUs)
        assertEquals(-1_000L, sample.modelToDrawUs)
        assertEquals(-3_000L, sample.sourceToDrawUs)
    }
}
