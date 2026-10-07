package com.chaners.guiyuan.system

internal data class RuntimeDiagnosticEvent(
    val schemaVersion: Int,
    val event: String,
    val component: String,
    val state: String?,
    val fields: Map<String, String>,
)

internal data class RuntimeEventSnapshot(
    val schemaVersion: Int,
    val sessionId: String?,
    val events: List<RuntimeDiagnosticEvent>,
) {
    fun component(name: String): RuntimeDiagnosticEvent? =
        events.firstOrNull { event -> event.component == name }

    fun reportLines(): List<String> =
        events.map { event ->
            buildString {
                append("component=")
                append(event.component)
                event.state?.let { state ->
                    append(" state=")
                    append(state)
                }
                append(" event=")
                append(event.event)
                event.fields
                    .toSortedMap()
                    .forEach { (key, value) ->
                        append(' ')
                        append(key)
                        append('=')
                        append(DiagProtocol.encode(value))
                    }
            }
        }

    companion object {
        private const val SessionIdField = "sessionId"

        // Keep the old wire key because existing runtime logs already use it.
        private const val SnapshotExcludeField = "healthSnapshot"

        private val eventMetadataFields =
            setOf(
                SessionIdField,
                SnapshotExcludeField,
                "uptimeMs",
                "sequence",
            )

        private val componentOrder =
            listOf(
                "module",
                "diagnostics",
                "compatibility",
                "statusHostHook",
                "statusHost",
                "network",
                "connectivity",
                "defaultDataSubscription",
                "presentationRuntime",
                "mobileType",
                "mobilePresentation",
                "airplane",
                "tint",
                "scene",
                "stableStatus",
                "renderer",
                "runtimeSession",
                "islandMotion",
                "hotReload",
            )

        fun fromLines(lines: List<String>): RuntimeEventSnapshot {
            val parsedEvents =
                lines.mapNotNull(DiagProtocol::parse)
            val latestSessionId =
                parsedEvents
                    .asReversed()
                    .firstNotNullOfOrNull { event -> event.fields[SessionIdField] }
            val scopedEvents =
                if (latestSessionId == null) {
                    parsedEvents
                } else {
                    parsedEvents.filter { event ->
                        event.fields[SessionIdField] == latestSessionId
                    }
                }

            val latest = linkedMapOf<String, RuntimeDiagnosticEvent>()
            scopedEvents
                .filterNot { event ->
                    event.fields[SnapshotExcludeField].equals("false", ignoreCase = true)
                }
                .forEach { event ->
                    latest[event.component] =
                        event.copy(fields = event.fields - eventMetadataFields)
                }

            val events =
                buildList {
                    componentOrder.forEach { component ->
                        latest[component]?.let { event -> add(event) }
                    }
                    latest.keys
                        .filterNot(componentOrder::contains)
                        .sorted()
                        .forEach { component ->
                            add(latest.getValue(component))
                        }
                }

            return RuntimeEventSnapshot(
                schemaVersion = scopedEvents.maxOfOrNull { event -> event.schemaVersion } ?: 0,
                sessionId = latestSessionId,
                events = events,
            )
        }
    }
}

internal object DiagProtocol {
    const val SchemaVersion = 2

    private const val Marker = "diag "

    fun format(
        event: String,
        component: String,
        state: String? = null,
        fields: Map<String, String> = emptyMap(),
    ): String =
        buildString {
            append(Marker)
            append("schema=")
            append(SchemaVersion)
            append(" event=")
            append(encode(event))
            append(" component=")
            append(encode(component))
            state?.let {
                append(" state=")
                append(encode(it))
            }
            fields
                .toSortedMap()
                .forEach { (key, value) ->
                    append(' ')
                    append(key)
                    append('=')
                    append(encode(value))
                }
        }

    fun parse(line: String): RuntimeDiagnosticEvent? {
        val markerIndex = line.indexOf(Marker)
        if (markerIndex < 0) {
            return null
        }

        val values =
            line
                .substring(markerIndex + Marker.length)
                .trim()
                .split(Regex("\\s+"))
                .mapNotNull { token ->
                    val separator = token.indexOf('=')
                    if (separator <= 0 || separator == token.lastIndex) {
                        null
                    } else {
                        token.substring(0, separator) to decode(token.substring(separator + 1))
                    }
                }
                .toMap()

        val event = values["event"] ?: return null
        val component = values["component"] ?: return null
        val state = values["state"]

        return RuntimeDiagnosticEvent(
            schemaVersion = values["schema"]?.toIntOrNull() ?: 0,
            event = event,
            component = component,
            state = state,
            fields = values - setOf("schema", "event", "component", "state"),
        )
    }

    internal fun encode(value: String): String =
        value
            .replace("%", "%25")
            .replace("\r", "%0D")
            .replace("\n", "%0A")
            .replace("\t", "%09")
            .replace(" ", "%20")
            .replace("=", "%3D")

    private fun decode(value: String): String =
        value
            .replace("%3D", "=")
            .replace("%20", " ")
            .replace("%09", "\t")
            .replace("%0A", "\n")
            .replace("%0D", "\r")
            .replace("%25", "%")
}
