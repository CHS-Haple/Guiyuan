package com.chaners.guiyuan.system

// Read-only triage from the already captured session; no new Root commands or native hooks.
internal object ItDebugTriage {
    private val geometryKeys = setOf(
        "parent", "parentIndex", "viewPath", "bounds", "clipBounds", "width", "height",
        "translationX", "translationY", "alpha", "visibility", "left", "top", "right", "bottom",
    )

    internal fun reportLines(snapshot: DiagSnapshot): List<String> {
        val sessionId = snapshot.runtimeEvents.sessionId
        val scoped = snapshot.entries.filter { entry ->
            entry.structured &&
                (sessionId == null || entry.fields["sessionId"] == sessionId)
        }
        val warnings = scoped.count { it.level == LogLevel.Warning }
        val errors = scoped.count { it.level == LogLevel.Error || it.level == LogLevel.Fatal }
        val hookEvents = scoped.filter { entry ->
            entry.event.orEmpty().contains("hook", ignoreCase = true) ||
                entry.component.orEmpty().contains("hook", ignoreCase = true)
        }
        val lifecycleEvents = scoped.filter { entry ->
            listOf("session", "reload", "scene", "keyguard", "aod").any { token ->
                entry.event.orEmpty().contains(token, ignoreCase = true) ||
                    entry.component.orEmpty().contains(token, ignoreCase = true)
            }
        }
        val geometryEvents = scoped.filter { entry ->
            entry.fields.keys.any(geometryKeys::contains)
        }
        val notable = scoped.filter { entry ->
            entry.level == LogLevel.Warning ||
                entry.level == LogLevel.Error ||
                entry.level == LogLevel.Fatal ||
                entry in hookEvents ||
                entry in lifecycleEvents ||
                entry in geometryEvents
        }.takeLast(40)

        return buildList {
            add("source=existing-session-events-only")
            add("collectedNewNativeData=false")
            add("sessionId=" + (sessionId ?: "not-observed"))
            add("structuredEvents=" + scoped.size)
            add("warningEvents=$warnings")
            add("errorEvents=$errors")
            add("hookEvents=" + hookEvents.size)
            add("lifecycleEvents=" + lifecycleEvents.size)
            add("geometryEvents=" + geometryEvents.size)
            add("nativeViewTree=" + if (geometryEvents.isEmpty()) "not-observed" else "partial-event-fields-only")
            add("capturedStackTraces=existing-log-only")
            add("notableEvents=" + notable.size)
            notable.forEachIndexed { index, entry ->
                val fields = entry.fields.toSortedMap().entries.take(8).joinToString(",") { (key, value) ->
                    "$key=${DiagProtocol.encode(value.take(80))}"
                }
                add(
                    "notable[$index]=" +
                        "level=${entry.level.name} event=${entry.event ?: "unknown"} " +
                        "component=${entry.component ?: "unknown"} state=${entry.state ?: "unknown"} " +
                        "fields=$fields",
                )
            }
        }
    }
}
