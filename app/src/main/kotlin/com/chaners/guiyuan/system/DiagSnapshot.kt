package com.chaners.guiyuan.system

import android.content.Context
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.DiagRepo
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DiagSnapshot(
    val environment: RuntimeEnv,
    val diagnosticsLevel: DiagLevel,
    val runtimeLog: DiagLogReader.Snapshot,
    val runtimeHealth: RuntimeHealthSnapshot,
    val sessionEntries: List<DiagLogEntry>,
    val capturedAt: OffsetDateTime,
)

internal object DiagCapture {
    suspend fun capture(context: Context): DiagSnapshot {
        val appCtx = context.applicationContext
        val env = RuntimeEnv.resolve(appCtx)
        val level = DiagRepo(appCtx).currentLevel()
        val log = DiagLogReader.read()
        val entries =
            withContext(Dispatchers.Default) {
                log.latestSessionLines.map(DiagLogParser::parse)
            }
        val health = RuntimeHealthSnapshot.fromLines(log.latestSessionLines)

        return DiagSnapshot(
            environment = env,
            diagnosticsLevel = level,
            runtimeLog = log,
            runtimeHealth = health,
            sessionEntries = entries,
            capturedAt = OffsetDateTime.now(),
        )
    }
}
