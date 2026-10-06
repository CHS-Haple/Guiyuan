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
    val health: RuntimeHealthSnapshot,
    val entries: List<LogEntry>,
    val capturedAt: OffsetDateTime,
) {
    companion object {
        // Capture once so the screen and exported report describe the same runtime session.
        suspend fun capture(context: Context): DiagSnapshot {
            val appCtx = context.applicationContext
            val log = DiagLogReader.read()
            val entries =
                withContext(Dispatchers.Default) {
                    log.sessionLines.map(DiagLogParser::parse)
                }

            return DiagSnapshot(
                env = RuntimeEnv.resolve(appCtx),
                level = DiagRepo(appCtx).current(),
                log = log,
                health = RuntimeHealthSnapshot.fromLines(log.sessionLines),
                entries = entries,
                capturedAt = OffsetDateTime.now(),
            )
        }
    }
}
