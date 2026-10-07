package com.chaners.guiyuan.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeDiagnosticsProtocolTest {
    @Test
    fun formattedEventRoundTripsStructuredFields() {
        val line =
            RuntimeDiagnosticsProtocol.format(
                event = "source.install",
                component = "network",
                state = "ready",
                fields =
                    mapOf(
                        "source" to "cold start",
                        "hooks" to "4",
                    ),
            )

        val parsed = requireNotNull(RuntimeDiagnosticsProtocol.parse(line))

        assertEquals(RuntimeDiagnosticsProtocol.SchemaVersion, parsed.schemaVersion)
        assertEquals("source.install", parsed.event)
        assertEquals("network", parsed.component)
        assertEquals("ready", parsed.state)
        assertEquals("cold start", parsed.fields["source"])
        assertEquals("4", parsed.fields["hooks"])
    }

    @Test
    fun legacyEventWithoutSchemaStillParses() {
        val parsed =
            requireNotNull(
                RuntimeDiagnosticsProtocol.parse(
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
                RuntimeDiagnosticsProtocol.format(
                    event = "source.install",
                    component = "network",
                    state = "error",
                ),
                RuntimeDiagnosticsProtocol.format(
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
                RuntimeDiagnosticsProtocol.format(
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
                RuntimeDiagnosticsProtocol.format(
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
                RuntimeDiagnosticsProtocol.format(
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
                RuntimeDiagnosticsProtocol.format(
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
        assertEquals(RuntimeDiagnosticsProtocol.SchemaVersion, snapshot.schemaVersion)
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
                RuntimeDiagnosticsProtocol.format(
                    event = "source.install",
                    component = "network",
                    state = "ready",
                    fields = mapOf("hooks" to "4"),
                ),
                RuntimeDiagnosticsProtocol.format(
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
                    RuntimeDiagnosticsProtocol.format(
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
