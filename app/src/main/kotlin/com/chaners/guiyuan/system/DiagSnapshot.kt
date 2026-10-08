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
) {
    companion object {
        // Capture once so the screen and exported report describe the same runtime session.
        suspend fun capture(context: Context): DiagSnapshot {
            val appCtx = context.applicationContext
            val env = RuntimeEnv.resolve(appCtx)
            val level = DiagRepo(appCtx).current()
            val log = DiagLogReader.read()
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
            )
        }
    }
}
