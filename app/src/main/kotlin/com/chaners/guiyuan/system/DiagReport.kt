package com.chaners.guiyuan.system

import com.chaners.guiyuan.BuildConfig

internal object DiagReport {
    private const val DETAILED_LINES = 600
    private const val RELEASE_LINES = 120

    internal fun build(snapshot: DiagSnapshot): String {
        val env = snapshot.env
        val level = snapshot.level
        val log = snapshot.log
        val limit =
            if (
                level.name == "Detailed" &&
                (BuildConfig.RUNTIME_DIAGNOSTICS || BuildConfig.DEVELOPMENT_PROBES)
            ) {
                DETAILED_LINES
            } else {
                RELEASE_LINES
            }
        val lines = log.sessionLines
        val health = snapshot.health
        val logLines = lines.takeLast(limit)
        val requested = level.name.lowercase()
        val runtime = health.component("diagnostics")
        val effective = runtime?.fields?.get("level")
        val syncState =
            when {
                !BuildConfig.RUNTIME_DIAGNOSTICS && !BuildConfig.DEVELOPMENT_PROBES ->
                    "not-applicable"
                runtime == null ||
                    runtime.state == "unknown" ||
                    runtime.state == "unavailable" ->
                    "unavailable"
                BuildConfig.DEVELOPMENT_PROBES ->
                    if (effective == "detailed") {
                        "development-forced"
                    } else {
                        "mismatch"
                    }
                effective == requested ->
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
            appendLine("requestedLevel=" + requested)
            appendLine("effectiveRuntimeLevel=" + (effective ?: "unavailable"))
            appendLine("syncState=" + syncState)
            appendLine("schemaVersion=" + health.schemaVersion)
            appendLine("sessionId=" + (health.sessionId ?: "legacy-or-unavailable"))
            appendLine()
            appendLine("[Device]")
            appendLine("manufacturer=" + env.manufacturer)
            appendLine("name=" + env.device)
            appendLine("model=" + env.model)
            appendLine("device=" + env.codename)
            appendLine("android=" + env.androidVersion)
            appendLine("sdk=" + env.sdk)
            appendLine("os=" + env.osVersion)
            appendLine("systemUiVersion=" + env.sysUiVersion)
            appendLine(
                "systemUiVersionCode=" +
                    (env.sysUiVersionCode?.toString() ?: "unknown"),
            )
            appendLine()
            appendLine("[Runtime health]")
            appendLine("source=structured-runtime-events")
            health.reportLines().forEach(::appendLine)
            appendLine()
            appendLine("[Runtime log]")
            appendLine("source=" + log.source.reportName)
            appendLine("collection=" + collectionState(log.result))
            appendLine("lines=" + logLines.size)
            if (logLines.isEmpty()) {
                appendLine("No Guiyuan runtime log entries were available.")
            } else {
                logLines.forEach(::appendLine)
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
