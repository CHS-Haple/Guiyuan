package com.chaners.guiyuan.system

import android.content.Context
import com.chaners.guiyuan.settings.DiagnosticsLevel
import com.chaners.guiyuan.settings.DiagnosticsRepo
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DiagnosticsSnapshot(
    val environment: RuntimeEnv,
    val diagnosticsLevel: DiagnosticsLevel,
    val runtimeLog: DiagnosticsLogReader.Snapshot,
    val runtimeHealth: RuntimeHealthSnapshot,
    val sessionEntries: List<DiagnosticLogEntry>,
    val capturedAt: OffsetDateTime,
)

internal object DiagnosticsSnapshotProvider {
    suspend fun capture(context: Context): DiagnosticsSnapshot {
        val appCtx = context.applicationContext
        val env = RuntimeEnv.resolve(appCtx)
        val level = DiagnosticsRepo(appCtx).currentLevel()
        val log = DiagnosticsLogReader.read()
        val entries =
            withContext(Dispatchers.Default) {
                log.latestSessionLines.map(DiagnosticsLogParser::parse)
            }
        val health = RuntimeHealthSnapshot.fromLines(log.latestSessionLines)

        return DiagnosticsSnapshot(
            environment = env,
            diagnosticsLevel = level,
            runtimeLog = log,
            runtimeHealth = health,
            sessionEntries = entries,
            capturedAt = OffsetDateTime.now(),
        )
    }
}
