package com.chaners.guiyuan.system

// A bounded summary of events already present in the user-requested diagnostic snapshot.
// It neither captures native data nor changes SystemUI runtime instrumentation.
internal object DiagEvidenceSummary {
    private const val MAX_NOTABLE = 40
    private const val MAX_FIELDS = 8
    private const val MAX_FIELD_CHARS = 80
    private val geometryFields = setOf(
        "parent", "parentIndex", "viewPath", "bounds", "clipBounds", "width", "height",
        "translationX", "translationY", "alpha", "visibility", "left", "top", "right", "bottom",
    )
    private val lifecycleTokens = listOf("session", "reload", "scene", "keyguard", "aod")

    fun reportLines(snapshot: DiagSnapshot): List<String> {
        val session = snapshot.runtimeEvents.sessionId
        var structured = 0
        var warnings = 0
        var errors = 0
        var hookRelated = 0
        var lifecycleRelated = 0
        var geometryFieldEntries = 0
        val notable = ArrayDeque<LogEntry>()

        snapshot.entries.forEach { entry ->
            if (!entry.structured || (session != null && entry.fields["sessionId"] != session)) {
                return@forEach
            }
            structured++
            val warning = entry.level == LogLevel.Warning
            val error = entry.level == LogLevel.Error || entry.level == LogLevel.Fatal
            if (warning) warnings++
            if (error) errors++
            val name = entry.event.orEmpty()
            val component = entry.component.orEmpty()
            val hook = name.contains("hook", ignoreCase = true) ||
                component.contains("hook", ignoreCase = true)
            val lifecycle = lifecycleTokens.any { token ->
                name.contains(token, ignoreCase = true) ||
                    component.contains(token, ignoreCase = true)
            }
            val geometry = entry.fields.keys.any(geometryFields::contains)
            if (hook) hookRelated++
            if (lifecycle) lifecycleRelated++
            if (geometry) geometryFieldEntries++
            if (warning || error || hook || lifecycle || geometry) {
                if (notable.size == MAX_NOTABLE) notable.removeFirst()
                notable.addLast(entry)
            }
        }

        return buildList {
            add("source=existing-session-events-only")
            add("collectedNewNativeData=false")
            add("sessionId=" + (session ?: "not-observed"))
            add("structuredEntries=" + structured)
            add("warningEntries=" + warnings)
            add("errorEntries=" + errors)
            add("hookRelatedEntries=" + hookRelated)
            add("lifecycleRelatedEntries=" + lifecycleRelated)
            add("geometryFieldEntries=" + geometryFieldEntries)
            add("nativeViewTree=not-collected")
            add("notableEntries=" + notable.size)
            notable.forEachIndexed { index, entry ->
                val fields = entry.fields.toSortedMap().entries
                    .take(MAX_FIELDS)
                    .joinToString(",") { (key, value) ->
                        key + "=" + DiagProtocol.encode(value.take(MAX_FIELD_CHARS))
                    }
                add(
                    "notable[" + index + "]=" +
                        "level=" + entry.level.name +
                        " event=" + (entry.event ?: "unknown") +
                        " component=" + (entry.component ?: "unknown") +
                        " state=" + (entry.state ?: "unknown") +
                        " fields=" + fields,
                )
            }
        }
    }
}
