package com.chaners.guiyuan.system

import android.content.Context
import com.chaners.guiyuan.BuildConfig

internal object DiagnosticsReportBuilder {
    private const val DetailedLogLineLimit = 600
    private const val ReleaseLogLineLimit = 120

    suspend fun build(context: Context): String =
        build(DiagnosticsSnapshotProvider.capture(context.applicationContext))

    internal fun build(snapshot: DiagnosticsSnapshot): String {
        val environment = snapshot.environment
        val diagnosticsLevel = snapshot.diagnosticsLevel
        val selected = snapshot.runtimeLog
        val lineLimit =
            if (
                diagnosticsLevel.name == "Detailed" &&
                (BuildConfig.RUNTIME_DIAGNOSTICS || BuildConfig.DEVELOPMENT_PROBES)
            ) {
                DetailedLogLineLimit
            } else {
                ReleaseLogLineLimit
            }
        val sessionLines = selected.latestSessionLines
        val runtimeHealth = snapshot.runtimeHealth
        val moduleLines = sessionLines.takeLast(lineLimit)
        val requestedDiagnosticsLevel = diagnosticsLevel.name.lowercase()
        val runtimeDiagnostics = runtimeHealth.component("diagnostics")
        val effectiveDiagnosticsLevel = runtimeDiagnostics?.fields?.get("level")
        val diagnosticsSyncState =
            when {
                !BuildConfig.RUNTIME_DIAGNOSTICS && !BuildConfig.DEVELOPMENT_PROBES ->
                    "not-applicable"
                runtimeDiagnostics == null ||
                    runtimeDiagnostics.state == "unknown" ||
                    runtimeDiagnostics.state == "unavailable" ->
                    "unavailable"
                BuildConfig.DEVELOPMENT_PROBES ->
                    if (effectiveDiagnosticsLevel == "detailed") {
                        "development-forced"
                    } else {
                        "mismatch"
                    }
                effectiveDiagnosticsLevel == requestedDiagnosticsLevel ->
                    "matched"
                else ->
                    "mismatch"
            }

        return buildString {
            appendLine("Guiyuan Diagnostic Report")
            appendLine()
            appendLine("[App]")
            appendLine("version=" + BuildConfig.VERSION_NAME)
            appendLine("build=" + BuildConfig.BUILD_ID)
            appendLine("package=" + BuildConfig.APPLICATION_ID)
            appendLine("buildType=" + BuildConfig.BUILD_TYPE)
            appendLine("channel=" + BuildConfig.BUILD_CHANNEL)
            appendLine("diagnosticsPreference=" + diagnosticsLevel.name.lowercase())
            appendLine(
                "diagnosticsCapability=" +
                    when {
                        BuildConfig.DEVELOPMENT_PROBES -> "development"
                        BuildConfig.RUNTIME_DIAGNOSTICS -> "runtime"
                        else -> "release"
                    },
            )
            appendLine()
            appendLine("[Diagnostics state]")
            appendLine("requestedLevel=" + requestedDiagnosticsLevel)
            appendLine("effectiveRuntimeLevel=" + (effectiveDiagnosticsLevel ?: "unavailable"))
            appendLine("syncState=" + diagnosticsSyncState)
            appendLine("schemaVersion=" + runtimeHealth.schemaVersion)
            appendLine("sessionId=" + (runtimeHealth.sessionId ?: "legacy-or-unavailable"))
            appendLine()
            appendLine("[Device]")
            appendLine("manufacturer=" + environment.manufacturer)
            appendLine("name=" + environment.deviceName)
            appendLine("model=" + environment.model)
            appendLine("device=" + environment.codename)
            appendLine("android=" + environment.androidVersion)
            appendLine("sdk=" + environment.sdk)
            appendLine("os=" + environment.osVersion)
            appendLine("systemUiVersion=" + environment.systemUiVersionName)
            appendLine(
                "systemUiVersionCode=" +
                    (environment.systemUiVersionCode?.toString() ?: "unknown"),
            )
            appendLine()
            appendLine("[Runtime health]")
            appendLine("source=structured-runtime-events")
            runtimeHealth.reportLines().forEach(::appendLine)
            appendLine()
            appendLine("[Runtime log]")
            appendLine("source=" + selected.source.reportName)
            appendLine("collection=" + collectionState(selected.result))
            appendLine("lines=" + moduleLines.size)
            if (moduleLines.isEmpty()) {
                appendLine("No Guiyuan runtime log entries were available.")
            } else {
                moduleLines.forEach(::appendLine)
            }
            appendLine()
            appendLine("[Report]")
            appendLine("generatedAt=" + snapshot.capturedAt)
        }
    }

    private fun collectionState(result: RootShell.Result): String =
        when {
            result.isSuccess -> "ok"
            result.timedOut -> "timeout"
            result.error != null -> "error:" + result.error
            else -> "exit:" + result.exitCode
        }
}
