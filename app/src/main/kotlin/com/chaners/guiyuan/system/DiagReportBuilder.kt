package com.chaners.guiyuan.system

import android.content.Context
import com.chaners.guiyuan.BuildConfig

internal object DiagReportBuilder {
    private const val DetailedLogLineLimit = 600
    private const val ReleaseLogLineLimit = 120

    suspend fun build(context: Context): String =
        build(DiagCapture.capture(context.applicationContext))

    internal fun build(snapshot: DiagSnapshot): String {
        val env = snapshot.environment
        val level = snapshot.diagnosticsLevel
        val log = snapshot.runtimeLog
        val lineLimit =
            if (
                level.name == "Detailed" &&
                (BuildConfig.RUNTIME_DIAGNOSTICS || BuildConfig.DEVELOPMENT_PROBES)
            ) {
                DetailedLogLineLimit
            } else {
                ReleaseLogLineLimit
            }
        val lines = log.latestSessionLines
        val health = snapshot.runtimeHealth
        val moduleLines = lines.takeLast(lineLimit)
        val requestedLevel = level.name.lowercase()
        val runtimeDiag = health.component("diagnostics")
        val effectiveLevel = runtimeDiag?.fields?.get("level")
        val syncState =
            when {
                !BuildConfig.RUNTIME_DIAGNOSTICS && !BuildConfig.DEVELOPMENT_PROBES ->
                    "not-applicable"
                runtimeDiag == null ||
                    runtimeDiag.state == "unknown" ||
                    runtimeDiag.state == "unavailable" ->
                    "unavailable"
                BuildConfig.DEVELOPMENT_PROBES ->
                    if (effectiveLevel == "detailed") {
                        "development-forced"
                    } else {
                        "mismatch"
                    }
                effectiveLevel == requestedLevel ->
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
            appendLine("diagnosticsPreference=" + level.name.lowercase())
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
            appendLine("requestedLevel=" + requestedLevel)
            appendLine("effectiveRuntimeLevel=" + (effectiveLevel ?: "unavailable"))
            appendLine("syncState=" + syncState)
            appendLine("schemaVersion=" + health.schemaVersion)
            appendLine("sessionId=" + (health.sessionId ?: "legacy-or-unavailable"))
            appendLine()
            appendLine("[Device]")
            appendLine("manufacturer=" + env.manufacturer)
            appendLine("name=" + env.deviceName)
            appendLine("model=" + env.model)
            appendLine("device=" + env.codename)
            appendLine("android=" + env.androidVersion)
            appendLine("sdk=" + env.sdk)
            appendLine("os=" + env.osVersion)
            appendLine("systemUiVersion=" + env.systemUiVersionName)
            appendLine(
                "systemUiVersionCode=" +
                    (env.systemUiVersionCode?.toString() ?: "unknown"),
            )
            appendLine()
            appendLine("[Runtime health]")
            appendLine("source=structured-runtime-events")
            health.reportLines().forEach(::appendLine)
            appendLine()
            appendLine("[Runtime log]")
            appendLine("source=" + log.source.reportName)
            appendLine("collection=" + collectionState(log.result))
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
