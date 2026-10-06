package com.chaners.guiyuan.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun healthSnapshotUsesLatestEventForEachComponent() {
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
            RuntimeHealthSnapshot
                .fromLines(lines)
                .components
                .first { it.component == "network" }

        assertEquals("ready", network.state)
        assertEquals("4", network.fields["hooks"])
    }

    @Test
    fun healthSnapshotScopesToLatestRuntimeSession() {
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

        val snapshot = RuntimeHealthSnapshot.fromLines(lines)
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
    fun metricEventsDoNotReplaceHealthState() {
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
                RuntimeHealthSnapshot
                    .fromLines(lines)
                    .component("network"),
            )

        assertEquals("ready", network.state)
        assertEquals("source.install", network.event)
        assertEquals("4", network.fields["hooks"])
    }

    @Test
    fun completeHotReloadGenerationReportsHealthy() {
        val coreComponents =
            listOf(
                "module",
                "diagnostics",
                "compatibility",
                "statusHostHook",
                "statusHost",
                "network",
                "airplane",
                "presentationRuntime",
                "stableStatus",
                "renderer",
                "runtimeSession",
            )
        val lines =
            coreComponents.mapIndexed { index, component ->
                DiagProtocol.format(
                    event = "hotReload.test",
                    component = component,
                    state = "ready",
                    fields =
                        mapOf(
                            "sessionId" to "hot",
                            "sequence" to (index + 1).function toString() { [native code] }(),
                        ),
                )
            }

        val snapshot = RuntimeHealthSnapshot.fromLines(lines)

        assertEquals("healthy", snapshot.overall)
        assertEquals("hot", snapshot.sessionId)
    }

    @Test
    fun unobservedTintAndSceneDoNotDegradeReadyPresentationOwner() {
        val coreComponents =
            listOf(
                "module",
                "diagnostics",
                "compatibility",
                "statusHostHook",
                "statusHost",
                "network",
                "airplane",
                "presentationRuntime",
                "stableStatus",
                "renderer",
                "runtimeSession",
            )
        val lines =
            coreComponents.mapIndexed { index, component ->
                DiagProtocol.format(
                    event = "source.ready",
                    component = component,
                    state = "ready",
                    fields = mapOf("sequence" to (index + 1).function toString() { [native code] }()),
                )
            }

        val snapshot = RuntimeHealthSnapshot.fromLines(lines)

        assertEquals("healthy", snapshot.overall)
        assertEquals("ready", requireNotNull(snapshot.component("presentationRuntime")).state)
        assertEquals("unknown", requireNotNull(snapshot.component("tint")).state)
        assertEquals("unknown", requireNotNull(snapshot.component("scene")).state)
    }

    @Test
    fun missingCoreComponentsNeverReportHealthy() {
        val snapshot =
            RuntimeHealthSnapshot.fromLines(
                listOf(
                    DiagProtocol.format(
                        event = "module.loaded",
                        component = "module",
                        state = "ready",
                    ),
                ),
            )

        assertEquals("degraded", snapshot.overall)
        assertEquals(
            "unknown",
            snapshot.components.first { it.component == "renderer" }.state,
        )
    }
}
