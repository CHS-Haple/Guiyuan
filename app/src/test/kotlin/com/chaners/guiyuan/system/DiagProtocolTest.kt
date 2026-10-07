package com.chaners.guiyuan.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DiagProtocolTest {
    @Test
    fun formattedEventRoundTripsStructuredFields() {
        val line =
            DiagProtocol.format(
                event = "source.install",
                component = "network",
                state = "ready",
                fields =
                    mapOf(
                        "source" to "cold start",
                        "hooks" to "4",
                    ),
            )

        val parsed = requireNotNull(DiagProtocol.parse(line))

        assertEquals(DiagProtocol.SchemaVersion, parsed.schemaVersion)
        assertEquals("source.install", parsed.event)
        assertEquals("network", parsed.component)
        assertEquals("ready", parsed.state)
        assertEquals("cold start", parsed.fields["source"])
        assertEquals("4", parsed.fields["hooks"])
    }

    @Test
    fun formattedObservationCanOmitState() {
        val parsed =
            requireNotNull(
                DiagProtocol.parse(
                    DiagProtocol.format(
                        event = "pipeline.latency",
                        component = "renderLatency",
                        fields = mapOf("sourceToDrawUs" to "1200"),
                    ),
                ),
            )

        assertNull(parsed.state)
        assertEquals("1200", parsed.fields["sourceToDrawUs"])
    }

    @Test
    fun legacyEventWithoutSchemaStillParses() {
        val parsed =
            requireNotNull(
                DiagProtocol.parse(
                    "diag event=module.loaded component=module state=ready",
                ),
            )

        assertEquals(0, parsed.schemaVersion)
        assertEquals("module.loaded", parsed.event)
    }

    @Test
    fun eventSnapshotUsesLatestEventForEachComponent() {
        val lines =
            listOf(
                DiagProtocol.format(
                    event = "source.install",
                    component = "network",
                    state = "error",
                ),
                DiagProtocol.format(
                    event = "source.install",
                    component = "network",
                    state = "ready",
                    fields = mapOf("hooks" to "4"),
                ),
            )

        val network =
            requireNotNull(
                RuntimeEventSnapshot
                    .fromLines(lines)
                    .component("network"),
            )

        assertEquals("ready", network.state)
        assertEquals("4", network.fields["hooks"])
    }

    @Test
    fun eventSnapshotScopesToLatestRuntimeSession() {
        val lines =
            listOf(
                DiagProtocol.format(
                    event = "module.loaded",
                    component = "module",
                    state = "ready",
                    fields =
                        mapOf(
                            "sessionId" to "old",
                            "sequence" to "1",
                            "uptimeMs" to "100",
                        ),
                ),
                DiagProtocol.format(
                    event = "source.install",
                    component = "network",
                    state = "error",
                    fields =
                        mapOf(
                            "sessionId" to "old",
                            "sequence" to "2",
                            "uptimeMs" to "110",
                        ),
                ),
                DiagProtocol.format(
                    event = "module.loaded",
                    component = "module",
                    state = "ready",
                    fields =
                        mapOf(
                            "sessionId" to "new",
                            "sequence" to "1",
                            "uptimeMs" to "200",
                        ),
                ),
                DiagProtocol.format(
                    event = "source.install",
                    component = "network",
                    state = "ready",
                    fields =
                        mapOf(
                            "sessionId" to "new",
                            "sequence" to "2",
                            "uptimeMs" to "210",
                            "hooks" to "4",
                        ),
                ),
            )

        val snapshot = RuntimeEventSnapshot.fromLines(lines)
        val network = requireNotNull(snapshot.component("network"))

        assertEquals("new", snapshot.sessionId)
        assertEquals(DiagProtocol.SchemaVersion, snapshot.schemaVersion)
        assertEquals("ready", network.state)
        assertEquals("4", network.fields["hooks"])
        assertFalse(network.fields.containsKey("sessionId"))
        assertFalse(network.fields.containsKey("sequence"))
        assertFalse(network.fields.containsKey("uptimeMs"))
    }

    @Test
    fun excludedMetricEventsDoNotReplaceComponentSnapshot() {
        val lines =
            listOf(
                DiagProtocol.format(
                    event = "source.install",
                    component = "network",
                    state = "ready",
                    fields = mapOf("hooks" to "4"),
                ),
                DiagProtocol.format(
                    event = "pipeline.latency",
                    component = "network",
                    state = "observed",
                    fields =
                        mapOf(
                            "healthSnapshot" to "false",
                            "sourceToDrawUs" to "1200",
                        ),
                ),
            )

        val network =
            requireNotNull(
                RuntimeEventSnapshot
                    .fromLines(lines)
                    .component("network"),
            )

        assertEquals("ready", network.state)
        assertEquals("source.install", network.event)
        assertEquals("4", network.fields["hooks"])
    }

    @Test
    fun unobservedComponentsAreNotInvented() {
        val snapshot =
            RuntimeEventSnapshot.fromLines(
                listOf(
                    DiagProtocol.format(
                        event = "module.loaded",
                        component = "module",
                        state = "ready",
                    ),
                ),
            )

        assertEquals(1, snapshot.events.size)
        assertNull(snapshot.component("renderer"))
    }
}
