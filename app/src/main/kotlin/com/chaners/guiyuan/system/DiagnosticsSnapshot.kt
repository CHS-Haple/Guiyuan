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
    val allEntries: List<DiagnosticLogEntry>,
    val sessionEntries: List<DiagnosticLogEntry>,
    val shareLogResult: RootShell.Result,
    val shareLines: List<String>,
    val capturedAt: OffsetDateTime,
)

internal object DiagnosticsSnapshotProvider {
    private const val ShareLogTimeoutSeconds = 10L
    private const val ShareLogLineLimit = 80

    private const val ShareLogcatCommand =
        "logcat -d -b all -v threadtime -t 3000 | grep -F 'CombinedStatusShare' || true"

    suspend fun capture(context: Context): DiagnosticsSnapshot {
        val appContext = context.applicationContext
        val environment = RuntimeEnvironmentInfo.resolve(appContext)
        val diagnosticsLevel = DiagnosticsSettingsRepository(appContext).currentLevel()
        val runtimeLog = DiagnosticsLogReader.read()
        val parsedEntries =
            withContext(Dispatchers.Default) {
                runtimeLog.lines.map(DiagnosticsLogParser::parse) to
                    runtimeLog.latestSessionLines.map(DiagnosticsLogParser::parse)
            }
        val runtimeHealth = RuntimeHealthSnapshot.fromLines(runtimeLog.latestSessionLines)
        val shareLogResult =
            RootShell.execute(
                command = ShareLogcatCommand,
                timeoutSeconds = ShareLogTimeoutSeconds,
            )
        val shareLines =
            ShareDiagnosticsStore
                .read(appContext)
                .takeLast(ShareLogLineLimit)

        return DiagnosticsSnapshot(
            environment = environment,
            diagnosticsLevel = diagnosticsLevel,
            runtimeLog = runtimeLog,
            runtimeHealth = runtimeHealth,
            allEntries = parsedEntries.first,
            sessionEntries = parsedEntries.second,
            shareLogResult = shareLogResult,
            shareLines = shareLines,
            capturedAt = OffsetDateTime.now(),
        )
    }
}
