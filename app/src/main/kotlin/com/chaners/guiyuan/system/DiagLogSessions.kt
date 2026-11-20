package com.chaners.guiyuan.system

import com.chaners.guiyuan.BuildConfig

// The displayed log session is independent of its LSPosed/logcat transport.
internal object DiagLogSessions {
    private const val SESSION_ID_FIELD = "sessionId"
    private const val MODULE_LOADED_MESSAGE = "Module loaded in com.android.systemui"

    private data class TaggedSessions(
        val events: List<Pair<Int, DiagEvent>>,
        val firstIndex: Map<String, Int>,
    )

    internal fun latestSession(lines: List<String>): List<String> {
        if (lines.isEmpty()) return lines
        val tagged = collectTaggedSessions(lines)
        val sessionId = selectLatestSessionId(tagged)
        return if (sessionId != null) {
            retainLatestTaggedSession(lines, tagged, sessionId)
        } else {
            latestSessionFromLegacyMarkers(lines)
        }
    }

    private fun collectTaggedSessions(lines: List<String>): TaggedSessions {
        val events = lines.mapIndexedNotNull { index, line ->
            if ("sessionId=" in line) {
                DiagProtocol.parse(line)
                    ?.takeIf { event -> event.fields[SESSION_ID_FIELD] != null }
                    ?.let { event -> index to event }
            } else null
        }
        val firstIndex = mutableMapOf<String, Int>()
        events.forEach { (index, event) ->
            event.fields[SESSION_ID_FIELD]?.let { id ->
                firstIndex.getOrPut(id) { index }
            }
        }
        return TaggedSessions(events, firstIndex)
    }

    private fun selectLatestSessionId(tagged: TaggedSessions): String? {
        // A failed new generation may precede module.reloaded and old teardown can arrive later.
        val latestStart = tagged.events.lastOrNull { (_, event) ->
            event.event == "module.loaded" || event.event == "module.reloaded"
        }
        val failedNewGeneration = tagged.events.lastOrNull { (_, event) ->
            if (
                event.event != "hotReload.complete" ||
                (event.state != "error" && event.state != "unavailable")
            ) {
                false
            } else {
                val first = event.fields[SESSION_ID_FIELD]?.let { id -> tagged.firstIndex[id] }
                first != null && (latestStart == null || first > latestStart.first)
            }
        }
        return (failedNewGeneration ?: latestStart ?: tagged.events.lastOrNull())
            ?.second?.fields?.get(SESSION_ID_FIELD)
    }

    private fun retainLatestTaggedSession(
        lines: List<String>,
        tagged: TaggedSessions,
        sessionId: String,
    ): List<String> {
        val start = tagged.firstIndex[sessionId] ?: return lines
        val stale = tagged.events.asSequence()
            .filter { (index, event) ->
                index >= start && event.fields[SESSION_ID_FIELD] != sessionId
            }
            .map { it.first }
            .toSet()
        val pid = pidOf(lines[start])
        return lines.subList(start, lines.size)
            .filterIndexed { index, line ->
                index + start !in stale && (pid == null || pidOf(line) == pid)
            }
    }

    private fun latestSessionFromLegacyMarkers(lines: List<String>): List<String> {
        val buildToken = "build=" + BuildConfig.BUILD_ID
        val buildAnchor =
            lines.indexOfLast { line ->
                line.contains(buildToken) &&
                    (
                        line.contains(MODULE_LOADED_MESSAGE) ||
                            line.contains("Hot reload completed")
                    )
            }
        val anchor =
            if (buildAnchor >= 0) {
                buildAnchor
            } else {
                lines.indexOfLast { line ->
                    line.contains(MODULE_LOADED_MESSAGE)
                }
            }

        if (anchor < 0) {
            return lines
        }

        val pid = pidOf(lines[anchor]) ?: return lines.drop(anchor)
        val start =
            (anchor downTo 0).firstOrNull { index ->
                pidOf(lines[index]) == pid &&
                    lines[index].contains(MODULE_LOADED_MESSAGE)
            } ?: anchor

        return lines
            .subList(start, lines.size)
            .filter { line -> pidOf(line) == pid }
    }

    private fun pidOf(line: String): String? =
        (lspPidRe.find(line) ?: logcatPidRe.find(line))
            ?.groupValues?.getOrNull(1)

    private val lspPidRe =
        Regex(""":\s*(\d+):\s*\d+\s+[A-Z]/LSPosedFramework""")

    private val logcatPidRe =
        Regex("""^\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+\s+(\d+)\s+\d+\s+[VDIWEAF]\s+""")
}
