package com.chaners.guiyuan.system

import android.content.Context
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.DiagRepo
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DiagSnapshot(
    val env: RuntimeEnv,
    val level: DiagLevel,
    val log: DiagLogReader.Snapshot,
    val runtimeEvents: RuntimeEventSnapshot,
    val entries: List<LogEntry>,
    val capturedAt: OffsetDateTime,
    val liveSystemUiPids: Set<String> = emptySet(),
) {
    // Only the selected SystemUI generation can confirm that the runtime setting took effect.
    internal fun isRuntimeDetailConfirmed(): Boolean {
        val session = runtimeEvents.sessionId ?: return false
        if (liveSystemUiPids.isEmpty()) return false
        var bound = false
        var level: String? = null
        entries.forEach { entry ->
            if (
                !entry.structured ||
                entry.hostPkg != "com.android.systemui" ||
                entry.pid !in liveSystemUiPids ||
                entry.component != "diagnostics" ||
                entry.fields["sessionId"] != session
            ) {
                return@forEach
            }
            if (entry.event == "diagnostics.bind" || entry.event == "diagnostics.snapshot") {
                bound = entry.state == "ready"
            }
            entry.fields["level"]?.let { level = it }
        }
        return bound && level == "detailed"
    }

    companion object {
        // Capture once so the screen and exported report describe the same runtime session.
        suspend fun capture(context: Context): DiagSnapshot {
            val appCtx = context.applicationContext
            val env = RuntimeEnv.resolve(appCtx)
            val level = DiagRepo(appCtx).current()
            val log = DiagLogReader.read(appCtx)
            val pidResult = RootShell.execute(
                context = appCtx,
                command = "/system/bin/pidof com.android.systemui",
                timeoutSeconds = 3L,
            )
            val liveSystemUiPids =
                if (pidResult.isSuccess) {
                    pidResult.output.split(Regex("\\s+"))
                        .filter { pid -> pid.isNotBlank() && pid.all(Char::isDigit) }
                        .toSet()
                } else {
                    emptySet()
                }
            val (entries, runtimeEvents) =
                withContext(Dispatchers.Default) {
                    val lines = log.sessionLines
                    lines.map(DiagLogParser::parse) to RuntimeEventSnapshot.fromLines(lines)
                }

            return DiagSnapshot(
                env = env,
                level = level,
                log = log,
                runtimeEvents = runtimeEvents,
                entries = entries,
                capturedAt = OffsetDateTime.now(),
                liveSystemUiPids = liveSystemUiPids,
            )
        }
    }
}
