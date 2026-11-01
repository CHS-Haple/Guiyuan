package com.chaners.guiyuan.system

import android.content.Context
import com.chaners.guiyuan.BuildConfig
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object DiagLogReader {
    private const val TIMEOUT_SEC = 10L
    private const val HISTORY_PREFS = "guiyuan_diagnostics_history"
    private const val CLEAR_BOUNDARY_KEY = "cleared_at_local"

    // LSPosed module files and logcat are shared; never truncate or delete them.
    suspend fun clearHistory(context: Context): Boolean = withContext(Dispatchers.IO) {
        context.applicationContext.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CLEAR_BOUNDARY_KEY, LocalDateTime.now().toString())
            .commit()
    }

    private fun historyBoundary(context: Context): LocalDateTime? =
        context.applicationContext.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
            .getString(CLEAR_BOUNDARY_KEY, null)
            ?.let { raw -> runCatching { LocalDateTime.parse(raw) }.getOrNull() }

    // The source remains intact; this boundary scopes what the app displays and exports.
    internal fun afterHistoryClear(
        lines: List<String>,
        boundary: LocalDateTime?,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<String> {
        if (boundary == null) return lines
        return lines.filter { line ->
            val stamp = DiagLogParser.parse(line).timestamp ?: return@filter false
            val time = runCatching {
                if ('T' in stamp) {
                    LocalDateTime.parse(stamp)
                } else {
                    // logcat uses MM-dd without a year; the bounded read is recent.
                    val candidate = LocalDateTime.parse("${now.year}-${stamp.replace(' ', 'T')}")
                    if (candidate.isAfter(now.plusDays(1))) candidate.minusYears(1) else candidate
                }
            }.getOrNull()
            time != null && time.isAfter(boundary)
        }
    }

    private const val LSP_CMD =
        "for f in \$(ls -1t /data/adb/lspd/log/modules_*.log " +
            "/data/adb/lspd/log.old/modules_*.log 2>/dev/null | head -n 8); do " +
            "if grep -Fq 'com.chaners.guiyuan' \"\$f\"; then " +
            "grep -F 'com.chaners.guiyuan' \"\$f\" || true; break; fi; done"

    private const val LOGCAT_CMD =
        "logcat -d -b all -v threadtime -t 3000"

    internal enum class Source(
        val reportName: String,
    ) {
        LspModules("lsposed-modules"),
        LogcatFallback("logcat-fallback"),
    }

    internal data class Snapshot(
        val source: Source,
        val result: RootShell.Result,
        val lines: List<String>,
        val sessionLines: List<String>,
        val clearedAt: LocalDateTime? = null,
    )

    suspend fun read(context: Context): Snapshot = withContext(Dispatchers.Default) {
        val boundary = historyBoundary(context)
        val lspResult =
            RootShell.execute(
                context = context,
                command = LSP_CMD,
                timeoutSeconds = TIMEOUT_SEC,
            )
        val lspLines = afterHistoryClear(filterModuleLines(lspResult.output), boundary)

        // A cleared LSP history may have no new entries; filter logcat too before fallback.
        if (lspLines.isNotEmpty() || !lspResult.isSuccess) {
            return@withContext Snapshot(
                source = Source.LspModules,
                result = lspResult,
                lines = lspLines,
                sessionLines = latestSession(lspLines),
                clearedAt = boundary,
            )
        }

        val logcatResult =
            RootShell.execute(
                context = context,
                command = LOGCAT_CMD,
                timeoutSeconds = TIMEOUT_SEC,
            )
        val logcatLines = afterHistoryClear(filterLogcatLines(logcatResult.output), boundary)
        Snapshot(
            source = Source.LogcatFallback,
            result = logcatResult,
            lines = logcatLines,
            sessionLines = latestSession(logcatLines),
            clearedAt = boundary,
        )
    }

    internal fun filterModuleLines(output: String): List<String> =
        output
            .lineSequence()
            .filter { line ->
                line.contains("com.chaners.guiyuan") &&
                    line.contains("CombinedStatus")
            }
            .toList()

    internal fun filterLogcatLines(output: String): List<String> =
        output
            .lineSequence()
            .filter { line ->
                if ("CombinedStatus" !in line) return@filter false
                if ("com.chaners.guiyuan" in line) return@filter true

                val entry = DiagLogParser.parse(line)
                entry.framework == "logcat" &&
                    entry.tag == "CombinedStatus" &&
                    entry.structured
            }
            .toList()

    internal fun latestSession(lines: List<String>): List<String> {
        if (lines.isEmpty()) {
            return lines
        }

        // The new generation can fail before module.reloaded; old teardown may log later.
        val tagged =
            lines.mapIndexedNotNull { index, line ->
                if ("sessionId=" !in line) return@mapIndexedNotNull null
                DiagProtocol.parse(line)
                    ?.takeIf { event -> event.fields["sessionId"] != null }
                    ?.let { event -> index to event }
            }
        val latestStart =
            tagged.lastOrNull { (_, event) ->
                event.event == "module.loaded" || event.event == "module.reloaded"
            }
        val failedNewGeneration =
            tagged.lastOrNull { (_, event) ->
                if (
                    event.event != "hotReload.complete" ||
                    (event.state != "error" && event.state != "unavailable")
                ) {
                    false
                } else {
                    val id = event.fields["sessionId"]
                    val first =
                        tagged.firstOrNull { (_, prior) -> prior.fields["sessionId"] == id }
                            ?.first
                    first != null && (latestStart == null || first > latestStart.first)
                }
            }
        val sessionId =
            (failedNewGeneration ?: latestStart ?: tagged.lastOrNull())
                ?.second
                ?.fields
                ?.get("sessionId")
        if (sessionId != null) {
            val start =
                tagged.firstOrNull { (_, event) -> event.fields["sessionId"] == sessionId }?.first
                    ?: return lines
            val stale =
                tagged.asSequence()
                    .filter { (index, event) ->
                        index >= start && event.fields["sessionId"] != sessionId
                    }
                    .map { it.first }
                    .toSet()
            val pid = pidOf(lines[start])
            return lines
                .subList(start, lines.size)
                .filterIndexed { index, line ->
                    index + start !in stale && (pid == null || pidOf(line) == pid)
                }
        }

        val buildToken = "build=" + BuildConfig.BUILD_ID
        val buildAnchor =
            lines.indexOfLast { line ->
                line.contains(buildToken) &&
                    (
                        line.contains("Module loaded in com.android.systemui") ||
                            line.contains("Hot reload completed")
                    )
            }
        val anchor =
            if (buildAnchor >= 0) {
                buildAnchor
            } else {
                lines.indexOfLast { line ->
                    line.contains("Module loaded in com.android.systemui")
                }
            }

        if (anchor < 0) {
            return lines
        }

        val pid = pidOf(lines[anchor]) ?: return lines.drop(anchor)
        val start =
            (anchor downTo 0).firstOrNull { index ->
                pidOf(lines[index]) == pid &&
                    lines[index].contains("Module loaded in com.android.systemui")
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
