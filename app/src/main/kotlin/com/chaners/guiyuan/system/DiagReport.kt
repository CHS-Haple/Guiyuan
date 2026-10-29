package com.chaners.guiyuan.system

import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.settings.DiagLevel

internal object DiagReport {
    private const val DETAILED_LINES = 1500
    private const val RELEASE_LINES = 300

    internal fun exportLineLimit(level: DiagLevel): Int =
        if (
            level == DiagLevel.Detailed &&
            (BuildConfig.RUNTIME_DIAGNOSTICS || BuildConfig.DEVELOPMENT_PROBES)
        ) {
            DETAILED_LINES
        } else {
            RELEASE_LINES
        }

    internal fun omittedEarlierLines(snapshot: DiagSnapshot): Int =
        (snapshot.log.sessionLines.size - exportLineLimit(snapshot.level)).coerceAtLeast(0)

    internal fun build(snapshot: DiagSnapshot): String {
        val env = snapshot.env
        val level = snapshot.level
        val log = snapshot.log
        val limit = exportLineLimit(level)
        val omitted = omittedEarlierLines(snapshot)
        val lines = log.sessionLines
        val runtimeEvents = snapshot.runtimeEvents
        val logLines = lines.takeLast(limit)
        val requested = level.name.lowercase()
        // Level changes do not carry binding state; keep their observations separate.
        val diagnostics =
            snapshot.entries
                .asReversed()
                .filter { entry ->
                    entry.structured &&
                        entry.component == "diagnostics" &&
                        (runtimeEvents.sessionId == null ||
                            entry.fields["sessionId"] == runtimeEvents.sessionId)
                }
        val runtimeLevel =
            diagnostics.firstNotNullOfOrNull { it.fields["level"] } ?: "not-observed"
        val runtimeBinding =
            diagnostics.firstNotNullOfOrNull { entry ->
                if (entry.event == "diagnostics.bind" || entry.event == "diagnostics.snapshot") {
                    entry.state
                } else {
                    null
                }
            } ?: "not-observed"
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
            appendLine("runtimeLevel=" + runtimeLevel)
            appendLine("runtimeBinding=" + runtimeBinding)
            appendLine("schemaVersion=" + runtimeEvents.schemaVersion)
            appendLine("sessionId=" + (runtimeEvents.sessionId ?: "unavailable"))
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
            if (BuildConfig.FONT_AXIS_DIAGNOSTIC && level == DiagLevel.Detailed && snapshot.fontAxis != null) {
                appendLine("[Font axis diagnostics]")
                appendLine("receipt=" + snapshot.fontAxis.receipt)
                appendLine("mobile=" + snapshot.fontAxis.mobile)
                appendLine("battery=" + snapshot.fontAxis.battery)
                appendLine()
            }
            appendLine("[Runtime snapshot]")
            appendLine("source=structured-runtime-events")
            runtimeEvents.reportLines().forEach(::appendLine)
            appendLine()
            if (level == DiagLevel.Detailed &&
                (BuildConfig.RUNTIME_DIAGNOSTICS || BuildConfig.DEVELOPMENT_PROBES)
            ) {
                appendLine("[Diagnostic evidence]")
                DiagEvidenceSummary.reportLines(snapshot).forEach(::appendLine)
                appendLine()
            }
            appendLine("[Runtime log]")
            appendLine("source=" + log.source.reportName)
            appendLine("collection=" + collectionState(log.result))
            if (log.result.truncated) appendLine("capture=latest-complete-lines-only")
            appendLine("lines=" + logLines.size)
            if (omitted > 0) {
                appendLine("report=latest-session-lines-only")
                appendLine("omittedEarlierSessionLines=" + omitted)
            }
            if (logLines.isEmpty()) {
                appendLine(
                    if (log.result.isSuccess) {
                        "No Guiyuan runtime log entries were available."
                    } else {
                        "Runtime logs were not collected. Check Root access or log collection."
                    },
                )
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
