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
        val level = snapshot.level
        val log = snapshot.log
        val limit = exportLineLimit(level)
        val omitted = omittedEarlierLines(snapshot)
        val logLines = log.sessionLines.takeLast(limit)
        return buildString {
            appendLine("Guiyuan Diagnostic Report")
            appendLine()
            appendAppInfo(level)
            appendDiagnosticsState(snapshot)
            appendDeviceInfo(snapshot.env)
            appendLine("[Runtime snapshot]")
            appendLine("source=structured-runtime-events")
            snapshot.runtimeEvents.reportLines().forEach(::appendLine)
            appendLine()
            if (limit == DETAILED_LINES) {
                appendLine("[Diagnostic evidence]")
                DiagEvidenceSummary.reportLines(snapshot).forEach(::appendLine)
                appendLine()
            }
            appendRuntimeLog(log, logLines, omitted)
            appendLine("[Report]")
            appendLine("generatedAt=" + snapshot.capturedAt)
        }
    }

    private fun StringBuilder.appendAppInfo(level: DiagLevel) {
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
    }

    private fun StringBuilder.appendDiagnosticsState(snapshot: DiagSnapshot) {
        val runtimeEvents = snapshot.runtimeEvents
        val log = snapshot.log
        val level = snapshot.level
        val requested = level.name.lowercase()
        // The newest bind/snapshot supersedes older level evidence even when its level is absent.
        // A level-only event does not supersede the last observed binding.
        var levelEvent: LogEntry? = null
        var bindingEvent: LogEntry? = null
        for (entry in snapshot.entries.asReversed()) {
            if (!entry.structured ||
                entry.component != "diagnostics" ||
                (runtimeEvents.sessionId != null &&
                    entry.fields["sessionId"] != runtimeEvents.sessionId)
            ) continue

            when (entry.event) {
                "diagnostics.bind", "diagnostics.snapshot" -> {
                    if (levelEvent == null) levelEvent = entry
                    if (bindingEvent == null) bindingEvent = entry
                }
                "diagnostics.level" -> if (levelEvent == null) levelEvent = entry
            }
            if (levelEvent != null && bindingEvent != null) break
        }
        val runtimeLevel = levelEvent?.fields?.get("level") ?: "not-observed"
        val runtimeBinding = bindingEvent?.state ?: "not-observed"
        appendLine("[Diagnostics state]")
        appendLine("requestedLevel=" + requested)
        appendLine("runtimeLevel=" + runtimeLevel)
        appendLine("runtimeBinding=" + runtimeBinding)
        appendLine("systemUiDetailedConfirmed=" + snapshot.isRuntimeDetailConfirmed())
        if (log.clearedAt != null) {
            appendLine("visibleHistorySince=" + log.clearedAt)
            appendLine("historyClearScope=guiyuan-view-and-report-only")
        }
        appendLine("schemaVersion=" + runtimeEvents.schemaVersion)
        appendLine("sessionId=" + (runtimeEvents.sessionId ?: "unavailable"))
        appendLine()
    }

    private fun StringBuilder.appendDeviceInfo(env: RuntimeEnv) {
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
    }

    private fun StringBuilder.appendRuntimeLog(
        log: DiagLogReader.Snapshot,
        logLines: List<String>,
        omitted: Int,
    ) {
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
    }

    private fun collectionState(result: RootShell.Result): String =
        when {
            result.isSuccess -> "ok"
            result.timedOut -> "timeout"
            result.error != null -> "error:" + result.error
            else -> "exit:" + result.exitCode
        }
}
