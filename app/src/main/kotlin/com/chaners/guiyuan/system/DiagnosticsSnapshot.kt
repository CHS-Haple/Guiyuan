package com.chaners.guiyuan.system

import android.content.Context
import com.chaners.guiyuan.settings.DiagnosticsLevel
import com.chaners.guiyuan.settings.DiagnosticsSettingsRepository
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DiagnosticsSnapshot(
    val environment: RuntimeEnvironmentInfo,
    val diagnosticsLevel: DiagnosticsLevel,
    val runtimeLog: DiagnosticsLogReader.Snapshot,
    val runtimeHealth: RuntimeHealthSnapshot,
    val sessionEntries: List<DiagnosticLogEntry>,
    val capturedAt: OffsetDateTime,
)

internal object DiagnosticsSnapshotProvider {
    suspend fun capture(context: Context): DiagnosticsSnapshot {
        val appContext = context.applicationContext
        val environment = RuntimeEnvironmentInfo.resolve(appContext)
        val diagnosticsLevel = DiagnosticsSettingsRepository(appContext).currentLevel()
        val runtimeLog = DiagnosticsLogReader.read()
        val sessionEntries =
            withContext(Dispatchers.Default) {
                runtimeLog.latestSessionLines.map(DiagnosticsLogParser::parse)
            }
        val runtimeHealth = RuntimeHealthSnapshot.fromLines(runtimeLog.latestSessionLines)

        return DiagnosticsSnapshot(
            environment = environment,
            diagnosticsLevel = diagnosticsLevel,
            runtimeLog = runtimeLog,
            runtimeHealth = runtimeHealth,
            sessionEntries = sessionEntries,
            capturedAt = OffsetDateTime.now(),
        )
    }
}
