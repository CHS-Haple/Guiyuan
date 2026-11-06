package com.chaners.guiyuan.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DiagProtocolTest {
    private companion object {
        const val SOURCE_INSTALL_EVENT = "source.install"
        const val MODULE_LOADED_EVENT = "module.loaded"
        const val NETWORK_COMPONENT = "network"
        const val MODULE_COMPONENT = "module"
        const val READY_STATE = "ready"
        const val HOOKS_FIELD = "hooks"
        const val HOOK_COUNT = "4"
        const val SOURCE_TO_DRAW_FIELD = "sourceToDrawUs"
        const val SESSION_ID_FIELD = "sessionId"
        const val SEQUENCE_FIELD = "sequence"
        const val UPTIME_MS_FIELD = "uptimeMs"
    }

    @Test
    fun formattedEventRoundTripsStructuredFields() {
        val line =
            DiagProtocol.format(
                event = SOURCE_INSTALL_EVENT,
                component = NETWORK_COMPONENT,
                state = READY_STATE,
                fields =
                    mapOf(
                        "source" to "cold start",
                        HOOKS_FIELD to HOOK_COUNT,
                    ),
            )

        val parsed = requireNotNull(DiagProtocol.parse(line))

        assertEquals(DiagProtocol.SchemaVersion, parsed.schemaVersion)
        assertEquals(SOURCE_INSTALL_EVENT, parsed.event)
        assertEquals(NETWORK_COMPONENT, parsed.component)
        assertEquals(READY_STATE, parsed.state)
        assertEquals("cold start", parsed.fields["source"])
        assertEquals(HOOK_COUNT, parsed.fields[HOOKS_FIELD])
    }

    @Test
    fun formattedObservationCanOmitState() {
        val parsed =
            requireNotNull(
                DiagProtocol.parse(
                    DiagProtocol.format(
                        event = "pipeline.latency",
                        component = "renderLatency",
                        fields = mapOf(SOURCE_TO_DRAW_FIELD to "1200"),
                    ),
                ),
            )

        assertNull(parsed.state)
        assertEquals("1200", parsed.fields[SOURCE_TO_DRAW_FIELD])
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
        assertEquals(MODULE_LOADED_EVENT, parsed.event)
    }

    @Test
    fun eventSnapshotUsesLatestEventForEachComponent() {
        val lines =
            listOf(
                DiagProtocol.format(
                    event = SOURCE_INSTALL_EVENT,
                    component = NETWORK_COMPONENT,
                    state = "error",
                ),
                DiagProtocol.format(
                    event = SOURCE_INSTALL_EVENT,
                    component = NETWORK_COMPONENT,
                    state = READY_STATE,
                    fields = mapOf(HOOKS_FIELD to HOOK_COUNT),
                ),
            )

        val network =
            requireNotNull(
                RuntimeEventSnapshot
                    .fromLines(lines)
                    .component(NETWORK_COMPONENT),
            )

        assertEquals(READY_STATE, network.state)
        assertEquals(HOOK_COUNT, network.fields[HOOKS_FIELD])
    }

    @Test
    fun eventSnapshotScopesToLatestRuntimeSession() {
        val lines =
            listOf(
                DiagProtocol.format(
                    event = MODULE_LOADED_EVENT,
                    component = MODULE_COMPONENT,
                    state = READY_STATE,
                    fields =
                        mapOf(
                            SESSION_ID_FIELD to "old",
                            SEQUENCE_FIELD to "1",
                            UPTIME_MS_FIELD to "100",
                        ),
                ),
                DiagProtocol.format(
                    event = SOURCE_INSTALL_EVENT,
                    component = NETWORK_COMPONENT,
                    state = "error",
                    fields =
                        mapOf(
                            SESSION_ID_FIELD to "old",
                            SEQUENCE_FIELD to "2",
                            UPTIME_MS_FIELD to "110",
                        ),
                ),
                DiagProtocol.format(
                    event = MODULE_LOADED_EVENT,
                    component = MODULE_COMPONENT,
                    state = READY_STATE,
                    fields =
                        mapOf(
                            SESSION_ID_FIELD to "new",
                            SEQUENCE_FIELD to "1",
                            UPTIME_MS_FIELD to "200",
                        ),
                ),
                DiagProtocol.format(
                    event = SOURCE_INSTALL_EVENT,
                    component = NETWORK_COMPONENT,
                    state = READY_STATE,
                    fields =
                        mapOf(
                            SESSION_ID_FIELD to "new",
                            SEQUENCE_FIELD to "2",
                            UPTIME_MS_FIELD to "210",
                            HOOKS_FIELD to HOOK_COUNT,
                        ),
                ),
            )

        val snapshot = RuntimeEventSnapshot.fromLines(lines)
        val network = requireNotNull(snapshot.component(NETWORK_COMPONENT))

        assertEquals("new", snapshot.sessionId)
        assertEquals(DiagProtocol.SchemaVersion, snapshot.schemaVersion)
        assertEquals(READY_STATE, network.state)
        assertEquals(HOOK_COUNT, network.fields[HOOKS_FIELD])
        assertFalse(network.fields.containsKey(SESSION_ID_FIELD))
        assertFalse(network.fields.containsKey(SEQUENCE_FIELD))
        assertFalse(network.fields.containsKey(UPTIME_MS_FIELD))
    }

    @Test
    fun excludedMetricEventsDoNotReplaceComponentSnapshot() {
        val lines =
            listOf(
                DiagProtocol.format(
                    event = SOURCE_INSTALL_EVENT,
                    component = NETWORK_COMPONENT,
                    state = READY_STATE,
                    fields = mapOf(HOOKS_FIELD to HOOK_COUNT),
                ),
                DiagProtocol.format(
                    event = "pipeline.latency",
                    component = NETWORK_COMPONENT,
                    state = "observed",
                    fields =
                        mapOf(
                            "healthSnapshot" to "false",
                            SOURCE_TO_DRAW_FIELD to "1200",
                        ),
                ),
            )

        val network =
            requireNotNull(
                RuntimeEventSnapshot
                    .fromLines(lines)
                    .component(NETWORK_COMPONENT),
            )

        assertEquals(READY_STATE, network.state)
        assertEquals(SOURCE_INSTALL_EVENT, network.event)
        assertEquals(HOOK_COUNT, network.fields[HOOKS_FIELD])
    }

    @Test
    fun unobservedComponentsAreNotInvented() {
        val snapshot =
            RuntimeEventSnapshot.fromLines(
                listOf(
                    DiagProtocol.format(
                        event = MODULE_LOADED_EVENT,
                        component = MODULE_COMPONENT,
                        state = READY_STATE,
                    ),
                ),
            )

        assertEquals(1, snapshot.events.size)
        assertNull(snapshot.component("renderer"))
    }
}
