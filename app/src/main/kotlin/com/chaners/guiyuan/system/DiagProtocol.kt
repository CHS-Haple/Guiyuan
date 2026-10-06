package com.chaners.guiyuan.system

internal data class DiagnosticEvent(
    val schemaVersion: Int,
    val event: String,
    val component: String,
    val state: String,
    val fields: Map<String, String>,
)

internal data class HealthComponent(
    val component: String,
    val state: String,
    val event: String,
    val fields: Map<String, String>,
)

internal data class RuntimeHealthSnapshot(
    val overall: String,
    val schemaVersion: Int,
    val sessionId: String?,
    val components: List<HealthComponent>,
) {
    fun component(name: String): HealthComponent? =
        components.firstOrNull { component -> component.component == name }

    fun reportLines(): List<String> =
        buildList {
            add("overall=$overall")
            add("schemaVersion=$schemaVersion")
            add("sessionId=" + (sessionId ?: "legacy-or-unavailable"))
            components.forEach { component ->
                add(
                    buildString {
                        append("component=")
                        append(component.component)
                        append(" state=")
                        append(component.state)
                        append(" event=")
                        append(component.event)
                        component.fields
                            .toSortedMap()
                            .forEach { (key, value) ->
                                append(' ')
                                append(key)
                                append('=')
                                append(DiagProtocol.encode(value))
                            }
                    },
                )
            }
        }

    companion object {
        private const val SessionIdField = "sessionId"
        private const val HealthSnapshotField = "healthSnapshot"

        private val eventMetadataFields =
            setOf(
                SessionIdField,
                HealthSnapshotField,
                "uptimeMs",
                "sequence",
            )

        private val expectedComponents =
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

        private val coreComponents =
            setOf(
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

        fun fromLines(lines: List<String>): RuntimeHealthSnapshot {
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

            val latest = linkedMapOf<String, DiagnosticEvent>()
            scopedEvents
                .filterNot { event ->
                    event.fields[HealthSnapshotField].equals("false", ignoreCase = true)
                }
                .forEach { event ->
                    latest[event.component] = event
                }

            val components =
                buildList {
                    expectedComponents.forEach { component ->
                        val event = latest[component]
                        add(
                            HealthComponent(
                                component = component,
                                state = event?.state ?: "unknown",
                                event = event?.event ?: "not-observed",
                                fields = event?.fields.orEmpty() - eventMetadataFields,
                            ),
                        )
                    }

                    latest.keys
                        .filterNot(expectedComponents::contains)
                        .sorted()
                        .forEach { component ->
                            val event = latest.getValue(component)
                            add(
                                HealthComponent(
                                    component = component,
                                    state = event.state,
                                    event = event.event,
                                    fields = event.fields - eventMetadataFields,
                                ),
                            )
                        }
                }

            val byName = components.associateBy(HealthComponent::component)
            val moduleState = byName["module"]?.state
            val healthy =
                coreComponents.all { component ->
                    byName[component]?.state in setOf("ready", "disabled")
                }
            val overall =
                when {
                    moduleState == null || moduleState == "unknown" -> "unavailable"
                    healthy -> "healthy"
                    else -> "degraded"
                }

            return RuntimeHealthSnapshot(
                overall = overall,
                schemaVersion = scopedEvents.maxOfOrNull { event -> event.schemaVersion } ?: 0,
                sessionId = latestSessionId,
                components = components,
            )
        }
    }
}

internal object DiagProtocol {
    const val SchemaVersion = 1

    private const val Marker = "diag "

    fun format(
        event: String,
        component: String,
        state: String,
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
            append(" state=")
            append(encode(state))
            fields
                .toSortedMap()
                .forEach { (key, value) ->
                    append(' ')
                    append(key)
                    append('=')
                    append(encode(value))
                }
        }

    fun parse(line: String): DiagnosticEvent? {
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
        val state = values["state"] ?: return null

        return DiagnosticEvent(
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
