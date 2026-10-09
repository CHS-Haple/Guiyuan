package com.chaners.guiyuan.system

import android.content.Context
import com.chaners.guiyuan.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object DiagLogReader {
    private const val TIMEOUT_SEC = 10L

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
    )

    suspend fun read(context: Context): Snapshot = withContext(Dispatchers.Default) {
        val lspResult =
            RootShell.execute(
                context = context,
                command = LSP_CMD,
                timeoutSeconds = TIMEOUT_SEC,
            )
        val lspLines = filterModuleLines(lspResult.output)

        if (lspLines.isNotEmpty()) {
            return@withContext Snapshot(
                source = Source.LspModules,
                result = lspResult,
                lines = lspLines,
                sessionLines = latestSession(lspLines),
            )
        }

        val logcatResult =
            RootShell.execute(
                context = context,
                command = LOGCAT_CMD,
                timeoutSeconds = TIMEOUT_SEC,
            )
        val logcatLines = filterModuleLines(logcatResult.output)
        Snapshot(
            source = Source.LogcatFallback,
            result = logcatResult,
            lines = logcatLines,
            sessionLines = latestSession(logcatLines),
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

    internal fun latestSession(lines: List<String>): List<String> {
        if (lines.isEmpty()) {
            return lines
        }

        val sessionRefs =
            lines.mapIndexedNotNull { index, line ->
                if ("sessionId=" !in line) return@mapIndexedNotNull null
                DiagProtocol.parse(line)
                    ?.fields
                    ?.get("sessionId")
                    ?.let { sessionId -> index to sessionId }
            }
        val sessionId = sessionRefs.lastOrNull()?.second
        if (sessionId != null) {
            val start =
                sessionRefs.firstOrNull { (_, id) ->
                    id == sessionId
                }?.first
                    ?: return lines
            val pid = pidOf(lines[start])
            if (pid == null) {
                return lines.drop(start)
            }
            return lines
                .subList(start, lines.size)
                .filter { line -> pidOf(line) == pid }
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
        pidRe.find(line)?.groupValues?.getOrNull(1)

    private val pidRe =
        Regex(""":\s*(\d+):\s*\d+\s+[A-Z]/LSPosedFramework""")
}
