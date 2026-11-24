package com.chaners.guiyuan.system

// A bounded summary of events already present in the user-requested diagnostic snapshot.
// It neither captures native data nor changes SystemUI runtime instrumentation.
internal object DiagEvidenceSummary {
    private const val MAX_NOTABLE = 40
    private const val MAX_FIELDS = 8
    private const val MAX_FIELD_CHARS = 80
    private const val UNKNOWN_LABEL = "unknown"
    private val geometryFields = setOf(
        "parent", "parentIndex", "viewPath", "bounds", "clipBounds", "width", "height",
        "translationX", "translationY", "alpha", "visibility", "left", "top", "right", "bottom",
    )
    private val lifecycleTokens = listOf("session", "reload", "scene", "keyguard", "aod")

    private data class Summary(
        val session: String?,
        val structured: Int,
        val warnings: Int,
        val errors: Int,
        val hookRelated: Int,
        val lifecycleRelated: Int,
        val geometryFieldEntries: Int,
        val notable: List<LogEntry>,
    )

    fun reportLines(snapshot: DiagSnapshot): List<String> {
        val summary = summarize(snapshot)
        return buildList {
            add("source=existing-session-events-only")
            add("collectedNewNativeData=false")
            add("sessionId=" + (summary.session ?: "not-observed"))
            add("structuredEntries=" + summary.structured)
            add("warningEntries=" + summary.warnings)
            add("errorEntries=" + summary.errors)
            add("hookRelatedEntries=" + summary.hookRelated)
            add("lifecycleRelatedEntries=" + summary.lifecycleRelated)
            add("geometryFieldEntries=" + summary.geometryFieldEntries)
            add("nativeViewTree=not-collected")
            add("notableEntries=" + summary.notable.size)
            summary.notable.forEachIndexed { index, entry ->
                add(notableLine(index, entry))
            }
        }
    }

    // The diagnostic aggregation reads only the captured snapshot. Report
    // formatting remains independent of event classification and truncation.
    private fun summarize(snapshot: DiagSnapshot): Summary {
        val session = snapshot.runtimeEvents.sessionId
        var structured = 0
        var warnings = 0
        var errors = 0
        var hookRelated = 0
        var lifecycleRelated = 0
        var geometryFieldEntries = 0
        val notable = ArrayDeque<LogEntry>()

        for (entry in snapshot.entries) {
            if (!entry.structured || (session != null && entry.fields["sessionId"] != session)) {
                continue
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

        return Summary(
            session = session,
            structured = structured,
            warnings = warnings,
            errors = errors,
            hookRelated = hookRelated,
            lifecycleRelated = lifecycleRelated,
            geometryFieldEntries = geometryFieldEntries,
            notable = notable.toList(),
        )
    }

    private fun notableLine(index: Int, entry: LogEntry): String {
        val fields = entry.fields.toSortedMap().entries
            .take(MAX_FIELDS)
            .joinToString(",") { (key, value) ->
                key + "=" + DiagProtocol.encode(value.take(MAX_FIELD_CHARS))
            }
        return "notable[" + index + "]=" +
            "level=" + entry.level.name +
            " event=" + (entry.event ?: UNKNOWN_LABEL) +
            " component=" + (entry.component ?: UNKNOWN_LABEL) +
            " state=" + (entry.state ?: UNKNOWN_LABEL) +
            " fields=" + fields
    }
}
