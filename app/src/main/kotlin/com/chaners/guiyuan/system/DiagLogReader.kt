package com.chaners.guiyuan.system

import android.content.Context
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object DiagLogReader {
    private const val MODULE_PACKAGE = "com.chaners.guiyuan"
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val TIMEOUT_SEC = 10L
    private const val MODULE_LOG_TAG = "CombinedStatus"
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
            val stamp = DiagLogParser.parse(line).timestamp
            if (stamp == null) {
                false
            } else {
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
    }

    private const val LSP_CMD =
        "files=\$(ls -1t /data/adb/lspd/log/modules_*.log " +
            "/data/adb/lspd/log.old/modules_*.log 2>/dev/null | head -n 8); " +
            "fallback=''; selected=''; " +
            "for f in \$files; do " +
            "if [ -z \"\$fallback\" ] && grep -Fq '$MODULE_PACKAGE' \"\$f\"; then " +
            "fallback=\"\$f\"; fi; " +
            "if grep -F '$MODULE_PACKAGE' \"\$f\" | " +
            "grep -Fq '($SYSTEM_UI_PACKAGE)'; then selected=\"\$f\"; break; fi; done; " +
            "target=\${selected:-\$fallback}; " +
            "if [ -n \"\$target\" ]; then grep -F '$MODULE_PACKAGE' \"\$target\" || true; fi"

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
        val liveSystemUiPids: Set<String> = emptySet(),
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
            Snapshot(
                source = Source.LspModules,
                result = lspResult,
                lines = lspLines,
                sessionLines = DiagLogSessions.latestSession(lspLines),
                clearedAt = boundary,
            )
        } else {
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
                sessionLines = DiagLogSessions.latestSession(logcatLines),
                clearedAt = boundary,
            )
        }
    }

    internal fun filterModuleLines(output: String): List<String> {
        val lines =
            output.lineSequence()
                .filter { line ->
                    line.contains("$MODULE_PACKAGE") &&
                        line.contains(MODULE_LOG_TAG)
                }
                .toList()
        val systemUi = lines.filter { line ->
            DiagLogParser.parse(line).hostPkg == "$SYSTEM_UI_PACKAGE"
        }
        return systemUi.ifEmpty { lines }
    }

    internal fun filterLogcatLines(output: String): List<String> =
        output
            .lineSequence()
            .filter { line ->
                when {
                    MODULE_LOG_TAG !in line -> false
                    "$MODULE_PACKAGE" in line -> true
                    else -> {
                        val entry = DiagLogParser.parse(line)
                        entry.framework == "logcat" &&
                            entry.tag == MODULE_LOG_TAG &&
                            entry.structured
                    }
                }
            }
            .toList()

}
