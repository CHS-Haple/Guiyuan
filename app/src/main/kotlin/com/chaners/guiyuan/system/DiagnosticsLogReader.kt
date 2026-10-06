package com.chaners.guiyuan.system

import com.chaners.guiyuan.BuildConfig

internal object DiagLogReader {
    private const val LogTimeoutSeconds = 10L

    private const val LsposedModuleLogCommand =
        "for f in \$(ls -1t /data/adb/lspd/log/modules_*.log " +
            "/data/adb/lspd/log.old/modules_*.log 2>/dev/null | head -n 8); do " +
            "if grep -Fq 'com.chaners.guiyuan' \"\$f\"; then " +
            "grep -F 'com.chaners.guiyuan' \"\$f\" || true; break; fi; done"

    private const val LogcatCommand =
        "logcat -d -b all -v threadtime -t 3000"

    internal enum class Source(
        val reportName: String,
    ) {
        LsposedModules("lsposed-modules"),
        LogcatFallback("logcat-fallback"),
    }

    internal data class Snapshot(
        val source: Source,
        val result: RootShell.Result,
        val lines: List<String>,
        val latestSessionLines: List<String>,
    )

    suspend fun read(): Snapshot {
        val lsposedResult =
            RootShell.execute(
                command = LsposedModuleLogCommand,
                timeoutSeconds = LogTimeoutSeconds,
            )
        val lsposedLines = filterModuleLines(lsposedResult.output)

        if (lsposedLines.isNotEmpty()) {
            return Snapshot(
                source = Source.LsposedModules,
                result = lsposedResult,
                lines = lsposedLines,
                latestSessionLines = selectLatestSession(lsposedLines),
            )
        }

        val logcatResult =
            RootShell.execute(
                command = LogcatCommand,
                timeoutSeconds = LogTimeoutSeconds,
            )
        val logcatLines = filterModuleLines(logcatResult.output)
        return Snapshot(
            source = Source.LogcatFallback,
            result = logcatResult,
            lines = logcatLines,
            latestSessionLines = selectLatestSession(logcatLines),
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

    internal fun selectLatestSession(lines: List<String>): List<String> {
        if (lines.isEmpty()) {
            return lines
        }

        val structuredEvents =
            lines.mapIndexedNotNull { index, line ->
                DiagnosticsProtocol.parse(line)
                    ?.fields
                    ?.get("sessionId")
                    ?.let { sessionId -> index to sessionId }
            }
        val latestSessionId = structuredEvents.lastOrNull()?.second
        if (latestSessionId != null) {
            val start =
                structuredEvents.firstOrNull { (_, sessionId) ->
                    sessionId == latestSessionId
                }?.first
                    ?: return lines
            val processId = processId(lines[start])
            if (processId == null) {
                return lines.drop(start)
            }
            return lines
                .subList(start, lines.size)
                .filter { line -> processId(line) == processId }
        }

        val currentBuild = "build=" + BuildConfig.BUILD_ID
        val currentAnchor =
            lines.indexOfLast { line ->
                line.contains(currentBuild) &&
                    (
                        line.contains("Module loaded in com.android.systemui") ||
                            line.contains("Hot reload completed")
                    )
            }
        val anchor =
            if (currentAnchor >= 0) {
                currentAnchor
            } else {
                lines.indexOfLast { line ->
                    line.contains("Module loaded in com.android.systemui")
                }
            }

        if (anchor < 0) {
            return lines
        }

        val processId = processId(lines[anchor]) ?: return lines.drop(anchor)
        val start =
            (anchor downTo 0).firstOrNull { index ->
                processId(lines[index]) == processId &&
                    lines[index].contains("Module loaded in com.android.systemui")
            } ?: anchor

        return lines
            .subList(start, lines.size)
            .filter { line -> processId(line) == processId }
    }

    private fun processId(line: String): String? =
        ProcessIdRegex.find(line)?.groupValues?.getOrNull(1)

    private val ProcessIdRegex =
        Regex(""":\s*(\d+):\s*\d+\s+[A-Z]/LSPosedFramework""")
}
